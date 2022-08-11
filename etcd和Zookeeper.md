---
title: etcd和ZooKeeper
description: etcd和ZooKeeper的一些原理，ectd的会详细一点
date: 2020-09-23
lastmod: 2021-10-25
categories: ["etcd", "zookeeper"]
tags: ["etcd", "zookeeper"]
---

[TOC]

# ZooKeeper

ZooKeeper保证的是CP，不保证每次服务请求的可用性，在极端环境下，ZooKeeper可能会丢弃一些请求，消费者程序需要重新请求才能获得结果。另外在进行leader选举时集群都是不可用，所以说，ZooKeeper不能保证服务可用性。

## 使用场景

* 集群管理，监控节点存活状态
* 主节点选举，当服务以master-salve模式进行部署，当主节点挂掉后选出新的主节点
* 服务发现
* 分布式锁，提供独占锁、共享锁
* 分布式自增id
* 搭配Kafka、dubbo等使用

## 特点

* 顺序一致性：同一客户端发起的事务请求，最终将会严格地按照顺序被应用到 ZooKeeper 中去。
* 原子性：所有事务请求的处理结果在整个集群中所有机器上的应用情况是一致的，也就是说，要么整个集群中所有的机器都成功应用了某一个事务，要么都没有应用。
* 单一系统映像：无论客户端连到哪一个 ZooKeeper 服务器上，其看到的服务端数据模型都是一致的。
* 可靠性：一旦一次更改请求被应用，更改的结果就会被持久化，直到被下一次更改覆盖。

## 数据模型

类似文件系统，根节点为 / ，每创建一个节点会从根节点开始挂，树形结构，每个数据节点称为znode，可以存储数据，每个znode还有自己所属的节点类型和节点状态

> - 持久节点：一旦创建就一直存在，直到将其删除。
> - 持久顺序节点：一个父节点可以为其子节点 **维护一个创建的先后顺序** ，这个顺序体现在 **节点名称** 上，是节点名称后自动添加一个由 10 位数字组成的数字串，从 0 开始计数。
> - 临时节点：临时节点的生命周期是与 **客户端会话** 绑定的，**会话消失则节点消失** 。临时节点 **只能做叶子节点** ，不能创建子节点。
> - 临时顺序节点：父节点可以创建一个维持了顺序的临时节点(和前面的持久顺序性节点一样)。

## ZAB协议

通过ZAB协议保证注册到ZooKeeper上的主从节点状态同步，该协议有两种模式

* 崩溃恢复

  当整个 Zookeeper 集群刚刚启动或者Leader服务器宕机、重启或者网络故障导致**不存在过半的服务器与 Leader 服务器保持正常通信时，所有服务器进入崩溃恢复模式**，首先选举产生新的 Leader 服务器，然后集群中 Follower 服务器开始与新的 Leader 服务器进行数据同步。

* 消息广播

  当集群中超过半数机器与该 Leader 服务器完成数据同步之后，退出恢复模式进入消息广播模式，Leader 服务器开始接收客户端的事务请求生成事物提案（超过半数同意）来进行事务请求处理。

### 选举算法和流程

ZooKeeper集群机器要求至少三台机器，机器的角色分为Leader、Follower、Observer；当 Leader 服务器出现网络中断、崩溃退出与重启等异常情况时，ZAB 协议就会进入恢复模式并选举产生新的Leader服务器。

1. Leader选举：节点在一开始都处于选举阶段，一个节点只要求获得半数以上投票，就可以当选为准Leader；
2. Discovery发现：准Leader收集其他节点的数据，同步 Followers 最近接收的数据，并将最新的数据复制到自身；
3. Synchronization同步：准Leader将自身最新数据同步给其他落后的Follower节点，同步完成后，告知其他节点自己正式当选为Leader；
4. Broadcast广播：Leader正式对外服务，处理client请求，对消息进行广播，当收到一个写请求后，会生成Proposal广播给各个Follower节点，一半以上Follower节点应答后，Leader再发送Commit命令给各个Follower，告知他们提交相关提案。如果有新的节点加入，还需要对新节点进行同步。

默认使用FastLeaderElection算法，比如现在有5台服务器，每台服务器均没有数据，它们的编号分别是1, 2, 3, 4, 5按编号依次启动，它们的选择举过程如下：

1. 服务器1启动，给自己投票，然后发投票信息，由于其它机器还没有启动所以它收不到反馈信息，服务器1的状态一直属于Looking。
2. 服务器2启动，给自己投票，同时与之前启动的服务器1交换结果，由于服务器2的编号大所以服务器2胜出，但此时投票数没有大于半数，所以两个服务器的状态依然是Looking。
3. 服务器3启动，给自己投票，同时与之前启动的服务器1,2交换信息，由于服务器3的编号最大所以服务器3胜出，此时投票数正好大于半数，所以服务器3成为leader，服务器1,2成为Follower。
4. 服务器4启动，给自己投票，同时与之前启动的服务器1,2,3交换信息，尽管服务器4的编号大，但之前服务器3已经胜出，所以服务器4只能成为Follower。
5. 服务器5启动，后面的逻辑同服务器4成为Follower。

