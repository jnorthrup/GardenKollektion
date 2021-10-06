package gk.kademlia.bitops.impl

import gk.kademlia.bitops.BitOps

object UByteBitOps : BitOps<UByte> {
    override val one: UByte get() = 1.toUByte()
    override val xor: (UByte, UByte) -> UByte get() = UByte::xor
    override val and: (UByte, UByte) -> UByte get() = UByte::and
    override val shl: (UByte, Int) -> UByte get() = { uByte: UByte, i: Int -> uByte.toUInt().shl(i).toUByte() }
    override val shr: (UByte, Int) -> UByte get() = { uByte: UByte, i: Int -> uByte.toUInt().shr(i).toUByte() }
    override val plus: (UByte, UByte) -> UByte get() = { a, b -> (a + b).toUByte() }
    override val minus: (UByte, UByte) -> UByte get() = { a, b -> (a - b).toUByte() }
}