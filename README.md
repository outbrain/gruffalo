Gruffalo
========
[![Build Status](https://travis-ci.org/eranharel/gruffalo.svg?branch=master)](https://travis-ci.org/eranharel/gruffalo)

Gruffalo is an asynchronous Netty based graphite proxy.
It protects Graphite from the herds of clients by minimizing context switches and interrupts; by batching and aggregating metrics.

Gruffalo also allows you to replicate metrics between Graphite installations for DR scenarios, for example.

Gruffalo can easily handle a massive amount of traffic, and thus increase your metrics delivery system availability.

In practice, Gruffalo handles thousands of concurrent connections, and over several million metrics per minute per instance at our production environment.

## Usage
```
$ ./gruffalo-standalone.sh -h
usage: com.outbrain.gruffalo.StandaloneGruffaloServer
 -c,--clusters <arg>          Graphite relay clusters (required)
 -flushOnIdleTime <arg>       Max amount of time (seconds) in which a
                              channel can be idle before the current batch
                              is flushed (default 10). 0 means disabled.
 -h,--help                    print this message
 -maxBatchSize <arg>          Max size of metrics batch in characters.
                              This setting affects the memory footprint of
                              the server - each channel holds a buffer of
                              the specified size for batching (default
                              8192)
 -maxInflightBatches <arg>    Max number of allowed in-flight batches
                              before we start pushing back clients
                              (default 1500)
 -maxMetricLength <arg>       Maximum length of a frame we're willing to
                              decode (default 4096)
 -p,--port <arg>              TCP listen port (required)
 -reconnectOnIdleTime <arg>   Max time in seconds before we reconnect an
                              idle downstream channel (default 120)
```

The `gruffalo-standalone.sh` script allows you to configure the inbound port, multiple replication targets, 
and to optionally control the behavior of the proxy.

**Example execution**
Start Gruffalo on TCP port 3003, and proxy to 2 clusters: 
[localhost:2003,localhost:2004] and [localhost:2006,localhost:2006]
(traffic will be replicated to both clusters)
```
$ ./gruffalo-standalone.sh -p 3003 -c localhost:2003,localhost:2004 -c localhost:2005,localhost:2006
{"timestamp":"2019-05-25T16:11:07.074+03:00","message":"Creating a client pool for [localhost:2003,localhost:2004]","logger_name":"c.o.g.netty.GraphiteClientPool","thread_name":"main","level":"INFO","facility":"Gruffalo","environment":"DEV","hostName":"my.host"}
{"timestamp":"2019-05-25T16:11:07.084+03:00","message":"Client for [localhost:2003] initialized","logger_name":"c.o.g.netty.NettyGraphiteClient","thread_name":"main","level":"INFO","facility":"Gruffalo","environment":"DEV","hostName":"my.host"}
{"timestamp":"2019-05-25T16:11:07.089+03:00","message":"Client for [localhost:2004] initialized","logger_name":"c.o.g.netty.NettyGraphiteClient","thread_name":"main","level":"INFO","facility":"Gruffalo","environment":"DEV","hostName":"my.host"}
{"timestamp":"2019-05-25T16:11:07.089+03:00","message":"Client for [localhost:2003] is reconnecting","logger_name":"c.o.g.netty.NettyGraphiteClient","thread_name":"main","level":"INFO","facility":"Gruffalo","environment":"DEV","hostName":"my.host"}
{"timestamp":"2019-05-25T16:11:07.149+03:00","message":"Client for [localhost:2004] is reconnecting","logger_name":"c.o.g.netty.NettyGraphiteClient","thread_name":"main","level":"INFO","facility":"Gruffalo","environment":"DEV","hostName":"my.host"}
{"timestamp":"2019-05-25T16:11:07.153+03:00","message":"Creating a client pool for [localhost:2005,localhost:2006]","logger_name":"c.o.g.netty.GraphiteClientPool","thread_name":"main","level":"INFO","facility":"Gruffalo","environment":"DEV","hostName":"my.host"}
{"timestamp":"2019-05-25T16:11:07.153+03:00","message":"Client for [localhost:2005] initialized","logger_name":"c.o.g.netty.NettyGraphiteClient","thread_name":"main","level":"INFO","facility":"Gruffalo","environment":"DEV","hostName":"my.host"}
{"timestamp":"2019-05-25T16:11:07.154+03:00","message":"Client for [localhost:2006] initialized","logger_name":"c.o.g.netty.NettyGraphiteClient","thread_name":"main","level":"INFO","facility":"Gruffalo","environment":"DEV","hostName":"my.host"}
{"timestamp":"2019-05-25T16:11:07.154+03:00","message":"Client for [localhost:2005] is reconnecting","logger_name":"c.o.g.netty.NettyGraphiteClient","thread_name":"main","level":"INFO","facility":"Gruffalo","environment":"DEV","hostName":"my.host"}
{"timestamp":"2019-05-25T16:11:07.155+03:00","message":"Client for [localhost:2006] is reconnecting","logger_name":"c.o.g.netty.NettyGraphiteClient","thread_name":"main","level":"INFO","facility":"Gruffalo","environment":"DEV","hostName":"my.host"}
{"timestamp":"2019-05-25T16:11:07.166+03:00","message":"Connected to: localhost/127.0.0.1:2004","logger_name":"c.o.g.n.GraphiteChannelInboundHandler","thread_name":"nioEventLoopGroup-2-2","level":"INFO","facility":"Gruffalo","environment":"DEV","hostName":"my.host"}
{"timestamp":"2019-05-25T16:11:07.168+03:00","message":"Initializing TCP...","logger_name":"c.o.gruffalo.netty.GruffaloProxy","thread_name":"main","level":"INFO","facility":"Gruffalo","environment":"DEV","hostName":"my.host"}
{"timestamp":"2019-05-25T16:11:07.168+03:00","message":"Connected to: localhost/127.0.0.1:2003","logger_name":"c.o.g.n.GraphiteChannelInboundHandler","thread_name":"nioEventLoopGroup-2-1","level":"INFO","facility":"Gruffalo","environment":"DEV","hostName":"my.host"}
{"timestamp":"2019-05-25T16:11:07.168+03:00","message":"Connected to: localhost/127.0.0.1:2006","logger_name":"c.o.g.n.GraphiteChannelInboundHandler","thread_name":"nioEventLoopGroup-2-4","level":"INFO","facility":"Gruffalo","environment":"DEV","hostName":"my.host"}
{"timestamp":"2019-05-25T16:11:07.172+03:00","message":"Connected to: localhost/127.0.0.1:2005","logger_name":"c.o.g.n.GraphiteChannelInboundHandler","thread_name":"nioEventLoopGroup-2-3","level":"INFO","facility":"Gruffalo","environment":"DEV","hostName":"my.host"}
{"timestamp":"2019-05-25T16:11:07.176+03:00","message":"Binding to TCP port 3003","logger_name":"c.o.gruffalo.netty.GruffaloProxy","thread_name":"main","level":"INFO","facility":"Gruffalo","environment":"DEV","hostName":"my.host"}
{"timestamp":"2019-05-25T16:11:07.176+03:00","message":"Initialization completed","logger_name":"c.o.gruffalo.netty.GruffaloProxy","thread_name":"main","level":"INFO","facility":"Gruffalo","environment":"DEV","hostName":"my.host"}
{"timestamp":"2019-05-25T16:11:07.177+03:00","message":"******** Gruffalo started ********","logger_name":"c.o.g.StandaloneGruffaloServer","thread_name":"main","level":"INFO","facility":"Gruffalo","environment":"DEV","hostName":"my.host"}
```

### Logging
Gruffalo supports logstash compatible output, and logs to standard output, where logs can easily be collected.

The logging can be slightly modified using the following flags:
* `GRUFFALO_LOG_APPENDER` - if present and equals to `STDOUT` logs will be in text format.
If absent - logs will be in the logstash JSON format by default.
* `GRUFFLAO_ENVIRONMENT` - controls the value of the environment field, defaults to `DEV`

Reference Docs
--------------
*  [About the design] (http://www.slideshare.net/eranharel/reactive-by-example-at-reversim-summit-2015)
