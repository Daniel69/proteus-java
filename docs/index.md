#![image](images/proteus.png) Netifi Proteus
## Introduction
Proteus is an easy to use RPC layer that sits on top of [RSocket](http://rsocket.io). RSocket is a binary protocol for use on byte stream transports such as TCP, WebSockets, and [Aeron](https://github.com/real-logic/aeron). Proteus is able to leverage RSockets unique features allowing you develop complex realtime, streaming applications without needing Kafka, and Spark. It is completely non-blocking and reactive making it responsive and high performance.

### Protobuf
Proteus use [Protobuf 3](https://developers.google.com/protocol-buffers/docs/proto3) as its IDL. Protobuf works well with RSocket because they are both binary. Using protobuf makes it easy to switch from gRPC. You only need to replace the gRPC compiler with the Proteus compiler to start using Proteus.

### Full Duplex
Proteus is built with a full duplex protocol- RSocket. Unlike other protocols (stuff as http) it leverages this fact. An application that initiates a connection (traditionally a client) can actually serve requests from the application it is connecting with (traditionally the server).  In other words a client can connect to a server, and the server can call a Proteus service located on the client. The server does not have to wait for the phone to initiate a call, on the connection is necessary.

### Reactive Streams Compatible
Proteus's Java implementation is built using Reactive Streams compliant library [Reactor-core](https://github.com/reactor/reactor-core). Proteus generated code returns either a Flux which is a stream of many, or a Mono which is a stream of one. Proteus is compatible with any [Reactive Streams](http://www.reactive-streams.org/) implementation including RXJava 2, and Akka Streams. Proteus transparently supports back-pressure.

### Back-pressure
Proteus respects both RSocket and Reactive Stream backpresure. Backpressure is where the consumer of a stream sends the producer a message indicating how many requests it can handle. This allows Proteus reliable, and not become overwhelmed with too much demand. It also helps with tail latency preventing consumers from being over-loaded.

## Prerequisites
* JDK 1.8
* Protobuf Compiler 3.x

## Interaction Models
Proteus has 5 interaction rpc interaction models. They are modeled using a Protobuf 3 IDL. This section details the interaction models, and an example Protobuf rpc definition. The interaction models are request/response, request/stream, fire-and-forget, streaming request/one response, and streaming request/streaming response.

### Request/Response
Request response is analogous to a HTTP Rest call. A major difference is because this is non-blocking the caller can wait for the response for a very long time without blocking other requests on the same connection.

#### Protobuf
```
rpc RequestReply (SimpleRequest) returns (SimpleResponse) {}
```

### Fire-and-Forget
Fire and forget sends a request without a response. This is not just ignoring a response. The underlying protocol does not send anything back to the caller. To make a request fire-and-forget with Proteus you need request google.protobuf.Empty. This will generate fire and forget code.

#### Protobuf
```
import "google/protobuf/empty.proto";
...
rpc FireAndForget (SimpleRequest) returns (google.protobuf.Empty) {}
```

### Single Request / Stream Response
Sends a single response and then receives multiple responses. This can be used to model subscriptions to topics and queues. It can be also used to do things like page from a database. To generate this mark the response with the stream keyword.

#### Protobuf
```
rpc RequestStream (SimpleRequest) returns (stream SimpleResponse) {}
```

### Streaming Request / Single Response
Sends a stream of requests, and receives a single response.  This can be used to model transactions. You can send multiple requests over a logic channel, and then recieve a single response if they were processed correctly or not. Mark the request with the stream keyword to create this interaction model


#### Protobuf
```
rpc StreamingRequestSingleResponse (stream SimpleRequest) returns (SimpleResponse) {}
```

### Streaming Request / Streaming Response
Sends a stream of request and stream of responses. This models a fully duplex interaction. Mark the request and response with stream keyword to create this interaction model

#### Protobuf
```
rpc StreamingRequestAndResponse (stream SimpleRequest) returns (stream SimpleResponse) {}
```

## Documentation



## Release Notes

Please find release notes at [https://github.com/netifi/proteus-java/releases](https://github.com/netifi/proteus-java/releases).

## Bugs and Feedback

For bugs, questions, and discussions please use the [Github Issues](https://github.com/netifi/proteus-java/issues).

## License
Copyright 2017 Netifi Inc.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.