---
title: Go SE
description: go集合类、协程、runtime、GC原理
date: 2020-11-07
lastmod: 2021-10-24
categories: ["Go"]
tags: ["Go", "Go集合类原理", "Go协程", "Go GC"]
---

[TOC]

**以下基于go.1.14**

# 函数内联优化

函数内联优化：在A函数中调用了B函数，内联后，B函数的代码直接在A函数内原地展开，代替这个函数实现，当有多次调用时，就会多次展开

go在编译时会自动判断函数是否可以内联，当函数内包含以下内容时不会被内联：闭包调用，select，for，defer，go关键字创建的协程等。

内联的好处：因为函数调用被内联了，可以减少栈帧的创建，减少读写寄存器的读取，减少参数和函数的拷贝，提升性能

缺点：堆栈panic显示的行数可能不准确、增加编译出来的包的大小

编译时使用`go build -gcflags="-m -m" main.go`可以知道编译器的内联优化策略，

go编译时默认会使用内联优化，使用`go build --gcflags="-l" main.go`可禁掉全局内联，如果传递两个或以上-l，则会打开内联

# 数组

* 声明时必须指定固定长度，因为编译时需要知道数组长度以便分配内存，如`var arr1 [5]int`，或者`var arr2 = [5]int{1,2,3}, 其余数字为0`
* 数组长度最大是2Gb
* 当数组类型是整形时，所有元素都会被自动初始化为0，即声明完数组，数组会被设置类型的默认值
* 可以使用new()来创建，如`var arr3 = new([3]int)`，arr3的类型是`*[3]int`，arr1、arr2的类型是`[5]int`
* 函数的参数可以是[5]int, 表明入参是数组，如果是[]int，表明入参是slice。类型[3]int和[5]int是两种不同的类型。
* **数组是值类型**，赋值和传参会进行拷贝，函数内部的修改不会影响原始数组。
* 如果数组中的元素个数小于或等于4个，所有变量会直接在栈上初始化；当数组元素大于4个，变量就会在静态存储区初始化然后拷贝到栈上。

# 切片Slice

## 数据结构

slice本质是一个结构体，所以它是值类型是不难理解的，它仅仅只是对数组的一种包装，且该结构体不包含任何函数，任何对slice的处理都是go的内置函数来处理的。

```go
type Slice struct {
	ptr   unsafe.Pointer 	// 指向数组的指针
	len   int               // 切片长度
	cap   int               // 切片容量
}
```

## 基本

* 创建时无需指定长度，如 `slice1 := []int{1,2,3}, 此时长度和容量均为3`
* 从数组上截取`arr1 := [5]int; var slice2 []int = arr1[1:3], 此时长度2，容量5，且对slice2的修改会影响arr1`。
* 可以使用make([]type, len, cap)来创建，len必填，cap非必填，如果cap不填，初始cap=len。如`slice4 := make(int[], 5, 10)，长度5，容量10`。
* 可以使用new来创建，比如 `new([100]int)[0:50]` 效果等同于 `make([]int, 50, 100)`，或者 `slice := *new([]int) 为空切片`
* 空切片：`slice := make([]int, 0) 或 slice := []int{}`，nil切片：`var slice []int 或 slice := *new([]int)`；两者的区别在于，空切片会指向一个内存地址，但它没有分配任何的内存空间；nil切片是直接指向nil。

  打印时，两者的结果均为`[], len=0， cap=0`，但nil切片与nil比较的结果为true，空切片与nil的比较结果为false。
* 切片是对数组的一个连续片段的引用，对于**切片底层数组是引用类型**，作为函数参数时，虽然是传切片的值，但是底层数组传递指针，函数内部的修改会影响原始数组
* 一个数组可以创建多个slice，一个slice也可以创建多个slice，但是新老slice会共用底层数组，新老slice的修改都会互相影响。但是如果新slice经过append，使得slice底层数组扩容了，此时slice引用了新的数组，此时新老slice就不会互相影响了。

使用new()和make()的区别

> 看起来二者没有什么区别，都在堆上分配内存，但是它们的行为不同，适用于不同的类型。
>
> new (T) 为每个新的类型 T 分配一片内存，初始化为 0 并且返回类型为 *T 的内存地址：这种函数 返回一个指向类型为 T，值为 0 的地址的指针，它**适用于值类型，如数组和结构体**；它相当于 &T{}。
> make(T) 返回一个类型为 T 的初始值，它只**适用于 3 种内建的引用类型：切片、map 和 channel**。

