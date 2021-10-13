package rxf.server

import one.xio.AsioVisitor
import java.io.IOException
import java.net.UnknownHostException

/**
 * Created with IntelliJ IDEA.
 * User: jim
 * Date: 1/2/13
 * Time: 8:12 PM
 * To change this template use File | Settings | File Templates.
 */
interface RelaxFactoryServer {
    @Throws(UnknownHostException::class)
    fun init(hostname: String?, port: Int, topLevel: AsioVisitor?)

    @Throws(IOException::class)
    fun start()

    @Throws(IOException::class)
    fun stop()
    val isRunning: Boolean

    /**
     * Returns the port the server has started on. Useful in the case where
     * [.init] was invoked with 0, [.start] called,
     * and the server selected its own port.
     *
     * @return
     */
    val port: Int

    object App {
        fun get(): RelaxFactoryServer {
            var relaxFactoryServer = rxfTl.get()
            if (null == relaxFactoryServer) {
                relaxFactoryServer = RelaxFactoryServerImpl()
                rxfTl.set(relaxFactoryServer)
            }
            return relaxFactoryServer
        }
    }

    companion object {
        val rxfTl: InheritableThreadLocal<RelaxFactoryServer?> = InheritableThreadLocal()
    }
}