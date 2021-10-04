@file:Suppress("UNCHECKED_CAST")

package gk.kademlia

import vec.macros.Pai2
import vec.macros.`⟲`
import java.net.URI

typealias  AddressSize = () -> Int

interface Distance<T> {
    infix fun <T> Distance<T>.xor(other: T): T
}

typealias Address=URI
typealias Bucket<Sz> = LinkedHashMap<GUID<Sz>, Route<Sz>?>

typealias Payload = Any?
/**
 * https://youtu.be/7o0pfKDq9KE?t=869
 *  * data structure is K-V based like a map
 *  * Key is usually a hash of value
 *  * Hash Key to GUID space to choose node to store data
 *
 * ex:
 *
 * value: "Hello World"
 * Key<8>: Hash(value)  ((Sz ^2)-1) - 8 bitHash
 * Node<3> to Store: Hash(Key):15 - ((Sz ^2)-1) - 3 bit hash
 *
 */

interface GUID<Sz : AddressSize> : Distance<GUID<Sz>>{
    companion object
    {
        /**
         * for when n=3 is handy, spawn a new namespace with the first/best 3 nodes to volunteer.
         */
        val size2_election:AddressSize = 3.`⟲` as AddressSize

        /**
         * this node count probably out lives most single grenade detonations.
         */
        val size7_127:AddressSize = 7.`⟲` as AddressSize

        /**
         * for federating data, you want an unbounded DHT full of volunteers.
         */
        val size64_global :AddressSize= 64.`⟲` as AddressSize
    }
}


typealias  Envelope<Sz> = List<GUID<Sz>>
typealias ApiKey = String
class Message(override val first: ApiKey, override val second: Any?) : Pai2<ApiKey, Any?>


typealias  Api = (Any?) -> Any?

/**
 *
https://github.com/ep2p/kademlia-api
 */
enum class AgentApiKey {

    /**
     * https://youtu.be/7o0pfKDq9KE?t=252
    giveMeAListOfNodesThatArecloseToMe  */
    routesUpdate,

    /**
     * https://youtu.be/7o0pfKDq9KE?t=355
     */
    ping,

    /**
     * https://youtu.be/7o0pfKDq9KE?t=1010
     */
    copyDataToOtherNodes,

    /**
     * https://youtu.be/7o0pfKDq9KE?t=1010
     */
    moveDataToClosestNodesDuringShutdown,

    /**
     * https://youtu.be/7o0pfKDq9KE?t=1010
     */
    askClosestNodesForDataToStore,

    /**
     * node x joins this agent.
     * this agent sends an unused guid or error message.
     *
     *
     */
    join
}




