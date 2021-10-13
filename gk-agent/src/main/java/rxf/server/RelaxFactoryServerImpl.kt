package rxf.server

import one.xio.AsioVisitor
import one.xio.HttpMethod
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.UnknownHostException
import java.nio.channels.ClosedChannelException
import java.nio.channels.SelectableChannel
import java.nio.channels.SelectionKey
import java.nio.channels.ServerSocketChannel

class RelaxFactoryServerImpl : RelaxFactoryServer {
    override var port = 8080
    private var topLevel: AsioVisitor? = null
    private var hostname: InetAddress? = null
    private var serverSocketChannel: ServerSocketChannel? = null

    @Volatile
    override var isRunning = false
        private set

    @Throws(UnknownHostException::class)
    override fun init(hostname: String?, port: Int, topLevel: AsioVisitor?) {
        assert(this.topLevel == null && serverSocketChannel == null) { "Can't call init twice" }
        this.topLevel = topLevel
        this.port = port
        this.hostname = InetAddress.getByName(hostname)
    }

    @Throws(IOException::class)
    override fun start() {
        assert(serverSocketChannel == null) { "Can't start already started server" }
        isRunning = true
        try {
            serverSocketChannel = ServerSocketChannel.open()
            val addr = InetSocketAddress(hostname, port)
            serverSocketChannel.socket().bind(addr)
            port = serverSocketChannel.socket().localPort
            println(hostname!!.hostAddress + ":" + port)
            serverSocketChannel.configureBlocking(false)
            enqueue(serverSocketChannel, SelectionKey.OP_ACCEPT, topLevel)
            init(topLevel)
        } finally {
            isRunning = false
        }
    }

    @Throws(IOException::class)
    override fun stop() {
        HttpMethod.Companion.killswitch = true
        serverSocketChannel!!.close()
    }

    companion object {
        /**
         * handles the threadlocal ugliness if any to registering user threads into the selector/reactor pattern
         *
         * @param channel the socketchanel
         * @param op      int ChannelSelector.operator
         * @param s       the payload: grammar {enum,data1,data..n}
         * @throws java.nio.channels.ClosedChannelException
         */
        @Throws(ClosedChannelException::class)
        fun enqueue(channel: SelectableChannel?, op: Int, vararg s: Any?) {
            HttpMethod.Companion.enqueue(channel, op, *s)
        }

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
                ret += """	at ${stackTraceElement.className}.${stackTraceElement.methodName}(${stackTraceElement.fileName}:${stackTraceElement.lineNumber})
"""
                i++
            }
            return ret
        }

        @Throws(IOException::class)
        fun init(protocoldecoder: AsioVisitor?, vararg a: String?) {
            HttpMethod.Companion.init(protocoldecoder, *a)
        }
    }
}