[TOC]

# 一、面向对象

面向对象的特征：
抽象(注意与当前目标有关的，选择一部分，暂时不用部分细节，分为过程抽象、数据抽象)
**继承**：联结类的层次模型、允许和鼓励类的重用，派生类可以从它的基类那里继承方法和实例变量，进行修改和新增使其更适合
**封装**：封装是把过程和数据包围起来，对数据的访问只能通过已定义的界面，这些对象通过一个受保护的接口访问其他对象
**多态**：允许不同类的对象对同一消息作出响应，包括参数化多态性和包含多态性，灵活、抽象、行为共享、代码共享，解决程序函数同名问题

# 二、基础类型及其包装类型

| 基本类型 | boolean | byte | char      | short | int     | float | long | double |
| -------- | :-----: | ---- | --------- | ----- | ------- | ----- | ---- | ------ |
| 包装类型 | Boolean | Byte | Character | Short | Integer | Float | Long | Double |
| 位数     |    1    | 8    | 16        | 16    | 32      | 32    | 64   | 64     |
| 字节数   |         | 1    | 2         | 2     | 4       | 4     | 8    | 8      |

## 装箱和拆箱

以 int 和 Integer 为例

* 装箱的时候自动调用的是 Integer 的 valueOf(int) 方法，Integer a = 1; 就会触发调用valueOf(int)方法

  拆箱的时候自动调用的是Integer的 intValue() 方法

其中，装箱时，即调用 valueOf() 方法时，会先去缓存池里找，看看该值是否在缓存池的范围中，如果是，多次调用时会取得同一对象的引用，属于同一对象，如果不在缓存池中，则使用new Integer()

使用new Integer()初始化的，无论传入的数是否在缓存池的范围内，都是重新分配内存初始化的，属于不同对象

```java
Integer a = 23; Integer b = 23;
System.out.println(a == b); // true

Integer a1 = new Integer(23); Integer b1 = new Integer(23);
System.out.println(a1 == b1); // false

Integer c = 128; Integer d = 128; 
System.out.println(c == d); // false
```

* 基本类型缓存池，其他类型没有

  boolean：true、false

  short ： -128 ~127

  int ： -128 ~127

  char ： \u0000 ~ \u007F

* equals 和 == 对于 装箱和拆箱

  - == ：比较的是两个包装类型的，比较两者的引用，判断是否指向同一对象

    比较时一个是包装类型，一个是基本类型，则将自动拆箱，比较基本类型，无论值得范围是否在缓存池内

    比较时有算术运算，自动拆箱，运算，比较数值

    比较时，两者都是不同的包装类型，自动拆箱，比较数值

  - equals：看具体类型的equals方法，一般先比较类型，类型不一样直接返回false，类型一样再比较数值

    比较时传入基本类型，会进行装箱，之后进行equals比较

    ​比较时有算术运算，自动拆箱，运算，之后根据运算完后的类型再装箱（可能会向上转型），之后再进行equals比较


## 运算与转型

* 从低位类型到高位类型自动转换；从高位类型到低位类型需要强制类型转换
* 算术运算中，基本就是先转换为高位数据类型，再参加运算，结果也是最高位的数据类型
* short、byte、char计算时都会提升为int
* 采用 +=、*= 等缩略形式的运算符，系统会自动强制将运算结果转换为目标变量的类型
*  当运算符为自动递增运算符（++）或自动递减运算符（--）时，如果操作数为 byte，short 或 char类型不发生改变

```java
byte a = 127, b = 127; 
a += b; a -= b; //这种写法是可以的，会变成 a = (byte) (a+b) 
而 a = a + b； a = a - b；则会因为没有类型转化而出错，int无法转成byte
如果是final修饰 a、b，那么byte c = a + b 就不会出先编译错
```

# 三、String

## 1.基本

* 底层：private final char value[]; 

  value数组被final修饰，因此当它初始化之后就不能再引用其它数组

  String 内部没有改变 value 数组的方法，因此可以保证 String 不可变

  String类的方法都不是在原来的字符串上进行操作，而是重新生成新的字符数组

* 类本身被 final 修饰，使它不可被继承

* String类的 “+"  本质是使用StringBuffer的append方法，最终返回new的string

## 2.不可变的好处

* 可以缓存 hash 值

  因为 String 的 hash 值经常被使用，例如 String 用做 HashMap 的 key。不可变的特性可以使得 hash 值也不可变，因此只需要进行一次计算。

* String Pool 的需要

  如果一个 String 对象已经被创建过了，那么就会从 String Pool 中取得引用。只有 String 是不可变的，才可能使用 String Pool。

* 安全

  * String 经常作为参数，String 不可变性可以保证参数不可变。例如在作为网络连接参数的情况下如果 String 是可变的，那么在网络连接过程中，String 被改变，改变 String 对象的那一方以为现在连接的是其它主机，而实际情况却不一定是。

  * 不可变得特性使得它天生是线程安全的

## 3.字符串常量池（String Pool）

## 4.StringBuffer和StringBuilder

3和4具体参考[深入理解Java中的String](https://www.cnblogs.com/xiaoxi/p/6036701.html "")，这篇文章写得相当详细了

# 四、equals()、hashCode()、clone()



# 继承



# 反射



# 代理



## jdk动态代理

## cglib



# 泛型



# 注解



# 参考

[深入剖析Java中的装箱和拆箱](https://www.cnblogs.com/dolphin0520/p/3780005.html "")
[CyC2018/CS-Notes/java基础.md](https://github.com/CyC2018/CS-Notes/blob/master/docs/notes/Java%20%E5%9F%BA%E7%A1%80.md "")
[深入理解Java中的String](https://www.cnblogs.com/xiaoxi/p/6036701.html "")