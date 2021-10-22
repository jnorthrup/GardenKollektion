package gk.kademlia.id.impl

import gk.kademlia.bitops.impl.BigIntOps
import gk.kademlia.id.NUID
import gk.kademlia.net.NetMask
import java.math.BigInteger

abstract class BigIntegerNUID<Sz : NetMask<BigInteger>>(override var id: BigInteger? = null) : NUID<BigInteger> {
    override val ops = BigIntOps
}