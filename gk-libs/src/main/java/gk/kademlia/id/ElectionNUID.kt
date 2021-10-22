package gk.kademlia.id

import gk.kademlia.NetMask
import gk.kademlia.id.impl.UByteNUID

class ElectionNUID(id: UByte? = null) : UByteNUID(id) {
    override val netmask = NetMask.Companion.hotSz
}