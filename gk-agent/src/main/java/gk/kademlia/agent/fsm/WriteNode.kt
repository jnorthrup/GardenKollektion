package gk.kademlia.agent.fsm

import java.nio.channels.SelectionKey.OP_WRITE

class WriteNode(override val process: KeyAction) : FsmNode {
    override val interest: Int = OP_WRITE
}