package gk.kademlia.id

import gk.kademlia.NetMask
import gk.kademlia.id.impl.UByteNUID

class WorkerNUID(id: UByte? = null) : UByteNUID(id) {
    override val netmask = NetMask.Companion.warmSz
}