---
title: Java IO
description: Java BIO、NIO、线程模型、Netty相关原理
date: 2020-09-22
lastmod: 2021-10-23
categories: ["Java"]
tags: ["Java BIO", "Java NIO", "线程模型", "Netty"]
---

[TOC]

# BIO

特点

- BIO是同步阻塞的，以流的形式处理，基于字节流和字符流
- 每个请求都需要创建独立的线程，处理Read和Write
- 并发数较大时，就算是使用了线程池，也需要创建大量的线程来处理
- 连接建立后，如果处理线程被读操作阻塞了，那就阻塞了，只能等到读完才能进行其他操作

以基于TCP协议的Socket，编写服务端Demo

```java
package com.nixum.bio;

import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BIOServer {
  public static void main(String[] args) throws Exception {
    ExecutorService newCachedThreadPool = Executors.newCachedThreadPool();
    //创建ServerSocket
    ServerSocket serverSocket = new ServerSocket(8080);
    while (true) {
      // 主线程负责处理监听
      final Socket socket = serverSocket.accept();
      // 创建线程处理请求
      newCachedThreadPool.execute(() -> {
        handler(socket);
      });
    }
  }

  public static void handler(Socket socket) {
    try {
      byte[] bytes = new byte[1024];
      //通过socket获取输入流
      InputStream inputStream = socket.getInputStream();
      //循环的读取客户端发送的数据
      while (true) {
        int read =  inputStream.read(bytes);
        if(read != -1) {
          System.out.println("接收到的请求是：" + new String(bytes, 0, read));
        } else {
          break;
        }
        // 响应给客户端
        PrintWriter out = new PrintWriter(socket.getOutputStream());
        out.println("服务端接收到请求了，响应时间：" + new Date());
        out.flush();
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        // 关闭连接
        socket.close();
      }catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
```

客户端Demo：

```java
package com.atguigu.bio;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class BIOClient {
  public static void main(String[] args) throws Exception {
    Socket client = new Socket("127.0.0.1", 8080);
    Scanner scan = new Scanner(System.in);
    PrintStream out = new PrintStream(client.getOutputStream());
    BufferedReader bufferReader =
      new BufferedReader(new InputStreamReader(client.getInputStream()));
    while (true) {
      if (scan.hasNext()) {
        // 键盘输入，并发送请求
        String str = scan.next();
        if ("exit".equalsIgnoreCase(str)) {
          break;
        }
        out.println(str);
        out.flush();
        // 接收服务端的响应
        System.out.println(bufferReader.readLine());
      }
    }
    client.close();
  }
}

```

# NIO

特点：

* 同步非阻塞，通过缓冲区进行缓冲增加处理的灵活性，当某一线程里**没有数据可用**时，则去处理其他事情，保证线程不阻塞
* 由三个部分组成：Channel、Buffer、Selector

* 数据总是从Channel读到Buffer中，或从Buffer写到Channel中，事件 + Selector监听Channel，实现一个线程处理多个操作。每一个Channel会对应一个Buffer、一个Selector对应多个Channel，Selector通过事件决定使用哪个Channel

## Buffer

* 存储数据时使用，本质是一个数组
* 清除时本质只是把下列各个属性恢复到初始状态，数据没有被正常的擦除，而是由后面的数据覆盖
* 将数据读入Buffer后，需要调用flip方法进行反转后才能将Buffer里的数据写出来
* 可以put各种类型的数据进byteBuffer后，flip后，需要按顺序和类型进行get操作，否则会抛异常

### 重要属性

```java
// 下一个要读或写的位置索引
private int position = 0;
// 缓冲区的当前终点，<= limit，读写时位置索引不能超出limit
private int limit;
// 缓冲区容量
private int capacity;
// 标记，标记后用于重新恢复到的位置
private int mark = -1;
```

### 常用子类

每个基本类型都有对应的Buffer，比如CharBuffer、IntBuffer、DoubleBuffer、ByteBuffer（最常用）

