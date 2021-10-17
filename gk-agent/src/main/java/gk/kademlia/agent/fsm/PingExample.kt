package gk.kademlia.agent.fsm

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.Executors

fun main() {
    val threadPool = Executors.newCachedThreadPool()
    val serverSocketChannel = ServerSocketChannel.open()
    val addr = InetSocketAddress(2112)
    serverSocketChannel.socket().bind(addr)
    serverSocketChannel.configureBlocking(false)
    lateinit var agentFsm: FSM


    val top = AcceptNode {
        val accept = (it.channel() as ServerSocketChannel).accept()
        accept.configureBlocking(false)
        val buf = ByteBuffer.allocateDirect(80)
        val fsmNode = ReadNode {
            val socketChannel = it.channel() as SocketChannel
            val read = socketChannel.read(buf)
            return@ReadNode if (!buf.hasRemaining() || read == -1) {
                buf.flip()
                WriteNode {
                    if (buf.hasRemaining()) socketChannel.write(buf)
                    else socketChannel.close()
                    return@WriteNode null
                }
            } else null
        }
        agentFsm.qUp(fsmNode, null, accept)
        return@AcceptNode null
    }
    agentFsm = FSM(top)
    agentFsm.qUp(top, null, serverSocketChannel)
    threadPool.submit(agentFsm)

    val lock = Object()
    synchronized(lock) {
        while (!agentFsm.killswitch) {
            lock.wait(5000)
        }
    }
}