[TOC]

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
> new (T) 为每个新的类型 T 分配一片内存，初始化为 0 并且返回类型为 *T 的内存地址：这种函数 返回一个指向类型为 T，值为 0 的地址的指针，它**适用于值类型如数组和结构体**；它相当于 &T{}。
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
	// 1：可能有迭代器使用buckets，2：可能有迭代器使用oldbuckets，4：有协程正在向map中写入key，8：等量扩容
	flags     uint8  // 记录map的状态
    B         uint8  // buckets的数量，len(buckets) = 2^B
	noverflow uint16 // 溢出的bucket的个数
	hash0     uint32 // 哈希种子，为哈希函数的结果引入随机性。该值在创建哈希表时确定，在构造方法中传入

	buckets    unsafe.Pointer // 桶的地址
	oldbuckets unsafe.Pointer // 扩容时用于保存之前buckets的字段，大小是当前buckets的一半或0.75
	nevacuate  uintptr // 迁移进度，小于nevacuate的表示已迁移

	extra *mapextra // 用于扩容的指针，单个桶装满时用于存储溢出数据，溢出桶和正常桶在内存上是连续的
}

type mapextra struct {
	overflow    *[]*bmap
	oldoverflow *[]*bmap
	nextOverflow *bmap
}

type bmap struct {
    tophash [bucketCnt]uint8 // len为8的数组，即每个桶只能存8个键值对
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
```

![go map 结构图](https://github.com/Nixum/Java-Note/raw/master/Note/picture/go_map_struct.png)

## 基本

* 创建：`m := map[string]int{"1": 11, "2": 22}`或者 `m := make(map[string]int, 10)`

* 当使用字面量的方式创建哈希表时，如果{}中元素少于或等于25时，编译器会转成make的方式创建，再进行赋值；如果超过了25个，编译时会转成make的方式创建，同时为key和value分别创建两个数组，最后进行循环赋值

* 直接声明 `var m map[string]int`此时创建了一个nil的map，此时不能被赋值

* key不允许为slice、map、func，允许bool、numeric、string、指针、channel、interface、struct

  即key必须支持 == 或 != 运算的类型

* map的容量为 装载因子6.5 * 2^B 个元素，装载因子 = 哈希表中的元素 / 哈希表总长度，装载因子越大，冲突越多。

* 拉链法解决哈希冲突（指8个正常位和溢出桶），除留余数法得到桶的位置（哈希值的低B位）。

* key的哈希值的低B位计算获得桶的位置，高8位计算得到tophash的位置，进而找到key的位置。

* 溢出桶也是一个bmap，bmap的overflow会指向下一个溢出桶，所以**溢出桶的结构是链表，但是它们跟正常桶是在一片连续内存上，都在buckets数组里**。

* 每个桶存了8个tophash + 8对键值对。

* map是非线程安全的，扩容不是一个原子操作，通过hmap里的flag字段在并发修改时进行fast-fail。

* map的遍历是无序的，每次遍历出来的结果的顺序都不一样。

## 创建初始化

最终会在运行时调用makemap函数进行创建和初始化，
1. 计算哈希表占用的内存是否溢出或者超出能分配的最大值

2. 调用fastrand()获取随机哈希种子

3. 根据hint来计算需要的桶的数量，即计算B的值，用于初始化桶的数量 = 2^B；hint是一个预置的长度。

   这个值不知道怎么来的，本人的机器debug时发现默认创建map时，hint=137，B=5

4. 调用makeBucketArray()分配连续的空间，创建用于保存桶的数组

   当桶的数量小于2^4时，由于数据较少，哈希冲突的可能性较小，此时不会创建溢出桶。

   当桶的数量大于2^4时，就会额外创建2^(B-4)个溢出桶，溢出桶与普通桶在内存空间上是连续的。

```go
func makemap(t *maptype, hint int, h *hmap) *hmap {
...
	// 函数内，计算桶的数量
	B := uint8(0)
	//计算得到合适的B
	for overLoadFactor(hint, B) {
		B++
    }
    h.B = B
...
}

func overLoadFactor(count int, B uint8) bool {
	// 常量loadFactorNum=13 ，loadFactorDen=2，bucketCnt=8，bucketShift()函数返回2^B
	return count > bucketCnt && uintptr(count) > loadFactorNum*(bucketShift(B)/loadFactorDen)
}

func makeBucketArray(t *maptype, b uint8, dirtyalloc unsafe.Pointer) (buckets unsafe.Pointer, nextOverflow *bmap) {
	base := bucketShift(b)
	nbuckets := base
	if b >= 4 {
		nbuckets += bucketShift(b - 4)
		sz := t.bucket.size * nbuckets
		up := roundupsize(sz)
		if up != sz {
			nbuckets = up / t.bucket.size
		}
	}
	buckets = newarray(t.bucket, int(nbuckets))
	// 如果多申请了桶，将多申请的桶放在nextOverflow里备用
	if base != nbuckets {
		nextOverflow = (*bmap)(add(buckets, base*uintptr(t.bucketsize)))
		last := (*bmap)(add(buckets, (nbuckets-1)*uintptr(t.bucketsize)))
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

![go map 结构图](https://github.com/Nixum/Java-Note/raw/master/Note/picture/go_map_get.png)

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

[mapassign函数](https://github.com/Nixum/Java-Note/raw/master/Note/source_with_note/go_map_mapassign.go)

[newoverflow函数](https://github.com/Nixum/Java-Note/raw/master/Note/source_with_note/go_map_newoverflow.go)

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

[hashGrow函数](https://github.com/Nixum/Java-Note/raw/master/Note/source_with_note/go_map_hashGrow.go)

### 扩容迁移

扩容迁移发生在 mapassign 和 mapdelete 函数中，即进行插入、修改、删除时，才会调用growWrok函数和evacuate函数，完成真正的迁移工作后，才会进行插入、修改或删除。

迁移时是渐进式迁移，一次最多迁移两个bucket桶。

1. 在插入、修改或删除中，如果发现oldbuckets数组不为空，表示此时正在扩容中，需要进行扩容迁移，调用growWork函数，growWork函数调用一次evacuate函数，如果调用完成后，hmap的oldbuckets还是非空，则再调用一次evacuate函数，加快迁移进程。

2. 进入evacuate函数，如果是等量扩容，B值不变，老bucket桶上的键值计算出来的桶的序号不变，tophash不变，此时会将老桶上的键值对依次地一个个转移到新桶上，使这些键值对在新桶上排列更加紧凑；

   如果是增量扩容，容量变为原来的两倍，B值+1，老bucket桶上的键值计算出来的桶的序号改变，这些键值对计算后的bucket桶的序号可能跟之前一样，也可能是相比原来加上2^B，取决于key哈希值后 老B+1 位的值是0还是1，tophash不变，原来老bucket桶上的键值对会分流到两个新的bucket桶上。将老bucket桶上的键值对和其指向的溢出桶上的键值对进行迁移，依次转移到新桶上，每迁移完一个，hmap的nevacuate计数+1，直到老bucket桶上的键值对迁移完成，最后情况oldbuckets和oldoverflow字段

[evacuate函数](https://github.com/Nixum/Java-Note/raw/master/Note/source_with_note/go_map_evacuate.go)

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

# Channel

Channel的设计基于CSP模型。

CSP模型（Communicating Sequential Process，通信顺序进程），允许使用进程组来描述系统，独立运行，并且只通过消息传递的方式通信。

本质上就是，在使用协程执行函数时，不通过内存共享(会用到锁)的方式通信，而是通过Channel通信传递数据。

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
	recvq    waitq  // 等待队列，存储当前channel因缓冲区空间不足而接收阻塞的goroutine列表，双向链表
	sendq    waitq  // 等待队列，存储当前channel因缓冲区空间不足而发送阻塞的goroutine列表，双向链表

	lock mutex  // 互斥锁，保证每个读channel或写channel的操作都是原子的
}

type waitq struct {
	first *sudog
	last  *sudog
}
```

## 基本

* chan是引用类型，使用make关键字创建，未初始化时的零值是nil，如

  `ch := make(chan, string, 10)`，创建一个能处理string的缓冲区大小为10的channel，效果相当于异步队列，除非缓冲区用完，否则不会阻塞；
  
  `ch := make(chan, string)`，则创建了一个不存在缓冲区的channel，效果相当于同步阻塞队列，即如果连续发送两次数据，第一次如果没有被接收的话，第二次就会被阻塞。
  
* channel作为通道，负责在多个goroutine间传递数据，解决多线程下共享数据竞争问题。

* 当 chan是 nil时，对chan的发送和接收的调用者总是阻塞的

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

* 接收数据时可以有两个返回值，第一个是返回的元素，第二个是bool类型，表示是否成功地从chan中读取到一个值。如果是false，说明chan以及被close并且chan中没有缓存的数据，此时第一个元素是零值。所以，如果接收时第一个元素是零值，可能是sender真的发送了零值，也可能是closed并且没有元素导致的。
* 双向chan可以赋值给单向chan，但反过来不可以

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

使用` str <- ch 或 str, ok <- ch ok用于判断ch是否关闭，如果没有ok，可能会无法分配str接收到的零值是发送者发的还是ch关闭`接收数据，会转化为调用chanrecv1和chanrecv2函数，但最终会调用chanrecv函数接收数据。chanrecv1和chanrecv2函数都是设置阻塞参数为true。

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

# Runtime

* 不同于Java，Go没有虚拟机，很多东西比如自动GC、对操作系统和CPU相关操作都变成了函数，写在runtime包里。
* Runtime提供了go代码运行时所需要的基础设施，如协程调度、内存管理、GC、map、channel、string等内置类型的实现、对操作系统和CPU相关操作进行封装。
* 诸如go、new、make、->、<-等关键字都被编译器编译成runtime包里的函数
* build成可执行文件时，Runtime会和用户代码一起进行打包。

## 内存模型

由于不同的架构和不同的编译器优化，会发生指令重排，导致程序运行时不一定会按照代码的顺序执行，因此两个goroutine在处理共享变量时，能够看到其他goroutine对这个变量进行的写结果。

happens-before：程序的执行顺序和代码的顺序一样，就算真的发生了重排，从行为上也能保证和代码的指定顺序一样。

Go不像Java有volatile关键字实现CPU屏障来保证指令不重排，而是使用不同架构的内存屏障指令来实现同一的并发原语。

Go只保证goroutine内部重排对读写顺序没有影响，如果存在共享变量的访问，则对另一个goroutine影响很大。因此当有多个goroutine对共享变量的操作时，需要保证对该共享变量操作的happens-before顺序，保证并发安全，常用的手段就是它的并发操作相关的包，比如：

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



# Goroutine

## 基本

* 一般线程会占有1Mb以上的内存空间，每次对线程进行切换时会消耗较多内存，恢复寄存器中的内容还需要向操作系统申请或销毁对应的资源，每一次上下文切换都需要消耗~1us左右的时间，而Go调度器对goroutine的上下文切换为~0.2us，减少了80%的额外开销。

* 协程本质是一个数据结构，封装了要运行的函数和运行的进度，交由go调度器进行调度，不断切换的过程。由调度器决定协程是运行，还是切换出调度队列(阻塞)，去执行其他满足条件的协程。

  go的调度器在用户态实现调度，调度的是一种名叫协程的执行流结构体，也有需要保存和恢复上下文的函数，运行队列。

  协程同步造成的阻塞，只是调度器切换到别的协程去执行了，线程本身并不阻塞。

* Go的调度器通过**使用与CPU数量相等的线程**减少线程频繁切换的内存开销，同时**在每一个线程上执行额外开销更低的Goroutine**来降低操作系统和软件的负载。

* GPM模型 - M：N调度模型

* 1.2~1.3版本使用**基于协作的抢占式调度器**（通过编译器在函数调用时插入抢占式检查指令，在函数调用时检查当前goroutine是否发起抢占式请求），但gouroutine可能会因为垃圾回收和循环长时间占用资源导致程序暂停。

  从1.14版本开始使用**基于信号的抢占式调度**，垃圾回收在扫描栈时会触发抢占式调度，但抢占时间点不够多，还不能覆盖全部边缘情况。

  之所以要使用抢占式的，是因为不使用抢占式时，只有当goroutine主动让出CPU资源才能触发调度，可能会导致某个goroutine长时间占用线程，造成其他goroutine饿死；另外，垃圾回收需要暂停整个程序，在STW时，整个程序无法工作。

## 调度器的GPM模型

goroutine完全运行在用户态，借鉴M：N线程映射关系，采用GPM模型管理goroutine。

* G：即goroutine，代码中的`go func{}`，代表一个待执行的任务
* M：即machine，操作系统的线程，由操作系统的调度器调度和管理。
* P：即processor，处理器，运行在线程上的本地调度器，用来管理和执行goroutine，使得goroutine在一个线程上跑，提供了线程需要的上下文（用于在同一线程写多个goroutine的切换），负责调度线程上的LRQ，是实现从N：1到N：M映射的关键。

## GPM三者的关系与特点

* p的个数取决于GOMAXPROCS，默认使用CPU的个数，这些P会绑定到不同内核线程，尽量提升性能，让每个核都有代码在跑。

* M的数量不一定和P匹配，课堂设置多个M，M和P绑定后才可运行，多余的M会处于休眠状态。

  调度器最多可创建10000个M，但最多只有GOMAXPROCS个活跃线程能够正常运行。

  所以一般情况下，会设置与P一样数量的M，让所有的调度都发生在用户态，减少额外的调度和上下文切换开销。

* P包含一个LRQ(Local Run Queue本地运行队列)，保存P需要执行的goroutine的队列。LRQ是一个环形链表，最多存储256个待执行goroutine，当LRQ不够用时，新创建的goroutine会保存在GRQ中。

* 调度器本身包含一个GRQ(Global Run Queue全局运行队列)，保存所有未分配的goroutine。

## 调度的时机

* go调度器，本质是为需要执行的G寻找M以及P，不是一个实体，调度是需要发生调度时由M执行runtime.schedule方法进行
* channel、mytex等sync操作发生协程阻塞
* time.sleep
* IO
* GC
* 主动yield
* 运行过久或系统调度过久

## 调度Demo

单核机器，只有一个处理器P，系统初始化两个线程M0和M1，处理器P优先绑定线程M0，线程M1进入休眠状态。目前P正在处理G0，LRQ里的G1、G2、G3等待处理，GRQ里的G4、G5等到分配。

如果G0短时间处理完，P就会从LRQ取出G1进行处理，LRQ从GRQ取出G4进行分配；

![goroutine runtime_1](https://github.com/Nixum/Java-Note/raw/master/Note/picture/go_goroutine_runtime1.png)

如果G0处理得很慢，系统就会让M0休眠，挂起G0，唤醒线程M1，将LRQ转移给M1进行处理；

如果此时G1也处理得很慢，此时会阻塞，或者休眠M1，唤醒M0，回去继续处理G0；**切换M和G的操作由sysmon协程进行处理，即抢占式由sysmon函数实现**。

如果G1处理得很快，则继续获取LRQ里的下一个G；待LRQ里的G都执行完了，切回M0，继续处理G0。

![goroutine runtime_2](https://github.com/Nixum/Java-Note/raw/master/Note/picture/go_goroutine_runtime2.png)

如果是多核的，有多个P，多个M，当有一个P处理完所有的G后，会先从GRQ中获取G，如果获取不到，就会从另一个P的LRQ里取走一半G，继续处理。

## sysmon协程

由sysmon协程进行协作式抢占，对goroutine进行标记，执行goroutine时如果有标记就会让出CPU，对于syscall过久的P，会进行M和P的分配，防止P被占用过久影响调度。

![go sysmon goroutine](https://github.com/Nixum/Java-Note/raw/master/Note/picture/go_sysmon_goroutine.png)

## M：Machine

M本质时一个循环调度，不断的执行schedule函数，查找可运行的G。会在自旋与休眠的状态间转换

## G：Goroutine的状态

![go goroutine state](https://github.com/Nixum/Java-Note/raw/master/Note/picture/go_goroutine_state.png)

goroutine的状态不止以下几种，只是这几种比较常用

| G状态       | 值     | 说明                                                         |
| ----------- | ------ | ------------------------------------------------------------ |
| _Gidle      | 0      | 刚刚被分配，还没被初始化                                     |
| _Grunnable  | 1      | 表示在runqueue上，即LRQ，还没有被执行，此时的G才能被M执行，进入Grunning状态 |
| _Grunning   | 2      | 执行中，不在runqueue上，与M、P绑定                           |
| _Gsyscall   | 3      | 在执行系统调用，没有执行go代码，没在runqueue上，只与M绑定    |
| _Gwaiting   | 4      | 被阻塞（如IO、GC、chan阻塞、锁）不在runqueue，但一定在某个地方，比如channel中，锁排队中等 |
| _Gdead      | 6      | 限制没有在使用，也许执行完，或者在free list中，或者正在被初始化，可能有stack |
| _Gcopystack | 8      | 栈正在复制，此时没有go代码，也不在runqueue上                 |
| _Gscan      | 0x1000 | 与runnable、running、syscall、waiting等状态结合，表示GC正在扫描这个G的栈 |

## P：Processor的状态

![go processor state](https://github.com/Nixum/Java-Note/raw/master/Note/picture/go_processor_state.png)

| 状态      | 描述                                                         |
| --------- | ------------------------------------------------------------ |
| _Pidle    | 处理器没有运行用户代码或者调度器，被空闲队列或者改变其状态的结构持有，运行队列为空 |
| _Prunning | 被线程 M 持有，并且正在执行用户代码或者调度器                |
| _Psyscall | 没有执行用户代码，当前线程陷入系统调用                       |
| _Pgcstop  | 被线程 M 持有，当前处理器由于垃圾回收被停止                  |
| _Pdead    | 当前处理器已经不被使用                                       |

# GC

## 基本

* 使用可达性分析判断对象是否被回收
* 三色标记法进行GC，本质是标记-清除算法，三色标记法是其改进版，主要是为了减少STW的时间
* Go 语言为了实现高性能的并发垃圾收集器，使用三色抽象、并发增量回收、混合写屏障、调步算法以及用户程序协助等机制将垃圾收集的暂停时间优化至毫秒级以下

## 三色标记

* 白色：潜在垃圾，其内存可能会被垃圾收集器回收
* 灰色：活跃对象，因为存在指向白色对象的外部指针，垃圾收集器会扫描这些对象的子对象
* 黑色：活跃对象，包括不存在任何引用外部指针对象以及从根对象可达的对象

![go gc简化过程](https://github.com/Nixum/Java-Note/raw/master/Note/picture/go_gc.gif)

1. 初始对象都是白色，首先把所有对象都放到白色集合中
2. 从根节点开始遍历对象，遍历到的对象标记为灰色，放入到灰色集合
3. 遍历灰色对象，把自己标记为黑色，放入黑色集合，将其引用的对象标记为灰色，放入灰色集合
4. 重复第3步，直到灰色集合为空，此时所有可达对象都被标记，标记阶段完成
5. 清除阶段开始，白色集合里的对象为不可达对象，即垃圾，对内存进行迭代清扫，回收白色对象
6. 重置GC状态，将所有的对象放入白色集合中

> 实际上并没有对应颜色的集合，对象被内存分配器分配在span中，span里有个gcmarkBits字段，每个bit代表一个slot被标记，白色对象该bit为0，灰色或黑色为1。
>
> 每个p中都有wbBuf和gcw gcWork, 以及全局的workbuf标记队列, 实现生产者-消费者模型, 在这些队列中的指针为灰色对象, 表示已标记, 待扫描.
>
> 从队列中取出来并把其引用对象入队的为黑色对象, 表示已标记, 已扫描. (runtime.scanobject).

## 写屏障

在标记过程中，用户程序可能会修改对象的指针，导致标记错误，对象被错误回收，因此在标记阶段需要STW，此时也无法并发或增量执行。

> 想要在并发或增量的标记算法中保证正确性，需要达成任意一种三色不变性
>
> * 强三色不变性：黑色对象不会指向白色对象，只会指向灰色对象或黑色对象
> * 弱三色不变性：黑色对象指向的白色对象必须包含一条从灰色对象经由多个白色对象的可达路径

go中使用了写屏障来保证标记的正确性。写屏障是在写入指针前执行的一小段代码，用以防止并发标记时指针丢失，这一小段代码Go是在编译时加入的。

### Dijkstra的插入写屏障

![go dijkstra插入写屏障](https://github.com/Nixum/Java-Note/raw/master/Note/picture/go_dijkstra_插入写屏障.png)

> Dijkstra写屏障是对被写入的指针进行grey操作, 不能防止指针从heap被隐藏到黑色的栈中, 需要STW重扫描栈.

### Yuasa的删除写屏障

![go yuasa删除写屏障](https://github.com/Nixum/Java-Note/raw/master/Note/picture/go_yuasa_删除写屏障.png)

> Yuasa写屏障是对将被覆盖的指针进行grey操作, 不能防止指针从栈被隐藏到黑色的heap对象中, 需要在GC开始时保存栈的快照.

## 垃圾收集过程

> 1. 清理终止阶段；
>    1. **暂停程序**，所有的处理器在这时会进入安全点（Safe point）；
>    2. 如果当前垃圾收集循环是强制触发的，我们还需要处理还未被清理的内存管理单元；
> 2. 标记阶段；
>    1. 将状态切换至 `_GCmark`、开启写屏障、用户程序协助（Mutator Assiste）并将根对象入队；
>    2. 恢复执行程序，标记进程和用于协助的用户程序会开始并发标记内存中的对象，写屏障会将被覆盖的指针和新指针都标记成灰色，而所有新创建的对象都会被直接标记成黑色；
>    3. 开始扫描根对象，包括所有 Goroutine 的栈、全局对象以及不在堆中的运行时数据结构，扫描 Goroutine 栈期间会暂停当前处理器；
>    4. 依次处理灰色队列中的对象，将对象标记成黑色并将它们指向的对象标记成灰色；
>    5. 使用分布式的终止算法检查剩余的工作，发现标记阶段完成后进入标记终止阶段；
> 3. 标记终止阶段；
>    1. **暂停程序**、将状态切换至 `_GCmarktermination` 并关闭辅助标记的用户程序；
>    2. 清理处理器上的线程缓存；
> 4. 清理阶段；
>    1. 将状态切换至 `_GCoff` 开始清理阶段，初始化清理状态并关闭写屏障；
>    2. 恢复用户程序，所有新创建的对象会标记成白色；
>    3. 后台并发清理所有的内存管理单元，当 Goroutine 申请新的内存管理单元时就会触发清理；

## GC触发时机

太难了。。。有时间再继续整理

# 参考

[Go入门指南](https://learnku.com/docs/the-way-to-go/chapter-description/3611)

[深入理解Slice底层实现](https://zhuanlan.zhihu.com/p/61121325)

[Go 语言设计与实现](https://draveness.me/golang/docs/part2-foundation/ch03-datastructure/golang-array-and-slice)

[Golang源码-Map实现原理分析](https://studygolang.com/articles/27421)

[Go map原理剖析](https://segmentfault.com/a/1190000020616487)

[深度解密Go语言之channel](https://zhuanlan.zhihu.com/p/74613114)

[GC](https://qcrao91.gitbook.io/go/gc/gc)

[图解Go协程调度原理，小白都能理解 ](https://www.cnblogs.com/secondtonone1/p/11803961.html)

[深入golang runtime的调度](https://zboya.github.io/post/go_scheduler)

[gopher meetup-深入浅出Golang Runtime-yifhao](https://www.lanzous.com/i7lj0he)

极客时间 - Go并发编程实战