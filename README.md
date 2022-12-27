- André Cerqueira 201804991
- Bruna Rocha 201906417

## Token Ring Algorithm

```
$ javac ds/assignment/tokenring/Peer.java
$ java ds.assignment.tokenring.Peer ip port next_node_ip next_node_port
```

Every peer has its own shell that accepts `lock` and `unlock` in order to stop/start the token flow.

## Data Dissemination via Gossiping

```
$ wget https://github.com/dwyl/english-words/blob/master/words_alpha.txt?raw=true -O words.txt
$ javac ds/assignment/tokenring/Peer.java
$ java ds.assignment.tokenring.Peer ip port words.txt
```

As soon as a peer is created words start to get generated, another peers can be registered with the command
`register ip port`, after that any other word generated will be sent to the registered peers and disseminated
across the network.

## Totally Ordered Multicast

```
$ javac ds/assignment/multicast/Peer.java 
$ java ds.assignment.multicast.Peer ip port file_containing_all_ips_of_network
```

The file containing all ips have the following syntax (no whitespaces allowed before/after):

`ip:port`, represents a simple client
`s:ip:port`, represents a service provider

```
localhost:3000
localhost:3001
localhost:3002
localhost:3003
s:localhost:3004
s:localhost:3005
```
