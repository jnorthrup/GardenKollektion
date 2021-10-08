package gk.kademlia.bitops.impl

import gk.kademlia.bitops.BitOps

object UIntBitOps : BitOps<UInt> {
    override val one: UInt get() = 1.toUInt()
    override val xor: (UInt, UInt) -> UInt get() = UInt::xor
    override val and: (UInt, UInt) -> UInt get() = UInt::and
    override val shl: (UInt, Int) -> UInt get() = UInt::shl
    override val shr: (UInt, Int) -> UInt get() = UInt::shr
    override val plus: (UInt, UInt) -> UInt get() = UInt::plus
    override val minus: (UInt, UInt) -> UInt get() = UInt::minus
}