---
title: Gin框架原理
description: Gin框架原理
date: 2021-03-22
lastmod: 2021-08-15
categories: ["Go"]
tags: ["Go Gin原理", "web框架"]ne
---

[TOC]

[TOC]

# 标准库net/http

demo:

定义了一个路由 `/hello`，绑定了一个handler，输出当前path，ListenAndServe方法启动web服务，第一个参数表示监听的端口，第二个参数代表 处理所有的HTTP请求 的实例，等于nil时会使用默认的`DefaultServeMux`。

该demo是基于标准库实现的Web框架入口。

```go
func main() {
	http.HandleFunc("/hello", func (w http.ResponseWriter, req *http.Request) {
		fmt.Fprintf(w, "URL.Path = %q\n", req.URL.Path)
	})
	log.Fatal(http.ListenAndServe(":8080", nil))
}

// http.ListenAndServe()方法第二个参数的实现：
type Handler interface {
	ServeHTTP(ResponseWriter, *Request)
}

// 注册handler默认使用DefaultServeMux，与ListenAndServe第二个参数使用的处理器一致。
// 将path和对应的handler方法保存在DefaultServeMux的map中，等请求进来进行匹配处理。
func HandleFunc(pattern string, handler func(ResponseWriter, *Request)) {
	DefaultServeMux.HandleFunc(pattern, handler)
}
```

`http.ListenAndServe`是整个web的总流程，本质上还是走socket那一套，bind -》 listen -》 accept -》 read、write -》 close。

```go
func ListenAndServe(addr string, handler Handler) error {
	server := &Server{Addr: addr, Handler: handler}
	return server.ListenAndServe()
}

func (srv *Server) ListenAndServe() error {
	...
	ln, err := net.Listen("tcp", addr)
	...
	return srv.Serve(ln)
}

func (srv *Server) Serve(l net.Listener) error {
    ...
    // 将listener封装进server
	ctx := context.WithValue(baseCtx, ServerContextKey, srv)
	for {
        // 无限循环，accept接收客户端的请求
		rw, err := l.Accept()
		if err != nil {
			select {
			case <-srv.getDoneChan():
				return ErrServerClosed
			default:
			}
			if ne, ok := err.(net.Error); ok && ne.Temporary() {
				... // 设置默认超时时间
				time.Sleep(tempDelay)
				continue
			}
			return err
		}
        // 封装accept到的连接，起一个goroutine来处理这个连接，之后继续等待accept的返回
		connCtx := ctx
		if cc := srv.ConnContext; cc != nil {
			connCtx = cc(connCtx, rw)
			...
		}
		tempDelay = 0
		c := srv.newConn(rw)
		c.setState(c.rwc, StateNew) // before Serve can return
		go c.serve(connCtx)
	}
}

func (c *conn) serve(ctx context.Context) {
	...
 	// 处理连接、tls处理之类的，从连接中读取数据，封装成request、response，交给serverHandler处理
    ...
    // 默认的serverHandler.ServeHTTP方法会进行判断:
    // 如果handler为空就会使用默认的DefaultServeMux，否则就用用户定义的ServeHTTP来处理请求
    serverHandler{c.server}.ServeHTTP(w, w.req)
    ...
}

// DefaultServeMux的ServeHTTP方法，对请求进行路由匹配，用的就是http.HandleFunc里的map
// 根据path找到对应的handler，执行
func (mux *ServeMux) ServeHTTP(w ResponseWriter, r *Request) {
	if r.RequestURI == "*" {
		if r.ProtoAtLeast(1, 1) {
			w.Header().Set("Connection", "close")
		}
		w.WriteHeader(StatusBadRequest)
		return
	}
    // 获取注册的路由handler
	h, _ := mux.Handler(r)
	h.ServeHTTP(w, r)
}

// 路由handler的逻辑非常简单，就是直接根据path在map里匹配找到处理的handler
func (mux *ServeMux) handler(host, path string) (h Handler, pattern string) {
	mux.mu.RLock()
	defer mux.mu.RUnlock()
	if mux.hosts {
		h, pattern = mux.match(host + path)
	}
	if h == nil {
		h, pattern = mux.match(path)
	}
	if h == nil {
		h, pattern = NotFoundHandler(), ""
	}
	return
}

func (mux *ServeMux) match(path string) (h Handler, pattern string) {
	v, ok := mux.m[path]
	if ok {
		return v.h, v.pattern
	}
	for _, e := range mux.es {
		if strings.HasPrefix(path, e.pattern) {
			return e.h, e.pattern
		}
	}
	return nil, ""
}
```

# Gin对net/http的封装

```go
// http.ListenAndServe()方法第二个参数的实现：
type Handler interface {
	ServeHTTP(ResponseWriter, *Request)
}
```

Gin做的，就是实现这个Handler接口，掌管所有HTTP请求，提供丰富的能力：如路由分发、请求上下文的复用、中间件调用链等功能。

```go
import "github.com/gin-gonic/gin"

func handlePing(c *gin.Context) {
    c.JSON(200, gin.H{
            "message": "pong",
   })
}

func main() {
    r := gin.Default()
    r.GET("/ping", handlePing)
    // run的底层仍然是http.ListenAndServe
    r.Run() // 默认监听 0.0.0.0:8080
}
// 写法有很多种，本质还是调用gin.Default()的ServeHTTP方法
func main() {
    router := gin.Default()
    router.Handle(http.MethodGet, "ping", handlePing)
    server := &http.Server{
    	Addr:    ":8080",
		Handler: router,
    }
    log.Fatal(server.ListenAndServe())
}
```

so，主要看gin.Default()方法返回的engin，由gin的engin来进行请求封装、路由匹配、转发到对应的handler处理。

```go
func (engine *Engine) ServeHTTP(w http.ResponseWriter, req *http.Request) {
	// gin.Context对象复用池
    c := engine.pool.Get().(*Context)
    // 重置gin.context对象
	c.writermem.reset(w)
	c.Request = req
	c.reset()
	// 交给engine处理请求
	engine.handleHTTPRequest(c)
	// 处理完成把context放回复用池
	engine.pool.Put(c)
}
```

# 路由



# 中间件