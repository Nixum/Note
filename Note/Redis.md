# 数据类型及常用命名

# 与Memcached的区别

* Redis支持存储多种数据类型：string、list、hash、set、zset；而Memcached只支持string

* Redis支持持久化：RDB快照和AOF日志；Memcached不支持持久化

* Redis支持事务，使用MULTI 和 EXEC命令，支持流水线式发送命令 ；Memcahced不支持事务，命令只能一条一条的发

* Redis-Cluster 支持分布式存储，可以多台Redis服务器存储同样的数据；Memcached是以一致性哈希算法实现分布式存储，即多台Memcached服务器，Memcached根据查找的key计算出该数据在哪台服务器上

* 在 Redis 中，并不是所有数据都一直存储在内存中，可以将一些很久没用的 value 交换到磁盘； 

  Memcached 的数据则会一直在内存中，Memcached使用固定空间分配，将内存分为一组大小不同的slab_class，每个slab_class又分为一组大小相同的slab，每个slab又包含一组大小相同的chunk，根据数据大小，放到不同的chunk里，这种管理方式避免内存碎片管理问题，但是会带来内存浪费，即每个chunk内会放小于这个chunk大小的数据，chunk里有些空间没利用到

# 过期时间和数据淘汰策略

# 持久化

# 事务

