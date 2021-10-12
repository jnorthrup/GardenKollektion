package gk.kademlia.http

import java.io.IOError
import java.io.IOException
import java.io.RandomAccessFile
import java.lang.ref.Reference
import java.lang.ref.SoftReference
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.*
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.activation.MimeType
import kotlin.experimental.and

/**
 * See  http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html
 * User: jim
 * Date: May 6, 2009
 * Time: 10:12:22 PM
 */
enum class HttpMethod {
    GET {
        override fun onWrite(key: SelectionKey) {
            val a = key.attachment() as Array<Any>
            val xfer = a[1] as Xfer
            xfer.sendChunk(key)
        }

        override fun onConnect(key: SelectionKey) {
            onAccept(key)
        }

        /**
         * enrolls a new SelectionKey to the methods
         *
         * @param key
         * @throws IOException
         */
        override fun onAccept(key: SelectionKey) {
            try {
                assert(key.attachment() is ByteBuffer)
                val buffer = key.attachment() as ByteBuffer
                val parameters = methodParameters(buffer)
                val strings = parameters.toString().split(" ").toTypedArray()
                val fname = strings[0]
                val fnode = RandomAccessFile("./$fname".replace("..+/", "./"), "r")
                if (fnode.fd.valid()) {
                    val fc = fnode.channel
                    val channel = key.channel() as SocketChannel
                    val xfer = Xfer(fc, fname)
                    response(key, HttpStatus.`$200`)
                    val byteBufferReference = borrowBuffer(DEFAULT_EXP)
                    try {
                        val buffer1 = byteBufferReference.get()
                        var mimeType: MimeType? = null
                        try {
                            mimeType = MimeType.valueOf(fname.substring(fname.lastIndexOf('.') + 1))
                        } catch (ignored: Exception) {
                            throw IOError(ignored)
                        }
                        val x = (mimeType)?.let { """Content-Type: ${mimeType.contentType.toString()}""".trimIndent() }
                            ?: "\n"
                        val c = buffer1!!.asCharBuffer().append("""
    Connection: close
    ${x}Content-Length: ${fc.size()}
    """.trimIndent()).append("\n\n").flip() as CharBuffer
                        channel.write(UTF8.encode(c))
                        key.interestOps(SelectionKey.OP_WRITE)
                        key.attach(arrayOf(this, xfer))
                    } catch (e: Exception) {
                    } finally {
                        recycle(byteBufferReference, DEFAULT_EXP)
                    }
                    return
                }
            } catch (e: Exception) {
            }
            try {
                response(key, HttpStatus.`$404`)
                key.cancel()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        internal inner class Xfer(var fc: FileChannel?, var name: CharSequence) {
            var progress: Long = 0
            var creation = System.currentTimeMillis()
            var completion = -1L
            var chunk: Long = 0
            private val pipeline = false
            private fun sendChunk(key: SelectionKey) {
                var channel: SocketChannel? = null
                if (fc!!.isOpen && key.isValid && key.channel().isOpen) try {
                    channel = key.channel() as SocketChannel
                    progress += fc!!.transferTo(progress, Math.min(remaining, ++chunk shl 8), channel)
                    if (remaining < 1) {
                        throw COMPLETION_EXCEPTION
                    }
                } catch (e: Exception) {
                    key.cancel()
                    try {
                        fc!!.close()
                    } catch (e1: IOException) {
//                            e1.printStackTrace();
                    }
                    fc = null
                    try {
                        channel?.close()
                    } catch (e1: IOException) {
                    }
                } catch (e: CompletionException) {
                    try {
                        fc!!.close()
                    } catch (e1: IOException) {
                    }
                    if (pipeline) {
                        key.attach(`$`)
                        key.interestOps(SelectionKey.OP_READ)
                    } else key.cancel()
                    return
                }
            }

            val remaining: Long
                get() = try {
                    fc!!.size() - progress
                } catch (e: Exception) {
                    0
                }

            @Throws(IOException::class)
            fun logEntry(): CharSequence {
                return StringBuilder().append(javaClass.name).append(':').append(name).append(' ').append(progress)
                    .append('/').append(remaining)
            }
        }
    },
    POST, PUT, HEAD, DELETE, TRACE, CONNECT, OPTIONS, HELP, VERSION {
        override fun onAccept(selectionKey: SelectionKey) {
            if (selectionKey.isAcceptable) {
                var client: SocketChannel? = null
                val clientkey: SelectionKey? = null
                try {
                    client = serverSocketChannel!!.accept()
                    client.configureBlocking(false).register(selector, SelectionKey.OP_READ)
                } catch (e: IOException) {
                    e.printStackTrace()
                    try {
                        client?.close()
                    } catch (e1: IOException) {
                    }
                }
            }
        }

        /**
         * this is where we take the input channel bytes, and write them to an output channel
         *
         * @param key
         */
        override fun onWrite(key: SelectionKey) {
            val att = key.attachment() as Array<Any>
            if (att != null) {
                val method = att[0] as HttpMethod
                method.onWrite(key)
                return
            }
            key.cancel()
        }

        /**
         * this is where we implement http 1.1. request handling
         *
         *
         * Lifecycle of the attachemnts is
         *
         *  1.  null means new socket
         *  1. we attach(buffer) during the onConnect
         *  1.  we *expect* Object[HttpMethod,*,...] to be present for ongoing connections to delegate
         *
         *
         * @param key
         * @throws IOException
         */
        override fun onRead(key: SelectionKey) {
            var byteBufferReference: Reference<ByteBuffer?>? = null
            try {
                val p = key.attachment() as Array<Any>
                if (p == null) {
                    val channel: SocketChannel
                    channel = key.channel() as SocketChannel
                    byteBufferReference = borrowBuffer(DEFAULT_EXP)
                    try {
                        val buffer = byteBufferReference.get()
                        val i = channel.read(buffer)
                        buffer!!.flip().mark()
                        for (httpMethod in values()) if (httpMethod.recognize(buffer.reset() as ByteBuffer)) {
                            //System.out.println("found: " + httpMethod);
                            key.attach(buffer)
                            httpMethod.onConnect(key)
                            return
                        }
                        response(key, HttpStatus.`$400`)
                        channel.write(buffer)
                    } catch (e: Exception) {
                    } finally {
                        recycle(byteBufferReference, DEFAULT_EXP)
                    }
                    channel.close()
                    return
                }
                val fst = p[0] as HttpMethod
                fst.onRead(key)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    val token = ByteBuffer.wrap(name.toByteArray()).rewind().mark() as ByteBuffer
    val margin = name.length + 1

    /**
     * deduce a few parse optimizations
     *
     * @param request
     * @return
     */
    fun recognize(request: ByteBuffer): Boolean {
        if (Character.isWhitespace(request[margin].toInt())) for (i in 0 until margin - 1) if (request[i] != token[i]) return false
        return true
    }

    /**
     * returns a byte-array of token offsets in the first delim +1 bytes of the input buffer     *
     *
     *
     * stateless and heapless
     *
     * @param in
     * @return
     */
    fun tokenize(`in`: ByteBuffer): ByteBuffer {
        val out = `in`.duplicate().position(0) as ByteBuffer
        var isBlank = true
        var wasBlank = true
        val prevIdx = 0
        `in`.position(margin)
        var b = 0.toChar()
        while (b != '\n' && out.position() < margin) {
            wasBlank = isBlank
            b = (`in`.get() and 0xff).toInt().toChar()
            isBlank = Character.isWhitespace(b.code and 0xff)
            if (!isBlank && wasBlank) {
                out.put(((`in`.position() and 0xff).toByte() - 1).toByte())
                println("token found: " + `in`.duplicate().position(prevIdx))
            }
        }
        while (out.put(0.toByte()).position() < margin);
        return out.position(0) as ByteBuffer
    }

    @Throws(IOException::class)
    fun methodParameters(indexEntries: ByteBuffer): CharSequence {
        /***
         * seemingly a lot of work to do as little as possible
         *
         */
        indexEntries.position(0)
        var last = 0
        var b: Int

        // start from 0 and traverese to null terminator inserted during the tokenization...
        while (indexEntries.get().also { b = it.toInt() } != 0 && indexEntries.position() <= margin) last = b and 0xff
        val len = indexEntries.position()

        //this should be between 40 and 300 something....
        indexEntries.position(last)
        while (!Character.isISOControl(indexEntries.get() and 0xff.also { b = it }.toByte()) && !Character.isWhitespace(
                b) && '\n'.code != b && '\r'.code != b && '\t'.code != b
        );
        return decoder.decode(indexEntries.flip().position(margin) as ByteBuffer)
    }

    fun onRead(key: SelectionKey) {
        val o = key.attachment()
        if (o is ByteBuffer) {
            tokenize(o)
        }
    }

    /**
     * enrolls a new SelectionKey to the methods
     *
     * @param key
     * @throws IOException
     */
    fun onConnect(key: SelectionKey) {
        try {
            response(key, HttpStatus.`$501`)
            val b = key.attachment() as ByteBuffer
            val channel = key.channel()
            val c = channel as SocketChannel
            c.write(b.rewind() as ByteBuffer)
        } catch (e: IOException) {
        } finally {
            try {
                key.channel().close()
            } catch (e: IOException) {
            }
            key.cancel()
        }
    }

    fun onWrite(key: SelectionKey?) {
        throw UnsupportedOperationException()
    }

    fun onAccept(key: SelectionKey?) {
        throw UnsupportedOperationException()
    }

    internal class CompletionException : Throwable()
    companion object {
        private val COMPLETION_EXCEPTION = CompletionException()
        private const val DEFAULT_EXP = 0
        val UTF8 = Charset.forName("UTF8")
        private val RANDOM = Random()

        @Throws(IOException::class)
        private fun response(key: SelectionKey, httpStatus: HttpStatus) {
            val byteBufferReference = borrowBuffer(DEFAULT_EXP)
            try {
                val buffer = byteBufferReference.get()
                val charBuffer =
                    buffer!!.asCharBuffer().append("HTTP/1.1 ").append(httpStatus.name.substring(1)).append(' ')
                        .append(httpStatus.caption).append('\n').flip() as CharBuffer
                val out = UTF8.encode(charBuffer)
                (key.channel() as SocketChannel).write(out)
            } catch (e: Exception) {
            } finally {
                recycle(byteBufferReference, DEFAULT_EXP)
            }
        }

        val charset = UTF8
        val charsetEncoder = charset.newEncoder()
        val decoder = charset.newDecoder()
        var killswitch = false
        var selector: Selector? = null
        var serverSocketChannel: ServerSocketChannel? = null

        //    public ByteBuffer token;
        private const val port = 8080
        private var threadPool: ExecutorService? = null
        const val CHUNKDEFAULT = 4
        const val CHUNK_NUM = 128
        const val KBYTE = 1024
        private const val MAX_EXP = 16

        init {
            try {
                selector = Selector.open()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            threadPool = Executors.newCachedThreadPool()
            threadPool.submit(Runnable {
                try {
                    init()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })
        }

        fun borrowBuffer(vararg exp: Int): Reference<ByteBuffer?> {
            val slot = if (exp.size == 0) DEFAULT_EXP else exp[0]
            if (exp.size > 1) println("heap " + slot + " count " + counter[slot])
            val buffer = buffers[slot]
            val o: Reference<ByteBuffer?>?
            o = if (buffer.isEmpty()) {
                refill(slot)
                borrowBuffer(slot, counter[slot]++)
            } else {
                buffer.remove()
            }
            minus()
            return if (o!!.get() == null) borrowBuffer(*exp) else o
        }

        private fun minus() {
            //System.out.write('-');
        }

        @Synchronized
        private fun refill(slot: Int) {
            var queue = buffers[slot]
            if (queue == null) {
                buffers[slot] = ConcurrentLinkedQueue()
                queue = buffers[slot]
            }
            if (queue.isEmpty()) {
                val czize = KBYTE shl slot
                val buffer = ByteBuffer.allocateDirect(czize * CHUNK_NUM)
                for (i in 0 until CHUNK_NUM) {
                    val i2 = buffer.position()
                    val newPosition = i2 + czize
                    buffer.limit(newPosition)
                    queue.add(SoftReference(buffer.slice()))
                    plus()
                    buffer.position(newPosition)
                }
                //  System.out.flush();
            }
        }

        private val buffers = arrayOfNulls<Queue<*>>(MAX_EXP) as Array<Queue<Reference<ByteBuffer?>?>>

        init {
            for (i in buffers.indices) buffers[i] = ConcurrentLinkedQueue()
        }

        private fun init() {
            try {
                selector = Selector.open()
                serverSocketChannel = ServerSocketChannel.open()
                serverSocketChannel.socket().bind(InetSocketAddress(port))
                serverSocketChannel.configureBlocking(false)
                val listenerKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT)
                while (!killswitch) {
                    selector.select()
                    val keys = selector.selectedKeys()
                    val i = keys.iterator()
                    while (i.hasNext()) {
                        val key = i.next()
                        i.remove()
                        if (key.isValid) {
                            try {
                                val at = key.attachment()
                                val m: HttpMethod
                                m =
                                    if (at == null) `$` else ((if (at is Array<Any> && (at as Array<Any?>)[0] is HttpMethod) at[0] else at as? HttpMethod
                                        ?: `$`) as HttpMethod?)!!
                                if (key.isWritable) {
                                    m.onWrite(key)
                                }
                                if (key.isReadable) {
                                    m.onRead(key)
                                }
                                if (key.isConnectable) {
                                    m.onConnect(key)
                                }
                                if (key.isAcceptable) {
                                    m.onAccept(key)
                                }
                            } catch (e: Exception) {
                                key.attach(null)
                                key.channel().close()
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        //    static int counter[]=new int[MAX_EXP];
        fun recycle(byteBufferReference: Reference<ByteBuffer?>?, shift: Int) {
            val buffer = byteBufferReference!!.get()
            if (buffer != null) {
                buffer.clear()
                buffers[shift].add(byteBufferReference)
                plus()
            }
        }

        private fun plus() {
//        System.out.write('+');
        }

        private val counter = IntArray(MAX_EXP)

        @Throws(IOException::class)
        @JvmStatic
        fun main(a: Array<String>) {
            while (!killswitch) try {
                Thread.sleep(10000)
            } catch (e: InterruptedException) {
            }
        }
    }
}