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
    /**
     * each unit of the "fog" halves the bucket capacity and distance that will be added.
     */
    val fogOfWar: Int? = null,
) {

    private val bitOps = agentNUID.ops

    /**
     * contract is to have the route guid id fully realized in agent first
     */
    fun addRoute(other: Route<TNum>) = other.let { (g: NUID<TNum>) ->
        var res: Route<TNum>? = null
        val origDistance = agentNUID.netmask.distance(agentNUID.id!!, g.id!!)
        if (origDistance > 0) {
            fogOfWar?.let {
                val width = agentNUID.netmask.bits - (origDistance + fogOfWar)
                val x = (agentNUID.netmask.bits - width - fogOfWar).toInt()
                val buk = x - 1
                if (x in (0..buckets.size)) {
                    val buc = buckets[buk.toInt()]
                    val cap = agentNUID.netmask.run {
                        ops.run {
                            var x = shl(one, bits.toInt())
                            x = minus(x, one)
                            x = xor(x, mask)
                            x
                        }
                    }
                    if (buc.size.toInt() < bitOps.toNumber(cap).toInt())
                        res = buc.getOrPut(g.id!!, other.`⟲`)
                }
            } ?: let {
                res = buckets[origDistance.dec().toInt()].getOrPut(g.id!!, other.`⟲`)
            }
        }
        res
    }

    fun rmRoute(other: Route<TNum>) = other.let { (g) ->
        var res: Route<TNum>? = null
        val origDistance = agentNUID.netmask.distance(agentNUID.id!!, g.id!!)
        if (origDistance > 0) {
            fogOfWar?.let {
                val width = agentNUID.netmask.bits - (origDistance + fogOfWar)
                val x = agentNUID.netmask.bits - width - fogOfWar
                val buk = x - 1
                if (x in (0..buckets.size)) {
                    val buc = buckets[buk]
                    val cap = 1.shl(width)
                    if (buc.isNotEmpty())
                        res = buc.remove(g.id!!)
                }
            } ?: let {
                res = buckets.takeIf { it.isNotEmpty() }?.get(origDistance.dec())?.remove(g.id!!)
            }
        }
        res
    }


    val buckets: Array<LinkedHashMap<TNum, Pai2<NUID<TNum>, Address /* = java.net.URI */>>> =
        Array(agentNUID.netmask.bits - (fogOfWar ?: 0)) { LinkedHashMap() }

}