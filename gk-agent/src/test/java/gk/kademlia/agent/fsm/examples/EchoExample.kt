package gk.kademlia.agent.fsm

import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

fun main() {
    //typical boilerplate
    FSM.launch(echoAcceptor())
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