## 通知机制

客户端会对某个znode建立一个watcher事件，当该znode发生变化时，这些客户端会收到ZooKeeper的通知，然后客户端根据znode的变化来做出相应的改变，类似观察者模式



# ETCD

## 总体

是一个CP系统，分为三个版本，V1、V2和V3

共识算法使用Raft。将复杂的一致性问题分解成Leader选举、日志同步、安全性三个独立子问题，只有集群一半以上节点存活即可提供服务，具备良好可用性。

![](https://github.com/Nixum/Java-Note/raw/master/picture/ETCD架构.png)

etcdctl支持负载均衡、健康检测、故障转移，3.4版本中负载均衡使用轮询算法，轮询endpoints的每个节点建立长连接，将请求发送给etcd server。client和server之间使用HTTP/2.0协议通信。

etcd server在处理一个请求时会先将一系列的拦截器串联成一个执行，常见的拦截器有debug日志、metrics统计、etcd learner节点请求接口和参数限制等能力，另外还要求执行一个操作前集群必须有Leader，若请求延时超过指定阈值，会打印来源IP的慢查询日志。

### V2版本

数据模型参考ZooKeeper，使用基于目录的层次模式，使用Restful 风格的API，提供常用的Get/Set/Delete/Watch等API，实现对key-value数据的查询、更新、删除、监听等操作。

Key—Value存储上使用简单内存树，一个节点包含节点路径、父亲节点、孩子节点、过期时间、Value的值，是典型的低容量设计，数据全放内存，无需考虑数据分片，只保存Key的最新版本。

在Kubernetes中的使用场景：

> 使用Kubernetes声明式API部署服务的时候，Kubernetes 的控制器通过etcd Watch 机制，会实时监听资源变化事件，对比实际状态与期望状态是否一致，并采取协调动作使其一致。Kubernetes 更新数据的时候，通过CAS 机制保证并发场景下的原子更新，并通过对key 设置TTL来存储Event事件，提升Kubernetes 集群的可观测性，基于TTL特性，Event事件key到期后可自动删除。

缺点：

* 功能局限：不支持范围查询、分页查询、多key事务
* Watch机制可靠性问题：V2是内存型，不保存key历史版本的数据库，只在内存中使用滑动窗口保存最近1000条变更事件，当写请求比较多、网络波动等容易产生事件丢失问题
* 性能问题：使用HTTP/1.x协议，当请求响应较大时无法进行压缩；Json解析消耗CPU；当watcher较多时，由于不支持多路复用，会创建大量的连接；大量的TTL一样也需要为每个key发起续期，无法批量操作
* 内存开销问题：简单内存树保存key和value，量大时会导致较大的内存开销，保证可靠还需要全量内存树持久化到磁盘，消耗大量CPU和磁盘IO

### V3版本

为了解决V2版本的缺点，才诞生了V3版本

> 在内存开销、Watch 事件可靠性、功能局限上，它通过引入B-tree、 boltdb 实现一个MVCC数据库，数据模型从层次型目录结构改成扁平的key-value，提供稳定可靠的事件通知，实现了事务，支持多key原子更新，同时基于boltdb的持久化存储，显著降低了etcd 的内存占用、避免了etcd v2 定期生成快照时的昂贵的资源开销。
>
> 性能上，首先etcd v3 使用了 gRPC API，使用protobuf 定义消息，消息编解码性能相比JSON 超过2倍以上，并通过 HTTP/ 2.0 多路复用机制，减少了大量 watcher 等场景下的连接数。
>
> 其次使用 Lease优化TTL机制，每个Lease具有一 个 TTL，相同的TTL 的key关联一 个Lease，Lease过期的时候自动删除相关联的所有key，不再需要为每个key单独续期。
>
> 最后是etcd v3支持范围、分页查询，可避免大包等 expensive request。

### 应用

* 服务发现
* 消息发布订阅，利用Watcher机制
* 负载均衡
* 分布式锁：支持独占锁和顺序锁
* 集群监控和Leader选举
* 配置存储
* 各种读多写少的场景

## 读操作

客户端通过etcdctl发送get请求`etcdctl get [key名称] --endpoints [多个etcd节点地址]`，etcdctl通过负载均衡算法选择一个etcd节点，发起gRpc调用，etcd server收到请求后经过一系列gRpc拦截器后，进入KV Server模块。之后根据读的行为，进行对应的操作

读操作之前，如果有一个写操作：client发出一个写请求后，若Leader收到写请求，会将此请求持久化到WAL日志，并传播到各个节点，若一半以上的节点持久化成功，则该请求对应的日志条目被标识为已提交，etcd server模块异步从Raft模块获取已提交的日志，应用到状态机(boltdb等)。

### 串行读

直接读状态机（boltdb等）的数据返回，无需通过Raft协议与集群进行交互的模式，可能会读到旧数据。即：写请求广播到各个节点，但串行读可能读到某个还没进行写请求提交的节点(但可能其他节点已提交)，从而读到旧数据。

这种读取方式低延时，高吞吐，适用于读取数据敏感度低、对数据一致性要求不高的场景。


### 线性读（默认）

一旦一个值更新成功后（指有超过半数节点更新提交成功），任何线性读的client都能及时访问到。

#### ReadIndex：保证数据一致性

1. 节点C收到一个线性读请求后，首先会从Leader获取集群最新的已提交的日志索引(committed index)；
2. Leader收到ReadIndex请求后，为防止脑裂异常，会向各个Follower节点发送心跳确认，待一半以上节点确认Leader身份后，才能将已提交的索引(committed index))返回给节点C。
3. C节点继续等待，直到状态机上已应用索引(applied index)大于等于Leader的已提交索引(committed index)时，通知读请求，数据已赶上Leader，可以从状态机中访问数据

