# JVM内存模型

![JVM内存模型](https://github.com/Nixum/Java-Note/raw/master/Note/picture/JVM内存模型.png)

**方法区也叫永久代，持久代，非堆，不算在堆里面**

**年轻代也叫新生代**

注意区别于Java内存模型

JVM内存模型描述的是线程运行时的数据在内存的分布

Java内存模型是多线程情况下数据的分布

# 引用类型

* 强引用：通过new的方式创建，不会被轻易回收
* 软引用（SoftReference）：被软引用关联的对象只有在内存不够时才会被回收
* 弱引用（WeakReference）：被弱引用关联的对象一定会被回收，只能存活至下次垃圾回收发生之前
* 虚引用（PhantomReference）：比如将对象引用设置为null，该引用指向的对象就会被回收，相当于告知JVM可以回收该对象

# 垃圾回收算法

# 垃圾收集器

一般关注CMS和G1，有时间再补一下

# 内存分配和回收策略

> ### 1. 对象优先在 Eden 分配
>
> 大多数情况下，对象在新生代 Eden 上分配，当 Eden 空间不够时，发起 Minor GC。
>
> ### 2. 大对象直接进入老年代
>
> 大对象是指需要连续内存空间的对象，最典型的大对象是那种很长的字符串以及数组。
>
> 经常出现大对象会提前触发垃圾收集以获取足够的连续空间分配给大对象。
>
> -XX:PretenureSizeThreshold，大于此值的对象直接在老年代分配，避免在 Eden 和 Survivor 之间的大量内存复制。
>
> ### 3. 长期存活的对象进入老年代
>
> 为对象定义年龄计数器，对象在 Eden 出生并经过 Minor GC 依然存活，将移动到 Survivor 中，年龄就增加 1 岁，增加到一定年龄则移动到老年代中。
>
> -XX:MaxTenuringThreshold 用来定义年龄的阈值。
>
> ### 4. 动态对象年龄判定
>
> 虚拟机并不是永远要求对象的年龄必须达到 MaxTenuringThreshold 才能晋升老年代，如果在 Survivor 中相同年龄所有对象大小的总和大于 Survivor 空间的一半，则年龄大于或等于该年龄的对象可以直接进入老年代，无需等到 MaxTenuringThreshold 中要求的年龄。
>
> ### 5. 空间分配担保
>
> 在发生 Minor GC 之前，虚拟机先检查老年代最大可用的连续空间是否大于新生代所有对象总空间，如果条件成立的话，那么 Minor GC 可以确认是安全的。
>
> 如果不成立的话虚拟机会查看 HandlePromotionFailure 的值是否允许担保失败，如果允许那么就会继续检查老年代最大可用的连续空间是否大于历次晋升到老年代对象的平均大小，如果大于，将尝试着进行一次 Minor GC；如果小于，或者 HandlePromotionFailure 的值不允许冒险，那么就要进行一次 Full GC。
>
> ## Full GC 的触发条件
>
> 对于 Minor GC，其触发条件非常简单，当 Eden 空间满时，就将触发一次 Minor GC。而 Full GC 则相对复杂，有以下条件：
>
> ### 1. 调用 System.gc()
>
> 只是建议虚拟机执行 Full GC，但是虚拟机不一定真正去执行。不建议使用这种方式，而是让虚拟机管理内存。
>
> ### 2. 老年代空间不足
>
> 老年代空间不足的常见场景为前文所讲的大对象直接进入老年代、长期存活的对象进入老年代等。
>
> 为了避免以上原因引起的 Full GC，应当尽量不要创建过大的对象以及数组。除此之外，可以通过 -Xmn 虚拟机参数调大新生代的大小，让对象尽量在新生代被回收掉，不进入老年代。还可以通过 -XX:MaxTenuringThreshold 调大对象进入老年代的年龄，让对象在新生代多存活一段时间。
>
> ### 3. 空间分配担保失败
>
> 使用复制算法的 Minor GC 需要老年代的内存空间作担保，如果担保失败会执行一次 Full GC。具体内容请参考上面的第 5 小节。
>
> ### 4. JDK 1.7 及以前的永久代空间不足
>
> 在 JDK 1.7 及以前，HotSpot 虚拟机中的方法区是用永久代实现的，永久代中存放的为一些 Class 的信息、常量、静态变量等数据。
>
> 当系统中要加载的类、反射的类和调用的方法较多时，永久代可能会被占满，在未配置为采用 CMS GC 的情况下也会执行 Full GC。如果经过 Full GC 仍然回收不了，那么虚拟机会抛出 java.lang.OutOfMemoryError。
>
> 为避免以上原因引起的 Full GC，可采用的方法为增大永久代空间或转为使用 CMS GC。
>
> ### 5. Concurrent Mode Failure
>
> 执行 CMS GC 的过程中同时有对象要放入老年代，而此时老年代空间不足（可能是 GC 过程中浮动垃圾过多导致暂时性的空间不足），便会报 Concurrent Mode Failure 错误，并触发 Full GC。

**为什么有了 对象达到年龄限制后晋升的机制 还要有 动态年龄判定的机制**

* 如果MaxTenuringThreshold设置过大，会导致本该晋升到老年代的对象一直停留在Survivor区，直到Survivor溢出，这样对象老化机制就失效了

* 如果MaxTenuringThreshold设置过小，过早晋升的对象不能在年轻代充分回收，大量对象进入老年代，会引起频繁的Major GC

**关于MinorGC、Major、YoungGC、FullGC的说明**

* MinorGC：清理年轻代，等同于YoungGC，叫法不同而已

* MajorGC：清理老年代

* FullGC：清理整个堆空间 - 包括年轻代和老年代

# 调优

* JVM调优，一般是代码已经优化到了一定程度了，到了最后阶段才会进行JVM调优

* 对于Minor GC和Major GC频繁的优化，扩大Eden区，虽然可以降低Minor GC次数，但由于扫描的区域变大了，Minor GC时间可能会变长，但这点影比 当对象gc后仍然存活，需要复制到Survivor区带来的影响要小，影响Minor GC次数和时间的因素是每次GC后对象的存活数量，因此对于短期对象较多时，增加Eden区大小，同理，如果对象存活时间比较长、对象较多时，增加老年代大小

# 常用参数

只列举了常见的，参数大致分为三类：

## 行为参数：改变JVM基础行为

| 参数                      | 含义                                        | 说明                                               |
| ------------------------- | ------------------------------------------- | -------------------------------------------------- |
| -XX:+ScavengeBeforeFullGC | FullGC前触发一次MinorGC                     | 默认启用                                           |
| -XX:+UseGCOverheadLimit   | GC耗时过长，会跑OOM                         | 默认启用                                           |
| -XX:-UseConcMarkSweepGC   | 使用CMS低停顿垃圾收集器，减少FullGC暂停时间 | 默认不启用                                         |
| -XX:-UseParallelGC        | 启用并行GC                                  | 默认不启用                                         |
| -XX:-UseParallelOldGC     | 年轻代和老年代都使用并行垃圾收集器          | 默认不启用，当-XX:-UseParallelGC启用时该项自动启用 |
| -XX:-UseSerialGC          | 启用串行垃圾收集器                          | -Client时启用，默认不启用                          |
| -XX:+UseThreadPriorities  | 启用本地线程优先级                          | 默认启用                                           |

## 性能调优：JVM性能调优参数

| 参数                        | 含义                                       | 说明，有些默认值在不同环境下是不同的                         |
| --------------------------- | ------------------------------------------ | ------------------------------------------------------------ |
| -Xms                        | 整个堆的初始大小                           | 默认值：物理内存的1/64                                       |
| -Xmx                        | 整个堆的最大值                             | 默认值：物理内存的1/4                                        |
| -Xmn                        | 年轻代大小                                 | 设置该值等同于设置了-XX:NewSize和-XX:MaxNewSize，且两者相等，官方推荐是整个堆的3/8 |
| -XX:NewSize                 | 年轻代大小                                 |                                                              |
| -XX:MaxNewSize              | 年轻代最大值                               |                                                              |
| -Xss                        | 每个线程的栈大小                           | JDK1.5以后该值默认为1M                                       |
| -XX:PermSize                | 永久代大小                                 | 默认值：物理内存的1/64                                       |
| -XX:MaxPermSize             | 永久代最大值                               | 默认值：物理内存的1/4                                        |
| -XX:NewRatio                | 年轻代与老年代的比值                       | 默认值：2，年轻代包括Eden区和两个Survivor区，老年代不包括永久代。比如=4，表示年轻代：老年代=1：4，即年轻代占整个堆的1/5 |
| -XX:SurvivorRatio           | 年轻代里Eden区与两个Survivor的比值         | 默认值：8，表示一个Eden区：两个Survivor区的比值是8：2，一个Survivor区占整个年轻代的1/10 |
| -XX:SoftRefLRUPolicyMSPerMB | 每兆堆空闲空间中软引用的存活时间           | 默认值：1s                                                   |
| -XX:MaxTenuringThreshold    | 对象在年轻代的最大年龄                     | 默认值：15，即对象在年轻代熬过了15次Minor GC，达到阈值后晋升到老年代。=0时，对象初始化直接进入老年代 |
| -XX:PretenureSizeThreshold  | 对象超过多大直接在老年代中分配             | 默认值：0                                                    |
| -XX:TLABWasteTargetPercent  | TLAB(线程本地缓冲区)占Eden区的比例         | 默认值：1%                                                   |
| -XX:+CollectGen0First       | FullGC时是否先YGC                          | 默认值：false                                                |
| -XX:MinHeapFreeRatio        | GC后堆中空闲量占的最小比例                 | 默认值：40                                                   |
| -XX:MaxHeapFreeRatio        | GC后堆中空闲量占的最大比例                 | 默认值：70，GC后，如果发现空闲堆内存占到整个预估上限值的70%，则收缩预估上限值 |
| -XX:PreBlockSpin            | 自旋锁自选次数，-XX:+UseSpinning需要先启用 | -XX:+UseSpinning默认启用，自旋次数默认值：10次               |

## 调试参数：打开堆栈跟踪、打印、输出JVM参数，显示详细信息

| 参数                                           | 含义                                |
| ---------------------------------------------- | ----------------------------------- |
| -XX:ErrorFile=日志路径/日志文件名称.log        | 保存错误日志或者数据到文件中        |
| -XX:HeapDumpPath=堆信息文件路径/文件名称.hprof | 指定导出堆信息时的路径或文件名      |
| -XX:-HeapDumpOnOutOfMemoryError                | 当首次遭遇OOM时导出此时堆中相关信息 |
| -XX:-PrintGC                                   | 每次GC时打印相关信息                |
| -XX:-PrintGCDetails                            | 每次GC时打印详细信息                |
| -XX:-PrintGCTimeStamps                         | 打印每次GC的时间戳                  |
| -XX:-TraceClassLoading                         | 跟踪类的加载信息                    |
| -XX:-TraceClassLoadingPreorder                 | 跟踪被引用到的所有类的加载信息      |
| -XX:-TraceClassResolution                      | 跟踪常量池                          |
| -XX:-TraceClassUnloading                       | 跟踪类的卸载信息                    |

# 调优工具

## 命令行工具

### jps：虚拟机进程状况工具

用来查看机器上的Java进程，如pid，启动时的JVM参数，启动时的主类、jar包全路径名称，类似ps命令

```
无参数：显示进程ID和类名称
-q：只输出进程ID
-m：输出传入 main 方法的参数，即main方法的String[] args
-l：输出完全的包名，应用主类名，jar的完全路径名
-v：输出启动时带的jvm参数
```

### jstat：虚拟机统计信息监视工具

一般用来查看堆内gc情况，比如年轻代、老年代大小、YGC次数，平均耗时等

https://www.jianshu.com/p/213710fb9e40

### jmap：Java内存印象工具

用来查看堆内存的使用情况，比如输出内存中的所有对象，可以配合eclipse MAT分析内存泄漏情况

https://www.cnblogs.com/huanglog/p/10302901.html

官方的：https://docs.oracle.com/javase/7/docs/technotes/tools/share/jstat.html

### jhat：虚拟机堆转储快照分析工具

分析由jmap导出来的堆dump文件，作用类似Eclipse MAT，但是没MAT直观

### jstack：Java堆栈跟踪工具

查看方法或线程的执行情况，线程的堆栈信息，死锁检测，死锁原因

https://blog.csdn.net/wufaliang003/article/details/80414267

官方：https://docs.oracle.com/javase/7/docs/technotes/tools/share/jstack.html

### jinfo：Java配置信息工具

实时查看和调整JVM各项参数配置，进程运行时也能改JVM的配置

```
jinfo -sysprops [pid] 查看当前JVM全部系统属性
jinfo -flags [pid] 查看进程所有JVM参数，比jps -v更详细
jinfo -flag [[+代表打开，-代表关闭，都不写代表查看][JVM参数][赋值使用=][JVM参数值]] [pid]
```

## 可视化工具

### JConsole

监控Java应用程序，可查看概述、内存、线程、类、VM、MBeans、CPU、堆栈内容、死锁检测

### VisualVM

功能比JConsole更加强大，支持插件，还能看到年轻代、老年代的内存变化，以及gc频率、gc的时间

### Eclipse MAT

工具进行内存快照的分析，图表的方式展示，可以分析内存泄漏或溢出出现的代码段

# 参考

 [CS-Note](https://cyc2018.github.io/CS-Notes/#/notes/Java%20%E8%99%9A%E6%8B%9F%E6%9C%BA)

深入理解 Java 虚拟机 - 周志明

[JVM（三）调优工具](https://www.cnblogs.com/warehouse/p/9479104.html)

[jvm系列(七):jvm调优-工具篇](https://www.cnblogs.com/ityouknow/p/6437037.html)

[从实际案例聊聊Java应用的GC优化](https://tech.meituan.com/2017/12/29/jvm-optimize.html)

[JVM -XX: 参数介绍](https://www.cnblogs.com/langtianya/p/3898760.html)
