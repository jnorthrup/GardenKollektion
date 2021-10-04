package gk.kademlia

import vec.macros.`⟲`
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.asKotlinRandom
import kotlin.random.nextULong

/**
 * https://youtu.be/7o0pfKDq9KE?t=869
 *  * data structure is K-V based like a map
 *  * Key is usually a hash of value
 *  * Hash Key to GUID space to choose node to store data
 */

interface GUID<Sz : AddressSize, T> : Distance<GUID<Sz, T>, T> {
    val sz: Sz

    companion object {
        /**
         * for when n=3 is handy, spawn a new namespace with the first/best 3 nodes to volunteer.
         */
        object hotSz : AddressSize by 3.`⟲` as AddressSize
        class ElectionGUID(var bits: Int = ThreadLocalRandom.current().asKotlinRandom().nextBits(3)) :
            GUID<hotSz, Int> {

            override fun xor(other: GUID<hotSz, Int>): GUID<hotSz, Int> =
                ElectionGUID(this() xor (other()))

            override val randomGuid: Distance<GUID<hotSz, Int>, Int>
                get() = ElectionGUID(ThreadLocalRandom.current().asKotlinRandom().nextBits(3))

            override val sz: hotSz get() = hotSz
            override fun invoke(): Int = bits
        }

        /**
         * this node count probably out lives most single grenade detonations.
         */
        object warmSz : AddressSize by 7.`⟲` as AddressSize
        class WorkerGUID(var bits: Int?) :
            GUID<warmSz, Int> {

            override fun xor(other: GUID<warmSz, Int>) =
                WorkerGUID(this() xor (other()))

            override val randomGuid: Distance<GUID<warmSz, Int>, Int>
                get() = WorkerGUID(ThreadLocalRandom.current().asKotlinRandom().nextBits(7))
            override val sz get() = warmSz
            override fun invoke(): Int = bits!!
        }

        /**
         * for federating data, you want an unbounded DHT full of volunteers.
         */
        object coolSz : AddressSize by 64.`⟲` as AddressSize
        class SupportGUID(var bits: ULong?) :
            GUID<coolSz, ULong> {
            override fun xor(other: GUID<coolSz, ULong>) =
                SupportGUID(this() xor (other()))

            override val randomGuid: Distance<GUID<coolSz, ULong>, ULong>
                get() = SupportGUID(ThreadLocalRandom.current().asKotlinRandom().nextULong())
            override val sz get() = coolSz
            override fun invoke(): ULong = bits!!
        }
    }
}