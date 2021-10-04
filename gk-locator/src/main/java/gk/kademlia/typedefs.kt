@file:Suppress("UNCHECKED_CAST") @file:OptIn(ExperimentalUnsignedTypes::class, ExperimentalStdlibApi::class)

package gk.kademlia

import java.net.URI

typealias Address = URI
typealias Api = (Any?) -> Any?
typealias ApiKey = String
typealias Bucket<Sz, T> = LinkedHashMap<GUID<Sz, T>, Route<Sz, T>?>
typealias Envelope<Sz, T> = List<GUID<Sz, T>>
typealias Payload = Any?
