package gk.kademlia.id.impl

import gk.kademlia.NetworkSize
import gk.kademlia.bitops.impl.BigIntOps
import gk.kademlia.id.NUID
import java.math.BigInteger

abstract class BigIntegerNUID<Sz : NetworkSize>(override var id: BigInteger? = null) : NUID<BigInteger> {
    override val ops = BigIntOps
}