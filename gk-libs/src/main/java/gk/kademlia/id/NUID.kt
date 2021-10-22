package gk.kademlia.id

import gk.kademlia.bitops.BitOps
import gk.kademlia.net.NetMask
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.asKotlinRandom

/**
 * Network Unique ID
 *
 * network IDs within larger networks within larger networks
 *
 */

interface NUID<Primitive : Comparable<Primitive>> {
    var id: Primitive?
    val netmask: NetMask<Primitive>
    val ops: BitOps<Primitive>

    fun random(distance: Int? = null, centroid: Primitive = id!!) = ops.run {
        ThreadLocalRandom.current().asKotlinRandom().run {
            var accum = centroid
            val uBits = netmask.bits
            (distance?.takeIf { it <= uBits } ?: nextInt(uBits)).let { distance ->
                linkedSetOf<Int>().apply {
                    while (size < distance) add(nextInt(uBits))
                }
            }.sorted().forEach {
                accum = xor(accum, shl(one, it))
            }
            accum
        }
    }

    val capacity: Primitive get() = with(ops) { xor(netmask.mask, minus(shl(one, netmask.bits), one)) }
    fun assign(it: Primitive) {
        if (id != null)
            id.run { throw RuntimeException("GUID assigned twice for $id") }
        id = it
    }
}