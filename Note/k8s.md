[TOC]

# docker

## 底层原理:

容器技术的核心功能，就是通过约束和修改进程的动态表现，从而为其创造出一个“边界

**Namespace 技术**：进程只能看到被规定的视图，即 隔离，比如通过docker启动一个/bin/sh，再在容器里通过ps命令查看该/bin/sh进程的pid，会发现它的pid是1，但是实际上它在外部的宿主机里的pid是10，使得让在容器里运行的进程以为自己就在一个独立的空间里，实际上只是进行了逻辑的划分，本质还是依赖宿主机

与虚拟化的区别：虚拟化是在操作系统和硬件上进行隔离，虚拟机上的应用需要经过虚拟机在经过宿主机，有两个内核，本身就有消耗，而容器化后的应用仅仅只是宿主机上的进程而已，只用到宿主机一个内核

因为namespace隔离的并不彻底，由于内核共享，容器化应用仍然可以把宿主机的所有资源都吃掉，有些资源不同通过namespace隔离，比如修改了容器上的时间，宿主机上的时间也会被改变，因此需要Cgroups

**Cgroups 技术**：是用来制造约束的主要手段，即对进程设置资源能够使用的上限，如CPU、内存

比如，限定容器只能使用宿主机20%的CPU

```shell
docker run -it --cpu-period=100000 --cpu-quota=20000 ubuntu /bin/bash
```

**Mount namespace与rootfs(根文件系统)**：挂载在容器根目录上、用来为容器进程提供隔离后执行环境的文件系统，即容器镜像

镜像可以理解为是容器的文件系统（一个操作系统的所有文件和目录），它是只读的，挂载在宿主机的一个目录上

> 上面的读写层通常也称为容器层，下面的只读层称为镜像层，所有的增删查改操作都只会作用在容器层，相同的文件上层会覆盖掉下层。知道这一点，就不难理解镜像文件的修改，比如修改一个文件的时候，首先会从上到下查找有没有这个文件，找到，就复制到容器层中，修改，修改的结果就会作用到下层的文件，这种方式也被称为copy-on-write。

## 注意点

容器是“单进程模型”，单进程模型并不是指容器只能运行一个进程，而是指容器没有管理多个进程的能力，它只能管理一个进程，即如果在容器里启动了一个Web 应用和一个nginx，如果nginx挂了，你是不知道的。

另外，直到JDK 8u131以后，java应用才能很好的运用在docker中，在此之前可能因为docker隔离出的配置和环境，导致JVM初始化默认数值出错，因此如果使用以前的版本，需要显示设置默认配置，比如直接规定堆的最大值和最小值、线程数之类的

## 容器网络

