package gk.kademlia.agent.fsm

import vec.util._a
import java.nio.channels.SelectionKey

/**
 * node in a FSM tree of actions.
 */
interface FsmNode {
    val r: KeyAction?
    val w: KeyAction?
    val a: KeyAction?
    val c: KeyAction?
    val interestOrder get() = _a[SelectionKey.OP_ACCEPT, SelectionKey.OP_READ, SelectionKey.OP_CONNECT, SelectionKey.OP_WRITE]
    val interest get() = computeInterest(interestOrder)
    fun computeInterest(io: IntArray = interestOrder) = interestOrder.fold(0) { acc, i -> acc or i }
    fun process(selectionKey: SelectionKey) {
        for (i in interestOrder) {
            when (i) {
                SelectionKey.OP_ACCEPT -> a
                SelectionKey.OP_READ -> r
                SelectionKey.OP_CONNECT -> c
                SelectionKey.OP_WRITE -> w
                else -> null
            }?.run { this(selectionKey) }
        }
    }
}