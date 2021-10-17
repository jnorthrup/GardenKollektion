package gk.kademlia.agent.fsm

import vec.util._a
import java.nio.channels.SelectionKey

class readNode(override val r: KeyAction) : FsmNode {
    override val w = null
    override val a = null
    override val c = null
    override val interestOrder = _a[SelectionKey.OP_READ]
    override val interest = computeInterest()
}