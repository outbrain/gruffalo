Gruffalo
========
[![Build Status](https://travis-ci.org/outbrain/gruffalo.svg?branch=master)](https://travis-ci.org/outbrain/gruffalo)

Graphite is an asynchronous Netty based graphite proxy.
It protects Graphite from the herds of clients by minimizing context switches and interrupts by batching and aggregating metrics.

Gruffalo also allows you to replicate metrics between Graphite installations, for DR purposes for example.

Gruffalo can easily handle a massive amount of traffic, and thus increases your metrics delivery system availability. At Outbrain, we currently handle over 1700 concurrent connections, and over 2M metrics per minute per instance.

