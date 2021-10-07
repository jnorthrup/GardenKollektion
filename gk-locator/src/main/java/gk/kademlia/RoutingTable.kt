package gk.kademlia

import gk.kademlia.id.NUID
import vec.macros.`⟲`


/**
 * once an agent knows its network id it can create a routeTable, the agent will also be
 * responsible for assigning new GUIDS on all routes
 * before touching the route table.
 *
 */
open class RoutingTable<Sz : NetworkSize, T : Comparable<T>>(
    val agentNUID: NUID<T>,
    /**
     * each unit of the "fog" halves the bucket capacity and distance that will be added.
     */
    private val fogOfWar: Int = 0,
) {
    val bits get() = agentNUID.bits
    private val bitOps = agentNUID.ops

    /**
     * contract is to have the route guid id fully realized in agent first
     */
    fun addRoute(other: Route<T>) = other.let { (g) ->
        var res: Route<T>? = null
        val orig = bits.distance(bitOps, agentNUID.id!!, g.id!!)
        if (orig> 0) {
            val width = bits() - (orig + fogOfWar)
            val x = bits() - width - fogOfWar
            val buk = x - 1
            if (x in (0..buckets.size)) {
                val buc = buckets[buk]
                val cap = 1.shl(width)
                if (buc.size < cap) res = buc.getOrPut(g.id!!, other.`⟲`)

            }
        }
        res
    }


    val buckets = Array(bits() - fogOfWar) { LinkedHashMap<T, Route<T>>() }

}