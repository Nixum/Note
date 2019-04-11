- [一、面向对象](#%E4%B8%80%E9%9D%A2%E5%90%91%E5%AF%B9%E8%B1%A1)
- [二、基础类型及其包装类型](#%E4%BA%8C%E5%9F%BA%E7%A1%80%E7%B1%BB%E5%9E%8B%E5%8F%8A%E5%85%B6%E5%8C%85%E8%A3%85%E7%B1%BB%E5%9E%8B)
  - [装箱和拆箱](#%E8%A3%85%E7%AE%B1%E5%92%8C%E6%8B%86%E7%AE%B1)
  - [运算与转型](#%E8%BF%90%E7%AE%97%E4%B8%8E%E8%BD%AC%E5%9E%8B)
- [三、String](#%E4%B8%89string)
  - [1.基本](#1%E5%9F%BA%E6%9C%AC)
  - [2.不可变的好处](#2%E4%B8%8D%E5%8F%AF%E5%8F%98%E7%9A%84%E5%A5%BD%E5%A4%84)
  - [3.字符串常量池（String Pool）](#3%E5%AD%97%E7%AC%A6%E4%B8%B2%E5%B8%B8%E9%87%8F%E6%B1%A0string-pool)
  - [4.StringBuffer和StringBuilder](#4stringbuffer%E5%92%8Cstringbuilder)
- [四、equals()、hashCode()、clone()](#%E5%9B%9Bequalshashcodeclone)
  - [equals()](#equals)
  - [hashCode()](#hashcode)
  - [clone()](#clone)
- [关键字](#%E5%85%B3%E9%94%AE%E5%AD%97)
  - [final和static](#final%E5%92%8Cstatic)
  - [instanceof](#instanceof)
- [抽象类和接口](#%E6%8A%BD%E8%B1%A1%E7%B1%BB%E5%92%8C%E6%8E%A5%E5%8F%A3)
- [继承](#%E7%BB%A7%E6%89%BF)
  - [重写与重载](#%E9%87%8D%E5%86%99%E4%B8%8E%E9%87%8D%E8%BD%BD)
  - [初始化顺序](#%E5%88%9D%E5%A7%8B%E5%8C%96%E9%A1%BA%E5%BA%8F)
- [内部类](#%E5%86%85%E9%83%A8%E7%B1%BB)
  - [基础](#%E5%9F%BA%E7%A1%80)
  - [成员内部类](#%E6%88%90%E5%91%98%E5%86%85%E9%83%A8%E7%B1%BB)
  - [局部内部类](#%E5%B1%80%E9%83%A8%E5%86%85%E9%83%A8%E7%B1%BB)
  - [静态内部类](#%E9%9D%99%E6%80%81%E5%86%85%E9%83%A8%E7%B1%BB)
  - [匿名内部类](#%E5%8C%BF%E5%90%8D%E5%86%85%E9%83%A8%E7%B1%BB)
- [反射和内省](#%E5%8F%8D%E5%B0%84%E5%92%8C%E5%86%85%E7%9C%81)
- [枚举](#%E6%9E%9A%E4%B8%BE)
- [泛型](#%E6%B3%9B%E5%9E%8B)
- [注解](#%E6%B3%A8%E8%A7%A3)
- [参考](#%E5%8F%82%E8%80%83)

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

* 字符集

  unicode是字符集，一种标准，UTF-8、UTF-16、GBK之类的是编码方式，是字符集的具体实现

  UTF-16：定长,固定2字节， UTF-8：变长,中文占3字节,英文占1字节

  char可以保存一个中文字符

  java中采用unicode编码，无论中文、英文都是占2个字节

  java虚拟机中使用UTF-16编码方式

  java的字节码文件(.class)文件采用的是UTF-8编码，但是在java 运行时会使用UTF-16编码。

  参考[Java中的UTF-8、UTF-16编码字符所占字节数](https://blog.csdn.net/worm0527/article/details/70833531)

* 自动转换的顺序，

  由高到低：btye, short, char(这三个之间无法自动转换，只能强转) ---> int ---> long ---> float ---> double

* 为什么 占8字节的long 转占 4字节float 不需要强转转化？

  因为底层实现方式不同，浮点数在内存中的32位不是简单地转化为十进制，而是通过公式计算得到，最大值要比long的范围大

## 装箱和拆箱

以 int 和 Integer 为例

* 装箱的时候自动调用的是 Integer 的 valueOf(int) 方法，Integer a = 1; 就会触发调用valueOf(int)方法

  拆箱的时候自动调用的是Integer的 intValue() 方法

  parseInt(“”)方法是将字符串转化为基本类型

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

  - == ：比较的是两个包装类型的，同包装类型，比较两者的引用，判断是否指向同一对象；不同包装类型用==比较会出现编译错误

    比较时两者都是基本类型，无论类型是否一样，都是比较数值

    比较时一个是包装类型，一个是基本类型，无论值的范围是否在缓存池内，则将自动拆箱，比较基本类型

    比较时有算术运算，自动拆箱，运算，比较数值

  - equals：看具体类型的equals方法，一般先比较类型，类型不一样直接返回false，类型一样再比较数值

    比较时传入基本类型，会进行装箱，之后进行equals比较

    比较时有算术运算，自动拆箱，运算，之后根据运算完后的类型再装箱（可能会向上转型），之后再进行equals比较


## 运算与转型

* 从低位类型到高位类型自动转换；从高位类型到低位类型需要强制类型转换
* 算术运算中，基本就是先转换为高位数据类型，再参加运算，结果也是最高位的数据类型
* short、byte、char计算时都会提升为int
* 采用 +=、*= 等缩略形式的运算符，系统会自动强制将运算结果转换为目标变量的类型
* 当运算符为自动递增运算符（++）或自动递减运算符（--）时，如果操作数为 byte，short 或 char类型不发生改变
* 被final修饰的变量不会自动改变类型，当2个final修饰相操作时，结果会根据左边变量的类型自动转化

```java
byte a = 127, b = 127, d; 
final byte c = 127;
a += b; a -= b; //这种写法是可以的，会变成 a = (byte) (a+b) 
而 a = a + b； a = a - b；则会因为没有类型转化而出错，int无法转成byte
d = a + c; // 也会出错，a在运算时为自动提升为int
如果是final修饰 a、b，那么byte c = a + b 就不会编译错误
```

# 三、String

## 1.基本

* 底层：private final char value[]; 

  value数组被final修饰，因此当它初始化之后就不能再引用其它数组

  String 内部没有改变 value 数组的方法，因此可以保证 String 不可变

  String类的方法都不是在原来的字符串上进行操作，而是重新生成新的字符数组

* 类本身被 final 修饰，使它不可被继承

* String类的 “+"  本质是使用StringBuilder的append方法，最终返回new的string

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

## equals()

* 在不重写的情况下，Object类下的 equals() 方法比较的是两个对象的引用，即判断两个对象是否是同一个对象，此时等价于 ==
* 重写的情况下，看具体类重写后的equals方法，像String类的 equals 方法是先判断是否是String类型，再比较字符的内容

## hashCode()

* 作用是返回对象的int类型的哈希码，一般用于当索引，例如，在HashMap里，加入一对键值对，HashMap会先计算key的哈希值，取模，找到对应桶的下标

* hashCode()一般会和equals()有联系，例如，HashMap在找到要加入的键值对所在对应的桶，桶内的键值对的哈希码肯定是一样，为了判断该键值对是否重复出现，将使用key的equals进行比较

  为什么有了equals()还要hashCode方法，有了hashCode方法还要equals方法？

  1. 因为equals方法的实现比较复杂，效率较低，hashCode只需计算hash值就能进行对比，效率高
  2. hashCode方法不一定可靠，不同对象生成的hashCode可能一样，所以需要equals方法

  hashCode() 与 equals() 的相关规定

  1. 如果两个对象相等，则hashcode一定也是相同的
  2. 两个对象相等,对两个对象分别调用equals方法都返回true
  3. 两个对象有相同的hashcode值，它们也不一定是相等的
  4. 因此，equals 方法被覆盖过，则 hashCode 方法也必须被覆盖
  5. hashCode() 的默认行为是对堆上的对象产生独特值。如果没有重写 hashCode()，则该 class 的两个对象无论如何都不会相等（即使这两个对象指向相同的数据）

## clone()

* clone分为深拷贝和浅拷贝

  * 浅拷贝：对象的属性是基本类型的，直接复制一份；属性是引用类型的（如数组、对象），复制引用，也就是拷贝过后的两个对象里引用类型的属性，都是指向同一个对象

    Object类里的clone方法就是浅拷贝

  * 深拷贝：对象里的属性无论是基本类型还是引用类型，都是重新复制一份，即会为拷贝对象里的引用属性重新开辟内存空间，不再是和被拷贝对象指向同一个对象

* Object里的clone()方法被protected 修饰，一般类如果不重写的话是调用不了的，如果要重写的话，需要实现Cloneable接口，不然会抛出CloneNotSupportedException异常，之后可以通过super.clone()调用Object类中的原clone方法

# 关键字

## final和static

* final关键字主要用在三个地方：变量、方法、类。

1. 修饰变量，如果是基本数据类型的变量，则其数值一旦在初始化之后便不能更改；如果是引用类型的变量，则在对其初始化之后便不能再让其指向另一个对象，final修饰的变量一定要初始化，要不就直接初始化，不然就需要在每个构造方法里初始化，或者在代码块里初始化，代码块里的初始化和构造方法里的初始化只能二选一，否则会造成赋值重复
2. 修饰类，表明这个类不能被继承，final类中的所有成员方法都会被隐式地指定为final方法
3. 修饰方法，表明该方法不能被子类重写

* 声明为static成员不能被序列化

## instanceof

* 是关键字，也是运算符
* 用于判断一个对象是否是 一个类的实例，一个类的子类，一个接口的实现类

# 抽象类和接口

* 抽象类可以有构造方法（但不能实例化），接口没有

* 抽象类可以有普通成员变量、静态变量或常量，访问类型任意，接口只有静态常量，且默认被public static final修饰

* 抽象类可以包含非抽象的普通方法，接口中的方法必须是抽象的，不能有非抽象方法

* 抽象类中的**抽象方法访问类型**可以是**public、protected和不写**，接口中的方法默认是public abstract

* 抽象类中可以包含静态方法(可以调用)，接口不行

* JDK1.8之前接口中的方法不能有方法体，且不能被default修饰，但是可以不写访问修饰符，

  1.8之后（包含1.8）接口中的方法可以有方法体，表示默认方法体，但需要用default修饰

* JDK1.8之前接口中的方法不能用static修饰，1.8之后（包含1.8）才可以使用static修饰

# 继承

## 重写与重载

* 重写（Override）

  继承中，子类重写父类方法

  * 子类方法的**访问权限**必须**大于等于**父类方法；
  * 子类方法的返回类型必须是**父类方法返回类型或为其子类型**。
  * 子类抛出的**异常**比父类的**小或者相等**

   关于静态方法重写

  static方法不能被子类重写，子类如果定义了和父类完全相同的static方法，Son.staticmethod()或new Son().staticmethod()都是调用子类的，如果是Father.staticmethod()或Father f = new Son(); f.staticmethod()调用的是父类的

* 重载（Overload）

  存在于同一个类中，指一个方法与已经存在的方法名称上相同，但是参数类型、个数、顺序至少有一个不同

  注意，返回值不同，其它都相同不算是重载。

  

## 初始化顺序

（括号内的按出现先后进行初始化）

1. 父类（静态变量、静态代码块）

2. 子类（静态变量、静态代码块）

3. 父类（实例变量、普通代码块）

4. 父类（构造函数）

5. 子类（实例变量、普通代码块）

6. 子类（构造函数）

# 内部类

## 基础

* 内部类是指在一个外部类的内部再定义一个类。内部类作为外部类的一个成员，并且依附于外部类而存在的。

* 内部类可为静态，可用protected和private修饰（而外部类只能使用public和缺省的包访问权限）。

* 内部类主要有以下几类：成员内部类、局部内部类、静态内部类、匿名内部类

## 成员内部类

* 在外部类里想调用内部类里的方法和变量，只能通过new的形式创建内部类实例才可以使用

* 内部类调用外部类的方法，无论静态非静态，直接调用

* 内部类里的成员可以和外部类的成员的名字相同，直接使用的时候调用的是内部的，内部类调用外部类同名变量需要加上外部类名，如果没有同名变量就可以直接使用，不需要加外部类名

* 在其他地方想要调用有内部类的类的内部类方法，需要实例化一个外部类，再使用外部类的实例变量实例化内部类

* 可以被abstarct修饰，但无法实例化

* 不能有静态成员、方法

  ```java
  Outer out = new Outer();
  Outer.Inner outin = out.new Inner();
  outin.inner_f1();
  ```

## 局部内部类

* 局部内部类调用外部类的方法，直接调用
* 外部类调用内部类方法，初始化外部类实例，调用有局部内部类的方法
* 局部内部类 可以看成是 方法里的成员变量，只能在该方法里实例化
* 局部内部类 对于同名变量的访问方式同成员内部类
* 方法外的外部类的成员变量可以直接访问，但只能访问内部类所在方法里的final修饰的变量
* 不能被static还有访问控制符修饰，可以被abstract、final修饰

## 静态内部类

* 静态内部类中可以定义静态或者非静态的成员或方法

* 静态内部类只能访问外部类的静态成员，不能访问外部类的非静态的成员

* 外部类方法访问内部类静态成员，直接 内部类名.静态成员变量

  访问内部非静态成员，实例化内部类，在调用

  说白了，静态属于整个类的，不属于某个对象的，可以直接使用，不依赖外部类，可以直接调用，或者实例化外部类里的静态类

  ```java
  Outer.Inner outin = new Outer.Inner();
  outin.staticInner_f1();		// 或者直接调用 Outer.Inner.staticInner_f1()
  outin.inner_f1();			// 不能直接 Outer.Inner.inner_f1()
  System.out.print(outin.staticField + outin.field);
  // 同理只能 Outer.Inner.staticField, 不能Outer.Inner.field；
  ```

* 不可以只实例化外部类，再使用这个实例去实例化内部类或者调用内部类里的成员，这点跟成员内部类是不一样的


## 匿名内部类

* 匿名内部类不能有构造方法
* 无法被访问控制符、static修饰
* 匿名内部类不能定义任何静态成员、方法和类

匿名都没类名了，构造方法，静态成员之类的没办法调用了

参考[深入理解java内部类](https://www.cnblogs.com/ITtangtang/p/3980460.html "")

# 反射和内省

* 反射：可以在运行时动态获取类信息或者动态调用类方法；JVM运行的时候，读入类的字节码到 JVM 中，对该类的属性、方法、构造方法进行获取和调用

类加载一次之后会在JVM中缓存，但是如果是**不同的类加载器**去加载**同一个class**则会多次加载。

两个类相等需要类本身相等，并且使用同一个类加载器进行加载。这是因为每一个类加载器都拥有一个独立的类名称空间。

这里的相等，包括类的 Class 对象的 equals() 方法、isAssignableFrom() 方法、isInstance() 方法的返回结果为 true，也包括使用 instanceof 关键字做对象所属关系判定结果为 true。

反射慢是因为Java是静态语言，如果在JVM运行时才进行加载，进行参数和方法的解析，此时的JVM无法对反射加载的类进行优化，还有就是类加载需要经过验证，判断是否对JVM有害，反射加载验证会比平时在装载期的时间长，但是总的来说影响不大

* 内省：针对JavaBean，只能对Bean的属性进行操作；加载类，得到它的属性，对属性进行get/set，是对反射的一层封装

# 枚举

* 使用enum定义的枚举类默认继承了java.lang.Enum，而不是继承Object类

* 枚举类可以实现一个或多个接口

* 使用enum定义、非抽象的枚举类默认使用final修饰，不可以被继承，定义的Enum类默认被final修饰，无法被其他类继承

* 枚举类的所有实例都必须放在第一行展示，不需使用new 关键字，不需显式调用构造器。

  自动添加public static final修饰

* 枚举类的构造器只能是私有的

* 枚举类也能定义属性和方法，可以是静态和非静态的

参考 [java浅谈枚举类](https://www.cnblogs.com/sister/p/4700702.html "")

具体例子

反编译之后会发现，SPRING、SUMMER、FALL这些是静态常量(public static final 修饰)，而且是在静态代码块里初始化的，同时还附带有public static Season[] values()方法和public static Season valueOf(String s)方法

```java
public enum Season{
    // 调用无参构造器、括号里的变量称为自定义变量，可以有多个，要跟构造方法对应
    SPRING(),
    // 调用有参构造器
    SUMMER("夏天"),
    // 默认调用无参构造器
    FALL；

    private String name;

    // 默认是private的
    Season() {}

    private Season(String name) {
        this.name = name;
    }
// ----------------------------------------------------    
    // 如果在enum中定义了抽象方法,每个实例都要重写该方法
    public abstract String whatSeason();
    
    // 调用无参构造器
    SPRING() {
        // 方法无法调用enum类里的非静态变量，只能调用非静态变量
        @Override
        public String whatSeason(){return "chun";}
    },
    // 调用有参构造器
    SUMMER("夏天") {
        @Override
        public String whatSeason(){return "xia";}
    },
    // 默认调用无参构造器
    FALL {
        @Override
        public String whatSeason(){return "qiu";}
    };
}

```

原理参考[深入理解java类型](https://blog.csdn.net/javazejian/article/details/71333103 "")

# 泛型

* 泛型会类型擦除，那它如何保证类型的正确？

  java编译器是通过先检查代码中泛型的类型，然后再进行类型擦除，在进行编译的

  只能在编译期保证类型相同

* 泛型被擦除，统一使用原始类型Object，泛型类型变量最后都会被替换为原始类型，那为什么我们使用的时候不需要强转转换?

  比如ArrayList，它会帮我们进行强转转换，它会做了一个checkcast操作，检查什么类型，之后进行强转

* 类型擦除与多态导致冲突，如何解决

  比如本意是进行重写，实现多态。可是类型擦除后，只能变为了重载。这样，类型擦除就和多态有了冲突，JVM采用桥方法解决此问题

* 泛型擦除的优点

  类型安全，编译器会帮我们检查、消除强制类型转换，提高代码可读性、为未来版本的 JVM 的优化带来可能

* List<?>、List<Object>、List、List<? super T>、List<? extends T>的区别

  List<? super T>：T的父类，包括T，表示范围

  List<? extends T>：T的子类，包括T，表示范围

  List<?> 表示任意类型，如果没明确，就是Object或者任意类，与List、List<Object>一样，表示 点

  List 也可表示范围

  

参考 [10 道 Java 泛型面试题](https://cloud.tencent.com/developer/article/1033693 "")

参考[java泛型（二）、泛型的内部原理：类型擦除以及类型擦除带来的问题](https://www.cnblogs.com/xll1025/p/6489088.html "")

# 注解

# 序列化和反序列化

序列化：将对象转换为字节序列，用于将保存在JVM内存中的对象持久化，保存到文件或者网络传输

反序列化：将字节序列还原成对象

与JSON的比较，两者都可以用于网络传输，JSON更使用web方面、应用方面，易读，需要将JSON解析才能还原成对象，而序列化反序列化是JAVA提供的，由JVM来还原，应用范围会更广一些

除此之外也有其他协议，如基于XML文本协议：WebService、Burlap，二进制协议：Hessian，Hessian生成的字节流简凑、跨平台、高性能比JDK的序列化反序列化优秀

# 参考

[深入剖析Java中的装箱和拆箱](https://www.cnblogs.com/dolphin0520/p/3780005.html "")
[CyC2018/CS-Notes/java基础.md](https://github.com/CyC2018/CS-Notes/blob/master/docs/notes/Java%20%E5%9F%BA%E7%A1%80.md "")
[深入理解Java中的String](https://www.cnblogs.com/xiaoxi/p/6036701.html "")