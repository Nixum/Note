---
title: SpringMVC
date: 2019-02-15
weight: 21
categories: ["框架"]
tags: ["SpringMVC", "Spring Security"]
---

# SpringMVC工作原理

[SpringMVC工作原理详解](https://github.com/Nixum/JavaGuide/blob/master/%E4%B8%BB%E6%B5%81%E6%A1%86%E6%9E%B6/SpringMVC%20%E5%B7%A5%E4%BD%9C%E5%8E%9F%E7%90%86%E8%AF%A6%E8%A7%A3.md "")

流程图看链接里的即可

简单来说各个组件的作用

1. **前端控制器DispatcherServlet**：请求的入口，可以看成是中央处理器、转发器，负责调度其他组件，接收请求，完成响应
2. **处理器映射器HandlerMapping**：根据请求的url查找Handler，找到url对应的controller类，返回一条执行链，其中就包含拦截器和处理器（具体的controller类）；有配置文件方式，实现接口方式，注解方式等方式实现映射
3. **处理器适配器HandlerAdapter**：HandlerMapping找到对应的controller类后，再根据url找到对应的执行方法
4. **处理器Handler**：具体的处理方法，也就是我们所写具体的Controller类
5. **视图解析器View resolver**：根据逻辑View名称，找到对应的View，根据处理器返回的ModelAndView，将数据渲染到View上
6. **视图View**：例如jsp，freemarker之类的视图模板



拦截器在什么时候执行？

拦截器，是属于HandlerMapping级别的，可以有多个HandlerMapping ，每个HandlerMapping可以有自己的拦截器，拦截器可以设置优先级。一个请求交给一个HandlerMapping时，这个HandlerMapping先找有没有处理器来处理这个请求，如何找到了，就执行拦截器，执行完拦截后，交给目标处理器。如果没有找到处理器，那么这个拦截器就不会被执行。

实现HandlerInterceptor接口或者继承HandlerInterceptor，重写**boolean preHandle()、void postHandle()、void afterCompletion()方法**

* preHandle() 方法：该方法会在控制器方法前执行，其返回值表示是否中断后续操作。

  当其返回值为true时，表示继续向下执行；当其返回值为false时，会中断后续的所有操作（包括调用下一个拦截器和控制器类中的方法执行等）。

* postHandle()方法：该方法会在控制器方法调用之后，且解析视图之前执行。可以通过此方法对请求域中的模型和视图做出进一步的修改。

* afterCompletion()方法：该方法会在整个请求完成，即视图渲染结束之后执行。可以通过此方法实现一些资源清理、记录日志信息等工作。



# Spring Security

简单工作流程

请求(包含用户名，密码之类)——>登陆信息封装成一个Authentication对象——>AuthenticationManager，调用authenticate ()方法处理——>该方法会将对象传递给一系列AuthenticationAdapter（一系列Filter），每一个AuthenticationAdapter会调用它们配置的UserDetailsService处理