![](https://github.com/Nixum/Java-Note/raw/master/Note/picture/容器网络.png)

### 同一节点下容器间的通信

在 Linux 中，能够起到虚拟交换机作用的网络设备，是网桥（Bridge）。它是一个工作在数据链路层（Data Link）的设备，主要功能是根据 MAC 地址来将数据包转发到网桥的不同端口（Port）上，因此Docker 项目会默认在宿主机上创建一个名叫 docker0 的网桥，凡是连接在 docker0 网桥上的容器，就可以通过它来进行通信。

容器通过名叫 **Veth Pair** 的虚拟设备，即容器内的eth0网卡，将容器连接到docker0网桥上，多个容器会将其Veth Pair注册到宿主机的eth0上，Veth Pair相当于是连接不同network namespace的网线，一端在容器，一端在宿主机

网络请求实际上就是在这些虚拟设备上进行映射（经过路由表，IP转MAC，MAC转IP）和转发，到达目的地的。

所以当容器无法访问外网时，就可以检查docker0网桥是否能ping通，查看docker0和Veth Pair设备的iptables规则是否有异常

同一节点内的容器通信一般流程：容器A往容器B的IP发出请求，请求先经过容器A的eth0网卡，发送一个ARP广播，找到容器B IP对应的MAC地址，宿主机上的docker0网桥，把广播转发到注册到其身上的其他容器的eth0，容器B收到该广播后把MAC地址发给docker0，docker0回传给容器A，容器A发送数据包给docker0，docker0接收到数据包后，根据数据包的目的MAC地址，将其转发到容器B的eth0，

同一节点内，宿主机与容器通信一般流程：宿主机往容器的IP发出请求，这个请求的数据包先根据路由规则到达docker0网桥，转发到对应的Veth Pair设备上，由Veth Pair转发给容器内的应用

一个节点内的容器访问另一个节点一般流程：其实跟同一节点内，宿主机与容器通信类似，最终会转化为节点间的通信

### 不同节点下容器间的通信

对于在不同节点的容器通信，Docker 默认配置下，一台宿主机上的 docker0 网桥，和其他宿主机上的 docker0 网桥，没有任何关联，它们互相之间也没办法连通，因此需要创建一个整个集群的公用网桥，然后把集群里的所有容器都连接到这个网桥上，即每台宿主机上有一个特殊网桥来构成这个公用网桥，这个技术被称为overlay network（覆盖网络）



## dockerfile

[指令详解](https://www.cnblogs.com/panwenbin-logs/p/8007348.html)

* RUN

后面一般接shell命令，但是会构建一层镜像

要注意RUN每执行一次指令都会在docker上新键一层，如果层数太多，镜像就会太过膨胀影响性能，虽然docker允许的最大层数是127层。

有多条命令可以使用&&连接

* CMD

要注意CMD只允许有一条，如果有多条只有最后一条会生效

# Kubernetes

容器相当于进程，Kubernetes相当于操作系统

![](https://github.com/Nixum/Java-Note/raw/master/Note/picture/k8s项目架构.jpg)

kubelet 负责管理运行时的容器，这个交互的依赖是CRI的远程调用接口（接口定义了容器运行时的各项核心操作），OCI则是容器运行时对底层操作系统的规范，CRI就是将请求翻译成对底层系统的操作

kubelet 还通过 gRPC 协议同一个叫作 Device Plugin 的插件进行交互。这个插件，是 Kubernetes 项目用来管理 GPU 等宿主机物理设备的主要组件

kubelet 的另一个重要功能，则是调用网络插件和存储插件为容器配置网络和持久化存储，交互的接口是CNI和CSI

Master节点作用：编排、管理、调度用户提交的作业

## Pod

不同容器间需要共同协作，如war包和tomcat，就需要把它们包装成一个pod，概念类似于进程与进程组，pod并不是真实存在的，只是逻辑划分，同一个pod里的容器，本质上只是共享某些资源

原理：由于不同容器间可能存在依赖关系（如启动顺序的依赖），因此k8s会起一个中间容器infra，来关联其他容器，infra容器一定是最先起的，一个 Pod 只有一个 IP 地址，由Pod内容器共享，Pod 的生命周期只跟 Infra 容器一致，Infra管理共享资源

对于initContainer命令的作用是按配置顺序最先执行，执行完之后才会执行container命令，例如，对war包所在容器使用initContainer命令，将war包复制到挂载的卷下后，再执行tomcat的container命令启动tomcat以此来启动web应用，这种先启动一个辅助容器来完成一些独立于主进程（主容器）之外的工作，称为sidecar，边车

Pod可以理解为一个机器，容器是里面的进程，凡是调度、网络、储存、安全相关、跟namespace相关的属性，都是Pod级别的

### Pod在K8s中的生命周期

* Pending：Pod的yaml文件已经提交给k8s了，API对象已经被创建保存在etcd中，但是这个Pod里有容器因为某些原因导致不能被顺利创建
* Running：Pod已经调度成功，跟一个具体的节点绑定，内部容器创建成功，并且至少有一个正在运行
* Succeeded：Pod里所有容器都正常运行完毕，并且已经退出，在运行一次性任务时比较常见
* Failed：Pod里至少有一个容器以不正常的状态退出，需要查看Events和日志查看原因
* Unknown：异常状态，Pod的状态不能持续通过kubelet汇报给kube-apiserver，可能是主从节点间通信出现问题

除此之外，Pod的status字段还能细分一组Conditions，主要是描述造成当前status的具体原因

### Pod中的Projected Volume(投射数据卷)

k8s将预先定义好的数据投射进容器，支持的种类：secret、ConfigMap、Downward API、ServiceAccountToken

* Secret：将Pod想要访问的加密数据，存放到etcd中，通过在Pod中的容器里挂载Volume的方式访问这些数据

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: test-projected-volume 
spec:
  containers:
  - name: test-secret-volume
    image: busybox
    args:
    - sleep
    - "86400"
    volumeMounts:
    - name: mysql-cred
      mountPath: "/projected-volume"
      readOnly: true
  volumes:
  - name: mysql-cred
    projected:
      sources:
      - secret:
          name: user
      - secret:
          name: pass
```

这里是挂载了一个类型为projected的volume，存放数据的sources是一个secret对象(有属性name)，name的值表示数据存放的名字，比如通过这个名字取得对于的值（值可以是对应的存放数据的文件路径），

```
如：kubectl create secret generic user --from-file=./username.txt
```

也可直接在yaml文件中定义 secret对象，如下，通过data字段定义，kind是secret，值需要经过base64编码

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: mysecret
type: Opaque
data:
  user: YWRtaW4=
  pass: MWYyZDFlMmU2N2Rm
```

通过这种方式进行定义并运行后，会先将信息保存到ectd中，然后在以文件的形式挂载在容器的Volume目录里，文件名是${name}或者${data.key}

* configMap：configMap的作用、用法同secret，只是其内容不需要经过加密
* downward API：让Pod里的容器能够直接获取到这个Pod API对象本身的信息，只能获取Pod启动之前就能确定的信息，Pod运行之后的信息只能通过sidecar容器获取

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: test-downwardapi-volume
  labels:
    zone: us-est-coast
    cluster: test-cluster1
    rack: rack-22
spec:
  containers:
    - name: client-container
      ...
  volumes:
    - name: podinfo
      projected:
        sources:
        - downwardAPI:
            items:
              - path: "labels"
                fieldRef:
                  fieldPath: metadata.labels
```

这个volume的类型是projected，数据来源是downwardAPI，声明要暴露的信息是当前yaml文件定义的metadata.labels信息

* serviceAccountToken：在Pod中安装一个k8s的Client，使得可以从容器里直接访问并操作这个k8s的API，但是需要解决API Server授权问题，因此需serviceAccountToken，Pod默认挂载一个default service account

### Pod中的健康检查

对于Web应用，最简单的就是由Web应用提供健康检查的接口，我们在定义的API对象的时候设置定时请求来检查运行在容器中的web应用是否健康

```yaml
...
livenessProbe:
     httpGet:
       path: /healthz
       port: 8080
       httpHeaders:
       - name: X-Custom-Header
         value: Awesome
       initialDelaySeconds: 3
       periodSeconds: 3
```

### Pod的恢复机制

API对象中spec.restartPolicy字段用来描述Pod的恢复策略，默认是always，即容器不在运行状态则重启，OnFailure是只有容器异常时才自动重启，Never是从来不重启容器

Pod的恢复过程，永远发生在当前节点，即跟着API对象定义的spec.node的对应的节点，如果要发生在其他节点，则需要deployment的帮助

当Pod的restartPolicy是always时，Pod就会保持Running状态，无论里面挂掉多少个，因为Pod总会重启这些容器；当restartPolicy是never时，Pod里的所有容器都挂了，才会变成Failed，只有一个容器挂了也是Running

### Pod的通信

#### Pod内的容器间通信

Pod内部容器是共享一个网络命名空间的。

在Pod内部有一个默认的叫Pause的容器，作为独立共享的网络命名空间，其他容器启动时使用 -net=container就可用让当前容器加入的Pause容器，以此拥有同一个网络命名空间，所以Pod中的容器可以通过localhost来互相通信。

对容器来说，hostname就是Pod的名称。因为Pod中的所有容器共享同一个IP地址和端口空间，所以需要为每个需要接收连接的容器分配不同的端口，也就是说，Pod内的容器应用需要自己协调端口的使用。

另外，也可以使用PV和PVC来实现通信。

#### 同一节点下Pod间的通信

通过节点上的网桥和Pod上的 Veth Pair 实现，整体跟同一节点上容器间的通信 很像，只不过 Veth Pair 是挂在Pod的共享网络空间上的。Veth Pair将节点上的网桥和Pod上的共享网络空间进行连接，再通过网桥，连接不同的Pod的共享网络空间，实现不同Pod间网络通信。

![](https://github.com/Nixum/Java-Note/raw/master/Note/picture/同一节点下Pod间通信.png)

#### 不同节点下Pod间的通信

不同的节点下Pod间的通信，通过一套接口规范（CNI, Container Network Interface）来实现，常见的有Flannel、Calico以及AWS VPC CNI。

其中，flannel有VXLAN、host-gw、UDP三种实现。

**flannel UDP模式下的跨主机通信**
![](https://github.com/Nixum/Java-Note/raw/master/Note/picture/flannel_udp跨主机通信.png)

在由fannel管理的容器网络里，一个节点上的所有容器，都属于该宿主机被分配的一个子网。flannel会在宿主机上注册一个flannel0设备，保存各个节点的容器子网信息，flanneld进程会处理由flannel0传入的IP包，匹配到对应的子网，从etcd中找到该子网对应的宿主机的IP，封装成一个UDP包，交由flannel0，接着就跟节点间的网络通信一样，发送给目标节点了。因为多了一步flanneld的处理，涉及到了多次用户态与内核态间的数据拷贝，导致性能问题，优化的原则是减少切换次数，所以有了VXLAN模式、host-gw模式。

> UDP模式下，在发送IP包的过程，经过三次用户态与内核态的数据拷贝
>
> 1. 用户态的容器进程发出IP包经过docker0网桥进入内核态
> 2. IP包根据路由表进入flannel0设备，从而回到用户态的flanneld进程
> 3. flanneld进行UDP封包后重新进入内核态，将UDP包通过宿主机的eth0发送出去

**calico的跨主机通信**

简单来说，calico会在宿主机上创建一个路由表，维护集群内各个物理机，容器的路由规则，通过这张路由表实现跨主机通信

### Job

Job是一种特殊的Pod，即那些计算完成之后就退出的Pod，指状态变为complated

Job 会使用这种携带了 UID 的 Label，为了避免不同 Job 对象所管理的 Pod 发生重合，Job是直接控制Pod的

```
spec:
 backoffLimit: 5 //默认是6
 activeDeadlineSeconds: 100 //单位：秒
 parallelism: 2
 completions: 4
```

backoffLimit表示失败后的重试次数，下一次重试的动作分别发生在10s、20s、40s

activeDeadlineSeconds表示最长运行的时间，如果超过该限定时间，则会立即结束

parallelism表示一个 Job 在任意时间最多可以启动多少个 Pod 同时运行

completions表示 Job 至少要完成的 Pod 数目，即 Job 的最小完成数

Job Controller 在控制循环中进行的调谐（Reconcile）操作，是根据实际在 Running 状态 Pod 的数目、已经成功退出的 Pod 的数目，以及 parallelism、completions 参数的值共同计算出在这个周期里，应该创建或者删除的 Pod 数目，然后调用 Kubernetes API 来执行这个操作，当Job执行完处于complate状态时，并不会退出

## Controller

### Deployment

最基本的控制器对象，管理Pod的工具，比如管理多个相同Pod的实例，滚动更新

控制循环：在一个无限循环内不断的轮询集群中的对象，将其状态与期望的状态做对比后，对该对象采取相应的操作，与事件驱动的区别：事件驱动是被动型，接收到事件就执行相应的操作，事件是一次性，因此操作失败比较难处理，控制循环是不对轮询操作值只状态与期望一致的

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment
spec:
  selector:
    matchLabels:
      app: nginx
  replicas: 2
// 上面的部分定义了控制内容，判断实际与期望，并进行相应的操作，下面是被控制的对象
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx:1.7.9
        ports:
        - containerPort: 80
```

定义了一个deployment，它会检查标签为nginx的Pod，保证其数量=2，deployment根据template字段定义的容器的模板 来起相应的Pod的

deployment想要实现水平扩展/收缩，实际操控的是ReplicaSet对象，而ReplicaSet管理着定义数量的Pod

ReplicaSet表示版本，比如上面那份配置，replicas:2是一个版本，replicas:3是一个版本，这里是因为数量不同产生两个版本，每一个版本对应着一个ReplicaSet

deployment只适合控制无状态的Pod，如果是Pod与Pod之间有依赖关系，或者有状态时，deployment就不能随便杀掉任意的Pod再起新的Pod，比如多个数据库实例，因为数据库数据是存在磁盘，如果杀掉后重建，会出现实例与数据关系丢失，因此就需要StatefulSet

### StatefulSet

statefulSet通过headless service，使用这个DNS记录维持Pod的拓扑状态，因为在为Pod起名字的时候是按顺序编号的，因此可以通过编号来进行顺序启动

PVC（Persistent Volume Claim）：定义持久化卷的声明，作用类似于接口，开发人员直接使用而不用知道其具体实现，比如定义了数据库的用户名和密码之类的属性

PV（Persistent Volume）：持久化卷的具体实现，即定义了持久化数据的相关属性，如数据库类型、用户名密码

statefulSet通过PVC + PV + 编号的方式，就能实现 数据存储与Pod的绑定，当Pod被删除时，并不会删掉对应的PV，因此在起新的Pod的时候，会根据PVC找到原来的PV

>  用户提交请求创建pod，Kubernetes发现这个pod声明使用了PVC，那就靠PersistentVolumeController帮它找一个PV配对。
>
> 没有现成的PV，就去找对应的StorageClass，帮它新创建一个PV，然后和PVC完成绑定。
>
> 新创建的PV，还只是一个API 对象，需要经过“两阶段处理”变成宿主机上的“持久化 Volume”才真正有用：
> 第一阶段由运行在master上的AttachDetachController负责，为这个PV完成 Attach 操作，为宿主机挂载远程磁盘；
> 第二阶段是运行在每个节点上kubelet组件的内部，把第一步attach的远程磁盘 mount 到宿主机目录。这个控制循环叫VolumeManagerReconciler，运行在独立的Goroutine，不会阻塞kubelet主循环。
>
> 完成这两步，PV对应的“持久化 Volume”就准备好了，Pod可以正常启动，将“持久化 Volume”挂载在容器内指定的路径。  

### DaemonSet

DaemonSet 的主要作用，是让你在 Kubernetes 集群里，运行一个 Daemon Pod，这个Pod运行在k8s集群的每一个节点上，每个节点只允许一个，当有新的节点加入集群后，该Pod会在新节点上被创建出来，节点被删除，该Pod也被删除。

网络插件的Agent组件、存储插件的Agent组件等都是Daemon Pod

一般的Pod都需要节点准备好了，才可以调度上去，但是有些Pod需要在节点还没准备好的时候就需要部署上去，比如网络相关的Pod，因此需要DaemonSet的帮助，DaemonSet开始运行的时机，比k8s集群出现的时机要早。

它通过 控制循环，判断节点上是否已经运行了标签为xxx的Pod，来保证每个节点上有一个这样的Pod，在DaemonSet的API对象中通过在template Pod的API对象，使用nodeAffinity保证哪些节点需要创建这样的Pod，使用tolerations来容忍Pod在被打上污点标签的节点也可以部署，因为一般有污点的节点是不允许将Pod部署在上面的

### CronJob

如果仍然使用Deployment管理，因为它会对退出的Pod进行滚动更新，所以并不合适，因此需要使用CronJob

作用类似于Job类似于Pod，CronJob类似于Deployment

CronJob使用 spec.schedule来控制，使用jobTemplate来定义job模板，spec.concurrencyPolicy来控制并行策略

spec.concurrencyPolicy=Allow（一个Job没执行完，新的Job就能产生）、Forbid（新Job不会被创建）、Replace（新的Job会替换旧的，没有执行完的Job）

### Operator



## Service

每次Pod的重启都会导致IP发生变化，导致IP是不固定的，Service可以为一组相同的Pod套上一个固定的IP地址和端口，让我们能够以**TCP/IP**负载均衡的方式进行访问。

虽然Service每次重启IP也会发生变化，但是相比Pod

一般是pod指定一个访问端口和label，Service的selector指明绑定的Pod，配置端口映射，Service并不直接连接Pod，而是在selector选中的Pod上产生一个Endpoints资源对象，通过Service的VIP就能访问它代理的Pod了。

Service由kube-proxy组件 + kube-dns组件 + iptables共同实现。kube-proxy会为创建的service创建一条路由规则（由service到pod），并添加到宿主机的iptables中，所以请求经过Service会进行kube-proxy的转发。

创建Service时，会在Service selector的Pod中的容器注入同一namespace下所有service的ip和端口作为环境变量，该环境变量会随着Pod或Service的ip和端口的改变而改变，可以实现基于环境变量的服务发现，但是只有在同一namespace下的Pod内才可以使用此环境变量进行访问。

### 如何被集群外部访问

* service设置type=NodePort，暴露virtual IP，访问这个virtual IP的时候，会将请求转发到对应的Pod
* service设置type=LoadBalancer，设置一个外部均衡服务，这个一般由公有云提供，比如aws，阿里云的k8s服务
* service设置type=ExternalName，并设置externalName的值，这样就可以通过externalName访问，service会暴露DNS记录，通过访问这个DNS，解析得到DNS对应的VIP，通过VIP再转发到对应的Pod；此时也不会产生EndPoints、clusterIP。
* service设置externalIPs的值，这样也能通过该ip进行访问
* 可以直接通过port forward的方式，将Pod端口与节点上的端口一一对应暴露，提供访问，而不需要service

关于headless service

* headless service是指spec:clusterIP: None的service。一般用在statefulSet

* 一个service对应多个pod，通过dns访问service时，会根据dns查询出service的clusterIP，根据clusterIP + iptables转发给对应的pod，实现负载均衡。

  当service是headless service时，通过dns访问service，会根据dns查出该service关联的所有pod的ip和dns名字，由客户端来决定自己要访问哪个pod，并直接访问。通过headless service访问不会进行负载均衡。

## Ingress

作用与Service类似，主要是用于对**多个service**的包装，作为service的service，设置**一个**统一的负载均衡（这样就不用每个Service都设置一个LB了），设置Ingress **rule**，进行反向代理，实现的是**Http**负责均衡。

Ingress是反向代理的规则，Ingress Controller是负责解析Ingress的规则后进行转发。可以理解为Nginx，本质是将请求通过不同的规则进行路由转发。常见的实现有Nginx Ingress Controller。

Ingress Controller可基于Ingress资源定义的规则将客户端请求流量直接转发到Service对应的后端Pod资源上，其会绕过Service资源，省去了kube-proxy实现的端口代理开销。

### 和Istio Ingressgateway的区别

Istio Ingressgateway是Kubernate Ingress Controller的一种实现，能够支持更多方式的流量管理。

|                             | Nginx Ingress Controller              | Istio Ingressgateway                          |
| --------------------------- | ------------------------------------- | --------------------------------------------- |
| 根据HTTP Header选择路由规则 | 不支持                                | 支持                                          |
| Header规则支持正则表达式    | 不支持                                | 支持                                          |
| 服务之间设置权重拆分流量    | 不支持                                | 支持                                          |
| Header和权重规则组合使用    | 不支持                                | 支持                                          |
| 路由规则检查                | 不支持                                | 支持                                          |
| 路由规则粒度                | Service                               | Service下的不同Pod                            |
| 支持的协议                  | HTTP1.1/HTTP2/gRpc<br/>TCP/Websockets | HTTP1.1/HTTP2/gRpc<br/>TCP/Websockets/MongoDB |

## 声明式API

通过编排对象，在为它们定义服务的这种方法，就称为声明式API，

Pod就是一种API对象，每一个API对象都有一个Metadata字段，表示元数据，通过里面的labels字段（键值对）来找到这个API对象；每一个API对象都有一个Spec字段，来配置这个对象独有的配置，如为Pod添加挂载的Volume

命令式配置文件操作：编写一个yaml配置，使用kubectl create -f config.yaml创建controller和Pod，然后修改yaml配置，使用kubectl replace -f config.yaml，更新controller和Pod，kube-apiserver一次只能处理一个命令

声明式配置文件操作：编写yaml配置和更新yaml配置均使用kubectl apply -f config.yaml，kube-apiserver一次处理多个命令，并且具备merge能力

### 工作原理

k8s根据我们提交的yaml文件创建出一个API对象，一个API对象在etcd里的完整资源路径是由Group（API 组）、Version（API 版本）和 Resource（API 资源类型）三个部分组成的

![](https://github.com/Nixum/Java-Note/raw/master/Note/picture/API对象树形结构.png)

```
apiVersion: batch/v2alpha1
kind: CronJob
...
```

以上面的配置为例，CronJob是API对象的资源类型、batch是组、v2alpha1是版本

核心API对象如Pod、Node是没有Group的，k8s是直接在/api这个层级进行下一步的匹配

过程步骤

1. yaml文件被提交给APIServer，APIServer接收到后完成前置工作，如授权、超时处理、审计
2. 进入MUX和Routes流程，APIServer根据yaml提供的信息，使用上述的匹配过程，找到CronJob的类型定义
3. APIServer根据这个类型定义，根据yaml里CronbJob的相关字段，创建一个CronJob对象，同时也会创建一个SuperVersion对象，它是API资源类型所有版本的字段全集，用于处理不同版本的yaml转成的CronJob对象
4. APIServer 会先后进行 Admission() 和 Validation() 操作，进行初始化和校验字段合法性，验证过后保存在Registry的数据结构中
5. APIServer把验证过的API对象转换成用户最初提交的版本，进行序列化操作，保存在ETCD中

CRD（ Custom Resource Definition），一种API插件机制，允许用户在k8s中添加一个跟Pod、Node类型的，新的API资源，即kind为CustomResourceDefinition，类似于类的概念，这样就可以通过这个类，来创建属于这个类的实例(编写yaml文件)，这个实例就称为CR

比如有CRD为

```
apiVersion: apiextensions.k8s.io/v1beta1
kind: CustomResourceDefinition
metadata:
  name: networks.samplecrd.k8s.io
spec:
  group: samplecrd.k8s.io
  version: v1
  names:
    kind: Network
    plural: networks
  scope: Namespaced
```

CR为

```
apiVersion: samplecrd.k8s.io/v1
kind: Network
metadata:
  name: example-network
spec:
  cidr: "192.168.0.0/16"
  gateway: "192.168.0.1"
```

其中的资源类型、组、版本号要一一对应

上面这些操作只是告诉k8s怎么认识yaml文件，接着就需要编写代码，让k8s能够通过yaml配置生成API对象，以及如何使用这些配置的字段属性了，接着，还需要编写操作该API对象的控制器

控制器的原理：

![](https://github.com/Nixum/Java-Note/raw/master/Note/picture/控制器工作流程.png)

控制器通过APIServer获取它所关心的对象，依靠Informer通知器来完成，Informer与API对象一一对应

 Informer是一个自带缓存和索引机制，通过增量里的事件触发 Handler 的客户端库。这个本地缓存在 Kubernetes 中一般被称为 Store，索引一般被称为 Index。

Informer会使用Index库把增量里的API对象保存到本地缓存，并创建索引，Handler可以是对API对象进行增删改

Informer 使用了 Reflector 包，它是一个可以通过 ListAndWatch 机制获取并监视 API 对象变化的客户端封装。

Reflector 和 Informer 之间，用到了一个“增量先进先出队列”进行协同。而 Informer 与你要编写的控制循环之间，则使用了一个工作队列来进行协同

实际应用中，informers、listers、clientset都是通过CRD代码生成，开发者只需要关注控制循环的具体实现就行

## 配置相关

### Pod级别下的一些配置，即当kind: Pod

**NodeSelector**：将Pod和Node进行绑定的字段

```
如：
apiVersion: v1
kind: Pod
...
spec:
 nodeSelector:
   disktype: ssd
```

表示该Pod只能运行在携带了disktype:ssd标签的节点上，否则它将调度失败



**HostAliases**：定义了Pod的hosts文件（比如/etc/hosts）里的内容

```
spec:
  hostAliases:
  - ip: "10.1.2.3"
    hostnames:
    - "foo.remote"
    - "bar.remote"
```

表示在/etc/hosts文件的内容是将 ip 10.1.2.3映射为 foo.remote和bar.remote



**shareProcessNamespace**: true，Pod里面的容器共享PID namespace，即在同一个Pod里的容器可以相互看到对方



**hostNetwork**: true、hostIPC: true、hostPID: true表示共享宿主机的network、IPC、namespace



**ImagePullPolicy**=alaways(默认)、never、ifNotPresent，每次创建Pod都会${value}拉取镜像



**Lifecycle**: 容器状态发生变化时触发的一系列钩子，属于container级别

```
apiVersion: v1
kind: Pod
metadata:
  name: lifecycle-demo
spec:
  containers:
  - name: lifecycle-demo-container
    image: nginx
    lifecycle:
      postStart:
        exec:
          command: ["/bin/sh", "-c", "echo Hello from the postStart handler > /usr/share/message"]
      preStop:
        exec:
          command: ["/usr/sbin/nginx","-s","quit"]
```

在lifecycle里的postStart，指的是容器启动后，要执行的操作，但是它不是串行的，即在docker的ENTRYPOINT命令执行后就开始执行了，此时ENTRYPOINT命令执行的动作可能还没完成；preStart，指的是在容器被杀死之前，要执行的操作，只有这个动作执行完成，才允许容器被杀死

## 常用命令

```

查看所有Pod等，也可将pod换成cm、svc，下同
kubectl get pod -A

查看某个configmap的内容
kubectl describe configmap [confing名字] -n [命名空间]

将更新的configmap内容更新到etcd中
kubectl apply -f [文件名]

删除pod
kubectl delete pod [pod名称] -n [命名空间]

删除整个命名空间, 同时会把命名空间内所有的pod、svc、deployment等删掉
kubectl delete ns [命名空间]

查看所有Pod以及ip之类的信息
kubectl get pods --all-namespaces -o wide

进入pod里的指定容器, sh打开命令行
kubectl exec [pod名称] -n [名称空间] -c [pod内容器名称] -it sh

查看pod 的event
kubectl get event -n [名称空间]

查看容器日志
kubectl logs [pod名称] -n [名称空间] -c [pod内容器名称] -f

查看pod的yaml内容，pod、cm、svc等，也可将yaml换成json，即以json的格式查看
kubectl get pod [pod名称] -n [命名空间] -o yaml

```

# 参考

极客时间-深入剖析k8s-张磊
