# 类图

类图中的关系其实有多种版本的表示方法，这里仅总结自己常用的画法

## 访问作用域

* \+ : public

* \- : private
* \# : protocted

## 关系

### 1. 依赖（dependency）

依赖关系是五种关系中耦合最小的一种关系。

依赖在代码中主要体现为类A的某个成员函数的返回值、形参、局部变量或静态方法的调用，则表示类A引用了类B。

  A ----> B ： A use B （虚线+箭头）

![A use B](https://github.com/Nixum/Java-Note/raw/master/Note/picture/UML-use.png)

### 2. 关联（Association）

在程序代码中，具有关联关系的类常常被声明为类的引用类型的成员变量。

因为 关联 是 依赖 的更详细说明， 关联 是专门描述成员属性的关系，所以依赖中所有涉及成员属性的地方更适合使用：关联、聚合、组合

单向关联：

A ——————> B ： A has B （实心线 + 箭头）

 ![A has B](https://github.com/Nixum/Java-Note/raw/master/Note/picture/UML-association.png)

### 3. 聚合（Aggregation）

聚合是关联的一种特殊形式，暗含整体/部分关系，但是对方却不是唯一属于自己的那种关系。 用来表示集体与个体之间的关联关系，例如班级与学生之间存在聚合关系。

A <>—————— B :   A是集体，B是个体 （实线 + 空心菱形）

  ![A是集体，B是个体](https://github.com/Nixum/Java-Note/raw/master/Note/picture/UML-aggregation.png)

### 4. 组合（Composition）

组合又叫复合，用来表示个体与组成部分之间的关联关系。 在组合关系中整体对象可以控制成员对象的生命周期，一旦整体对象不存在，成员对象也不存在，整体对象和成员对象之间具有同生共死的关系。

A <#>———— B： A是整体，B是部分  （实线线 + 实心菱形）

  ![A是整体，B是部分](https://github.com/Nixum/Java-Note/raw/master/Note/picture/UML-composition.png)

### 5. 泛化

#### 5.1 继承（Generalization）

A ——————|> B : A继承了B  （实心线 + 空心三角箭头），A is B

![A继承了B](https://github.com/Nixum/Java-Note/raw/master/Note/picture/UML-generalization.png)

#### 5.2. 实现（Implementation）

A --------------|> B : A实现了接口B （虚心线 + 空心三角箭头）， A like B

![A实现了接口B](https://github.com/Nixum/Java-Note/raw/master/Note/picture/UML-implementation.png)

PS：为了方便，继承和接口实现也都可以用实线加空心三角箭头