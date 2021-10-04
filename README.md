# Kademlia routing for forum based IPFS Agents

an experiment in SPOF mitigation.

## node goals

simplest possible message forum storage:
  * [ ] host a vanilla kademlia node agent
  * [ ] host IPFS mappaple blocks on nodes with realtime read/write/replace operations
  * [ ] host elections as request for nodes (n=3) in smaller transient networks
  * [ ] transient networks perform work in duplicate, and triplicate for tie-breaker if work differs, work defined as
    coalescing small IPFS blocks into bigger segments on period roll-off basis and linking them as list-tails of block
    lists to list-tails one or more list tails, likely to tail, head, and n intermediate links
  * [ ] delegate IPFS storage to dedicated object storage
  * [ ] offload the node addresses to apache camel uri resolution
  * [ ] examine options to 







