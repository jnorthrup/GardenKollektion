package gk.kademlia.routing

import gk.kademlia.id.NUID
import gk.kademlia.include.Address
import gk.kademlia.include.Route
import gk.kademlia.net.NetMask
import vec.macros.Pai2
import vec.macros.`⟲`
import java.lang.Integer.min


/**
 * once an agent knows its network id it can create a routeTable, the agent will also be
 * responsible for assigning new GUIDS on all routes
 * before touching the route table.
 *
 */
open class RoutingTable<TNum : Comparable<TNum>, Sz : NetMask<TNum>>(
    val agentNUID: NUID<TNum>, val optimal: Boolean = false,
) {
    private val bitOps = agentNUID.ops

    /**
     * contract is to have the route guid id fully realized in agent first
     */
    fun addRoute(other: Route<TNum>): Pai2<NUID<TNum>, Address>? = other.let { (g: NUID<TNum>) ->
        var res: Route<TNum>? = null
        val origDistance = min(agentNUID.netmask.distance(agentNUID.id!!, g.id!!), bucketCount)
        if (origDistance > 0) res = buckets[origDistance.dec()].getOrPut(g.id!!, other.`⟲`)
        res
    }

    fun rmRoute(other: Route<TNum>): Pai2<NUID<TNum>, Address>? = other.let { (g) ->
        var res: Route<TNum>? = null
        val origDistance = agentNUID.netmask.distance(agentNUID.id!!, g.id!!)
        if (origDistance > 0) res = buckets.takeIf { it.isNotEmpty() }?.get(origDistance.dec())?.remove(g.id!!)
        res
    }


    private fun bucketFor(g: NUID<TNum>): Int =
        min(agentNUID.netmask.distance(agentNUID.id!!, g.id!!), bucketCount).dec()

    open val bucketCount: Int = agentNUID.netmask.bits.let { if (optimal) it else it / 2 + 1 }
    open val bucketSize: Int = agentNUID.netmask.bits.let { if (optimal) it else it / 2 + 1 }

    val buckets: Array<MutableMap<TNum, Route<TNum>>> = Array(bucketCount) { linkedMapOf() }

}

/*
fun <S, F : () -> S> create(code: () -> S): S {
    println("string")
    return code()
}

fun <S : Unit, F : () -> S> create(code: F): S {
    println("unit")
    return code.invoke()
}

fun main() {
    create { "a" } // prints unit
    val a = { "a" }
    create(a) // prints string
}*/