@file:Suppress("UNCHECKED_CAST")

package gk.kademlia

import vec.macros.Pai2
import vec.macros.`⟲`

abstract class Agent<Sz : AddressSize>(val nSize: Sz = 64.`⟲` as Sz) {
    val routingTable: List<Bucket<Sz>> = List<Bucket<Sz>>(nSize()) { Bucket() }

    /**bulk send 1 or more messages */
    abstract fun egress(envelope:Envelope<Sz>, vararg msg:Message)

    /**
     * bulk reroute
     */
    abstract fun ingress(envelope:Envelope<Sz>, vararg msg:Message)

}