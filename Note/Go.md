[TOC]

# 数组

* 声明时必须指定固定长度，因为编译时需要知道数组长度以便分配内存，如`var arr1 [5]int`，或者`var arr2 = [5]int{1,2,3}, 其余数字为0`
* 数组长度最大是2Gb
* 当数组类型是整形时，所有元素都会被自动初始化为0，即声明完数组，数组会被设置类型的默认值
* 可以使用new()来创建，如`var arr3 = new([5]int)`，arr3的类型是`*[5]int`，arr1、arr2的类型是`[5]int`
* **数组是值类型**，赋值和传参会进行拷贝，方法内部的修改不会影响原始数组

## 切片Slice

## 数据结构

```go
type Slice struct {
	ptr   unsafe.Pointer 	// 指向数组的指针
	len   int               // 切片长度
	cap   int               // 切片容量
}
```

## 基本

* 声明时无需指定长度，如 `slice1 := []int{1,2,3}`、从数组上切下来`arr1 := [5]int; var slice2 []int = arr1[1:3], 此时对slice2的修改会影响arr1`。
* 空切片：`slice := make([]int, 0)`，nil切片：`var slice []int 或 slice := []int{}`；两者的区别在于，空切片会指向一个内存地址，但它没有分配任何的内存空间；nil切片是直接指向nil。
* 切片是对数组的一个连续片段的引用，所以**切片是引用类型**，作为方法参数时，传递指针，方法内部的修改会影响原始数组
* 可以使用make([]type, len, cap)来创建，len必填，cap非必填。如`slice4 := new(int[], 5, 10)，长度5，容量10`。如果cap不填，初始cap=len。
* 可以使用new来创建，比如 `new([100]int)[0:50]` 效果等同于 `make([]int, 50, 100)`

使用new()和make()的区别

> 看起来二者没有什么区别，都在堆上分配内存，但是它们的行为不同，适用于不同的类型。
>
> new (T) 为每个新的类型 T 分配一片内存，初始化为 0 并且返回类型为 * T 的内存地址：这种方法 返回一个指向类型为 T，值为 0 的地址的指针，它**适用于值类型如数组和结构体**；它相当于 &T{}。
> make(T) 返回一个类型为 T 的初始值，它只**适用于 3 种内建的引用类型：切片、map 和 channel**。

## 扩容

### 原理

append()方法会返回一个新的slice，不会修改传入的slice，当使用append()向slice追加元素时：

1. 如果slice容量够用，则直接把新元素追加进去，长度 + 1，返回原slice
2. 原slice容量不够，将slice扩容，得到新的slice
3. 将新元素追加到新slice，长度 + 1，返回新slice

另外，copy方法拷贝两个slice时，会将源slice拷贝到目标slice，如果目标slice的长度<源slice，不会发送扩容。

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

* 如果期望容量大于当前容量的两倍就会使用期望容量；

* 如果当前切片的长度小于 1024 就会将容量翻倍；

* 如果当前切片的长度大于 1024 就会每次增加 25% 的容量，直到新容量大于期望容量；

但是，当切片长度

### 源码

```

```



# Map



# Channel



# Goroutine



# GC



# 参考

[Go入门指南](https://learnku.com/docs/the-way-to-go/chapter-description/3611)

[深入理解Slice底层实现](https://zhuanlan.zhihu.com/p/61121325)