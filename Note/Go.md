[TOC]

# 数组

* 声明时必须指定固定长度，因为编译时需要知道数组长度以便分配内存，如`var arr1 [5]int`，或者`var arr2 = [5]int{1,2,3}, 其余数字为0`
* 数组长度最大是2Gb
* 当数组类型是整形时，所有元素都会被自动初始化为0，即声明完数组，数组会被设置类型的默认值
* 可以使用new()来创建，如`var arr3 = new([5]int)`，arr3的类型是`*[5]int`，arr1、arr2的类型是`[5]int`
* 如果函数的参数可以是[5]int, 表明入参是数组，如果是[]int，表明入参是slice。类型[3]int和[5]int是两种不同的类型。
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
* 切片是对数组的一个连续片段的引用，所以**切片是引用类型**，作为函数参数时，传递指针，函数内部的修改会影响原始数组
* 一个数组可以创建多个slice，一个slice也可以创建多个slice，但是新老slice会共用底层数组，新老slice的修改都会互相影响。但是如果新slice经过append，使得slice底层数组扩容了，此时slice引用了新的数组，此时新老slice就不会互相影响了。

使用new()和make()的区别

> 看起来二者没有什么区别，都在堆上分配内存，但是它们的行为不同，适用于不同的类型。
>
> new (T) 为每个新的类型 T 分配一片内存，初始化为 0 并且返回类型为 * T 的内存地址：这种函数 返回一个指向类型为 T，值为 0 的地址的指针，它**适用于值类型如数组和结构体**；它相当于 &T{}。
> make(T) 返回一个类型为 T 的初始值，它只**适用于 3 种内建的引用类型：切片、map 和 channel**。

* range遍历的注意点：使用range遍历时，底层使用range遍历时，slice会发生一次拷贝，即range指向的slice是原始slice的拷贝；将slice的每个元素赋值给v时，也发生了一次拷贝。

demo
```
	arr := []int{1, 2, 3}
	for _, v := range arr {
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

另外，copy函数拷贝两个slice时，会将源slice拷贝到目标slice，如果目标slice的长度<源slice，不会发送扩容。

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

// 第一次append，扩容，拷贝旧数据到新数组；第二次append，没有产生新数组，只将元素进行追加；第三次append，扩容，拷贝旧数据到新数组
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
	oldbuckets unsafe.Pointer // 扩容时用于保存之前buckets的字段，大小是当前buckets的一半
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

* key不允许为slice、map、func，允许bool、numeric、string、指针、channel、interface、struct

* map的容量为 装载因子6.5 * 2^B 个元素，装载因子 = 哈希表中的元素 / 哈希表总长度，装载因子越大，冲突越多。

* 拉链法解决哈希冲突，除留余数法得到桶的位置。

* key的哈希值的低B位计算获得桶的位置，高8位计算得到tophash的位置，进而找到key的位置。

* 溢出桶也是一个bmap，bmap的overflow会指向下一个溢出桶，所以**溢出桶的结构是链表，但是它们跟正常桶是在一片连续内存上，即buckets数组**。

* 每个桶存了8个tophash + 8对键值对。

* map是非线程安全的，扩容不是一个原子操作。

* map的遍历是无序的，每次遍历出来的结果的顺序都不一样。

## 创建初始化

最终会在运行时调用makemap函数进行创建和初始化，
1. 计算哈希表占用的内存是否溢出或者超出能分配的最大值

2. 调用fastrand()获取随机哈希种子，计算B的值，用于初始化桶的数量 = 2^B

3. 调用makeBucketArray()分配连续的空间，创建用于保存桶的数组
   
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
7. 判断是否扩容，如果需要扩容，则扩容后继续从步骤5上继续，如果不用扩容则走步骤8
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

## 数据结构

```go
type hchan struct {
	qcount   uint   // channel中的元素个数
	dataqsiz uint   // channel中的循环数组的长度
	buf      unsafe.Pointer // channel中缓冲区数据指针，buf是一个循环数组
	elemsize uint16 // 当前channel能够收发的元素大小
	closed   uint32
	elemtype *_type // 当前channel能够收发的元素类型
	sendx    uint   // 指向底层循环数组buf，表示当前可发送的元素位置的索引值，当sendx=dataqsiz时，会回到buf数组的起点
	recvx    uint   // 指向底层循环数组buf，表示当前可接收的元素位置的索引值
	recvq    waitq  // 存储当前channel由于缓冲区空间不足而接收阻塞的goroutine列表，双向链表
	sendq    waitq  // 存储当前channel由于缓冲区空间不足而发送阻塞的goroutine列表，双向链表

	lock mutex  // 保证每个读channel或写channel的操作都是原子的
}

