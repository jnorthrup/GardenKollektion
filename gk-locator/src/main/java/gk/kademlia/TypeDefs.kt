@file:Suppress("UNCHECKED_CAST") @file:OptIn(ExperimentalUnsignedTypes::class, ExperimentalStdlibApi::class)

package gk.kademlia

import gk.kademlia.id.NUID
import vec.macros.Pai2
import java.net.URI

typealias Address = URI
typealias Api = (Any?) -> Any?
typealias ApiKey = String
typealias   Route<T> = Pai2<NUID<T>, Address>