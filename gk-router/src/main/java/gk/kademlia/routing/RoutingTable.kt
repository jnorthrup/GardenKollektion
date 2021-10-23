package gk.kademlia.routing

import gk.kademlia.id.NUID
import gk.kademlia.include.Address
import gk.kademlia.include.Route
import gk.kademlia.net.NetMask
import vec.macros.Pai2
import vec.macros.`⟲`


/**
 * once an agent knows its network id it can create a routeTable, the agent will also be
 * responsible for assigning new GUIDS on all routes
 * before touching the route table.
 *
 */
open class RoutingTable<TNum : Comparable<TNum>, Sz : NetMask<TNum>>(
    val agentNUID: NUID<TNum>,
) {
    private val bitOps = agentNUID.ops

    /**
     * contract is to have the route guid id fully realized in agent first
     */
    fun addRoute(other: Route<TNum>): Pai2<NUID<TNum>, Address>? = other.let { (g: NUID<TNum>) ->
        var res: Route<TNum>? = null
        val origDistance = agentNUID.netmask.distance(agentNUID.id!!, g.id!!)
        if (origDistance > 0) res = buckets[origDistance.dec()].getOrPut(g.id!!, other.`⟲`)
        res
    }

    fun rmRoute(other: Route<TNum>): Pai2<NUID<TNum>, Address>? = other.let { (g) ->
        var res: Route<TNum>? = null
        val origDistance = agentNUID.netmask.distance(agentNUID.id!!, g.id!!)
        if (origDistance > 0) res = buckets.takeIf { it.isNotEmpty() }?.get(origDistance.dec())?.remove(g.id!!);
        res
    }


    val buckets: Array<LinkedHashMap<TNum, Pai2<NUID<TNum>, Address /* = java.net.URI */>>> =
        Array(agentNUID.netmask.bits) { LinkedHashMap() }


}