package gk.kademlia.id

import gk.kademlia.NetworkSize
import gk.kademlia.bitops.BitOps
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
    val bits: NetworkSize
    val ops: BitOps<Primitive>

    fun random(distance: Int? = null, centroid: Primitive = id!!) = ops.run {
        ThreadLocalRandom.current().asKotlinRandom().run {
            var accum = centroid
            (distance?.takeIf { it <= bits() } ?: nextInt(bits())).let { distance ->
                linkedSetOf<Int>().apply {
                    while (size < distance) add(nextInt(until = bits()))
                }
            }.sorted().forEach {
                accum = xor(accum, shl(one, it))
            }
            accum
        }
    }

    val capacity: Primitive get() = with(ops) { minus(shl(one, bits()), one) }
    fun assign(it: Primitive) {
        if (id != null) id.run { throw RuntimeException("GUID assigned twice for $id") }
        id = it
    }
}