既然Follower节点都已经发送ReadIndex请求了，为啥不直接把读请求转发给Leader？原因是ReadIndex比较轻量，而读请求不轻，大量的读请求会造成Leader节点有比较大的负载。

#### MVCC：支持Key多历史版本，多事务功能

核心是内存树形索引模型treeIndex + 嵌入式KV持久化存储库boltdb组成。

**boltdb**会对key的每一次修改，都生成一个新的版本号对象，以**版本号为key，value为用户key-value**等信息组成的结构体。版本号全局递增，通过**treeIndex模块保存用户key和版本号的映射**。查询时，先去treeIndex模块查询key对应的版本号，根据版本号到boltdb里查询对应的value信息。

* treeIndex基于B树实现，只会保存用户的key和版本号的映射，具体的value信息则保存再boltdb里。

* boltdb基于B+树实现的kv键值库，支持事务，提供Get/Put等API，etcd通过boltdb实现一个key的多历史版本。在读取boltdb前，会从一个内存读事务buffer中，二分查找要访问的key是否在buffer里，提高查询速度。

  若buffer未命中，就进到boltdb中查询。boltdb通过bucket隔离集群元数据于用户数据，每个bucket对应一张表（一颗B+树），用户数据的key的值等于bucket名字，etcd MVCC元数据存放的bucket是meta。

## 写操作

客户端通过etcdctl发送put请求`etcdctl put [key值] [value值] --endpoints [多个ectd节点地址]`，etcdctl通过负载均衡算法选择一个etcd节点，发起gRpc调用，etcd server收到请求后经过一系列gRpc拦截器、Quota模块后，进入KV Server模块，KV Server模块向Raft模块提交一个写操作的提案。随后，Raft模块通过HTTP网络模块转发到集群的多数节点持久化，状态变成已提交，etcd server从Raft模块获取已提交的日志条目，传递给Apply模块，Apply模块通过MVCC模块执行命令内容，更新状态机。

etcd写入的value大小默认不超过1.5MB

### Quota模块

etcd的db文件配额只有2G，当超过时，整个集群变成只读，无法写入，这个限制主要是为了保证etcd的性能，官方建议最大不超过8G，不禁用配额。

当etcd server收到写请求时，会先检测db大小 + 上请求时的key-value大小，判断是否超过配额，如果超过，会产生一个NO SPACE的告警，并通过Raft日志同步给其他节点，告知db无空间，并将告警持久化存储到db中，使得集群内其他节点也都拒绝写入，变成只读。

如果达到配额后，再次去修改配额大小，还需要额外发送一个取消警告，消除NO SPACE告警带来的影响。

其次是要检测etcd的压缩配置，如果没有机制去回收旧版本，会导致内存和db大小一直膨胀。

回收机制有多种，常见的是保留最近一段时间的历史版本，给旧版本数据打上free标记，后续新写入的数据直接覆盖而无需申请新空间。另一种回收机制是回收空间，减少db大小，但会产生碎片，产生碎片就需要整理，旧的db文件数据会写入新的db文件，对性能影响较大。

### KVServer模块

写请求在通过Raft算法实现节点间的数据复制前，由KVServer进行一系列的检查。

1. 限速判断，保证集群稳定，避免雪崩。比如Raft模块已提交的日志索引(committed index)比已应用到状态机的日志索引(applied index)超过了5000。
2. 尝试获取请求中的鉴权信息，若使用了鉴权，则判断请求中的密码、token是否正确。
3. 检查写入的包大小是否超过默认的1.5MB。
4. 通过检查后，生成一个唯一的ID，并将该请求关联到一个对应的消息通知channel，向Raft模块发起一个提案，之后KV Server会等待写请求的返回，写入结果通过消息通知channel返回，或者超时，默认超时时间是7秒。

### WAL模块

1. Raft模块收到提案后，如果当前节点是Follower节点，则转发给Leader，只有Leader才能处理写请求。

2. Leader收到提案后，通过Raft模块输出待转发给Follower节点的消息和待持久化的日志条目，日志条目记录了写操作的内容。
3. Leader节点从Raft模块获取到以上消息和日志条目后，将写请求提案消息广播给集群各个节点，同时需要把集群Leader任期号、投票信息、已提交索引、提案内容持久化待一个WAL日志文件中，用于保证集群一致性、可恢复性。
4. 当一半以上节点持久化此日志条目后，Raft模块通过channel告知etcd server模块，写请求提案已被超半数节点确认，提案状态转为已提交，从channel中取出提案内容，添加到FIFO队列中，等待Apply模块顺序、异步依次执行提案内容。

