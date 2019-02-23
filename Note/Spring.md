# ContextLoaderListener

[【Spring】浅谈ContextLoaderListener及其上下文与DispatcherServlet的区别](https://www.cnblogs.com/weknow619/p/6341395.html "")

* 作为Spring启动入口

* 实现了ServletContextListener 接口，监听ServletContext，如果 ServletContext 发生变化（如服务器启动时ServletContext 被创建，服务器关闭时 ServletContext 将要被销毁）时，执行监听器里的方法
* 为IOC容器提供环境，扫描包，将带有注解的Bean加入到容器用于依赖注入，或者加载xml文件，将xml注册的bean加入容器用于依赖注入

# IOC

## 1.Bean的作用域

Spring中的bean默认都是单例的，对于一些公共属性，在多线程下并不安全，spring支持将bean设置为其他作用域，如prototype(多例，使用时才创建，每次获取的bean都不是同一个)、request（每次请求创建新bean，request结束，bean销毁）、session（每次请求创建新bean，仅在当前HTTP session内有效）、globalSession（基于 portlet 的 web 应用，现在很少用了）

## 2.Bean的生命周期

* Spring 容器可以管理 singleton 作用域下 bean 的生命周期，在此作用域下，Spring 能够精确地知道bean何时被创建，何时初始化完成，以及何时被销毁；prototype 作用域的bean，Spring只负责创建，之后就不再管理，只能程序猿通过代码控制

* 生命周期执行过程，引用[【Spring】Bean的生命周期](https://yemengying.com/2016/07/14/spring-bean-life-cycle/ "")
  * Bean容器找到配置文件中Spring Bean的定义。
  * Bean容器利用Java Reflection API创建一个Bean的实例。
  * 如果涉及到一些属性值 利用set方法设置一些属性值。
  * 如果Bean实现了BeanNameAware接口，调用setBeanName()方法，传入Bean的名字。
  * 如果Bean实现了BeanClassLoaderAware接口，调用setBeanClassLoader()方法，传入ClassLoader对象的实例。
  * 如果Bean实现了BeanFactoryAware接口，调用setBeanClassLoader()方法，传入ClassLoader对象的实例。
  * 与上面的类似，如果实现了其他*Aware接口，就调用相应的方法。
  * 如果有和加载这个Bean的Spring容器相关的BeanPostProcessor对象，执行postProcessBeforeInitialization()方法
  * 如果Bean实现了InitializingBean接口，执行afterPropertiesSet()方法。
  * 如果Bean在配置文件中的定义包含init-method属性，执行指定的方法。
  * 如果有和加载这个Bean的Spring容器相关的BeanPostProcessor对象，执行postProcessAfterInitialization()方法
  * 此时bean已经准备就绪，可以被应用程序使用了，他们将一直驻留在应用上下文中，直到该应用上下文被销毁
  * 当要销毁Bean的时候，如果Bean实现了DisposableBean接口，执行destroy()方法。
  * 当要销毁Bean的时候，如果Bean在配置文件中的定义包含destroy-method属性，执行指定的方法。

## 3.初始化

* 读取xml文件/扫描包类上的注解
* 解析BeanDefinition
* 注册到BeanFactory

## 4.注入方式

* setter注入

* 构造器注入

* 自动装配：xml下使用“autowire”属性，有no、byName、byType、constructor、autodetect方式可选

  注解注入：@Resource默认是使用byName进行装配，@Autowired默认使用byType

# AOP

