package rxf.server.daemon

import one.xio.AsioVisitor
import rxf.server.PreRead
import rxf.server.driver.RxfBootstrap
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import kotlin.text.Charsets.UTF_8

/**
 * this visitor shovels data from the outward selector to the inward selector, and vice versa.  once the headers are
 * sent inward the only state monitored is when one side of the connections close.
 */
open class HttpPipeVisitor(
    protected var name: String, //  public AtomicInteger remaining;
    var otherKey: SelectionKey, vararg val  b: ByteBuffer
) : AsioVisitor.Impl(), PreRead {
    var isLimit = false

    @Throws(Exception::class)
    override fun onRead(key: SelectionKey) {
        val channel = key.channel() as SocketChannel
        if (otherKey.isValid) {
            val read = channel.read(inBuffer)
            if (read == -1) /*key.cancel();*/ {
                channel.shutdownInput()
                key.interestOps(SelectionKey.OP_WRITE)
                channel.write(ByteBuffer.allocate(0))
            } else {
                //if buffer fills up, stop the read option for a bit
                otherKey.interestOps(SelectionKey.OP_READ or SelectionKey.OP_WRITE)
                channel.write(ByteBuffer.allocate(0))
            }
        } else {
            key.cancel()
        }
    }

    @Throws(Exception::class)
    override fun onWrite(key: SelectionKey) {
        val channel = key.channel() as SocketChannel
        val flip = outBuffer.flip() as ByteBuffer
        if (PROXY_DEBUG) {
            val decode: CharBuffer =  UTF_8.decode(flip.duplicate())
            System.err.println("writing to $name: $decode-")
        }
        val write = channel.write(flip)
        if (-1 == write || isLimit /*&& null != remaining && 0 == remaining.get()*/) {
            key.cancel()
        } else {
            //      if (isLimit() /*&& null != remaining*/) {
            //        /*this.remaining.getAndAdd(-write);*//*
            //        if (1 > remaining.get()) */{
            //          key.channel().close();
            //          otherKey.channel().close();
            //          return;
            //        }
            //      }
            key.interestOps(SelectionKey.OP_READ or SelectionKey.OP_WRITE) // (getOutBuffer().hasRemaining() ? OP_WRITE : 0));
            outBuffer.compact()
        }
    }

    val inBuffer
        get() = b[0]
    val outBuffer
        get() = b[1]

    companion object {
        val PROXY_DEBUG = "true" == RxfBootstrap.getVar("PROXY_DEBUG", false.toString())
    }
}