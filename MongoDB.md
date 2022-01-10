---
title: MongoDB
description: MongoDB常用语句、架构、复制集与分片、集合的设计原则、索引、事务原理，引擎相关的待补充
date: 2021-11-28
lastmod: 2021-12-25
categories: ["数据库"]
tags: ["MongoDB", "数据库", "数据库-锁", "数据库事务", "索引", "主从架构", "数据库优化"]
---

[TOC]

# 特点

1. 分布式数据库，Json数据模型，面向对象数据模型，不强制表的scheme
2. 当应用场景不清晰时，可以直接以对象模型直接存储，无需关心字段，表结构灵活，动态增加新字段
3. 不用太过关注表间的关系，可直接嵌套存储，将多种关系存储在同一张表上，同时也加快查表，因为它可以减少磁盘定位次数，如果是关系型数据库，同时查多张表就需要定位多次
4. 原生支持高可用，一般的部署方式是部署三个节点replica set，最多50个；多replica set可以实现自恢复（当主节点挂点后会选出从节点），异地容灾，数据库滚动更新
5. 原生支持横向扩展，通过水平扩展分片实现，外部并不感知有多少个分片，只会当成一个分片使用
6. 支持字段级加密，针对隐私数据，比如身份证、电话等，在入库时可以进行加密，查询时解密
7. 支持地理位置经纬度查询
8. 强大的聚合查询，适合报表、时序数据

# NoSQL语句

客户端使用驱动时连接的执行流程