type waitq struct {
	first *sudog
	last  *sudog
}
```

## 基本

* chan是引用类型，使用make关键字创建，如

  `ch := make(chan, string, 10)`，创建一个能处理string的缓冲区大小为10的channel，效果相当于异步队列，除非缓冲区用完，否则不会阻塞；
  
  `ch := make(chan, string)`，则创建了一个不存在缓冲区的channel，效果相当于同步阻塞队列，即如果连续发送两次数据，第一次如果没有被接收的话，第二次就会被阻塞。
  
* channel作为通道，负责在多个goroutine间传递数据，解决多线程下共享数据竞争问题。

## 发送数据

使用`ch <- "test"`发送数据，最终会调用chansend函数发送数据。

1. 如果是阻塞的，chansend在发送数据前会为当前channel加锁。
2. 当存在等待的接收者时，通过send函数，从接收队列recvq中取出最先进入等待的goroutine，直接发送数据。
3. 当缓冲区存在空余空间时，会使用chanbuf计算出下一个可以存储数据的位置，将要发送的数据拷贝到缓冲区并增加sendx索引和qcount计数器，将发送的数据写入channel缓冲区。
4. 当不存在缓冲区或者缓冲区已满，会先调用getg函数获取正在发送数据的goroutine，执行acquireSudog函数创建sudog对象，设置此次阻塞发送的相关信息（如发送的channel、是否在select控制结构中和待发送数据的内存地址、发送数据的goroutine），将该sudog对象加入sendq队列，调用goparkunlock函数让当前goroutine进入等待，表示当前goroutine正在等待其他goroutine从channel中接收数据，等待调度器唤醒。调度器唤醒后，将一些属性值设置为零，并释放sudog对象，表示向channel发送数据结束。

## 接收数据

使用` str <- ch 或 str, ok <- ch ok用于判断ch是否关闭，如果没有ok，可能会无法分配str接收到的零值是发送者发的还是ch关闭`接收数据，最终会调研chanrecv函数接收数据。

1. 当从一个空channel中接收数据时会调研gopark函数，此时当前goroutine进入等待。
2. 如果当前channel已经被关闭且缓冲区不存在任何数据，此时会清除ep指针中的数据并立即返回。
3. 当channel的sendq队列存在等待状态的goroutine时，使用recv函数直接从阻塞的发送者或缓冲区中获取数据。
4. 当缓冲区存在数据时，从channel的缓冲区中的recvx的索引位置接收数据，如果接收数据的内存地址不为空，会直接将缓冲区里的数据拷贝到内存中，清除队列中的数据，递增recvx，递减qcount，完成数据接收。当发送recvx超过channel的buf时，会将其归零。
5. 当缓冲区不存在数据且channel的sendq不存在等待的goroutine时，创建sudog对象，加入recvq队列，当前goroutine进入阻塞状态，等待其他goroutine向channel发送数据。

# Runtime

* 不同于Java，Go没有虚拟机，很多东西比如自动GC、对操作系统和CPU相关操作都变成了函数，写在runtime包里。
* Runtime提供了go代码运行时所需要的基础设施，如协程调度、内存管理、GC、map、channel、string等内置类型的实现、对操作系统和CPU相关操作进行封装。
* 诸如go、new、make、->、<-等关键字都被编译器编译成runtime包里的函数
* build成可执行文件时，Runtime会和用户代码一起进行打包。

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