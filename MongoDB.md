---
title: MongoDB
description: MongoDB常用语句、架构、复制集与分片、集合的设计原则、索引、事务原理，引擎相关
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

`$unwind`：将查询到的数组展开

`$grouphLookup`：图搜索

`$facet/$bucket`：分面搜索，根据不同范围条件，多个维度一次性进行分组输出

# 文档模型设计原则

* 传统关系型数据库设计，从概念模型 -》逻辑模型 -》物理模型，关系明确，遵循三范式（1.要有主键，列不可分，2.每列与主键相关，3.不能存在传递依赖(不允许字段冗余)），表现形式上，一对多关系，外键在多那张表上，多对多关系，会有第三张表来做关联

  对于文档模型，一般对应关系型数据库设计的逻辑模型阶段，通过嵌套实体数组，map或者引用字段来处理实体间的关系，字段冗余限制宽松

* 实体间的关系，一对一使用嵌套map来表示；一对多使用嵌套数组表示；多对多使用嵌套数组+冗余字段来表示；此外，也可以通过嵌套数组存id + 另一张表来表示实体间的关系，通过id来进行联表（使用`aggregate + $lookup`）

  **注意**：嵌套时要注意整个文档大小，限制是16M，读写比例，也要注意数组长度大小，一般会使用id引用模式来解决；**$lookup只支持left outer join**，不支持分片表

* 模式套用：

  * 场景：时序数据，解决：对于同一个实体，将其变化字段的存储和更新，聚合在内嵌数组中
  * 场景：大文档、多相似字段、多相似索引，比如sku的多属性，解决：将多个相似字段转成内嵌数组，索引建立在内嵌数组的实体字段中，
  * 场景：由于schemeless的特性，不同版本存在不一样的字段，需要对字段做校验，解决：增加一个版本字段，通过该字段进行判断
  * 场景：计数统计，每秒要进行计数更新，不需要准确的计数，解决利用mongo的随机计数器$inc，
  * 场景：精确统计，排行版，解决：使用预聚合字段，比如增加排名字段，每次更新依赖字段的同时，更新排名字段，进行排行查询时，直接根据排行字段进行排序即可，不用重新聚合