## Channel

* 作用类似流，但流是单线的，只能读或只能写，而Channel是双向的，可以同时进行读写
* 可异步
* 通常与Buffer配合使用，也可以使用Buffer数组，当一个buffer存满时会取下一个buffer取处理

### 常用子类

* FileChannel专门处理文件相关的数据（从FileIn/OutputStream的getChannel()方法得到）

* ServerSocketChannel和SocketChannel用于处理TCP连接的数据
* DatagramChannel用于处理UDP连接的数据

读写文件的Demo，比如从一个文件里读出数据并写入另一个文件

```java
package com.nixum.nio;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class CopyFileAToB {
  public static void main(String[] args) throws Exception {
    FileInputStream fileInputStream = new FileInputStream("first.txt");
    FileChannel readFileChannel = fileInputStream.getChannel();

    FileOutputStream fileOutputStream = new FileOutputStream("second.txt");
    FileChannel writeFileChannel = fileOutputStream.getChannel();

    // 只设置成1024的容量，通过循环 + clear覆盖重写
    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
    while (true) {
      byteBuffer.clear();
      int read = readFileChannel.read(byteBuffer);
      if (read == -1) {
        break;
      }
      // 写操作前需要先flip
      byteBuffer.flip();
      writeFileChannel.write(byteBuffer);
    }
    // close
    fileInputStream.close();
    fileOutputStream.close();
  }
}
```

## Selector

* 一个线程处理多个连接，就是靠Selector，Channel需要事先注册到Selector上，Selector根据事件选择Channel进行处理。实际上是一个发布订阅模型，通过事件触发
* 只有真正有读写事件时才会进行读写，就不用为每个连接都创建一个线程了
* 避免多线程上下文切换的开销
* selectionKey，可以理解为触发selector的事件，有4种，OP_ACCEPT：有新连接产生，一般用于服务端建立连接、OP_CONNECT：连接已建立，一般用于客户端建立连接、OP_READ：读操作、OP_WRITE：写操作

NIO基本使用Demo，服务端，也可以不使用Selector，但这样就跟BIO没什么差别了

```java
package com.nixum.nio;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class NIOServer {
  private ServerSocketChannel serverSocketChannel;
  private Selector selector;

  public NIOServer(int port) {
    try {
      serverSocketChannel = ServerSocketChannel.open();
      selector = Selector.open();
      serverSocketChannel.socket().bind(new InetSocketAddress(port));
      // 需要显式设置为非阻塞
      serverSocketChannel.configureBlocking(false);
      // 注册serverSocketChannel，触发事件为 OP_ACCEPT，用于建立连接
      serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void run() throws Exception {
    while (true) {
      // 等待1s获取事件，这样一次性可以获取多个事件进行处理，如果没有则继续
      if(selector.select(1000) == 0) {
        System.out.println("这1秒内没有收到数据");
        continue;
      }
      // 监听到事件发生， 获取发生的事件集合
      Set<SelectionKey> selectionKeys = selector.selectedKeys();
      Iterator<SelectionKey> keyIterator = selectionKeys.iterator();

      // 判断事件的类型
      while (keyIterator.hasNext()) {
        SelectionKey key = keyIterator.next();
        // 触发的是OP_ACCEPT事件
        if (key.isAcceptable()) {
          connectionHandler(selector, serverSocketChannel);
        }
        // 触发OP_READ事件
        if (key.isReadable()) {
          readHandler(key);
        }
        // 将处理完的事件集合移除，防止重复操作
        keyIterator.remove();
      }
      // 处理完事件集合
    }
  }

  private void connectionHandler(Selector selector, ServerSocketChannel serverSocketChannel) throws Exception {
    // 建立连接
    SocketChannel socketChannel = serverSocketChannel.accept();
    // 将SocketChannel 设置为非阻塞
    socketChannel.configureBlocking(false);
    // 建立连接后，将socketChannel注册到selector，初始化一个Buffer用来装数据，发送OP_READ给selector，当selector收到OP_READ事件后，就会使用该socketChannel处理该Buffer
    socketChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));
  }

  private void readHandler(SelectionKey key) throws Exception {
    // 获取处理该事件的channel
    SocketChannel channel = (SocketChannel) key.channel();
    // 获取该channel关联的Buffer
    ByteBuffer buffer = (ByteBuffer) key.attachment();
    // 将channel里的数据读到buffer里
    channel.read(buffer);
    System.out.println("接收到请求是：" + new String(buffer.array()));
  }

  public static final int PORT = 8080;
  public static void main(String[] args) throws Exception{
    NIOServer server = new NIOServer(PORT);
    server.run();
  }
}
```