* range遍历的注意点：使用range遍历时，底层的slice会发生一次拷贝，即range指向的slice是原始slice的拷贝，长度也是；将slice的每个元素赋值给v时，也发生了一次拷贝，无法通过修改v来修改slice。

demo
```go
	arr := []int{1, 2, 3} // 比如此时arr的地址是0xc00000e380
	for _, v := range arr {
        // 此时arr的地址是0xc00000c3c0，可见发生了拷贝，v的地址就一直不变的
		arr = append(arr, v)
	}
	fmt.Println(arr)  // 打印：1 2 3 1 2 3

	arr2 := []int{1, 2, 3}
	newArr := []*int{}
	for _, v := range arr2 {
		newArr = append(newArr, &v)
	}
	for _, v := range newArr {
		fmt.Printf("%v ", *v) // 打印3 3 3，因为是v指向了同一个指针
	}
```

## 扩容

### 原理

当使用append()函数向slice追加元素，会根据slice的容量判断是否需要扩容。另外，因为slice是值传递，append()函数不会修改传入的slice，返回是重新对底层数组、长度、容量做包装，返回新slice。

1. 如果slice容量够用，则直接把新元素追加进去，长度 + 1，返回原slice
2. 原slice容量不够，将slice扩容，得到新的slice
3. 将新元素追加到新slice，长度 + 1，返回新slice

另外，copy函数拷贝两个slice时，会将源slice拷贝到目标slice，如果目标slice的长度<源slice，不会发生扩容。

Demo：

```go
data := [10]int{}
slice := data[5:8]
slice = append(slice, 9)// slice=? data=?
slice = append(slice, 10, 11, 12)// slice=? data=?
结果：
// 第一次append后结果
slice = [0 0 0 9]
data = [0 0 0 0 0 0 0 0 9 0]
// 第二次append后结果
slice = [0 0 0 9 10 11 12]
data = [0 0 0 0 0 0 0 0 9 0]
```

### 扩容策略

扩容实际上包括两部分：计算容量的规则 和 内存对齐

1. 如果期望容量大于当前容量的两倍就会使用期望容量，期望容量指的是把元素加进去后的容量。

2. 如果当前切片的长度小于 1024，扩容两倍。

3. 如果当前切片的长度大于 1024 就会每次增加 25% 的容量，即扩容1.25倍，直到新容量大于期望容量。

4. 此时只是确定切片的大致容量，之后会判断切片中元素所占字节大小，如果字节大小是1、2、8的倍数时，会进行内存对齐，这个操作之后，扩容后的容量可能会 > 原容量的两倍 或 1.25倍。

   内存对齐主要是为了提高内存分配效率，减少内存碎片。

```go
// 此时期望容量是2 + 3 = 5 > 旧容量的两倍 2 * 2 = 4，期望容量为5，占40个字节，触发内存对齐，向上取整为48字节，此时新容量为 48 / 8 = 6。
s1 := []int64{1, 2}
s1 = append(s1, 4, 5, 6)
fmt.Printf("len=%d, cap=%d\n",len(s1),cap(s1))  // len=5, cap=6

// 第一次append，扩容，拷贝旧数据到新数组，容量增长两倍；
// 第二次append，没有产生新数组，只将元素进行追加；
// 第三次append，扩容，拷贝旧数据到新数组，容量增长两倍；
s := []int{1, 2}
s = append(s, 4)
fmt.Printf("len=%d, cap=%d\n",len(s),cap(s))  // len=3, cap=4
s = append(s, 5)
fmt.Printf("len=%d, cap=%d\n",len(s),cap(s))  // len=4, cap=4
s = append(s, 6)
fmt.Printf("len=%d, cap=%d\n",len(s),cap(s))  // len=5, cap=8
```

# Map

## 数据结构

