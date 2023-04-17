---
title: Go Sync包相关
description: Go sync包相关类的实现原理
date: 2021-03-22
weight: 3
categories: ["Go"]
tags: ["Go并发", "Go sync包"]
---

[TOC]

# 变量可见性

由于不同的架构和不同的编译器优化，会发生指令重排，导致程序运行时不一定会按照代码的顺序执行，因此两个goroutine在处理共享变量时，能够看到其他goroutine对这个变量进行的写结果。

happens-before：程序的执行顺序和代码的顺序一样，就算真的发生了重排，从行为上也能保证和代码的指定顺序一样。

Go不像Java有volatile关键字实现CPU屏障来保证指令不重排，而是使用不同架构的内存屏障指令来实现同一的并发原语。

**Go只保证goroutine内部重排对读写顺序没有影响**，如果存在共享变量的访问，则影响另一个goroutine。因此当有多个goroutine对共享变量的操作时，需要保证对该共享变量操作的happens-before顺序。

```go
// 例子：
var a, b int
go func() {
    a := 5
    b := 1
}
go func() {
    for b == 1 {}
    fmt.Println(a)
}
// 当两个goroutine同时执行时，因为指令重排的缘故，第二个goroutine打印a可能是5，也可能是0
```

## 证heppens before的手段

* init函数：同一个包下可以有多个init函数，多个签名相同的init函数；main函数一定在导入的包的init函数执行之后执行；当有多个init函数时，从main文件出发，递归找到对应的包 - 包内文件名顺序 - 一个文件内init函数顺序执行init函数。

* 全局变量：包级别的变量在**同一个文件中是按照声明顺序逐个初始化**的；当该变量在初始化时**依赖其它的变量时，则会先初始化该依赖的变量**。**同一个包下的多个文件，会按照文件名的排列顺序进行初始化**。

  init函数也是如此，当init函数引用了全局变量a，运行main函数时，肯定是先初始化a，再执行init函数。

  当init函数和全局变量**无引用关系时，先初始化全局变量，再执行init函数**

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

# Mutex - 互斥锁

## 数据结构

```go
type Mutex struct {
	state int32   // 分成四部分，最小一位表示锁是否被持有，第二位表示是否有唤醒的goroutine，第三位表示是否处于饥饿状态，剩余的位数表示等待锁的goroutine的数量，最大数量为2^(32-3)-1个，以goroutine初始空间为2k，则达到最大数量时需要消耗1TB内存
	sema  uint32  // 信号量变量，用来控制等待goroutine的阻塞休眠和唤醒
}
const (
	mutexLocked = 1 << iota // 持有锁的标记，此时被锁定
	mutexWoken  // 唤醒标记，从正常模式被唤醒
	mutexStarving // 饥饿标记，进入饥饿模式
	mutexWaiterShift = iota  // 阻塞等待的waiter数量
    starvationThresholdNs = 1e6
}
```

