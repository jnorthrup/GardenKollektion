# Kademlia routing for forum based IPFS Agents

an experiment in SPOF mitigation.

the name reflects the intent of gardens of slow moving data grown by gardens of realtime data grown by gardens of p2p
agents.

IPFS objects are the intended data to be assembled into larger IPFS blocks for persistent object storage.

a large (64 bit) address space would be adequate to preserve blocks redundantly for IPFS hosting.

a "topic" concept might only need 7 bits of address space (127 nodes tops) to preserve administrative authority on
content authenticity, large enough to minimize hot spots and small enough to achieve a capacity at a point of
diminishing returns

an "election" concept of 2 bits of address space (3 nodes) would be sufficient to

* elect 3 volunteers for n=3 "warm" IPFS block storage from a controller pool (like 7 bits)
* elect 3 volunteers for n=3 indefinite IPFS block storage from a supporter pool (like 64 bits)

and tentatively experiment wth electing workers

* elect 3 volunteers with 1 judge/verifier and 2 workers to transform a collection of "warm" blocks (perhaps past "edit"
  time limit) into a new tail node on a linked set of blocks
* escalate and repeat the attempt at consensus to arrive at two or more consistent transforms,
* and finally ascertaining the correct order, data blocks, and transform results, evict the source blocks and link a new
  block to the tail of the old blocks

## node goals

simplest possible message forum storage:

* [ ] host a vanilla kademlia node agent
* [ ] host IPFS mappable blocks on nodes with realtime read/write/replace operations
* [ ] host elections as request for nodes (n=3) in smaller transient networks
* [ ] transient networks perform work in duplicate, and triplicate for tie-breaker if work differs, work defined as
  coalescing small IPFS blocks into bigger segments on period roll-off basis and linking them as list-tails of block
  lists to list-tails one or more list tails, likely to tail, head, and n intermediate links
* [ ] delegate IPFS storage to dedicated object storage
* [ ] offload the node addresses to apache camel uri resolution
 






