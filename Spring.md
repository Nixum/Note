---
title: Spring和SpringBoot
description: Spring和SpringBoot常见问题、IOC、AOP原理
date: 2019-04-17
weight: 20
categories: ["框架"]
tags: ["Spring", "SpringBoot", "IOC和AOP"]
---

[TOC]

# SpringBoot

## Spring 和 Spring Boot区别

Spring Boot实现了自动配置，降低了项目搭建的复杂度。它主要是为了解决使用Spring框架需要进行大量的配置太麻烦的问题，所以它并不是用来替代Spring的解决方案，而是和Spring框架紧密结合用于提升Spring开发者体验的工具。同时它集成了大量常用的第三方库配置(例如Jackson, JDBC, Mongo, Redis, Mail等等)，做到零配置即用。内置Tomcat作为Web服务器，不像之前还要把服务部署到Tomcat在进行启动。

## SpringBoot整个启动流程

1. 构建SpringApplication对象，执行其run方法
2. 加载properties/yaml等配置文件
3. 创建ApplicationContext（也可以称为Bean、IOC容器）
4. 将扫描到的Bean或者xml中的bean，先解析成BeanDefinition，注册到ApplicationContext中的BeanFactory中（即自动配置过程，也是IOC容器的refresh方法执行过程）
5. 实例化Bean，进行依赖注入，（AOP也是在此处实现，创建代理实例加入IOC容器）

