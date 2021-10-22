package gk.kademlia.agent


import gk.kademlia.id.NUID
import gk.kademlia.net.NetMask
import gk.kademlia.routing.RoutingTable

interface Agent<TNum : Comparable<TNum>, Sz : NetMask<TNum>> {
    /**
     * Network Unique Id
     */
    val NUID: NUID<TNum>
    val routingTable: RoutingTable<TNum, Sz>
}