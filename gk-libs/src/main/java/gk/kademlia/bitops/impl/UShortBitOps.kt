package gk.kademlia.bitops.impl

import gk.kademlia.bitops.BitOps
import kotlin.experimental.and
import kotlin.experimental.xor

object UShortBitOps : BitOps<UShort> {
    override val one: UShort get() = 1.toUShort()
    override val xor: (UShort, UShort) -> UShort get() = UShort::xor
    override val and: (UShort, UShort) -> UShort get() = UShort::and
    override val shl: (UShort, Int) -> UShort get() = { uShort: UShort, i: Int -> uShort.toUInt().shl(i).toUShort() }
    override val shr: (UShort, Int) -> UShort get() = { uShort: UShort, i: Int -> uShort.toUInt().shr(i).toUShort() }
    override val plus: (UShort, UShort) -> UShort get() = { a, b -> (a + b).toUShort() }
    override val minus: (UShort, UShort) -> UShort get() = { a, b -> (a - b).toUShort() }
}

object ShortBitOps : BitOps<Short> {
    override val one: Short get() = 1.toShort()
    override val xor: (Short, Short) -> Short get() = Short::xor
    override val and: (Short, Short) -> Short get() = Short::and
    override val shl: (Short, Int) -> Short get() = { Short: Short, i: Int -> Short.toUInt().shl(i).toShort() }
    override val shr: (Short, Int) -> Short get() = { Short: Short, i: Int -> Short.toUInt().shr(i).toShort() }
    override val plus: (Short, Short) -> Short get() = { a, b -> (a + b).toShort() }
    override val minus: (Short, Short) -> Short get() = { a, b -> (a - b).toShort() }
}