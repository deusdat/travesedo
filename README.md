# Travesedo

Travesedo is a Clojure driver for the ArangoDB data store system. It was inspired by clojure/java.jdbc wrapper library
and its ease of configuration passing. The goal of the driver is to provide the most idiomatic Clojure driver while fully
leveraging the power of the ArangoDB tools.

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
      they inherit from the master configuration.
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

### Async Communication
By default, operations in ArangoDB are synchronous per connection. For many applications this is perfectly fine,
you might want to take advantage of the asynchronous abilities at times. That's where the __:wait-for-sync__ option comes in.
There are four possible values.
* __Not set__ - means the system defaults to __:wait-for-sync :false__.
* __:false__ - this means that the system will use regular synchronous communication.
* __:true__ - indicates that you want a fire and forget approach. If the server has room in the work queue, a success message comes back. Otherwise the driver will throw an exception.
* __:store__ - means that the server should queue the work, but return a job id for later pickup.
The driver will return the job id in the __:job-id__ field of the response map.

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
               })
```

## License

Copyright © 2014 DeusDat Solutions.

Distributed under the Eclipse Public License either version 1.0.
