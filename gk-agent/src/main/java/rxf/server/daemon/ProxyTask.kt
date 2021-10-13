package rxf.server.daemon

import one.xio.HttpMethod
import rxf.server.BlobAntiPatternObject
import java.lang.Boolean
import java.net.InetSocketAddress
import java.net.StandardSocketOptions
import java.nio.channels.SelectionKey
import java.nio.channels.ServerSocketChannel
import kotlin.Array
import kotlin.String
import kotlin.Throwable

/**
 * this launches the main service thread and assigns the proxy port to socketservers.
 * User: jnorthrup
 * Date: 10/1/13
 * Time: 7:27 PM
 */
open class ProxyTask : Runnable {
    var prefix: String? = null
    lateinit var proxyPorts: Array<String>
    override fun run() {
        try {
            for (proxyPort in proxyPorts) {
                HttpMethod.Companion.enqueue(ServerSocketChannel.open().bind(
                    InetSocketAddress(proxyPort.toInt()), 4096).setOption(
                    StandardSocketOptions.SO_REUSEADDR, Boolean.TRUE).configureBlocking(false),
                    SelectionKey.OP_ACCEPT, ProxyDaemon(this))
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            //boilerplate HttpMethod.init() here
            BlobAntiPatternObject.eXECUTOR_SERVICE.submit(object : ProxyTask() {
                init {
                    proxyPorts = args
                }
            })
        }
    }
}