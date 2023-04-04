# Kademlia in 30 seconds

* all gossip nodes have guid
* all guid pair's have hamming distance (number of XOR bits that are '1')
* all distances have buckets
* all messages route to the bucket[distance] of destination guid per hop
* buckets store routes based on uptime (FILO) to prevent FIFO attacks on the stable routes

# Kademlia Payloads

this project explores an external IPFS C+C cluster written in niether Golang nor Js, striking a balance between
simplifiation and borrowing from existing IPFS to match impedences with usecases

other experiments include ephemeral network subsets for actor hierarchies and hyperarchies in content preservation and
consensus based publishing methods and CMS

# 1-pager

The GardenKollection is a distributed computing framework that aims to enable efficient and secure collaboration between
nodes in a network. It is designed to leverage the Kademlia DHT protocol and incorporate subnets of volunteer actors
with specific capabilities to perform various tasks.

The framework is intended to be used in a variety of use cases, including game support tools, large dataset archiving,
ephemeral storage caching, and more. It can also be used for resource allocation and workload distribution among nodes
in a network.

To achieve its goals, the framework will leverage various open source tools and platforms, including Kotlin Common as a
primary compilation target, IPFS for distributed storage, Kademlia DHT for peer-to-peer networking, and various other
tools and libraries to support specific use cases.

The GardenKollection's approach to distributed computing is focused on minimizing technical debt, ensuring security, and
leveraging the strengths of existing technologies to provide a robust and flexible framework for collaborative work.
 