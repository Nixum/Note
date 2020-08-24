[TOC]

# Spring 和 Spring Boot区别

Spring Boot实现了自动配置，降低了项目搭建的复杂度。它主要是为了解决使用Spring框架需要进行大量的配置太麻烦的问题，所以它并不是用来替代Spring的解决方案，而是和Spring框架紧密结合用于提升Spring开发者体验的工具。同时它集成了大量常用的第三方库配置(例如Jackson, JDBC, Mongo, Redis, Mail等等)，做到零配置即用

SpringBoot自动配置流程：

* 启动类 Application.java ：**@SpringBootApplication** 和 main方法里有个**SpringApplication.run(Application.class, args)**

* @SpringBootApplication里包含了

  @SpringBootConfiguration(**@Configuration**)：JavaConfig配置类，相当一个xml文件，@Bean的方法名为xml文件里\<bean\>的id

  **@ComponentScan**：配上路径，用于扫描文件上的注解，并注入到IOC容器中，如扫描@Server@Controller

  **@EnableAutoConfiguration**：自动配置，借助@Import的帮助，将所有符合自动配置条件的bean定义加载到IoC容器，里面的@Import(**EnableAutoConfigurationImportSelector.class**)帮助SpringBoot应用将所有符合条件的@Configuration配置都加载到当前SpringBoot创建并使用的IoC容器

* **EnableAutoConfigurationImportSelector**类里有个SpringFactoriesLoader工厂加载器，通过里面的loadFactoryNames方法，传入**工厂类名称**和**对应的类加载器**，加载该类加器搜索路径下的指定文件**spring.factories文件**，传入的工厂类为接口，而文件中对应的类则是接口的实现类，或最终作为实现类，得到这些类名集合后，通过**反射**获取这些类的类对象、构造方法，最终生成实例。
* 因此只要在maven中加入了所需依赖，根据spring.factories文件里的key-value，能够在类路径下找到对应的class文件，就会触发自动配置

