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