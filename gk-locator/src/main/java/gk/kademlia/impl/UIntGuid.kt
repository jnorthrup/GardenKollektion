package gk.kademlia.impl

import gk.kademlia.GUID
import gk.kademlia.NetworkSize
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.asKotlinRandom

abstract class UIntGuid<Sz : NetworkSize>(override var id: UInt?) : GUID<Sz, UInt> {

    override fun random(bucket: Int?) = (ThreadLocalRandom.current().asKotlinRandom().nextBits(sz())).toUInt()
    override val one: UInt get() = 1.toUInt()
    override val xor: (UInt, UInt) -> UInt get() = UInt::xor
    override val and: (UInt, UInt) -> UInt get() = UInt::xor
    override val shl: (UInt,  Int) -> UInt get() = UInt::shl
    override val shr: (UInt,  Int) -> UInt get() = UInt::shr
    override val plus: (UInt, UInt) -> UInt get() = UInt::plus
    override val minus: (UInt, UInt) -> UInt get() = UInt::minus
}

