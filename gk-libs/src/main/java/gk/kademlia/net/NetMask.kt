@file:OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)

package gk.kademlia.net

import gk.kademlia.bitops.BitOps

interface NetMask<P : Comparable<P>> {

    /**
     * Kademlia specifies a bit "count"
     */
    val bits: Int

    /**math functions*/
    val ops: BitOps<P> get() = BitOps.minOps<P>(bits.toUInt()) as BitOps<P>

    /**one function
     * satu=indonesian 1
     */
    val satu: P get() = ops.one

    /**
     * IP networks have NetMasks.  netmasks with kademlia don't change the routing factors.
     * by default the mask is all open
     */
    val mask: P
        get() = ops.run {
            var acc = xor(satu, satu)
            repeat(bits.toInt()) { x ->
                acc = shl(satu, x)
                acc = xor(satu, acc)
            }
            acc
        }

    fun distance(p1: P, p2: P) = ops.run {
        val xor1 = xor(p1, p2)
        (0 until bits.toInt()).fold(0) { acc, i ->
            if (one == and(one, shr(xor1, i)))
                acc.inc() else acc
        }.toInt()
    }

    companion object {
        /**
         * for federating data, you want an unbounded DHT full of volunteers.
         */
        object coolSz : NetMask<ULong> {
            override val bits = 64
        }

        /**
         * this node count probably out lives most single grenade detonations.
         */
        object warmSz : NetMask<Byte> {
            override val bits = 7
        }


        /**
         * for when n=3 is handy, spawn a new namespace with the first/best 3 nodes to volunteer.
         */
        object hotSz : NetMask<Byte> {
            override val bits = 2
        }
    }
}

operator fun NetMask<*>.invoke() = this.bits