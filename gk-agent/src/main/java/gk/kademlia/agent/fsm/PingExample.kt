package gk.kademlia.agent.fsm

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.Executors

fun main() {
    //typical boilerplate
    val threadPool = Executors.newCachedThreadPool()
    val serverSocketChannel = ServerSocketChannel.open()
    val addr = InetSocketAddress(2112)
    serverSocketChannel.socket().bind(addr)
    serverSocketChannel.configureBlocking(false)
    lateinit var agentFsm: FSM

    val top = echoAcceptor()
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

private fun echoAcceptor() = AcceptNode {
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