![SpringBoot启动流程](https://github.com/Nixum/Java-Note/raw/master/picture/SpringBoot启动流程.png)

参考[SpringBoot启动流程解析](https://www.cnblogs.com/trgl/p/7353782.html)

[SpringBoot启动流程](https://www.cnblogs.com/lay2017/p/11478237.html)

## SpringBoot自动配置流程

**自动配置流程只是SpringBoot启动中的一个环节，该环节只是在告诉Spring要在哪里找到Bean的声明。**

启动类main方法为入口，main方法所在的类会被**@SpringBootApplication**修饰， 通过main方法里执行**SpringApplication.run(Application.class, args)**进行启动，Spring启动时会解析出@SpringBootApplication注解，进行Bean的加载和注入。

* @SpringBootApplication里包含了

  * **@SpringBootConfiguration**：作用类似于**@Configuration**，JavaConfig配置类，相当一个xml文件，配合@Bean注解让IOC容器管理声明的Bean

  * **@ComponentScan**：配上包路径，用于扫描指定包及其子包下所有类，如扫描@Component、@Server、@Controller等，并注入到IOC容器中

  * **@EnableAutoConfiguration**：自动配置的核心注解，主要用于找出所有自动配置类。该注解会使用**@Import(EnableAutoConfigurationImportSelector.class**)帮助SpringBoot应用将所有符合条件的@Configuration配置都加载到当前SpringBoot创建并使用的IoC容器。

* **EnableAutoConfigurationImportSelector**类里有个SpringFactoriesLoader工厂加载器，通过里面的loadFactoryNames方法，传入**工厂类名称**和**对应的类加载器**，加载该类加载器搜索路径下的指定文件**spring.factories文件**，传入的工厂类为接口，而文件中对应的类则是接口的实现类，或最终作为实现类，得到这些类名集合后，通过**反射**获取这些类的类对象、构造方法，最终生成实例。

  因此只要在maven中加入了所需依赖，根据**spring.factories**文件里的key-value，能够在类路径下找到对应的class文件，就会触发自动配置

## 自定义starter

实际上就是编写自动配置类，会使用到一系列配置注解，如@Configuration、@EnableConfigurationProperties、@Component、@Bean、@ConditionOnXX、@AutoConfigureOrder等，让IOC容器加载我们自定义的Bean进去；

另外就是必须在META-INF文件夹下创建spring.factories，告知Spring在哪找到配置类。

```
org.springframework.boot.autoconfigure.EnableAutoConfiguration=[自定义配置类的全限定名称]
```

自定义Starter可以理解为一个Jar包，该Jar包在Maven或Gradle注册后，服务启动时，IOC容器会去自动加载。

自定义Starter内也可以使用配置文件，设定默认配置的key-value，当本项目里有配置的key与starter里定义的配置key重复时可以被替换

## ContextLoaderListener

[【Spring】浅谈ContextLoaderListener及其上下文与DispatcherServlet的区别](https://www.cnblogs.com/weknow619/p/6341395.html "")

* 作为Spring启动入口
* 实现了ServletContextListener 接口，监听ServletContext，如果 ServletContext 发生变化（如服务器启动时ServletContext 被创建，服务器关闭时 ServletContext 将要被销毁）时，执行监听器里的方法
* 为IOC容器提供环境，扫描包，将带有注解的Bean加入到容器用于依赖注入，或者加载xml文件，将xml注册的bean加入容器用于依赖注入

# 常用注解

## @Controller与@RestController

* @Controller 默认是返回视图，即方法的return返回的是视图层的路径，只有+@ResponseBody才会返回Json格式的数据
* @RestController实际上是@Controller + @ResponseBody组合，默认返回json格式的数据

## @Autowired与@Resource

* @Autowired 注解，修饰类成员变量、方法及构造方法，完成自动装配的工作，默认按 byType 自动注入。只有一个required属性，默认是true，表示必须注入，不能为null

  @Autowired 自动注入时，Spring 容器中匹配的候选 Bean 数目必须有且仅有一个。因为它是按类型注入的，如果有多个同类型的Bean会导致出错，此时可以配合@Qualifier来规避这种情况，通过 @Qualifier("实例名称") 指定注入bean的名称，消除歧义，此时与 @Resource指定name属性作用相同。

* @Resource 的作用相当于 @Autowired，只不过 @Autowired 按 byType 自动注入，面@Resource 默认按 byName 自动注入，该注解有两个属性，name和type，分别代表通过名称查找bean和通过类型查找bean。

  此外@Resource还有其他属性，如lookup、shareable、mappedName等

## @Component与@Bean

两者都是用于标记，被标记的实例会被Spring管理

* @Component只能作用类，配合@ComponentScan注解让Spring启动时进行扫描，当扫描到@Component修饰的类时会进行实例化和依赖注入

  @Component是一个比较通用的语义，@Service、@Repository的作用与@Component相同的，只是语义不同，修饰的类所在的层次不同

* @Bean只能作用于方法，通过方法来实例化Bean，Bean的名称为方法的名称（如果有前缀get会自动忽略），交由IOC容器管理，通常与@Configuration配合使用，等价于xml文件中的<bean>配置，方法名相当于<bean>中的id，需要唯一

  @Bean的方式初始化bean会更加灵活，因为可以在方法内部进行逻辑处理，比如利用配置文件 + 工厂模式实例化不同的bean

## @Configuration与@ConfigurationProperties

* @Configuration作用于类，相当于加载bean的xml文件，一般配合@Bean注解使用，让IOC容器管理我们声明的bean
* @ConfigurationProperties用于读取key-value的那种配置文件，如properties、yaml等，类似于@Value，主要用于配置文件的字段注入，有属性prefix表示前缀，key为属性名称，将配置文件里的配置绑定到类的属性上。

## @SpringBootApplication与@ComponentScan与@Import

两者都用于告知Spring在哪里找到bean，只是扫描的路径不同

* @SpringBootApplication作用与main方法所在的类，用于启动IOC容器，默认会扫描该类所在包及其子包下，进行Bean的实例化和管理

  @SpringBootApplication实际上包含了三个注解，@ComponentScan、@EnableAutoConfiguration、@SpringBootConfiguration，详情见上面SpringBoot启动流程。

* @ComponentScan("包路径")，用于扫描指定包及其子包下的类，哪些需要交由IOC容器管理，一般用于扫描@SpringBootApplication扫描不到的包。在非SpringBoot项目下，必须使用。

* @Import，相当于xml中的<import/>，主要是导入Configuration类，作用类似@ComponentScan，只不过@ComponentScan是通过扫描找到，范围广，@Import是直接指定某个Config类

* @ImportResource("classpath*:xml文件")，则是直接导入指定的xml文件

# IOC和DI

**控制反转**：实际上就是把开发人员对程序执行流程的控制，反转到由程序自己来执行，代表性的例子就是 模板方法设计模式，实际上是一种设计思想，就像spring把依赖注入给抽成框架，由框架来自动创建对象、管理对象生命周期、注入等，开发者只需要关注类间的关系即可

**依赖注入**：实际上就是对类成员初始化，并不在类内部进行，而是在外部初始化后通过构造方法、参数等方式才传递给类，就像spring那几种注入方式：构造器注入、set方法注入、注解注入

## Bean的作用域

Spring中的bean默认都是单例的，对于一些公共属性，在多线程下并不安全，spring支持将bean设置为其他作用域

* prototype：多例，使用时才创建，每次获取的bean都不是同一个
* request：每次请求创建新bean，request结束，bean销毁
* session：每次请求创建新bean，仅在当前HTTP session内有效
* globalSession：基于 portlet 的 web 应用，现在很少用了

## 注入方式

- setter注入

- 构造器注入，无法解决循环依赖问题

- 自动装配：xml下使用“autowire”属性，有no、byName、byType、constructor、autodetect方式可选

  注解注入：@Resource默认是使用byName进行装配，@Autowired默认使用byType。

  byName和byType指的是依赖注入时寻找bean的方式。@Resource和@Autowired都可以修饰属性、setter方法、构造器，此时表示的是以哪种方式进行注入

## Bean加载流程

初始化IOC容器（工厂入货）

* 读取xml文件 / 扫描包类上的注解
* 解析成BeanDefinition，创建了Bean的定义类
* 注册到BeanFactory，此时的工厂里只保存了类创建所需要的各种信息还没有真正的实例化Bean对象

依赖注入（工厂出货）

* 初始化IOC容器
* 初始化Bean（没有设置Lazy-init）
* 反射创建Bean实例
* 注入

详细源码分析，参考[Spring: 源码解读Spring IOC](https://www.cnblogs.com/ITtangtang/p/3978349.html "")

[Spring  IOC源码分析](https://segmentfault.com/a/1190000015221968 "")

具体例子，参考[Spring IOC核心源码学习](https://yikun.github.io/2015/05/29/Spring-IOC%E6%A0%B8%E5%BF%83%E6%BA%90%E7%A0%81%E5%AD%A6%E4%B9%A0/ "")

## 三级缓存解决循环依赖

1. 第一级缓存：单例缓存池singletonObjects，存放完全初始化完的实例或者AOP实例对象，此时已经完成注入，直接可用了。
2. 第二级缓存：早期提前暴露的对象缓存earlySingletonObjects，用于检测循环引用，与singletonFactories互斥，如果一级缓存获取不到，则在此层获取实例，如果获取不到，且允许去三级缓存获取，则从三级缓存中获取，并remove加入二级缓存。一般用于存放完成了部分依赖注入的Bean。
3. 第三级缓存：singletonFactories单例对象工厂缓存，存放初始化不完全的实例（还有依赖没注入），如果到了三级缓存都获取不到，就会进行初始化，并加入。

当出现循环依赖的对象注入时，会利用这三级缓存来解决问题，但是Spring只能解决Setter方法的注入，无法解决构造器注入，原因是如果通过构造器注入，需要先准备好需要注入的属性

假如现在有类A，持有属性B，类B，持有属性A

通过构造器注入：初始化A，此时需要B，那初始化B，此时需要A，但是A因为构造器注入需要先有B，此时无法完成初始化。

通过Setter方法注入：

1. 初始化A，会先依次从三级缓存中获取A实例，获取不到，说明A还未初始化，初始化A产生实例，将实例A加入singletonFactories中。
2. 对A进行依赖注入，发现需要注入B，依次从三级缓存里获取B实例，到了第三层都获取不到，说明还未初始化，初始化B产生实例，加入singletonFactories，对B进行依赖注入，发现需要注入A，依次从三级缓存里获取A实例，在singletonFactories获取到还未初始化完全的实例A，从singletonFactories中remove，加入到earlySingletonObjects，注入到B中，此时B完全初始化完成，从earlySingletonObjects中remove，将B加入到singletonObjects中。
3. 回到对A进行依赖注入部分，由于B刚刚初始化完成加入了singletonObjects，所以A获取到B，进行注入，A初始化完全，加入singletonObjects中

**总结**：Spring在实例化一个bean的时候，先实例化该bean，然后递归的实例化其所依赖的所有bean，直到某个bean没有依赖其他bean，此时就会将该实例返回，然后反递归的将获取到的bean设置为各个上层bean的属性中。

实际上，看起来earlySingletonObjects会有点多余，这一层的缓存主要是扩展时使用。

## Bean的生命周期

- Spring 容器可以管理 singleton 作用域下 bean 的生命周期，在此作用域下，Spring 能够精确地知道bean何时被创建，何时初始化完成，以及何时被销毁；prototype 作用域的bean，Spring只负责创建，之后就不再管理，只能由开发人员通过代码控制

- 关于xxxAware类型的接口，Aware之前的名字表示IOC容器可以获得什么资源，Aware方法都是在初始化阶段前被调用。

- 生命周期执行过程，**总结为：实例化 -> 属性赋值 -> 初始化 -> 销毁**

  **实例化**

  - Bean容器找到配置文件中Spring Bean的定义。
  - Bean容器利用反射创建一个Bean的实例（对scope为singleton且非懒加载的bean实例化，此处会**触发AOPCreator解析@Aspect类**）

  **属性赋值** - 依赖注入

  - 如果涉及到一些属性值 利用set方法设置一些属性值。
  - 如果Bean实现了BeanNameAware接口，调用setBeanName()方法，传入Bean的名字，相当于xml中<bean>中的id或name。
  - 如果Bean实现了BeanClassLoaderAware接口，调用setBeanClassLoader()方法，传入ClassLoader对象的实例。
  - 如果Bean实现了BeanFactoryAware接口，调用setBeanFactory()方法，传入beanFactory对象的实例，可以通过beanFactory获取其他Bean。
  - 与上面的类似，如果实现了其他*Aware接口，就调用相应的方法，比如实现了ApplicationContextAware接口，就可以获取上下文，作用同BeanFactory，只是能获取到其他数据。
  - 如果Bean实现了BeanPostProcessor接口，执行postProcessBeforeInitialization()方法（需手动实现该方法，**利用该方法实现AOP，在此处创建代理类**）

  **初始化**

  - 如果Bean实现了InitializingBean接口，执行afterPropertiesSet()方法。作用同xml配置中的init-method配置一样，用于指定初始化方法，但是先于init-method方法执行。
  - 如果Bean在xml配置文件中<bean>的init-method属性，执行指定的方法进行初始化。
  - 如果Bean实现了BeanPostProcessor接口，执行postProcessAfterInitialization()方法（需手动实现该方法，**利用该方法实现AOP，在此处创建代理类**）
  - 此时bean已经准备就绪，可以被应用程序使用了，他们将一直驻留在应用上下文中，直到该应用上下文被销毁

  **销毁**

  - 当要销毁Bean的时候，如果Bean实现了DisposableBean接口，执行destroy()方法。
  - 当要销毁Bean的时候，如果Bean在xml配置文件中的<bean>的destroy-method属性，执行指定的方法进行销毁。

参考：[Spring Bean生命周期](https://www.jianshu.com/p/3944792a5fff)

[Spring Bean生命周期](https://www.jianshu.com/p/1dec08d290c1)：这篇写得不错

## ApplicationContext与BeanFactory的区别

* ApplicationContext与BeanFactory类似，BeanFactory只提供最基本的Bean对象创建与获取，ApplicationContext指的是上下文，包含BeanFactory的功能（实现了BeanFactory接口），同时也提供了其他额外的功能，比如事件机制、监听、拦截器、资源访问等。
* BeanFactory采用延迟加载来注入Bean，只有在使用到某个Bean的时候才会实例化，ApplicationContext则在容器启动时，就实例化Bean，常驻在容器内，也可以为Bean配置Lazy-init=true来让Bean延迟实例化。

## IOC模拟

[Spring——原理解析-利用反射和注解模拟IoC的自动装配](https://www.cnblogs.com/weilu2/p/spring_ioc_analysis_principle_bsici_on_reflection_annotation.html)

[根据 Spring 源码写一个带有三级缓存的 IOC](https://zhuanlan.zhihu.com/p/144627581)

# AOP

* 切面Aspect：指代理类，声明切点和通知
* 切点PointCut：指要把切面的通知在被代理类的方法的位置，即表达式execution（值的格式为 [方法的访问修饰符] [被代理类的全限定名称]和其方法名(方法参数)]），切点可以有多个，配合通知使用
* 通知Advice：代理类的增强方法，通知分为前置通知、环绕通知、后置通知、后置返回通知、后置异常通知
* 连接点JoinPoint：被代理类的方法

## 基本原理

AOP基于代理模式，代理分为三种代理：静态代理，JDK动态代理，CGLib代理

### 静态代理

```java
// 接口
public interface A { public void method();}

// 代理类
public class BProxy implements A {
    
    public B b;
    
    public BProxy() {}
    
    public BProxy(B b) {
        this.b = b;
    }
    
    public void method(){
        // 为被代理类进行一系列前置操作
        b.method();
        // 为被代理类进行一系列后置操作
    }
}

// 被代理类
public class B implements A {
    public void method() {
        // 具体的处理逻辑
    }
}

public static void main(String[] args) {
    B b = new B();
    BProxy bProxy = new BProxy(b);
    bProxy.method();
}
```

好处：在不修改目标对象的功能的前提下，增添新方法
缺点：每个对象都需要有代理对象，导致有很多代理类；接口增加方法，所有的实现类都要改

### JDK动态代理

动态代理是为了解决上述问题，通过反射 + 多态的方式，动态为一个一个类设置代理，只有知道那个类的接口才可以。利用 JDK java.lang.reflect包里的InvocationHandler接口，代理方法具体逻辑写在invoke方法里

```java
代理对象的执行方法！
public interface InvocationHandler {
    // proxy：动态产生的代理对象，method：被代理类要执行的方法，args：方法所需参数
    public Object invoke(Object proxy, Method method, Object[] args)
        throws Throwable;
}
```

还有java.lang.reflect包中的Proxy类的newProxyInstance方法,，他是静态的，作用是为被代理类构建代理类，并返回，在这个动态生成的代理类中已经织入了InvocationHandler，而它又持有被代理类，对代理对象的所有接口方法调用都会转发到InvocationHandler.invoke()方法

```java
该方法将会为被代理类生成代理类，代理类执行与被代理类的接口时，会执行invocationHandler的方法！
public static Object newProxyInstance(ClassLoader loader,	// 被代理类的类加载器，如上面B类的类加载器
                                      Class<?>[] interfaces,// 被代理类的接口数组
                                      InvocationHandler h)	// 上面实现了InvocationHandler接口的实例
    throws IllegalArgumentException
```

具体例子

```java
public class ProxyFactory {

    // 被代理类
	private Object target;
	public ProxyFactory(Object target){
		this.target = target;
	}
	// 匿名内部类的方式重写invoke方法
	public Object getProxyInstance(){
        // 返回代理类
		return Proxy.newProxyInstance(target.getClass().getClassLoader(),
				target.getClass().getInterfaces(),
				new InvocationHandler() {
					@Override
					//当代理者(接口)执行接口里的方法的时候就会调用此方法
					public Object invoke(Object proxy, Method method, Object[] args)
							throws Throwable {
						System.out.println("为被代理类进行一系列前置操作");
						//执行实现了该接口的类中的方法，被代理类的方法
						Object returnValue = method.invoke(target, args);
						System.out.println("为被代理类进行一系列后置操作");
						return returnValue;  //返回的是调用方法后的结果
					}
				});
	}
}

public static void main(String[] args) {
    A b = new B();	// B b = new B();也是可以的
    A bProxy = (A) new ProxyFactory(b).getProxyInstance();	// 必须转接口类型
    // 调用该接口里的方法都会被代理
    bProxy.method();
}
```

好处：只需写一次该接口的代理类就可以为以后许多实现了此接口的被代理类进行代理，一次编写，处处使用
缺点：只针对一个接口实现的代理，只能针对接口写代理类

### CGLIB代理

CGLIB代理就可以直接代理普通类，不需要接口了

原理：直接读取被代理类的字节码，通过继承的方式实现，在内存中**构建被代理类的子类对象**从而实现对目标对象的功能扩展

具体例子：

```java
public class ProxyFactory implements MethodInterceptor{
	private Object target;
	public ProxyFactory(Object target){
		this.target = target;
	}
	//给目标对象创建代理对象
	public Object getProxyInstance(){
		Enhancer en = new Enhancer();
		//设置父类
		en.setSuperclass(target.getClass());
		//设置回调函数
		en.setCallback(this);
		//创建子类代理
		return en.create();
	}
	@Override
	//object 为CGLib动态生成的代理实例
	//Method 为上文实体类所调用的被代理的方法引用
	//Object[] 方法的参数列表
	//MethodProxy 为生成的代理类对方法的代理引用
	public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable{
		System.out.println("为被代理类进行一系列前置操作");
		Object returnValue = method.invoke(target,args);
		//或者写成 Object returnValue1 = proxy.invokeSuper(obj, args);
		System.out.println("为被代理类进行一系列后置操作");
		return returnValue;
	}
}
public static void main(String[] args) {
    B b = new B();
    // B类的所有非final方法，包括它的父类都会被代理
    B bProxy = (B) new CGlibProxy(b).getProxyInstance();
    bProxy.method();
}
```

## SpringBoot中的实现

* SpringBoot中提供@EnableAspectJAutoProxy开启对AOP的支持，其中属性proxyTargetClass=true时使用cglib，为false使用JDK的动态代理，默认为false

* @EnableAspectJAutoProxy注解主要是使用AspectJAutoProxyRegistrar类将AOP处理工具注册到Bean中。

* 在这个AOP处理工具中有一个AnnotationAwareAspectJAutoProxyCreator类，该类
  * 实现了一系列Aware接口，使用BeanFactory：使得Bean可以管理
  * 实现了order接口：用于设置切面的优先级
  * 继承了ProxyConfig：该类封装了代理的通用逻辑，cglib或JDK动态代理开关配置等
  
* Bean加载完AnnotationAwareAspectJAutoProxyCreator类后，会解析开发者定义的切面类、切点、通知，在BeanFactory中找到被代理类，结合通知进行封装，创建出代理类。由于被代理类可被设置多重代理，在创建代理类时，会根据切面的优先级，不断套在被代理类上，形成拦截器链。

* 执行代理类的方法时，就会调用方法拦截器链，进行方法增强。

AOP的实现会在创建Bean实例对象前触发@Aspect切面对象，获得Advisor。生成Bean实例对象之后，才会再次触发对该Bean实例对象做代理增强，增强的Advisor来自之前的解析结果。

[SpringAOP详细介绍](https://blog.csdn.net/JinXYan/article/details/89302126)

[Spring 源码分析Aop](https://blog.csdn.net/fighterandknight/article/details/51209822 "")

# 事务

## Spring事务传播行为

- PROPERGATION_MANDATORY：方法必须运行在事务中，如果当前事务不存在，抛异常
- PROPAGATION_NESTED：当前事务存在，则该方法运行在嵌套事务中
- PROPAGATION_NEVER：方法不能运行事务中，否则抛异常
- PROPAGATION_REQUIRED：当前方法必须运行在事务中，如果当前存在事务，该方法运行其中，否则创建新事务
- PROPAGATION_REQUIRES_NEW：当前方法必须运行在事务中，如果当前存在事务，则该事务在方法运行期间被挂起
- PROPAGATION_SUPPORTS：当前方法不需要运行在事务中，但如果存在事务，也可运行在事务中
- PROPAGATION_NOT_SUPPORTED：当前方法不能运行在事务中，如果存在事务，则挂起该方法
