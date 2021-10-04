@file:OptIn(ExperimentalStdlibApi::class,ExperimentalUnsignedTypes::class)

package gk.kademlia

interface AddressSize : () -> Int {
    val zones: ULongArray get() = ULongArray(invoke()) { 1.toULong().rotateLeft(it) }
}