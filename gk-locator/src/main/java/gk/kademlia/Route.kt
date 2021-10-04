package gk.kademlia



open class Route<Sz : AddressSize,T>(    var guid: GUID<Sz,T> ? = null,  ) {
    val nSize get() =  guid?.sz

    lateinit var address: Address
}