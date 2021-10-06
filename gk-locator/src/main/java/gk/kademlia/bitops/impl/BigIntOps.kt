package gk.kademlia.bitops.impl

import gk.kademlia.bitops.BitOps
import java.math.BigInteger

object BigIntOps : BitOps<BigInteger> {
    override val one: BigInteger get() = BigInteger.ONE
    override val xor: (BigInteger, BigInteger) -> BigInteger get() = BigInteger::xor
    override val and: (BigInteger, BigInteger) -> BigInteger get() = BigInteger::xor
    override val shl: (BigInteger, Int) -> BigInteger get() = BigInteger::shl
    override val shr: (BigInteger, Int) -> BigInteger get() = BigInteger::shr
    override val plus: (BigInteger, BigInteger) -> BigInteger get() = BigInteger::plus
    override val minus: (BigInteger, BigInteger) -> BigInteger get() = BigInteger::minus
}