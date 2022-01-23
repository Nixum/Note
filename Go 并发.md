---
title: Go并发
description: context、channel、并发包相关类的实现原理
date: 2021-03-22
lastmod: 2021-05-15
categories: ["Go"]
tags: ["Go并发", "Go channel原理", "context原理"]
---

[TOC]

# 变量可见性

由于不同的架构和不同的编译器优化，会发生指令重排，导致程序运行时不一定会按照代码的顺序执行，因此两个goroutine在处理共享变量时，能够看到其他goroutine对这个变量进行的写结果。

happens-before：程序的执行顺序和代码的顺序一样，就算真的发生了重排，从行为上也能保证和代码的指定顺序一样。

Go不像Java有volatile关键字实现CPU屏障来保证指令不重排，而是使用不同架构的内存屏障指令来实现同一的并发原语。

**Go只保证goroutine内部重排对读写顺序没有影响**，如果存在共享变量的访问，则影响另一个goroutine。因此当有多个goroutine对共享变量的操作时，需要保证对该共享变量操作的happens-before顺序。

## 证heppen before的手段

* init函数：同一个包下可以有多个init函数，多个签名相同的init函数；main函数一定在导入的包的init函数执行之后执行；当有多个init函数时，从main文件出发，递归找到对应的包 - 包内文件名顺序 - 一个文件内init函数顺序执行init函数。

* 全局变量：包级别的变量在同一个文件中是按照声明顺序逐个初始化的；当该变量在初始化时依赖其它的变量时，则会先初始化该依赖的变量。同一个包下的多个文件，会按照文件名的排列顺序进行初始化。

  init函数也是如此，当init函数引用了全局变量a，运行main函数时，肯定是先初始化a，再执行init函数。

  当init函数和全局变量无引用关系时，先初始化全局变量，再执行init函数

```go
var (
  a = c + b  // == 9
  b = f()    // == 4
  c = f()    // == 5
  d = 3      // 全部初始化完成后 == 5 
)

func f() int {
  d++
  return d
}
---
func init() {
	a += 1
    fmt.Println(a)
	fmt.Println(4)
}

var a = getA()

func getA() int {
	fmt.Println(2)
	return 2
}
// 运行后，输出2，3，4
---
func init() {
	fmt.Println(4)
}

var a = getA()

func getA() int {
	fmt.Println(2)
	return 2
}
// 运行后，输出2，4
```

* goroutine：启动goroutine的go语句执行，一定happens before此goroutine内的代码

```go
var a string
func f() {
	print(a)
}
func hello() {
	a = "hello"
	go f()
}
执行hello方法，必定打印出hello
```

* channel：
  * send操作必定heppen before于receive操作；
  * close一个channel的操作，必定happen before从关闭的channel中读取一个零值；
* 此外还有Mutex / RWMutex、WaitGroup、Once、atomic

# Context



# Channel

Channel的设计基于CSP模型。

CSP模型（Communicating Sequential Process，通信顺序进程），允许使用进程组来描述系统，独立运行，并且只通过消息传递的方式通信。

本质上就是，在使用协程执行函数时，不通过内存共享(会用到锁)的方式通信，而是通过Channel通信传递数据。

动画参考：https://go.xargin.com/docs/data_structure/channel/

## 数据结构

```go
type hchan struct {
	qcount   uint   // 已经接收但还没被取走的元素个数，即channel中的循环数组的元素个数
	dataqsiz uint   // channel中的循环数组的长度
	buf      unsafe.Pointer // channel中缓冲区数据指针，buf是一个循环数组，buf的总大小是elemsize的整数倍
	elemsize uint16 // 当前channel能够收发的元素大小
	closed   uint32
	elemtype *_type // 当前channel能够收发的元素类型
	sendx    uint   // 指向底层循环数组buf，表示当前可发送的元素位置的索引值，当sendx=dataqsiz时，会回到buf数组的起点，一旦接收新数据，指针就会加上elemsize，移向下个位置
	recvx    uint   // 指向底层循环数组buf，表示当前可接收的元素位置的索引值
	recvq    waitq  // 等待接收队列，存储当前channel因缓冲区空间不足而阻塞的goroutine列表，双向链表
	sendq    waitq  // 等待发送队列，存储当前channel因缓冲区空间不足而阻塞的goroutine列表，双向链表

	lock mutex  // 互斥锁，保证每个读channel或写channel的操作都是原子的
}

type waitq struct {
	first *sudog
	last  *sudog
}
```

## 基本

* chan是引用类型，使用make关键字创建，未初始化时的零值是nil，如

  `ch := make(chan string, 10)`，创建一个能处理string的缓冲区大小为10的channel，效果相当于异步队列，除非缓冲区用完，否则不会阻塞；

  `ch := make(chan string)`，则创建了一个不存在缓冲区的channel，效果相当于同步阻塞队列，即如果连续发送两次数据，第一次如果没有被接收的话，第二次就会被阻塞。

* channel作为通道，负责在多个goroutine间传递数据，解决多线程下共享数据竞争问题。

* 带有 <- 的chan是有方向的，不带 <- 的chan是双向的，比如

```go
  chan string        // 双向chan，可以发送和接收string
  chan<- struct{}    // 只能发送struct到chan中
  <-chan int         // 只能从chan中接收int
```

