package gk.kademlia.agent


import gk.kademlia.NetMask
import gk.kademlia.id.NUID
import gk.kademlia.routing.RoutingTable

interface Agent<Sz : NetMask, TNum : Comparable<TNum>> {
    /**
     * Network Unique Id
     */
    val NUID: NUID<TNum>
    val routingTable: RoutingTable<Sz, TNum>
}




