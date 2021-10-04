@file:Suppress("UNCHECKED_CAST", "SpellCheckingInspection")
@file:OptIn(ExperimentalStdlibApi::class)

package gk.kademlia

import java.util.concurrent.ConcurrentSkipListMap
import kotlin.math.max
import kotlin.math.min

interface RoutingTable<Sz : AddressSize, T : Number> {
    val agent: Agent<Sz, T>
    val nSize: Sz
    val routes: ConcurrentSkipListMap<GUID<Sz, T>, Route<Sz, T>>


    operator fun RoutingTable<Sz, T>.plus(node: Route<Sz, T>): Route<Sz, T>? =  routes.put(agent.guid xor node.guid!!, node)
     infix fun at(pow:Int){
         gk.kademlia.GUID
         (max(0, min(pow, nSize())))
     }

}

interface Agent<Sz : AddressSize, T : Number> {
    val guid: GUID<Sz, T>
    val routingTable: RoutingTable<Sz, T>

    /**bulk send 1 or more messages */
    val send: Api

    /**
     * bulk reroute
     */
    val recv: Api

}