![客户端执行流程](https://github.com/Nixum/Java-Note/raw/master/picture/MongoDB客户端执行流程.jpg)

数据库端执行流程

![数据库端执行流程](https://github.com/Nixum/Java-Note/raw/master/picture/MongoDB数据库端执行流程.jpg)

要获取ticket是因为MongoDB默认存储引擎wiredtiger的机制，ticket代表着系统资源的数量，ticket数量有限，读写操作都需要先获得ticket才可以进行下一步操作，机制类似信号量。

## 连接

连接mongoDB语句，当有多节点或多分片时，连接也要写上，`mongodb://节点1的host:port, 节点2的host:port,.../databaseName?[options: maxPoolSize(java默认是100), maxWaitTime(查询的最大等待事件), writeConcern, readConcern]`

mongoDB驱动里已提供负载均衡，多节点探测

## 聚合

作用相当与group by，可作用与多个collection，可进行查询和计算。Mongo的聚合操作发生在pipeline中，由多个stage组成，有点像责任链，通过多个state来过滤，聚合数据，每一个{}代表一个state

demo

```sql
MySQL中的
SELECT department, count(null) as emp_QTY 
FROM User 
WHERE gender='female' 
GROUP BY department 
HAVING count(*)<10

等价于mongo中的

db.user.aggregate([
    { $match: {gender: 'female'}},
    {
    	$group: {
    	     _id: '$DEPARTMENT',
            emp_qty: {$sum: 1}
    	}
    },
    { $match: {emp_qty: {$lt: 10}}}
])
```

几个比较特别的运算符

$unwind：将查询到的数组展开

$grouphLookup：图搜索

$facet/$bucket: 分面搜索，根据不同范围条件，多个维度一次性进行分组输出

## 优化

* 查询时，尽量使用索引，为经常做查询的条件添加索引
* 查询时，只查询需要的字段，而不是查询全部，减少网络资源的浪费
* 更新时，只更新必要的字段，而不是每次更新都把整个json文档发送过去，减少网络资源的浪费
* 插入时，尽可能直接批量插入，而不是一条一条插
* 通过mongodb提供的TTL索引，可以实现过期自删数据
* 建表时，文档嵌套不超过3层
* 尽量少用count()来计算总页数，而是使用limit
* 尽量少用skip/limit形式分页，而是通过id来定位起始的位置，这点跟aws dynamoDB很像，不过至少有提供这种功能
* 尽量少用事务，跨分片事务，避免过大事务，控制更新的文档(行)数量
* 使用aggregate时，前一个stage计算得到的数据会传递到下个stage，如果前一个stage没有该数据，则下一个stage无法获取到（尽管表中有该字段）
* 使用aggregate时，pipeline最开始时的match sort可以使用到索引，一旦发生过project投射，group分组，lookup表关联，unwind打散等操作后，则无法使用索引。

## 分析

* 在查询语句中使用explain()方法分析查询语句，有三种分析模式，通过传参的方式使用，比如：`db.getCollection("order").explain('executionStats').find({条件})`
  * queryPlanner：默认，只会输出被查询优化器选择出来的查询计划winningPlane
  * executionStats：除了输出被查询优化器选择出来的查询计划winningPlane，并执行语句（如果是写操作，不会真正操作数据库），给出分析结果，比如扫描的行数，使用什么索引，耗时，返回的条数等
  * allPlansExecution：列出所有可能的查询计划并执行，给出所有方案的结果，mongo支持这种分析模式，但aws的documentDB不支持
```
# 常见的stage枚举：
COLLSCAN：全表扫描
IXSCAN：索引扫描
FETCH：根据前面扫描到的位置抓取完整文档，相当于回表
IDHACK：针对_id进行查询
SHARD_MERGE 合并分片中结果
SHARDING_FILTER 分片中过滤掉孤立文档
SORT：进行内存排序，最终返回结果
SORT_KEY_GENERATOR：获取每一个文档排序所用的键值
LIMIT：使用limit限制返回数
SKIP：使用skip进行跳过
IDHACK：针对_id进行查询
COUNTSCAN：count不使用用Index进行count时的stage返回
COUNT_SCAN：count使用了Index进行count时的stage返回
TEXT：使用全文索引进行查询时候的stage返回
SUBPLA：未使用到索引的$or查询的stage返回
PROJECTION：限定返回字段时候stage的返回

# 一个executionStats例子
{ 
    "queryPlanner" : {
        "plannerVersion" : 1.0, 
        "namespace" : "库名.表名", 
        "winningPlan" : {
            "stage" : "SORT_AGGREGATE", 
            "inputStage" : {
                "stage" : "IXONLYSCAN", 
                "indexName" : "索引名", 
                "direction" : "forward"
            }
        }
    }, 
    "executionStats" : {
        "executionSuccess" : true, 
        "planningTimeMillis" : "0.276", 
        "nReturned" : "1", 
        "executionTimeMillis" : "10517.898",  # 执行时间
        "totalKeysExamined" : "10519.0",  # 总扫描数
        "totalDocsExamined" : "567542",   # 总扫描文档数
        "executionStages" : {
            "stage" : "SORT_AGGREGATE", 
            "nReturned" : "1", 
            "executionTimeMillisEstimate" : "10517.497", 
            "inputStage" : {
                "stage" : "IXONLYSCAN", # 
                "nReturned" : "567542", # 返回的文档数
                "executionTimeMillisEstimate" : "9786.122",  # 执行此阶段的耗时
                "indexName" : "索引名", 
                "direction" : "forward"
            }
        }
    }, 
    "serverInfo" : {
        "host" : "oms", 
        "port" : 27017.0, 
        "version" : "4.0.0"
    }, 
    "ok" : 1.0, 
    "operationTime" : Timestamp(1640604817, 1)
}
```
* 对表的或对库的，使用stats()方法，比如`db.getCollection("order").stats()，或者db.stats()`，可以查询整张表的信息或整个库的统计信息
```
# 针对库的
{ 
    "db" : "order",  # 库名
    "collections" : 11.0,  # 集合数
    "objects" : 4938161.0,  # 对象数
    "storageSize" : 4236443648.0,  # 占用磁盘大小
    "indexes" : 32.0,  # 索引数
    "indexSize" : 1424130048.0,  # 索引大小
    "fileSize" : 5660573696.0,   # 文件大小
    "ok" : 1.0, 
    "operationTime" : Timestamp(1640604514, 1)
}
# 针对表的
{ 
    "ns" : "oms.order", 
    "count" : 749031.0,  # 数量
    "size" : 1479336225.0, # 大小
    "avgObjSize" : 1975.90745, # 每个对象的平均大小
    "storageSize" : 2164432896.0, # 存储大小 
    "capped" : false, 
    "nindexes" : 1.0, # 索引个数
    "totalIndexSize" : 31129600.0, # 总索引大小
    "indexSizes" : {
        "_id_" : 94568448.0, 
        "id" : 77438976.0, 
        "idx_createAt_desc" : 31129600.0,   # 各个索引的大小
    }, 
    "ok" : 1.0, 
    "operationTime" : Timestamp(1640327496, 1)
}
```
* 慢查询
```

```

# 复制集Replica Set机制

一般是一主多从，所有实例存储相同数据

## 特点

* 数据分发，将数据从一个区域复制到另一区域，减少另一区域的读延时
* 读写分离
* 异地容灾，快速切换可用节点
* 复制集主要用于实现高可用，增加复制集的节点数目并不会提升写性能，因为写操作都发生在主节点上，但可以提升读性能。提升性能一般是使用分片的机制

## 机制

* 一般部署奇数个复制集，至少三个，且均要有投票权，一般分为一个主节点，用于接受写入操作和选举时的投票，两个从节点，复制主节点上的数据和选举时的投票
* 主节点上所有的写操作，会被记录到oplog日志中，从节点通过在主节点上打开一个tailable游标不断获取新进入主节点的oplog，进行重放，实现与主节点的数据一致
* 通过选举实现故障恢复
  * 每两个节点间 每2s互发心跳，5次心跳未收到时判断为节点失联
  * 当主节点失联时，从节点发起选举，选出新的主节点，当失联的是从节点，则不会产生选举
  * 选举算法采用raft算法
  * 复制集最多可以有50个节点，但具有投票权的节点最多7个
* 由于从节点有机会成为主节点，所以最好保证多节点的版本，配置上保持一致

# 分片机制

分片主要是对同一实例水平的横向扩展，将原本一个实例里的数据拆分出来多分片存储，最多支持1024个分片，所以实际上是所有分片的数据加起来才是完整数据，每一个分片也是一个复制集

## 特点

* 提升访问性能，降低节点的压力，对外提供同一入口，屏蔽内部集群部署，通过分片路由节点mongos对请求分发到不同的分片，也有负载均衡。分发的规则是通过配置节点configServer决定，配置节点存储了数据与分片的映射。

  mongos、configServer、分片均以复制集为单位，部署多个复制集，实现高可用

* 数据存储时会自动均衡，当mongo发现数据存储分布不均衡时，会做chunk迁移，chunk的概念类似MySQL中的页，一个chunk包含多个文档 = 一页中包含多行记录

* 支持动态扩容

* 可基于集合(即表)进行分片

## 设计原则

* 分片模式
  - 基于范围：根据id或者其他字段按范围进行分片存储，每一个分片的数据上是相邻的，但可能会导致热点数据分布不均
  - 基于哈希：根据指定字段进行hash，数据分布均匀，但范围查询效率低
  - 基于地域、时效、tag分区
* 分片键的选择最好选择基数大(比如id，范围大)，分布均匀，在查询条件中可以明确定位
* 数据量不超过3TB，尽可能保持2TB一个分片
* 分片的数量根据存储容量、集合(即表)和索引的占比、并发量、来决定，**mongodb默认会使用物理机60%的内存来**

# 文档模型设计原则

* 传统关系型数据库设计，从概念模型 -》逻辑模型 -》物理模型，关系明确，遵循三范式（1.要有主键，列不可分，2.每列与主键相关，3.不能存在传递依赖(不允许字段冗余)），表现形式上，一对多关系，外键在多那张表上，多对多关系，会有第三张表来做关联

  对于文档模型，一般对应关系型数据库设计的逻辑模型阶段，通过嵌套实体数组，map或者引用字段来处理实体间的关系，字段冗余限制宽松

* 实体间的关系，一对一使用嵌套map来表示；一对多使用嵌套数组表示；多对多使用嵌套数组+冗余字段来表示；此外，也可以通过嵌套数组存id + 另一张表来表示实体间的关系，通过id来进行联表（使用aggregate + $lookup）

  **注意**：嵌套时要注意整个文档大小，限制是16M，读写比列，也要注意数组长度大小，一般会使用id引用模式来解决；$lookup只支持left outer join，不支持分片表

* 模式套用：

  * 场景：时序数据，解决：对于同一个实体，将其变化字段的存储和更新，聚合在内嵌数组中
  * 场景：大文档、多相似字段、多相似索引，比如sku的多属性，解决：将多个相似字段转成内嵌数组，索引建立在内嵌数组的实体字段中，
  * 场景：由于schemeless的特性，不同版本存在不一样的字段，需要对字段做校验，解决：增加一个版本字段，通过该字段进行判断
  * 场景：计数统计，每秒要进行计数更新，不需要准确的计数，解决利用mongo的随机计数器$inc，
  * 场景：精确统计，排行版，解决：使用预聚合字段，比如增加排名字段，每次更新依赖字段的同时，更新排名字段，进行排行查询时，直接根据排行字段进行排序即可，不用重新聚合

# 索引

## 原理

MMap存储引擎的 主键索引、普通索引、组合索引的数据结构都是B-树，而wiredtiger存储引擎则使用LSM树，类似b+树，现在wiredtiger引擎是默认的存储引擎

[LSM树参考](https://juejin.im/post/6844903688075477000)

为什么MongoDB MMAP引擎使用b-树作为索引的数据结构，而MySQL使用b+树？

* 两种数据结构的主要区别在于b-树查询的深度比较随机，节点包含了全部数据，而b+树比较平均，b-树的全表扫描比b+树差
* 关系型数据库因为关联性强的原因，会经常出现联表操作，b+树可以加快全表扫描的速度，而Mongo鼓励表关系用数组嵌套表示，对全表扫描的需求不是特别强烈
* Mongo一般也用于定点查询，使用b-树单次查询的效率会比较高

## 调优

* nosql语句最后添加.explain()来查看执行情况，类似MySQL的explain，一般关注executeTimeMillis(扫描花费的时间)、totalDocsExamined(扫描的总条数)、executionStages.docsExamined、executionStages.inputStage.stage(使用到什么索引)这几个字段
* 创建组合索引时，一般以能够  精确匹配 -》排序 -》范围匹配  的字段顺序进行创建
* 可以创建部分索引，比如对id>50的文档才会对id创建索引；对有值的字段才建索引，需要在创建索引时使用partialFilterExpression表达式
* 创建索引时使用后台创建索引，{background: true}

# 引擎

## MMAP

## wiredtiger

有空再补...

# 事务

mongo是分布式数据库，通过部署多复制集来实现，单个MongoDB server不支持事务，至少需要一主一从两个。

4.0版本支持复制集多文档事务，4.2版本支持分片事务，但其实3.2版本有支持单文档原子操作了，主要是用在单文档嵌套更新上，默认自动的，MongoDB中文档 = 行记录。

另外，Spring 5.1.1 / SpringBoot 2.x 以上 @Transactional可以支持Mongo，需要为TransactionManager注入MongoDBFactory

## 分布式下的写操作

对于事务内多文档执行写操作，保证原子性，失败能回滚

### writeConcern参数

通过参数writeConcern来保证写操作到达多少个节点才算成功，写操作会等到写到复制集后才会返回

使用：nosql语句里包含{writeConcern: {w: 1, timeout: 3000}}，单位毫秒，超时不意味着都写失败，有可能已经写入了一些节点了，写操作性能会取决于节点的数量，会影响写入的时间，但对集群不会有压力

* 默认值为1，表示数据被写入到一个节点就算成功
* =[数字x]，表示数据被写入到x个节点就算成功
* =0，表示发出的写操作，不关心是否成功
* =majority（推荐）表示发出的写操作需要写入到多数复制集上才算成功，一般写操作写入主节点，由主节点同步给从节点
* =all，需要全部节点确认，但如果同步时有节点挂了，此时写操作会被阻塞

### journal参数

写操作时，会先写到内存，在写入磁盘（journal日志文件和数据文件），参数journal字段来保证达到哪步操作才算成功，由配置文件里`storage.journal.enabled`控制是否开启，`storage.journal.commitInternalMs`决定刷盘时间间隔

使用：nosql语句里包含 {journal: {j: true}} 会保证每次写入都会进行刷盘

* =true，表示写操作要落到journal日志文件才算成功
* =false，表示写操作到达内存就算成功

### journal日志与oplog日志的区别

* **journal日志**：由MongoDB引擎层使用，**主要用于控制写操作是否立即持久化**，因为如果发生写操作，mongo一般是先将写操作写入内存，在定时（默认是1分钟）将内存里的数据刷盘持久化。

  如果写操作的成功判定出现在写完内存后，如果此时宕机，将会丢失写操作的记录，如果判定发生在写完journal日志之后，如果宕机可以利用journal日志进行恢复。

  写入journal日志时，也是先写入内存，再定时（默认是100ms）写入磁盘

* **oplog日志**：只用在主从复制集的使用，通过oplog来**实现节点间的数据同步**，从节点获取主节点的oplog日志后进行重放，同步数据。oplog本身在MongoDB里是一个集合(表)

  mongo的一次写操作，包括 1.将文档数据写进集合 2.更新集合的索引信息 3.写入oplog日志，（此时只是写入内存），这三个步骤是原子性的，要么成功要么失败。一次写操作可以是批量的也可以是单条，一次写操作对应一条journal日志。
  
* **开启jourbal和writeConcern后的数据写入顺序**：

  1. 写操作进来，先将写操作写入journal缓存，将写操作产生的数据写入数据缓存，将journal内存里的写操作日志同步到oplog里，异步响应给客户端；
  2. 后台线程检测到日志变化，将oplog同步给从节点；另外，后台每100ms会将journal缓存里的日志刷盘，每60s将数据缓存里的数据刷盘

## 分布式下读操作

### readPreference参数

读操作时，决定使用那一个节点来处理读请求，也可以配合tag使用，来指定节点访问

使用：可以设置在连接语句里，驱动程序API，nosql语句+.readPref("primary")

* =primary（默认，推荐），只选择主节点
* =primaryPreferred：优先选择主节点，不可用时使用从节点
* =secondary：只选择从节点，一般用于历史数据查询，针对时效性低的数据，或者报表服务的操作
* =secondaryPreferred：优先选择从节点，不可用时选择主节点
* =nearest：选择最近节点，根据ping time决定，一般用于异地部署

### readConcern参数

读操作时，决定这个节点的数据哪些是可读的，**类似MySQL中的隔离性**

使用：需要在mongo的配置文件的server参数中，增加`enableMajorityReadConcern: true`，

对于值枚举未available、local、majority、linearizable，需要在查询语句后 + .readConcern("local或者其他枚举")开启使用；

对于snapshot，则是在开启事务时指定，`var session = db.getMongo().startSession(); session.startTransaction({readConcern: {level: "snapshot"}, writeConcern: {w: "majority"}});`  session作为事务对象，配合snapshot快照、MVCC、redo log来实现事务。

* =available：读取所有可用数据

* =local（默认）：读取所有可用且属于当前分片的数据，类似available，只是限制在分片上，

  mongo为了实现数据均衡，有分片chunk迁移机制，当分片1将数据迁移至分片2，但还没迁移完成时，此时两个分片都存在该chunk数据，但是此时该数据仍属于分片1，=local时不能读到分片2的该数据，=available时则可以

* =majority：读取在多数节点上提交完成的数据，通过MVCC机制实现，**作用类似MySQL中的提交读隔离级别**。

  比如，当读操作指向主节点，且写操作已经同步到多数从节点时，才可以读到该数据；当读操作指向一个从节点，且从节点完成写操作，且通知了主节点并得到主节点的响应时，才能读到该数据。

  作用：

  1. 主要是防止分布式数据库脏读，这里脏读指的是，在一次写操作到达多数节点前读取了这个写操作，又由于故障之类的导致写操作回滚了，此时读到的数据就算脏读。
  2. 配合writeConcern=mahority来实现读写分离，向主节点写入数据，从节点也能读到数据

* =linearizable：线性读取文档，性能较差，**作用类似MySQL中的serializable(串行化)隔离级别**。

  只对读取单个文档时有效，保证在写操作完成后一定可以读到，一次读操作需要所有节点响应

* =snapshot：读取最近快照中的数据，**作用类似MySQL中的可重复读隔离级别**，事务默认60s，需要4.2的驱动，会影响chunk迁移，多文档读操作必须使用主节点读

  * 全局事务管理器通过自旋创建快照
  * 实现在同一事务下读取同一数据的一致性，也可以读取同一事务内的写操作；
  * 当多事务内出现写操作冲突时，排在后面事务的写操作会失败，触发abort错误，此时只能把该事务抛弃，再重新开始事务
  * 当事务外和事务内的写操作冲突，事务内的写操作早于事务外的写操作，事务外的写操作会进入等待，直到事务内提交，才会执行

# Change Stream

* 类似MySQL中的触发器，但它是异步的，非事务，实现变更追踪，在应用回调中触发，可同时触发多个触发器，故障后会从故障点恢复执行（故障前会拿到id，通过id来恢复）

* 基于oplog实现，在oplog上开启tailable cursor追踪复制集上的变更操作，可追踪的事件包括ddl、dml语句，在ddl、dml语句执行后触发
* 使用时需要开启readConcern，且设置read/writeConcern：majority，只推送已经在大多数节点上提交的变更操作
* 使用：`db.[具体的collectionName].watch([...]).操作函数`，当满足watch里的条件时会触发操作方法
* 使用场景：跨集群复制、微服务变更数据库时通知其他微服务

# 备份和恢复

备份主要是为了防止误操作，一个节点的误操作会同步到其他节点，备份同时也可以进行数据回溯

## 备份方案

* 延迟节点备份，通过设置一个延迟节点，比如让其与主节点的存储差距延迟一个小时的数据量，通过oplog + oplog window重放实现

* 全量备份，数据文件快照(一般是某个时间点) + oplog(该时间点之后操作日志记录)实现

  `mongodump -h [需要备份的mongo实例的host:port] -d [数据库名称] -o [备份存放路径]`命令保存数据快照

  恢复：`mongorestore -h [目标Mongo实例的host:port] -d [数据库实例名称] [[快照所在的路径]| -dir [快照所在的路径]]`

# 监控

一般使用mongodb的ops manager，或者grafana实现

## 监控指标

通过db.serverStatus()方法来获取指标，serverStatus方法记录的是自mongo启动以来的指标数据，常见指标有很多，一般会关注下面几个

* connections：连接数信息
* locks：mongoDB使用锁的情况
* network：网络使用情况统计
* opconters：CURD执行次数统计
* repl：复制集配置信息
* men：内存使用情况
* scan and order：每秒内存排序操作的平均比例
* oplog window：代表oplog可容纳多长时间的操作，表示从节点可以离线多久后可以追上主节点的数据
* wiredTiger：包含大量WirdTiger引擎执行情况的信息，如block-manager：WT数据块的读写情况，session：session使用量，concurrentTransactions：ticket使用情况
* metrics：一系列指标统计信息

# 参考

极客时间 - mongodb高手课
