package gk.kademlia.agent.fsm

import vec.macros.Tripl3
import java.nio.channels.ClosedChannelException
import java.nio.channels.SelectableChannel
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min

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
            } else synchronized(q) { q.add(Tripl3.invoke(this, fsmNode.interest, fsmNode)) }
            selector.wakeup()
        }
    }
}