**WAL持久化机制**：先将Raft日志条目内容序列化后保存到WAL记录的Data字段，计算Data的CRC值，设置Type为EntryType，组成一个完成的WAL记录，最后记录WAL记录的长度，顺序写入WAL长度，再写入记录内容，调用fsync持久化到磁盘。

主要作用是为了保证etcd重启时，重放日志提案，保证命令的执行。

```
WAL日志结构
LenField -------- 数据长度
Type ------------ WAL记录类型，有5种，分别是文件元数据记录、日志条目记录、状态信息记录、CRC、快照
CRC ------------- 校验码
Data ------------ WAL记录内容

Raft日志条目Data的结构
Term ----------- uint64，Leader任期号，随Leader选举增加
Index ---------- 日志条目的索引，单调递增，同时也用于确保幂等操作
Type ----------- 日志类型，如 普通的命令日志还是集群配置变更日志
Data ----------- 提案内容
```

### Apply模块

从FIFO队列取出提案执行，同时会保证可靠性，包括crash重启，消费消息的幂等，防止重复提交。通过consistent index字段存储系统当前已执行过的日志条目索引 + 日志条目中的Index字段保证幂等。

1. 从FIFO队列取出提案后，如果之前没被执行过，则进入到MVCC模块

   etcd再进行更新时会为key生成一个版本号，版本号的生成单调递增，启动时默认是1，如果有持久化的数据，则读取boltdb中的数据的最大值，作为当前版本号，版本号格式`{[版本号],[子版本号]}`

2. 写操作执行时，MVCC会递增当前版本号作为key的版本号，存储到treeIndex中。

3. 将新生成的版本号做为key，写操作对应的value写入boltdb，每个key对应一个bucket，boltdb的value包括写操作的key名，key创建时的版本号，最后一次修改时的版本号，key自身修改的次数，写操作的value值，租约信息。将这些信息序列化成一个二进制数据，写入boltdb中，此时还只在boltdb的内存bucket buffer中。此时如果有读请求，会优先从bucket buffer中读取，其次才从boltdb读。

   boltdb不是每个value都是直接写到磁盘的，因为key递增，会顺序写入，所以会合并多个写事务请求，异步(默认每个100ms)，批量事务一次性提交，提高吞吐量。

## Raft协议

Raft协议保证了etcd在节点故障、网络分区等异常场景下的高可用和数据强一致性。

为了避免单点故障，常见的多副本复制方案有两种：主从复制和去中心化复制。

除了复制方案，另一种是共识算法如Paxos或Raft。

Raft将共识算法拆分成三个子问题：

* Leader选举，Leader故障后集群快速选出新Leader。
* 日志复制，集群只有Leader能写入日志，Leader负责复制日志到Follower节点，强制Follower节点与自己保持一致。
* 安全性，一个任期内集群只能产生一个Leader；已提交的日志条目在发生Leader选举时，一定会存在更高任期的新Leader日志中；各个节点的状态机应用的任意位置的日志条目内容一样；发起竞选投票时，任期值小的节点不会竞选成功；如果集群不出现故障，那么一个任期将无限延续下去；投票出现冲突也有可能直接进入下一任再次竞选。

> 当有节点宕机时：
>
> 如果是 Follower 节点宕机，如果剩余可用节点数量超过半数，集群可以几乎没有影响的正常工作。
>
> 如果是 Leader 节点宕机，那么 Follower 就收不到心跳而超时，发起竞选获得投票，成为新一轮 任期 的 Leader，继续为集群提供服务。**需要注意的是：etcd 目前没有任何机制会自动去变化整个集群总共的节点数量**，即如果没有人为的调用 API，etcd 宕机后的节点仍然被计算为总节点数中，任何请求被确认需要获得的投票数都是这个总数的半数以上。

### Leader选举

Raft协议定义集群中节点的状态，任何时刻，每个节点肯定处于某一状态

* Follower：同步Leader收到的日志，etcd启动时的默认状态，等待 Leader 发来心跳信息。若等待超时，则状态由 Follower 切换到 Candidate 进入下一轮 term 发起竞选，等到收到集群多数节点的投票时，该节点转变为 Leader。

