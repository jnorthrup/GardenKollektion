package gk.kademlia.bitops

import gk.kademlia.bitops.impl.*

interface BitOps<Primitive : Comparable<Primitive>> {
    val one: Primitive
    val shl: (Primitive, Int) -> Primitive
    val shr: (Primitive, Int) -> Primitive
    val xor: (Primitive, Primitive) -> Primitive
    val and: (Primitive, Primitive) -> Primitive
    val plus: (Primitive, Primitive) -> Primitive
    val minus: (Primitive, Primitive) -> Primitive
    fun toNumber(x: Primitive): Number = x.let {
        when (one) {
            is Number -> it as Number
            is UByte -> (it as UByte).toInt()
            is UShort -> (it as UShort).toInt()
            is UInt -> (it as UInt).toLong()
            else -> (it).toString().toBigInteger()
        }
    }

    companion object {
        /**
         * minimum bitops types for the intended bitcount of NUID
         */
        fun <Primitive : Comparable<Primitive>> minOps(size: UInt) =
            when (size) {
                in UInt.MIN_VALUE..7u -> ByteBitOps
                8u -> UByteBitOps
                in 9u..15u -> ShortBitOps
                16u -> UShortBitOps
                in 17u..31u -> IntBitOps
                32u -> UIntBitOps
                in 33u..63u -> LongBitOps
                64u -> ULongBitOps
                else -> BigIntOps
            } as BitOps<Primitive>
    }
}