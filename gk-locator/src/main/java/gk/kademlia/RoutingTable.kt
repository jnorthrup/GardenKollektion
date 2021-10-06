package gk.kademlia

import gk.kademlia.id.NUID


/**
 * once an agent knows its network id it can create a routeTable, the agent will also be
 * responsible for assigning new GUIDS on all routes
 * before touching the route table.
 *
 */
open class RoutingTable<Sz : NetworkSize, T : Comparable<T>>(private val agentNUID: NUID<T>) {
    val bits get() = agentNUID.bits
    private val bitOps = agentNUID.ops
    private val here = agentNUID.id!!

    /**
     * contract is to have the route guid id fully realized in agent first
     */
    fun addRoute(other: Route<T>): Route<T> = other.let { (g, a): Route<T> ->
        val distance = bits.distance(bitOps, here, g.id!!)
        val distance1 = distance - 1
        val linkedHashMap = buckets[distance1]
        linkedHashMap.getOrPut(g.id!!) { other }
    }

    val buckets = Array(bits()) { LinkedHashMap<T, Route<T>>() }

}