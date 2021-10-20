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

/**
 * https://stackoverflow.com/a/48924178
 *
 * Here are the safe and portable versions of the zig zag mappings for 64b integers in C (note the arithmetic negation):
 *
 * #include <stdint.h>
 *
 * uint64_t zz_map( int64_t x )
 * {
 * return ( ( uint64_t ) x << 1 ) ^ -( ( uint64_t ) x >> 63 );
 * }
 *
 * int64_t zz_unmap( uint64_t y )
 * {
 * return ( int64_t ) ( ( y >> 1 ) ^ -( y & 0x1 ) );
 * }*/
inline fun zz_map(x: Long): ULong = ((x.toULong()) shl 1) xor (-((x.toULong()) shr 63).toLong()).toULong()
inline fun zz_unmap(y: ULong): Long = ((y shr 1) xor ((-(y and 1.toULong()).toLong()).toULong())).toLong()