```go
type hmap struct {
	count     int    // 哈希表中元素的数量
	flags     uint8  // 记录map的状态, 1：可能有迭代器使用buckets，2：可能有迭代器使用oldbuckets，4：有协程正在向map中写入key，8：等量扩容
    B         uint8  // buckets的数量，len(buckets) = 2^B
	noverflow uint16 // 溢出的bucket的个数
	hash0     uint32 // 哈希种子，为哈希函数的结果引入随机性。该值在创建哈希表时确定，在构造方法中传入

	buckets    unsafe.Pointer // 桶的地址，指向一个bmap数组，即指向很多个桶
	oldbuckets unsafe.Pointer // 扩容时用于保存之前buckets的字段，大小是当前buckets的一半或0.75，非扩容状态下为null
	nevacuate  uintptr // 扩容迁移的进度，小于nevacuate的buckets表示已迁移完成

	extra *mapextra // 用于扩容的指针，存储单个桶装满时溢出的数据，溢出桶和正常桶在内存上是连续的，该字段是为了优化GC扫描而设计的。
}

type mapextra struct {
    // 当map的key和value都不是指针，并且size都小于128字节时（即可以被inline），会把bmap标记为不含指针，避免gc时扫描整个hmap。
    // 通过overflow实现，bmap.overflow是个指针，指向溢出的bucket，GC时又必定会扫描指针，也就是会扫描所有bmap，
    // 而当map的key和value都是非指针类型时，可直接标记整个map的颜色，避免扫描每个bmap的overflow指针，
    // 但溢出的bucket总是存在，与key和value的类型无关，于是就利用overflow来指向溢出的bucket，
    // 并把bmap结构体里的overflow指针类型变成unitptr类型（编译期干的），于是整个bmap就完全没指针了，也就不会被GC扫描。
    // 另一方面，当 GC 在扫描 hmap 时，通过 extra.overflow 这条路径（指针）就可以将 overflow 的 bucket 正常标记成黑色，从而不会被 GC 错误地回收。
	overflow    *[]*bmap  // 包含hmap.buckets的overflow的buckets
	oldoverflow *[]*bmap  // 包含扩容时的hmap.oldbuckets的overflow的bucket
	nextOverflow *bmap    // 指向空闲的 overflow bucket 的指针
}

// 即桶bucket的结构
type bmap struct {
    tophash [bucketCnt]uint8 // len为8的数组，即每个桶只能存8个键值对，包含此桶中每个key的哈希值的高8位，如果tophash[0] < minTopHash，tophash[0]则代表桶的搬迁evacuation状态。
}

// 但由于go没有泛型，哈希表中又可能存储不同类型的键值对，所以键值对所占的内存空间大小只能在编译时推导，
// 无法先设置在结构体中，这些字段是在运行时通过计算内存地址的方式直接访问，这些额外的字段都是编译时动态创建
type bmap struct {
    topbits  [8]uint8    // 通过tophash找到对应键值对在keys和values数组中的下标
    keys     [8]keytype
    values   [8]valuetype
    pad      uintptr
    overflow uintptr // 每个桶只能存8个元素，超过8个时会存入溢出桶，溢出桶只是临时方案，溢出过多时会进行扩容
}

// 重要的常量标志
const (
    // 一个桶中最多能装载的键值对（key-value）的个数为8
    bucketCntBits = 3
    bucketCnt     = 1 << bucketCntBits

    // 触发扩容的装载因子为13/2=6.5
    loadFactorNum = 13
    loadFactorDen = 2

    // 键和值超过128个字节，就会被转换为指针
    maxKeySize  = 128
    maxElemSize = 128

    // 数据偏移量应该是bmap结构体的大小，它需要正确地对齐。
    // 对于amd64p32而言，这意味着：即使指针是32位的，也是64位对齐。
    dataOffset = unsafe.Offsetof(struct {
        b bmap
        v int64
    }{}.v)


    // 每个桶（如果有溢出，则包含它的overflow的链接桶）在搬迁完成状态下，要么会包含它所有的键值对，要么一个都不包含（但不包括调用evacuate()方法阶段，该方法调用只会在对map发起write时发生，在该阶段其他goroutine是无法查看该map的）。简单的说，桶里的数据要么一起搬走，要么一个都还未搬。
    // tophash除了放置正常的高8位hash值，还会存储一些特殊状态值（标志该cell的搬迁状态）。正常的tophash值，最小应该是5，以下列出的就是一些特殊状态值。
    emptyRest      = 0 // 表示cell为空，并且比它高索引位的cell或者overflows中的cell都是空的。（初始化bucket时，就是该状态）
    emptyOne       = 1 // 空的cell，cell已经被搬迁到新的bucket
    evacuatedX     = 2 // 键值对已经搬迁完毕，key在新buckets数组的前半部分
    evacuatedY     = 3 // 键值对已经搬迁完毕，key在新buckets数组的后半部分
    evacuatedEmpty = 4 // cell为空，整个bucket已经搬迁完毕
    minTopHash     = 5 // tophash的最小正常值

    // flags
    iterator     = 1 // 可能有迭代器在使用buckets
    oldIterator  = 2 // 可能有迭代器在使用oldbuckets
    hashWriting  = 4 // 有协程正在向map写人key
    sameSizeGrow = 8 // 等量扩容

    // 用于迭代器检查的bucket ID
    noCheck = 1<<(8*sys.PtrSize) - 1
)
```

