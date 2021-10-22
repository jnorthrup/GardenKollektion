package gk.kademlia.bitops.impl

import gk.kademlia.bitops.BitOps

object ULongBitOps : BitOps<ULong> {
    override val one: ULong get() = 1.toULong()
    override val xor: (ULong, ULong) -> ULong get() = ULong::xor
    override val and: (ULong, ULong) -> ULong get() = ULong::and
    override val shl: (ULong, Int) -> ULong get() = ULong::shl
    override val shr: (ULong, Int) -> ULong get() = ULong::shr
    override val plus: (ULong, ULong) -> ULong get() = ULong::plus
    override val minus: (ULong, ULong) -> ULong get() = ULong::minus
}

object LongBitOps : BitOps<Long> {
    override val one: Long get() = 1.toLong()
    override val xor: (Long, Long) -> Long get() = Long::xor
    override val and: (Long, Long) -> Long get() = Long::and
    override val shl: (Long, Int) -> Long get() = Long::shl
    override val shr: (Long, Int) -> Long get() = Long::shr
    override val plus: (Long, Long) -> Long get() = Long::plus
    override val minus: (Long, Long) -> Long get() = Long::minus
}