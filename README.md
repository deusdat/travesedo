# Travesedo

Travesedo is a Clojure driver for the ArangoDB data store system. It was inspired by clojure/java.jdbc wrapper library
and its ease of configuration passing. The goal of the driver is to provide the most idiomatic Clojure driver while fully
leveraging the power of the ArangoDB tools.

I'm personally using the project. As a result, the driver has a bit of a pragmatic approach: getting features that I need out.
As I have time, I will extend the driver (presently focused on document interaction) to the larger body of ArangoDB features.

## Usage

Every operation requires a database context. At its simplest the context requires a :conn value. There are three possible values:
- Single Server Mode
```clojure
    {:type :simple
      :url "http://localhost:8529"
      :uname "example_user"
      :password "secret"}
```

- Replica Set
```clojure
    {:type :replica
      :url "http://masterhost:8529"
      :uname "master_user_name"
      :password "secret"
      :replicas [{:url "http://replica1host:8529"
                        :uname "replica_user_name"
                        :password "replica_password"}]}
      ;; if no uname & password fields are set in the replicas,
      ;;they inherit from the master configuration.
```

- Sharded
```clojure
    {:type :shard
    :uname "common_user_name"
    :password "common_password"
    :coordinators [{:url "http://coord1host:8529"}  ;; Inherits the password
                                                    ;; from the outer config.
                            {:url "http://coord2host:8529"}
                              :uname "replica2_user "
                              :password "replicate2_password"]}
```
When __:type :replica__ is used the driver will randomly select the replica from which to read. If you want use a different selection algorithm for reads, you can set __:conn-select__ to a function that takes the connection and returns a map with the from __{:url :uname :password}__.

The following is the simplest context.
```clojure
  (def context {:conn {:type :simple :url "http://locahost:8529" :uname "dev_user " :password "secret"}})
```

## Adding Configuration Nuance
Knowing how to make a valid connection string is great, but you'll want to do more than that. This section covers the process of using the drivers.

### Submiting a Payload
The driver will automatically convert a collection into a json format. To pass a payload to ArangoDB you need to add a key to the call context named __:payload__. The functions for interacting with ArangoDB will tell you what the payload should look like.

### Async Communication
By default, operations in ArangoDB are synchronous per connection. For many applications this is perfectly fine,
you might want to take advantage of the asynchronous abilities at times. That's where the __:async__ option comes in.
There are four possible values.
* __Not set__ - means the system defaults to __:async :false__.
* __:false__ - this means that the system will use regular synchronous communication.
* __:true__ - indicates that you want a fire and forget approach. If the server has room in the work queue, a success message comes back. Otherwise the driver will throw an exception.
* __:store__ - means that the server should queue the work, but return a job id for later pickup.
The driver will return the job id in the __:job-id__ field of the response map.

## Flushing to Disk
Like many largely in-memory databases ArangoDB keeps "commited" items in memory for some time before flushing them to the disk. In the case of a system failure, it is possible to lose these changes. If you need to wait for a flush, set __:wait-for-sync :true__ in the ctx.

## Working with Revisions
ArangoDB leverages  MVCC  for transaction semantics. Depending on the operation, you might want to see if the version in the database matches the version you have in local memory. You might want to check the opposite of that. That's when you can use __:if-match__ and __:if-none-match__. The driver will throw a 412 exception when __:if-none-match__ is passed and the revision is the same or when __:if-match__ is passed and the revisions are different. if both are passed, the last one retrieved from the map wins.

## Specifying Collection and Database
Most operations need to know on which database they are working: __:db "db_name"__.

## Full Context Example
```clojure
(def context {:conn {:type :simple
                      :url "http://localhost:8529"
                      :uname "dev_user"
                      :password "secret"}
               :async :stored
               :if-match "123123"
               :conn-select (fun [conn]...)
               :db "example_db"
               :connection-timeout 1000
               :socket-timeout 2000
               :accept-all-ssl? true})
```

## Exception Handling
Errors like 404, or 500 are returned as maps. The driver does not throw exceptions. You should check the result's :error to see if everything was fine.

## Attempt at Idiomatic Clojure.
The input and top level attributes of the response keys follow idomatic,
lowercase names. For example, ArangoDB returns "isSystem" when working with 
collection meta data as part of its root document. The driver will convert this
to :is-system. The driver will not convert 

## License

Copyright © 2014 DeusDat Solutions.

Distributed under the Eclipse Public License either version 1.0.
