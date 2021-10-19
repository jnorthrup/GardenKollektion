package gk.kademlia.agent.fsm

import vec.macros.Tripl3
import java.net.InetSocketAddress
import java.nio.channels.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.min

class FSM(var topLevel: FsmNode? = null) : Runnable, AutoCloseable {
    private val q = ConcurrentLinkedQueue<Tripl3<SelectableChannel, Int, FsmNode>>()
    var selectorThread: Thread = Thread.currentThread()
    var selector: Selector = Selector.open()
    var timeoutMax: Long = 1024
    var timeout: Long = 1

    override fun run() {
        selectorThread = Thread.currentThread()

        while (selector.isOpen) {
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
            val select = selector.select { key ->
                key.takeIf { it.isValid }?.apply {
                    val node: FsmNode =
                        (attachment() as? FsmNode ?: topLevel ?: TODO("toplevel builtin functions not yet implemented"))
                    node.process(this)?.let { qUp(it, this) }
                }
            }
            timeout = if (0 == select) min(timeout shl 1, timeoutMax)
            else 1
        }
    }


    override fun close() {
        try {
            selector.close()
        } finally {
        }
        selectorThread.interrupt()

    }

    /**
     * handles the threadlocal ugliness if any to registering user threads into the selector/reactor pattern
     *
     * @param fsmNode provides the needed selector interest
     * @param selectionKey the already registered key or null
     * @param channel the Channel when selection Key is null
     */
    fun qUp(fsmNode: FsmNode, selectionKey: SelectionKey?, channel: SelectableChannel? = selectionKey?.channel()) {
        channel?.takeIf { it.isOpen }?.run {
            if (Thread.currentThread() === selectorThread) try {
                register(selector, fsmNode.interest, fsmNode)
            } catch (e: ClosedChannelException) {
                e.printStackTrace()
            } else synchronized(q) { q.add(Tripl3.invoke(this, fsmNode.interest, fsmNode)) }
            selector.wakeup()
        }
    }

    companion object {
        @JvmStatic
        fun launch(
            top: FsmNode,
            executorService: ExecutorService = Executors.newCachedThreadPool(),
            inetSocketAddress: InetSocketAddress = InetSocketAddress(2112),
            channel: SelectableChannel = ServerSocketChannel.open(),
        ) {
            executorService.apply {
                val agentFsm: FSM = FSM(top)
                submit {
                    val channel1 = channel.configureBlocking(false)
                    (channel1 as? NetworkChannel)?.bind(inetSocketAddress)
                    agentFsm.qUp(top, null, channel1)
                    submit(agentFsm)
                }
                val lock = Object()
                synchronized(lock) {
                    while (!isShutdown) {
                        lock.wait(1000)
                    }
                    /**
                     * boilerplate shutdown code should be here.
                     */
                    agentFsm.selector.close()//the kill switch
                }

            }
        }

    }
}