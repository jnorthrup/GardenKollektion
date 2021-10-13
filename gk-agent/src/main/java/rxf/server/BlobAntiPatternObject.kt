package rxf.server

import one.xio.HttpHeaders
import one.xio.HttpMethod
import rxf.server.driver.RxfBootstrap
import vec.util.rem
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.net.URISyntaxException
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * [ Blob Anti Pattern ](http://www.antipatterns.com/briefing/sld024.htm)
 * used here as a pattern to centralize the antipatterns
 * User: jim
 * Date: 4/17/12
 * Time: 11:55 PM
 */
object BlobAntiPatternObject {
    val RXF_CACHED_THREADPOOL = "true" == RxfBootstrap.getVar("RXF_CACHED_THREADPOOL", "false")
    val CONNECTION_POOL_SIZE = RxfBootstrap.getVar("RXF_CONNECTION_POOL_SIZE", "20")!!.toInt()
    val CE_TERMINAL = "\n0\r\n\r\n".toByteArray(Charsets.UTF_8)

    //"premature optimization" s/mature/view/
    val STATIC_VF_HEADERS= Rfc822HeaderState.Companion.staticHeaderStrings(HttpHeaders.ETag,
        HttpHeaders.`Content$2dLength`,
        HttpHeaders.`Transfer$2dEncoding`)
    val STATIC_JSON_SEND_HEADERS= Rfc822HeaderState.Companion.staticHeaderStrings(HttpHeaders.ETag,
        HttpHeaders.`Content$2dLength`,
        HttpHeaders.`Content$2dEncoding`)
    val STATIC_CONTENT_LENGTH_ARR=
        Rfc822HeaderState.Companion.staticHeaderStrings(HttpHeaders.`Content$2dLength`)
    val HEADER_TERMINATOR = "\r\n\r\n".toByteArray(Charsets.UTF_8)
    val ATOMIC_INTEGER = AtomicInteger(0)
    val REALTIME_CUTOFF = RxfBootstrap.getVar("RXF_REALTIME_CUTOFF", "3")!!.toInt()
    const val PCOUNT = "-0xdeadbeef.2"
    const val GENERATED_METHODS = "/*generated methods vsd78vs0fd078fv0sa78*/"
    const val IFACE_FIRE_TARGETS = "/*fire interface ijnoifnj453oijnfiojn h*/"
    const val FIRE_METHODS = "/*embedded fire terminals j63l4k56jn4k3jn5l63l456jn*/"
    private val couchConnections = LinkedBlockingDeque<SocketChannel?>(CONNECTION_POOL_SIZE)
    var isDEBUG_SENDJSON = System.getenv().containsKey("DEBUG_SENDJSON")
    val REALTIME_UNIT = TimeUnit.valueOf(RxfBootstrap.getVar("RXF_REALTIME_UNIT",
        ((isDEBUG_SENDJSON) % TimeUnit.HOURS.name) ?: TimeUnit.SECONDS.name)!!)
    var upstreamAddress: InetSocketAddress? = null
    var eXECUTOR_SERVICE =
        ((RXF_CACHED_THREADPOOL) % Executors.newCachedThreadPool()) ?: Executors.newFixedThreadPool(Runtime.getRuntime()
            .availableProcessors() + 3)

    init {
        val rxfcouchprefix = RxfBootstrap.getVar("RXF_UPSTREAM_PREFIX", "http://localhost:5984")
        try {
            val uri = URI(rxfcouchprefix)
            var port: Int = uri.port.let { port -> if (-1 != port) port else 80 }
            upstreamAddress = InetSocketAddress(uri.host, port)
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
    }

    fun createUpstreamConnection(): SocketChannel? {
        var ret: SocketChannel? = null
        while (!HttpMethod.Companion.killswitch) {
            val poll = couchConnections.poll()
            when {
                null != poll -> {
                    // If there was at least one entry, try to use that
                    // Note that we check both connected&&open, its possible to be connected but not open, at least in 1.7.0_45
                    if (poll.isConnected && poll.isOpen) {
                        ret = poll;break
                    }
                    //non null entry, but invalid, continue in loop to grab the next...
                }
                else -> {
                    // no recycled connections available for reuse, make a new one
                    try {
                        val channel = SocketChannel.open(upstreamAddress)
                        channel.configureBlocking(false)
                        ret = channel;break
                    } catch (e: Exception) {
                        // if something went wrong in the process of creating the connection, continue in loop...
                        e.printStackTrace()
                    }
                }
            }
        }
        // killswitch, return null
        return ret
    }

    fun recycleChannel(channel: SocketChannel?) {
        try {
            // Note that we check both connected&&open, its possible to be connected but not open, at least in 1.7.0_45
            if (!channel!!.isConnected || !channel.isOpen || !couchConnections.offerLast(channel)) {
                channel.close()
            }
        } catch (e: IOException) {
            //eat all exceptions, recycle should be brain-dead easy
            e.printStackTrace()
        }
    }

    fun <T> deepToString(vararg d: T): String {
        return Arrays.deepToString(d) +  HttpMethod.wheresWaldo ()
    }

    fun <T> arrToString(vararg d: T): String {
        return Arrays.deepToString(d)
    }

    val receiveBufferSize by lazy {

        val socketChannel = createUpstreamConnection()
        socketChannel!!.socket().receiveBufferSize.also {
            recycleChannel(socketChannel)
        }


    }

    val sendBufferSize by lazy {
        val socketChannel = createUpstreamConnection()
        socketChannel!!.socket().receiveBufferSize.also {
            recycleChannel(socketChannel)
        }
    }


    @JvmStatic
    fun dequote(s: String?): String? {
        var ret = s
        if (null != s && ret!!.startsWith("\"") && ret.endsWith("\"")) {
            ret = ret.substring(1, ret.lastIndexOf('"'))
        }
        return ret
    }

    /**
     * 'do the right thing' when handed a buffer with no remaining bytes.
     *
     * @param buf
     * @return
     */
    fun avoidStarvation(buf: ByteBuffer?): ByteBuffer? = buf?.apply { if (!hasRemaining()) buf.rewind() }

    val defaultOrgName: String?
        get() = UpstreamNamespace.Companion.UPSTREAM_DEFAULT_ORGNAME

    /**
     * byte-compare of suffixes
     *
     * @param terminator  the token used to terminate presumably unbounded growth of a list of buffers
     * @param currentBuff current ByteBuffer which does not necessarily require a list to perform suffix checks.
     * @param prev        a linked list which holds previous chunks
     * @return whether the suffix composes the tail bytes of current and prev buffers.
     */
    fun suffixMatchChunks(terminator: ByteArray?, currentBuff: ByteBuffer?, vararg prev: ByteBuffer?): Boolean {
        var tb = currentBuff
        var prevMark = prev.size
        val bl = terminator!!.size
        var rskip = 0
        var i = bl - 1
        while (0 <= i) {
            rskip++
            val comparisonOffset = tb!!.position() - rskip
            if (0 > comparisonOffset) {
                prevMark--
                if (0 <= prevMark) {
                    tb = prev[prevMark]
                    rskip = 0
                    i++
                } else {
                    return false
                }
            } else if (terminator[i] != tb[comparisonOffset]) {
                return false
            }
            i--
        }
        return true
    }

    var LOOPBACK: InetAddress? = null

}