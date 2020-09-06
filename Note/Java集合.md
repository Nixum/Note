[TOC]

以下笔记如没指定版本，都是基于JDK1.8

# Collection

![javaCollection类图简版](https://github.com/Nixum/Java-Note/raw/master/Note/picture/collection类图.png)

## Set

### HashSet

#### 1.基本

* 底层是HashMap，因此初始容量，默认负载因子、扩容倍数这些都和HashMap一样

* 由于HashSet只需要key，因此value统一使用静态不可变的Object对象来装，即所有key共享这一个对象

 ```java
private transient HashMap<E,Object> map;
private static final Object PRESENT = new Object();
 ```

* HashSet**允许存入null**
* 不是线程安全的
* 不保证插入元素的顺序

## List

### ArrayList

#### 1.基本

* 底层：Object数组

* 默认大小：10 （调用空参构造方法时）  

  最大是Integer.MAX_VALUE - 8（2^31 - 1，一些虚拟器需要在数组前加个头标签，所以减去 8 ）

  调用此构造方法时，

  ```java
  public ArrayList(Collection<? extends E> c)
  ```
  其中要注意的是，里面有这样一句话  
  ```java
  // c.toArray might (incorrectly) not return Object[] (see 6260652)
  if (elementData.getClass() != Object[].class)
      elementData = Arrays.copyOf(elementData, size, Object[].class);
  ```

  如果传入的是 参数c 是由 List<String> asList = Arrays.asList("aaa","bbb"); 这样来的，那么这个asList.toArray()是一个String[].class类，而不是Object[].class，原因是Arrays的asList()方法返回的是内部类Arrays&ArrayList，其toArray()方法与ArrayList的toArray()方法不一样

* 不是线程安全，因此一般在单线程中使用

* 允许存入null对象，size也会计入

* 每次扩容原来的 1.5 倍，如果扩容后仍然不够用，则采用满足够用时的数量

#### 2.扩容

因为ArrayList是底层是数组，当add操作时会使用 ensureCapacityInternal() 方法保证容量，根据size与数组的长度进行比较，如果size比数组长度大时，使用 grow() 方法进行扩容  

在 grow(int minCapacity) 方法中的 int newCapacity = oldCapacity + (oldCapacity >> 1); 新容量的大小为原来的1.5倍，之后使用这个新容量，调用 Arrays.copyOf(elementData, newCapacity) 把原数组整个复制到新数组中，可以看到，复制的时候代价是很大的

如果扩容之后的容量仍然不够，则将容量扩充至当前需要的数量

比如 List<String> list = new ArrayList<>();，此时ArrayList类里的size是0，但是Object数组长度是10

ArrayList类里：

```java
public boolean add(E e) {
    ensureCapacityInternal(size + 1);  // Increments modCount!!
    elementData[size++] = e;
    return true;
}

private void ensureCapacityInternal(int minCapacity) {
    if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
		minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity);
	}
    ensureExplicitCapacity(minCapacity);
}

private void ensureExplicitCapacity(int minCapacity) {
    // 记录数组修改(增加或删除或调整内部数组的大小)的次数
	modCount++;
	// 判断是否需要扩容
    if (minCapacity - elementData.length > 0)
		grow(minCapacity);
}

// 扩容
private void grow(int minCapacity) {
	// overflow-conscious code
	int oldCapacity = elementData.length;
	int newCapacity = oldCapacity + (oldCapacity >> 1);	// 相当于 old + (old / 2)
	if (newCapacity - minCapacity < 0)
		newCapacity = minCapacity;
	if (newCapacity - MAX_ARRAY_SIZE > 0)
		newCapacity = hugeCapacity(minCapacity);
    // minCapacity is usually close to size, so this is a win:
	elementData = Arrays.copyOf(elementData, newCapacity);
}
```

#### 3.删除

删除指定下标的值时，调用 System.arraycopy() 将 index+1 后面的元素都复制到 index 位置上，不需要遍历数组，时间复杂度为 O(N)，可以看出，删除元素的代价也是非常高的。

```java
public E remove(int index) {
    rangeCheck(index);

    modCount++;
    E oldValue = elementData(index);

    int numMoved = size - index - 1;
    if (numMoved > 0)
        System.arraycopy(elementData, index+1, elementData, index,
                         numMoved);
    elementData[--size] = null; // clear to let GC do its work

    return oldValue;
}
```

同理的remove(Object o)（但是需要遍历数组）、removeAll()都是差不多的操作

#### 4.读取和修改

修改：直接修改数组对应下标的值

读取：直接获取数组对应下标的值

注意到两个方法都有 checkForComodification(); 的判断，该方法的判断主要是提示并发修改异常

```java
public E set(int index, E e) {
    rangeCheck(index);
    checkForComodification();
    E oldValue = ArrayList.this.elementData(offset + index);
    ArrayList.this.elementData[offset + index] = e;
    return oldValue;
}

public E get(int index) {
    rangeCheck(index);
    checkForComodification();
    return ArrayList.this.elementData(offset + index);
}
```

#### 5.Fail-Fast

一种错误检查机制，因为ArrayList不是线程安全的，因此当多线程修改ArrayList对象时，或者在迭代操作中修改ArrayList对象或者序列化操作中，会进行Fail-Fast检查。

检查的原理就是使用 全局变量 modCount 来检查，modCount 用来记录 ArrayList 内数组修改(增加、删除或调整内部数组的大小)的次数，当比较前后的modCount不一致时，抛出ConcurrentModificationException异常

单线程中，如果需要在迭代中remove元素，应该使用迭代器迭代，并且使用迭代器提供的remove()（里面也是调用了ArrayList的remove方法，只是有修改modCount的值）方法，而不是ArrayList的remove方法，多线程的情况下只能使用线程安全的集合类了

#### 6.序列化

ArrayList的底层Object数组被 transient 修饰，该关键字声明数组默认不会被序列化

之后通过重写writeObject() 和 readObject() 将数组里的元素取出，进行序列化

之所以这么做是因为ArrayList的自动扩容机制，数组内元素实际数量可能会比数组长度小，如果一整个序列化的话会浪费空间，通过手动序列化的方式，只序列化实际存储的元素，而不是整个数组

类实现Serializable接口，序列化时会使用 ObjectOutputStream 的 writeObject() 将对象转换为字节流并输出。而 writeObject() 方法在传入的对象存在 writeObject() 的时候会去反射调用该对象的 writeObject() 来实现序列化。

#### 7.与Vector的区别

* Vector实现方式跟ArrayList类似，也是基于数组实现，默认大小也是10，但它是线程安全的
* Vecotr扩容是扩容到原来的2倍，ArrayList是1.5倍
* Vecotr的线程安全是因为其方法都使用了 synchronized 关键字进行修饰同步，效率比ArrayList差，这也是不推荐使用Vector的原因，保证线程安全可以使用 juc 包里其他类
* 当需要线程安全的ArrayList时，不使用Vector，而是使用 CopyOnWriteArrayList 类 或者 Collections.synchronizedList(List<T> list);

### LinkedList

#### 1.基本

* 底层是链表，且是双向链表
* 采用链表，因此插入、删除效率高(但需要知道被删除节点)，查找效率低，不支持随机查找，而ArrayList底层是数组，因此支持随机查找，查找效率高，但是插入，删除效率低
* LinkedList底层是双向链表的缘故，可以当成队列使用，创建队列Queue或者Deque时，采用LinkedList作为实现
* 不是线程安全的

### CopyOnWriteArrayList

#### 1.基本

* 线程安全的ArrayList，基本使用同ArrayList
* 底层是Object数组，但是有 volatile 修饰，还有一把可重入锁 ReentrantLock 保证线程安全
* 保证线程安全的原理是 读写分离，读的时候使用 全局变量里的Object数组，每次写的时候加锁，在方法里创建一个新数组，将全局变量里的Object数组 通过Arrays.copyOf复制 给方法里的新数组，之后进行写操作，完了再把全局变量的Object数组指向新数组。由于存在复制操作，因此add()、set()、remove()的开销很大
* 读操作不能读取实时性的数据，因为写操作的数据可能还未同步
* 适合读多写少的场景

# Map

![Map类图](https://github.com/Nixum/Java-Note/raw/master/Note/picture/Map类图.png)

### HashMap

#### 1.基本

* 底层：一个Node类型的数组，而1.7的是Entry 类型的，不过基本也差不多

  ```java
  transient Node<K,V>[] table;
  ```

  Node类是HashMap里的一个内部类

  ```java
  static class Node<K,V> implements Map.Entry<K,V> {
  	final int hash;		// key的hash码
  	final K key;
  	V value;
  	Node<K,V> next;		// 对应的链表
  }
  ```

数组中的每个位置称为一个桶，一个桶存放一条链表，同一个桶的HashCode一样

- 默认容量是16，最大容量是2^30，即数组长度

  默认的填充因子是0.75

  桶的个数，即数组的长度总是 2的n次幂

  当桶上链表的结点数大于 8 时，链表转红黑树，小于 6 时，红黑树转链表

  桶的个数，即数组长度少于 64 时，先扩容，大于64时，链表转红黑树，也就是说，它是先检查链表结点数，如果大于8，再检查数组长度，再决定是否转红黑树

* 两个变量：

  ```java
  // 临界值,衡量数组是否需要扩增的一个标准，threshold=容量x填充因子，键值对个数超过这个阈值，扩容2倍
  int threshold;
  // 填充因子,控制数组存放数据的疏密程度
  final float loadFactor;
  ```

- 每次扩容为原来的 2 倍
- 允许key=null，此时将该元素放入Node类型数组下标为0处
- 不是线程安全的，支持fast-fail机制
- 不保证插入顺序

* 基本结构：

1.8之前，数组的每一个元素中存放一条链表：

![1.8之前的HashMap](https://github.com/Nixum/Java-Note/raw/master/Note/picture/1.7HashMap.jpg)

1.8，数组中的每一个元素存放一条链表，当链表的长度超过8（默认）之后，将链表转换成红黑树，以减少搜索时间

![1.8HashMap](https://github.com/Nixum/Java-Note/raw/master/Note/picture/1.8HashMap.jpg)

#### 2.创建过程

* 构造方法：除留余数法

* 哈希冲突解决方法：拉链法

```java
Map<String, String> map = new HashMap<>();
```

首先创建一个Map，调用空参构造方法，此时只是设置了默认的填充因子，Node数组还没有初始化；除了public HashMap(Map<? extends K, ? extends V> m) 这个构造方法外，其他的构造方法基本都是只设置了填充因子和容量，并没有对数组初始化，当第一次调用put方法时，才对数组进行内存分配，完成初始化，延迟加载

其中一个可以指定容量和填充因子的构造方法中，

```java
public HashMap(int initialCapacity, float loadFactor)
```

如果传入的容量不是 2的n次幂，HashMap总会保证其数组的容量为2的n次幂，使用下面方法，原因是为了加快取模的运算速度，具体解释看下一节

```java
// 用于保证容量为2的n次，原理是求出一个数的掩码 + 1，即可得到大于该数的最小2的n次
static final int tableSizeFor(int cap) {
    int n = cap - 1;
    n |= n >>> 1;
    n |= n >>> 2;
    n |= n >>> 4;
    n |= n >>> 8;
    n |= n >>> 16;
    return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
}
```

当第一次调用put方法时，对数组(桶)进行判断，如果为空，则为数组分配内存，完成初始化，之后再根据key的情况，加入到HashMap中

```java
map.put("key111", "value111");
```

对put方法的解析具体请看[集合框架源码学习之HashMap(JDK1.8)](https://juejin.im/post/5ab0568b5188255580020e56 "")

#### 3.确定键值对所在的桶的下标

当向map中put进 key-value 时，对key采用哈希 + 除留余数法，确定该键值对所在的桶的下标

首先计算key的hash值，之后对数组的容量取模，得到的余数就是桶的下标

JDK1.7

```java
final int hash(Object k) {
    int h = hashSeed;		// hashSeed的值受容量和resize方法而定
    if (0 != h && k instanceof String) {
        return sun.misc.Hashing.stringHash32((String) k);
    }
    h ^= k.hashCode();

    h ^= (h >>> 20) ^ (h >>> 12);
    return h ^ (h >>> 7) ^ (h >>> 4);
}

static int indexFor(int h, int length) {
    // assert Integer.bitCount(length) == 1 : "length must be a non-zero power of 2";
    return h & (length-1);
}
```
但是这里并不直接采用取模%操作， 而是使用了位运算，因为位运算的代价比求模运算小的多，因此在进行这种计算时用位运算的话能带来更高的性能

而取模(%)操作中如果除数是2的幂次则等价于与其除数减一的与(&)操作，这也解释了为什么容量每次都是2的n次幂

在JDK1.8中，此操作被简化了，也取消了indexFor(int h, int length)方法，确定桶下标直接 hash & (length-1)

```java
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);	// ^ 按位异或
}
```

当计算得到该键值对所在桶的下标后，判断该桶是否为空，如果为空，将此键值对插入到桶中

当容量未达到阈值，且容量未变，此时出现新键值对，其key计算得到的桶的下标所在位置已经有结点了，则通过拉链法来解决冲突

先判断新的键值对的key的hash值与头结点的key的hash值是否一致，且key是否相等，如果这两项均满足，则说明该新的key与头结点的一样，将value覆盖掉原来的结点，如果不相等，则

在JDK1.7中，使用头插法加入链表，在多线程中，头插法可能会导致hashMap扩容时造成死循环，因此在1.8修复这个问题了

在JDK1.8中，使用尾插法加入链表，当然还有一系列判断是否需要将链表转换为红黑树，具体条件看上面已说明

#### 4.扩容

当加入到HashMap中的元素越来越多时，碰撞的概率也越来越高，链表的长度变长，查找效率降低，此时就需要对数组进行扩容。HashMap根据键值对的数量，来调整数组长度，保证查找效率

在两种情况下，HashMap会进行扩容

* 当put操作时，数组大小超过阈值 threshold（容量 x 负载因子），JDK1.7、1.8都是

```java
final Node<K,V>[] resize()；
```

扩容时，将扩容到原来的2倍（newThr = oldThr << 1），之后进行一次重新的hash分配。

创建新数组，遍历整个旧的hashMap，重新计算每个键值对的hash值以及对应的桶下标，**扩展后Node对象的位置要么在原位置，要么移动到原偏移量两倍的位置**，之后将旧hashMap中的结点加入到新的数组中

HashMap 使用了一个特殊的机制，降低重新计算桶下标的操作。

假设原数组长度 capacity 为 16，扩容之后 new capacity 为 32：

```java
capacity     : 00010000
new capacity : 00100000
```

对于一个 Key，

- 它的哈希值如果在第 5 位上为 0，那么取模得到的结果和之前一样；
- 如果为 1，那么得到的结果为原来的结果 +16。

#### 5.与HashTable的区别

* HashMap不是线程安全的，HashTable是线程安全的(因为其方法使用了synchronized修饰来保证同步，效率较差)
* HashMap允许插入key为null的键值对，而HashTable不允许
* 如果不指定容量，HashMap默认容量是16，每次扩容为原来的 2 倍；HashTable默认容量是11，每次扩容为原来的 2n+1 倍
* jdk1.8后对HashMap链表的改变，HashTable没有

### ConcurrentHashMap

#### 1.基本

* 线程安全的HashMap，使用方法同HashMap
* 不允许存入key为null的键值对

#### 2.线程安全的底层原理，JDK1.7和JDK1.8的实现不一样

- JDK1.7

  - JDK1.7 采用分段锁（segment）机制，每个分段锁维护几个桶，多个线程可以同时访问不同分段锁上的桶，并发度指segment的个数

  - 默认的分段锁是16，segment的数量一经指定就不会再扩了；每个segment里面的HashEntry数组的最小容量是2，每次扩容 2 倍

  - 也是延迟初始化，当第一次put的时候，执行第一次hash取模定位segment的位置，如果segment没有初始化，因为put可能出现并发操作，则通过CAS赋值初始化，之后执行第二次hash取模定位HashEntry数组的位置，通过继承 ReentrantLock的tryLock() 方法尝试去获取锁，如果获取成功就直接插入相应的位置，如果已经有线程获取该segment的锁，那当前线程会以自旋的方式去继续的调用 tryLock() 方法去获取锁，超过指定次数就挂起，等待唤醒
  - size（统计键值对数量）操作：因为存在并发的缘故，size的可能随时会变，ConcurrentHashMap采用的做法是先采用不加锁的模式，尝试计算size的大小，比较前后两次计算的结果，结果一致就认为当前没有元素加入，计算的结果是准确的，尝试次数是 3 次，如果超过了 3 次，则会对segment加锁，锁住对segment的操作，之后再统计个数

![1.7concurrentHashMap](https://github.com/Nixum/Java-Note/raw/master/Note/picture/1.7concurrentHashMap.jpg)

- JDK1.8 
  - 采用CAS和synchronized来保证并发安全，更接近HashMap
  - synchronized只锁定当前链表或红黑二叉树的首节点，这样只要hash不冲突，就不会产生并发
  - 扩容的时候由于只有一个table数组，将在多线程下各个线程都会帮忙扩容，加快扩容速度
  - size操作：在扩容和addCount()方法就进行处理，而不像1.7那样要等调用的时候才计算

**红黑树的特性**:
（1）每个节点要么是黑色，要么是红色。
（2）根节点是黑色。
（3）每个叶子节点（NIL）是黑色。（注意：这里叶子节点，是指为空(NIL或NULL)的叶子节点）
（4）如果一个节点是红色的，则它的子节点必须是黑色的。
（5）从一个节点到该节点的子孙节点的所有路径上包含相同数目的黑节点。

是一种二叉平衡树，但是平衡不是非常严格，因此结点的旋转次数少，插入、删除效率高，插入、删除、搜索都是时间复杂度都是O(log2 n)

插入和删除时，先理解为二叉搜索树的插入和删除，然后再进行变色，优先变色，变色后仍无法达到上面那五个特性才开始旋转，来达到上面的特性，插入时节点一般是红色

参考：[红黑树插入和删除](https://baijiahao.baidu.com/s?id=1663270991795039269&wfr=spider&for=pc)

# 使用Stream处理集合

[Java 8 中的 Streams API 详解](https://www.ibm.com/developerworks/cn/java/j-lo-java8streamapi/index.html)

# 参考

[Java：集合，Collection接口框架图](https://www.cnblogs.com/nayitian/p/3266090.html "")

[CyC2018/CS-Notes/java容器.md](https://github.com/CyC2018/CS-Notes/blob/master/docs/notes/Java%20%E5%AE%B9%E5%99%A8.md "")

[c.toArray might not return Object[]?](https://www.cnblogs.com/liqing-weikeyuan/p/7922306.html "")

[Java集合---ArrayList的实现原理](https://www.cnblogs.com/ITtangtang/p/3948555.html "")

[集合框架源码学习之HashMap(JDK1.8)](https://juejin.im/post/5ab0568b5188255580020e56 "")

[JDK1.8 HashMap源码分析](https://www.cnblogs.com/xiaoxi/p/7233201.html "")

[【JUC】JDK1.8源码分析之CopyOnWriteArrayList（六）](https://www.cnblogs.com/leesf456/p/5547853.html "")

[红黑树(一)之 原理和算法详细介绍](http://www.cnblogs.com/skywang12345/p/3624343.html "")