* Candidate：可以发起Leader选举。当Follower接收Leader的心跳信息超时时，转为此状态，并立即发起竞选Leader投票，自增任期号，投票给自己，并向其他节点发送竞选Leader投票的消息，当获得多数节点的支持后，即可变成Leader节点。

  节点收到竞选消息后可能出现两种情况：

  1. 节点判断**发出竞选消息的节点的数据至少和自己一样新，并且任期号大于自己的**，并且自己还没投票给其他节点，就可以投票给发出竞选消息的节点了。
  2. 节点此时也发出了竞选，并投票给自己，此时将拒绝投票给发出竞选消息的节点，相互等待直至竞选超时，开启新一轮竞选。

  3.4版本后，节点在进入Candidate前，会先进入PreCandidate状态，此时不自增任期号，而是直接发起预投票，若可以获得多数节点支持，才能变成Candidate，自增任期号发起选举。

  Candidate 在等待其它节点投票的过程中如果发现别的节点已经竞选成功成为 Leader 了，就会切换为 Follower 节点。

  > **为了确保在最短时间内选出Leader，防止竞选冲突**，当节点处于Candidate状态时，有一个竞选的超时时间，且该超时时间是一个随机值，每一个 Candidate 的超时时间都不一致，所以会有时间差。在这个时间差内，如果 Candidate1 收到的竞选信息比自己发起的竞选信息的任期号大（即对方为新一轮 term），并且新一轮想要成为 Leader 的 Candidate2 包含了所有提交的数据，那么 Candidate1 就会投票给 Candidate2，这样就保证了只有很小的概率会出现竞选冲突。
  >
  > **为了防止别的 Candidate 在遗漏部分数据的情况下发起投票成为 Leader**，当竞选超时时，会重新发起新一轮选举，此时任期号提升，发起新一轮投票，投票时，如果别的节点发现发起竞选的节点的数据不完整，就不会投票给他，以此来解决这个问题。

* Leader：唯一性，拥有同步日志的特权，通过定时广播心跳给Follower节点，以维持Leader身份。Leader节点有一个任期号，充当Raft算法的逻辑时钟，可以比较各个节点的数据新旧、识别过期Leader等。

  默认的心跳间隔时间是100ms，默认的竞选超时时间是1000ms，当超时时间大于竞选时间时，节点从Follower状态转为Candidate状态。

  当现有Leader发现新的Leader任期号后，就会转为Follower节点；当现有Leader因为crash重启后，会先变成Follower，若此时无法与其他节点通信，也会进入选举流程，不过会先转为PreCandidate发起预投票，避免因数据落后且在Candidate状态因自增任期号，在恢复通信后造成选举异常。

### 日志复制

Leader节点通过NextIndex字段标识要发送给Follower节点的下一个日志条目索引，MatchIndex字段标识Follower节点已复制的最大日志条目索引，每个节点都有。

一个日志条目被确认为已提交的前提是它需要被Leader同步到一半以上节点。

1. 客户端发送写操作消息msg给etcd，进入Leader节点的Raft模块
2. Leader节点的etcd server模块通过channel从Raft模块获取Ready结构，通过HTTP协议的网络模块将追加日志条目消息广播给Follower，并同时将待持久化的日志持久化到WAL文件中，最后将日志条目追加到稳定的Raft日志存储中。Raft日志存储在内存中，即使丢失也可以通过WAL文件重建。
3. 各个Follower收到追加日志条目消息，通过安全检查后，会持久化消息到WAL日志中，并将消息追加到Raft日志存储，随后向Leader回复一个应答追加日志条目的消息，告知Leader当前已复制的日志最大索引。
4. Leader收到应答追加日志条目消息后，将Follower回复的已复制日志最大索引更新到追踪Follower进展的MatchIndex字段，根据该字段信息，计算出一个位置，如果这个位置已经被一半以上节点持久化，那么这个位置之前的日志条目都可以被标记成已提交。Leader通过心跳信息告知已提交的日志索引位置给Follower。
5. 各个节点的etcd server模块，通过channel从Raft模块获取已提交的日志条目，应用日志条目内容到存储状态机，返回给客户端。

> Raft 为了保证数据的强一致性，所有的数据流向都是从 Leader 流向 Follower，所有 Follower 的数据必须与 Leader 保持一致，如果不一致会被覆盖。
>
> 一个更新数据的请求都最先由 Leader 接收，WAL存下来，然后同步到其他节点；当Leader节点收到大多数Follower 节点的反馈后，提交数据，然后异步通知其他 Follower也进行数据提交。所以写请求肯定是发送到Leader里，而读请求可以发送到任一节点，但要注意是要串行读还是线性读。

### 安全性

Raft通过给选举和日志复制增加规则，保证当Leader crash后，能在众多Follower中选举出有最新日志条目的Follower成为新Leader。

当节点收到选举投票时，需检查发出选举消息的节点的最后一条日志的任期号，若小于自己则拒绝投票，如果相同，日志比自己短，也拒绝投。节点需将投票信息持久化，防止异常重启后再投票给其他节点。

在复制上，通过Leader完全特性、只附加原则和日志匹配保证Leader提交消息并广播给其他节点后crash，这条新消息不会被其他节点删除的问题和各个节点的同Raft日志位置含有相同的日志条目。

* 完全特性：某个日志条目在某个任期号中已被提交，那么这个条目必然出现在更大任期号的所有Leader中
* 只附加原则：Leader只能追加日志条目，不能删除
* 日志匹配：追加日志时会进行一致性检查，Leader发送追加日志的消息时，会把新的日志条目紧接之前的条目的索引位置和任期号包含在里面，Follower节点会检查相同索引位置的任期号是否与Leader一致，一致才追加。

