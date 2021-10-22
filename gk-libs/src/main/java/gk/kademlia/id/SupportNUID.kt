package gk.kademlia.id

import gk.kademlia.id.impl.ULongNUID
import gk.kademlia.net.NetMask

class SupportNUID(override var id: ULong? = null) : ULongNUID<NetMask.Companion.coolSz>(id) {
    override val netmask = NetMask.Companion.coolSz
}