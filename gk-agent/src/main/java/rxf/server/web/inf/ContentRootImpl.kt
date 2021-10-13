package rxf.server.web.inf

import one.xio.*
import rxf.server.*
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
    private var rootPath: String? = CouchNamespace.Companion.COUCH_DEFAULT_FS_ROOT
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

    @Throws(Exception::class)
    override fun onRead(key: SelectionKey) {
        channel = key.channel() as SocketChannel
        if (cursor == null) {
            if (key.attachment() is Array<Any>) {
                val ar = key.attachment() as Array<Any>
                for (o in ar) {
                    if (o is ByteBuffer) {
                        cursor = o
                        continue
                    }
                    if (o is Rfc822HeaderState) {
                        req = o.`$req`()
                        continue
                    }
                }
            }
            key.attach(this)
        }
        cursor =
            if (null == cursor) ByteBuffer.allocateDirect(BlobAntiPatternObject.getReceiveBufferSize()) else if (cursor!!.hasRemaining()) cursor else ByteBuffer.allocateDirect(
                cursor!!.capacity() shl 1).put(
                cursor!!.rewind() as ByteBuffer)
        val read = channel!!.read(cursor)
        if (read == -1) key.cancel()
        val flip: Buffer = cursor!!.duplicate().flip()
        req = Rfc822HeaderState().addHeaderInterest(HttpHeaders.`Accept$2dEncoding`,
            HttpHeaders.`If$2dModified$2dSince`,
            HttpHeaders.`If$2dUnmodified$2dSince`).`$req`().apply(
            (flip as ByteBuffer)) as Rfc822HeaderState.HttpRequest
        if (!BlobAntiPatternObject.suffixMatchChunks(BlobAntiPatternObject.HEADER_TERMINATOR, req!!.headerBuf())) {
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
        val res = req!!.`$res`()
        if (null != since) {
            val cachedDate: Date = DateHeaderParser.Companion.parseDate(since)
            if (cachedDate.after(fdate)) {
                res!!.status(HttpStatus.`$304`).headerString(HttpHeaders.Connection, "close")
                    .headerString(HttpHeaders.`Last$2dModified`,
                        DateHeaderParser.Companion.formatHttpHeaderDate(fdate))
                val write = channel!!.write(res.`as`(ByteBuffer::class.java))
                key.interestOps(SelectionKey.OP_READ).attach(null)
                return
            }
        } else {
            since = req!!.headerString(HttpHeaders.`If$2dUnmodified$2dSince`)
            if (null != since) {
                val cachedDate: Date = DateHeaderParser.Companion.parseDate(since)
                if (cachedDate.before(fdate)) {
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
                        if (BlobAntiPatternObject.DEBUG_SENDJSON) {
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
            res!!.status(HttpStatus.`$200`).headerString(HttpHeaders.`Content$2dType`,
                (mimeType ?: MimeType.bin).contentType).headerString(HttpHeaders.`Content$2dLength`, length.toString())
                .headerString(
                    HttpHeaders.Connection, "close")
                .headerString(HttpHeaders.Date, DateHeaderParser.Companion.formatHttpHeaderDate(fdate))
            if (null != ceString) res.headerString(HttpHeaders.`Content$2dEncoding`, ceString)
            val response = res.`as`(ByteBuffer::class.java)
            channel!!.write(response)
            val sendBufferSize = BlobAntiPatternObject.getSendBufferSize()
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
                    channel!!.write(req!!.`$res`().status(HttpStatus.`$404`)
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