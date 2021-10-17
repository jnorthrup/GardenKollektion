package gk.kademlia.agent


import gk.kademlia.NetworkSize
import gk.kademlia.id.NUID
import gk.kademlia.routing.RoutingTable

interface Agent<Sz : NetworkSize, T : Comparable<T>> {
    val NUID: NUID<T>
    val routingTable: RoutingTable<Sz, T>
}


