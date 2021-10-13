package rxf.server

import one.xio.AsioVisitor
import one.xio.HttpHeaders
import rxf.server.BlobAntiPatternObject.receiveBufferSize
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.util.*
import java.util.regex.Pattern

/**
 * User: jim
 * Date: 6/4/12
 * Time: 1:40 AM
 */
class HttpProxyImpl(private val passthroughExpr: Pattern) : AsioVisitor.Impl() {
    @Throws(Exception::class)
    override fun onWrite(browserKey: SelectionKey) {
        browserKey.selector().wakeup()
        browserKey.interestOps(SelectionKey.OP_READ)
        val path: String
        var state: Rfc822HeaderState? = null
        for (o in Arrays.asList(browserKey.attachment())) if (o is Rfc822HeaderState) {
            ActionBuilder.get()!!.state(o.also { state = it })
            break
        }
        if (null == state) {
            throw Error("this GET proxy requires " + Rfc822HeaderState::class.java.canonicalName + " in " + SelectionKey::class.java.canonicalName + ".attachments :(")
        }
        path = state!!.pathResCode()
        val matcher = passthroughExpr.matcher(path)
        if (matcher.matches()) {
            val link = matcher.group(1)
            val req = """GET $link HTTP/1.1
Accept: image/*, text/*
Connection: close

"""
            val couchConnection = BlobAntiPatternObject.createUpstreamConnection()
            RelaxFactoryServerImpl.Companion.enqueue(couchConnection,
                SelectionKey.OP_CONNECT or SelectionKey.OP_WRITE,
                object : AsioVisitor.Impl() {
                    @Throws(Exception::class)
                    override fun onRead(couchKey: SelectionKey) {
                        val channel = couchKey.channel() as SocketChannel
                        val dst = ByteBuffer.allocateDirect(BlobAntiPatternObject.receiveBufferSize)
                        val read = channel.read(dst)
                        val proxyState = Rfc822HeaderState(*HEADER_INTEREST)
                        val total = proxyState.headerString(HttpHeaders.`Content$2dLength`)!!.toInt()
                        val browserChannel = browserKey.channel() as SocketChannel
                        try {
                            val write = browserChannel.write(dst.rewind() as ByteBuffer)
                        } catch (e: IOException) {
                            couchConnection!!.close()
                            return
                        }
                        couchKey.selector().wakeup()
                        couchKey.interestOps(SelectionKey.OP_READ).attach(object : AsioVisitor.Impl() {
                            private val browserSlave: AsioVisitor.Impl = object : AsioVisitor.Impl() {
                                @Throws(Exception::class)
                                override fun onWrite(key: SelectionKey) {
                                    try {
                                        val write = browserChannel.write(dst)
                                        if (!dst.hasRemaining() && remaining == 0) browserChannel.close()
                                        browserKey.selector().wakeup()
                                        browserKey.interestOps(0)
                                        couchKey.selector().wakeup()
                                        couchKey.interestOps(SelectionKey.OP_READ).selector().wakeup()
                                    } catch (e: Exception) {
                                        browserChannel.close()
                                    } finally {
                                    }
                                }
                            }
                            init {
                                browserKey.attach(browserSlave)
                            }
                            val sharedBuf = ByteBuffer.allocateDirect(total.coerceAtMost(receiveBufferSize))

                            var remaining = total

                            @Throws(Exception::class)
                            override fun onRead(couchKey: SelectionKey) {
                                if (browserKey.isValid && remaining != 0) {
                                    dst.compact() //threadsafety guarantee by monothreaded selector
                                    remaining -= couchConnection!!.read(dst)
                                    dst.flip()
                                    couchKey.selector().wakeup()
                                    couchKey.interestOps(0)
                                    browserKey.selector().wakeup()
                                    browserKey.interestOps(SelectionKey.OP_WRITE).selector().wakeup()
                                } else {
                                    BlobAntiPatternObject.recycleChannel(couchConnection)
                                }
                            }
                        })
                    }

                    @Throws(Exception::class)
                    override fun onWrite(couchKey: SelectionKey) {
                        couchConnection!!.write(Charsets.UTF_8.encode(req))
                        couchKey.selector().wakeup()
                        couchKey.interestOps(SelectionKey.OP_READ)
                    }
                })
        }
    }

    companion object {
        val HEADER_INTEREST=
            Rfc822HeaderState.Companion.staticHeaderStrings(HttpHeaders.`Content$2dLength`)
    }
}