客户端，这里没有使用selector，直接使用socketChannel连接：

```java
package com.nixum.nio;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class NIOClient {
  public static void main(String[] args) throws Exception {
    SocketChannel socketChannel = SocketChannel.open();
    socketChannel.configureBlocking(false);
    InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", 8080);
    if (!socketChannel.connect(inetSocketAddress)) {
      // 这里的例子是为了说明如果连接和处理是异步的，非阻塞的
      while (!socketChannel.finishConnect()) {
        System.out.println("当连接还没完成时，会循环打印，但此时也在执行连接，请稍等");
      }
    }
    // 如果要使用selector，则在此处将socketChannel注册进selector，剩下处理跟上面类似
    String str = "hello, world";
    ByteBuffer buffer = ByteBuffer.wrap(str.getBytes());  // 根据str的大小wrap一个buffer
    // 发送请求
    socketChannel.write(buffer);
  }
}
```

# 线程模型

## 传统阻塞IO模型

典型的BIO例子，有一个ServiceSocket在监听端口，一个线程处理一个连接，监听端口、建立连接，read操作、业务处理、write操作这一整个过程都是阻塞的

## Reactor模型

reactor其实就是针对传统阻塞IO模型的缺点，将上述操作拆分出来异步处理，通过事件通知，由一个中心进行分发，本质就算IO复用 + 线程池，甚至单线程 + 消息队列也可以

### 1. 单Reactor单线程

