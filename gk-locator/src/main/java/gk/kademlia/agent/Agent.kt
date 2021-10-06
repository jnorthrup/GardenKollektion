package gk.kademlia.agent

import gk.kademlia.NetworkSize
import gk.kademlia.RoutingTable
import gk.kademlia.id.NUID

interface Agent<Sz : NetworkSize, T : Comparable<T>> {
    val NUID: NUID<T>
    val routingTable: RoutingTable<Sz, T>
}