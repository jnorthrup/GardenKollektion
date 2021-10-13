package rxf.server.web.inf

import one.xio.AsioVisitor
import one.xio.HttpHeaders
import one.xio.HttpStatus
import one.xio.MimeType
import rxf.server.*
import rxf.server.BlobAntiPatternObject.receiveBufferSize
import java.io.File
import java.io.RandomAccessFile
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.util.*

/**
 * User: jim
 * Date: 6/4/12
 * Time: 1:42 AM
 */
open class ContentRootImpl : AsioVisitor.Impl, PreRead {
    protected var req: Rfc822HeaderState.HttpRequest? = null
    var file: File? = null
    private var rootPath: String? = UpstreamNamespace.Companion.UPSTREAM_DEFAULT_FS_ROOT
    private var cursor: ByteBuffer? = null
    private var channel: SocketChannel? = null

    constructor() {
        init()
    }

    constructor(rootPath: String?) {
        this.rootPath = rootPath
        init()
    }

    fun init() {
        val dir = File(rootPath)
        if (!dir.isDirectory && dir.canRead()) throw IllegalAccessError("can't verify readable dir at $rootPath")
    }

    /**
     * onRead always has an inbound payload.
     */
    @Throws(Exception::class)
    override fun onRead(key: SelectionKey) {
        channel = key.channel() as SocketChannel
        if (cursor == null) {
            val attachment = key.attachment()
            (attachment as? Array<*>)?.let { ar ->
                for (o in ar) {
                    when (o) {
                        is ByteBuffer -> {
                            cursor = o
                        }
                    }
                    if (o is Rfc822HeaderState) {
                        req = o.`$req`
                    }
                }
            }
            key.attach(this)
        }
        (ByteBuffer.allocateDirect(receiveBufferSize).takeIf { (null == cursor) }
            ?: (cursor.takeIf { cursor!!.hasRemaining() }
                ?: ByteBuffer.allocateDirect(cursor!!.capacity() shl 1)?.put(cursor!!.rewind() as ByteBuffer)))?.also { cursor = it }
        val read = channel!!.read(cursor)
        if (read == -1) key.cancel()
        val flip: Buffer = cursor!!.duplicate().flip()
        req = Rfc822HeaderState().addHeaderInterest(HttpHeaders.`Accept$2dEncoding`,
            HttpHeaders.`If$2dModified$2dSince`,
            HttpHeaders.`If$2dUnmodified$2dSince`).`$req`.invoke(
            (flip as ByteBuffer)) as Rfc822HeaderState.HttpRequest
        if (!BlobAntiPatternObject.suffixMatchChunks(BlobAntiPatternObject.HEADER_TERMINATOR, req!!.headerBuf )) {
            return
        }
        cursor = flip.slice()
        key.interestOps(SelectionKey.OP_WRITE)
    }

