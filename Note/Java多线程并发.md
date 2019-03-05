# 线程与进程的区别

## 进程

可以简单理解为一个应用程序，进程是资源分配的基本单位。

## 线程

线程是独立调度的基本单位。一个进程中可以有多个线程，线程之间共享进程资源。

## 区别

1. 拥有资源

   进程是资源分配的基本单位，但是线程不拥有资源，线程可以访问隶属进程的资源。

2. 调度

   线程是独立调度的基本单位，在同一进程中，线程的切换不会引起进程切换，从一个进程中的线程切换到另一个进程中的线程时，会引起进程切换。

3. 系统开销

   由于创建或撤销进程时，系统都要为之分配或回收资源，如内存空间、I/O 设备等，所付出的开销远大于创建或撤销线程时的开销。类似地，在进行进程切换时，涉及当前执行进程 CPU 环境的保存及新调度进程 CPU 环境的设置，而线程切换时只需保存和设置少量寄存器内容，开销很小。

4. 通信方面

   线程间可以通过直接读写同一进程中的数据进行通信，在java中如使用共享变量、wait/notify机制、管道；但是进程通信需要借助管道、消息队列、共享存储、信号量、信号、套接字socket

# 线程的状态

## 1.状态转换

![线程状态转换](https://github.com/Nixum/Java-Note/blob/master/Note/picture/线程状态转换.png)

“阻塞”与“等待”的区别：
“阻塞”状态是等待着获取到一个排他锁，进入“阻塞”状态都是被动的，离开“阻塞”状态是因为其它线程释放了锁，不阻塞了；
“等待”状态是在等待一段时间 或者 唤醒动作的发生，进入“等待”状态是主动的

## 2. 状态

* New（新建）：通过new创建一个新线程，但还没运行，还有一些其他基础工作要做
* Runnable（可运行，就绪）：线程调用start方法，可能处于正在运行也可能处于没有运行，取决于操作系统提供的运行时间
* Running（运行）
* Blocked（阻塞）：线程已经被挂起，等待锁的释放，直到另一个线程走完临界区或发生了相应锁对象wait()操作后，它才有机会去争夺进入临界区的权利
  1. 等待阻塞：运行的线程执行wait()方法，JVM会把该线程放入等待池中。
  2. 同步阻塞：运行的线程在获取对象的同步锁时，若该同步锁被别的线程占用，则JVM会把该线程放入锁池中。
  3. 其他阻塞：运行的线程执行sleep()或join()方法，或者发出了I/O请求时，JVM会把该线程置为阻塞状态。当sleep()状态超时、join()等待线程终止或者超时、或者I/O处理完毕时，线程重新转入就绪状态。
* Waiting（无限期等待）： 处于此状态的线程会等待另外一个线程，不会被分配CPU执行时间，直到被其他线程唤醒
  1. 没有设置timeout参数的Object.wait()
  2. 没有设置timeout参数的Thread.join()
  3. LockSupport.park() 以上方法会使线程进入无限等待状态
* Timed_waiting（限期等待）：不会被分配CPU执行时间，不过无需等待被其它线程显示的唤醒
  1. Thread.sleep()方法
  2. 设置了timeout参数的Object.wait()方法
  3. 设置了timeout参数的Thread.join()方法
  4. LockSupport.parkNanos()方法
  5. LockSupport.parkUntil()方法
* TERMINATED（结束，死亡）：已终止线程的线程状态，线程已经结束执行，run()方法走完了，线程就处于这种状态或者出现没有捕获异常终止run方法意外死亡

# 死锁

## 死锁产生的条件

* 互斥条件：一个资源每次只能被一个线程使用；
* 请求与保持条件：一个线程因请求资源而阻塞时，对已获得的资源保持不放；
* 不剥夺条件：进程已经获得的资源，在未使用完之前，不能强行剥夺；
* 循环等待条件：若干线程之间形成一种头尾相接的循环等待资源关系。

## 避免死锁

互斥条件是保证线程安全的条件，因此不能破环，只能尽量破坏其他造成死锁的条件，比如提前分配各个线程所需资源；设置等待时间或者自旋次数，超时中断；分配好获得锁的顺序；



# Object和Thread中关于线程的一些方法

## Object类中wait()和notify()、notifyAll()

* wait()使得线程进入等待状态，同时释放锁，等待其他线程notify()、notifyAll()的唤醒

  因为wait()和notify()、notifyAll()是对象中的方法，如果wait()没有释放锁，其他线程就无法获得锁进入同步代码块中，也就无法执行notify()或者notifyAll()方法唤醒挂起的线程，造成死锁

* 这套方法只能在同步块synchronized中使用，否则会抛IllegalMonitorStateException异常

* wait() 方法可以设置时间，时间到了也会进入就绪状态

* notify()方法只会随机唤醒某个在等待的线程，notifyAll()方法是唤醒全部

* 可响应中断

* 会抛InterruptedException异常

为什么操作线程的方法wait()和notify()、notifyAll()是Object类中的？

java提供的锁是对象级别的，等待需要锁，把每个对象看成一个锁，同一个对象可以放入不同的线程中，从而达到不同线程可以等待或唤醒，如果是线程里的方法，当前线程可能会等待多个线程的锁，这样操作比较复杂

## Thread中的yield()

* 静态方法
* yield()的作用是让步。它能让当前线程由“运行状态”进入到“就绪状态”，从而让其它具有相同优先级的等待线程获取执行权
* 该方法只是对线程调度器的一个建议，而且也只是建议具有相同优先级的其它线程可以运行，并不能保证在当前线程调用yield()之后，其它具有相同优先级的线程就一定能获得执行权，也有可能是当前线程又进入到“运行状态”继续运行

## Thread中的 suspend() 和resume()

* 不是静态方法
* suspend()用于挂起线程， resume() 用于唤醒线程，需要配套使用，这两个方法被标为**过期，不推荐**
* suspend() 在导致线程暂停的同时，并不会去释放任何锁资源。其他线程都无法访问被它占用的锁。直到对应的线程执行 resume() 方法后，被挂起的线程才能继续，从而其它被阻塞在这个锁的线程才可以继续执行
* 如果 resume() 操作出现在 suspend() 之前执行，那么线程将一直处于挂起状态，同时一直占用锁，这就产生了死锁。而且，对于被挂起的线程，它的线程状态居然还是 Runnable
* 不会抛InterruptedException异常

## Thread类中的sleep()方法

* 静态方法
* 使当前正在执行的线程进入休眠（阻塞），不会释放锁，单位是毫秒
* sleep() 可能会抛出 InterruptedException，因为异常不能跨线程传播回 main() 中，因此必须在本地进行处理。线程中抛出的其它异常也同样需要在本地进行处理
* 可响应中断
* 会抛InterruptedException异常

## Thread类中的join()方法

* 不是静态方法
* 在线程中调用另一个线程的 join() 方法，会将当前线程挂起（阻塞），而不是一直等待，直到目标线程结束
* 没有释放锁
* 可响应中断
* 会抛InterruptedException异常

## Condition类中的await()和signal()、signalAll()

* Condition类中的await()和signal()、signalAll()用来代替传统Object里的wait()和notify()、notifyAll()方法，作用基本相同
* await()可以指定条件，Condition类中的await()和signal()、signalAll()会更加灵活
* Condition配合Lock(ReentrantLock)使用，Lock 可以用来获取一个 Condition对象、还有加锁解锁，阻塞队列中就使用了Condition来模拟线程间协作
* 会抛InterruptedException异常

# 中断

参考[Java并发--InterruptedException机制](https://blog.csdn.net/meiliangdeng1990/article/details/80559012 )

# 关键字

## synchronized

在进入synchronized代码块时，执行 monitorenter，将计数器 +1，释放锁 monitorexit 时，计数器-1；当一个线程判断到计数器为 0 时，则当前锁空闲，可以占用；反之，当前线程进入等待状态。

synchronized是几种锁的封装：自旋锁、锁消除、锁粗化、轻量锁、偏向锁，在加对象锁时，在对象的对象头中的Mark Word记录对象的线程锁状态，根据线程的竞争情况在这几种锁中切换

* 当synchronized(xxx.class)锁住的是类时，多个线程访问不同对象(它们同类),就会锁住代码段，当一个线程执行完这个代码段后才轮到别的线程，可以理解成全局锁
* 当synchronized(object)锁住的是对象时，多个线程访问不同对象(它们同类),它们相互之间并不影响，只有当多个线程访问同一对象时，才会锁住代码段，等到一个线程执行完之后才轮到别的线程执行
* 当synchronized(this)锁住的是当前的对象，当synchronized块里的内容执行完之后，释放当前对象的锁。同一时刻若有多个线程访问这个对象，则会被阻塞
* synchronized下不可被中断
* synchronized是非公平锁

在用synchronized关键字的时候，尽量缩小代码段的范围，能在代码段上加同步就不要再整个方法上加同步，减小锁的粒度，使代码更大程度的并发，如果锁的代码段太长了，别的线程等得就久一点

# synchronized和ReentrantLock区别

相同点：

* 都是加锁实现同步，阻塞性同步
* 可重入
* 有相同的并发性和内存语义

不同点：

* synchronized，是关键字，底层靠JVM实现，通过操作系统调度；ReentrantLock是JDK提供的类，源码可查

* synchronized锁的范围看{}，由JVM控制锁的添加和释放；ReentrantLock手动控制锁的添加和释放，可以灵活控制加锁解锁的位置，相对来讲锁的灵活，锁的细粒度都比synchronized好些

* synchronized是非公平锁，ReentrantLock支持公平锁和非公平锁，默认是公平锁

* synchronized不可中断，除非抛异常，否则只能等同步的代码执行完；ReentrantLock持锁在长期不释放锁时，正在等待的线程可以选择放弃等待，方法如下：

  * lock(), 如果获取了锁立即返回，如果别的线程持有锁，当前线程则一直处于休眠状态，直到获取锁
  * tryLock(), 如果获取了锁立即返回true，如果别的线程正持有锁，立即返回false；
  * tryLock (long timeout, TimeUnit unit)，   如果获取了锁定立即返回true，如果别的线程正持有锁，会等待参数给定的时间，在等待的过程中，如果获取了锁定，就返回true，如果等待超时，返回false；
  * lockInterruptibly: 如果获取了锁定立即返回，如果没有获取锁定，当前线程处于休眠状态，直到或者锁定，或者当前线程被别的线程中断

* synchronized在JDK1.6版本中进行了优化，性能跟ReentrantLock差不多，ReentrantLock仅比synchronized多了一些新功能

  synchronized和ReentrantLock都提供了最基本的锁功能，ReentrantLock多了一些额外的功能：可以提供一个Condition类，实现分组唤醒需要唤醒的线程；提供一些方法监听当前锁的信息

## volatile

需要先了解Java的内存模型，简单的说就是 每个线程有自己的工作内存，工作内存存在高速缓存中，而变量存在于主内存中，线程只能操作工作内存中的变量

线程操作变量的时候，变量会从主内存中load到工作内存中，线程再在工作内存中使用变量，处理完之后再把变量更新到主内存中

- 保证此变量对所有线程的可见性，即当以线程修改这个变量的值，新值对其他线程可知，即如果此变量发送改变，会立即同步到主内存中，volatile只能保证可见性

  由于java里的运算不是原子性，所有volatile变量的运算在并发下一样不安全  
  当出现  
       运算结构不依赖变量的当前值，或能确保只有单一的线程修改变量的值   
       变量不需要与其他的状态变量共同参与不变约束  
  此时需要加锁

- 禁止指令重排序优化，普通变量只会保证该方法执行过程中所依赖赋值的结果都能获得正确的结果，不能保证变量赋值操作的顺序与程序代码的执行顺序一致

典型用法是 检查某个状态标记以判断是否退出循环

# 一些锁的概念

参考[java中的锁](http://www.importnew.com/19472.html)

# JUC包内的一些类

## AQS

* AbstractQueuedSynchronized的缩写，全名：抽象队列同步器，基本是JUC包中各个同步组件的底层基础了

* 内置一个 `volatile int state` 记录同步状态；

  还有一个`双端队列（遵循FIFO）`用于存放资源堵塞的线程，被阻塞的线程加入队尾，队列头结点释放锁时，唤醒后面结点

* AQS提供了一些同步组件，如下面提到的方法，使我们能制作自定义同步组件

### 同步方式

#### 独占式：如ReentrantLock

获取：

1. 调用入口方法acquire
2. 调用模版方法tryAcquire(arg)尝试获取锁，若成功则返回，若失败则走下一步
3. 将当前线程构造成一个Node节点，并利用CAS将其加入到同步队列到尾部，然后该节点对应到线程进入自旋状态
4. 自旋时，首先判断其前驱节点释放为头节点&是否成功获取同步状态，两个条件都成立，则将当前线程的节点设置为头节点，如果不是，则利用`LockSupport.park(this)`将当前线程挂起 ,等待被前驱节点唤醒

释放：

1. 调用入口方法release
2. 调用模版方法`tryRelease`释放同步状态
3. 获取当前节点的下一个节点
4. 利用`LockSupport.unpark(currentNode.next.thread)`唤醒后继节点，之后重复上面 获取 第4步

#### 共享式：如Semaphore，CountDownLatch

获取：

1. 调用入口方法acquireShared
2. 进入tryAcquireShared(arg)模版方法获取同步状态，如果返回值>=0，则说明同步状态(state)有剩余，获取锁成功直接返回
3. 如果tryAcquireShared(arg)返回值<0，说明获取同步状态失败，向队列尾部添加一个共享类型的Node节点，随即该节点进入自旋状态
4. 自旋时，首先检查 (前驱节点是否为头节点 & tryAcquireShared()是否>=0)  (即成功获取同步状态)
5. 如果是，则说明当前节点可执行，同时把当前节点设置为头节点，并且唤醒所有后继节点
6. 如果否，则利用`LockSupport.unpark(this)`挂起当前线程，等待被前驱节点唤醒

释放：

1. 调用releaseShared(arg)模版方法释放同步状态
2. 如果释放成，则遍历整个队列，利用`LockSupport.unpark(nextNode.thread)`唤醒所有后继节点

#### 区别

* 独占式每次释放锁只唤醒后继结点；共享式每次释放锁会唤醒所有后继结点，使它们同时获取同步状态
* 独占锁的同步状态值为1，同一时刻只能有一个线程成功获取同步状态；共享锁的同步状态>1，取值由上层同步组件确定

## 原子操作类

多线程下，java 自增自减操作是线程不安全的，因此JUC包才提供了线程安全的原子操作类

### 原理

CAS + 自旋保证

[AtomicInteger源码分析——基于CAS的乐观锁实现](https://blog.csdn.net/qfycc92/article/details/46489553 "")

## ReentrantLock

### 原理

[ReentrantLock源码之一lock方法解析(锁的获取)](http://www.blogjava.net/zhanglongsr/articles/356782.html)

# 无同步方案

## CAS

## ThreadLocal\<T\>

是java.lang包中的一个类，使用 ThreadLocal 维护变量时，其为每个使用该变量的线程提供独立的变量副本，把共享数据的可见范围限制在同一个线程之内，所以每一个线程都可以独立的改变自己的副本，而不会影响其他线程对应的副本。

### 原理

每个线程内部都会维护一个 静态的ThreadLocalMap对象，该对象里有一个 Entry（K-V 键值对）数组

Entry 的 Key 是一个 ThreadLocal 实例，Value 是一个线程特有对象。

Entry 的作用即是：为其属主线程建立起一个 ThreadLocal 实例与一个线程特有对象之间的对应关系；

Entry 对 Key 的引用是弱引用；Entry 对 Value 的引用是强引用；

每次对ThreadLocal做 get、set操作时，以get方法为例，先通过Thread.currentThread()获得当前线程，在获取该线程对象里的ThreadLocalMap，以当前对象为key(当前线程的ThreadLoacl)在ThreadLocalMap中找到value，强转返回

在一些场景 (尤其是使用线程池) 下，由于 ThreadLocal.ThreadLocalMap 的底层数据结构导致 ThreadLocal 有内存泄漏的情况，应该尽可能在每次使用 ThreadLocal 后手动调用 remove()，以避免出现 ThreadLocal 经典的内存泄漏甚至是造成自身业务混乱的风险



# 参考

[CyC2018/CS-Notes/并发](https://github.com/CyC2018/CS-Notes/blob/master/docs/notes/Java%20%E5%B9%B6%E5%8F%91.md)

[过期的suspend()挂起、resume()继续执行线程](https://www.cnblogs.com/zhengbin/p/6505971.html)

[ReenTrantLock可重入锁（和synchronized的区别）总结](https://blog.csdn.net/qq838642798/article/details/65441415)

[ReentrantLock锁和Synchronized锁的异同点](https://blog.csdn.net/weixin_40792878/article/details/81369385)

[Java程序员开发岗面试知识点解析](https://zhuanlan.zhihu.com/p/44185603)

[Java并发包基石-AQS详解](https://www.cnblogs.com/chengxiao/p/7141160.html)

[Java并发-AQS及各种Lock锁的原理](https://blog.csdn.net/zhangdong2012/article/details/79983404)

[Java并发之AQS详解](https://www.cnblogs.com/daydaynobug/p/6752837.html)

Java并发编程实战