* chan可以是任何类型的，比如可以是 chan<- 类型，<-总是尽量和左边的chan结合，比如

```go
chan<- chan int    // 等价于 chan<- (chan int)
chan<- <-chan int  // 等价于 chan<- (<-chan int)
<-chan <-chan int  // 等价于 <-chan (<-chan int)
chan (<-chan int)  // 等价于 chan (<-chan int)
```

* 接收数据时可以有两个返回值，第一个是返回的元素，第二个是bool类型，表示是否成功地从chan中读取到一个值。如果是false，说明chan已经被close并且chan中没有缓存的数据，此时第一个元素是零值。所以，如果接收时第一个元素是零值，可能是sender真的发送了零值，也可能是closed并且没有元素导致的。
* 双向chan可以赋值给单向chan，但反过来不可以；
* 给一个nil channel发送数据，会造成永久阻塞，从一个nil channel接收数据，会造成永久阻塞；
* 给一个已经关闭的channel发送数据，会引起panic；
* 从一个已经关闭的channel接收数据，如果缓冲区为空，则返回一个零值；

## 初始化

```go
func makechan(t *chantype, size int) *hchan {
    ...
    elem := t.elem
        // 略去检查代码
        mem, overflow := math.MulUintptr(elem.size, uintptr(size))
    ...

    var c *hchan
    switch {
    case mem == 0:
      // chan的size或者元素的size是0，不必创建buf
      c = (*hchan)(mallocgc(hchanSize, nil, true))
      c.buf = c.raceaddr()
    case elem.ptrdata == 0:
      // 元素不是指针，分配一块连续的内存给hchan数据结构和buf
      c = (*hchan)(mallocgc(hchanSize+mem, nil, true))
            // hchan数据结构后面紧接着就是buf
      c.buf = add(unsafe.Pointer(c), hchanSize)
    default:
      // 元素包含指针，那么单独分配buf
      c = new(hchan)
      c.buf = mallocgc(mem, elem, true)
    }
  
    // 元素大小、类型、容量都记录下来
    c.elemsize = uint16(elem.size)
    c.elemtype = elem
    c.dataqsiz = uint(size)
    lockInit(&c.lock, lockRankHchan)

    return c
  }
```

## 发送数据

使用`ch <- "test"`发送数据，最终会调用chansend函数发送数据，该函数设置了阻塞参数为true。

1. 如果chan是nil，则把发送者的goroutine park（阻塞休眠），此时发送者将被永久阻塞。
2. 如果chan没有被close，但是chan满了，则直接返回false，但是由于阻塞参数为true，这部分不会被执行。
3. 如果chan被close了，再往里发数据会触发panic。
4. 当存在等待的接收者时，通过send函数，从接收队列recvq中取出最先进入等待的goroutine，直接发送数据，不需要先放到buf中。
5. 当没有接收者，且缓冲区存在空余空间时，会使用chanbuf计算出下一个可以存储数据的位置，将要发送的数据拷贝到缓冲区并增加sendx索引和qcount计数器，将发送的数据写入channel缓冲区，写入后就返回成功。
6. 当不存在缓冲区或者缓冲区已满，会先调用getg函数获取正在发送者的goroutine，执行acquireSudog函数创建sudog对象，设置此次阻塞发送的相关信息（如发送的channel、是否在select控制结构中和待发送数据的内存地址、发送数据的goroutine），将该sudog对象加入sendq队列，调用goparkunlock函数让当前发送者的goroutine进入等待，表示当前goroutine正在等待其他goroutine从channel中接收数据，等待调度器唤醒。调度器唤醒后，将一些属性值设置为零，并释放sudog对象，表示向channel发送数据结束。

## 接收数据

使用` str <- ch 或 str, ok <- ch ok用于判断ch是否关闭，如果没有ok，可能会无法分辨str接收到的零值是发送者发的还是ch关闭`接收数据，会转化为调用chanrecv1和chanrecv2函数，但最终会调用chanrecv函数接收数据。chanrecv1和chanrecv2函数都是设置阻塞参数为true。

1. 如果chan是nil，则把接收者的goroutine park（阻塞休眠），接收者被永久阻塞。

2. 如果当前channel已经被关闭且缓冲区不存在任何数据，此时会清除ep指针中的数据并立即返回。

3. 如果chan已经被close，且队列中没有缓存元素，返回selected为true，received为false。

4. 当channel的sendq队列存在等待状态的goroutine时，如果是unbuffer的chan，直接使用recv函数直接从阻塞的发送者中获取数据；如果是有buffer的chan，则从sendq队列的头中读取一个值，并把这个发送者的值加入队列的尾部，即优先获取发送者的数据。

5. 当channel的sendq队列没有等待状态的goroutine，且缓冲区存在数据时，从channel的缓冲区中的recvx的索引位置接收数据，如果接收数据的内存地址不为空，会直接将缓冲区里的数据拷贝到内存中，清除队列中的数据，递增recvx，递减qcount，完成数据接收。当发送recvx超过channel的buf时，会将其归零。

   这个和chansend共用一把锁，所以不会有并发问题。

