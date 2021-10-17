package gk.kademlia.agent.fsm

import java.nio.channels.SelectionKey

/**
 * node in a FSM tree of actions.
 */
interface FsmNode {

    /**
     * this is part of the FSM and can be made mutable var to suppress e.g. READ, WRITE
     *
     * the default behavior is that the interestOrder is OR'd together for complex classes
     */
    val interest: Int

    /** performs simple action */
    val process: (SelectionKey) -> FsmNode?
}


interface ComplexFsmNode : FsmNode {
    val a: KeyAction?
    val c: KeyAction?
    val r: KeyAction?
    val w: KeyAction?

    /**
     * intersetOrder is a  helper for complex protocols that might immediately fire ACCEPT,READER,WRITE and can
     * do so more than once if there's a benefit to holding the worker thread there. */
    val interestOrder: IntArray //by lazy {  _a[SelectionKey.OP_ACCEPT, SelectionKey.OP_READ, SelectionKey.OP_CONNECT, SelectionKey.OP_WRITE]}
    fun computeInterest(io: IntArray = interestOrder): Int = interestOrder.fold(0) { acc, i -> acc or i }

    /**
     * the complex processor by default can support `{A|C}[W]RWRWRWRWRW` ops in rapid succession without yeilding.
     * however
     */
    override val process: KeyAction
        get() = { selectionKey ->
            var res: FsmNode? = null
            for (i in interestOrder) {
                res = when (i) {
                    SelectionKey.OP_ACCEPT -> a
                    SelectionKey.OP_READ -> r
                    SelectionKey.OP_CONNECT -> c
                    SelectionKey.OP_WRITE -> w
                    else -> null
                }?.let { it(selectionKey) }
            }
            res
        }
}
