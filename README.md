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