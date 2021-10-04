package gk.kademlia

open class Route<Sz : AddressSize>(val nSize: Sz = GUID.size64_global as Sz) {
    var guid: GUID<Sz>? = null
}