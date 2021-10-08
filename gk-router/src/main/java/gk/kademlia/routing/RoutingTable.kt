package gk.kademlia.routing
import gk.kademlia.NetworkSize
import gk.kademlia.include.Route
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
    val fogOfWar: Int? = null,
) {
    val bits get() = agentNUID.bits
    private val bitOps = agentNUID.ops

    /**
     * contract is to have the route guid id fully realized in agent first
     */
    fun addRoute(other: Route<T>) = other.let { (g) ->
        var res: Route<T>? = null
        val origDistance = bits.distance(bitOps, agentNUID.id!!, g.id!!)
        if (origDistance > 0) {
            fogOfWar?.let {
                val width = bits() - (origDistance + fogOfWar)
                val x = bits() - width - fogOfWar
                val buk = x - 1
                if (x in (0..buckets.size)) {
                    val buc = buckets[buk]
                    val cap = 1.shl(width)
                    if (buc.size < cap)
                        res = buc.getOrPut(g.id!!, other.`⟲`)
                }
            }?:let {
            res=    buckets[origDistance.dec()] .getOrPut(g.id!!, other.`⟲`)
            }


        }
        res
    }
    fun rmRoute(other: Route<T>) = other.let { (g) ->
        var res: Route<T>? = null
        val origDistance = bits.distance(bitOps, agentNUID.id!!, g.id!!)
        if (origDistance > 0) {
            fogOfWar?.let {
                val width = bits() - (origDistance + fogOfWar)
                val x = bits() - width - fogOfWar
                val buk = x - 1
                if (x in (0..buckets.size)) {
                    val buc = buckets[buk]
                    val cap = 1.shl(width)
                    if (buc.isNotEmpty())
                        res = buc.remove(g.id!!)
                }
            }?:let {
                res=buckets.takeIf { it.isNotEmpty() }?.get (origDistance.dec())?.remove(g.id!! )
            }
        }
        res
    }


    val buckets = Array(bits() -( fogOfWar?:0)) { LinkedHashMap<T, Route<T>>() }

}