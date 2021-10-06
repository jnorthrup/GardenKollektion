package gk.kademlia.id

import gk.kademlia.NetworkSize
import gk.kademlia.id.impl.UByteNUID

class WorkerNUID(id: UByte? = null) : UByteNUID(id) {
    override val bits = NetworkSize.Companion.warmSz
}