package gk.kademlia.bitops.impl

import gk.kademlia.bitops.BitOps
import kotlin.experimental.and
import kotlin.experimental.xor

object UByteBitOps : BitOps<UByte> {
    override val one: UByte get() = 1.toUByte()
    override val xor: (UByte, UByte) -> UByte get() = UByte::xor
    override val and: (UByte, UByte) -> UByte get() = UByte::and
    override val shl: (UByte, Int) -> UByte get() = { uByte: UByte, i: Int -> uByte.toUInt().shl(i).toUByte() }
    override val shr: (UByte, Int) -> UByte get() = { uByte: UByte, i: Int -> uByte.toUInt().shr(i).toUByte() }
    override val plus: (UByte, UByte) -> UByte get() = { a, b -> (a + b).toUByte() }
    override val minus: (UByte, UByte) -> UByte get() = { a, b -> (a - b).toUByte() }
}

object ByteBitOps : BitOps<Byte> {
    override val one: Byte get() = 1.toByte()
    override val xor: (Byte, Byte) -> Byte get() = Byte::xor
    override val and: (Byte, Byte) -> Byte get() = Byte::and
    override val shl: (Byte, Int) -> Byte get() = { Byte: Byte, i: Int -> Byte.toInt().shl(i).toByte() }
    override val shr: (Byte, Int) -> Byte get() = { Byte: Byte, i: Int -> Byte.toInt().shr(i).toByte() }
    override val plus: (Byte, Byte) -> Byte get() = { a, b -> (a + b).toByte() }
    override val minus: (Byte, Byte) -> Byte get() = { a, b -> (a - b).toByte() }
}