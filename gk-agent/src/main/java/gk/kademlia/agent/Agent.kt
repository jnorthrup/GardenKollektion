package gk.kademlia.agent

import gk.kademlia.NetworkSize
import gk.kademlia.id.NUID
import gk.kademlia.routing.RoutingTable
import vec.macros.Tripl3
import vec.util._a
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.*
import java.nio.channels.SelectionKey.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import kotlin.math.min

interface Agent<Sz : NetworkSize, T : Comparable<T>> {
    val NUID: NUID<T>
    val routingTable: RoutingTable<Sz, T>
}

class FSM(var topLevel: FsmNode? = null) : Runnable, AutoCloseable {
    private val q = ConcurrentLinkedQueue<Tripl3<SelectableChannel, Int, FsmNode>>()
    var selectorThread: Thread = Thread.currentThread()
    var selector: Selector = Selector.open()
    var timeoutMax: Long = 1024
    var timeout: Long = 1
    var killswitch = false
    override fun run() {
        this.selectorThread = Thread.currentThread()

        while (!killswitch && selector.isOpen) {
            synchronized(q) {
                while (q.isNotEmpty()) {
                    val s = q.remove()
                    val x = s.first
                    val op = s.second
                    val att = s.third
                    try {
                        x.configureBlocking(false)
                        x.register(selector, op, att)!!
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }

                }
            }
            val select = selector.select(timeout)
            timeout = if (0 == select) min(timeout shl 1, timeoutMax)
            else
                1
            if (0 != select)
                innerloop(topLevel!!)
        }
    }

    fun innerloop(protocoldecoder: FsmNode) {
        val keys = selector.selectedKeys()
        val i = keys.iterator()
        while (i.hasNext()) {
            val key = i.next()
            i.remove()
            if (key.isValid) {
                val node: FsmNode = ((key.attachment() as? FsmNode) ?: topLevel!!)
                node.process(key)
            }
        }
    }

    override fun close() {
        try {
            this.selector.close()
        } finally {
        }
        this.selectorThread.interrupt()
    }

    /**
     * handles the threadlocal ugliness if any to registering user threads into the selector/reactor pattern
     *
     * @param fsmNode provides the needed selector interest
     * @param selectionKey the already registered key or null
     * @param channel the Channel when selection Key is null
     */
    fun qUp(fsmNode: FsmNode, selectionKey: SelectionKey?, channel: SelectableChannel? = selectionKey?.channel()) {
        channel?.takeIf { !killswitch && it.isOpen }?.run {
            if (Thread.currentThread() === selectorThread) try {
                register(selector, fsmNode.interest, fsmNode)
            } catch (e: ClosedChannelException) {
                e.printStackTrace()
            } else synchronized(q) { q.add(Tripl3.Companion.invoke(this, fsmNode.interest, fsmNode)) }
            selector.wakeup()
        }
    }
}


typealias KeyAction = (SelectionKey) -> Unit

/**
 * node in a FSM tree of actions.
 */
interface FsmNode {
    val r: KeyAction?
    val w: KeyAction?
    val a: KeyAction?
    val c: KeyAction?
    val interestOrder get() = _a[OP_ACCEPT, OP_READ, OP_CONNECT, OP_WRITE]
    val interest get() = computeInterest(interestOrder)
    fun computeInterest(io: IntArray = interestOrder) = interestOrder.fold(0) { acc, i -> acc or i }
    fun process(selectionKey: SelectionKey) {
        for (i in interestOrder) {
            when (i) {
                OP_ACCEPT -> a
                OP_READ -> r
                OP_CONNECT -> c
                OP_WRITE -> w
                else -> null
            }?.run { this(selectionKey) }
        }
    }
}

class readNode(override val r: KeyAction) : FsmNode {
    override val w = null
    override val a = null
    override val c = null
    override val interestOrder = _a[OP_READ]
    override val interest = computeInterest()
}

class writeNode(override val w: KeyAction) : FsmNode {
    override val r = null
    override val a = null
    override val c = null
    override val interestOrder = _a[OP_WRITE]
    override val interest = computeInterest()
}

class acceptNode(override val a: KeyAction) : FsmNode {
    override val w = null
    override val r = null
    override val c = null
    override val interestOrder = _a[OP_ACCEPT]
    override val interest = computeInterest()
}

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
    agentFsm.qUp(top,null,serverSocketChannel)
    threadPool.submit(agentFsm)

    val lock = Object()
    synchronized(lock) {
        while (!agentFsm.killswitch) {
            lock.wait(5000)
        }
    }
}