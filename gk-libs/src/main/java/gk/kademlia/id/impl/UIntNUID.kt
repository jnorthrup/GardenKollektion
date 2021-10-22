package gk.kademlia.id.impl

import gk.kademlia.bitops.impl.UIntBitOps
import gk.kademlia.id.NUID
import gk.kademlia.net.NetMask

abstract class UIntNUID<Sz : NetMask<UInt>>(override var id: UInt? = null) : NUID<UInt> {

    override val ops = UIntBitOps
}