    @Throws(Exception::class)
    override fun onWrite(key: SelectionKey) {
        var finalFname = fileScrub(rootPath + SLASHDOTSLASH + req!!.path().split("\\?").toTypedArray()[0])
        file = File(finalFname)
        if (file!!.isDirectory) {
            file = File("$finalFname/index.html")
        }
        finalFname = file!!.canonicalPath
        val fdate = Date(file!!.lastModified())
        var since = req!!.headerString(HttpHeaders.`If$2dModified$2dSince`)
        val accepts = req!!.headerString(HttpHeaders.`Accept$2dEncoding`)
        val res = req!!.`$res`
        if (null != since) {
            val cachedDate  = DateHeaderParser.parseDate(since)
            if (cachedDate?.after(fdate) == true) {
                res.status(HttpStatus.`$304`).headerString(HttpHeaders.Connection, "close")
                    .headerString(HttpHeaders.`Last$2dModified`,
                        DateHeaderParser.Companion.formatHttpHeaderDate(fdate))
                val write = channel!!.write(res.`as`(ByteBuffer::class.java))
                key.interestOps(SelectionKey.OP_READ).attach(null)
                return
            }
        } else {
            since = req!!.headerString(HttpHeaders.`If$2dUnmodified$2dSince`)
            if (null != since) {
                val cachedDate = DateHeaderParser.Companion.parseDate(since)
                if (cachedDate?.before(fdate)==true) {
                    res!!.status(HttpStatus.`$412`).headerString(HttpHeaders.Connection, "close").headerString(
                        HttpHeaders.`Last$2dModified`, DateHeaderParser.Companion.formatHttpHeaderDate(fdate))
                    val write = channel!!.write(res.`as`(ByteBuffer::class.java))
                    key.interestOps(SelectionKey.OP_READ).attach(null)
                    return
                }
            }
        }
        var ceString: String? = null
        if (null != accepts) {
            for (compType in CompressionTypes.values()) {
                if (accepts.contains(compType.name)) {
                    val f = File(file!!.absoluteFile.toString() + "." + compType.suffix)
                    if (f.isFile && f.canRead()) {
                        if (BlobAntiPatternObject.isDEBUG_SENDJSON) {
                            System.err.println("sending compressed archive: " + f.absolutePath)
                        }
                        ceString = compType.name
                        file = f
                        break
                    }
                }
            }
        }
        val send200 = file!!.canRead() && file!!.isFile
        if (send200) {
            val randomAccessFile = RandomAccessFile(file, "r")
            val total = randomAccessFile.length()
            val fileChannel = randomAccessFile.channel
            val substring = finalFname.substring(finalFname.lastIndexOf('.') + 1)
            val mimeType = MimeType.valueOf(substring)
            val length = randomAccessFile.length()
            res.status(HttpStatus.`$200`).headerString(HttpHeaders.`Content$2dType`,
                (mimeType ?: MimeType.bin).contentType.first()).headerString(HttpHeaders.`Content$2dLength`, length.toString())
                .headerString(
                    HttpHeaders.Connection, "close")
                .headerString(HttpHeaders.Date, DateHeaderParser.Companion.formatHttpHeaderDate(fdate))
            if (null != ceString) res.headerString(HttpHeaders.`Content$2dEncoding`, ceString)
            val response = res.`as`(ByteBuffer::class.java)
            channel!!.write(response)
            val sendBufferSize = BlobAntiPatternObject.sendBufferSize
            val progress = longArrayOf(fileChannel.transferTo(0, sendBufferSize.toLong(), channel))
            key.interestOps(SelectionKey.OP_WRITE or SelectionKey.OP_CONNECT)
            key.selector().wakeup()
            key.attach(object : AsioVisitor.Impl() {
                @Throws(Exception::class)
                override fun onWrite(key: SelectionKey) {
                    var remaining = total - progress[0]
                    progress[0] += fileChannel.transferTo(progress[0],
                        Math.min(sendBufferSize.toLong(), remaining),
                        channel)
                    remaining = total - progress[0]
                    if (0L == remaining) {
                        fileChannel.close()
                        randomAccessFile.close()
                        key.selector().wakeup()
                        key.interestOps(SelectionKey.OP_READ).attach(null)
                    }
                }
            })
        } else {
            key.selector().wakeup()
            key.interestOps(SelectionKey.OP_WRITE).attach(object : AsioVisitor.Impl() {
                @Throws(Exception::class)
                override fun onWrite(key: SelectionKey) {
                    channel!!.write(req!!.`$res`.status(HttpStatus.`$404`)
                        .headerString(HttpHeaders.`Content$2dLength`, "0").`as`(
                            ByteBuffer::class.java))
                    key.selector().wakeup()
                    key.interestOps(SelectionKey.OP_READ).attach(null)
                }
            })
        }
    }

    companion object {
        val SLASHDOTSLASH = File.separator + "." + File.separator
        val DOUBLESEP = File.separator + File.separator
        fun fileScrub(scrubMe: String?): String? {
            val inverseChar = if ('/' == File.separatorChar) '\\' else '/'
            return scrubMe?.trim { it <= ' ' }?.replace(inverseChar, File.separatorChar)?.replace(DOUBLESEP,
                "" + File.separator)?.replace("..", ".")
        }
    }
}