![SpringBoot启动流程](https://github.com/Nixum/Java-Note/raw/master/Note/picture/SpringBoot启动流程.png)

参考[SpringBoot启动流程解析](https://www.cnblogs.com/trgl/p/7353782.html)

# ContextLoaderListener

[【Spring】浅谈ContextLoaderListener及其上下文与DispatcherServlet的区别](https://www.cnblogs.com/weknow619/p/6341395.html "")

* 作为Spring启动入口

* 实现了ServletContextListener 接口，监听ServletContext，如果 ServletContext 发生变化（如服务器启动时ServletContext 被创建，服务器关闭时 ServletContext 将要被销毁）时，执行监听器里的方法
* 为IOC容器提供环境，扫描包，将带有注解的Bean加入到容器用于依赖注入，或者加载xml文件，将xml注册的bean加入容器用于依赖注入

# 三级缓存

1. 第一级缓存：单例缓存池singletonObjects。
2. 第二级缓存：早期提前暴露的对象缓存earlySingletonObjects。（属性还没有值对象也没有被初始化）
3. 第三级缓存：singletonFactories单例对象工厂缓存。

# IOC和DI

**控制反转**：实际上就是把开发人员对程序执行流程的控制，反转到由程序自己来执行，代表性的例子就是 模板方法设计模式，实际上是一种设计思想，就像spring把依赖注入给抽成框架，由框架来自动创建对象、管理对象生命周期、注入等，开发者只需要关注类间的关系即可

**依赖注入**：实际上就是对类成员初始化，并不在类内部进行，而是在外部初始化后通过构造方法、参数等方式才传递给类，就像spring那几种注入方式：构造器注入、set方法注入、注解注入

## 1.Bean的作用域

Spring中的bean默认都是单例的，对于一些公共属性，在多线程下并不安全，spring支持将bean设置为其他作用域，如prototype(多例，使用时才创建，每次获取的bean都不是同一个)、request（每次请求创建新bean，request结束，bean销毁）、session（每次请求创建新bean，仅在当前HTTP session内有效）、globalSession（基于 portlet 的 web 应用，现在很少用了）

## 2.Bean的生命周期

* Spring 容器可以管理 singleton 作用域下 bean 的生命周期，在此作用域下，Spring 能够精确地知道bean何时被创建，何时初始化完成，以及何时被销毁；prototype 作用域的bean，Spring只负责创建，之后就不再管理，只能程序猿通过代码控制

* 生命周期执行过程，引用[【Spring】Bean的生命周期](https://yemengying.com/2016/07/14/spring-bean-life-cycle/ "")
  * Bean容器找到配置文件中Spring Bean的定义。
  * Bean容器利用Java Reflection API创建一个Bean的实例（对scope为singleton且非懒加载的bean实例化）
  * 如果涉及到一些属性值 利用set方法设置一些属性值。
  * 如果Bean实现了BeanNameAware接口，调用setBeanName()方法，传入Bean的名字。
  * 如果Bean实现了BeanClassLoaderAware接口，调用setBeanClassLoader()方法，传入ClassLoader对象的实例。
  * 如果Bean实现了BeanFactoryAware接口，调用setBeanClassLoader()方法，传入ClassLoader对象的实例。
  * 与上面的类似，如果实现了其他*Aware接口，就调用相应的方法。
  * 如果有和加载这个Bean的Spring容器相关的BeanPostProcessor对象，执行postProcessBeforeInitialization()方法(需手动注册该方法)
  * 如果Bean实现了InitializingBean接口，执行afterPropertiesSet()方法。
  * 如果Bean在配置文件中的定义包含init-method属性，执行指定的方法。
  * 如果有和加载这个Bean的Spring容器相关的BeanPostProcessor对象，执行postProcessAfterInitialization()方法(需手动注册该方法)
  * 此时bean已经准备就绪，可以被应用程序使用了，他们将一直驻留在应用上下文中，直到该应用上下文被销毁
  * 当要销毁Bean的时候，如果Bean实现了DisposableBean接口，执行destroy()方法。
  * 当要销毁Bean的时候，如果Bean在配置文件中的定义包含destroy-method属性，执行指定的方法。

其他参考：[Spring Bean生命周期](https://www.jianshu.com/p/3944792a5fff)

## 3.初始化

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

具体例子，参考[Spring IOC核心源码学习](https://yikun.github.io/2015/05/29/Spring-IOC%E6%A0%B8%E5%BF%83%E6%BA%90%E7%A0%81%E5%AD%A6%E4%B9%A0/ "")

## 4.注入方式

* setter注入

* 构造器注入

* 自动装配：xml下使用“autowire”属性，有no、byName、byType、constructor、autodetect方式可选

  注解注入：@Resource默认是使用byName进行装配，@Autowired默认使用byType

## 5.IOC模拟

[SpringIOC简单模拟，菜鸟篇](https://blog.csdn.net/wangaiheng/article/details/79793397)

[Spring——原理解析-利用反射和注解模拟IoC的自动装配](https://www.cnblogs.com/weilu2/p/spring_ioc_analysis_principle_bsici_on_reflection_annotation.html)

# AOP

## 原理

Aop基于代理模式，代理分为三种代理：静态代理，JDK动态代理，CGLib代理

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

## 静态代理

其实就是上面那种模式，哪个类需要代理就为那个类编写一个代理类

好处：在不修改目标对象的功能的前提下，增添新方法
缺点：每个对象都需要有代理对象，导致有很多代理类；接口增加方法，所有的实现类都要改

## JDK动态代理

动态代理是为了解决上述问题，通过反射 + 多态的方式，动态为一个一个类设置代理，只有知道那个类的接口才可以

利用 JDK java.lang.reflect包里的InvocationHandler接口，代理方法具体逻辑写在invoke方法里

```java
public interface InvocationHandler {
    // proxy：动态产生的代理对象，method：被代理类要执行的方法，args：方法所需参数
    public Object invoke(Object proxy, Method method, Object[] args)
        throws Throwable;
}
```

还有java.lang.reflect包中的Proxy类的newProxyInstance方法,，他是静态的，作用是为被代理类构建代理类，并返回，在这个动态生成的代理类中已经织入了InvocationHandler，而它又持有被代理类，对代理对象的所有接口方法调用都会转发到InvocationHandler.invoke()方法

```java
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

## CGLIB代理

CGLIB代理就可以直接代理普通类，不需要接口了

原理：直接读取被代理类的字节码，通过继承的方式实现，在内存中构建被代理类的子类对象从而实现对目标对象的功能扩展

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

Spring底层

[Spring 源码分析Aop](https://blog.csdn.net/fighterandknight/article/details/51209822 "")

[Spring AOP原理分析](https://blog.csdn.net/yuexianchang/article/details/77018603 "")

[常见的Spring面试题](http://www.codeceo.com/article/spring-top-25-interview.html#xml_based_configuration)