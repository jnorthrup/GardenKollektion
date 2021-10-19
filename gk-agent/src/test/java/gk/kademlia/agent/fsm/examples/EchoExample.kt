package gk.kademlia.agent.fsm

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.Executors

fun main() {
    //typical boilerplate
    Executors.newCachedThreadPool().apply {
        submit {
            val top = echoAcceptor()
            val agentFsm: FSM = FSM(top)
            agentFsm.qUp(top, null, ServerSocketChannel.open().bind(InetSocketAddress(2112)).configureBlocking(false))
            submit(agentFsm)
        }
        val lock = Object()
        synchronized(lock) {
            while (!isShutdown) {
                lock.wait(5000)
            }
        }
    }
}

fun echoAcceptor() = AcceptNode {
    val accept = (it.channel() as ServerSocketChannel).accept()
    accept.configureBlocking(false)
    val buf = ByteBuffer.allocateDirect(80)
    val fsmNode = echoReader(buf)
    accept.register(it.selector(), fsmNode.interest, fsmNode)
    return@AcceptNode null
}

private fun echoReader(buf: ByteBuffer) = ReadNode {
    val socketChannel = it.channel() as SocketChannel
    val read = socketChannel.read(buf)
    if (!buf.hasRemaining() || read == -1) {
        buf.flip()
        echoWriter(buf)
    } else null
}

private fun echoWriter(
    buf: ByteBuffer,
) = WriteNode {
    val socketChannel = it.channel() as SocketChannel
    if (buf.hasRemaining()) socketChannel.write(buf) else socketChannel.close()
    null
}
