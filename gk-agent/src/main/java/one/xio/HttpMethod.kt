package one.xio

import java.io.IOException
import java.nio.channels.*
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * See  http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html
 * User: jim
 * Date: May 6, 2009
 * Time: 10:12:22 PM
 */
enum class HttpMethod {
    GET, POST, PUT, HEAD, DELETE, TRACE, CONNECT, OPTIONS, HELP, VERSION;

    companion object {
        private val q = ConcurrentLinkedQueue<Array<*>>()
        var killswitch = false
        lateinit var selector: Selector
        lateinit var selectorThread: Thread

        /**
         * handles the threadlocal ugliness if any to registering user threads into the selector/reactor pattern
         *
         * @param channel the socketchanel
         * @param op      int ChannelSelector.operator
         * @param s       the payload: grammar {enum,data1,data..n}
         */
        fun enqueue(channel: SelectableChannel?, op: Int, vararg s: Any?) {
            assert(channel != null && !killswitch) { "Server appears to have shut down, cannot enqueue" }
            assert(channel!!.isOpen) { "Can't enqueue a closed channel" }
            if (Thread.currentThread() === selectorThread) try {
                channel.register(selector, op, s)
            } catch (e: ClosedChannelException) {
                e.printStackTrace()
            } else {
                q.add(arrayOf(channel, op, s))
            }
            val selector1 = selector
            selector1.wakeup()
        }

        /**
         * macro for a quick stacktrace to stuff into weakHashMap elsewhere
         */
        fun wheresWaldo(vararg depth: Int): String {
            val d = if (depth.size > 0) depth[0] else 2
            val throwable = Throwable()
            val throwable1 = throwable.fillInStackTrace()
            val stackTrace = throwable1.stackTrace
            var ret = ""
            var i = 2
            val end = StrictMath.min(stackTrace.size - 1, d)
            while (i <= end) {
                val stackTraceElement = stackTrace[i]
                ret += "	at ${stackTraceElement.className}.${stackTraceElement.methodName}(${stackTraceElement.fileName}:${stackTraceElement.lineNumber})\n"
                i++
            }
            return ret
        }

        @Throws(IOException::class)
        fun init(protocoldecoder: AsioVisitor?, vararg a: String?) {
            selector = Selector.open()
            selectorThread = Thread.currentThread()
            synchronized(a) {
                val timeoutMax: Long = 1024
                var timeout: Long = 1
                while (!killswitch) {
                    while (!q.isEmpty()) {
                        val s = q.remove()
                        val x = s!![0] as SelectableChannel?
                        val sel = selector
                        val op = s[1] as Int?
                        val att = s[2]
                        //          System.err.println("" + op + "/" + String.valueOf(att));
                        try {
                            x!!.configureBlocking(false)
                            val register = x.register(sel, op!!, att)!!
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    }
                    val select = selector.select(timeout)
                    timeout = if (0 == select) StrictMath.min(timeout shl 1, timeoutMax) else 1
                    if (0 != select) innerloop(protocoldecoder)
                }
            }
        }

        @Throws(IOException::class)
        private fun innerloop(protocoldecoder: AsioVisitor?) {
            val keys = selector.selectedKeys()
            val i = keys.iterator()
            while (i.hasNext()) {
                val key = i.next()
                i.remove()
                if (key.isValid) {
                    val channel = key.channel()
                    try {
                        val m = inferAsioVisitor(protocoldecoder, key)
                        if (key.isValid && key.isWritable) {
                            if ((channel as SocketChannel).socket().isOutputShutdown) {
                                key.cancel()
                            } else {
                                m!!.onWrite(key)
                            }
                        }
                        if (key.isValid && key.isReadable) {
                            if ((channel as SocketChannel).socket().isInputShutdown) {
                                key.cancel()
                            } else {
                                m!!.onRead(key)
                            }
                        }
                        if (key.isValid && key.isAcceptable) {
                            m!!.onAccept(key)
                        }
                        if (key.isValid && key.isConnectable) {
                            m!!.onConnect(key)
                        }
                    } catch (e: Throwable) {
                        val attachment = key.attachment()
                        if (attachment is Array<*>) {
                            System.err.println("BadHandler: " + Arrays.deepToString(attachment))
                        } else System.err.println("BadHandler: $attachment")
                        if (AsioVisitor.`$DBG`) {
                            val asioVisitor = inferAsioVisitor(protocoldecoder, key)
                            if (asioVisitor is AsioVisitor.Impl) {
                                val visitor = asioVisitor
                                if (AsioVisitor.`$origins`!!.containsKey(visitor)) {
                                    val s: String = AsioVisitor.`$origins`.get(visitor)!!
                                    System.err.println("origin$s")
                                }
                            }
                        }
                        e.printStackTrace()
                        key.attach(null)
                        channel.close()
                    }
                }
            }
        }

        fun inferAsioVisitor(defaultService: AsioVisitor?, key: SelectionKey) = key.attachment().let { attachment ->
            (((attachment as? Array<*>)?.let(Array<*>::first))
                ?: (attachment as? Iterable<*>)?.let(Iterable<*>::first)) as? AsioVisitor ?: defaultService
        }
    }
}

