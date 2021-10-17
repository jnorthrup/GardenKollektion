package gk.kademlia.agent.fsm

import vec.util._a
import java.nio.channels.SelectionKey

class acceptNode(override val a: KeyAction) : FsmNode {
    override val w = null
    override val r = null
    override val c = null
    override val interestOrder = _a[SelectionKey.OP_ACCEPT]
    override val interest = computeInterest()
}