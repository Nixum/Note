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

* 同步非阻塞，通过缓冲区进行缓冲增加处理的灵活性，当某一线程里没有数据可用时，则去处理其他事情，保证线程不阻塞
* 由三个部分组成：Channel、Buffer、Selector

* 数据总是从Channel读到Buffer中，或从Buffer写到Channel中，事件 + Selector监听Channel，实现一个线程处理多个操作。每一个Channel会对应一个Buffer、一个Selector对应多个Channel，Selector通过事件决定使用哪个Channel

## Buffer

* 存储数据时使用，本质是一个数组
* 清除时本质只是把下列各个属性恢复到初始状态，数据没有被正常的擦除，而是由后面的数据覆盖
* 将数据读入Buffer后，需要调用flip方法进行反转后才能将Buffer里的数据写出来
* 可以put各种类型的数据进byteBuffer后，flip后，g需要按顺序和类型进行get操作，否则会抛异常

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
* 通常与Buffer配合使用

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
  public static void main(String[] args) throws Exception{
    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    Selector selector = Selector.open();
    serverSocketChannel.socket().bind(new InetSocketAddress(8080));
    // 需要显式设置为非阻塞
    serverSocketChannel.configureBlocking(false);
    // 注册serverSocketChannel，触发事件为 OP_ACCEPT，用于建立连接
    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
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
          // 建立连接
          SocketChannel socketChannel = serverSocketChannel.accept();
          // 将SocketChannel 设置为非阻塞
          socketChannel.configureBlocking(false);
          // 建立连接后，将socketChannel注册到selector，初始化一个Buffer用来装数据，发送OP_READ给selector，当selector收到OP_READ事件后，就会使用该socketChannel处理该Buffer
          socketChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));
        }
        // 触发OP_READ事件
        if (key.isReadable()) {
          // 获取处理该事件的channel
          SocketChannel channel = (SocketChannel) key.channel();
          // 获取该channel关联的Buffer
          ByteBuffer buffer = (ByteBuffer) key.attachment();
          // 将channel里的数据读到buffer里
          channel.read(buffer);
          System.out.println("接收到请求是：" + new String(buffer.array()));
        }
        // 将处理完的事件集合移除，防止重复操作
        keyIterator.remove();
      }
	  // 处理完事件集合
    }
	// 退出
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



### 2. 单Reactor多线程



### 3. 主从Reactor多线程

# Netty

对NIO的包装，简化NIO的使用，基于主从Reactor多线程模型，事件驱动

# 参考：

[Java NIO 的前生今世 之四 NIO Selector 详解](https://segmentfault.com/a/1190000006824196)

[深入浅出NIO之Channel、Buffer](https://www.jianshu.com/p/052035037297)

[Java NIO：IO与NIO的区别](https://www.cnblogs.com/aspirant/p/8630283.html)

[尚硅谷Netty教程](https://www.bilibili.com/video/BV1DJ411m7NR)