当Follower日志和Leader冲突时，会导致两者的日志不一致，此时Leader会强制Follower直接复制自己的日志来解决，因此在Follower中冲突的日志条目会被Leader的日志覆盖，Leader会记录Follower复制进度的nextIndex，如果Follower在追加日志时一致性检查失败，就会拒绝请求，此时Leader会减小nextIndex值并重试，最终在某个位置让Follower跟Leader一致。尽管WAL日志模块只能追加，对于那些想要删除的持久化日志条目，WAL模块确实没有删除，当发现一个raft log index位置上有多个日志条目时，会通过覆盖的方式，将最后写入的日志条目追加到raft log中，通过覆盖实现删除。

## 鉴权

etcd鉴权体系由控制面和数据面组成。

控制面：通过etcdctl和鉴权API动态调整认证、鉴权规则，AuthServer收到请求后，为了确保各个节点间鉴权元数据一致性，会通过Raft模块进行数据同步。当对应的Raft日志条目被集群半数以上节点确认后，Apply模块通过鉴权存储AuthStore模块，执行日志条目内容，将规则存储到boltdb的鉴权表里。

数据面：由认证和授权组成。目前有两种认证机制：密码认证和证书认证。通过认证后，在访问MVCC模块前，还需要进行授权，检查client是否有权限操作你请求的数据路径，使用的是RBAC机制。

### 认证

* 密码认证：etcd鉴权模块使用bcrpt库的blowfish算法，基于明文密码、随机分配的salt、自定义的cost、迭代多次计算得到一个hash值，并将加密算法版本、salt值、cost、hash值组成一个字符串，作为加密后的密码。以用户名为key，用户名、加密后的密码作为value，存储到boltdb的authUsers bucket里。

  验证密码成功后，返回一个token给client，后续请求携带此token而无需进行密码校验了。默认的token过期时间是5分钟，仅在开发或测试环境中使用。正式环境一般使用JWT。

* 证书认证：类似HTTPS，证书认证在稳定性和性能上都优于密码认证。

### 授权

使用RBAC授权模型。

## 租约 Lease

etcd通过Lease实现活性检测，可以检测各个客户端的存活能力，业务client需要定期向etcd发送心跳请求汇报讲课状态，属于主动型上报，让etcd server保证在约定的有效期内，不删除client关联到此lease上的key-value，若未在有效期内续租，就会删除Lease和其关联的key-value。

基于Lease的TTL特性，可以解决类似Leader选举、Kubernetes Event自动淘汰、服务发现场景中故障节点自动剔除等问题。

检查Lease是否过期、维护最小堆、针对过期Lease发起revoke操作，都由Leader节点负责。

创建Lease时，etcd会保存Lease信息到boltdb的lease bucket中，与该Lease关联的节点需要定期发送KeepAlive请求给etcd server续约Lease。

etcd在启动时，会创建Lessor模块，通过两个异步任务管理Lease：

1. 一个RevokeExpiredLease任务定时检测是否有过期的Lease，使用最小堆管理Lease，每隔500ms进行检查，发起撤销过期Lease的操作，获取到LeaseId后通知整个集群删除Lease和关联的数据；过期默认淘汰限速是每秒1000个。
2. 另一个是CheckpointScheduledLease，定时(默认5min)触发更新Lease的剩余到期时间的操作，定期批量将Lease剩余的TTL基于Raft Log同步给Follower节点，更新其LeaseMap中剩余的TTL信息；另外，Leader节点收到KeepAlive请求后，重置TTL，并同步给Follower节点进行更新。

Lease续约是一个高频率的操作，当完成Lease的创建和节点数据的关联，在正常情况下，节点存活时，需要定时发送KeepAlive请求给etcd续期健康状态的Lease。TTL时间过长会导致节点异常无法从etcd中删除，过短会导致client频繁发送续约请求。另外，Lease的数目可能会很大。为了解决这个问题，etcd在v3版本上，一个是采用grpc解决连接复用问题，减少连接数，另一个是当有不同的key的TTL相同，会复用同一个Lease，减少Lease数目。

Lease最小的TTL时间是 比选举的时间长，默认是2s

## MVCC

MVCC特性由treeIndex、Backend/boltdb组成，实现对key-value的增删查改功能。MVCC模块将请求划分为两个类别，分别是读事务（ReadTxn）和写事务（WriteTxn）。读事务负责处理range请求，写事务负责put/delete操作。

TreeIndex中key的版本号与boltdb中的value关联。

### TreeIndex模块

基于内存版本的B-tree实现Key索引管理，保存用户key与版本号revision的映射关系。之所以使用B-tree，是因为etcd支持范围查询，B树每个节点可以容纳比较多的数据，树高度低，查找次数少，so不用哈希表或平衡二叉树。

```go
type keyIndex struct {
	key         []byte     // key值
	modified    revision   // 最后一次修改key时的etcd版本号
	generations []generation  // 保存一个key若干代版本号信息，每代包含对key的多次修改版本号列表
}

type generation struct {
    ver      int64         // key的修改次数 
    created  revision      // generation结构创建时的版本号
    revs     []revision    // 每次修改key时的revision追加到此数组
}

type revision struct {
    main int64   // 全局递增主版本号，随put/txn/delete事务递增，一个事务内的key main版本号一致，空集群时启动时默认为1
    sub  int64   // 事务内的子版本号，从0开始随事务内put/delete递增
}
```

