@file:Suppress("UNCHECKED_CAST") @file:OptIn(ExperimentalUnsignedTypes::class, ExperimentalStdlibApi::class)

package gk.kademlia.include

import gk.kademlia.id.NUID
import vec.macros.Pai2
import java.net.URI

typealias   Address = URI
typealias   Route<TNum> = Pai2<NUID<TNum>, Address>