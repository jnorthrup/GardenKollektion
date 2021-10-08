package gk.kademlia.bitops.impl

import gk.kademlia.bitops.BitOps

object UlongBitOps : BitOps<ULong> {
    override val one: ULong get() = 1.toULong()
    override val xor: (ULong, ULong) -> ULong get() = ULong::xor
    override val and: (ULong, ULong) -> ULong get() = ULong::and
    override val shl: (ULong, Int) -> ULong get() = ULong::shl
    override val shr: (ULong, Int) -> ULong get() = ULong::shr
    override val plus: (ULong, ULong) -> ULong get() = ULong::plus
    override val minus: (ULong, ULong) -> ULong get() = ULong::minus
}