![go map 结构图](https://github.com/Nixum/Java-Note/raw/master/picture/go_map_struct.png)

注：一个bmap里key和value是各自存的，而key/value一对对存储，这样的好处是省掉padding字段，节省内存空间，方便内存对齐。

> 例如，有这样一个类型的 map：`map[int64]int8`，如果按照 `key/value...` 这样的模式存储，那在每一个 key/value 对之后都要额外 padding 7 个字节；而将所有的 key，value 分别绑定到一起，这种形式 `key/key/.../value/value/...`，则只需要在最后添加 padding，每个 bucket 设计成最多只能放 8 个 key-value 对，如果有第 9 个 key-value 落入当前的 bucket，那就需要再构建一个 bucket ，通过 `overflow` 指针连接起来。

## 基本

* 创建：`m := map[string]int{"1": 11, "2": 22}`或者 `m := make(map[string]int, 10)`

* 当使用字面量的方式创建哈希表时，如果{}中元素少于或等于25时，编译器会转成make的方式创建，再进行赋值；如果超过了25个，编译时会转成make的方式创建，同时为key和value分别创建两个数组，最后进行循环赋值

* 直接声明 `var m map[string]int`此时创建了一个**nil的map**，此时不能被赋值，但可以取值，虽然不会panic，但会得到零值

* key不允许为slice、map、func，允许bool、numeric、string、指针、channel、interface、struct

  即key必须支持 == 或 != 运算的类型

* map容量最多为 6.5 * 2^B 个元素，6.5是装载因子阈值常量，装载因子 = 哈希表中的元素 / 哈希表总长度，装载因子越大，冲突越多。B最大值是63.

* 拉链法解决哈希冲突（指8个正常位和溢出桶），除留余数法得到桶的位置（哈希值的低B位）。

* key的哈希值的低B位计算获得桶的位置，高8位计算得到tophash的位置，进而找到key的位置。

* 溢出桶也是一个bmap，bmap的overflow会指向下一个溢出桶，所以**溢出桶的结构是链表，但是它们跟正常桶是在一片连续内存上，都在buckets数组里**。

* 每个桶存了8个tophash + 8对键值对。

* map是非线程安全的，扩容不是一个原子操作，通过hmap里的flag字段在并发修改时进行fast-fail。

* map的遍历是无序的，每次遍历出来的结果的顺序都不一样。

## 创建初始化

通过`make(map[type]type)`，或者`make(map[type]type, n), n <= 8`创建的map，底层会调用makemap_small函数，并直接从堆上进行分配。

```go
func makemap_small() *hmap {
    h := new(hmap)
    h.hash0 = fastrand()
    return h
}
```

当hint > 8时，则会在运行时调用makemap函数进行创建和初始化，

1. 计算哈希表占用的内存是否溢出或者超出能分配的最大值

2. 调用fastrand()获取随机哈希种子

3. 根据hint来计算需要的桶的数量来计算B的值（hint是make的第二个参数），用于初始化桶的数量 = 2^B；

4. 调用makeBucketArray()分配连续的空间，创建用于保存桶的数组

   当桶的数量小于2^4时，由于数据较少，哈希冲突的可能性较小，此时不会创建溢出桶。

   当桶的数量大于2^4时，就会额外创建2^(B-4)个溢出桶，溢出桶与普通桶在内存空间上是连续的。

```go
func makemap(t *maptype, hint int, h *hmap) *hmap {
...
    h.hash0 = fastrand()
	// 函数内，计算桶的数量
	B := uint8(0)
	//计算得到合适的B
	for overLoadFactor(hint, B) {
		B++
    }
    h.B = B
    if h.B != 0 {
		var nextOverflow *bmap
		h.buckets, nextOverflow = makeBucketArray(t, h.B, nil)
		if nextOverflow != nil {
			h.extra = new(mapextra)
			h.extra.nextOverflow = nextOverflow
		}
	}
...
}

func overLoadFactor(count int, B uint8) bool {
	// 常量loadFactorNum=13 ，loadFactorDen=2，bucketCnt=8，bucketShift()函数返回2^B
	return count > bucketCnt && uintptr(count) > loadFactorNum*(bucketShift(B)/loadFactorDen)
}

func makeBucketArray(t *maptype, b uint8, dirtyalloc unsafe.Pointer) (buckets unsafe.Pointer, nextOverflow *bmap) {
	base := bucketShift(b)  // base = 2 ^ B
	nbuckets := base
    // B < 4时，即桶的数量小于16，认为哈希冲突几率较小，因此不会创建溢出桶
    // B >= 4时，创建2^(B-4)个溢出桶
	if b >= 4 {
		nbuckets += bucketShift(b - 4)
		sz := t.bucket.size * nbuckets
		up := roundupsize(sz)
		if up != sz {
			nbuckets = up / t.bucket.size
		}
	}
    if dirtyalloc == nil {
        // 为null, 会分配一个新的底层数组
		buckets = newarray(t.bucket, int(nbuckets))
	} else {
        // 不为null，则它指向的是曾经分配过的底层数组，该底层数组是由之前同样的t和b参数通过makeBucketArray分配的，如果数组不为空，需要把该数组之前的数据清空并复用
		buckets = dirtyalloc
		size := t.bucket.size * nbuckets
		if t.bucket.ptrdata != 0 {
			memclrHasPointers(buckets, size)
		} else {
			memclrNoHeapPointers(buckets, size)
		}
	}
	// 如果多申请了桶，将多申请的桶放在nextOverflow里备用
	if base != nbuckets {
        // 先计算出多申请出来的内存地址 nextOverflow
		nextOverflow = (*bmap)(add(buckets, base*uintptr(t.bucketsize)))
        // 计算出申请的最后一块bucket的地址
		last := (*bmap)(add(buckets, (nbuckets-1)*uintptr(t.bucketsize)))
        // 将最后一块bucket的overflow指针（指向链表的指针）指向buckets 的首部。 原因呢，是为了将来判断是否还有空的bucket 可以让溢出的bucket空间使用。
		last.setoverflow(t, (*bmap)(buckets))
	}
	return buckets, nextOverflow
}
```

## 查找与插入

`v := m[key]`使用函数mapaccess1()进行查找， `v, ok := m[key]`使用函数mapaccess2()进行查找

mapaccess2也会调用mapaccess1，只是返回的时候会返回多一个用于表示当前键值对是否存在的布尔值。

### key的定位

1. 找buckets数组中的bucket的位置：key经过哈希计算得到哈希值，取出hmap的B值，取哈希值的后B位，计算后面的B位的值得到桶的位置（实际上这一步就是除留余数法的取余操作）。
2. 确定使用buckets数组还是oldbuckets数组：判断oldbuckets数组中是否为空，不为空说明正处于扩容中，还没完成迁移，则重新计算桶的位置，并在oldbuckets数组找到对应的桶；如果为空，则在buckets数组中找到对应的桶。
3. 在桶中找tophash的位置：用key哈希计算得到的哈希值，取高8位，计算得到此bucket桶中的tophash，之后在桶中的正常位遍历比较。
4. 每个桶是一整片连续的内存空间，先遍历bucket桶中的正常位，与桶中的tophash进行比较，当找到对应的tophash时，根据tophash进行计算得到key，根据key的大小计算得到value的地址，找到value。
5. 如果bucket桶中的正常位没找到tophash，且overflow不为空，则继续遍历溢出桶overflow bucket，直到找到对应的tophash，再根据key的大小计算得到value的地址，找到value。

```go
// 计算得到bucket桶在buckets数组中的位置
bucket := hash & bucketMask(h.B)
b := (*bmap)(unsafe.Pointer(uintptr(h.buckets) + bucket*uintptr(t.bucketsize)))

// 计算得到tophash，miniTopHash用于表示迁移进度
top := uint8(hash >> (sys.PtrSize*8-8))
if top < minTopHash {
    top += minTopHash
}
return top

// 计算key和value，dataoffset是tophash[8]所占用的大小，所以key的地址就是：b的地址 + dataOffset的偏移 + 对应的索引i * key的大小
k := add(unsafe.Pointer(b), dataOffset+i*uintptr(t.keysize))
val = add(unsafe.Pointer(b), dataOffset+bucketCnt*uintptr(t.keysize)+i*uintptr(t.valuesize))
```

![go map 结构图](https://github.com/Nixum/Java-Note/raw/master/picture/go_map_get.png)

### 插入

插入也需要先定位到key的位置后才能进行插入，定位key的操作跟上面是类似的，调用mapassign函数。

1. 先判断hmap是否为空，是否是并发写入，如果是直接抛错误。
2. 对key进行哈希，如果bucket桶为空，则分配bmap数组，如果没有指定长度，则惰性分配。
3. 根据key的哈希值的低B位，计算得到桶的位置；判断是否正在扩容。
4. 根据key的哈希值的高8位，计算出tophash，先遍历正常位，从第一个tophash开始，比较桶上的每个tophash是否等于计算得到的tophash，如果不等，再判断该tophash是否为空，如果为空，计算key和value的内存地址，进行插入。如果不为空，则遍历下一个tophash。
5. 如果桶上的tophash与计算的tophash相等，说明发生了哈希冲突，先计算key的地址，找到key，判断key是否相等，如果相等，计算key对应的value地址，将value的值进行更新。
6. 如果key不相等，遍历下一个tophash，直到正常位遍历完成，如果此时还不能插入，继续遍历溢出桶，如果溢出桶为空，退出循环
7. 判断是否扩容，如果需要扩容，则扩容后（扩容迁移）继续从步骤5上继续，如果不用扩容则走步骤8
8. 如果还没进行插入，说明正常位已经满了，且还不需要扩容，此时会调用newoverflow函数，先使用hmap预先在noverflow中创建好的桶，如果有，遍历这个创建好的桶链表，直到可以放入新的键值对；如果没有，则创建一个桶，增加noverflow计数，将新键值对放入这个桶中，然后将新桶挂载到当前桶overflow字段，成为溢出桶。

[mapassign函数](https://github.com/Nixum/Java-Note/raw/master/source_with_note/go_map_mapassign.go)

[newoverflow函数](https://github.com/Nixum/Java-Note/raw/master/source_with_note/go_map_newoverflow.go)

## 扩容

### 扩容条件

没有正在进行扩容 && （负载因子超过6.5 || 存在过多溢出桶 overflow buckets）

即`!h.growing() && (overLoadFactor(h.count+1, h.B) || tooManyOverflowBuckets(h.noverflow, h.B))`

### 扩容策略

#### 增量扩容 - 降低哈希冲突

overLoadFactor函数，该函数返回true，表示哈希表内的元素过多，哈希冲突的概率变大，查找可能在找到桶，遍历完桶内的元素，还要继续遍历溢出桶链表，此时需要**增量扩容，扩容为原来的两倍 **，降低哈希冲突的概率。

```go
func overLoadFactor(count int, B uint8) bool {
    // loadFactorNum = 13, loadFactorDen = 2, 即count > 8 && count / (2^B) > 6.5
    return count > bucketCnt && uintptr(count) > loadFactorNum *(bucketShift(B)/loadFactorDen)
}
```

#### 等量扩容 - 提高桶的利用率，防止内存泄漏，加快查询效率

tooManyOverflowBuckets函数，该函数返回true，表示由于某一个桶满后，开始使用溢出桶，不断的插入数据到溢出桶，又不断的删除正常桶上的正常位，但此时哈希表的数量又没超阈值，即空桶太多，溢出桶的数量太多，而每次查找又得先遍历正常位，查找效率变低，此时需要**等量扩容，容量不变，重新迁移键值对**。

```go
func tooManyOverflowBuckets(noverflow uint16, B uint8) bool {
    // 如果负载因子太低, 不操作。
    // 如果负载因子太高，maps的扩容和缩容会使用大量未使用的内存
    // 太多指的是溢出桶的数量和buckets数组的数量一样.
    if B > 15 {
        B = 15
    }
    // The compiler doesn't see here that B < 16; mask B to generate shorter shift code.
    return noverflow >= uint16(1)<<(B&15)
}
```

### 触发扩容

触发扩容条件时，会执行hashGrow函数，进行新桶的分配，此时还未迁移数据。

1. 首先会判断是增量扩容还是等量扩容，如果是增量扩容，B + 1，如果是等量扩容，B + 0

2. 将当前buckets数组挂在hmap的oldbuckets字段，当前的溢出桶挂在hmap.mapextra.oldoverflow

3. 创建新的buckets数组，容量为新的B值，预创建溢出桶（溢出桶的数量看上面创建初始化逻辑），然后将新的buckets数组挂在buckets字段，新的溢出桶挂在hmap.mapextra.nextOverflow字段上

触发扩容条件，对新桶进行内存分配，只是创建了新的桶，旧数据还在旧桶上，之后还需要完成数据迁移。

[hashGrow函数](https://github.com/Nixum/Java-Note/raw/master/source_with_note/go_map_hashGrow.go)

### 扩容迁移

扩容迁移发生在 mapassign 和 mapdelete 函数中，即进行插入、修改、删除时，才会调用growWrok函数和evacuate函数，完成真正的迁移工作后，才会进行插入、修改或删除。

迁移时是渐进式迁移，一次最多迁移两个bucket桶。

1. 在插入、修改或删除中，如果发现oldbuckets数组不为空，表示此时正在扩容中，需要进行扩容迁移，调用growWork函数，growWork函数调用一次evacuate函数，如果调用完成后，hmap的oldbuckets还是非空，则再调用一次evacuate函数，加快迁移进程。

2. 进入evacuate函数，如果是等量扩容，B值不变，老bucket桶上的键值计算出来的桶的序号不变，tophash不变，此时会将老桶上的键值对依次地一个个转移到新桶上，使这些键值对在新桶上排列更加紧凑；

   如果是增量扩容，容量变为原来的两倍，B值+1，老bucket桶上的键值计算出来的桶的序号改变，这些键值对计算后的bucket桶的序号可能跟之前一样，也可能是相比原来加上2^B，取决于key哈希值后 老B+1 位的值是0还是1，tophash不变，原来老bucket桶上的键值对会分流到两个新的bucket桶上。将老bucket桶上的键值对和其指向的溢出桶上的键值对进行迁移，依次转移到新桶上，每迁移完一个，hmap的nevacuate计数+1，直到老bucket桶上的键值对迁移完成，最后情况oldbuckets和oldoverflow字段

[evacuate函数](https://github.com/Nixum/Java-Note/raw/master/source_with_note/go_map_evacuate.go)

## 删除

调用delete函数，无论要删除的key是否存在，delete都不会返回任何结果。删除实际上也是一个key的定位 + 删除的操作，定位到key后，将其键值对置空，hmap的count - 1，tophash置为empty。

## 遍历

对go中的map是无序的，每次遍历出来的顺序都是不一样的，go在每次遍历map时，并不是固定地从0号bucket桶开始遍历，每次都是从一个随机值序号的bucket桶开始遍历，并且是从这个bucket桶的一个随机序号的 正常位开始遍历。

> 1. 首先从buckets数组中，随机确定一个索引，作为startBucket，然后确定offset偏移量，得到桶中的正常位的位置，作为起始key的地址。
> 2. 遍历当前bucket及bucket.overflow，判断当前bucket是否正在扩容中，如果是则跳转到3，否则跳转到4。
> 3. 如果是在扩容中，遍历时会先到当前bucket扩容前的老的bucket桶中遍历那些能迁移到当前桶的key。
>
> 假如原先的buckets为0，1，那么扩容后的新的buckets为0，1，2，3，此时我们遍历到了buckets[0]， 发现这个bucket正在扩容，那么找到bucket[0]所对应的oldbuckets[0]，遍历里面的key，这时候仅仅遍历那些key经过hash后，可以散列到bucket[0]里面的部分key；同理，当遍历到bucket[2]的时候，发现bucket正在扩容，找到oldbuckets[0]，然后遍历里面可以散列到bucket[2]的那些key。
>
> 4. 遍历当前这个bucket即可。
> 5. 继续遍历bucket下面的overflow链表。
> 6. 如果遍历到了startBucket，说明遍历完了，结束遍历。

# 参考

[从 map 的 extra 字段谈起](https://cloud.tencent.com/developer/article/1859042)

[Go入门指南](https://learnku.com/docs/the-way-to-go/chapter-description/3611)

[深入理解Slice底层实现](https://zhuanlan.zhihu.com/p/61121325)

[Go 语言设计与实现](https://draveness.me/golang/docs/part2-foundation/ch03-datastructure/golang-array-and-slice)

[Golang源码-Map实现原理分析](https://studygolang.com/articles/27421)

[Go map原理剖析](https://segmentfault.com/a/1190000020616487)

[深度解密Go语言之channel](https://zhuanlan.zhihu.com/p/74613114)
