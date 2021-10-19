package gk.kademlia.agent.fsm

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.concurrent.Executors

/**
 *
 * simple demo that echo's back the first 40 bytes or clearss the echo buffer.
 *
 * $ nc -u :: 2112
aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
 */

fun main() {
    //typical boilerplate
    val threadPool = Executors.newCachedThreadPool()
    val udpChannel = DatagramChannel.open()
    val addr = InetSocketAddress(2112)
    udpChannel.socket().bind(addr)
    udpChannel.configureBlocking(false)
    lateinit var agentFsm: FSM

    val buf: ByteBuffer = ByteBuffer.allocate(20)
    lateinit var top: ReadNode
    top = ReadNode {
        val datagramChannel = it.channel() as DatagramChannel
        val sa = datagramChannel.receive(buf)
        if (buf.hasRemaining()) {
            buf.clear()
            top;
        } else
            WriteNode {
                datagramChannel.send(buf.flip(), sa)
                if (!buf.hasRemaining()) {
                    buf.clear()
                    top
                } else null
            }
    }

    agentFsm = FSM(top)
    agentFsm.qUp(top, null, udpChannel)
    threadPool.submit(agentFsm)

    val lock = Object()
    synchronized(lock) {
        while (!agentFsm.killswitch) {
            lock.wait(5000)
        }
    }
}