### Backend/boltdb模块

负责etcd的key-value持久化存储，主要由ReadTx、BatchTx、Buffer组成，ReadTx定义了抽象的读写事务接口，BatchTx在ReadTx之上定义了抽象的写事务接口，Buffer是数据缓存区。Backend支持多种实现，当前使用boltdb，基于B+ tree实现，支持事务的key-value嵌入式数据库。

value的数据结构，版本号格式`{main, sub}`

```go
key：用户的key
value：用户的value
create_revision：key创建时的版本号，与treeIndex中generate的created对应
mod_revision：key最后一次修改时的版本号，put操作时的全局版本号+1作为该值
version：key的修改次数，每次修改时，与treeIndex中generate的ver值+1
lease：
```

一般情况下为了etcd的写性能，默认堆积的写事务数大于1万才在事务结束时同步持久化，由backend的一个goroutine完成，通过事务批量提交，定时将boltdb页缓存中的脏数据提交到持久化存储磁盘中。

### 创建/更新操作

1. 在treeIndex中获取key的KeyIndex信息
2. 填充boltdb的value数据，写入新的key-value到blotdb和buffer中
3. 创建/更新KeyIndex到treeIndex中
4. backend异步事务提交，将boltdb中的数据持久化到磁盘中

### 查询操作

创建一个读事务对象(TxnRead / ConcurrentReadTx)，全量拷贝当前写事务未提交的buffer数据，读不到则从boltdb中查询

### 删除操作

删除操作是软删除，原理类似更新，会在被删除的key版本号追加删除标志t，对应的boltdb value也变成了只包含用户key的KeyValue结构，treeIndex模块也会给此key的KeyIndex结构追加一个空的generation对象，标识此索引对应的key被删除，当查询时发现其存在空的generation对象，并且查询的版本号大于等于被删除的版本号时，返回空。

删除key时会生成events，Watch模块会根据key的删除标识，生成对应的Delete事件；或者当重启etcd，遍历boltdb中的key构建treeIndex内存树时，未这些key上次tombstone标识。

真正删除treeIndex中的KeyIndex、boltdb的value是通过压缩组件异步完成，之所以要延迟删除，一个是为了watcher能够有相应的处理，另一个是减少B tree平衡影响读写性能。

## Watch

客户端订阅etcd的某个key，当key发生变化时，客户端能够感知。这也是Kubernetes控制器的工作基础。

### client获取事件的方式

V2版本：使用轮询推送，每一个watcher对应一个TCP连接，client通过HTTP/1.1协议长连接定时轮询server，获取最新数据变化事件。但是大量的轮询会产生一定的QPS，server端会消耗大量的socket、内存等资源。

V3版本：使用流式推送，因为使用的是基于HTTP/2的gRpc协议，实现了一个TCP连接支持多gRPC stream，一个gRPC stream又支持多个watcher，降低了系统资源的消耗。

### 事件的存储和保留

V2版本：滑动窗口，使用环形数组存储历史事件版本，当key被修改后，相关的事件就会被添加到数组中来，若超过一定容量（默认1000），则会淘汰旧事件，容易导致事件丢失，当事件丢失时，client需要获取最新的版本号才能继续监听，查询成本比较大。

V3版本：MVCC，将事件保存到boltdb中，持久化到磁盘中，通过配置压缩策略控制历史版本数。

版本号是etcd的逻辑时钟，当client因网络等异常连接断开后，通过版本号可以从server的boltdb获取错过的历史事件，而无需全量同步，它是etcd Watch机制数据量增量同步的核心。

### 事件推送机制

![](https://github.com/Nixum/Java-Note/raw/master/picture/etcd事件推送架构.png)

client对每一个key发起的watch请求，etcd的gRPCWatchServer收到watch请求后，会创建一个serverWatchStream，它负责接收client的gRPC Stream的create/cancel watcher请求（recvLoop goroutine)，并将从MVCC模块接收的watch事件转发给client（sendLoop goroutine）

当serverWatchStream收到create watcher请求后，serverWatchStream会调用MVCC模块的WatchStream子模块分配一个watcher id，并将watcher注册到MVCC的WatchableKV模块。

watchableStore将watcher划分为synced / unsynced / victim三类

* synced watcher：如果创建的watcher未指定版本号或版本号为0或指定版本号大于etcd server当前的最新版本号，那它就会保存在 synced watcherGroup中，表示此类watcher监听的数据都已经同步完毕，等待新的变更。
* unsynced watcher：如果创建的watcher指定的版本号小于etcd server当前最新版本号，那它就会保存在 unsynced watcherGroup中，表示此类watcher监听的数据还未同步完成，落后于当前最新数据的变更，正在等待同步。
* victim：当接收watch事件的channel的buffer满了，该watcher会从synced watcherGroup中删除，然后保存到victim的watcherBatch中，通过异步机制重试保证事件可靠性。

当etcd启动时，WatchableKV模块会运行syncWatcherLoop和syncVictimsLoop goroutine，分别负责不同场景下的事件推送。

