package gk.kademlia.agent.fsm

import vec.util._a
import java.nio.channels.SelectionKey

class writeNode(override val w: KeyAction) : FsmNode {
    override val r = null
    override val a = null
    override val c = null
    override val interestOrder = _a[SelectionKey.OP_WRITE]
    override val interest = computeInterest()
}