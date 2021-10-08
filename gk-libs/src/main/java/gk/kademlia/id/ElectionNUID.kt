package gk.kademlia.id

import gk.kademlia.NetworkSize
import gk.kademlia.id.impl.UByteNUID

class ElectionNUID(id: UByte? = null) : UByteNUID(id) {
    override val bits = NetworkSize.Companion.hotSz
}