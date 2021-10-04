package gk.kademlia

import vec.macros.Pai2

class Message(override val first: ApiKey, override val second: Any?) : Pai2<ApiKey, Any?>