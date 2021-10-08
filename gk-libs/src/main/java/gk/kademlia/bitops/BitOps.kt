package gk.kademlia.bitops

interface BitOps<Primitive : Comparable<Primitive>> {
    val xor: (Primitive, Primitive) -> Primitive
    val and: (Primitive, Primitive) -> Primitive
    val one: Primitive
    val shl: (Primitive, Int) -> Primitive
    val shr: (Primitive, Int) -> Primitive
    val plus: (Primitive, Primitive) -> Primitive
    val minus: (Primitive, Primitive) -> Primitive
}