# 个人笔记
CSDN之类的博客里边的广告太多，写文章的时候也没有使用md方便，所以打算本地直接写后提交github随时可看。

之前大部分笔记是记在本地，要逐步把他们整理后在提交上来，慢慢更新

```
├── README.md
├── controller              # 请求处理层，只负责协议校验和协议转换
│   ├── order.go        
│   └── xxx.go
├── service                 # 业务处理层
│   ├── order.go        
│   └── xxx.go
├── thirdPlatform           # 适配层，将内部协议转化为第三方的请求协议转化、针对第三方响应做业务处理 
│   ├── factory.go          # 包含抽象工厂和通用接口、方法，缓存可以在这里做
│   ├── shopify             # 包含shopify相关的业务方法
│   │   ├── request.go      # shopify相关请求
│   │   ├── response.go     # shopify相关响应
│   │   ├── order.go        # 缓存可在加这
│   |   └── xxx.go
│   └── wix                 # 包含wix相关的业务方法
│       ├── request.go      # wix相关请求
│       ├── response.go     # wix相关响应
│       ├── order.go        
│       └── xxx.go
├── dao                     # 数据持久层，缓存可以在这做
│   │   ├── cache.go        # 缓存工具类
│   │   ├── db.go           # 直接操作数据库
│   │       ├── order.go        
│   |       └── xxx.go
│   └── order.go
└── infrastructure          # 基础服务层
│   ├── config              # 配置读取
│   │   └── config.go
│   ├── consts              # 常量
│   │   ├── common.go       # 通用常量
│   │   ├── order.go        
│   |   └── xxx.go         
│   ├── model               # model层，用于声明业务结构体
│   │   ├── bo.go              # 业务处理对象
│   │   ├── do.go              # 数据持久对象
│   │   ├── request.go         # 请求
│   │   └── response.go        # 响应
│   └── util                # 工具包
│       └── http            
├── main.go
├── configs.yaml            # configs为配置文件，当配置项变多时，最好分文件整合到一个文件夹里
├── go.mod
└── go.sum
```
