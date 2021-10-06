package gk.kademlia.id.impl

import gk.kademlia.NetworkSize
import gk.kademlia.bitops.impl.UlongBitOps
import gk.kademlia.id.NUID

abstract class ULongNUID<Sz : NetworkSize>(override var id: ULong? = null) : NUID<ULong> {
    override val ops = UlongBitOps
}


