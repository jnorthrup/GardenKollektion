package gk.kademlia.id.impl

import gk.kademlia.bitops.impl.ULongBitOps
import gk.kademlia.id.NUID
import gk.kademlia.net.NetMask

abstract class ULongNUID<Sz : NetMask<ULong>>(override var id: ULong? = null) : NUID<ULong> {
    override val ops = ULongBitOps
}