![](https://github.com/Nixum/Java-Note/raw/master/picture/go_mutex_state.png)

- mutexLocked 对应右边低位第一个bit，1 代表锁被占用，0代表锁空闲

- mutexWoken 对应右边低位第二个bit，1 表示已唤醒，0表示未唤醒

  从正常模式被唤醒，用于加锁和解锁过程中的通信，比如同一时刻，一个协程在解锁，一个协程在加锁，正在加锁的协程可能在自旋，此时标记为唤醒，另一个协程解锁后，锁立马被这个协程拿到，避免唤醒存在阻塞队列中的协程；

- mutexStarving 对应右边低位第三个bit，1 代表锁处于饥饿模式，0代表锁处于正常模式

- mutexWaiterShift 值为3，根据 `mutex.state >> mutexWaiterShift` 得到当前阻塞的`goroutine`数目，最多可以阻塞`2^29`个`goroutine`。

- starvationThresholdNs 值为1e6纳秒，也就是1毫秒，当等待队列中队首goroutine等待时间超过`starvationThresholdNs`也就是1毫秒，mutex进入饥饿模式。

## 基本

* 只有Lock和Unlock两个方法，用于锁定临界区

* Mutex的零值是没有goroutine等待的未加锁状态，不会因为没有初始化而出现空指针或者无法获取到锁的情况，so无需额外的初始化，直接声明变量即可使用`var lock sync.Mutex`，或者是在结构体里的属性，均无需初始化

* 锁有两种模式：正常模式和饥饿模式

  **正常模式下**，如果Mutex已被一个goroutine获取了锁，其他等待的goroutine们会一直等待，组成等待队列，当该goroutine释放锁后，等待的goroutine是以先进先出的队列排队获取锁；

  如果此时有新的goroutine也在获取锁，会参与到获取锁的竞争中，这是非公平的，因为新请求锁的goroutine是在CPU上被运行，并且数量也可能很多，所以被唤醒的goroutine获取锁的概率并不大，所以，如果等待队列中的goroutine等待超过1ms，则会优先加入到队列的头部，如果超过1ms都没有获取到锁，则进入饥饿模式；

  **饥饿模式下**，锁的所有权会直接从释放锁的goroutine转交给队首的goroutine，新请求锁的goroutine就算锁的空闲状态也不会去获取锁，也不会自旋，直接加入等待队列的队尾，以此解决等待的goroutine的饥饿问题；

  恢复为正常模式的条件：一个goroutine获取锁后，当前goroutine是队列的最后一个，退出饥饿模式；

* Unlock方法可以被任意goroutine调用，释放锁，即使它本身没有持有这个锁，so写的时候要牢记，谁申请锁，就该谁释放锁，保证在一个方法内被调用

* 必须先使用Lock方法才能使用Unlock方法，否则会panic，重复释放锁也会panic

* 自旋的次数与cpu核数，p的数量有关

* 注意Mutex在使用时不能被复制，比如方法传参的没有使用指针，导致执行方法的参数时被复制

* Mutex是不可重入锁，获取锁的goroutine无法重复获取锁，因为Mutex本身不记录哪个goroutine拥有这把锁，因此如果要实现可重入锁，则需要对Mutex进行包装，实现Locker接口，同时记录获取锁的goroutine的id和重入次数

  获取goroutine id的方法：

  ​	1.使用runtime.Stack()方法获取栈帧里的goroutine id

  ​	2.获取运行时的G指针，反解出G的TLS结构，获取存在TLS结构中的goroutine id

  ​	3.给获取锁的goroutine设置token，进行标记

## Lock方法

1. 调用Lock的goroutine通过**CAS的方式设置锁标志**，如果获取到了直接返回；

2. 否则进入`lockSlow方法`，`lockSlow方法`主要是通过自旋等待锁的释放；自旋是为了不让goroutine进入休眠，让其在一段时间内保持运行，忙等待快速获取锁；

   goroutine本身进入自旋的条件比较苛刻：

   * 互斥锁只有在正常模式才能进入自旋；

   * `runtime.sync_runtime_canSpin`需要返回true：
   	1. 运行在多 CPU 的机器上；
   	2. 当前 Goroutine 为了获取该锁进入自旋的次数小于四次；
   	3. 当前机器上至少存在一个正在运行的处理器 P 并且处理的运行队列为空；
   
3. 在lockSlow方法内，意味着锁已经被持有，当前调用Lock方法的goroutine正在等待，且非饥饿状态，其首先会自旋，尝试获取锁，无需休眠，否则进入 4

   不满足自旋时，当前锁可能有如下几种状态：

   - 锁还没有被释放，锁处于正常状态
   - 锁还没有被释放， 锁处于饥饿状态
   - 锁已经被释放， 锁处于正常状态
   - 锁已经被释放， 锁处于饥饿状态

5. 由于lock方法会被多个goroutine执行，所以锁的状态会不断变化，此时会生成当前goroutine的 new state 作为期望状态

   * 如果是非饥饿状态，锁的new state设置为已持有锁
   * 如果已经持有锁，或者是饥饿状态，waiter数量 + 1
   * 如果已经持有锁，且是饥饿状态，锁的new state设置为饥饿状态
   * 如果当前goroutine被唤醒，锁的new state设置为唤醒状态

6. **CAS更新当前锁的状态**为new state，如果更新成功

   5.1. 如果锁的原状态old state是未被锁，且非饥饿状态，表明当前goroutine获取到了锁，**退出结束**
   5.2. 判断当前goroutine是新加入的还是被唤醒的，新加入的放到等待队列的尾部，刚被唤醒的加入等待队列的头部，**通过信号量阻塞**，直到当前goroutine被唤醒
   5.3. 从这里开始被唤醒的goroutine，都是表示是从阻塞队列里出来的。goroutine被唤醒后，判断当前state是否是饥饿状态，如果不是则更新锁的状态为被唤醒，表示有G被唤醒，继续循环，跳到 2
   5.4. 如果当前state是饥饿状态，当前goroutine获取锁，waiter数量 - 1，设置当前锁的状态是饥饿状态，如果当前goroutine是队列中最后一个goroutine，清除当前锁的饥饿状态，**更新当前锁的状态和waiter数量，退出结束**

7. 如果更新失败，设置old state 等于 当前锁的状态

当前goroutine能获取锁，是通过是否能成功修改锁的状态修改为持有锁实现的。

```go
func (m *Mutex) Lock() {
	// cas的方式获取锁，获取到之后立即返回
	if atomic.CompareAndSwapInt32(&m.state, 0, mutexLocked) {
		return
	}
	// 获取不到锁，说明已被其他goroutine获取到了锁，此时会尝试通过自旋的方式等待锁的释放
	m.lockSlow()
}

// 这个方法的代码，是会被多个G同时执行的
func (m *Mutex) lockSlow() {
	var waitStartTime int64
	starving := false
    // 当前goroutine是否被唤醒
	awoke := false
	iter := 0
	old := m.state
	for {
		// 判断当前state已被锁，且非饥饿状态，且能自旋（能否自旋的条件见上）
		if old&(mutexLocked|mutexStarving) == mutexLocked && runtime_canSpin(iter) {
			// 自旋过程中如果发现state还没有设置woken标识，则进行设置，标记自己被唤醒
            // 自旋是为了让其他goroutine在释放锁后能第一时间唤醒此goroutine
			if !awoke && old&mutexWoken == 0 && old>>mutexWaiterShift != 0 &&
				atomic.CompareAndSwapInt32(&m.state, old, old|mutexWoken) {
				awoke = true
			}
            // 进入自旋
			runtime_doSpin()
			iter++
			old = m.state
			continue
		}
        // old是当前锁状态，new是期望锁状态
		new := old
		// 如果此时是非饥饿状态，期望锁状态设置为持有锁
		if old&mutexStarving == 0 {
			new |= mutexLocked
		}
        // 如果已经持有锁 或者 是饥饿状态，state的waiter数量+1，表示当前goroutine在等待
		if old&(mutexLocked|mutexStarving) != 0 {
			new += 1 <<mutexWaiterShift
		}
		// 如果此时是饥饿状态，还持有锁，期望锁状态设置为饥饿
		if starving && old&mutexLocked != 0 {
			new |= mutexStarving
		}
        // goroutine被唤醒，期望锁状态设置为唤醒
		if awoke {
            // 如果此时非唤醒，说明锁的状态不一致，抛错误
			if new&mutexWoken == 0 {
				throw("sync: inconsistent mutex state")
			}
			new &^= mutexWoken
		}
        // cas更新锁的状态，这里锁的状态可能是4种中其中一种
		if atomic.CompareAndSwapInt32(&m.state, old, new) {
            // 如果old不是饥饿状态也不是被锁状态，表明当前goroutine已通过cas获取到了锁，break
			if old&(mutexLocked|mutexStarving) == 0 {
				break
			}
			// 如果之前已经在等了，就排在队首
			queueLifo := waitStartTime != 0
            // 如果之前没在等，则初始化等待时间
			if waitStartTime == 0 {
				waitStartTime = runtime_nanotime()
			}
            // 该方法会不断尝试获取锁并陷入休眠等待信号量的释放，一旦当前 goroutine 可以获取信号量，它就会立刻返回
			runtime_SemacquireMutex(&m.sema, queueLifo, 1)
            // 走到这一步，说明当前goroutine是进过阻塞队列的，所以接下来在饥饿状态下会优先获取锁
            // 如果当前goroutine是饥饿状态，或者已经等待超过1ms，就设置为饥饿状态
			starving = starving || runtime_nanotime()-waitStartTime > starvationThresholdNs
			old = m.state
            // 如果是饥饿模式
			if old&mutexStarving != 0 {
				// 如果goroutine被唤醒，且处于饥饿状态，锁的所有权转移给当前goroutine
				if old&(mutexLocked|mutexWoken) != 0 || old>>mutexWaiterShift == 0 {
					throw("sync: inconsistent mutex state")
				}
                // 当前goroutine获取锁，waiter数量-1
				delta := int32(mutexLocked - 1<<mutexWaiterShift)
                // 如果当前goroutine非饥饿状态，或者 当前goroutine是队列中最后一个goroutine
				if !starving || old>>mutexWaiterShift == 1 {
					// 退出饥饿模式
					delta -= mutexStarving
				}
                // 当前goroutine成功修改锁状态为持有锁
				atomic.AddInt32(&m.state, delta)
				break
			}
            // 不是饥饿模式，就把当前goroutine设置为被唤醒，自旋次数重置为0
			awoke = true
			iter = 0
		} else {
            // cas不成功，没有拿到锁，锁被其他goroutine获取或者锁没有被释放，更新状态，重新循环
			old = m.state
		}
	}
}
```

## Unlock方法

1. 将state的锁位-1，如果state=0，即此时没有加锁，且没有正在等待获取锁的goroutine，则直接结束方法，如果state != 0，执行unlockSlow方法，唤醒等待的goroutine；
2. 如果Mutex处于饥饿状态，当前goroutine不更新锁状态，直接唤醒等待队列中的waiter，继续执行，相当于解锁了，然后由等待队列中的队首goroutine获得锁；
3. 如果Mutex处于正常状态，如果没有waiter，或者已经有在处理的waiter的情况，则直接释放锁，state锁位-1，返回；否则，waiter数-1，设置唤醒标记，通过CAS解锁，唤醒在等待锁的goroutine，此时新老goroutine一起竞争锁；

```go
func (m *Mutex) Unlock() {
    // 修改state的状态为释放锁
	new := atomic.AddInt32(&m.state, -mutexLocked)
	if new != 0 {
		// 说明此时没有成功解锁，或者有其他goroutine在等待解锁
		m.unlockSlow(new)
	}
}

func (m *Mutex) unlockSlow(new int32) {
	if (new+mutexLocked)&mutexLocked == 0 {
		throw("sync: unlock of unlocked mutex")
	}
    // 非饥饿模式下
	if new&mutexStarving == 0 {
		old := new
		for {
			// 如果没有等待的goroutine，或者 锁有以下几种情况时，直接返回
            // 1. 锁被其他goroutine获取了
            // 2. 或者有等待的goroutine被唤醒，不用再唤醒阻塞队列里的goroutine，可以直接返回
            // 3. 或者锁是饥饿模式，锁之后要直接交给等待队列队首的goroutine
			if old>>mutexWaiterShift == 0 || old&(mutexLocked|mutexWoken|mutexStarving) != 0 {
				return
			}
			// 能走到这这里说明此时锁的状态还是空闲，
            // 且没有goroutine被唤醒，且队列中有goroutine在等待获取锁
            // 等待获取锁的goroutine数量-1，设置woken标识
			new = (old - 1<<mutexWaiterShift) | mutexWoken
            // 设置新的state，通过信号量唤醒一个阻塞的goroutine获取锁
            // 此时可能会新老的waiter一起竞争
			if atomic.CompareAndSwapInt32(&m.state, old, new) {
				runtime_Semrelease(&m.sema, false, 1)
				return
			}
			old = m.state
		}
	} else {
		// 饥饿模式下，直接将锁的所有权给队首的goroutine，即第二个参数为true
        // 此时的state还没加锁，被唤醒的goroutine会设置它，如果此时有新的goroutine来请求锁，因为还处于饥饿状态，就仍然认为还被锁，新来的goroutine不会抢到锁
		runtime_Semrelease(&m.sema, true, 1)
	}
}
```

## 基于Mutex的拓展

* 可重入锁
* 增加tryLock方法，通过返回true或false来表示获取锁成功或失败，主要用于控制获取锁失败后的行为，而不用阻塞在方法调用上
* 增加等待计数器，比如等待多少时间后还没获取到锁则放弃
* 增加可观测性指标，比如等待锁的goroutine的数量，需要使用`unsafe.Pointer方法`获取Mutex中的state的值，解析出正在等待的goroutine的数量
* 实现线程安全的队列，通过在出队和入队方法中使用Mutex保证线程安全

## 关于Mutex中的sema

golang底层通过`runtime_SemacquireMutex`和`runtime_Semrelease`来实现切换阻塞协程和释放被阻塞协程重新运行等操作。

在runtime中，有一个长度是251的全局semtable数组，每个元素是一棵平衡树的根，树的每个节点是sudog结构组成的一个双向链表。

semtable会被多个协程操作，有并发问题，底层使用真正的锁，依赖操作系统实现，不能被用户使用。

Mutex中的sema是一个信号量，Mutex通过sema字段，取其地址右移三位再对数组长度取模，得到semtable的索引，映射到semtable数组，从而知道goroutine被包装成sudog之后要存在semtable数组中的哪一棵平衡树上，以此就可以通过同一个信号量找到对应的在等待的协程双向链表。

但是不同的信号量地址可能会映射到同一个semtable索引，为了避免唤醒错误的协程，会对拿出来的平衡树进行遍历，匹配sema的地址，取出对应的协程。

# RWMutex - 读写锁

## 数据结构

```go
type RWMutex struct {
    w Mutex           // 互斥锁解决多个writer的竞争
    writerSem uint32  // writer信号量 
    readerSem uint32  // reader信号量 
    readerCount int32 // reader的数量，可以是负数，负数表示此时有writer等待请求锁，此时会阻塞reader
    readerWait int32  // 等待读完成的reader的数量，保证写操作不会被读操作阻塞而饿死
}
const rwmutexMaxReaders = 1 << 30 // 最大的reader数量
```

## 基本

* 主要提升Mutex在读多写少的场景下的吞吐量，读时共享锁，写时排他锁，基于Mutex实现

* 由5个方法构成：
  * Lock/Unlock：写操作时调用的方法。如果锁已经被 reader 或者 writer 持有，那么，Lock 方法会一直阻塞，直到能获取到锁；Unlock 则是配对的释放锁的方法。
  * RLock/RUnlock：读操作时调用的方法。如果锁已经被 writer 持有的话，RLock 方法会一直阻塞，直到能获取到锁，否则就直接返回；而 RUnlock 是 reader 释放锁的方法。
  * RLocker：这个方法的作用是为读操作返回一个 Locker 接口的对象。它的 Lock 方法会调用 RWMutex 的 RLock 方法，它的 Unlock 方法会调用 RWMutex 的 RUnlock 方法
  
* 同Mutex，RWMutex的零值是未加锁状态，无需显示地初始化

* 由于读写锁的存在，可能会有饥饿问题：比如因为读多写少，导致写锁一直加不上，因此go的RWMutex使用的是写锁优先策略：

  如果已经有一个writer在等待请求锁的话，会阻止新的reader请求读锁，优先保证writer。

  如果已经有一些reader请求了读锁，则新请求的writer会等待在其之前的reader都释放掉读锁后才请求获取写锁，等待writer解锁后，后续的reader才能继续请求锁。

* 同Mutex，均为不可重入，使用时应避免复制；

  要注意reader在加读锁后，想要加写锁，则必须要先解除读锁后才能解除写锁，否则会形成相互依赖导致死锁；比如先加读锁，再加写锁，解除写锁，解除读锁，这样就会导致死锁，因为加写锁时，需要读锁先释放，而读锁释放又依赖写锁释放，从而导致死锁

  注意reader是可以重复加读锁的，重复加读锁时，外层reader必须等里层的reader释放锁后自己才能释放锁。

* 必须先使用RLock / Lock方法才能使用RUnlock / Unlock方法，否则会panic，重复释放锁也会panic。

* 可以利用RWMutex实现线程安全的map

## RLock / RUnlock 方法

**仅对readerCount值进行原子操作**，还有就是操作当前goroutine和reader信号量

1. RLock时，对readerCount的值+1，判断是否< 0，如果是，说明此时有writer在竞争锁或已持有锁，则将当前goroutine加入readerSem指向的队列中，进行等待，防止写锁饥饿。
2. RUnlock时，对readerCount的值-1，判断是否<0，如果是，说明当前有writer在竞争锁，调用`rUnlockSlow方法`，对readerWait的值-1，判断是否=0，如果是，说明当前goroutine是最后一个要解除读锁的，此时会唤醒要请求写锁的writer。

```go
func (rw *RWMutex) RLock() {
	if atomic.AddInt32(&rw.readerCount, 1) < 0 {
        // readerCount小于0，说明有Writer，此时阻塞读操作
		runtime_SemacquireMutex(&rw.readerSem, false, 0)
	}
}

func (rw *RWMutex) RUnlock() {
	if r := atomic.AddInt32(&rw.readerCount, -1); r < 0 {
		// readerCount小于0，说明有Writer，判断要不要唤醒被阻塞的Writer
		rw.rUnlockSlow(r)
	}
}

// 进入此方法说明有正在等待的writer
func (rw *RWMutex) rUnlockSlow(r int32) {
	if r+1 == 0 || r+1 == -rwmutexMaxReaders {
		race.Enable()
		throw("sync: RUnlock of unlocked RWMutex")
	}
	if atomic.AddInt32(&rw.readerWait, -1) == 0 {
		// readerWaiter等于0，说明此时是最后一个reader，此时可以唤醒被阻塞的writer
		runtime_Semrelease(&rw.writerSem, false, 1)
	}
}
```

## Lock方法

RWMutex**内部使用Mutex实现写锁互斥**，解决多个writer间的竞争，readerWait字段实现写操作不会被读操作阻塞而饿死。

1. 调用w的Lock方法加锁，防止其他writer上锁，cas反转 readerCount的值并更新到RWMutex中，使其变成负数`readerCount - rwmutexMaxReaders` 告诉reader有writer要请求锁；
2. 如果此时`readerCount != 0`，说明当前有reader持有读锁，需要记录需要等待完成的reader的数量，即readerWait的值（readerWaiter + readerCount），并且如果此时readerWait != 0，将当前goroutine加入writerSema指向的队列中，进行等待。直到有goroutine调用RUnlock方法且是最后一个释放锁时，才会被唤醒。

```go
func (rw *RWMutex) Lock() {
	// 加锁，保证只有一个writer能处理
	rw.w.Lock()
	// readerCount取反进行更新，表示有writer在执行，阻塞后面的读操作，
    // 因为readerCount，readerWait都是全局变量，在读锁方法那边是没有锁保护的，所以是cas保证并发安全
    // readerCount再取反回来，用来更新readerWait的值，判断是否有读操作在等待
	r := atomic.AddInt32(&rw.readerCount, -rwmutexMaxReaders) + rwmutexMaxReaders
	// readerWait 不等于0，说明有reader在执行，需要挂起当前的写操作，直到RUnlock被调用来唤醒
	if r != 0 && atomic.AddInt32(&rw.readerWait, r) != 0 {
		runtime_SemacquireMutex(&rw.writerSem, false, 0)
	}
}
```

## Unlock方法

1. cas反转readerCount的值（readerCount + rwmutexMaxReaders），使其变成reader的数量，唤醒这些reader
2. 调用w的Unlock方法释放当前goroutine的锁，让其他writer可以继续竞争。

```go
func (rw *RWMutex) Unlock() {
	// 反转readerCount值使其变正数，表示可以进行读操作
	r := atomic.AddInt32(&rw.readerCount, rwmutexMaxReaders)
	if r >= rwmutexMaxReaders {
		race.Enable()
		throw("sync: Unlock of unlocked RWMutex")
	}
	// 根据readerWait唤醒正在阻塞的读操作
	for i := 0; i < int(r); i++ {
		runtime_Semrelease(&rw.readerSem, false, 0)
	}
	// 解锁，允许其他写操作执行
	rw.w.Unlock()
}
```

# sync.Map

## 数据结构

```go
type Map struct {
    // 锁，用于保护dirty
	mu Mutex
	// 存读的数据，只读，由dirty提升得到
	read atomic.Value
	// 包含最新写入的数据，并且在写的时候，如果dirty是nil，会把read中未被删除的数据拷贝到该dirty中
	dirty map[interface{}]*entry
    // 当从read中读不到数据，但在dirty中读到数据时该值+1, 当len(dirty) == misses时，将dirty拷贝到read中，此动作会发生在get和delete的操作中
	misses int
}

type readOnly struct {
	m       map[interface{}]*entry
    // true表明dirty中存在read中没有的键值对，有两种情况：1.被删除的key，只能在read中找到；2.新增加的key，只能在dirty中找到
	amended bool
}

// read和dirty都包含了*entry，里面的p是一个指针，read和dirty各自维护了一套key，但他们都指向同一个value
type entry struct {
    // p的状态有三种：1.=nil，表示键值对已被删除；2.=expunged，表示该key被标记删除；3.=正常值
    p unsafe.Pointer
}
```

## 基本

* 基本的并发安全map的实现：将map与RWMutex封装成一个结构体，使用读写锁封装map的各种操作即可。

* 使用RWMutex封装的并发安全的map，因为锁的粒度太大，性能不会太好；通过减少锁的粒度和持有锁的时间，可以提升性能，常见的减少锁的粒度是将锁分片，将锁进行分片，分别控制map中不同的范围的key，类似JDK7中的ConcurrentHashMap的segment锁实现。

* 官方出品的sync.Map，有六个方法：

  * LoadOrStore：根据key获取value，如果该key存在且没有被标记为删除，则返回原来的value和true，不存在则进行store，返回该value和false
  * Load：根据key获取value
  * Delete：删除
  * LoadAndDelete：根据key删除对应的键值对，如果可以存在，返回对应的value和true
  * Range：遍历
  * Store：添加key和value

* 官方出品的sync.Map，但它只有在部分特殊的场景里才有优势，比如一个只会增长的map，一个key只会被写一次，读很多次；或者 多个goroutine为不相交的键集读、写和重写键值对；

  sync.Map内部有两个map，一个只读read，一个可写dirty，对只读read的操作(读、更新、删除)不需要加锁，以此减少锁对性能的影响；
  
* sync.Map没有len方法，要获取里面有多少个key只能遍历获取；

## Store方法

**创建新dirty时，将read中非删除的键值对赋值给dirty是在store方法中执行。**

1. 更新或写入键值对时，先判断read中是否存在，如果存在，会自旋更新该键值对直到成功；

   原因是**read中的键值对，一定包含了dirty中的键值对**，另外，read和dirty指向同一个value，所以直接修改一次即可；

2. 如果read中读不到，才会进行加锁；

   加锁后再次判断read中是否存在，确定read中真的不存在才会操作dirty；

3. 如果read中存在，判断该key是否被删除，如果是，更新dirty的键值对，如果不是，更新read中的键值对；

4. 如果read中不存在，则读取dirty，判断dirty是否存在，存在则更新dirty的键值对；

5. 如果dirty不存在，且dirty中不存在有的键值对在read中没有，如果dirty为空，创建新dirty，同时需要遍历把read中非删除的键值对赋给dirty；更新`read.amended`的值，表明dirty中存在read中没有的键值对；

6. 最后再将新的键值对添加到dirty中；

7. 解锁；

总结：如果是新key，则加锁，优先put到dirty中，如果是dirty为空，则创建新dirty，将read中非删除键值对赋值给新dirty，将read标记为有key在dirty中但不存在在read中，解锁；如果是已存在的key，由于read和dirty的value是同一个引用，直接cas更新read即可。

```go
read, _ := m.read.Load().(readOnly)
	if e, ok := read.m[key]; ok && e.tryStore(&value) {
		return
	}

	m.mu.Lock()
	read, _ = m.read.Load().(readOnly)
	if e, ok := read.m[key]; ok {
		if e.unexpungeLocked() {
			m.dirty[key] = e
		}
		e.storeLocked(&value)
	} else if e, ok := m.dirty[key]; ok {
		e.storeLocked(&value)
	} else {
		if !read.amended {
            // 将readMap中非删除的键值对赋值给dirtyMap
			m.dirtyLocked()
            // 标记dirtyMap中包含readMap中不存在的键值对
			m.read.Store(readOnly{m: read.m, amended: true})
		}
		m.dirty[key] = newEntry(value)
	}
	m.mu.Unlock()
}
```

## Load方法

**将dirty提升为read这个操作在load方法中执行。**

1. 不加锁，优先读取read中的键值对，判断key是否存在，存在则返回；
2. 如果read中不存在，且dirty中包含了read中不存在的键值对，加锁，再次读取read中的键值对；
3. 判断read中的键值对是否存在，存在则返回；
4. 如果read中不存在，且dirty中包含了read中不存在的键值对，查询dirty中是否存在；
5. 同时增加miss的值(miss表示读取穿透的次数)，当miss的值等于dirty的长度时，就会将dirty提升为read，只需简单的赋值即可，然后将dirty置为null，重置miss数，避免总是从dirty中加锁读取；
6. 解锁，将dirty中的查询结果返回；

总结：优先读read中的key，读不到，判断read的标记（dirty是否包含read中不存在的key），加锁，再读read，还读不到，再判断dirty是否包含read中不存在的key，如果是，才会去读dirty，同时miss值+1，当miss值=dirty长度时，将dirty中的键值对赋值给read，解锁。

```go
func (m *Map) Load(key interface{}) (value interface{}, ok bool) {
	read, _ := m.read.Load().(readOnly)
	e, ok := read.m[key]
	if !ok && read.amended {
		m.mu.Lock()
		read, _ = m.read.Load().(readOnly)
		e, ok = read.m[key]
		if !ok && read.amended {
			e, ok = m.dirty[key]
            // 增加miss的值，判断释放要将dity提升为read
			m.missLocked()
		}
		m.mu.Unlock()
	}
	if !ok {
		return nil, false
	}
	return e.load()
}

func (m *Map) missLocked() {
	m.misses++
	if m.misses < len(m.dirty) {
		return
	}
    // 将dirtyMap提升给readMap
	m.read.Store(readOnly{m: m.dirty})
	m.dirty = nil
	m.misses = 0
}
```

## Delete方法

**将dirty提升为read这个操作也会在delete方法中执行。**

1. 判断read中是否存在该key；
2. 如果read中不存在，且dirty中包含了read中不存在的key，加锁；
3. 如果read中真的不存在，且dirty中包含了read中不存在的key，删除dirty中该key和value，此时miss也会 + 1，当miss值=dirty长度时，将dirty中非删除的键值对赋值给read，解锁；
4. 如果存在该key（此时该键值对只会在read中存在），自旋，直接在该key对应的entry打上expunged标记，表示删除；

总结：优先读read，读不到，加锁，如果dirty中存在该key，dirty中该键值对会被真正的删除，但此时read中的键值对还没被删除，只是其key对应的value被打上一个expunged标记，表示删除，使其在被get的时候能分辨出来，read中该key真正的删除只有在将dirty提升为read的时候；

```go
func (m *Map) Delete(key interface{}) {
	m.LoadAndDelete(key)
}

func (m *Map) LoadAndDelete(key interface{}) (value interface{}, loaded bool) {
	read, _ := m.read.Load().(readOnly)
	e, ok := read.m[key]
	if !ok && read.amended {
		m.mu.Lock()
		read, _ = m.read.Load().(readOnly)
		e, ok = read.m[key]
		if !ok && read.amended {
			e, ok = m.dirty[key]
			delete(m.dirty, key)
			// 增加miss的值，判断释放要将dity提升为read
			m.missLocked()
		}
		m.mu.Unlock()
	}
	if ok {
        // 自旋，cas给key打上删除标记
		return e.delete()
	}
	return nil, false
}
```

## LoadOrStore方法

基本上和Store方法一样，只是增多一点逻辑：如果该key存在且没有被标记为删除，则返回原来的value和true，不存在则进行store，返回该value和false。

# WaitGroup

## 数据结构

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

信号量的作用：

- 当信号量>0时，表示资源可用，获取信号量时系统自动将信号量减1；
- 当信号量==0时，表示资源暂不可用，获取信号量时，当前线程会进入睡眠，当信号量为正时被唤醒。

## 基本

* state的值由32bit的值表示信号量，64bit的值表示计数和waiter的数量组成。因为原子操作只能64bit对齐，而计数值和waiter的数量是一个64bit的值，在64bit的编译器上，一次读取是64bit，刚好可以直接操作，但是如果是32bit的机器，一次只能读32bit，为了保证进行64bit对齐时一定能获取到计数值和waiter的值，在进行64bit的原子操作对齐时，第一次是对齐到了一个空32bit和第一个32bit的值，第二次对齐就能保证获取了。
* 同RWMutex，WaitGroup的三个方法内还很多data race检查，保证并发时候共享数据的正确性，一旦检查出有问题，会直接panic
* 一开始设置WaitGroup的计数值必须大于等于0，否则会过不了data race检查，直接panic
* Add的值必须 等于 调用Done的次数，当Done的次数超过计数值，也会panic
* Wait方法的调用一定要晚于Add，否则会导致死锁
* WaitGroup可以在计数值为0时可重复使用
* noCopy是一个实现了Lock接口的结构体，且不对外暴露，其Lock方法和Unlock方法都是空实现，用于vet工具检查WaitGroup在使用过程中有没有被复制；当我们自定义的结构不想被复制使用时，也可以使用它。
* 使用时要避免复制

## Add方法

1. 原子的将WaitGroup的计数值加到state上，如果当前的计数值 > 0，或者 waiter的数量等于0，直接返回
2. 否则，即代表当前的计数值为0，但waiter的数量不一定为0，此时state的值就是waiter的数量
3. 将state的值设置为0，即waiter的数量设置为0，然后唤醒所有waiter

```go
func (wg *WaitGroup) state() (statep *uint64, semap *uint32) {
	if uintptr(unsafe.Pointer(&wg.state1))%8 == 0 {
		return (*uint64)(unsafe.Pointer(&wg.state1)), &wg.state1[2]
	} else {
		return (*uint64)(unsafe.Pointer(&wg.state1[1])), &wg.state1[0]
	}
}

func (wg *WaitGroup) Add(delta int) {
	statep, semap := wg.state()
	state := atomic.AddUint64(statep, uint64(delta)<<32)
	v := int32(state >> 32) // 计数器
	w := uint32(state) // 等待计数器
	if v < 0 {
		panic("sync: negative WaitGroup counter")
	}
    // 等待计数器不为0，说明已经执行了wait方法，此时不允许调用add方法
	if w != 0 && delta > 0 && v == int32(delta) {
		panic("sync: WaitGroup misuse: Add called concurrently with Wait")
	}
	if v > 0 || w == 0 {
		return
	}
    // 如果执行到这里，说明计数器为0，但等待计数器不为0
	// 说明此时发生了并发调用Add方法和wait方法，并发调用导致状态不一致
	if *statep != state {
		panic("sync: WaitGroup misuse: Add called concurrently with Wait")
	}
    // 状态位清零，唤醒等待的goroutine
	*statep = 0
	for ; w != 0; w-- {
		runtime_Semrelease(semap, false, 0)
	}
}
```

## Done方法

1. 调用Add方法，只是参数为-1，表示计数值 - 1，有一个waiter完成其任务；waiter指的是调用Wait方法的goroutine

```go
func (wg *WaitGroup) Done() {
	wg.Add(-1)
}
```

## Wait方法

1. 循环内不断检测state的值，当其计数值为0时，说明所有任务已经完成，调用这个方法的goroutine不必继续等待，直接返回，结束该方法
2. 否则，说明此时还有任务没完成，调用该方法的goroutine成为waiter，把waiter的数量 + 1，加入等待队列，阻塞自己

```go
func (wg *WaitGroup) Wait() {
	statep, semap := wg.state()
	for {
		state := atomic.LoadUint64(statep)
		v := int32(state >> 32)
		w := uint32(state)
        // 计数器为0，说明goroutine执行结束
		if v == 0 {
			return
		}
		// 调用wait方法的goroutine的数目+1，此时调用Add方法时就能知道有多少goroutine在等待
		if atomic.CompareAndSwapUint64(statep, state, state+1) {
			// 阻塞等待，直至被唤醒
			runtime_Semacquire(semap)
			if *statep != 0 {
				panic("sync: WaitGroup is reused before previous Wait has returned")
			}
			return
		}
	}
}
```

# Cond = condition + Wait/Notify

## 数据结构

```go
type Cond struct { 
    noCopy noCopy // 使用vet工具在编译时检测是否被复制
    checker copyChecker // 用于运行时被检测是否被复制
    L Locker // 当观察或者修改等待条件的时候需要加锁
    notify notifyList // 等待队列 
}
```

## 基本

* 初始化时，要指定使用的锁，比如Mutex

* Cond 是等待某个条件满足，这个条件的修改可以被任意多的 goroutine 更新，而且 Cond 的 Wait 不关心也不知道其他 goroutine 的数量，只关心等待条件。

* Signal方法，类似Java的notify方法，允许调用者唤醒一个等待此Cond的goroutine，如果此时没有waiter，则无事发生；如果此时Cond的等待队列中有多个goroutine，则移除队首的goroutine并唤醒；

  使用Signal方法时不强求已调用了加锁方法

* Broadcast方法，类似Java的notifyAll方法，允许调用者唤醒等待此Cond的所有goroutine，如果此时没有waiter，则无事发生；如果此时Cond的等待队列中有多个goroutine，则清空整个等待队列，全部唤醒；

  使用Broadcast方法时不强求已调用了加锁方法

* Wait方法，类似Java的wait方法，把调用者的goroutine放入Cond的等待队列中并阻塞，直到被Signal或Broadcast方法唤醒

  调用Wait方法时必须已调用了加锁方法，否则会panic，因为Wait方法内是**先解锁**，将当前goroutine加入到**等待**队列，然后**解锁**，阻塞休眠当前goroutine，直到被**唤醒**，然后**加锁**

  调用Wait后一定要检测等待条件是否满足，还需不需要继续等待，在等待的goroutine被唤醒不等于等待条件已满足，可能只是被某个goroutine唤醒而已，被唤醒时，只是得到了一次检测机会。

# Once

## 数据结构

```go
type Once struct {
    done uint32
    m Mutex
}
```

## 基本

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

# Pool

这里是针对go 1.13之后的版本

## 数据结构

```go
type Pool struct {
    // 使用go vet工具可以检测用户代码是否复制了pool
	noCopy noCopy

    // 每个 P 的本地队列，实际类型为 [P]poolLocal数组，长度是固定的，P的id对应[P]poolLocal下标索引，通过这样的设计，多个 G 使用同一个Pool时，减少竞争，提升性能
	local     unsafe.Pointer
	// [P]poolLocal 本地队列的长度
	localSize uintptr

    // GC 时使用，分别接管 local 和 localSize，victim机制用于减少GC后冷启动导致的性能抖动，让对象分配更平滑，降低GC压力的同时提高命中率，由poolCleanup()方法操作
	victim     unsafe.Pointer
	victimSize uintptr

	// 自定义的对象创建回调函数，当 pool 中无可用对象时会调用此函数
	New func() interface{}
}
```

当Pool没有缓存对象时，调用 New 函数生成以下对象

```go
type poolLocal struct {
	poolLocalInternal

	// 将 poolLocal 补齐至两个缓存行的倍数，防止 false sharing,
	// 每个缓存行具有 64 bytes，即 512 bit
	// 目前我们的处理器一般拥有 32 * 1024 / 64 = 512 条缓存行
	// 伪共享，仅占位用，防止在 cache line 上分配多个 poolLocalInternal
	pad [128 - unsafe.Sizeof(poolLocalInternal{})%128]byte
}

// Local 每个P都有一个
type poolLocalInternal struct {
    // P 的私有对象，使用时无需要加锁，用于不同G执行get和put
	private interface{}
    // 双向链表
    // 同一个P上不同G可以多次执行put方法，需要有地方能存储, 并且别的P上的G可能过来偷，通过cas实现
	shared  poolChain
}
```

poolChain 是一个双向链表的实现；

poolDequeue 被实现为单生产者，多消费者的固定大小无锁的环形队列，生产者可以从 head 插入和删除，而消费者仅能从 tail 删除

```go
type poolChain struct {
	// 只有生产者会 push to，不用加锁
	head *poolChainElt

	// 读写需要原子控制，pop from
	tail *poolChainElt
}

type poolChainElt struct {
	poolDequeue

	// next 被 producer 写，consumer 读。所以只会从 nil 变成 non-nil
	// prev 被 consumer 写，producer 读。所以只会从 non-nil 变成 nil
	next, prev *poolChainElt
}

type poolDequeue struct {
    // headTail 包含一个 32 位的 head (高32位)和一个 32 位的 tail(低32位) 指针。这两个值都和 len(vals)-1 取模过。
	// tail 是队列中最老的数据，head 指向下一个将要填充的 slot
    // slots 的有效范围是 [tail, head)，由 consumers 持有。
    // 通过对其cas操作保证并发安全
	headTail uint64

	// vals 是一个存储 interface{} 的环形队列，它的 size 必须是 2 的幂，初始化长度为8
	// 如果 slot 为空，则 vals[i].typ 为空；否则，非空。
	// 一个 slot 在这时宣告无效：tail 不指向它了，vals[i].typ 为 nil
	// 由 consumer 设置成 nil，由 producer 读
	vals []eface
}
```

![](https://github.com/Nixum/Java-Note/raw/master/picture/go_syncPool数据结构.png)

* 每次垃圾回收时，Pool会把victim中的对象移除，然后把local的数据给victim，local置为nil，如果此时有Get方法被调用，则会从victim中获取对象。通过这种方式，避免缓存元素被大量回收后再再次使用时新建很多对象；
* 获取重用对象时，先从local中获取，获取不到再从victim中获取；
* poolLocalInternal用于CPU缓存对齐，避免false sharing；
* private字段代表一个可复用对象，且只能由相应的一个P存取，因为一个P同时只能执行一个goroutine，所以不会有并发问题；
* shared字段可以被任意的P访问，但是只有本地的P能pushHead/popHead，其他P可以popTail，相当于只有一个本地P作为生产者，多个P作为消费者，它由一个lock-free的队列实现；

## 基本

* sync.Pool用于保存一组可独立访问的临时对象，它池化的对象如果没有被其他对象持有引用，可能会在未来某个时间点（GC发生时）被回收掉；

* sync.Pool是并发安全的，多个gotoutine可以并发调用它存取对象；

* 不能复制使用；

* 在1.13以前，保证并发安全使用了带锁的队列，且在GC时，直接清空所有Pool的`local`和`poollocal.shared`，GC的时间可能会很长;

  1.13后，改成了lock-free的队列实现，避免锁对性能的影响，且在GC时，使用victim作为次级缓存，GC时将对象放入其中，下次GC来临之前，如果有 Get 调用则会从victim中取，直到下一次GC来临时回收，拉长实际回收时间，使得单位时间内GC的开销减少；

* 包含了三个方法：New、Get、Put；**Get方法调用时，会从池中移走该元素**；

* 当Pool里没有元素可用时，Get方法会返回nil；可以向Pool中Put一个nil的值，Pool会将其忽略；

* 在使用Put归还对象时，需要将对象的属性reset；

* 当使用Pool作为buffer池时，要注意buffer如果太大，reset后它就会占很大空间，引起内存泄漏，因此在回收元素时，需要检查大小，如果太大了就直接置为null，丢弃即可；

* Pool 里对象的生命周期受 GC 影响，不适合于做连接池，因为连接池需要自己管理对象的生命周期；

* Pool 不可以指定大小，大小只受制于 GC 临界值；

* `procPin` 将 G 和 P 绑定，防止 G 被抢占。在绑定期间，GC 无法清理缓存的对象；

* `sync.Pool` 的最底层使用链表，链表元素是切片(当作环形队列)，并将缓存的对象存储在切片中；

* 底层切片初始化长度为8，始终保持2的n次幂的增长，最大的容量是2^30，达到上限时，再生成的队列容量都是2^30；链表的节点的环形队列长度是 `head -> 32 -> 16 -> 8 -> tail`；

* Get方法调用时，**如果是从其他P的local.shared的尾部窃取复用对象**，同时会移除环形队列里的元素，当环形队列被窃取到为空时，会移除当前节点；

## Get方法

1. 如果非第一次访问，调用`p.pin()`函数，**将当前 G 固定在P上，防止被抢占**，并获取pid，再根据pid号找到当前P对应的poolLocal；如果P的数量大于poolLocal的数量，就会进入`p.pinSlow()`方法，加锁，创建P个poolLocal。
2. 拿到poolLocal后，优先从local的private字段取出一个元素，将private置为null；
3. 如果从private取出的元素为null，则从当前的`local.shared`的head中取出一个双端环形队列，遍历队列获取元素，如果有pop出来并返回；如果还取不到，沿着pre指针到下一个双端环形队列继续获取，直到获取到或者遍历完双向链表；
4. 如果还没有的话，调用`getSlow`函数，遍历其他P的poolLocal（从pid+1对应的poolLocal开始），从它们shared 的 tail 中弹出一个双端环形队列，遍历队列获取元素，如果有，pop出来并返回；如果还取不到（如果当前节点为null，则删除），沿着next指针到下一个双端环形队列继续获取，如果还没有，直到获取到或者遍历完双向链表；如果还没有，就到别的P上继续获取；
5. 如果所有P的poolLocal.shared都没有，则对victim中以在同样的方式，先从当前P的poolLocal的private里找，找不到再在shared里找，获取一遍；
6. Pool相关操作**执行完，调用`runtime_procUnpin()`解除非抢占**；
7. 如果还取不到，则调用New函数生成一个，然后返回；

因为当前的G被固定在了P上，在查找元素时不会被其他P执行。

## pin方法

> `pin` 的作用就是将当前 G 和 P 绑定在一起，禁止抢占，并返回对应的 poolLocal 以及 P 的 id。
>
> 如果 G 被抢占，则 G 的状态从 running 变成 runnable，会被放回 P 的 LRQ 或 GRQ，等待下一次调度。下次再执行时，就不一定是和现在的 P 相结合了。因为之后会用到 pid，如果被抢占了，有可能接下来使用的 pid 与所绑定的 P 并非同一个。
>
> 所谓的抢占，就是把 M 绑定的 P 给剥夺了，因为我们后面获取本地的 poolLocal 是根据pid获取的，如果这个过程中 P 被抢走，就乱套了，所以需要设置禁止抢占，实现的原理就是让 M 的locks字段不等于0，比如+1，实际上也相当于对M上锁，让调度器知道 M 不适合抢占，这里就很好体现了数据的局部性：让G和M在被抢占后，仍然找回原来的P，这里通过禁止抢占，来保证数据局部性。
>
> 执行完之后，P 不可抢占，且 GC 不会清扫 Pool 里的对象。

在Pool里，还有一个全局Pool数组，allPools和oldPools，用于保存所有声明的Pool对象，便于GC时遍历所有声明的Pool，使用了victim cache机制让GC更平滑（调用poolCleanup方法）。

当P的数量大于 poolLocal 数组的长度时，就会进入 pinSlow 方法，构建新的 poolLocal 节点。

进入pinSlow方法后，首先会解除G和P的绑定，再上锁，锁定allPools（因为是全局变量），之所以先解除绑定再上锁，主要是锁的粒度比较大，被阻塞的概率也大，如果还占用着P，浪费资源；锁定成功后，才再次进行绑定，由于此时P可能被其他线程占用了，p.local可能会发生变化，此时还需要对pid进行检查，如果P的数量大于 poolLocal 的长度，才创建新的poolLocal数组，长度为P的个数，这一步其实是懒加载，懒汉式初始化 poolLocal数组 作为 P的本地数组，如果是首次创建，p还会加入allPools。

## Put方法

1. 如果Put进来的元素是null，直接返回；
2. 调用`p.pin()`函数，将当前 G 固定在P上，防止被抢占，并获取pid，再根据pid号找到当前P对应的poolLocal；
3. 尝试将put进来的元素赋值给private，如果本地private没有值，直接赋值；
4. 否则，原子操作将其加入到shared对应的双端队列的队首；

## GC前

Pool会在init方法中使用`runtime_registerPoolCleanup`注册GC的钩子`poolCleanup`来进行pool回收处理。

其中一个主要动作是 `poolCleanup()` 方法，该方法主要就是在GC开始前：

1. 遍历oldPools数组，将其中的pool对象的victim置为nil；
2. 遍历allPools数组，将local对象赋值给victim，local对象赋值为nil；
3. 然后将allPools赋值给oldPools，allPools置为nil；

当GC开始时候，就会将 oldPools数组中 pool对象 已释放的 victim cache 中所有对象的回收（因为已经被置为null了）。因为victim cache的设计，pool中的复用对象会在每两个GC循环中清除；

# 原子操作

* 依赖atomic包，因为没有泛型，目前该包支持int32、int64、uint32、unit64、uintptr、Pointer的原子操作，比如Add、CompareAndSwap、Swap、Load、Store等（Pointer不支持Add），对于有符号的数值来说，Add一个负数相当于减；
* 对于现代多核操作系统来说，由于cache、指令重排、可见性问题，一个核对地址的值的更改，在更新到主内存中前，会先存在多级缓存中，此时，多个核看到该数据可能还没看到更新的数据，还在使用旧数据，而atomic包提供的方法会提供内存屏障的功能，保证赋值数据的完整性和可见性；

* atomic操作的对象是一个地址，不是变量值；

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

# Weighted = Semaphore信号量

## 数据结构

```go
type Weighted struct {
    size    int64         // 最大资源数
    cur     int64         // 当前已被使用的资源
    mu      sync.Mutex    // 互斥锁，对字段的保护
    waiters list.List     // 等待队列，通过channel实现通知机制
}
```

## 基本

* 信号量中的PV操作，P：获取资源，如果获取不到，则阻塞，加入到等待队列中；V：释放资源，从等待队列中唤醒一个元素执行P操作

* 二进位信号量，或者说只有一个计数值的信号量，其实相当于go中的Mutex互斥锁

* 初始化时，必须指定初始的信号量

* 只调用Release方法会直接panic；Release方法传入负数，会导致资源被永久持有；因此要保证请求多少资源，就释放多少资源

* Mutex中使用的sema是一个信号量，只是其实现是在runtime中，并没有对外暴露，在扩展包中，暴露了一个信号量工具Weighted

* Weighted分为3个方法：Acquire方法，相当于P操作，第一个参数是context，可以使用context实现timeout或cancel机制，终止goroutine；正常获取到资源时，返回null，否则返回ctx.Err，信号量计数值不变。

  Release方法，相当于V操作，可以释放n个资源，返回给信号量；

  TryAcquire方法，尝试获取n个资源，但不会阻塞，成功时返回true，否则一个也不获取，返回false

* 信号量的实现也可通过buffer为n的channel实现，只是一次只能请求一个资源，而Weighted一次可以请求多个

## Acquire方法

1. 加锁，判断可用资源 >= 入参所需的资源数，且没有waiter，说明资源足够，直接cur+上所需资源数，解锁返回
2. 如果所需资源数>最大资源数，说明是不可能任务，解锁，依赖ctx的Done方法返回，否则一直等待
3. 如果资源数不够，将调用者加入等待队列，并创建一个read chan，用于通知唤醒，解锁
4. 等待唤醒有两种条件，一种是通过read chan唤醒，另一种是通过ctx.Done唤醒

## Release方法

1. 加锁，当前已使用资源数cur - 入参要释放的资源数，唤醒等待队列中的元素，解锁
2. 唤醒等待队列的元素时，会遍历waiters队列，按照先入先出的方式唤醒调用者，前提是释放的资源数要够队首的元素资源的要求，比如只释放了100个资源，但是队首元素要求101个资源，那队列中的所有等待者都将继续等待，直到队首元素出队，这样做是为了避免饥饿

# SingleFlight

## 结构体

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

## 基本

* SingleFlight可以合并多个请求为一个请求，再将该请求的结果返回给多个请求，从而达到合并并发请求的目的，减少并发调用的数量。比如有多个相同的读请求查库，那就可以合并成一个请求查库，再把结果响应回这多个请求中；或者是解决缓存击穿问题，降低对下游服务的并发压力

* 底层由Mutex和Map实现，Mutex保证并发读写保护，Map保存同一个key正在处理的请求

* 包含3个方法，Do方法：提供一个key和一个函数，对于同一个key，在同一时间只有一个函数在执行，之后同一个key并发的请求会等待，等到第一个执行的结果就是该key的所有结果，调用完成后，会移除这个key。返回值shared表示结果是否来自多个相同请求。

  DoChan方法：类似Do方法，只是返回是一个chan，待入参函数执行完，产生结果后就能在chan中接收这个结果

  Forget方法：告诉Group忽略这个key，之后这个key的请求会执行入参函数，而不是等待前一个未完成的入参函数的结果

# CyclicBarrier - 循环栅栏

## 数据结构

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

## 基本

* 类似Java的CyclicBarrier，允许一组goroutine相互等待，到达一个共同的执行点再继续往下执行；同时也可被重复使用。
* CyclicBarrier是一个接口，然后有两个初始化的方法，New方法，指定循环栅栏的参与者数量即可初始化；NewWithAction方法，除了指定参与者数量，第二个参数是一个函数，表示在最后一个参与者到达之后，但其他参与者还没放行之前，会调用该函数
* 每个参与的goroutine都会调用Await方法进行阻塞，当调用Await方法的goroutine的个数=参与者的数量时，Await方法造成的阻塞才会解除

# ErrGroup

* 类似WaitGroup，只是功能更丰富，多了与Context集成，可以通过Context监控是否发生cancel；error可以向上传播，把子任务的错误传递给Wait的调用者
* ErrGroup用于并发处理子任务，将一个大任务拆成几个小任务，通过Go方法并发执行。
* ErrGroup有三个方法：withContext、Go、Wait，用法与WaitGroup相似，只是不需要设置计数值，且可以通过Wait方法获取子任务返回的错误，但它只会返回第一个出现的错误，如果所有子任务都执行成功，返回null；当发生错误时不会立即返回，而是等到其他任务完成了才会返回。

* Go方法会创建一个goroutine来执行子任务，如果并发的量太大，会导致创建大量的goroutine，带来goroutine的调度和GC压力，占用更多资源，解决方案可以是使用worker pool或者信号量来控制goroutine的数量或保持重用
* 子任务如果发生panic会导致程序崩溃

# 检测工具

* go race detector：主要用于检测多个goroutine对共享变量的访问是否存在协程安全问题。编译器通过探测所有内存的访问，加入代码监视对内存地址的访问，在程序运行时，监控共享变量的非同步访问，出现race时，打印告警信息。比如在运行时加入race参数`go run -race main.go`，当执行到一些并发操作时，才会检测运行时是否有并发问题
* 命令`go vet xxx.go`可以进行死锁检测

# 参考

[go中sync.Mutex源码解读](https://www.cnblogs.com/ricklz/p/14535653.html)

[go中waitGroup源码解读](https://www.cnblogs.com/ricklz/p/14496612.html)

[深度解密Go语言之sync.map](https://zhuanlan.zhihu.com/p/344834329)

[golang的对象池sync.pool源码解读](https://zhuanlan.zhihu.com/p/99710992)

[深度解密 Go 语言之 sync.Pool](https://www.cnblogs.com/qcrao-2018/p/12736031.html)
