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
    val top = acceptNode {
        val accept = (it.channel() as ServerSocketChannel).accept()
        accept.configureBlocking(false)
        val buf = ByteBuffer.allocateDirect(80)

        val fsmNode = readNode {
            val socketChannel = it.channel() as SocketChannel
            val read = socketChannel.read(buf)
            if (!buf.hasRemaining() || read == -1) {

                buf.flip()
                val fsmNode = writeNode {
                    if (buf.hasRemaining()) socketChannel.write(buf)
                    else socketChannel.close()
                }
                agentFsm.qUp(fsmNode, it)
            }
        }
        agentFsm.qUp(fsmNode, null, accept)
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