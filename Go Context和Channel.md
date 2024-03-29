---
title: Go Context和Channel
description: context、channel、select 原理
date: 2021-03-22
weight: 5
categories: ["Go"]
tags: ["Go channel原理", "context原理", "select原理"]
---

[TOC]

# Context

一个接口，包含如下方法，主要用于实现主协程对子协程的控制，作用包括取消执行、设置超时时间、携带键值对等

```go
type Context interface {
    // 获取到期时间，如果没有，ok则返回false
	Deadline() (deadline time.Time, ok bool)
	// 返回一个chan，表示取消信号，如果通道关闭则代表该 Context 已经被取消；如果返回的为 nil，则代表该 Context 是一个永远不会被取消的 Context。
    Done() <-chan struct{}
	// 返回该 Context 被取消的原因。如果只使用 Context 包的 Context 类型的话，那么只可能返回 Canceled （代表被明确取消）或者 DeadlineExceeded （因超时而取消）
    Err() error
    // 获取Context中的键值对
	Value(key interface{}) interface{}
}
```

一个demo，引用：[通知多个子goroutine退出运行](https://strikefreedom.top/goroutine-concurrency-control-and-communication)

```go
 package main
 
 import (
 	"context"
 	"crypto/md5"
 	"fmt"
 	"io/ioutil"
 	"net/http"
 	"sync"
	"time"
)

type favContextKey string

func main() {
	wg := &sync.WaitGroup{}
	values := []string{"https://www.baidu.com/", "https://www.zhihu.com/"}
	ctx, cancel := context.WithCancel(context.Background())

	for _, url := range values {
		wg.Add(1)
		subCtx := context.WithValue(ctx, favContextKey("url"), url)
		go reqURL(subCtx, wg)
	}

	go func() {
		time.Sleep(time.Second * 3)
		cancel()
	}()

	wg.Wait()
	fmt.Println("exit main goroutine")
}

func reqURL(ctx context.Context, wg *sync.WaitGroup) {
	defer wg.Done()
	url, _ := ctx.Value(favContextKey("url")).(string)
	for {
		select {
        // 调用Done方法检测是否有父节点调用cancel方法通知子节点退出运行, chan被close时触发
		case <-ctx.Done():
			fmt.Printf("stop getting url:%s\n", url)
			return
		default:
			r, err := http.Get(url)
			if r.StatusCode == http.StatusOK && err == nil {
				body, _ := ioutil.ReadAll(r.Body)
				subCtx := context.WithValue(ctx, favContextKey("resp"), fmt.Sprintf("%s%x", url, md5.Sum(body)))
				wg.Add(1)
				go showResp(subCtx, wg)
			}
			r.Body.Close()
			//启动子goroutine是为了不阻塞当前goroutine，这里在实际场景中可以去执行其他逻辑，这里为了方便直接sleep一秒
			// doSometing()
			time.Sleep(time.Second * 1)
		}
	}
}

func showResp(ctx context.Context, wg *sync.WaitGroup) {
	defer wg.Done()
	for {
		select {
		case <-ctx.Done():
			fmt.Println("stop showing resp")
			return
		default:
			//子goroutine里一般会处理一些IO任务，如读写数据库或者rpc调用，这里为了方便直接把数据打印
			fmt.Println("printing: ", ctx.Value(favContextKey("resp")))
			time.Sleep(time.Second * 1)
		}
	}
}
```

## emptyCtx

go提供了两个基本的context创建，emptyCtx是`int`类型的重新定义，emptyCtx没有过期时间，不能被取消，不能设置value，作用仅作为context树的根节点。

```go
var (
  background = new(emptyCtx)
  todo       = new(emptyCtx)
)

func Background() Context {
  return background
}

func TODO() Context {
  return todo
}
```

## cancelCtx

通过根context，比如emptyCtx之后，调用`withCancel()`方法可以创建cancelCtx用于取消操作。

有两种方式可触发取消：

1. 返回的CancelFunc被调用，此时会取消当前context和其所有的子context
2. Done这个chan被close了，此时会取消当前context和其所有的子context

```go
type CancelFunc func()

// 创建一个可被取消的context
func WithCancel(parent Context) (ctx Context, cancel CancelFunc) {
	if parent == nil {
		panic("cannot create context from nil parent")
	}
	c := newCancelCtx(parent)
    // 构建树形的cancel
	propagateCancel(parent, &c)
	return &c, func() { c.cancel(true, Canceled) } // canceled是一个error实现
}

func newCancelCtx(parent Context) cancelCtx {
	return cancelCtx{Context: parent}
}

// 主要是将child ctx与parent ctx绑定，放到parent的children属性中
func propagateCancel(parent Context, child canceler) {
	done := parent.Done()
	if done == nil {
		return // parent is never canceled
	}

	select {
	case <-done:
		// parent is already canceled
		child.cancel(false, parent.Err())
		return
	default:
	}

    // 获取parent的cancelCtx，ok是用来判断父context是不是CancelCtx类型的
	if p, ok := parentCancelCtx(parent); ok {
		p.mu.Lock()
		if p.err != nil {
			// parent已经被取消，触发child的取消
			child.cancel(false, p.err)
		} else {
            // parent没有被取消，把当前context作为parent的child
			if p.children == nil {
				p.children = make(map[canceler]struct{})
			}
			p.children[child] = struct{}{}
		}
		p.mu.Unlock()
	} else {
        // 表示parent的ctx不是一个cancelCtx，没有children属性，无法构建成树，只能通过parent的done来向下传播
		atomic.AddInt32(&goroutines, +1)
		go func() {
			select {
			case <-parent.Done():
				child.cancel(false, parent.Err())
			case <-child.Done():
			}
		}()
	}
}
```

可被取消的context实现了canceler接口，具体实现：

```go
type canceler interface {
	cancel(removeFromParent bool, err error)
	Done() <-chan struct{}
}

type cancelCtx struct {
	Context // 存储父context的指针

	mu       sync.Mutex
	done     chan struct{} // 作为取消信号的channel，子协程监听该channel判断是否要cancel
	children map[canceler]struct{} // 被关联的可被取消的context
	err      error                // 第一次取消时被设置
}

func (c *cancelCtx) Done() <-chan struct{} {
   c.mu.Lock()
   if c.done == nil {
      c.done = make(chan struct{})
   }
   d := c.done
   c.mu.Unlock()
   return d
}

func (c *cancelCtx) cancel(removeFromParent bool, err error) {
	// 在向下传播cancel时, 必须带上原始的error
    if err == nil {
		panic("context: internal error: missing cancel error")
	}
	c.mu.Lock()
	if c.err != nil {
		c.mu.Unlock()
		return // already canceled
	}
	c.err = err
	if c.done == nil {
		c.done = closedchan
	} else {
		close(c.done)
	}
	for child := range c.children {
		// 取消所有子context
		child.cancel(false, err)
	}
	c.children = nil
	c.mu.Unlock()

    // 从父context的children中移除当前context
	if removeFromParent {
		removeChild(c.Context, c)
	}
}
```

## timerCtx

可超时自动取消的context，内部使用cancelCtx + timer实现，调用`WithDeadline()`方法可以创建timerCtx用于超时取消操作。

`WithTimeout()方法`和`WithDeadline()方法`，效果是一样的，只是时间的含义不一样。

```go
func WithTimeout(parent Context, timeout time.Duration) (Context, CancelFunc) {
	return WithDeadline(parent, time.Now().Add(timeout))
}
```

关于withTimeout()方法，返回的cancel函数，即使不主动调用，也不影响资源的最终释放，它到时间了也会自动调用，建议是提前主动调用，尽快释放，避免等待时间过长导致浪费。

```go
func WithDeadline(parent Context, deadline time.Time) (Context, CancelFunc) {
  if cur, ok := parent.Deadline(); ok && cur.Before(deadline) {
    // 如果parent可以更早结束, 那么返回一个包装parent的cancelCtx
    return WithCancel(parent)
  }
  c := &timerCtx{
    // 组合一个新的cancelCtx
    cancelCtx: newCancelCtx(parent),
    deadline:  deadline,
  }
  propagateCancel(parent, c) // 组织树形结构
  d := time.Until(deadline)
  if d <= 0 {
    // 如果时间已经到了, 直接触发取消
    c.cancel(true, DeadlineExceeded)
    return c, func() { c.cancel(true, Canceled) }
  }
  c.mu.Lock()
  defer c.mu.Unlock()
  if c.err == nil {
    // 新建定时器, 到期触发取消
    c.timer = time.AfterFunc(d, func() {
      c.cancel(true, DeadlineExceeded)
    })
  }
  // 返回值还有用于直接取消的CancelFunc
  return c, func() { c.cancel(true, Canceled) 
}
    
type timerCtx struct {
  cancelCtx
  timer *time.Timer // Under cancelCtx.mu.

  deadline time.Time
}
```

## valueCtx

`valueCtx`内部仍然使用`Context`存储父`Context`的指针，并用`interface{}`存储键值；

如果当前`valueCtx`找不到需要的`key`，会沿着树向上一直查找直到根节点，类似链表的搜索；

使用`WithValue`创建时，会判断`key`是否实现`Comparable`接口。如果没有实现，会触发`panic`；

key的类类型不应该是内置类型，以避免冲突。使用的时候应该自定义类型；

# Channel

Channel的设计基于CSP模型。

CSP模型（Communicating Sequential Process，通信顺序进程），允许使用进程组来描述系统，独立运行，并且只通过消息传递的方式通信。

本质上就是，在使用协程执行函数时，不通过内存共享(会用到锁)的方式通信，而是通过Channel通信传递数据。

动画参考：https://go.xargin.com/docs/data_structure/channel/

## 基本

* chan是引用类型，使用make关键字创建，未初始化时的零值是nil，如

  `ch := make(chan string, 10)`，创建一个能处理string的缓冲区大小为10的channel，效果相当于异步队列，除非缓冲区用完，否则不会阻塞；

  `ch := make(chan string)`，则创建了一个不存在缓冲区的channel，效果相当于同步阻塞队列，len永远返回0。

  即 假如没有接收者，同一个方法内，连续发送两次数据，第一次如果没有被接收的话，此时就阻塞了，轮不到第二次发送，但如果size = 1，第一次发送的数据就会进入buf数组，不阻塞，到了第二次发送才阻塞；

  `var ch chan int`表示创建了一个nil channel；

* channel作为通道，负责在多个goroutine间传递数据，解决多线程下共享数据竞争问题。

* `len()`方法获取buff中未被读取的数量，即qcount的值；

  `cap()`方法获取buff数组的长度

* 带有 <- 的chan是有方向的，不带 <- 的chan是双向的，比如

```go
  chan string        // 双向chan，可以发送和接收string  chan<- struct{}    // 只能发送struct到chan中  <-chan int         // 只能从chan中接收int
```

* chan可以是任何类型的，比如可以是 chan<- 类型，<-总是尽量和左边的chan结合，比如

```go
chan<- chan int    // 等价于 chan<- (chan int)chan<- <-chan int  // 等价于 chan<- (<-chan int)<-chan <-chan int  // 等价于 <-chan (<-chan int)chan (<-chan int)  // 等价于 chan (<-chan int)
```

* 接收数据时可以有两个返回值，第一个是返回的元素，第二个是bool类型，表示是否成功地从chan中读取到一个值。如果是false，说明chan已经被close并且chan中没有缓存的数据，此时第一个元素是零值。所以，如果接收时第一个元素是零值，可能是sender真的发送了零值，也可能是closed并且没有元素导致的，所以最好通过第二个返回值来确定。
* 双向chan可以赋值给单向chan，但反过来不可以；
* 给一个nil channel发送数据，会造成永久阻塞，从一个nil channel接收数据，会造成永久阻塞；
* 给一个已经关闭的channel发送数据，会引起panic；

```go
// 因为是无缓冲区的，只有当存在receiver的时候才能send成功，否则就一直阻塞，所以当close的时候，就会panic
ch := make(chan int)
go func() { ch <- 1 }() // panic: send on closed channel
time.Sleep(time.Second)
go func() { close(ch) }()
time.Sleep(time.Second)
x, ok := <-ch
fmt.Println(x, ok)
```

* 从一个已经关闭的channel接收数据，如果缓冲区为空，则返回一个零值；

* 已关闭的channel再次关闭，会panic；

* 对于一个不关闭的channel，在方法结束后，只要channel没有被引用，会被GC自动回收；

* 关闭channel的原则：不要向已关闭的channel发送数据或者再次关闭，关闭的动作尽量在sender做，主要还是分场景：

  * 一个sender一个receiver的场景：在sender处关闭。

  * 一个sender多个recevier的场景：在sender处关闭。

  * 多个sender一个receiver的场景：增加一个传递关闭信号的 channel，receiver 通过信号 channel 下达关闭数据 channel 指令。senders 监听到关闭信号后，停止发送数据。

    ```go
    	dataCh := make(chan int, 100)
        stopCh := make(chan struct{})
    	// senders
        for i := 0; i < NumSenders; i++ {
            go func() {
                for {
                    select {
                    case <- stopCh:
                        // 接收关闭信号退出
                        return
                    case dataCh <- rand.Intn(Max):
                    }
                }
            }()
        }
        // the receiver
        go func() {
            for value := range dataCh {
                if value == Max-1 {
                    fmt.Println("send stop signal to senders.")
                    // 直接关闭
                    close(stopCh)
                    return
                }
    
                fmt.Println(value)
            }
        }()
    ```

  * 多个sender多个receiver的场景：再增加一个中间的channel，用来接收标识关闭的数据，收到后直接close传递关闭的信号channel即可。

    ```go
    	dataCh := make(chan int, 100)
        stopCh := make(chan struct{})
    
        // 当使用select发送数据到toStop时，一定要有buffer，防止中间channel没准备好而错失关闭时机的问题
        toStop := make(chan string, 1)
        var stoppedBy string
    
        // 中间channel，用于接收标识关闭的数据
        go func() {
            stoppedBy = <-toStop
            close(stopCh)
        }()
    
        // senders
        for i := 0; i < NumSenders; i++ {
            go func(id string) {
                for {
                    value := rand.Intn(Max)
                    // 发送者也可以关闭
                    if value == 0 {
                        toStop <- "sender#" + id
                        return
                    }
    
                    select {
                    case <- stopCh: // 真正的停止
                        return
                    case dataCh <- value:
                    }
                }
            }(strconv.Itoa(i))
        }
    
        // receivers
        for i := 0; i < NumReceivers; i++ {
            go func(id string) {
                for {
                    select {
                    case <- stopCh:  // 真正的停止
                        return
                    case value := <-dataCh:
                        // 接收者也能进行关闭
                        if value == Max-1 {
                            case toStop <- "receiver#" + id:
                            return
                        }
    
                        fmt.Println(value)
                    }
                }
            }(strconv.Itoa(i))
        }
    ```

* channel在关闭时会自动退出循环；

```go
ch := make(chan int, 100)
for elem := range ch {
	fmt.println(elem)
}
```

* 注意channel不提供跨goroutine的数据保护，如果多个channel传递一份数据的指针，使得每个goroutine可以操作同一份数据，也会出现并发安全问题；

|         | nil   | empty                | full                 | not full & empty     | closed                         |
| ------- | ----- | -------------------- | -------------------- | -------------------- | ------------------------------ |
| receive | block | block                | read value           | read value           | 返回未读的元素，读完后返回零值 |
| send    | block | write value          | block                | writed value         | panic                          |
| close   | panic | closed，没有未读元素 | closed，保留未读元素 | closed，保留未读元素 | panic                          |

## 数据结构

```go
type hchan struct {
	qcount   uint   // 已经接收但还没被取走的元素个数，即channel中的循环数组的元素个数
    dataqsiz uint   // channel中的循环数组的长度, ch:=make(chan int, 10), 就是这个10
	buf      unsafe.Pointer // channel中缓冲区数据指针，buf是一个循环数组，buf的总大小是elemsize的整数倍
	elemsize uint16 // 当前channel能够收发的元素大小
	closed   uint32
	elemtype *_type // 当前channel能够收发的元素类型
	sendx    uint   // 指向底层循环数组buf，表示当前可发送的元素位置的索引值，当sendx=dataqsiz时，会回到buf数组的起点，一旦接收新数据，指针就会加上elemsize，移向下个位置
	recvx    uint   // 指向底层循环数组buf，表示当前可接收的元素位置的索引值
	recvq    waitq  // 等待接收队列，存储当前channel因缓冲区空间不足而阻塞的goroutine列表，双向链表
	sendq    waitq  // 等待发送队列，存储当前channel因缓冲区空间不足而阻塞的goroutine列表，双向链表

	lock mutex  // 互斥锁，保证每个读channel或写channel的操作都是原子的，保护hchan和sudog上的字段。
    // 持有lock时，禁止更改另一个G的状态(比如不要把状态改成ready)，防止死锁
}

// 双向链表
// sudog表示goroutine，是对goroutine的一层封装，代表一个在等待队列中的G
// 一个G可以出现在多个等待队列上，因此一个G可以有多个sudog
type waitq struct {
	first *sudog
	last  *sudog
}

type sudog struct {
    g    *g
    next *sudog
    prev *sudog
    elem unsafe.Pointer
}
```

## 初始化

```go
func makechan(t *chantype, size int) *hchan {
    ...
    elem := t.elem
    // 略去检查代码，检查数据项大小是否超过64KB，是否有错误的内存对齐，缓冲区大小是否溢出
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

使用`ch <- "test"`发送数据，最终会调用`chansend()`函数发送数据，该函数设置了阻塞参数为true；

1. 如果chan是nil，则把发送者的goroutine park（阻塞休眠），此时发送者将被永久阻塞；

2. 如果chan没有被close，但是chan满了，则直接返回false，但是由于阻塞参数为true，这部分不会被执行；

3. **上锁**，保证线程安全，再次检查chan是否被close，**如果被close，再往里发数据会触发 解锁，panic**；

4. 同步发送 - **优先发送给等待接收的G**

   如果**没被close，当recvq存在等待的接收者时**，通过`send()`函数，取出第一个等待的goroutine，直接发送数据，不需要先放到buf中；

   `send()`函数将因为等待数据的接收而阻塞的goroutine的状态从Gwaiting或者Gscanwaiting改为Grunnable，把goroutine绑定到P的LRQ中，**等待下一轮调度**时会立即执行这个等待发送数据的goroutine；

5. 异步发送 - **其次是发送到buf区**

   **当recvq中没有等待的接收者，且buf区存在空余空间时**，会使用`chanbuf()`函数获取**sendx索引值**，计算出下一个可以存储数据的位置，然后调用`typedmemmove()`函数将要发送的数据拷贝到buff区，增加sendx索引和qcount计数器，完成之后解锁，返回成功；

6. 阻塞发送 - **最后才保存在待发送队列，阻塞（阻塞只发生在这里，此时G和M分离）**

   **当recvq中没有等待的接收者，且buf区已满或不存在buf区时**，会先调用`getg()`函数获取正在发送者的goroutine，执行`acquireSudog()`函数获取sudoG对象，设置此次阻塞发送的相关信息（如发送的channel、是否在select控制结构中和待发送数据的内存地址、发送数据的goroutine）

   然后将该sudoG对象加入sendq队列，调用`goparkunlock()`函数让当前发送者的goroutine进入等待状态，表示当前goroutine正在等待其他goroutine从channel中接收数据，等待调度器唤醒；

   此时len()返回值为0，数据的发送是阻塞在方法中的。
   
   调度器唤醒后，将一些属性值设置为零，并释放sudog对象，表示向channel发送数据结束；

**channel发送数据时涉及两次goroutine的调度**：

1. 当接收队列里存在sudoG可以直接发送数据时，执行`goready()`函数，将G从Gwaiting或GScanwaiting转为Grunnable，等待下次调度触发，交由M执行；
2. 当没有等待接收数据的G，并且没有缓冲区，或者缓冲区已满时，执行`gopark()`函数挂起当前G，将G阻塞，此时状态为Gwaiting，让出CPU等待调度器调度；

```go
// ep指的是用来发送数据的内存指针，数据类型与hchan中的类型一致
// 返回值表示带发送的数据是否 send 成功，即是否被接受，比如进buff或者被接收者接收；
// ch <- [val] 时，block=true; select时，block=false
func chansend(c *hchan, ep unsafe.Pointer, block bool, callerpc uintptr) bool {
	// 如果chan是nil，则把发送者的goroutine park（阻塞休眠），此时发送者将被永久阻塞
    if c == nil {
		if !block {
			return false
		}
		gopark(nil, nil, waitReasonChanSendNilChan, traceEvGoStop, 2)
		throw("unreachable")
	}

	if raceenabled {
		racereadpc(c.raceaddr(), callerpc, funcPC(chansend))
	}

    // 下面代码进针对select场景
    // 当chan不为null，且没被close，full方法判断chan发送是否阻塞，是则直接返回true
    // full方法有两种情况判断是否可发送
    // 1. 如果hchan.dataqsiz=0，说明是阻塞队列，如果此时hchan.recvq.first==nil，说明没有接收者，发送阻塞
    // 2. 比较hchan.qcount是否等于hchan.dataqsiz，如果是说明chan已满，发送阻塞
	if !block && c.closed == 0 && full(c) {
		return false
	}
    
    // 上锁
	lock(&c.lock)
	// 如果chan被关闭，再往里发送数据就会解锁，然后panic
	if c.closed != 0 {
		unlock(&c.lock)
		panic(plainError("send on closed channel"))
	}
	// 如果chan没关闭，获取接收者等待队列中的第一个G开始发送数据
	if sg := c.recvq.dequeue(); sg != nil {
		// G存在，调用send函数，send函数主要完成两件事
        // 1. 调用sendDirect()函数将数据拷贝到接收变量的内存地址上
        // 2. 调用goready()函数将等待接收的阻塞G的状态从Gwaiting或者Gscanwaiting改为Grunnable，把G绑定到P的LRQ中，下一轮调度时会唤醒这个等待接收数据的G立即执行。
		send(c, sg, ep, func() { unlock(&c.lock) }, 3)
		return true
	}
	// 当recvq中没有等待接收数据的G，且chan的缓冲区还有空间时
	if c.qcount < c.dataqsiz {
        // 调用chanbuf获取sendx索引的元素的指针，；
		qp := chanbuf(c, c.sendx)
		if raceenabled {
			raceacquire(qp)
			racerelease(qp)
		}
        // 调用typedmemmove()将要发送的数据拷贝到缓冲区buf
		typedmemmove(c.elemtype, qp, ep)
        // 然后增加sendx索引和qcount计数器的值
		c.sendx++
		if c.sendx == c.dataqsiz {
            // 因为buf缓冲区是环形，如果索引到了队尾，则置0重新回到队头
			c.sendx = 0
		}
		c.qcount++
        // 完成后就解锁，返回成功
		unlock(&c.lock)
		return true
	}

    // 能过到这边，说明没有等待接收数据的G，并且没有缓冲区，或者缓冲区已满，此时进入阻塞发送
	if !block {
		unlock(&c.lock)
		return false
	}

	// 获取当前goroutine
	gp := getg()
    // 获取一个sudo G；acquireSudog()方法主要是获取可复用的sudoG对象，会优先从本地缓存获取，获取不到就会从全局缓存中获取，追加到本地缓存，如果全局缓存也没有，则新创建一个sudoG
	mysg := acquireSudog()
	mysg.releasetime = 0
	if t0 != 0 {
		mysg.releasetime = -1
	}
	// 为sudo G设置好要发送的数据和状态，比如发送的Channel、是否在select中和待发送的数据的内存地址等
	mysg.elem = ep
	mysg.waitlink = nil
	mysg.g = gp
	mysg.isSelect = false
	mysg.c = c
	gp.waiting = mysg
	gp.param = nil
    // 将sudo G加入待发送队列
	c.sendq.enqueue(mysg)
    // 调用gopark方法挂起当前goroutine，状态为waitReasonChanSend，阻塞等待channel
	gopark(chanparkcommit, unsafe.Pointer(&c.lock), waitReasonChanSend, traceEvGoBlockSend, 2)
	// 确保发送的值保存活动状态，直到接收者将其复制出来。因为sudoG具有指向堆栈对象的指针，但其不能作为GC时的root对象。发送的数据是分配在堆上的，避免被GC。
	KeepAlive(ep)

	// 当goroutine被唤醒后，解除阻塞状态，完成channel阻塞数据的发送
	if mysg != gp.waiting {
		throw("G waiting list is corrupted")
	}
	gp.waiting = nil
	gp.activeStackChans = false
	if gp.param == nil {
		if c.closed == 0 {
			throw("chansend: spurious wakeup")
		}
		panic(plainError("send on closed channel"))
	}
	gp.param = nil
	if mysg.releasetime > 0 {
		blockevent(mysg.releasetime-t0, 2)
	}
	mysg.c = nil
    // 发送完成之后，解除channel的绑定，重置sudoG状态，释放sudoG，释放时，如果本地缓存已满，会转移一部分到全局缓存，否则放到本地缓存等待被复用
	releaseSudog(mysg)
	return true
}
```

## 接收数据

使用` str <- ch 或 str, ok <- ch (ok用于判断ch是否关闭，如果没有ok，可能会无法分辨str接收到的零值是发送者发的还是ch关闭)`接收数据，会转化为调用`chanrecv1和chanrecv2函数`，但最终会调用`chanrecv函数`接收数据。chanrecv1和chanrecv2函数都是设置阻塞参数为true。

1. 如果chan是nil，则把接收者的goroutine park（阻塞休眠），接收者被永久阻塞；

2. 不上锁检查 buf 区大小：如果chan的buf区大小为0 或者 没有数据可接收，检查是否被关闭，被关闭则返回；如果没被关闭，则再次检查buf区大小是否为0 或者 没有数据可接收，如果是，则清除ep指针中的数据并返回selected为true，received为false；

   这里两次empty检查，因为第一次检查，chan可能还没关闭，但是第二次检查时关闭了，由于可能在两次检查之间有待接收的数据达到了，所以需要两次empty检查；

3. **上锁**检查buf区大小：上锁，如果chan已经被close，且buf区没有数据，清除ep指针中的数据，解锁，返回selected为true，received为false；

4. 同步接收 - **如果无buf，消费发送等待队列中G的数据，如果buf满，先拿buf区的，发送的再加入**

   **当chan的sendq队列存在等待状态的goroutine时**（能拿到就说明要不就是buf区为0，要不就是buf区已满）

   **如果是无buf区的chan**，直接使用`recv()`函数从阻塞的发送者中获取数据；

   **如果是有buf区的chan**，说明此时buf区已满，则先从buf区中获取可接收的数据（从buf区中copy到接收者的内存），然后从sendq队列的队首中读取待发送的数据，加入到buf区中（将发送者的数据copy到buf区，替换刚刚buf区copy出去的位置），更新可接收和可发送的下标chan.recvx和sendx的值；

   最后调用`goready()`函数将等待发送数据而阻塞gorouotine的状态从Gwaiting 或者 Gscanwaiting 改变成 Grunnable，把goroutine绑定到P的LRQ中，**等待下一轮调度时**立即释放这个等待发送数据的goroutine；

5. 异步接收 - **其次是消费buf区中的数据**

   **当channel的sendq队列没有等待状态的goroutine，且buf区存在数据时**，从channel的buf区中的recvx的索引位置接收数据，如果接收数据的内存地址不为空，会直接将缓冲区里的数据拷贝到内存中，清除buf区中的数据，递增recvx，递减qcount，完成数据接收；

   这个和chansend共用一把锁，所以不会有并发问题；

6. 阻塞接收 - **最后才是保存在接收等待队列，阻塞（阻塞只发生在这里，此时G和M分离）**

   **当channel的sendq队列没有等待状态的goroutine，且buf区不存在数据时**，执行`acquireSudog()`函数获取sudoG对象，设置此次阻塞发送的相关信息（如发送的channel、是否在select控制结构中和待发送数据的内存地址、发送数据的goroutine）

   然后将该sudoG对象加入待发送recvq队列，调用`goparkunlock()`函数让当前接收者的goroutine进入等待状态，表示当前goroutine正在等待其他goroutine从channel中发送数据，等待调度器唤醒；

   此时方法会阻塞在ch的接收中，len()返回值为0；
   
   goroutine被唤醒后，chan完成阻塞数据的接收，接收完成后进行基本的参数检查，解除chan的绑定，释放sudoG，表示接收数据完成；

**channel 接收过程中包含 2 次有关 goroutine 调度过程**：

1. 当发送队列中存在 sudoG 时，调用`goready()`，G 的状态从 Gwaiting 或者 Gscanwaiting 改变成 Grunnable，等待下次调度便立即运行；
2. 当 buf 区为空，且没有发送者时，调用 `gopark()`挂起当前G，此时状态为Gwaiting，让出 cpu 的使用权并等待调度器的调度；

```go
// ep指的是用来接收数据的内存指针，数据类型与hchan中的类型一致
// [val] <- ch 时，block=true; select时，block=false
func chanrecv(c *hchan, ep unsafe.Pointer, block bool) (selected, received bool) {
	if debugChan {
		print("chanrecv: chan=", c, "\n")
	}
	// 如果chan是nil，接收者会被阻塞，gopark会引起waitReasonChanReceiveNilChan原因的休眠，并抛出unreachable的错误
	if c == nil {
		if !block {
			return
		}
		gopark(nil, nil, waitReasonChanReceiveNilChan, traceEvGoStop, 2)
		throw("unreachable")
	}
    // 这段代码仅针对select的场景
	// 当chan不为nil，在没有获取锁的情况下，检查chan的buf区大小和是否存在可接收数据
    // empty方法是原子检查，检查chan.dataqsiz、chan.qcount是否为0，发送队列是否为空
	if !block && empty(c) {
		if atomic.Load(&c.closed) == 0 {
			return
		}
        // 这里两次empty检查，因为第一次检查，chan可能还没关闭，但是第二次检查时关闭了，由于可能在两次检查时有待接收的数据达到了，所以需要两次empty检查
		if empty(c) {
			if raceenabled {
				raceacquire(c.raceaddr())
			}
            // 如果chan的buf区大小和是否存在可接收数据，此时会清除ep指针中的数据
			if ep != nil {
				typedmemclr(c.elemtype, ep)
			}
			return true, false
		}
	}
	
	var t0 int64
	if blockprofilerate > 0 {
		t0 = cputicks()
	}
    // 获取锁后，再检查一遍
	lock(&c.lock)
    // 如果chan已经关闭且buf区不存在数据了，则清理ep指针中的数据并返回
    // 这里也是从已经关闭的chan中读数据，读出来的是该类型零值的原因
	if c.closed != 0 && c.qcount == 0 {
		if raceenabled {
			raceacquire(c.raceaddr())
		}
		unlock(&c.lock)
		if ep != nil {
			typedmemclr(c.elemtype, ep)
		}
		return true, false
	}

    // 从发送队列队首中找到等待发送的goroutine（能拿到就说明要不就是buf区为0，要不就是buf区已满）
    // 如果buf区大小为0，则直接接收数据；
    // 否则，说明buf区已满，先从buf区中获取要发送的数据，再将sender的数据加入到buf区中，更新可接收和可发送的下标chan.recvx和sendx的值
	if sg := c.sendq.dequeue(); sg != nil {
		recv(c, sg, ep, func() { unlock(&c.lock) }, 3)
		return true, true
	}

    // 当chan的buf区存在数据时，直接从buf区中获取数据，进行接收，更新接收数据的下标值，解锁
	if c.qcount > 0 {
		qp := chanbuf(c, c.recvx)
		if raceenabled {
			raceacquire(qp)
			racerelease(qp)
		}
		if ep != nil {
			typedmemmove(c.elemtype, ep, qp)
		}
		typedmemclr(c.elemtype, qp)
		c.recvx++
		if c.recvx == c.dataqsiz {
			c.recvx = 0
		}
		c.qcount--
		unlock(&c.lock)
		return true, true
	}

	if !block {
		unlock(&c.lock)
		return false, false
	}

	// 到了这里，说明sendq里没有待发送的goroutine，且buf区也没有数据
	gp := getg()
	mysg := acquireSudog()
	mysg.releasetime = 0
	if t0 != 0 {
		mysg.releasetime = -1
	}
	mysg.elem = ep
	mysg.waitlink = nil
	gp.waiting = mysg
	mysg.g = gp
	mysg.isSelect = false
	mysg.c = c
	gp.param = nil
    // 设置好待接收的sudoG后，加入待发送的等待队列
	c.recvq.enqueue(mysg)
    // 挂起当前goroutine，状态设置为waitReasonChanReceive，阻塞等待chan
	gopark(chanparkcommit, unsafe.Pointer(&c.lock), waitReasonChanReceive, traceEvGoBlockRecv, 2)
	// goroutine被唤醒后，完成chan阻塞数据的接收，解除chan的绑定释放sudoG
	if mysg != gp.waiting {
		throw("G waiting list is corrupted")
	}
	gp.waiting = nil
	gp.activeStackChans = false
	if mysg.releasetime > 0 {
		blockevent(mysg.releasetime-t0, 2)
	}
	closed := gp.param == nil
	gp.param = nil
	mysg.c = nil
	releaseSudog(mysg)
	return true, !closed
}


// 这里sg指的是等待发送队列中的G，ep指其携带的数据
func recv(c *hchan, sg *sudog, ep unsafe.Pointer, unlockf func(), skip int) {
	if c.dataqsiz == 0 {
		if ep != nil {
			// 从 sender 里面拷贝数据
			recvDirect(c.elemtype, sg, ep)
		}
	} else {
	    // 这里对应 buf 满的情况
		qp := chanbuf(c, c.recvx)
		// 将数据从 buf 中拷贝到接收者内存地址中
		if ep != nil {
			typedmemmove(c.elemtype, ep, qp)
		}
		// 将数据从 sender 中拷贝到 buf 中
		typedmemmove(c.elemtype, qp, sg.elem)
		c.recvx++
		if c.recvx == c.dataqsiz {
			c.recvx = 0
		}
		c.sendx = c.recvx // c.sendx = (c.sendx+1) % c.dataqsiz
	}
	sg.elem = nil
	gp := sg.g
	unlockf()
	gp.param = unsafe.Pointer(sg)
	sg.success = true
	if sg.releasetime != 0 {
		sg.releasetime = cputicks()
	}
	goready(gp, skip+1)
}
```

## 关闭

1. 如果 chan 为 nil，close 会 panic；

2. 上锁：

   如果 chan 已经 closed，再次 close 也会 panic；

   否则的话，如果 chan 不为 nil，chan 也没有 closed，设置chan的标记为closed；

3. **优先释放所有的接收者**：

   将接收者等待队列中的sudoG对象加入到待清除队列glist中，这里会优先回收接收者，这样即使从close中的chan读取数据，也不会panic，最多读到默认值；

   这样第6步执行的时候，才会先执行接收者，接收后面发送者的数据（接收buff数组里的数据，因为sender里的会被panic掉），否则发送者发送的数据无法被先接收。

4. **其次是释放所有发送者**：
   将发送者等待队列中的sudoG对象加入到待清除队列glist中，这里可能会发生panic，因为往一个close的chan中发送数据会panic；

5. 解锁

6. 进行最后的调度，遍历glist中的sudoG，调用`goready()`触发调度，将每个goroutine状态从 Gwaiting 转为 Grunnable状态，等待调度器调度；

```go
func closechan(c *hchan) {
	if c == nil {
		panic(plainError("close of nil channel"))
	}

	lock(&c.lock)
	if c.closed != 0 {
		unlock(&c.lock)
		panic(plainError("close of closed channel"))
	}
	c.closed = 1
	var glist gList
	// 释放所有接收者：将所有接收者的sudoG等待队列加入到待清除的队列glist中
	for {
		sg := c.recvq.dequeue()
		if sg == nil {
			break
		}
        // 销毁资源
		if sg.elem != nil {
			typedmemclr(c.elemtype, sg.elem)
			sg.elem = nil
		}
		if sg.releasetime != 0 {
			sg.releasetime = cputicks()
		}
		gp := sg.g
		gp.param = nil
		if raceenabled {
			raceacquireg(gp, c.raceaddr())
		}
		glist.push(gp)
	}
	// 释放所有发送者：将所有发送者的sudoG等待队列加入到待清除的队列glist中
    // 如果发送者队列存在发送者，那这些发送者所在的goroutine会产生panic
	for {
		sg := c.sendq.dequeue()
		if sg == nil {
			break
		}
        // 销毁资源
		sg.elem = nil
		if sg.releasetime != 0 {
			sg.releasetime = cputicks()
		}
		gp := sg.g
		gp.param = nil
		if raceenabled {
			raceacquireg(gp, c.raceaddr())
		}
		glist.push(gp)
	}
	unlock(&c.lock)

	// 为所有被阻塞的 goroutine 调用 goready 触发调度。将所有 glist 中的 goroutine 状态从 _Gwaiting 设置为 _Grunnable 状态，等待调度器的调度
	for !glist.empty() {
		gp := glist.pop()
		gp.schedlink = 0
		goready(gp, 3)
	}
}
```

## 应用场景

* 实现生产者 - 消费组模型，数据传递，比如[worker池的实现](http://marcio.io/2015/07/handling-1-million-requests-per-minute-with-golang/)

  ```go
  func consumer(taskChan <-chan int) {
      for i := 0; i < 5; i++ {
          go func(id int) {
              for {
                  task := <- taskChan
                  time.Sleep(time.Second) // 模拟耗时
              }
          }(i)
      }
  }
  
  func main() {
      taskCh := make(chan int, 100)
      go consumer(taskCh)
      // 生产者
      for i := 0; i < 10; i++ {
          taskCh <- i
      }
      // wait...
  }
  ```

* 信号通知：利用 如果chan为空，那receiver接收数据的时候就会阻塞等待，直到chan被关闭或有新数据进来 的特点，将一个协程将信号(closing、closed、data ready等)传递给另一个或者另一组协程，比如 wait/notify的模式。

* 协程池，把要操作的逻辑封装成task，通过chan传输实现协程复用

* 任务编排：让一组协程按照一定的顺序并发或串行执行，比如实现waitGroup的功能

* 控制并发量，可以配合WaitGroup进行控制goroutine的数量

  ```go
  func main() {
      ch := make(chan int, 3)
      for i := 0; i < 10; i++ {
  // 这个放在外层和放在里层的效果不同，都可以控制并发量，但是前者会阻塞for循环，后者不会
          ch <- 1   // 放满三个后就会阻塞循环，此时最多存在3个goroutine
  		go func(k int) {
  			fmt.Println(k) // do something...
  			time.Sleep(time.Second)
  			// 防止泄露
  			defer func() {
  				<- ch
  			}()
  		}(i)
      }
  }
  
  // 配合WaitGroup控制goroutine的数量
  func main() {
      ch := make(chan int, 3)
      wg := sync.WaitGroup{}
      for i := 0; i < 10; i++ {
          ch <- 1 // ch如果放在里层就达不到控制goroutine的效果了
          wg.Add(1)
  		go func(k int) {
              defer wg.Done()
              // ch<- 1, ch如果放在这，那就只阻塞goroutine里的逻辑，goroutine还是会创建多个
  			fmt.Println(k) // do something...
  			time.Sleep(time.Second)
              // 防止泄露
  			defer func() {
  				<- ch
  			}()
  		}(i)
      }
      wg.Wait()
  }
  ```

* 任务定时

  ```go
  func worker() {
      ticker := time.Tick(1 * time.Second)
      for {
          select {
          case <- ticker:
              // 执行定时任务
              fmt.Println("执行 1s 定时任务")
          }
      }
  }
  // 或者
  func worker() {
      for {
          select {
      		case <-time.After(100 * time.Millisecond):
      		case <-s.stopc:
          		return false
  		}
      }
  }
  ```

* 实现互斥锁的机制，比如，容量为 1 的chan，放入chan的元素代表锁，谁先取得这个元素，就代表谁先获取了锁

  ```go
  type Locker struct {
      ch chan int
  }
  
  func NewLocker() *Locker {
      locker := &Locker{ch: make(chan int, 1)}
      locker.ch <- 1
      return locker
  }
  
  func (locker *Locker) Lock() {
      <- locker.ch
  }
  
  func (locker *Locker) UnLock() {
      select {
          case locker.ch <- struct{}{}: 
      	default: 
          	panic(" unlock of unlocked mutex") 
      }
  }
  ```

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
    // 如果上面的协程任务处理的时间过长，触发下面select的超时机制，此时process函数返回，之后当上面的协程任务执行完之后，由于process已经执行完，下面result接收chan的值被回收，所以没有接收者，导致上面的协程任务一直卡在 ch <- true，进而导致goroutine泄漏。解决方案就是使用容量为1的ch即可。
    select {
    case result := <-ch:
        return result
    case <-time.After(timeout):
        return false
    }
}
```

# select

## 结构

select在runtime中不存在结构体表示，但是case倒是有

```go
const (
    // scase.kind，到时select就是轮询判断这些类型的
    // send 或者 recv 发生在一个 nil channel 上，就有可能出现这种情况
    caseNil = iota
    caseRecv
    caseSend
    caseDefault
)

// select 中每一个 case 的数据结构定义
type scase struct {
	elem unsafe.Pointer // 接收 或 发送数据的变量地址
	c    *hchan         // 存储正在使用的chan
	kind uint16         // case的种类

	releasetime int64
	pc          uintptr // return pc (for race detector / msan)
}
```

## 基本

* 非阻塞收发：当chan中存在可接收数据，直接处理那个chan，否则执行default语句

```go
ch := make(chan int)
    select {
    case i := <-ch:
        println(i)
    default:
        println("default")
    }
```

* 随机执行：select遇到多个case就绪，会进行随机选择

```go
ch1 := make(chan int)
ch2 := make(chan int)
    select {
    case j := <-ch1:
        println(j)
    case i := <-ch2:
        println(i)
    }
```

编译器会对select中的case进行优化，总共有四种情况：

1. 不包含任何case，即空select，此时会阻塞当前的goroutine

2. 只包含一个case，此时select会被优化成 if ，当chan没有数据可接收时，就会把阻塞当前goroutine，直到有数据到来；如果chan是nil，就会永远阻塞当前goroutine

```go
select {
case v, ok <-ch:
    // ...    
}
// 会被优化成
if ch == nil {
    block()
}
v, ok := <-ch
// ...
```

3. 存在两个case，其中一个是default：

   * 发送，这种情况下，发送是不阻塞的：

   ```go
   ch := make(chan int, 1) // 一定要1及以上，才先会走case，如果是0会死锁，直接走default，
   select {
   case ch <- i:
       // ...
   default:
       // ...
   }
   // 底层调用的是chansend(c, elem, false, getcallerpc())，这里的阻塞参数是false， 表示这次发送不会阻塞
   if selectnbsend(ch, i) {
       // ...
   } else {
       // ...
   }
   ```

   * 接收，这种情况下，chan有值就走case，否则走default

   ```go
     select {
     case v <- ch: // case v, received <- ch:
         // ...
     default:
         // ...
     }
     
     if selectnbrecv(&v, ch) { // if selectnbrecv2(&v, &received, ch) {
         // ...
     } else {
         // ...
     }
   ```

   * 通用的select条件：比如select里包含多个case，会编译成通过`runtime的selectgo方法`处理case，`selectgo`会返回 case的序号 还有 是否被接收的标识，然后被编译成多个if，用于判断选中哪个case。

   ```go
     selv := [3]scase{}
     order := [6]uint16
     for i, cas := range cases {
         c := scase{}
         c.kind = ...
         c.elem = ...
         c.c = ...
     }
     chosen, revcOK := selectgo(selv, order, 3)
     if chosen == 0 {
         // ...
         break
     }
     if chosen == 1 {
         // ...
         break
     }
     if chosen == 2 {
         // ...
         break
     }
   ```

## selectgo的流程

1. 获取case数组，随机打乱，确定打乱后的轮询顺序数组pollorder和加锁顺序数组lockorder，数组里存放的元素是chan

2. 按加锁顺序数组，调用chan的锁，依次进行锁定

3. 进入主循环，遍历 轮询顺序数组pollorder
   

**第一阶段**，查找是否已经存在准备就绪的chan（此时的chan可以执行收发操作）此时需要处理四种类型的case：

1. 当case不包含chan时，直接跳过；

2. 当case会从chan中接收数据时：

* 如果当前chan的`sendq队列`上有等待的goroutine，就会跳到 `recv标签`，如果没有buf区，则从`sendq队列`上获取数据，否则，从chan的buf区读取数据后，将`sendq队列`中等待的goroutine中的数据放入到buf区中相同的位置；
* 如果当前chan的buf区不为空，就跳到`bufrecv标签`，从chan的buf区中获取数据
* 如果当前chan已经被关闭，就会跳到 `rclose标签` 做一些清除的收尾工作；

3. 当case会从chan中发送数据时：

* 如果当前chan已经被关闭，会直接跳到 `sclose标签`，触发panic；
* 如果当前chan的`recvq队列`上有等待的goroutine，就跳到 `send标签` 向chan发送数据；
* 如果当前chan的缓冲区存在空闲位置，就会将等待发送的数据存入缓冲区中，因为select相当于有接收者了，不会出现发送阻塞的情况；

4. 当case是default时，表示前面的所有case都没有被执行，此时会解锁所有的chan并返回（意味着当前select结构的收发都是非阻塞的），直接执行default内容；

第一阶段只是查找所有case中是否有可以立即被处理的chan，无论是数据是在等待的goroutine上，还是buf区中，只要存在数据满足条件就会立即处理，然后返回；如果不能立刻找到活跃的chan，就会进入下一循环；

**第二阶段**，将当前goroutine加入到chan对应的收发队列上并等待其他goroutine的唤醒:

* 将当前goroutine，包装成sudogo，遍历case，加入到case的chan的`sendq队列`或者`recvq队列`中（同时，这个sudog会关联当前case的chan，然后将这些sudog组成链表，挂在当前goroutine下，用于唤醒之后的查找）

  ![](https://github.com/Nixum/Java-Note/raw/master/picture/go_selectgo.png)

* 调用`gopark函数`挂起当前goroutine，等待被调度器唤醒；

**第三阶段**，当前goroutine被唤醒后，找到满足条件的chan并进行处理：

* 等到select对应的chan准备好后，当前goroutine会被调度器唤醒，被唤醒后，获取当前goroutine的sudog，依次对比所有case里chan对应的sudog结构，找到被唤醒的case，并释放其他未被使用的sudog结构；
*  由于当前的select结构已经被挑选了其中一个case执行，剩下的case中没有被用到的sudog会被直接忽略并释放掉，为了不影响chan的正常使用，还需要将这些废弃的sudog从chan中出队；

```go
func selectgo(cas0 *scase, order0 *uint16, ncases int) (int, bool) {
    cas1 := (*[1 << 16]scase)(unsafe.Pointer(cas0))
    order1 := (*[1 << 17]uint16)(unsafe.Pointer(order0))

    scases := cas1[:ncases:ncases]
    // 轮询顺序数组
    pollorder := order1[:ncases:ncases]
    // 加锁顺序数组
    lockorder := order1[ncases:][:ncases:ncases]

    for i := range scases {
        cas := &scases[i]
        if cas.c == nil && cas.kind != caseDefault {
            *cas = scase{}
        }
    }

    // 根据chan的地址排序
    for i := 1; i < ncases; i++ {
        // 随机轮询，避免chan饿死
        j := fastrandn(uint32(i + 1))
        pollorder[i] = pollorder[j]
        pollorder[j] = uint16(i)
    }
    // 按照之前生成的加锁顺序锁定 select 语句中包含所有的 Channel
    sellock(scases, lockorder)

    // ...后面太长就不贴了
}
```

# Timer

https://www.cyhone.com/articles/analysis-of-golang-timer/

# 时间轮

概念理解：[一张图理解Kafka时间轮](https://zhuanlan.zhihu.com/p/121483218)

[手把手教你如何用golang实现一个timewheel时间轮](https://lk668.github.io/2021/04/05/2021-04-05-%E6%89%8B%E6%8A%8A%E6%89%8B%E6%95%99%E4%BD%A0%E5%A6%82%E4%BD%95%E7%94%A8golang%E5%AE%9E%E7%8E%B0%E4%B8%80%E4%B8%AAtimewheel/)

[Go语言中时间轮的实现](https://www.luozhiyun.com/archives/444)

![](https://github.com/Nixum/Java-Note/raw/master/picture/Kafka时间轮简单实现.png)

总结：

通过DelayQueue（优先级队列实现，队列里的每个元素，都是某一个具体时间的list） + 环形数组（数组的每个元素是个list，索引代表时间格）

DelayQueue会根据环形数组中的每个元素进行排序；

添加任务时，判断任务执行时间，加入环形数组中，对应的环形数组的元素（list），加入DelayQueue中。，
