package gk.kademlia.id

import gk.kademlia.NetworkSize
import gk.kademlia.id.impl.ULongNUID

class SupportNUID(override var id: ULong? = null) : ULongNUID<NetworkSize.Companion.coolSz>(id) {
    override val bits = NetworkSize.Companion.coolSz
}