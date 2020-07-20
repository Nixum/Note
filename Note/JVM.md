# JVM内存模型

![JVM内存模型](https://github.com/Nixum/Java-Note/raw/master/Note/picture/JVM内存模型.png)

注意区别于Java内存模型

JVM内存模型描述的是线程运行时的数据在内存的分布

Java内存模型是多线程情况下数据的分布

# 垃圾回收算法

# 垃圾收集器

# 内存分配和回收策略

# 常用参数

 	

# 调优工具

## 命令行工具

* jps：虚拟机进程状况工具

* jstat：虚拟机统计信息监视工具

* jmap：Java内存印象工具

* jhat：虚拟机堆转储快照分析工具

* jstack：Java堆栈跟踪工具

* jinfo：Java配置信息工具

## 可视化工具

* JConsole：监控Java应用程序，可查看概述、内存、线程、类、VM、MBeans、CPU、堆栈内容、死锁检测
* VisualVM：功能比JConsole更加强大，支持插件，还能看到年轻代、老年代的内存变化，以及gc频率、gc的时间
* Eclipse MAT：工具进行内存快照的分析，图表的方式展示，可以分析内存泄漏或溢出出现的代码段

# 参考

 [CS-Note](https://cyc2018.github.io/CS-Notes/#/notes/Java%20%E8%99%9A%E6%8B%9F%E6%9C%BA)

深入理解 Java 虚拟机 - 周志明

[JVM（三）调优工具](https://www.cnblogs.com/warehouse/p/9479104.html)

[jvm系列(七):jvm调优-工具篇](https://www.cnblogs.com/ityouknow/p/6437037.html)