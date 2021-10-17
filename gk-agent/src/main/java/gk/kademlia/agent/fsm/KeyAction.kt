package gk.kademlia.agent.fsm

import java.nio.channels.SelectionKey

typealias KeyAction = (SelectionKey) -> Unit