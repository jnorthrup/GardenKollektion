package gk.kademlia.id

import gk.kademlia.NetMask
import gk.kademlia.id.impl.ULongNUID

class SupportNUID(override var id: ULong? = null) : ULongNUID<NetMask.Companion.coolSz>(id) {
    override val netmask = NetMask.Companion.coolSz
}