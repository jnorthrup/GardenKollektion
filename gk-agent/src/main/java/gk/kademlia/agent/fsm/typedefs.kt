package gk.kademlia.agent.fsm

import vec.macros.Pai2
import vec.macros.Vect02
import java.nio.channels.SelectionKey

typealias KeyAction = (SelectionKey) -> FsmNode?
/**like RFC822 smtp message*/
typealias SimpleMessage = Pai2<Vect02<String, String>, String>