6. 当channel的sendq队列没有等待状态的goroutine，且缓冲区不存在数据时，创建sudog对象，加入recvq队列，当前goroutine进入阻塞状态，等待其他goroutine向channel发送数据。

## 关闭

1. 如果 chan 为 nil，close 会 panic
2. 如果 chan 已经 closed，再次 close 也会 panic。
3. 否则的话，如果 chan 不为 nil，chan 也没有 closed，就把等待队列中的 sender（writer）和 receiver（reader）从队列中全部移除并唤醒。

## 应用场景

* 实现生产者 - 消费组模型，数据传递，比如[worker池的实现](http://marcio.io/2015/07/handling-1-million-requests-per-minute-with-golang/)
* 信号通知：利用 如果chan为空，那receiver接收数据的时候就会阻塞等待，直到chan被关闭或有新数据进来 的特点，将一个协程将信号(closing、closed、data ready等)传递给另一个或者另一组协程，比如 wait/notify的模式。
* 任务编排：让一组协程按照一定的顺序并发或串行执行，比如实现waitGroup的功能
* 实现互斥锁的机制，比如，容量为 1 的chan，放入chan的元素代表锁，谁先取得这个元素，就代表谁先获取了锁

> 共享资源的并发访问使用传统并发原语；
>
> 复杂的任务编排和消息传递使用 Channel；
>
> 消息通知机制使用 Channel，除非只想 signal 一个 goroutine，才使用 Cond；
>
> 简单等待所有任务的完成用 WaitGroup，也有 Channel 的推崇者用 Channel，都可以；
>
> 需要和 Select 语句结合，使用 Channel；需要和超时配合时，使用 Channel 和 Context。

注意点：使用chan要注意panic和goroutine泄露，另外，只要一个 chan 还有未读的数据，即使把它 close 掉，你还是可以继续把这些未读的数据消费完，之后才是读取零值数据。

在使用chan和select配合时要注意会出现goroutine泄漏的情况：

```go
func process(timeout time.Duration) bool {
    ch := make(chan bool)

    go func() {
        // 模拟处理耗时的业务
        time.Sleep((timeout + time.Second))
        ch <- true // block
        fmt.Println("exit goroutine")
    }()
    // 如果上面的协程任务处理的时间过长，触发下面select的超时机制，此时process函数返回，之后当上面的协程任务执行完之后，由于process已经执行完，下面result接收chan的值被回收，导致没有接收者，导致上面的协程任务一直卡在 ch <- true，进而导致goroutine泄漏。解决方案就是使用容量为1的ch即可。
    select {
    case result := <-ch:
        return result
    case <-time.After(timeout):
        return false
    }
}
```

|         | nil   | empty                | full                 | not full & empty     | closed                         |
| ------- | ----- | -------------------- | -------------------- | -------------------- | ------------------------------ |
| receive | block | block                | read value           | read value           | 返回未读的元素，读完后返回零值 |
| send    | block | write value          | block                | writed value         | panic                          |
| close   | panic | closed，没有未读元素 | closed，保留未读元素 | closed，保留未读元素 | panic                          |

# 并发包

## Mutex - 互斥锁

### 数据结构

```go
type Mutex struct {
	state int32   // 分成三部分，最小一位表示锁是否被持有，第二位表示是否有唤醒的goroutine，第三位表示是否处于饥饿状态，剩余的位数表示等待锁的goroutine的数量，最大数量为2^(32-3)-1个，以goroutine初始空间为2k，则达到最大数量时需要消耗1TB内存
	sema  uint32  // 信号量变量，用来控制等待goroutine的阻塞休眠和唤醒
}
const (
	mutexLocked = 1 << iota // 持有锁的标记
	mutexWoken  // 唤醒标记
	mutexStarving // 饥饿标记
	mutexWaiterShift = iota  // 阻塞等待的waiter数量
    starvationThresholdNs = 1e6
}
```

### 基本

* 只有Lock和Unlock两个方法，用于锁定临界区

* Mutex的零值是没有goroutine等待的未加锁状态，不会因为没有初始化而出现空指针或者无法获取到锁的情况，so无需额外的初始化，直接声明变量即可使用`var lock sync.Mutex`，或者是在结构体里的属性，均无需初始化

* 如果Mutex已被一个goroutine获取了锁，其他等待的goroutine们会一直等待，组成等待的队列，当该goroutine释放锁后，等待的goroutine是以先进先出的队列排队获取锁；如果此时有新的goroutine也在获取锁，会参与到获取锁的竞争中，如果等待队列中的goroutine等待超过1ms，则会加入队头优先获取锁，新来的goroutine加入到队尾，以此解决等待的goroutine的饥饿问题。

* Unlock方法可以被任意goroutine调用，释放锁，即使它本身没有持有这个锁，so写的时候要牢记，谁申请锁，就该谁释放锁，保证在一个方法内被调用

* 必须先使用Lock方法才能使用Unlock方法，否则会panic，重复释放锁也会panic

* 自旋的次数与自旋次数、cpu核数，p的数量有关

* 注意Mutex在使用时不能被复制，比如方法传参的没有使用指针，导致执行方法的参数时被复制

* Mutex是不可重入锁，获取锁的goroutine无法重复获取锁，因为Mutex本身不记录哪个goroutine拥有这把锁，因此如果要实现可重入锁，则需要对Mutex进行包装，实现Locker接口，同时记录获取锁的goroutine的id和重入次数

  获取goroutine id的方法：

  ​	1.使用runtime.Stack()方法获取栈帧里的goroutine id

  ​	2.获取运行时的g指针，反解出g的TLS结构，获取存在TLS结构中的goroutine id

  ​	3.给获取锁的goroutine设置token，进行标记

### Lock方法

1. 调用Lock的goroutine通过CAS的方式获取锁，如果获取到了直接返回，否则进入lockSlow方法
2. 在lockSlow方法内，意味着锁已经被持有，当前调用Lock方法的goroutine正在等待，且非饥饿状态，其首先会自旋，尝试获取锁，而无需休眠，主要是在当临界区耗时很短的场景下提高性能
3. 非饥饿状态下抢锁，先在state锁标志位+1，如果锁已经被持有或者此时是饥饿状态，state的waiter的数量+1，进入等待
4. 如果此时是饥饿状态，并且锁还被持有，state设置为饥饿状态，清除mutexWoken标记，表示非唤醒状态
5. 使用3中的锁位，CAS尝试加锁，如果成功，检查原来的锁是未加锁状态，并且也不是饥饿状态，则成功获取锁，返回
6. 如果是未加锁状态，判断是否是第一次加入waiter队列（通过waitStartTime变量），如果是，则加入队尾，如果不是首次，则加入到队首(设置sema的值)，阻塞等待，直至被唤醒（因为锁被释放了）
7. 唤醒后，如果是饥饿状态（即当前时间 - waitStartTime > 1ms），在state锁标志位+1，waiter数-1，获取锁，然后判断没有其他waiter或者此goroutine等待时间没有超过1ms，清除饥饿标记

### Unlock方法

1. 将state的锁位-1，如果state=0，即此时没有加锁，且没有正在等待获取锁的goroutine，则直接结束方法，如果state != 0，执行unlockSlow方法
2. 如果Mutex处于饥饿状态，直接唤醒等待队列中的waiter
3. 如果Mutex处于正常状态，如果没有waiter，或者已经有在处理的情况，则直接释放锁，state锁位-1，返回；否则，waiter数-1，设置唤醒标记，通过CAS解锁，唤醒在等待锁的goroutine（此时新老goroutine一起竞争锁）

### 基于Mutex的拓展

* 可重入锁
* 增加tryLock方法，通过返回true或false来表示获取锁成功或失败，主要用于控制获取锁失败后的行为，而不用阻塞在方法调用上
* 增加等待计数器，比如等待多少时间后还没获取到锁则放弃
* 增加可观测性指标，比如等待锁的goroutine的数量，需要使用unsafe.Pointer方法获取Mutex中的state的值，解析出正在等待的goroutine的数量
* 实现线程安全的队列，通过在出队和入队方法中使用Mutex保证线程安全

## RWMutex - 读写锁

### 数据结构

```go
type RWMutex struct {
    w Mutex           // 互斥锁解决多个writer的竞争
    writerSem uint32  // writer信号量 
    readerSem uint32  // reader信号量 
    readerCount int32 // reader的数量，可以是负数，负数表示此时有writer等待请求锁，此时会阻塞reader
    readerWait int32  // writer等待完成的reader的数量
}
const rwmutexMaxReaders = 1 << 30 // 最大的reader数量
```

### 基本

* 主要提升Mutex在读多写少的场景下的吞吐量，读时共享锁，写时排他锁，基于Mutex实现
* 由5个方法构成：
  * Lock/Unlock：写操作时调用的方法。如果锁已经被 reader 或者 writer 持有，那么，Lock 方法会一直阻塞，直到能获取到锁；Unlock 则是配对的释放锁的方法。
  * RLock/RUnlock：读操作时调用的方法。如果锁已经被 writer 持有的话，RLock 方法会一直阻塞，直到能获取到锁，否则就直接返回；而 RUnlock 是 reader 释放锁的方法。
  * RLocker：这个方法的作用是为读操作返回一个 Locker 接口的对象。它的 Lock 方法会调用 RWMutex 的 RLock 方法，它的 Unlock 方法会调用 RWMutex 的 RUnlock 方法
* 同Mutex，RWMutex的零值是未加锁状态，无需显示地初始化
* 由于读写锁的存在，可能会有饥饿问题：比如因为读多写少，导致写锁一直加不上，因此go的RWMutex使用的是写锁优先策略，如果已经有一个writer在等待请求锁的话，会阻止新的reader请求读锁，优先保证writer。如果已经有一些reader请求了读锁，则新请求的writer会等待在其之前的reader都释放掉读锁后才请求获取写锁，等待writer解锁后，后续的reader才能继续请求锁。
* 同Mutex，均为不可重入，使用时应避免复制；要注意reader在加读锁后，不能加写锁，否则会形成相互依赖导致死锁；注意reader是可以重复加读锁的，重复加读锁时，外层reader必须=里层的reader释放锁后自己才能释放锁。
* 必须先使用RLock / Lock方法才能使用RUnlock / Unlock方法，否则会panic，重复释放锁也会panic。
* 可以利用RWMutex实现线程安全的map

### RLock / RUnlock 方法

1. RLock时，对readerCount的值+1，判断是否< 0，如果是，说明此时有writer在竞争锁或已持有锁，则将当前goroutine加入readerSem指向的队列中，进行等待，防止写锁饥饿。
2. RUnlock时，对readerCount的值-1，判断是否<0，如果是，说明当前有writer在竞争锁，调用rUnlockSlow方法，对readerWait的值-1，判断是否=0，如果是，说明当前goroutine是最后一个要解除读锁的，此时会唤醒要请求写锁的writer。

### Lock方法

RWMutex内部使用Mutex实现写锁互斥，解决多个writer间的竞争

1. 调用w的Lock方法加锁，防止其他writer上锁，反转 readerCount的值，使其变成负数（readerCount - rwmutexMaxReaders + rwmutexMaxReaders）告诉reader有writer要请求锁
2. 如果此时readerCount != 0，说明当前有reader持有读锁，需要记录需要等待完成的reader的数量，即readerWait的值（readerWaiter + 第1步算的readerCount的值），并且如果此时readerWait != 0，将当前goroutine加入writerSema指向的队列中，进行等待。直到有goroutine调用RUnlock方法且是最后一个释放锁时，才会被唤醒。

### Unlock方法

1. 反转readerCount的值（readerCount + rwmutexMaxReaders），使其变成reader的数量，唤醒这些reader
2. 调用w的Unlock方法释放当前goroutine的锁，让其他writer可以继续竞争。

## WaitGroup

### 数据结构

```go
type WaitGroup struct { 
    // 避免复制，使用vet工具在编译时检测是否被复制
    noCopy noCopy
    // 因为64bit值的原子操作需要64bit对齐，但是32bit编译器不支持，所以数组中的元素在不同的架构中不一样
    // 如果地址是64bit对齐，数组前两个元素做state，后一个元素做信号量；如果地址是32bit对齐，数组后两个元素做state，第一个元素做信号量
    // 高32bit是WaitGroup的计数值，低32bit是waiter的计数,另外32bit是用作信号量
    state1 [3]uint32
}
```

### 基本

* state的值由32bit的值表示信号量，64bit的值表示计数和waiter的数量组成。因为原子操作只能64bit对齐，而计数值和waiter的数量是一个64bit的值，在64bit的编译器上，一次读取是64bit，刚好可以直接操作，但是如果是32bit的机器，一次只能读32bit，为了保证进行64bit对齐时一定能获取到计数值和waiter的值，在进行64bit的原子操作对齐时，第一次是对齐到了一个空32bit和第一个32bit的值，第二次对齐就能保证获取了。
* 同RWMutex，WaitGroup的三个方法内还很多data race检查，保证并发时候共享数据的正确性，一旦检查出有问题，会直接panic
* 一开始设置WaitGroup的计数值必须大于等于0，否则会过不了data race检查，直接panic
* Add的值必须=调用调用Done的次数，当Done的次数超过计数值，也会panic
* Wait方法的调用一定要晚于Add，否则会导致死锁
* WaitGroup可以在计数值为0时可重复使用
* noCopy是一个实现了Lock接口的结构体，且不对外暴露，其Lock方法和Unlock方法都是空实现，用于vet工具检查WaitGroup在使用过程中有没有被复制；当我们自定义的结构不想被复制使用时，也可以使用它。
* 使用时要避免复制

### Add方法

1. 原子的将WaitGroup的计数值加到state上，如果当前的计数值 > 0，或者 waiter的数量等于0，直接返回
2. 否则，即代表当前的计数值为0，但waiter的数量不一定为0，此时state的值就是waiter的数量
3. 将state的值设置为0，即waiter的数量设置为0，然后唤醒所有waiter

### Done方法

1. 调用Add方法，只是参数为-1，表示计数值 - 1，有一个waiter完成其任务；waiter指的是调用Wait方法的goroutine

### Wait方法

1. 循环内不断检测state的值，当其计数值为0时，说明所有任务已经完成，调用这个方法的goroutine不必继续等待，直接返回，结束该方法
2. 否则，说明此时还有任务没完成，调用该方法的goroutine成为waiter，把waiter的数量 + 1，加入等待队列，阻塞自己

## Cond = condition + Wait/Notify

### 数据结构

```go
type Cond struct { 
    noCopy noCopy // 使用vet工具在编译时检测是否被复制
    checker copyChecker // 用于运行时被检测是否被复制
    L Locker // 当观察或者修改等待条件的时候需要加锁
    notify notifyList // 等待队列 
}
```

### 基本

* 初始化时，要指定使用的锁，比如Mutex

* Cond 是等待某个条件满足，这个条件的修改可以被任意多的 goroutine 更新，而且 Cond 的 Wait 不关心也不知道其他 goroutine 的数量，只关心等待条件。

* Signal方法，类似Java的notify方法，允许调用者唤醒一个等待此Cond的goroutine，如果此时没有waiter，则无事发生；如果此时Cond的等待队列中有多个goroutine，则移除队首的goroutine并唤醒；

  使用Signal方法时不强求已调用了加锁方法

* Broadcast方法，类似Java的notifyAll方法，允许调用者唤醒等待此Cond的所有goroutine，如果此时没有waiter，则无事发生；如果此时Cond的等待队列中有多个goroutine，则清空整个等待队列，全部唤醒；

  使用Broadcast方法时不强求已调用了加锁方法

* Wait方法，类似Java的wait方法，把调用者的goroutine放入Cond的等待队列中并阻塞，直到被Signal或Broadcast方法唤醒

  调用Wait方法时必须已调用了加锁方法，否则会panic，因为Wait方法内是**先解锁**，将当前goroutine加入到**等待**队列，然后**解锁**，阻塞休眠当前goroutine，直到被**唤醒**，然后**加锁**

  调用Wait后一定要检测等待条件是否满足，还需不需要继续等待，在等待的goroutine被唤醒不等于等待条件已满足，可能只是被某个goroutine唤醒而已，被唤醒时，只是得到了一次检测机会。

## Once

### 数据结构

```go
type Once struct {
    done uint32
    m Mutex
}
```

### 基本

* sync.Once只有一个Do方法，入参是一个无参数无返回值的函数，当且仅当第一次调用Do方法的时候该函数才会执行，即使之后调用了n次、入参的值不一样都不会被执行
* 可以将sync.Once与想要只初始化一次的对象封装成一个结构体，提供只初始化一次该值的方法，常用于初始化单例资源、并发访问只初始化一次的共享资源、需要延迟初始化的场景等
* Once传入的函数参数，就算在执行时发生panic，Once也会认为已经执行过了，so如果要知道Once里传入的方法是否执行成功，模仿Do函数自己写一个返回参数的入参方法
* 内部的实现非常简单，就是一个flag + 一个双重校验锁

```go
func (o *Once) Do(f func()) {
    // 判断flag是否被置为0，即函数是否还没被执行过
	if atomic.LoadUint32(&o.done) == 0 { 
        o.doSlow(f) 
    }
}
func (o *Once) doSlow(f func()) { 
    o.m.Lock() 
    defer o.m.Unlock()
    if o.done == 0 {
        // 因为其他最外层的判断+LoadUnit32没有被锁保护，so这里得原子操作
        defer atomic.StoreUint32(&o.done, 1) 
        f() 
    }
}
```

## 并发安全的map

* 将map与RWMutex封装成一个结构体，使用读写锁封装map的各种操作即可

* 使用RWMutex封装的并发安全的map，因为锁的粒度太大，性能不会太好；通过减少锁的粒度和持有锁的时间，可以提升新能，常见的减少锁的粒度是将锁分片，将锁进行分片，分别控制map中不同的范围的key，类似JDK7中的ConcurrentHashMap的segment锁实现。

* 官方出品的sync.Map，但它只有在部分特殊的场景里才有优势，比如一个只会增长的map，一个key只会被写一次，读很多次；或者 多个goroutine为不相交的键集读、写和重写键值对

  sync.Map内部有两个map，一个只读read，一个可写dirty，对只读read的操作(读、更新、删除)不需要加锁，以此减少锁对性能的影响。

  如果read中读不到，就会加锁读取dirty里的，同时增加miss的值(miss表示读取穿透的次数)，当miss的值=dirty的长度时，就会将dirty提升为read，然后清空dirty，避免总是从dirty中加锁读取

  加锁读取dirty时，还要再检查read，确定read中真的不存在才会操作dirty

  dirty提升为read时，只需简单的赋值即可，创建新的dirty时，需要遍历把read中非expunged的值赋给dirty

  删除key时，只是对该key打上一个expunged标记，只有在将dirty提升为read时才会清理删除的数据

  sync.Map没有len方法，要获取里面有多少个key只能遍历获取

## Pool

### 数据结构

![](https://github.com/Nixum/Java-Note/raw/master/picture/go syncPool数据结构.png)

* 每次垃圾回收时，Pool会把victim中的对象移除，然后把local的数据给victim，local置为nil，如果此时有Get方法被调用，则会从victim中获取对象。通过这种方式，避免缓存元素被大量回收后再再次使用时新建很多对象
* 获取重用对象时，先从local中获取，获取不到再从victim中获取
* poolLocalInternal用于CPU缓存对齐，避免false sharing
* private字段代表一个缓存元素，且只能由相应的一个P存取，因为一个P同时只能执行一个goroutine，所以不会有并发问题
* shared字段可以被任意的P访问，但是只有本地的P次啊能pushHead/popHead，其他P可以popTail，相当于只有一个本地P作为生产者，多个P作为消费者，它由一个local-free的队列实现

### 基本

* sync.Pool用于保存一组可独立访问的临时对象，它池化的对象如果没有被其他对象持有引用，可能会在未来某个时间点被回收掉
* sync.Pool是并发安全的，多个gotoutine可以并发调用它存取对象；
* 不能复制使用
* 在1.13以前，保证并发安全使用了带锁的队列，1.13后，改成了lock-free的队列实现，避免锁对性能的影响
* 包含了三个方法：New、Get、Put；Get方法调用时，会从池中移走该元素
* 当Pool里没有元素可用时，Get方法会返回nil；可以向Pool中Put一个nil的值，Pool会将其忽略
* 当使用Pool作为buffer池时，要注意buffer如果太大，reset后它就会占很大空间，引起内存泄漏，因此在回收元素时，需要检查大小，如果太大了就直接置为null，丢弃即可

### Get方法

1. 将当前goroutine固定在P上，优先从local的private字段取出一个元素，将private置为null
2. 如果取出的元素为null，从当前的local.shared的head中取出一个元素，如果还取不到，调用getSlow函数去其他shared中取
3. getSlow函数会遍历所有local，从它们的shared的head中弹出一个元素，如果还没有，则对victim中以在同样的方式(先从private里找，找不到再在shared里找)获取一遍
4. 如果还取不到，则调用New函数生成一个，然后返回

因为当前的goroutine被固定在了P上，在查找元素时不会被其他P执行

### Put方法

1. 如果Put进来的元素是null，直接返回
2. 固定当前goroutine，如果本地private没有值，直接设置，否则加入到shared中

## 原子操作

* 依赖atomic包，因为没有泛型，目前该包支持int32、int64、uint32、unit64、uintptr、Pointer的原子操作，比如Add、CompareAndSwap、Swap、Load、Store等（Pointer不支持Add），对于有符号的数值来说，Add一个负数相当于减
* 对于现代多核操作系统来说，由于cache、指令重排、可见性问题，一个核对地址的值的更改，在更新到主内存中前，会先存在多级缓存中，此时，多个核看到该数据可能还没看到更新的数据，还在使用旧数据，而atomic包提供的方法会提供内存屏障的功能，保证赋值数据的完整性和可见性

* atomic操作的对象是一个地址，不是变量值

用atomic实现的lock-free的队列

```go
package queue
import (
  "sync/atomic"
  "unsafe"
)
// lock-free的queue
type LKQueue struct {
  head unsafe.Pointer
  tail unsafe.Pointer
}
// 通过链表实现，这个数据结构代表链表中的节点
type node struct {
  value interface{}
  next  unsafe.Pointer
}
func NewLKQueue() *LKQueue {
  n := unsafe.Pointer(&node{})
  return &LKQueue{head: n, tail: n}
}
// 入队
func (q *LKQueue) Enqueue(v interface{}) {
  n := &node{value: v}
  for {
    tail := load(&q.tail)
    next := load(&tail.next)
    if tail == load(&q.tail) { // 尾还是尾
      if next == nil { // 还没有新数据入队
        if cas(&tail.next, next, n) { //增加到队尾
          cas(&q.tail, tail, n) //入队成功，移动尾巴指针
          return
        }
      } else { // 已有新数据加到队列后面，需要移动尾指针
        cas(&q.tail, tail, next)
      }
    }
  }
}
// 出队，没有元素则返回nil
func (q *LKQueue) Dequeue() interface{} {
  for {
    head := load(&q.head)
    tail := load(&q.tail)
    next := load(&head.next)
    if head == load(&q.head) { // head还是那个head
      if head == tail { // head和tail一样
        if next == nil { // 说明是空队列
          return nil
        }
        // 只是尾指针还没有调整，尝试调整它指向下一个
        cas(&q.tail, tail, next)
      } else {
        // 读取出队的数据
        v := next.value
        // 既然要出队了，头指针移动到下一个
        if cas(&q.head, head, next) {
          return v // Dequeue is done.  return
        }
      }
    }
  }
}

// 将unsafe.Pointer原子加载转换成node
func load(p *unsafe.Pointer) (n *node) {
  return (*node)(atomic.LoadPointer(p))
}

// 封装CAS,避免直接将*node转换成unsafe.Pointer
func cas(p *unsafe.Pointer, old, new *node) (ok bool) {
  return atomic.CompareAndSwapPointer(
    p, unsafe.Pointer(old), unsafe.Pointer(new))
}
```

## Weighted = Semaphore信号量

### 数据结构

```go
type Weighted struct {
    size    int64         // 最大资源数
    cur     int64         // 当前已被使用的资源
    mu      sync.Mutex    // 互斥锁，对字段的保护
    waiters list.List     // 等待队列，通过channel实现通知机制
}
```

### 基本

* 信号量中的PV操作，P：获取资源，如果获取不到，则阻塞，加入到等待队列中；V：释放资源，从等待队列中唤醒一个元素执行P操作

* 二进位信号量，或者说只有一个计数值的信号量，其实相当于go中的Mutex互斥锁

* 初始化时，必须指定初始的信号量

* 只调用Release方法会直接panic；Release方法传入负数，会导致资源被永久持有；因此要保证请求多少资源，就释放多少资源

* Mutex中使用的sema是一个信号量，只是其实现是在runtime中，并没有对外暴露，在扩展包中，暴露了一个信号量工具Weighted

* Weighted分为3个方法：Acquire方法，相当于P操作，第一个参数是context，可以使用context实现timeout或cancel机制，终止goroutine；正常获取到资源时，返回null，否则返回ctx.Err，信号量计数值不变。

  Release方法，相当于V操作，可以释放n个资源，返回给信号量；

  TryAcquire方法，尝试获取n个资源，但不会阻塞，成功时返回true，否则一个也不获取，返回false

* 信号量的实现也可通过buffer为n的channel实现，只是一次只能请求一个资源，而Weighted一次可以请求多个

### Acquire方法

1. 加锁，判断可用资源 >= 入参所需的资源数，且没有waiter，说明资源足够，直接cur+上所需资源数，解锁返回
2. 如果所需资源数>最大资源数，说明是不可能任务，解锁，依赖ctx的Done方法返回，否则一直等待
3. 如果资源数不够，将调用者加入等待队列，并创建一个read chan，用于通知唤醒，解锁
4. 等待唤醒有两种条件，一种是通过read chan唤醒，另一种是通过ctx.Done唤醒

### Release方法

1. 加锁，当前已使用资源数cur - 入参要释放的资源数，唤醒等待队列中的元素，解锁
2. 唤醒等待队列的元素时，会遍历waiters队列，按照先入先出的方式唤醒调用者，前提是释放的资源数要够队首的元素资源的要求，比如释放100个资源，但是队首元素要求101个资源，那队列中的所有等待者都将继续等待，直到队首元素出队，这样做是为了避免饥饿

## SingleFlight

### 结构体

```go
// 代表一个正在处理的请求，或者已经处理完的请求
type call struct {
    wg sync.WaitGroup
    // 这个字段代表处理完的值，在waitgroup完成之前只会写一次, waitgroup完成之后就读取这个值
    val interface{}
    err error
  
    forgotten bool  // 指示当call在处理时是否要忘掉这个key
    dups  int  // 相同的key的请求数
    chans []chan<- Result
}
  
// group代表一个singleflight对象
type Group struct {
    mu sync.Mutex       // protects m
    m  map[string]*call // lazily initialized
}
```

### 基本

* SingleFlight可以合并多个请求为一个请求，再将该请求的结果返回给多个请求，从而达到合并并发请求的目的，减少并发调用的数量。比如有多个相同的读请求查库，那就可以合并成一个请求查库，再把结果响应回这多个请求中；或者是解决缓存击穿问题，降低对下游服务的并发压力

* 底层由Mutex和Map实现，Mutex保证并发读写保护，Map保存同一个key正在处理的请求

* 包含3个方法，Do方法：提供一个key和一个函数，对于同一个key，在同一时间只有一个函数在执行，之后同一个key并发的请求会等待，等到第一个执行的结果就是该key的所有结果，调用完成后，会移除这个key。返回值shared表示结果是否来自多个相同请求。

  DoChan方法：类似Do方法，只是返回是一个chan，待入参函数执行完，产生结果后就能在chan中接收这个结果

  Forget方法：告诉Group忽略这个key，之后这个key的请求会执行入参函数，而不是等待前一个未完成的入参函数的结果

## CyclicBarrier - 循环栅栏

### 数据结构

```go
type CyclicBarrier interface {
    // 等待所有的参与者到达，如果被ctx.Done()中断，会返回ErrBrokenBarrier
    Await(ctx context.Context) error
    // 重置循环栅栏到初始化状态。如果当前有等待者，那么它们会返回ErrBrokenBarrier
    Reset()
    // 返回当前等待者的数量
    GetNumberWaiting() int
    // 参与者的数量
    GetParties() int
    // 循环栅栏是否处于中断状态
    IsBroken() bool
}
```

### 基本

* 类似Java的CyclicBarrier，允许一组goroutine相互等待，到达一个共同的执行点再继续往下执行；同时也可被重复使用。
* CyclicBarrier是一个接口，然后有两个初始化的方法，New方法，指定循环栅栏的参与者数量即可初始化；NewWithAction方法，除了指定参与者数量，第二个参数是一个函数，表示在最后一个参与者到达之后，但其他参与者还没放行之前，会调用该函数
* 每个参与的goroutine都会调用Await方法进行阻塞，当调用Await方法的goroutine的个数=参与者的数量时，Await方法造成的阻塞才会解除

## ErrGroup

* 类似WaitGroup，只是功能更丰富，多了与Context集成，可以通过Context监控是否发生cancel；error可以向上传播，把子任务的错误传递给Wait的调用者
* ErrGroup用于并发处理子任务，将一个大任务拆成几个小任务，通过Go方法并发执行。
* ErrGroup有三个方法：withContext、Go、Wait，用法与WaitGroup相似，只是不需要设置计数值，且可以通过Wait方法获取子任务返回的错误，但它只会返回第一个出现的错误，如果所有子任务都执行成功，返回null；当发生错误时不会立即返回，而是等到其他任务完成了才会返回。

* Go方法会创建一个goroutine来执行子任务，如果并发的量太大，会导致创建大量的goroutine，带来goroutine的调度和GC压力，占用更多资源，解决方案可以是使用worker pool或者信号量来控制goroutine的数量或保持重用
* 子任务如果发生panic会导致程序崩溃

## 检测工具

* go race detector：主要用于检测多个goroutine对共享变量的访问是否存在协程安全问题。编译器通过探测所有内存的访问，加入代码监视对内存地址的访问，在程序运行时，监控共享变量的非同步访问，出现race时，打印告警信息。比如在运行时加入race参数`go run -race main.go`，当执行到一些并发操作时，才会检测运行时是否有并发问题
* 命令`go vet xxx.go`可以进行死锁检测
