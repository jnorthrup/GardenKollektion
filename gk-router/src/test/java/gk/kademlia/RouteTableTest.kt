@file:OptIn(ExperimentalUnsignedTypes::class)

package gk.kademlia

import cursors.io.FibonacciReporter
import gk.kademlia.id.WorkerNUID
import gk.kademlia.routing.RoutingTable
import org.junit.*
import vec.macros.t2
import vec.util.debug
import vec.util.logDebug
import java.net.URI
import kotlin.test.assertEquals


class RouteTableTest {
    val nuid = WorkerNUID(0u)
    val nuid1 = WorkerNUID(with(nuid) { ops.xor(nuid.capacity, id!!) })
    val d_ones = UByteArray(nuid.netmask()) { with(nuid.ops) { shl(one, it) } }
    val upper = d_ones.drop(1)
    val d_twos = UByteArray(upper.size) { x -> with(nuid.ops) { xor(one, upper[x]) } }
    val d_twos_point_one = UByteArray(upper.size) { x -> with(nuid.ops) { xor(shl(one, x), upper[x]) } }

    @Test
    fun testRouteAdd() {
        val NUID = nuid
        var routingTable = RoutingTable<NetMask.Companion.warmSz, UByte>(NUID, null)

        routingTable.addRoute(nuid.run {
            val id1 = random(netmask())
            WorkerNUID(id1) t2 URI("urn:$id1")
        }).run {
            routingTable.addRoute(nuid1 t2 URI("urn:null"))
            for (dOne in d_ones) routingTable.addRoute(WorkerNUID(dOne) t2 URI("urn:$dOne@net"))
            for (dOne in d_twos) routingTable.addRoute(WorkerNUID(dOne) t2 URI("urn:$dOne@net"))
            for (dOne in d_twos_point_one) routingTable.addRoute(WorkerNUID(dOne) t2 URI("urn:$dOne@net"))

            assertEquals(routingTable.buckets[0].size, 7)
            assertEquals(routingTable.buckets[1].size, 11)
            assertEquals(routingTable.buckets[6].size, 1)
            val fibonacciReporter: FibonacciReporter = FibonacciReporter(20000)
            for (n in 0 until 20000) {
                fibonacciReporter.report(n)
                routingTable.addRoute(WorkerNUID(nuid.run { random() }) t2 URI("urn:$n"))
            }
            logDebug {
                "bits: ${routingTable.bits()} fog: ${routingTable.fogOfWar} total: ${routingTable.buckets.sumOf { it.size }} bits/count: ${routingTable.buckets.mapIndexed { x, y -> x.inc() to y.size }}"
            }
        }
        assertEquals(1.shl(routingTable.bits()).dec(), routingTable.buckets.sumOf { it.size })
        var c = 0


        routingTable = object : RoutingTable<NetMask.Companion.warmSz, UByte>(NUID, c++) {}

        routingTable.addRoute(nuid.run {
            val id1 = random(netmask())
            WorkerNUID(id1) t2 URI("urn:$id1")
        }).run {
            routingTable.addRoute(nuid.run {
                val id1 = random(netmask())
                WorkerNUID(id1) t2 URI("urn:$id1")
            })

            val ich = nuid.ops.one
            run {
                val linkedSetOf = linkedSetOf(ich).also(LinkedHashSet<UByte>::clear)

                while (linkedSetOf.size < 3) linkedSetOf.add(nuid.run { random(netmask() - 1) })
                linkedSetOf.forEach { routingTable.addRoute(WorkerNUID(it) t2 URI("urn:$it")) }
            }
            run {
                val linkedSetOf = linkedSetOf(ich).also(LinkedHashSet<UByte>::clear)

                while (linkedSetOf.size < 7) linkedSetOf.add(nuid.run { random(1) })
                linkedSetOf.forEach { routingTable.addRoute(WorkerNUID(it) t2 URI("urn:$it")) }
            }
            debug { }

            assertEquals(7, routingTable.buckets[0].size)
            val fibonacciReporter = FibonacciReporter(20000, "routed")
            for (n in 0 until 20000) {
                fibonacciReporter.report(n)
                routingTable.addRoute(WorkerNUID(nuid.run { random() }) t2 URI("urn:$n"))
            }
            logDebug {
                "bits: ${routingTable.bits()} fog: ${routingTable.fogOfWar} total: ${routingTable.buckets.sumOf { it.size }} bits/count: ${routingTable.buckets.mapIndexed { x, y -> x.inc() to y.size }}"
            }
        }
        routingTable = object : RoutingTable<NetMask.Companion.warmSz, UByte>(NUID, c++) {}

        routingTable.addRoute(nuid.run {
            val id1 = random(netmask())
            WorkerNUID(id1) t2 URI("urn:$id1")
        })
            .run {
                routingTable.addRoute(nuid.run {
                    val id1 = random(netmask())
                    WorkerNUID(id1) t2 URI("urn:$id1")
                })

                val ich = nuid.ops.one
                run {
                    val linkedSetOf = linkedSetOf(ich).also(LinkedHashSet<UByte>::clear)

                    while (linkedSetOf.size < 3) linkedSetOf.add(nuid.run { random(netmask() - 1) })
                    linkedSetOf.forEach { routingTable.addRoute(WorkerNUID(it) t2 URI("urn:$it")) }
                }
                run {
                    val linkedSetOf = linkedSetOf(ich).also(LinkedHashSet<UByte>::clear)

                    while (linkedSetOf.size < 7) linkedSetOf.add(nuid.run { random(1) })
                    linkedSetOf.forEach { routingTable.addRoute(WorkerNUID(it) t2 URI("urn:$it")) }
                }
                debug { }

                assertEquals(7, routingTable.buckets[0].size)
                assertEquals(1, routingTable.buckets[5].size)

                val fibonacciReporter = FibonacciReporter(20000, "routed")
                for (n in 0 until 20000) {
                    fibonacciReporter.report(n)
                    routingTable.addRoute(WorkerNUID(nuid.run { random() }) t2 URI("urn:$n"))
                }
                logDebug {
                    "bits: ${routingTable.bits()} fog: ${routingTable.fogOfWar} total: ${routingTable.buckets.sumOf { it.size }} bits/count: ${routingTable.buckets.mapIndexed { x, y -> x.inc() to y.size }}"
                }
            }
        routingTable = object : RoutingTable<NetMask.Companion.warmSz, UByte>(NUID, c++) {}

        routingTable.addRoute(nuid.run {
            val id1 = random(netmask())
            WorkerNUID(id1) t2 URI("urn:$id1")
        })
            .run {
                routingTable.addRoute(nuid.run {
                    val id1 = random(netmask())
                    WorkerNUID(id1) t2 URI("urn:$id1")
                })

                val ich = nuid.ops.one
                run {
                    val linkedSetOf = linkedSetOf(ich).also(LinkedHashSet<UByte>::clear)

                    while (linkedSetOf.size < 3) linkedSetOf.add(nuid.run { random(netmask() - 1) })
                    linkedSetOf.forEach { routingTable.addRoute(WorkerNUID(it) t2 URI("urn:$it")) }
                }
                run {
                    val linkedSetOf = linkedSetOf(ich).also(LinkedHashSet<UByte>::clear)

                    while (linkedSetOf.size < 7) linkedSetOf.add(nuid.run { random(1) })
                    linkedSetOf.forEach { routingTable.addRoute(WorkerNUID(it) t2 URI("urn:$it")) }
                }
                debug { }

                assertEquals(7, routingTable.buckets[0].size)
                val fibonacciReporter = FibonacciReporter(20000, "routed")
                for (n in 0 until 20000) {
                    fibonacciReporter.report(n)
                    routingTable.addRoute(WorkerNUID(nuid.run { random() }) t2 URI("urn:$n"))
                }
                logDebug {
                    "bits: ${routingTable.bits()} fog: ${routingTable.fogOfWar} total: ${routingTable.buckets.sumOf { it.size }} bits/count: ${routingTable.buckets.mapIndexed { x, y -> x.inc() to y.size }}"
                }
            }
        routingTable = object : RoutingTable<NetMask.Companion.warmSz, UByte>(NUID, c++) {}

        routingTable.addRoute(nuid.run {
            val id1 = random(netmask())
            WorkerNUID(id1) t2 URI("urn:$id1")
        })

        val ich = nuid.ops.one
        run {
            val linkedSetOf = linkedSetOf(ich).also(LinkedHashSet<UByte>::clear)

            while (linkedSetOf.size < 3) linkedSetOf.add(nuid.run { random(netmask() - 1) })
            linkedSetOf.forEach { routingTable.addRoute(WorkerNUID(it) t2 URI("urn:$it")) }
        }
        run {
            val linkedSetOf = linkedSetOf(ich).also(LinkedHashSet<UByte>::clear)

            while (linkedSetOf.size < 7) linkedSetOf.add(nuid.run { random(1) })
            linkedSetOf.forEach { routingTable.addRoute(WorkerNUID(it) t2 URI("urn:$it")) }
        }
        debug { }

        assertEquals(routingTable.buckets[0].size, 7)
        val fibonacciReporter = FibonacciReporter(20000, "routed")
        for (n in 0 until 20000) {
            fibonacciReporter.report(n)
            routingTable.addRoute(WorkerNUID(nuid.run { random() }) t2 URI("urn:$n"))
        }
        logDebug {
            "bits: ${routingTable.bits()} fog: ${routingTable.fogOfWar} total: ${routingTable.buckets.sumOf { it.size }} bits/count: ${routingTable.buckets.mapIndexed { x, y -> x.inc() to y.size }}"
        }
    }
}