* syncWatcherLoop：遍历unsynced watcherGroup中的每个watcher，获取key的所有历史版本，转成事件，推送给接收的channel，完成后将watcher从unsynced watcherGroup转移到synced watcherGroup。
* syncVictimsLoop：遍历victim watcherBatch，尝试将堆积的事件再次推送到watcher的接收channel中，若推送失败则再次加入等待重试；若推送成功，watcher监听的最小版本号小于当前版本号，则加入unsynced watcherGroup中，大于则加入synced watcherGroup中。

### 高效的找到监听key的所有watcher

由于watcher可以监听key范围、key前缀，

当收到创建watcher请求时，会把watcher监听的key的范围插入到区间树中，当产生一个事件时，etcd首先会从map中找到是否有watcher监听了该key，其次还要从区间树中找到与key相交的所有区间，得到所有watcher。

## 事务

etcd事务API由IF、Then、Else语句组成。

etcd通过WAL日志 + consistent index + boltdb保证原子性；

WAL日志+boltdb保证持久性；

数据库和业务程序保证一致性；

通过MVCC机制实现读写不阻塞，解决隔离性的脏读问题；MVCC快照读解决隔离性的不可重复读问题；MVCC版本号实现冲突检测机制，在串行提交事务时保证读写的数据都是最新的，未被他人修改。

## boltdb

### 磁盘布局

boltdb文件存放在etcd数据目录下的member/snap/db文件，etcd启动时，会通过mmap机制将db文件映射到内存，后续从内存中快速读取文件中的数据。

![](https://github.com/Nixum/Java-Note/raw/master/picture/etcd_boltdb文件布局.png)

开头两个是固定的db元数据meta page；freeList page记录db中哪些页是空闲的，可使用的；

写操作时，会先打开db文件并增加文件锁，防止其他进程以读写模式打开后操作meta和free page，导致db文件损坏；然后通过mmap机制将db文件映射到内存中，并读取两个meta page到db对象实例，校验meta page的magic version、checksum是否有效，若两个meta page都无效，说明db文件损坏，将异常退出。

执行put请求前会先执行bucket请求，先根据meta page中记录root bucket的root page，按照B+树的查找算法，从root page递归搜索到对应叶子节点page面，返回key名称，leaf类型；如果leaf类型未bucketLeafFlag，且key相等，说明已经创建过，不允许bucket重复创建，否则往B+树种添加一个flag为bucketLeafFlag的key，key的名称为bucket name，value为bucket结构；

执行完bucket请求，就会进行put请求，跟创建bucket类似，根据子bucket的root page，从root page递归搜索此key到leaf page，如果没有找到，则在返回的位置插入新key和value，插入位置的查找使用二分法。

当执行完一个put请求时，值只是更新到boltdb的内存node数据结构里，此时还未持久化。当代码执行到tx.commit api时，才会将node内存数据结构中的数据持久化到boltdb中。一般是经过 删除节点后重平衡操作、分裂操作、持久化freelist、持久化dirty page、持久化meta page。

## 压缩

由于更新和删除都会增加版本号，内存占用和db文件就会越来越大，当达到etcd OOM和db大小的最大配额时，最终不可写入，因此需要适合的压缩策略，避免db大小增长失控。

压缩是使用Compact接口，可以设置自动也可通过业务服务手动调用，压缩时首先会检查请求的版本号rev是否被压缩过，然后更新当前server已压缩的版本号，并将耗时的压缩任务保存在FIFO队列种异步执行。压缩任务执行时，首先会压缩treeIndex模块中的keyIndex索引，其次会遍历boltdb中的key，删除已废弃的key，遍历boltdb时会控制删除的key数100个，每批间隔10ms，分批完成删除操作。

压缩过程中，compact接口会持久化存储当前已调度的压缩版本号到boltdb，保证当发生crash后各个节点间的数据一致性。

压缩时会保留keyIndex中的最大版本号(为了保证key仍存在)，移除小于等于当前压缩的版本号，通过一个map记录treeIndex中有效的版本号返回给boltdb模块使用。

遍历删除boltdb中的数据后，db文件不会变小，而是通过freelist page记录哪些页是空闲的，覆盖使用

使用参数`--auto-compaction-retention '[0|1]'`0表示关闭自动压缩，1 表示开启自动压缩策略，使用参数`--auto-compaction-mode '[periodic|revision]'`，periodic表示周期性压缩，revision表示版本号压缩

* 时间周期性压缩：只保留最近一段时间写入的历史版本，periodic compactor会根据设置的压缩时间间隔，划分为10个区间，通过etcd MVCC模块获取当前的server版本号，追加到rev数组中，通过当前时间减去上一次执行compact操作的时间，如果间隔大于设置的压缩时间，则取出rev数组首元素，发起压缩。

* 版本号压缩：保留最近多少个历史版本，revision compactor会根据设置的保留版本号数，每隔5分钟定时获取当前server最大版本号，减去想保留的历史版本数，得到要压缩的历史版本，发起压缩。

  

# 参考

极客时间 - etcd实战课
