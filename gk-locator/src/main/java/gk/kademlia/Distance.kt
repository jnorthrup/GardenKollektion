package gk.kademlia

interface Distance<T,Primitive> {
    infix  fun  xor(other: T): T
    val randomGuid:Distance<T,Primitive>
    operator fun invoke():Primitive
}