![单Reactor单线程模型](https://github.com/Nixum/Java-Note/raw/master/picture/单Reactor单线程模型.jpg)

* Acceptor实际上也是一个Handler，只是处理的事件不同，当Reactor收到(select)连接事件时调用
* 当Reactor收到(select)非连接事件，比如读事件、写事件、处理其他业务的事件等，会起一个handler来处理
* 当Handler处理完当前事件后，将下一次要处理的事件和相关参数丢给Reactor进行select和dispatch
* 单线程模型，天然是线程安全的，但是当handler处理过慢时就会造成事件堆积，阻塞主线程(Reactor)，处理能力下降，因此要求handler处理尽可能的快。
* 异常处理要小心，否则会导致整个线程垮掉
* 比如上面NIO的例子就是这个模型，当业务复杂时，也可将handler抽出。不同的Handler类实现不同的业务处理，再配合对象池实现复用

### 2. 单Reactor多线程

![单Reactor多线程模型](https://github.com/Nixum/Java-Note/raw/master/picture/单Reactor多线程模型.jpg)

* 在单Reactor单线程模型的基础上，因为Handler的处理流程相对固定，就将比较耗时的业务处理包装成任务交由线程池处理，加快Handler的处理速度
* 实际上如果只是在Handler处将业务逻辑交给线程池去做，再同步等待结果，只是一种伪异步，本质上Handler还是要等任务执行完才能执行send操作。优化的方法是先将Handler存起来，把业务处理提交给线程池后，就结束handler的执行了，这样就能把主线程释放出来，处理其他事件。当线程池里的任务执行完，只需将结果、handlerId、事件交由Reactor，Reactor根据事件和HandlerId找到对应的Handler去响应结果就可以了。
* 由于业务处理使用了多线程，需要注意共享数据的问题，处理起来会比较复杂，线程安全只存在于Reactor所在的线程
* Reactor需要处理的事件变多，高并发下容易出现性能瓶颈

### 3. 主从Reactor多线程

![多Reactor多线程模型](https://github.com/Nixum/Java-Note/raw/master/picture/多Reactor多线程模型.jpg)

* 在单Reactor多线程模型的基础上，将Handler下沉处理，通过子Reactor来提高并发处理能力。Acceptor处理连接事件后，将连接分配给SubReactor处理，例如一个连接对应一个SubReactor，SubReactor负责处理连接后的业务处理，可以把这层理解为单Reactor多线程模型的Reactor
* 由于又多了一层，线程处理更加复杂，同一Reactor下才能保证线程安全，不同Reactor间要注意数据共享问题

# Netty

对NIO的包装，简化NIO的使用；实现客户端重连、闪断、半包读写、失败缓冲、网络拥塞和异常流处理；基于主从Reactor多线程模型，事件驱动

## Server端线程模型

![Netty线程模型](https://github.com/Nixum/Java-Note/raw/master/picture/netty线程模型.jpg)

## Demo

* BossGroup专门处理连接，WorkerGroup专门处理读写
* NioEventLoop是一个无限循环的线程，不断的处理事件，每一个NioEventLoop有一个selector，用于监听事件
* NioEventLoop内部串行化设计，负责消息的读取 -> 解码 -> 处理 -> 编码 -> 发送
* 一个NioEventLoopGroup包含多个NioEventLoop，每个NioEventLoop包含一个Selector，一个taskQueue
* Selector可以注册监听多个NioChannel，每个NioChannel只会绑定在唯一的NioEventLoop上，每个NioChannel都绑定有一个自己的ChannelPipeline
* 注意如果在一次连接中多次调用ChannelHandlerContext的writeAndFlush响应数据回去时，每次writeAndFlush写出去的数据会整合在一起后才响应回去，即TCP的粘包，接收端只会接收到合并后的数据包，需要特殊处理去拆包
* netty中的I/O操作是异步的，如 bind、wirte、connect方法都是返回一个ChannelFeture，可以使用ChannelFeture的sync方法将异步改为同步，或者调用其他方法来判断其状态和结果

服务端

```java
package com.nixum.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NettyServer {
    
  private static final int PORT = 8080;

  public static void main(String[] args) throws Exception {
  
    // 一个BossGroup线程池处理连接请求，设置了一个线程
    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    // 一个WorkerGroup线程池进行业务处理，默认设置的线程数是 CPU核数 * 2
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    try {
      // 创建服务端的启动对象，配置参数
      ServerBootstrap bootstrap = new ServerBootstrap();

      // 初始化设置
      bootstrap.group(bossGroup, workerGroup) // 设置两个线程组
               .channel(NioServerSocketChannel.class) // 指定NioSocketChannel为传输channel
               // .localAddress(new InetSocketAddress(PORT)) // 可以在这里设置监听端口或者设置初始化配置后使用bind()方法设置
               .option(ChannelOption.SO_BACKLOG, 128) // 设置线程队列得到连接个数
               .childOption(ChannelOption.SO_KEEPALIVE, true) //设置保持活动连接状态
               .handler(null) // 该handler对应bossGroup, childHandler对应 workerGroup
               .childHandler(new ChannelInitializer<SocketChannel>() {
                 // 当一个连接被接收时，给channelPipeline添加一个Handler
                 @Override
                 protected void initChannel(SocketChannel ch) throws Exception {
                   // 可以使用一个集合管理 SocketChannel，在推送消息时，可以将业务加入到各个channel 对应的 NIOEventLoop 的 taskQueue 或者 scheduleTaskQueue
                   // 给workGroup的eventLoop对应的channel设置handler
                   ch.pipeline().addLast(new NettyServerHandler());
                 }
               });
      System.out.println("服务器 is ready...");
      // 同步阻塞等待直到绑定完成，并监听端口
      ChannelFuture cf = bootstrap.bind(PORT).sync();
      // 设置监听器，监听连接事件
      cf.addListener(new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
          if (cf.isSuccess()) {
            System.out.println("监听端口 " + PORT + " 成功");
          } else {
            System.out.println("监听端口 " + PORT + " 成功");
          }
        }
      });
      // 获取cfChannel的CloseFuture，并阻塞当前线程直到其完成
      cf.channel().closeFuture().sync();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }
}
```

服务端业务处理器NettyServerHandler

```java
package com.nixum.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.util.CharsetUtil;

// netty通过一系列的Handler来实现业务处理，用户自定义Handler需要继承ChannelInboundHandlerAdapter才能使用
public class NettyServerHandler extends ChannelInboundHandlerAdapter {
  /*
    读取客户端发送的请求
    ChannelHandlerContext ctx: 上下文对象, 含有管道pipeline, 通道channel, 地址
    Object msg: 就是客户端发送的数据 默认Object
  */
  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
	// 这里假设有需要执行一个非常耗时的任务
	/*
    // 解决方案1: 用户程序自定义的普通任务
    ctx.channel().eventLoop().execute(() -> {
      try {
        Thread.sleep(5 * 1000);
        ctx.writeAndFlush(Unpooled.copiedBuffer("hello, 客户端2", CharsetUtil.UTF_8));
      } catch (Exception e) {
        System.out.println("发生异常" + e.getMessage());
      }
    });

    // 解决方案2: 用户自定义定时任务 -> 该任务是提交到 scheduleTaskQueue中
    ctx.channel().eventLoop().schedule(() -> {
      try {
        Thread.sleep(5 * 1000);
        ctx.writeAndFlush(Unpooled.copiedBuffer("hello, 客户端~(>^ω^<)喵4", CharsetUtil.UTF_8));
      } catch (Exception e) {
        System.out.println("发生异常" + e.getMessage());
      }
    }, 5, TimeUnit.SECONDS);
	*/

    Channel channel = ctx.channel();
    // 将 msg对象转成一个ByteBuf，这里ByteBuf是Netty提供的，不是NIO的ByteBuffer.
    ByteBuf buf = (ByteBuf) msg;
    System.out.println("客户端发送消息是: " + buf.toString(CharsetUtil.UTF_8));
    System.out.println("客户端地址: " + channel.remoteAddress());
  }

  //数据读取完毕，一般在这里需要对要发送的数据进行编码
  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    // 将数据写入到缓存，并刷新, 响应给客户端
    ctx.writeAndFlush(Unpooled.copiedBuffer("hello, 客户端", CharsetUtil.UTF_8));
  }

  //处理异常, 一般是需要关闭通道
  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    ctx.close();
  }
}

```

客户端：

```java
package com.nixum.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class NettyClient {
  public static void main(String[] args) throws Exception {

    EventLoopGroup group = new NioEventLoopGroup();
    try {
      // 创建客户端启动对象，这里使用的是BootStrap
      Bootstrap bootstrap = new Bootstrap();

      // 初始化设置
      bootstrap.group(group) //设置线程组
               .channel(NioSocketChannel.class) // 设置客户端通道的实现类
               .handler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) throws Exception {
                   // 添加Handler
                   ch.pipeline().addLast(new NettyClientHandler());
                 }
               });

      // 启动客户端去连接服务器端阻塞直至成功
      ChannelFuture channelFuture = bootstrap.connect("127.0.0.1", 8080).sync();
      // 同上
      channelFuture.channel().closeFuture().sync();
    } finally {
      group.shutdownGracefully();
    }
  }
}

```

客户端业务处理Handler

```java
package com.nixum.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

public class NettyClientHandler extends ChannelInboundHandlerAdapter {

  // 当通道就绪就会触发该方法
  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    ctx.writeAndFlush(Unpooled.copiedBuffer("hello, server", CharsetUtil.UTF_8));
  }

  //当通道有读取事件时，会触发
  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    ByteBuf buf = (ByteBuf) msg;
    System.out.println("服务器回复的消息: " + buf.toString(CharsetUtil.UTF_8));
    System.out.println("服务器的地址: " + ctx.channel().remoteAddress());
  }

  // 异常时的处理
  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    cause.printStackTrace();
    ctx.close();
  }
}
```

## 核心组件

### ServerBootstrap和Bootstrap

服务引导类，通过它设置配置（链式调用）和启动服务，ServerBootstrap用于服务端，Bootstap用于客户端

### Channel、ChannelPipeline、ChannelHandlerContext和ChannelHandler

* Channel：网络通信的组件，提供异步网络I/O操作，操作都是异步的，会返回一个ChannelFuture实例，通过注册在ChannelFuture上的监听器进行回调操作。NioServerSocketChannel用于TCP服务端、NioSocketChannel用于TCP客户端，NioDatagramChannel用于UDP连接
* ChannelHandler：是一个接口，通过实现该接口来注册到ChannelPipeline上进行使用。一般使用其出站和入站的两个适配器如ChannelOutboundHandlerAdapter和ChannelInboundHandlerAdapter或者SimpleChannelInboundHandler和SimpleChannelOutboundHandler
* ChannelPipeline：保存ChannelHandler的队列，像是责任链模式
  * 每一个Channel对应一个ChannelPipeline，一个ChannelPipeline维护了一个由ChannelHandlerContext组成的双向链表，每一个ChannelHandlerContext关联一个ChannelHandler
  * 入站事件会从链表的head往后传递到最后一个入站handler，出站事件会从链表tail往前传递到最前一个出战handler，两种类型的handler互不干扰
* ChannelHandlerContext：上下文，包含一个ChannelHandler，绑定ChannelPipeline和Channel的信息

### EventLoopGroup

包含一组EventLoop，默认设置的EventLoop线程数是 CPU核数 * 2，每个EventLoop维护一个Selector

## 一般流程

1. BossGrop本质上是一个NioEventGroup，只包含一个NioEventLoop事件循环的线程。WorkGroup本质上也是一个NioEventGroup，但它包含了 CPU*2 个NioEventEventLoop来处理连接后的业务逻辑。
2. NioEventLoop是一个死循环，不断的处理事件和消息队列的任务。
3. 初始化时将BossGrop和WorkGroup注册到ServerBootstrap并进行相应的配置(如Channel、ChannelHandler)，之后通过bind()方法绑定端口和ServerSocketChannel后启动。
4. BossGroup轮询Accept事件，获取事件后接受连接，创建一个新的NioSocketChannel，绑定ChannelPipeline，为ChannelPipeline添加ChannelHandler，注册到WorkGroup上，发送Read事件。
5. WorkGroup中一个EventLoop轮询Read事件，调用Channel的ChannelPipeline进行处理。
6. ChannelPipeline中每个节点是一个Context，用Context包装Handler，由Context组成双向链表，节点间通过AbstractChannelHandlerContext 类内部的 fire 系列方法 进行传递，入站方法叫inbound，从head节点开始，出站方法叫outbound，由tail节点开始。
7. 对于耗时的方法，一般丢给线程池处理，如上面Demo中的例子



# 参考：

[Java NIO 的前生今世 之四 NIO Selector 详解](https://segmentfault.com/a/1190000006824196)

[深入浅出NIO之Channel、Buffer](https://www.jianshu.com/p/052035037297)

[Java NIO：IO与NIO的区别](https://www.cnblogs.com/aspirant/p/8630283.html)

[尚硅谷Netty教程](https://www.bilibili.com/video/BV1DJ411m7NR)

[netty in action]()

