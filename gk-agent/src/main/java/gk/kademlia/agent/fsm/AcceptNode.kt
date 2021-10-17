package gk.kademlia.agent.fsm

import java.nio.channels.SelectionKey.OP_ACCEPT

class AcceptNode(override val process: KeyAction) : FsmNode {
    override val interest: Int = OP_ACCEPT

}