@file:OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)

package gk.kademlia

import gk.kademlia.bitops.BitOps

interface NetworkSize {
    operator fun invoke(): Int
    fun <P : Comparable<P>> distance(ops: BitOps<P>, p1: P, p2: P): Int {
        with(ops) {
            var distance = 0
            val xor1 = xor(p1, p2)
            for (c in 0 until invoke())
                if (one == and(one, shr(xor1, c)))
                    distance++
            return distance
        }
    }

    companion object {
        /**
         * for federating data, you want an unbounded DHT full of volunteers.
         */
        object coolSz : NetworkSize {
            override fun invoke(): Int = 64
        }

        /**
         * this node count probably out lives most single grenade detonations.
         */
        object warmSz : NetworkSize {
            override fun invoke(): Int = 7
        }


        /**
         * for when n=3 is handy, spawn a new namespace with the first/best 3 nodes to volunteer.
         */
        object hotSz : NetworkSize {
            override fun invoke(): Int = 2
        }
    }
}

