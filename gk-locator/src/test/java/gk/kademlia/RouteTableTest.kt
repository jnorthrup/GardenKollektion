package gk.kademlia

import gk.kademlia.agent.Agent
import gk.kademlia.id.WorkerNUID
import gk.kademlia.net.warmSz
import junit.framework.Assert.assertEquals
import org.junit.Test
import vec.macros.t2
import vec.util.debug
import java.net.URI


class RouteTableTest {
    val nuid = WorkerNUID(0u)
    val nuid1 = WorkerNUID(with(nuid) { ops.xor(nuid.capacity, id!!) })
    val d_ones = UByteArray(nuid.bits()) { with(nuid.ops) { shl(one, it) } }
    val upper = d_ones.drop(1)
    val d_twos = UByteArray(upper.size) { x -> with(nuid.ops) { xor(one, upper[x]) } }
    val d_twos_point_one = UByteArray(upper.size) { x -> with(nuid.ops) { xor(shl(one, x), upper[x]) } }

    val agent = object : Agent<warmSz, UByte> {
        override val NUID = nuid
        override val routingTable = object : RoutingTable<warmSz, UByte>(NUID) {}
        override val send: Api = { any -> any }
        override val recv: Api = { any -> any }
    }

    @Test
    fun testRouteAdd() {
        agent.run {
            routingTable.addRoute(nuid1 t2 URI("urn:null"))
            for (dOne in d_ones) routingTable.addRoute(WorkerNUID(dOne) t2 URI("urn:$dOne@net"))
            for (dOne in d_twos) routingTable.addRoute(WorkerNUID(dOne) t2 URI("urn:$dOne@net"))
            for (dOne in d_twos_point_one) routingTable.addRoute(WorkerNUID(dOne) t2 URI("urn:$dOne@net"))

            assertEquals(routingTable.buckets[0].size, 7)
            assertEquals(routingTable.buckets[1].size, 11)
            assertEquals(routingTable.buckets[6].size, 1)
            debug { }
        }
    }
}