参考：[Mongo进阶 - 系统设计：模式构建](https://www.pdai.tech/md/db/nosql-mongo/mongo-y-doc.html)，这个更全，也挺实用的

# 调优

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
* 创建组合索引时，一般以能够  精确匹配 -》排序 -》范围匹配  的字段顺序进行创建
* 可以创建部分索引，比如对id>50的文档才会对id创建索引；对有值的字段才建索引，需要在创建索引时使用partialFilterExpression表达式
* 创建索引时使用后台创建索引，{background: true}

## 分析

### 慢查询

* 开启语句：`db.getProfilingLevel(1, 1000)，记录所有超过1000ms的语句`，profile的级别有0(关闭)、1(慢查询)、2(全部)
* 查询记录：`db.system.profile.find()`

### 查询语句上的分析

在查询语句中使用explain()方法分析查询语句，有三种分析模式，通过传参的方式使用，比如：`db.getCollection("表名").explain('executionStats').find({条件})`

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

### 库或集合上的分析

对表的或对库的，使用stats()方法，比如`db.getCollection("order").stats()，或者db.stats()`，可以查询整张表的信息或整个库的统计信息

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

# 存储引擎

## MMapv1

* 4.0版本开始被弃用
* 内存映射文件，不支持压缩，擅长于大容量插入、读取和就地更新的操作；
* 每一个数据库由一个.ns文件和一个或多个数据文件组成

  * .ns文件实际上是一个hash表，用于快速定位某个集合在数据文件中的起始位置；
  * 每个数据文件会被划分成多个extent，每个extent只包含一个集合的数据，一个extent包含多个文档，同一个extent中的所有文档使用双向链表连接；
* 为了保证连续的存储空间，避免产生磁盘碎片，MMAPv1对数据文件的使用采用预分配策略：数据库创建之后，先创建一个编号为0的文件，大小为64M，当这个文件有一半以上被使用时，再创建一个编号为1的文件，大小是上一个文件的两倍，即128M，依此类推，直到创建文件大小达到2G，以后再创建的文件大小就都是2G了；
* MMapv1引擎默认会将所有的redo log记录到硬盘上，每隔60秒写一次数据文件，每隔100毫秒写一次到日志文件，确保所有修改都能持久化到磁盘上；
* 锁是集合级别的；
* MMapv1引擎会最大限度的使用内存，分配给它的内存越大，性能越好；同时，内存由操作系统管理，如果其他进程需要内存，MMapv1会让出自己的空闲内存
* 文档按照写入顺序存储在磁盘上，由于文档可能会更新导致长度变长，如果原有存储空间不够，文档就需要进行迁移，导致集合中所有索引都要同步修改文档新的存储位置，影响性能，同时也反映出，如果文档没有索引，时无法保证文档在read中的自然顺序。有两种策略来避免文档迁移的情况发生：

> **基于paddingFactor（填充因子）的自适应分配方式**
>
> 　　这种方式会基于每个集合中的文档更新历史计算文档更新的平均增长长度，然后根据平均增长长度设置一个paddingFactor（填充因子，大小大于1）， 以后在新文档插入或旧文档移动时分配的空间=文档实际长度×paddingFactor。
>
> **基于usePowerOf2Sizes的预分配方式**
>
> 　　这种方式则不考虑更新历史，直接为文档分配比文档大小大而又最接近文档大小的2的N次方大小的存储空间（当大小超过2MB时则变为2MB的倍数增长），例如若文档大小为200Bytes则直接分配256Bytes的空间，这种方式也会产生一定的磁盘碎片。
>
> 　　对于第一种策略，由于每个文档大小不一，经过填充后的空间大小也不一样，如果集合上的更新操作很多，那么因为记录移动而导致的空闲空间会因为大小不一而难以重用。而第二种策略就不一样了，它分配的空间大小都是2的N次方，会更容易维护和利用，当那些删除或更新变大而产生的磁盘碎片，比如文档变大，需要迁移到新空间，此时就空间就会被标记为delete，这些被标记为删除的空间可以被insert重用，另外，由于预分配允许文档尺寸有限度的增长，而无需每次更新变大都重新分配空间。
>
> 所以，改进后的MMAPv1便抛弃了第一种策略，只使用较优的第二种策略。
>
> 另外，MongoDB还提供了一个“No Padding Allocation”策略，按照数据的实际尺寸分配空间，如果某个集合上绝大多数情况下执行的都是insert或者in-place update（更新后文档size不会变大），还有极少数的delete，那么可以在这个集合使用这个策略，提高磁盘空间利用率。

## InMemory

* 将所有数据都存在内存中，只有少量的元数据和诊断日志、临时数据存储到磁盘文件；
* 文档级别的锁，同一时刻多个写操作可以修改同一个集合中的不同文档，修改同一文档时会上锁；
* 将数据库的数据、索引和操作日志等内容存储到内存中。可以通过参数`--inMemorySizeGB`设置它占用的内存大小，默认为：50% of RAM - 1GB；
* 不需要单独的日志文件，不存在记录日志和等待数据持久化的问题。so宕机时，所有存储在内存中的数据都将会丢失；
* 数据虽然不会写入磁盘，但是会记录oplog；

## WiredTiger

* 3.0版本引入，3.2版本成为默认的存储引擎
* 采用插件式的存储引擎架构，既支持B+树，也支持[LSM树](https://juejin.im/post/6844903688075477000)，默认配置使用B+树；
* 设计了一个能充分利用CPU并行计算的内存模型的无锁并行框架，使其在多核CPU上表现优异；
* 实现了一套基于BLOCK/Extent的磁盘访问算法，使其在数据压缩和磁盘IO上表现优异；
* 实现基于snapshot的ACID事务，简化事务模型，摈弃传统事务锁隔离又能同时保证ACID；
* 实现了一种基于Hazard Pointer的LRU cache模型，充分利用内存容量的同时又能拥有很高的事务读写并发；
* 支持压缩，占用磁盘空间更小，有两种压缩模式：Snappy和Zlib

题外话关于LSM树：

LSM树，全称：Log-Structured Merge-Tree，是一种数据结构的设计思想，不是一种数据结构，一般基于B树及其变体实现，主要是将随机操作变为顺序操作，提升磁盘写速度，但会牺牲一些读性能；

LSM树由三部分组成：

1. MemTable，常驻于内存，用于保存最近更新的数据，按照Key有序组织这些数据，保证有序性通常会使用一些数据结构，比如红黑树，跳表等
2. Immutable MemTable，当MemTable达到一定大小后，会转换为Immutable MemTable，处于MemTable和SSTable的中间状态，写操作由新的MemTable处理，在转存过程中不阻塞数据更新操作。
3. SSTable，有序键值对集合，是LSM树在磁盘中的结构，为了加速读取，通常是通过建立key的索引 + 布隆过滤器来加快key的查找。

LSM树在数据插入、修改、删除等操作时，为了保证顺序写，是直接将操作记录追加到内存中（而B+树是直接修改原数据），当达到一定的数据量后，再批量写入磁盘，不断将Immutable MemTable刷盘持久化，变成SSTable，而不用去修改之前的SSTable中的key，保证顺序读写。由于是操作记录的追加写入，在不同的SSTable中，可能存在对相同的Key的记录，而只有最新的记录才是最准确的，因此需要对SSTable进行压缩，通过合并多个SSTable来清除冗余记录，读取时，也是倒序查询。

> LSM树有两种压缩策略：
>
> 1. size-tiered：保证每层SSTable的大小相近，同时限制每一层SSTable的数量。比如，每层限制SSTable为N，当每层SSTable达到N后，则触发Compact操作合并这些SSTable，并将合并后的结果写入到下一层成为一个更大的sstable。
>
>    由此可以看出，当层数达到一定数量时，最底层的单个SSTable的大小会变得非常大。并且size-tiered策略会导致空间放大比较严重。即使对于同一层的SSTable，每个key的记录是可能存在多份的，只有当该层的SSTable执行compact操作才会消除这些key的冗余记录。
>
> 2. leveled：也是采用分层的思想，每一层限制总文件的大小。但是跟size-tiered策略不同的是，leveled会将每一层切分成多个大小相近的SSTable。这些SSTable是这一层是全局有序的，意味着一个key在每一层至多只有1条记录，不存在冗余记录。之所以可以保证全局有序，是因为合并策略和size-tiered不同。
>
>    leveled策略相较于size-tiered策略来说，每层内key是不会重复的，即使是最坏的情况，除开最底层外，其余层都是重复key，按照相邻层大小比例为10来算，冗余占比也很小。因此空间放大问题得到缓解。但是写放大问题会更加突出。举一个最坏场景，如果LevelN层某个SSTable的key的范围跨度非常大，覆盖了LevelN+1层所有key的范围，那么进行Compact时将涉及Level N+1层的全部数据。

将LSM树存储结构抽象成两种树，比如有C0 tree，常驻在内存，另一个为C1 tree，结构常驻在硬盘，比如B+树，C1所有节点都是100%满的，节点的大小为磁盘块的大小；

插入新记录时，先在日志文件中以append的方式顺序插入操作日志；新记录的索引插入到C0中，在内存完成，不涉及磁盘IO，当C0大小达到某一阈值或每隔一段时间后，将C0中的记录滚动合并到磁盘C1中，如果C1体量太大就像C2合并，以此类推；合并时从C1中读取未合并叶子节点，从小到大找C0中的节点，合并排序，删除C0，循环执行，直至满了，就写入磁盘，从而达到将随机操作变成顺序操作；

查找时，先查C0，查不到就查C1，以此类推；

删除时，查询同查找逻辑，只是找到后只进行标记，等待异步删除；

参考：https://zhuanlan.zhihu.com/p/103968892、https://zhuanlan.zhihu.com/p/181498475、https://www.51cto.com/article/680775.html

### 数据文件在磁盘上的数据结构

B+树的leaf page包含一个：

* 页头(page header)：页的类型、页中实际载荷数据的大小、页中记录条数

* 块头(block header)：此页的checksum

* 真正的数据(key/value)：块在磁盘上的寻址位置等信息

WiredTiger有一个块设备管理的模块，用来为page分配block。如果要定位某一行数据（key/value）的位置，可以先通过block的位置找到此page（相对于文件起始位置的偏移量），再通过page找到行数据的相对位置，最后可以得到行数据相对于文件起始位置的偏移量offsets。由于offsets是一个8字节大小的变量，所以WiredTiger磁盘文件的大小，其最大值可以非常大(264bit)。

### 数据在内存上的数据结构

按需将磁盘上的数据以Page为单位加载到内存，一个page树就是一个checkpoint

> 内存里面B+Tree包含三种类型的page，即rootpage（根结点）、internal page（非叶子结点）和leaf page（叶子结点），前两者包含指向其子页的page index指针，不包含集合中的真正数据，leaf page包含集合中的真正数据即keys/values和指向父页的home指针；
>
> leaf page还维护了三个数组：
>
> * WT_ROW数组，表示从磁盘加载进来的数据数组，每条记录还有一个cell_offset变量，表示这条记录在page上的偏移量
> * WT_UPDATE数组，记录数据加载之后到下一个checkpoint间被修改的数据，每条被修改的记录都有一个数组元素对应，多次修改时，所有修改值以链表的形式保存（MVCC，相当于内存级别的oplog）
> * WT_INSERT_HEAD数组，表示数据加载之后到下一个checkpoint间新增的数据，每个插入的数据以跳表的形式组成，提高插入效率。

### checkpoint机制

checkpoint 相当于一个日志，记录上次checkpoint后相关数据文件的变化。

#### 作用

* 发生写操作时，只是将数据写入内存和journal日志，然后再通过check point机制将内存里面发生修改的数据写到数据文件进行持久化保存，确保数据一致性，同时也通过延迟持久化的方式，提升磁盘效率；
* 实现数据库在某个时刻意外发生故障，再次启动时，缩短数据库的恢复时间；

有点像MySQL中的change buffer + 持久化，优化写操作的速度和数据持久化的保证。

#### 触发时机

* 按一定时间周期：默认60s，执行一次checkpoint，所以DB宕机重启后可以快速恢复60s之前的数据，配合journal日志恢复60s之后的数据；

* 按一定日志文件大小：当 journal日志文件大小达到2GB（如果已开启），执行一次checkpoint；

* 任何打开的数据文件被修改，关闭时将自动执行一次checkpoint;

#### 执行流程

每个checkpoint包含一个root page、三个指向磁盘具体位置上pages的列表以及磁盘上文件的大小，采用copy on write的方式管理增删改，增删改操作会先缓冲在cache里，持久化时，不会在原来的叶子结点上进行，而是写入新分配的page，每次checkpoint都会产生一个新的root page。

![](https://github.com/Nixum/Java-Note/raw/master/picture/MongoDB_checkpoint.png)

![](https://github.com/Nixum/Java-Note/raw/master/picture/MongoDB_checkpoint_process.png)

1. 上排他锁，打开集合文件，读取最新的checkpoint数据；

   集合文件会按checkponit指定的大小被截取，so如果此时发生系统故障，恢复时可能会丢失checkpoint之后的数据（如果没有开启journal，如果有，是就会通过journal日志来重放）；

2. 在内存构造一棵包含root page的live tree，表示当前可修改的checkpoint结构，用来跟踪后面写操作引起的文件变化，其他历史的checkpoint信息只能进行读或删除；

3. 内存里的page随着被增删改后，写入磁盘里按需分配的page时，会从live tree中的available列表中选取可用的page供其使用，随后，这个新的page被加入到checkpoint的allocated列表中；

4. 如果一个checkpoint被删除时，它所包含的allocated和discarded两个列表信息将被合并到最新checkpoint对应的列表上，任何不再需要的磁盘pages，也会将其引用添加到live tree的availabe列表中；

5. 删除旧checkpoint；

6. 根据此时的live tree生成新的checkpoint，当新的checkpoint生成时，会重新刷新其allocated、available、discard三个列表中的信息，并计算此时集合文件的大小以及root page的位置、大小、check sum等信息；

7. 将新生成的信息作为checkpoint元信息写入磁盘中的文件；

   生成的checkponit默认名为WiredTigerCheckpoint，如果不明确指定其它名称，则新check point将自动取代上一次生成的checkpoint。

### 事务实现

3.2版本有支持单文档原子操作了，主要是用在单文档嵌套更新上，默认自动的；4.0版本支持复制集多文档事务，4.2版本支持分片事务。

另外，Spring 5.1.1 / SpringBoot 2.x 以上 @Transactional可以支持Mongo，需要为TransactionManager注入MongoDBFactory

* MVCC：以本次trancsaction_id + 本次修改后的value组成一个结点，每次修改都会追加到链表的头部，每次读取时根据trancsaction_id和本次事务的snapshot来找到对应的历史版本，该链表挂在叶子结点的WT_UPDATE字段中；
* snapshot：拉取当前事务之前的数据快照，确定当前事务哪些数据可见，哪些不可见，确定事务状态；
* operation_array：本次事务中已执行的操作列表，用于事务回滚，类似MySQL的undo log；
* redo log，即journal日志：记录当前操作，一个mongoDB实例中的所有DB共享journal文件；
* trancsaction_id：全局唯一事务id，通过cas自增生成；
* 全局事务管理器：管理系统所有事务，用数组保存所有历史事务对象transaction_array，用于snapshot的创建，扫描transaction_array时使用cas保证并发安全；
* namespace文件：默认大小为16M，主要用于保存集合、索引的属性、命令信息等字段；

#### 事务执行流程

1. 事务开启：创建事务对象，加入全局事务管理器，确定事务的隔离级别和redo log的刷盘方式，并将事务状态设置为 执行 ，如果事务隔离级别是snapshot-Isolation，则在本次事务执行前创建一个snapshot。
2. 事务执行：如果是读操作，则不做任何记录；如果是写操作，生成trancsaction_id，设置当前事务状态为HAS_TXN_ID，记录当前操作，保存到operation_array中；记录修改记录和trancsaction_id，保存mvcc的链表中；写入一条redo log到本地事务对象的redo_log_buf中。
3. 事务提交：将redo_log_buf中的数据写入redo log file日志文件中，并将redo log file持久化到磁盘，清除提交事务对象的snapshot对象，修改当前提交的事务的transaction_id状态为WT_TNX_NONE，保证其他事务在创建snapshot时看到本次事务的状态是已提交
4. 事务回滚：遍历operation_array，对每个数组单元对应update的transaction_id状态设置以为WT_TXN_ABORTED，标示mvcc 对应的修改单元值被回滚，在其他读事务进行mvcc读操作的时候，跳过这个放弃的值即可

多文档事务错误处理机制

- 当一个事务开始后，如果事务要修改的文档在事务外部被修改过，则事务修改这个文档时会触发Abort错误，因为此时的修改存在冲突，这种情况下，事务终止，应用端需要重做事务。
- 如果一个事务已经开始修改一个文档，在事务以外尝试修改同一个文档，则事务以外的修改会等待事务完成才能继续进行。

#### 事务隔离级别

不同于MySQL，MongoDB的Wired Tiger的事务均基于snapshot实现了。

* Read-Uncommited：未提交读，事务开启时，可以读到别的事务最新修改但还未提交的值，会出现脏读；

* Read-Commited：提交读，默认，事务开启时，只能读到别的事务最新修改且已提交的值，这种隔离级别下可能在一个长事务多次读取一个值时前后读取不一致，出现不可重复读现象；

  WT引擎会在当前事务下，执行每一个操作前都对系统中的事务做一次snapshot，然后在这个snapshot上读写；

* Snapshot-Isolation：快照隔离，事务开启时，只能读到当前事务前最后提交的值，整个事务期间只能看到这个版本的snapshot，不管这个值在此期间被其他事务修改了多少次，防止不可重复读；

因为Mongo中的事务是真的单一文档实现，所以不存在幻读现象。当有事务同时对同一文档做修改时还是会加锁，行锁的规则跟MySQL类似，也是分读写锁和意向读写锁。

> 但实现原理不太一致
>
> 为了解决写写冲突，写操作写入时，会做校验，确保从读开始没有其他写操作修改相应数据。如果有了修改，就重新执行整个写操作。这样就减少了资源被锁住的时间，成本是多个写操作修改相同数据时，有可能会发生多次重试。对于读多写少，写冲突少的情况，这种交换是合适的。这种并发处理机制称为乐观并发控制，即乐观锁。和悲观锁相比，通过增加校验和重试机制，放弃了在整个过程上加锁。
>
> insert：库级别的意向读锁(r)，表级别的意向读锁(r)，文档级别的读锁(R)
>
> update：库级别的意向写锁(w)，表级别的意向写锁(w)，文档级别的写锁(W)
>
> foreground方式创建索引：库级别的写锁(W)
>
> background方式创建索引：库级别的意向写锁(w)，表级别的意向写锁(w)
>
> 锁是公平锁，所有的请求会排队获取相应的锁。但是mongodb为了优化吞吐量，在执行某个请求时，会同时执行和它相容的其他请求。比如一个请求队列需要的锁如下，执行IS请求的同时，会同时执行和它相容的其他S和IS请求。等这一批请求的S锁释放后，再执行X锁的请求。
>
> 这种处理机制保证了在相对公平的前提下，提高了吞吐量，不会让某一类请求长时间的等待。对于长时间的读或者写操作，某些条件下，mongodb会临时的让渡锁，以防止长时间的阻塞。

#### 事务日志

Wired Tiger引擎在保证事务的持久化可靠性上通过redo log实现，只记录事务过程中对文档的修改，以追加的方式写入wt_transaction对象的redo_log_buf中，等到事务提交时将这个redo_log_buf以同步的方式写入WT的重做日志的磁盘文件中。

如果数据库程序发生异常或者崩溃，可以通过上一个checkpoint的位置重演磁盘上这个磁盘文件来恢复已经提交的事务来保证事务的持久性。

WT引擎通过LSN(LogSequence Number日志序列号)来管理redo log，redo log的操作对象叫logrec，对应提交的事务，是一个二进制buffer，事务的每个操作被记录成logop对象，一个logrec包含多个logop，logrec在刷入磁盘之前会进行空间压缩。

redo log以WAL的方式写入日志，即事务过程中所有的修改在提交前需要将其对应的redo log写入磁盘文件，之后再进行事务提交，事务执行完成。

为了减少日志刷盘频繁操作IO，其刷盘逻辑类似MySQL，会将同时发生的事务日志合并到一个slotbuffer中，先完成合并的事务线程会同步等待一个完成刷盘信号，最后完成日志数据合并的事务线程将slotbuffer中的所有日志数据sync到磁盘并通知这个slotbuffer中等待其他事务线程刷盘完成。并发事务的logrec合并到slotbuffer通过cas保证并发安全。

#### 事务恢复

> 事务的redo log主要是防止内存中已经提交的事务修改丢失，但如果所有的修改都存在内存中，随着时间和写入的数据越来越多，内存就会不够用，这个时候就需要将内存中的修改数据写入到磁盘上，一般在WT中是将整个BTREE上的page做一次checkpoint并写入磁盘。
>
> WT中的checkpoint是一个append方式管理的，也就是说WT会保存多个checkpoint版本。不管从哪个版本的checkpoint开始都可以通过重演redo log来恢复内存中已提交的事务修改。整个重演过程就是就是简单的对logrec中各个操作的执行。这里值得提一下的是因为WT保存多个版本的checkpoint，那它会将checkpoint做为一种元数据写入到元数据表中，元数据表也会有自己的checkpoint和redo log，但是保存元数据表的checkpoint是保存在WiredTiger.wt文件中，系统重演普通表的提交事务之前，先会重演元数据事务提交修改。
>
> WT的redo log是通过配置开启或者关闭的，MongoDB并没有使用WT的redolog来保证事务修改不丢，而是采用了WT的checkpoint和MongoDB复制集的功能结合来保证数据的完整性的。大致的细节是如果某个mongoDB实例宕机了，重启后通过MongoDB的复制协议将自己最新checkpoint后面的修改从其他的MongoDB实例复制过来。

### 缓存实现

WT引擎内部使用**LRU cache作为缓存模型**，采用分段扫描和hazardpointer的淘汰机制，充分利用现代计算机超大内存容量的特性来提高事务读写并发。在高速不间断写入内存操作非常快，但是由于内存中的数据最终需要写入磁盘，因为写内存的速度远高于写磁盘，最终可能导致间歇性写挂起的现象出现。

WT内部将内存划分为3块：

* 存储引擎内部的cache，默认大小是`MAX((RAM - 1G)/2, 256M)`，即如果16G，就会有7.5G给WT内部当cache；
* 索引cache，默认500M；
* 文件系统cache，利用的是操作系统的文件系统缓存，目的是减少内存与磁盘的交互；

当cache空间不足时，会进行淘汰，淘汰的时机由`eviction_target：内存使用量`和`eviction_dirty_target：内存脏数据量`来控制，超过一定的阈值后触发。

缓解读写挂起的方法：

* WT 2.8版本不再分为预前刷盘和checkpoint刷盘，而是采用逐个对B+树直接做checkpoint刷盘，避免evict page的拥堵，缓解OS cache缓冲太多的文件脏数据问题
* 使用direct IO
* 多个磁盘存储，redo log文件单独放在一个磁盘上，数据放在另外的磁盘上，避免redo log和checkpoint发生刷盘竞争
* 使用SSD

# 复制集读写关注

mongo是分布式数据库，通过部署多复制集来实现，单个MongoDB server不支持复制集读写关注，至少需要一主一从两个。

## 分布式下的写操作

对于事务内多文档执行写操作，保证原子性，失败能回滚

### writeConcern参数

通过参数writeConcern来保证写操作到达多少个节点才算成功，写操作会等到写到复制集后才会返回

使用：nosql语句里包含`{writeConcern: {w: 1, timeout: 3000}}`，单位毫秒，超时不意味着都写失败，有可能已经写入了一些节点了，写操作性能会取决于节点的数量，会影响写入的时间，但对集群不会有压力

* 默认值为1，表示数据被写入到一个节点就算成功
* =[数字x]，表示数据被写入到x个节点就算成功
* =0，表示发出的写操作，不关心是否成功
* =majority（推荐）表示发出的写操作需要写入到多数复制集上才算成功，一般写操作写入主节点，由主节点同步给从节点
* =all，需要全部节点确认，但如果同步时有节点挂了，此时写操作会被阻塞

### journal参数

写操作时，会按操作顺序先写到内存，再写入磁盘（journal日志文件和数据文件），参数journal字段来保证达到哪步操作才算成功，由配置文件里`storage.journal.enabled`控制是否开启，`storage.journal.commitInternalMs`决定刷盘时间间隔

使用：nosql语句里包含 `{journal: {j: true}}` 会保证每次写入都会进行刷盘

* =true，表示写操作要落到journal日志文件才算成功
* =false，表示写操作到达内存就算成功

### journal日志与oplog日志的区别

* **journal日志：即redo log **，由MongoDB引擎层使用，**主要用于控制写操作是否立即持久化**，因为如果发生写操作，mongo一般是先将写操作写入内存，在**定时（默认是1分钟）通过check point机制将内存里的数据刷盘持久化**。

  如果写操作的成功判定出现在写完内存后，如果此时宕机，将会丢失写操作的记录，如果判定发生在写完journal日志之后，如果宕机可以利用journal日志进行恢复。

  **写入journal日志时，也是先写入内存，再根据写入buffer的大小（默认是100M）或者 定时（默认是100ms）写入磁盘。**

* **oplog日志**：只用在主从复制集的使用，通过**oplog来实现节点间的数据同步**，从节点获取主节点的oplog日志后进行重放，同步数据。oplog本身在MongoDB里是一个集合。

  mongo的一次写操作，包括 1.将文档数据写进集合； 2.更新集合的索引信息； 3.写入oplog日志，（此时只是写入内存），这三个步骤是原子性的，要么成功要么失败。一次写操作可以是批量的也可以是单条，一次写操作对应一条journal日志。
  
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

读操作时，决定这个节点的数据哪些是可读的，**类似MySQL中的隔离**

使用：需要在mongo的配置文件的server参数中，增加`enableMajorityReadConcern: true`，

对于值枚举为available、local、majority、linearizable，需要在查询语句后 + .readConcern("local或者其他枚举")开启使用；

对于snapshot，则是在开启事务时指定，`var session = db.getMongo().startSession(); session.startTransaction({readConcern: {level: "snapshot"}, writeConcern: {w: "majority"}});`  session作为事务对象，配合snapshot快照、MVCC、redo log来实现事务。

* =available：读取所有可用数据

* =local（默认）：读取所有可用且属于当前分片的数据，类似available，只是限制在分片上，

  mongo为了实现数据均衡，有分片chunk迁移机制，当分片1将数据迁移至分片2，但还没迁移完成时，此时两个分片都存在该chunk数据，但是此时该数据仍属于分片1，=local时不能读到分片2的该数据，=available时则可以

* =majority：读取在多数节点上提交完成的数据，通过MVCC机制实现，**作用类似MySQL中的提交读隔离级别**。

  比如，当读操作指向主节点，且写操作已经同步到多数从节点时，才可以读到该数据；当读操作指向一个从节点，且从节点完成写操作，且通知了主节点并得到主节点的响应时，才能读到该数据。

  作用：

  1. 主要是防止分布式数据库脏读，这里脏读指的是，在一次写操作到达多数节点前读取了这个写操作，又由于故障之类的导致写操作回滚了，此时读到的数据就算脏读。
  2. 配合writeConcern=majority来实现读写分离，向主节点写入数据，从节点也能读到数据

* =linearizable：线性读取文档，性能较差，**作用类似MySQL中的serializable(串行化)隔离级别**。

  只对读取单个文档时有效，保证在写操作完成后一定可以读到，一次读操作需要所有节点响应

* =snapshot：读取最近快照中的数据，**作用类似MySQL中的可重复读隔离级别**，事务默认60s，需要4.2的驱动，会影响chunk迁移，多文档读操作必须使用主节点读

  * 全局事务管理器通过自旋创建快照
  * 实现在同一事务下读取同一数据的一致性，也可以读取同一事务内的写操作；
  * 当多事务内出现写操作冲突时，排在后面事务的写操作会失败，触发abort错误，此时只能把该事务抛弃，再重新开始事务
  * 当事务外和事务内的写操作冲突，事务内的写操作早于事务外的写操作，事务外的写操作会进入等待，直到事务内提交，才会执行

# 复制集Replica Set机制

一般是一主多从，所有实例存储相同数据

## 特点

* 数据分发，将数据从一个区域复制到另一区域，减少另一区域的读延时
* 读写分离
* 异地容灾，快速切换可用节点
* 复制集主要用于实现高可用，增加复制集的节点数目并不会提升写性能，因为写操作都发生在主节点上，但可以提升读性能。提升性能一般是使用分片的机制

## 节点分类

除了常见的主从节点，MongoDB还支持 仲裁节点，只用于选举投票，其作用是在当部署了偶数个节点的复制集时，就算有一台节点宕机，也能选出主节点。仲裁节点本身不存储数据，非常轻量化。

另外，从节点也可分化出另外两种节点

* Priority0从节点：一般作为备用节点，当无法在合理时间内添加新成员节点时，实现替换作为过渡；priority0节点的选举优先级为0，表示不会被选举成主节点；

* hidden从节点：隐藏节点不会收到任何写请求，即使设置为复制集读选项，一般用于报表节点、备份节点进行数据备份、离线计算等任务，不影响复制集的其他服务；

  hidden节点的priority也是0，无法被选为主节点，对driver不可见。

* delay从节点：延时节点，同时也是hidden节点，其数据会落后于主节点一段时间，用于帮助我们在人为误操作或其他意外情况下进行数据恢复。

## 选举时机

* 往复制集中加入新节点
* 初始化复制集
* 对复制集进行维护，如`rs.stepDown(时间)，在多少时间后将当前主库降级`或`rs.reconfig()重新配置现有复制集`操作时
* 从节点失联，默认超时时间是10s

## 机制

* 复制集中的数据同步，有两种形式：初始化同步(Initial Sync)，即全量同步；复制(Replication)：增量同步；

* 一般部署奇数个复制集，至少三个，且均要有投票权，一般分为一个主节点，用于接受写入操作和选举时的投票，两个从节点，复制主节点上的数据和选举时的投票

* 主节点上所有的写操作，会被记录到oplog日志中，从节点通过在主节点上打开一个tailable游标不断获取新进入主节点的oplog，进行重放，实现与主节点的数据一致。（异步）

  oplog(操作日志)是一个特殊的有上限的集合(老的日志会被overwrite)，它保存所有修改数据库中存储的数据的操作的滚动记录。

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

  mongos、configServer、shard均以复制集为单位，部署多个复制集，实现高可用

  shard：分片，真正的数据存储位置，以chunk为单位存数据，每个分片可部署为一个复制集

  mongos：查询路由，提供客户端和分片集群之间的接口

  configServer：存储元数据和配置数据

* 数据存储时会自动均衡，当mongo发现数据存储分布不均衡时，会做chunk迁移，chunk的概念类似MySQL中的页，一个chunk包含多个文档 = 一页中包含多行记录

  * chunk的大小默认是64M，如果超过，chunk会进行分裂，如果单位时间存储需求很大，就设置更大的chunk。chunk的大小会影响迁移的速度，小的chunk迁移速度快，数据分配均匀，但是数据分裂频繁，路由节点消耗资源更多；大的chunk分裂少，但是迁移时更耗资源。
  * chunk的分裂和迁移非常消耗IO资源；
  * chunk在插入和更新时进行分裂，读数据不会分裂

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

[MongoDB存储引擎（上）——MMAPv1](https://www.cnblogs.com/wujuntian/p/8431674.html)

