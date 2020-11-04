[TOC]

# 数组

* 声明时必须指定固定长度，因为编译时需要知道数组长度以便分配内存，如`var arr1 [5]int`，或者`var arr2 = [5]int{1,2,3}, 其余数字为0`
* 数组长度最大是2Gb
* 当数组类型是整形时，所有元素都会被自动初始化为0，即声明完数组，数组会被设置类型的默认值
* 可以使用new()来创建，如`var arr3 = new([5]int)`，arr3的类型是`*[5]int`，arr1、arr2的类型是`[5]int`
* 如果函数的参数可以是[5]int, 表明入参是数组，如果是[]int，表明入参是slice。类型[3]int和[5]int是两种不同的类型。
* **数组是值类型**，赋值和传参会进行拷贝，函数内部的修改不会影响原始数组

## 切片Slice

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

### 扩容

扩容实际上包括两部分：计算容量的规则 和 内存对齐

1. 如果期望容量大于当前容量的两倍就会使用期望容量；期望容量指的是把元素加进去后的容量。

```go
// 此时期望容量是2 + 3 = 5 > 旧容量的两倍 2 * 2 = 4，至于最终的cap是6，是因为经过内存对齐后，容量发生了改变
s1 := []int{1, 2}
s1 = append(s1, 4, 5, 6)
fmt.Printf("len=%d, cap=%d\n",len(s1),cap(s1))  // len=5, cap=6
```

2. 如果当前切片的长度小于 1024，扩容两倍；

```go
// 第一次append，扩容，拷贝旧数据到新数组；第二次append，没有产生新数组，只将元素进行追加；第三次append，扩容，拷贝旧数据到新数组
s := []int{1, 2}
s = append(s, 4)
fmt.Printf("len=%d, cap=%d\n",len(s),cap(s))  // len=3, cap=4
s = append(s, 5)
fmt.Printf("len=%d, cap=%d\n",len(s),cap(s))  // len=4, cap=4
s = append(s, 6)
fmt.Printf("len=%d, cap=%d\n",len(s),cap(s))  // len=5, cap=8
```

3. 如果当前切片的长度大于 1024 就会每次增加 25% 的容量，即扩容1.25倍，直到新容量大于期望容量；

4. 进行扩容的容量计算后，还会进行内存对齐，这个操作之后，扩容后的容量可能会 > 原容量的两倍 或 1.25倍

# Map



# Channel



# Goroutine



# GC



# 参考

[Go入门指南](https://learnku.com/docs/the-way-to-go/chapter-description/3611)

[深入理解Slice底层实现](https://zhuanlan.zhihu.com/p/61121325)
