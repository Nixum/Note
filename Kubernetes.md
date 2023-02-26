---
title: Kubernetes和Istio
description: Kubernetes和Istio架构、原理
date: 2020-07-22
lastmod: 2021-12-03
categories: ["devops"]
tags: ["Kubernetes", "Istio"]
---

[TOC]

# Kubernetes

当前Kubernetes社区对外宣传是单个集群最多支持5000个节点，Pod总数不超过150k，容器总数不超过300k，单节点Pod数量不超过100个。

## 基本

![](https://github.com/Nixum/Java-Note/raw/master/picture/k8s项目架构.jpg)

**容器的本质是进程，Kubernetes相当于操作系统**，管理这些进程组。

* CNI：Container Network Interface，容器网络接口规范，如 Flannel、Calico、AWS VPC CNI

* CRI：Container Runtime Interface，容器运行时的各项核心操作的接口规范，是一组gRPC接口。包含两类服务，镜像服务和运行时服务。镜像服务提供下载、检查和删除镜像的RPC接口；运行时服务包含用于管理容器生命周期，与容器交互的调用的RPC接口（exec / attach / port-forward等）。dockershim、containerd、cri-o都是遵循CRI的容器运行时，称为高层级运行时。

* CSI：Container Storage Interface，容器存储的接口规范，如PV、PVC

* OCI：Open Container Initiative，容器运行时和镜像操作规范，镜像规范规定高层级运行时会下载一个OCI镜像，并把它解压称OCI运行时文件系统包；运行时规范描述如何从OCI运行时文件系统包运行容器程序，并且定义其配置、运行环境和生命周期。定义新容器的namespaces、cgroups和根文件系统；它的一个参考实现是runC，称为底层级运行时。

* CRD：Custom Resource Definition，自定义的资源对象，即yaml文件中的Kind，如Operator就是实现CRD的控制器，之后直接使用Operator创建的CRD声明对象即可使用

  每一个对象都包含两个嵌套对象来描述规格（Spec）和状态（Status），对象的规格就算我们期望的目标状态，而状态描述了对象当前状态，这一部分由Kubernetes本身提供和管理，通过describe才能看到Status的信息。

  ```go
  type Deployment struct {
  	metav1.TypeMeta `json:",inline"`
  	metav1.ObjectMeta `json:"metadata,omitempty" protobuf:"bytes,1,opt,name=metadata"`
  
  	Spec DeploymentSpec `json:"spec,omitempty" protobuf:"bytes,2,opt,name=spec"`
  	Status DeploymentStatus `json:"status,omitempty" protobuf:"bytes,3,opt,name=status"`
  }
  ```

* Master节点作用：编排、管理、调度用户提交的作业
  * Scheduler：编排和调度Pod，基本原理是通过监听api-server获取待调度的pod，然后基于一系列筛选和评优，为pod分配最佳的node节点，在每次需要调度Pod时执行。
  * API-Server：提供集群对外访问的API接口实现对集群资源的CRUD以及watch，是集群中各个组件数据交互和通信的枢纽，当收到一个创建pod的请求时会进行认证、限速、授权、准入机制等检查后，写入etcd。唯一一个与etcd集群通信的组件。
  * Controller Manager：管理控制器的，比如Deployment、Job、CronbJob、RC、StatefulSet、Daemon等，核心思想是监听、比较资源实际状态与期望状态是否一致，否则进行协调。
  
* Device Plugin：管理节点上的硬件设备，比如GPU

* Worker节点作用：运行或执行用户作业

  * kubelet：负责创建、管理各个节点上运行时的容器和Pod，这个交互依赖CRI的远程调用接口，通过Socket和CRI通信；

    周期性地从API Server接收新的或者修改的Pod规范并且保证节点上的Pod和其他容器的正常运行，保证节点会向目标状态迁移，向Master节点发送宿主机的健康状态；（处理Master下发到本节点的任务，管理Pod和Pod中的容器）

    kubelet 使用cAdvisor对worker节点资源进行监控。在 Kubernetes 系统中，cAdvisor 已被默认集成到 kubelet 组件内，当 kubelet 服务启动时，它会自动启动 cAdvisor 服务，然后 cAdvisor 会实时采集所在节点的性能指标及在节点上运行的容器的性能指标。（监控和定期向Master汇报节点资源使用率）

    kubelet 还通过 gRPC 协议同一个叫作 Device Plugin 的插件进行交互。这个插件，是 Kubernetes 项目用来管理 GPU 等宿主机物理设备的主要组件；
    kubelet 的另一个重要功能，则是调用网络插件和存储插件为容器配置网络和持久化存储，交互的接口是CNI和CSI；

  * Kube-Proxy：作为daemonset部署在每个节点上，负责宿主机的子网管理，用于为Pod创建代理服务，同时也能将服务暴露给外部，其原理就是在多个隔离的网络中把请求转发给正确的Pod或容器；

    从API-Server获取所有service信息，创建Endpoints，转发service到Pod间的请求，默认使用iptables模式，但当service数量变多时有性能问题，1.8版本后使用IPVS模式提升性能；

* coreDNS：低版本的kubernetes使用kube-dns，1.12后默认使用coreDNS，用于实现域名查找功能

### 核心组件的协作流程

![](https://github.com/Nixum/Java-Note/raw/master/picture/Kubenetes组件协作流程.png)

1. 以创建Deployment为例，用户或控制器通过kubectl、RestAPI或者其他客户端向API-Server发起创建deployment的请求；
2. API-Server收到创建请求后，经过认证、鉴权、准入三个环节后，将deployment对象保存到etcd中，触发watch机制，通知API-Server，API-Server再调用ControllerManager对应的deployment控制器进行操作；
3. ControllerManager中的DeploymentController会监听API-Server中所有Deployment的事件变更，当收到该deployment对象的创建事件后，就会检查当前namespace中所有的ReplicaSet对象，判断其属性是否存在该deployment对象，如果没有，DeploymentController会向API-Server发起创建ReplicaSet对象的请求，经过API-Server的检查后，将该ReplicaSet对象保存在etcd中，触发watch机制通知API-Server调用控制器...；
4. 同上，会触发ReplicaSet Controller的检查，最后创建一个Pod对象，保存在etcd中；
5. Scheduler监听到API-Server中有新的Pod对象被创建，就会查看Pod是否被调度，如果没有，Scheduler就会给它分配一个最优节点，并更新Pod对象的`spec.nodeName`属性，随后该Pod对象被同步回API-Server里，并保存在etcd中；
6. 最后，节点的kubelet会一直监听API-Server的Pod资源变化，当发现有新的Pod对象分配到自己所在的节点时，kubelet就会通过gRPC通信，通过CRI向容器运行时创建容器，运行起来。

## API-Server

> API-Server 作为统一入口，任何对数据的操作都必须经过 API-Server。API-Server负责各个模块之间的通信，集群里的功能模块通过API-Server将信息存入到etcd中，etcd 存储集群的数据信息，其他模块通过API-Server读取这些信息，实现来模块之间的交互。

主要场景：

1. 节点上的 Kubelet 会周期性的调用API-Server的接口，上报节点信息，API-Server将节点信息更新到etcd中
2. 节点上的 Kubelet 会通过API-Server上的Watch接口，监听Pod信息，判断Pod是要从本节点进行调度、删除还是修改
3. Controller Manager的Node-Controller模块会通过API-Server的Watch接口，监控节点信息，进行相应的处理
4. Scheduler通过API-Server的Watch接口，监听Pod的信息进行调度

### 原理

API-Server本质上也是控制器那一套，通过List-Watch和缓存机制，来解决被大量调用的问题，保证消息实时性、可靠性、顺序性和性能，另外其他模块也是差不多的机制，对集群信息进行了缓存，通过List-Watch进行更新进行保证。

### 流程

api-server在收到请求后，会进行一系列的执行链路：

1. 认证：校验发起请求的用户身份是否合法，支持多种方式如x509客户端证书认证、静态token认证、webhook认证
2. 限速：默认读 400/s，写 200/s，1.19版本以前不支持根据请求类型进行分类、按优先级限速，1.19版本以后支持将请求按重要程度分类限速，支持多租户，可有效保障Leader选举之类的高优先级请求得到及时响应，防止一个异常client导致整个集群被限速。
3. 审计：记录用户对资源的详细操作行为
4. 授权：检查用户是否有权限对其访问的资源进行相关操作，支持RBAC、ABAC、webhook，1.12版本后默认授权机制是RBAC
5. 准入控制：提供在访问资源前拦截请求的静态和动态扩展能力，如镜像拉取策略。
6. 与etcd交互

## 调度器Scheduler

主要职责就是为新创建的Pod寻找合适的节点，默认调度器会先调用一组叫Predicate的调度算法检查每个Node，再调用一组叫Priority的调度算法为上一步结果里的每个Node打分和排序，将新创建的Pod调度到得分最高的Node上。

### 原理

![](https://github.com/Nixum/Java-Note/raw/master/picture/k8s默认调度原理.png)

* 第一个控制循环叫Informer Path，它会启动一系列Informer，监听etcd中的Pod、Node、Service等与调度相关的API对象的变化，将新创建的Pod添加进调度队列，默认的调度队列是优先级队列；

  此外，还会对调度器缓存进行更新，因为需要尽最大可能将集群信息Cache化，以提高两个调度算法组的执行效率，调度器只有在操作Cache时，才会加锁。

* 第二个控制循环叫Scheduling Path，是负责Pod调度的主循环，它会不断从调度队列里出队一个Pod，调用**Predicate算法**（预选）进行过滤，得到可用的Node，Predicate算法需要的Node信息，都是从Cache里直接拿到；

  然后调用**Priorities算法**（优选）为这些选出来的Node进行打分，得分最高的Node就是此次调度的结果。

* 得到可调度的Node后，调度器就会将Pod对象的nodeName字段的值，修改为Node的名字，实现绑定，此时修改的是Cache里的值，之后才会创建一个goroutine异步向API Server发起更新Pod的请求，完成真正的绑定工作；这个过程称为乐观绑定。

* Pod在Node上运行起来之前，还会有一个叫Admit的操作，调用一组GeneralPredicates的调度算法验证Pod是否真的能够在该节点上运行，比如资源是否可用，端口是否占用之类的问题。

Scheduler内置各个阶段的插件，Predicate预选阶段和Priorities优选阶段就算遍历回调插件求出可用的Node结果。所以当节点很多的时候，如果所有节点都需要参与预先调度的过程，就会导致调度的延时很高。

![](https://github.com/Nixum/Java-Note/raw/master/picture/K8s调度器框架扩展点.png)

> 比如大集群有 2500 个节点, 注册的插件有 10 个, 那么 筛选 Filter 和 打分 Score 过程需要进行 2500 * 10 * 2 = 50000 次计算, 最后选定一个最高分值的节点来绑定 pod. k8s scheduler 考虑到了这样的性能开销, 所以加入了百分比参数控制参与预选的节点数.
>
> `numFeasibleNodesToFind` 方法根据当前集群的节点数计算出参与预选的节点数量, 把参与 Filter 的节点范围缩小, 无需全面扫描所有的节点, 这样避免 k8s 集群 nodes 太多时, 造成无效的计算资源开销.
>
> `numFeasibleNodesToFind` 策略是这样的, 当集群节点小于 100 时, 集群中的所有节点都参与预选. 而当大于 100 时, 则使用下面的公式计算扫描数. scheudler 的 `percentageOfNodesToScore` 参数默认为 0, 源码中会赋值为 50 %.
>
> ```
> numAllNodes * (50 - numAllNodes/125) / 100
> ```

> 整个 kubernetes scheduler 调度器只有一个协程处理主调度循环 `scheduleOne`, 虽然 kubernetes scheduler 可以启动多个实例, 但启动时需要 leaderelection 选举, 只有 leader 才可以处理调度, 其他节点作为 follower 等待 leader 失效. 也就是说整个 k8s 集群调度核心的并发度为 1 个.
>
> 云原生社区中有人使用 kubemark 模拟 2000 个节点的规模来压测 kube-scheduler 处理性能及时延, 测试结果是 30s 内完成 15000 个 pod 调度任务. 虽然 kube-scheduler 是单并发模型, 但由于预选和优选都属于计算型任务非阻塞IO, 又有 `percentageOfNodesToScore` 参数优化, 最重要的是创建 pod 的操作通常不会太高并发. 这几点下来单并发模型的 scheduler 也还可以接受的.

### 调度策略

调度的本质是过滤，通过筛选所有节点组，选出符合条件的节点。

Predicates阶段：

> `findNodesThatFitPod` 方法用来实现调度器的预选过程，其内部调用插件的 PreFilter 和 Filter 方法来筛选出符合 pod 要求的 node 节点集合。

* GeneralPredicates算法组，最基础的调度策略，由Admit操作执行
  * PodFitsResources：检查节点是否有Pod的requests字段所需的资源
  * PodFitsHost：检查节点的宿主机名称是否和Pod的`spec.nodeName`一致
  * PodFitsHostPorts：检查Pod申请的宿主机端口`spec.nodePort`是否已经被占用
  * PodMatchNodeSelector：检查Pod的nodeSelector或nodeAffinity指定的节点是否与待考察节点匹配
* Volume的检查
  * NodeDiskConflict：检查多个Pod声明的持久化Volume是否有冲突，比如一个AWS EBS不允许被多个Pod使用
  * MaxPDVolumeCountPredicate：检查节点上某一类型的持久化Volume是否超过设定值，超过则不允许同类型Volume的Pod调度到上面去
  * VolumeZonePredicate：检查持久化Volume的可用区(Zone)标签，是否与待考察节点的标签匹配
  * VolumeBindingPredicate：检查Pod对应的PV的nodeAffinity是否与某个节点的标签匹配
* 与Node相关的规则
  * PodToleratesNodeTaints：检查Pod的Toleration字段是否与Node的Taint字段匹配
  * NodeMeemoryPressurePredicate：检查当前节点的内存是否充足
* 与Pod相关的规则，与GeneralPredicates类似
  * PodAffinityPredicate：检查待调度的Pod与Node上已有的Pod的亲和(affinity)和反亲和(anti-affinity)的关系

筛选出可用Node之后，为这些Node进行打分

Priorities阶段(打分规则)：

> `prioritizeNodes` 方法为调度器的优选阶段的实现. 其内部会遍历调用 framework 的 PreScore 插件集合里 `PeScore` 方法, 然后再遍历调用 framework 的 Score 插件集合的 `Score` 方法. 经过 Score 打分计算后可以拿到各个 node 的分值.

* LeastRequestedPriority：选出空闲资源（CPU和Memory）最多的宿主机。

  `score = (cpu((capacity - sum(requested))10 / capacity) + memory((capacity-sum(requested))10 / capacity)) / 2`

* BalancedResourceAllocation：调度完成后，节点各种资源分配最均衡的节点，避免出现有些节点资源被大量分配，有些节点则很空闲。

  `score = 10 - variance(cpuFraction, memoryFraction, volumeFraction) * 10，Fraction=Pod请求资源 / 节点上可用资源，variance=计算每两种Faction资源差最小的节点`

可以修改调度器的配置，让调度器在Predicates或Priorities阶段选择不同的策略。

### 常见的调度方式

* Deployment或RC：该调度策略主要功能就是自动部署一个容器应用的多份副本，以及持续监控副本的数量，在集群内始终维持用户指定的副本数量。

* NodeSelector：定向调度，当需要手动指定将Pod调度到特定Node上，可以通过Node的标签（Label）和Pod的nodeSelector属性相匹配。

* NodeAffinity亲和性调度：

  * requiredDuringSchedulingIgnoredDuringExecution：硬规则，必须满足指定的规则，调度器才可以调度Pod至Node上（类似nodeSelector，语法不同）。

  * preferredDuringSchedulingIgnoredDuringExecution：软规则，优先调度至满足的Node的节点，但不强求，多个优先级规则还可以设置权重值。

* Taints和Tolerations（污点和容忍）：

  * Taint：使Node拒绝特定Pod运行；

  * Toleration：为Pod的属性，表示Pod能容忍（运行）标注了Taint的Node。

### 优先级与抢占机制

给Pod设置优先级，使得高优先级的Pod可用先调度，即使调度失败也比低优先级的Pod有优先调度的机会。

需要先定义一个PriorityClass的API对象才能给Pod设置优先级。

```yaml
apiVersion: scheduling.k8s.io/v1beta1 
kind: PriorityClass 
metadata: 
  name: high-priority 
value: 1000000 
globalDefault: false 
description: "This priority class should be used for high priority service pods only."
---
Pod内
spec:
  containers:
    ...
  priorityClassName: high-priority
```

PriorityClass里的value值越高，优先级越大，优先级是一个32bit的整数，最大不超过 10亿，超过10亿的值是Kubernetes保留给系统Pod使用，保证系统Pod不会被用户Pod抢占；globalDefault为true表示该PriorityClass的值会成为系统默认值，为false表示只有使用了该PriorityClass的Pod才有优先级，没有声明则默认是0。

#### 抢占过程

当一个高优先级的Pod调度失败时，调度器会试图从当前集群里寻找一个节点，当该节点上的一个或多个低优先级的Pod被删除后，待调度的高优先级的Pod可用被调度到该节点上，但是抢占过程不是立即发生，而只是在待调度的高优先级的Pod上先设置`spec.nominatedNodeName=Node名字`，等到下一个调度周期再决定是否针对要运行在该节点上，之所以这么做是因为被删除的Pod有默认的30秒优雅退出时间，在这个过程中，可能有新的更加被适合给高优先级调度的节点加入。

#### 抢占原理

抢占算法的实现基于两个队列，一个是activeQ队列，存放下一个调度周期里需要调度的Pod；另一个是unschedulableQ队列，存放调度失败的Pod，当一个unschedulableQ里的Pod被更新之后，调度器就会把该Pod移动到activeQ里；

1. 当调度失败时，抢占算法开始工作，首先会检查调度失败的原因，判断是否可以为调度失败的Pod寻找一个新节点；
2. 之后调度器会把自己缓存的所有节点信息复制一份，通过这份副本模拟抢占过程，找出可以使用的节点和需要被删除的Pod列表，确定抢占是否可以发生；
3. 执行真正的抢占工作，检查要被删除的Pod列表，把这些Pod的nominatedNodeName字段清除，为高优先级的Pod的nominatedNodeName字段设置为节点名称，之后启动一个协程，移除需要删除的Pod。

第2步和第3步都执行了抢占算法Predicates，只有这两遍抢占算法都通过，才算抢占成功，之所以要两次，是因为需要满足InterPodAntiAffinity规则和下一次调度周期不一定会调度在该节点的情况。

### 自定义调度

K8s有默认的调度器，get Pod -o yaml时能看到， `pod.spec.schedulerName: default-scheduler`，所以可以设置该值来选择具体某一个调度器来实现调度。

有两种方式实现自定义调度：

* 通过K8s默认调度器Plugin机制实现，scheduling-framework在调度周期和绑定周期提供了很多丰富的扩展点，在这些扩展点上可以实现自定义调度逻辑。

  自定义插件有三个步骤：1、写一个go程序，实现调度器的插件接口；2、编写配置文件`KubeSchedulerConfiguration`；3、将自定义插件编译进默认调度器；

* K8s也支持部署多个调度器，也可以基于scheduling-framework编写一个新的调度器，以pod的形式部署到集群，通过pod的`pod.spec.schedulerName`指定调度器，自定义的调度器只能以主备的方式部署，Leader只能有一个，保证不会并发处理；

## Kubelet和CRI

Kubelet本质也是一个控制循环SyncLoop，在这个控制循环里又包含了很多小的控制循环，每个控制循环有自己的职责，比如Volume Manager、Image Manager、Node Status Manager、CPU Manager等，而驱动整个主的控制循环的事件有四种：

1. Pod的更新事件
2. Pod生命周期变化
3. Kubelet本身设置的执行周期
4. 定期的清理事件

每个节点上的Kubelet里的SyncLoop主控制循环通过Watch机制，监听nodeName是自己节点的Pod，在内存里缓存这些Pod的信息和状态，当接收到新事件时，执行相应的操作。在管理Pod内的容器时，不会直接调用docker的API，而是通过CRI的grpc接口来间接执行，这样无论底层容器可以简单的从docker换成其他容器程序。

每台工作节点上的docker容器会使用dockershim提供CRI接口，来与kubelet交互，如果不是使用docker容器，则需要额外部署CRI shim来处理，通过这些shim转发kubelet的请求来操作容器。

CRI是对容器操作相关的接口，而不是对于Pod，分为两组类型的API，

1. RuntimeService：处理容器相关操作，比如创建和启动容器，删除容器，执行命令
2. ImageService：处理容器镜像的相关操作，比如拉取镜像、删除镜像

![](https://github.com/Nixum/Java-Note/raw/master/picture/CRI_work_flow.png)

### exec命令执行原理

对标docker的exec命令，docker的exec原理：一个进程可以选择加入某个进程已有的Namespace中，从而达到进入容器的目的。

![](https://github.com/Nixum/Java-Note/raw/master/picture/kubectl_exec_命令原理.png)

这里以docker作为容器引擎为例：

1. kubectl执行exec命令后，首先发送Get请求到API-Server，获取pod的相关信息；
2. 获取到信息后，kubectl再发送Post请求，API-Server返回`101 upgrade`响应给kubectl，表示切换到 SPDY 协议。SPDY协议允许在单个TCP连接上复用独立的 stdin / stdout / stderr / spdy-err 流。
3. API-Server找到对应的Pod和容器，将Post请求转发到节点的Kubelet，再转发到向对应容器的Docker shim请求一个流式端点URL，并将exec请求转发到Docker exec API。Kubelet再将这个URL以 redirect 的方式返回给API-Server，请求就会重定向到对应的Streaming Server上发起exec请求，并维护长连接，之后就是在这个长连接的基础上执行命令和获取结果了。

除了exec命令意外，其他的如 attach、port-forward、logs 等命令也是类似的模式。

### 节点NotReady原因

notReady一般是因为节点的Kubelet无法与API-Server通信，导致没办法把节点的信息及时同步给API-Server，此时节点就会被标记为NotReady，所以问题就变成了，为啥Kubelet无法与API-Server通信，原因：

* Kubelet启动失败
* 网络组件故障，导致无法通信
* 集群网络分区
* 节点OOM
* Kubelet与容器通信时响应超时，死锁，导致无法处理其他事情
* 节点上Pod的数量太多，导致relist操作无法在3分钟内完成

当节点出现NotReady时，节点上的容器依然可以提供服务，只是脱离了Kubenetes的调度，不会做任何改变，此时Kubernetes会将NotReady节点上的Pod调度到其他正常的节点上。

时间段：

* Kubelet默认每**10s**会上报一次节点的数据到API-Server，可以通过参数`--node-status-update-frequency`配置上报频率。

* Controller-Manager会定时检查Kubelet的状态，默认是**5s**，可以通过参数`--node-monitor-period`配置。

* Kubelet在更新状态失败时，会进行重试，默认是**5次**，可通过参数`nodeStatusUpdateRetry`配置。

* 当节点失联一段时间后，节点就会被判定为NotReady状态，默认时间是**40s**，可通过参数`--node-monitor-grace-period`配置；

* NotReady一段时间后，节点的状态转为UnHealthy状态，默认时间是**1m**，可通过参数`--node-startup-grace-period`配置；

* 当节点UnHealthy状态一段时间后，节点上的Pod将被转移，默认是**5m**，可通过参数`--pod-eviction-timeout`配置。

由于Controller-Manager和Kubelet是异步工作，可能存在信息获取的延迟，所以Pod整体被迁移的时间也会有延迟。

Kubernetes 1.13版本后，节点处于不同的状态时将被打上不同的污点，节点上的Pod就可以基于污点在不同的状态被驱逐了，这个时候Pod被迁移的时间就不定了。

## etcd

etcd作为Kubernetes的元数据存储，API-Server是唯一直接跟etcd交互的组件，API-Server对外提供的监听机制底层实现就是etcd的watch。

### 资源存储格式

资源以 `prefix + / + 资源类型 + / + namespace + / + 具体资源名称` 组成，作为key。基于etcd提供的范围查询能力，支持按具体资源名称查询、namespace查询。默认的prefix是 /registry。

对于基于label查询，是由API-Server通过范围查询遍历etcd获取原始数据，然后再通过label过滤。

### 资源创建流程

创建资源时，会经过 BeforeCreate 策略做etcd的初始化工作，Storgae.Create接口调用etcd进行存储，最后在经过AfterCreate和Decorator。

由于put并不是并发安全的接口，并发时可能导致key覆盖，所以API-Server会调用Txn接口将数据写入etcd。

当资源信息写入etcd后，API-Server就会返回给客户端了。资源的真正创建是基于etcd的Watch机制。

controller-manager内配备多种资源控制器，当触发etcd的Watch机制时会通知api-server，api-server在调用对应的控制器进行资源的创建。比如，当我们创建一个deployment资源写入etcd，通过Watch机制，通知给api-server，api-server调用controller-manager里的deployment-controller创建ReplicaSet资源对象，写入etcd，再次触发Watch机制，经过api-server调用ReplicaSet-controller，创建一个待调度的Pod资源，写入etcd，触发Watch机制，经过api-server，scheduler监听到待调度的Pod，就会为其分配节点，通过api-server的Bind接口，将调度后的节点IP绑定到Pod资源上。kubelet通过同样的Watch机制感知到新建的Pod，发起Pod创建流程。

Kubernetes中使用Resource Version实现增量监听逻辑，避免客户端因为网络等异常出现中断后，数据无法同步的问题；同时，客户端可通过它来判断资源是否发生变化。

在Get请求中ResourceVersion有三种取值：

* 未指定，默认为空串：API-Server收到后会向etcd发起共识读/线性读，获取集群最新数据，所以在集群规模较大时，未指定查的性能会比较差。

* =字符串0：API-Server收到后会返回任意资源版本号的数据，优先返回最新版本；一般情况下先从API-Server缓存中获取数据返回给客户端，有可能读到过期数据，适用于一致性要求不高的场景。

* =非0字符串：API-Server收到后，会保证Cache中的最新ResouorceVersion大于等于请求中的ResourceVersion，然后从Cache中查询返回。

  Cache的原理是基于etcd的Watch机制来更新，=非0字符串且Cache中的ResourceVersion没有大于请求中的ResourceVersion时，会进行最多3秒的等待。

> 若使用的Get接口，那么API-Server会取资源key的ModRevision字段填充Kubernetes资源的ResourceVersion字段（ v1. meta/ ObjectMeta.ResourceVersion）。若你使用的是List接口，API-Server会在查询时，使用etcd当前版本号填充ListMeta.ResourceVersion字段（ v1. meta/ ListMeta.ResourceVersion）。
>

在Watch请求中ResourceVersion的三种取值：

* 未指定，默认为空串：一是为了帮助客户端建立初始状态，将当前已存在的资源通过Add事件返回给客户端；二是会从当前版本号开始监听，后续新增写请求导致数据变化时会及时推给客户端。
* =字符串0：帮助客户端建立初始状态，但它会从任意版本号开始监听，接下来的行为和 未指定 时一致。
* =非0字符串：从精确的版本号开始监听，只会返回大于等于精确版本号的变更事件。

## Pod

Pod是最小的API对象。Pod中的容器会作为一个整体被Master打包调度到一个节点上运行。

由于不同容器间需要共同协作，如war包和tomcat，就需要把它们包装成一个pod，概念类似于进程与进程组，pod并不是真实存在的，只是逻辑划分。

**同一个pod里的所有容器，共享同一个Network Namespace，也可以共享同一个Volume**。

这种把多个容器组合打包在一起管理的模式也称为容器设计模式。

另外还有静态Pod：

> 静态pod是由kubelet进行管理的仅存在于特定Node的Pod上，他们不能通过API-Server进行管理，无法与ReplicationController、Deployment或者DaemonSet进行关联，并且kubelet无法对他们进行健康检查。静态Pod总是由kubelet进行创建，并且总是在kubelet所在的Node上运行。

### 原理

由于不同容器间可能存在依赖关系（如启动顺序的依赖），因此k8s会起一个中间容器Infra容器，来关联其他容器，infra容器一定是最先起的，其他容器通过Join Network Namespace的方式与Infa容器进行关联。

**一个 Pod 只有一个 IP 地址，由Pod内容器共享，Pod 的生命周期只跟 Infra 容器一致，Infra管理共享资源**。

kubelet会为每个节点上创建一个基本的cbr0网桥，并为每一个Pod创建veth虚拟网络设备，绑定一个IP地址，同一个Pod中的所有容器会通过这个网络设备共享网络，从而能通过localhost相互访问彼此暴露的端口和服务。

对于initContainer命令的作用是按配置顺序最先执行，执行完之后才会执行container命令，例如，对war包所在容器使用initContainer命令，将war包复制到挂载的卷下后，再执行tomcat的container命令启动tomcat以此来启动web应用，这种先启动一个辅助容器来完成一些独立于主进程（主容器）之外的工作，称为sidecar，边车。

Pod可以理解为一个机器，容器是里面的进程，凡是调度、网络、储存、安全相关、跟namespace相关的属性，都是Pod级别的。

### 关于Pause容器 / Infra容器

pause容器，也叫Infra容器，每个Pod都会自动创建的容器，但不属于用户自定义容器。因为Pod的作用，就是管理Pod内多个容器之间最高效的共享某些资源和数据，而Pod内的容器是被namespace和cgroups隔开的。Infra容器在Pod中担任Linux Namespace共享的基础，启用Pid namespace，开启init进程。Infra容器可使得Pod内的容器共享pid namespace、network namespace、ipc namespace(用于共享systemV IPC或POSIX消息队列进行通信)、uts namespace(共享主机名)、共享Pod级别的Volumes。

以network namespace为例，如果里面只有用户自己的容器，没有Infra容器这个第三方容器，只要network的owner容器退出了，Pod内的其他容器的网络就都异常了，这显然不合理。所以在同一个Pod内的其他容器通过 join namespace的方式加入到Infra container的netword namespace中，这样，Pod内的所有容器共享同一份网络资源，Pod的IP地址 = Pod第一次创建的Infra container的IP地址。

Infra container是一个只有700KB左右的镜像，永远处于pause状态。整个Pod的生命周期等于Infra container的生命周期，与内部的其他容器无关，且Infra conatiner一定是第一个启动的，这也是Kubernetes里运行更新Pod里的某一个镜像，而整个Pod不会被重建和重启的原因。即使把Pod里的所有用户容器都kill掉，Pod也不会退出。

### Pod在K8s中的生命周期

* Pending：Pod的yaml文件已经提交给k8s了，API对象已经被创建保存在etcd中，但是这个Pod里有容器因为某些原因导致不能被顺利创建。
* Running：Pod已经调度成功，跟一个具体的节点绑定，内部容器创建成功，并且至少有一个正在运行。
* Succeeded：Pod里所有容器都正常运行完毕，并且已经退出，在运行一次性任务时比较常见。
* Failed：Pod里至少有一个容器以不正常的状态退出，需要查看Events和日志查看原因。
* Unknown：异常状态，Pod的状态不能持续通过kubelet汇报给API-Server，可能是主从节点间通信出现问题。

除此之外，Pod的status字段还能细分一组Conditions，主要是描述造成当前status的具体原因，比如PodScheduled、Ready、Initialized以及Unschedulable。

在Pod的containers定义中，有个lifecycle字段，用于定义容器的状态发生变化时产生的hook。

### Side Car

在声明容器时，使用initContainers声明，用法同containers，initContainers作为辅助容器，必定比containers先启动，如果声明了多个initContainers，则会按顺序先启动，当所有initContainers都运行成功后，才会开始初始化Pod的各自信，并创建和启动containers。

因为Pod内所有容器共享同一个Network Namespace的特性，initContainers辅助容器常用于与Pod网络相关的配置和管理，比如常见的实现是Istio。

### Pod中的Projected Volume(投射数据卷)

k8s将预先定义好的数据投射进容器，支持的种类：secret、ConfigMap、Downward API，这三种PV一般存放不经常更新的数据，ServiceAccountToken则是在访问Kubernetes API Server时会使用到。

#### Secret

将Pod想要访问的加密数据，存放到etcd中，通过在Pod中的容器里挂载Volume的方式访问这些数据。如果etcd里的这些数据发生了改变，挂载到容器里的数据会在一定的延时后进行更新。

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

通过这种方式进行定义并运行后，会先将信息保存到ectd中，然后在以文件的形式挂载在容器的Volume目录里，文件名是${name}或者${data.key}。

#### configMap

configMap的作用、用法同secret，只是其内容不需要经过加密

#### downward API

让Pod里的容器能够直接获取到这个Pod API对象本身的信息，只能获取Pod启动之前就能确定的信息，Pod运行之后的信息只能通过sidecar容器获取。

比如下面这份配置，volume的类型是projected，数据来源是downwardAPI，声明要暴露的信息是当前yaml文件定义的metadata.labels信息，K8S会自动挂载为容器里的/podInfo/labels文件。

Downward API支持的字段是固定的，使用fieldRef字段可以查看宿主机名称、IP、Pod的标签等信息；resourceFieldRef字段可以查看容器CPU、内存等信息。

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
      volumeMounts:
      - name: podinfo
        mountPath: "/podInfo"
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

#### serviceAccountToken

serviceAccountToken是一种特殊的Secret，一般用于访问Kubernetes API Server时提供token作为验证的凭证。Kubernets提供了一个默认的default Service Account，任何运行在Kubernets中的Pod都可以使用，无需显式声明挂载了它。

默认挂载路径：`/var/run/secrets/kubernetes.io/serviceaccount`，里面包含了`ca.crt，namespace，token`三个文件，用于授权当前Pod访问API Server。

如果要让Pod拥有不同访问API Server的权限，就需要不同的service account，也就需要不同的token了。

#### 挂载宿主机上的目录

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: two-containers
spec:
  restartPolicy: Never
  containers:
  - name: nginx-container
    image: nginx
    volumeMounts:
    - name: shared-data
      mountPath: /usr/share/nginx/html
  - name: debian-container
    image: debian
    volumeMounts:
    - name: shared-data
      mountPath: /pod-data
      command: ["/bin/sh"]
      args: ["-c", "echo Hello from the debian container > /pod-data/ index.html"]
  volumes:
  - name: shared-data
    hostPath:
      path: /data
```

声明了两个容器，都挂载了shared-data这个Volume，且该Volume是hostPath，对应宿主机上的/data目录，所以么，nginx-container 可 以 从 它 的`/usr/share/ nginx/html` 目 录 中， 读取到debian-container生 成 的 index.html文件。

### Pod的资源分配 QoS

Pod的资源分配由定义的Container决定，比如

```yaml
...
spec:
  containers:
  - name: app
    resources:
      requests:
        memory: "64Mi" # 单位是bytes，注意1Mi=1024*1024，1M=1000*1000
        cpu: "250m" # 单位是个数，250m表示250millicpu，使用0.25个CPU的算力，也可以直接写成0.25，默认是1，且是cpu share的
      limits:
        memory: "128Mi"
        cpu: "500m"
...
```

CPU属于可压缩资源，当CPU不足时，Pod只会"饥饿"，不会退出；

内存数与不可压缩资源，当内存不足时，Pod会因为OOM而被kill掉；

Matser的kube-scheduler会根据requests的值进行计算，根据limits设置cgroup的限制，即request用于调度，limit用于限制，不同的requests和limits设置方式，会将Pod划分为不同的QoS类型，用于对Pod进行资源回收和调度。QoS按优先级从高到低：

1. Guaranteed：只设置了limits或者limits和requests的值一致；

   在保证是Guaranteed类型的情况下，requests和limits的CPU和内存设置相等，此时是cpuset设置，容器会绑到某个CPU核上，不会与其他容器共享CPU算力，减少CPU上下文切换的次数，提升性能。

   **绑核只会在设置的值是整数时才会生效。**

2. Burstable：不满足Guaranteed级别，但至少有一个Container设置了requests；

3. Best-Effort：requests和limits都没有设置，当节点资源充足时可以充分使用，但是当节点被Guaranteed或Burstable Pod抢占时，资源就会被压缩；

当设置`limits`而没有设置`requests`时，Kubernetes 默认令`requests`等于`limits`。

K8s会将节点上的CPU资源分成两类：共享池和独享池，如果在Guaranteed上设置了绑核，就会占用独享池，否则是使用共享池，所以，如果一个节点上有比较多的Pod设置绑核，会导致非绑核的Pod都使用共享池，使得竞争严重，使用共享池的Pod性能下降。

节点上除了运行用户容器，还运行了其他系统进程，比如kubelet、ssh等，为了保证这些外部的进程有足够的资源运行，Kubernetes引入 Eviction Policy特性，设置了默认的资源回收阈值，当Kubernetes所管理的不可压缩资源短缺时，就会触发Eviction机制，不可压缩节点资源有：内存、磁盘、容器运行镜像的存储空间等。

> 每个运行状态容器都有其 OOM 得分，得分越高越会被优先杀死；其中Guaranteed级别的Pod得分最低，BestEffort级别的Pod得分最高，所以优先级Guaranteed> Burstable> Best-Effort，优先级低的Pod会在资源不足时优先被杀死；同等级别优先级的 Pod 资源在 OOM 时，与自身的 requests 属性相比，其内存占用比例最大的 Pod 对象将被首先杀死。

**kubelet默认的资源回收阈值**：

`memory.available < 100Mi`；`nodefs.available < 10%`；`nodefs.inodesFree < 5%`；`imagefs.available < 15%；`

达到阈值后，会对node设置状态，避免新的Pod被调度到这个node上。

> 当发生资源（Eviction）回收时的策略：
>
> 首当其冲的，自然是BestEffort类别的Pod。
>
> 其次，是属于Burstable类别、并且发生“饥饿”的资源使用量已经超出了requests的Pod。 
>
> 最后，才是Guaranteed类别。并且，Kubernetes会保证只有当Guaranteed类别的Pod的资源使用量超过了其limits的限制，或者宿主机本身正处于Memory Pressure状态时，Guaranteed的Pod才可能被选中进行Eviction操作。

### Pod中的健康检查

对于Web应用，最简单的就是由Web应用提供健康检查的接口，我们在定义的API对象的时候设置定时请求来检查运行在容器中的web应用是否健康，主要是为了防止服务未启动完成就被打入流量或者长时间未响应依然没有重启等问题。

livenessProbe：保活探针，当不满足检查规则时，直接重启Pod；

readnessProbe：只读探针，当不满足检查规则时，不放流量进Pod；

startupProbe：启动检查探针，应用一些启动缓慢的业务，避免业务长时间启动而被上面两类探针kill掉；

```yaml
...
livenessProbe:
     httpGet: # 除此之外还有Exec、TCPSocket两种不同的probe方式
       path: /healthz
       port: 8080
       httpHeaders:
       - name: X-Custom-Header
         value: Awesome
       initialDelaySeconds: 3
       periodSeconds: 3
```

### Pod的恢复机制

API对象中`spec.restartPolicy`字段用来描述Pod的恢复策略，默认是always，即容器不在运行状态则重启，OnFailure是只有容器异常时才自动重启，Never是从来不重启容器

Pod的恢复过程，永远发生在当前节点，即跟着API对象定义的`spec.node`的对应的节点，如果要发生在其他节点，则需要deployment的帮助。

当Pod的restartPolicy是always时，Pod就会保持Running状态，无论里面挂掉多少个，因为Pod总会重启这些容器；当restartPolicy是never时，Pod里的所有容器都挂了，才会变成Failed，只有一个容器挂了也是Running。

不能简单的依赖Pod的status字段，而是要通过livenessProbe或者readnessProbe来的健康检查来判断是否需要恢复。

### PodPreset

预置类型的Pod，是Pod配置文件上追加字段的预置模板，PodPreset里定义的内容，只会在Pod对象被创建之前追加在这个对象本身，不会影响任何Pod控制器的定义。

一个Pod可以对应多个PodPreset，多个PodPreset间会进行合并，如果有冲突字段，则后面执行的PodPreset不会修改前面的字段。

```yaml
apiVersion: settings.k8s.io/v1alpha1
kind: PodPreset
metadata:
  name: allow-database
spec: 
  selector: 
    matchLabels: 
      role: frontend 
  env: 
    - name: DB_PORT 
      value: "6379" 
  volumeMounts: 
    - mountPath: /cache 
      name: cache-volume 
  volumes: 
    - name: cache-volume 
      emptyDir: {}
```

在先创建完这个API对象后，在创建对应Pod，这个PodPreset会根据selector，选择lables中有role: frontend的Pod，为其添加env、volumeMounts、volumes等声明。

### Pod的通信

Kubernetes上的CNI网桥相当于docker0网桥，容器(Pod)与宿主机通信，仍然使用VethPair设备，CNI网桥会接管所有CNI插件负责的容器(Pod)，主要是与Pod中的Infra容器的Network Namespace交互。

CNI网络方案主要由两部分工作

* 1：实现网络方案，比如创建和配置所需要的虚拟设备、配置路由表、ARP和FDB表；
* 2：实现对应的CNI插件，比如通过该插件可以配置Infra容器里的网络栈，并把它连接在CNI网桥上。

本质上Kubernetes的网络跟docker是类似的，在集群中，所有的容器都可以直接使用IP地址与其他容器通信，而无需IP映射；宿主机也可直接使用IP与所有容器通信，而无需IP映射；每个容器都拥有自己的IP地址，且在其他容器，宿主机看到的是一样的。

#### Pod内的容器间通信

Pod内部容器是共享一个网络命名空间的。

在Pod内部有一个默认的叫Pause的容器，作为独立共享的网络命名空间，其他容器启动时使用 -net=container就可用让当前容器加入的Pause容器，以此拥有同一个网络命名空间，所以Pod中的容器可以通过localhost来互相通信。

对容器来说，hostname就是Pod的名称。因为Pod中的所有容器共享同一个IP地址和端口空间，所以需要为每个需要接收连接的容器分配不同的端口，也就是说，Pod内的容器应用需要自己协调端口的使用。

另外，也可以使用PV和PVC来实现通信。

#### 同一节点下Pod间的通信

通过节点上的网桥和Pod上的 Veth Pair 实现，整体跟同一节点上容器间的通信 很像，只不过 Veth Pair 是挂在Pod的共享网络空间上的。Veth Pair将节点上的网桥和Pod上的共享网络空间进行连接，再通过网桥，连接不同的Pod的共享网络空间，实现不同Pod间网络通信。

![](https://github.com/Nixum/Java-Note/raw/master/picture/同一节点下Pod间通信.png)

#### 不同节点下Pod间的通信

不同的节点下Pod间的通信，通过一套接口规范（CNI, Container Network Interface）来实现，常见的有CNI插件实现有Flannel、Calico以及AWS VPC CNI。

其中，flannel有VXLAN、host-gw、UDP三种实现。

**flannel UDP模式下的跨主机通信**

下图container-1发送请求给container-2流程:

1. container-1发送数据包，源：100.96.1.2，目标：100.96.2.3，经过docker0，发现目标IP不存在，此时会把该数据包交由宿主机处理。
2. 通过宿主机上的路由表，发现flannel0设备可以处理该数据包，宿主机将该数据包发送给flannel0设备。
3. flannel0设备（TUM）由flanneld进程管理，数据包的处理从内核态(Linux操作系统)转向用户态(flanneld进程)，flanneld进程知道目标IP在哪个节点，就把该数据包发往node2。
4. node2对该数据包的处理，则跟node1相反，最后container2收到数据包。

![](https://github.com/Nixum/Java-Note/raw/master/picture/flannel_udp跨主机通信.png)

flanneld通过为各个宿主机建立子网，知道了各个宿主机能处理的IP范围，子网与宿主机的对应关系，都会保存在etcd中，flanneld将原数据包再次封装成一个UDP包，同时带上目标节点的真实IP，发往对应节点。

再由fannel管理的容器网络里，一个节点上的所有容器，都属于该宿主机被分配的一个子网。flannel会在宿主机上注册一个flannel0设备，保存各个节点的容器子网信息，flanneld进程会处理由flannel0传入的IP包，匹配到对应的子网，从etcd中找到该子网对应的宿主机的IP，封装成一个UDP包，交由flannel0，接着就跟节点间的网络通信一样，发送给目标节点了。因为多了一步flanneld的处理，涉及到了多次用户态与内核态间的数据拷贝，导致性能问题，优化的原则是减少切换次数，所以有了VXLAN模式、host-gw模式。

> UDP模式下，在发送IP包的过程，经过三次用户态与内核态的数据拷贝
>
> 1. 用户态的容器进程发出IP包经过docker0网桥进入内核态
> 2. IP包根据路由表进入flannel0设备，从而回到用户态的flanneld进程
> 3. flanneld进行UDP封包后重新进入内核态，将UDP包通过宿主机的eth0发送出去

**flannel VXLAN模式下的跨主机通信**

通过VXLAN模式（Virtual Extensible LAN）解决UDP模式下上下文切换频繁带来的性能问题。

原理是通过在二层网络上再设置一个VTEP设备，该设备是Linux内核中一个模块，可以在内核态完成数据的封装和解封。flannel.1设备（VTEP）既有IP地址又有MAC地址，在数据包发往flannel.1设备时，通过二层数据帧，将原数据包加上目标节点的MVC地址，再加上VTEP标识，封装成一个二层数据帧，然后再封装成宿主机网络里的普通UDP数据包，发送给目标节点，目标节点在内核网络栈中发现了VTEP标识，就知道可以在内核态处理了。

![](https://github.com/Nixum/Java-Note/raw/master/picture/flannel_vxlan跨主机通信.png)

数据包的发送都要经过OSI那几层模型的，经过的每一层都需要进行包装和解封，才能得到原始数据，在这期间二层网络(数据链路层)是在内核态处理，三层网络(网络层)是在用户态处理。

**flannel host-gw模式（三层网络方案）**

二层指的是在知道下一跳的IP对应的MAC地址后，直接在数据链路层通信，如果不知道，就需要在网络层设置路由表，通过路由通信，此时就是三层网络。

将每个Flannel子网的下一跳设置成该子网对应的宿主机的IP地址，用这台宿主机充当网关，Flannel子网和主机信息都保存在ETCD中，由flanneld进程WATCH这些数据的变化，实时更新路由表，这种方案的性能最好。

![](https://github.com/Nixum/Java-Note/raw/master/picture/flannel_host_gw跨主机通信.png)

**calico的跨主机通信**

类似flannel host-gw模式，calico会在宿主机上创建一个路由表，维护集群内各个物理机、容器的路由规则，通过这张路由表实现跨主机通信。通过边界网关协议BGP，在集群的各个节点中实现路由信息共享。

因此calico不需要在宿主机上创建任何网桥设备，通过Veth Pair设备 + 路由表的方式，即可完成节点IP寻找和转发。

但这种方案会遇到路由表的规模问题，且最优情况是集群节点在同一个子网。

![](https://github.com/Nixum/Java-Note/raw/master/picture/calico跨主机通信.png)

### Pod中的网络隔离

通过NetworkPolicy对象来实现，控制Pod的入站和出站请求，实现了NetworkPolicy的网络插件包括Calico、Weave，不包括Flannel。通过NetworkPolicy Controller监控NetworkPolicy，修改宿主机上的iptable。

```yaml
apiVersion: networking.k8s.io/v1 
kind: NetworkPolicy 
metadata: 
  name: test-network-policy 
  namespace: default 
spec: 
  podSelector: 
    matchLabels: 
      role: db # 标签为role: db的Pod拥有该NetworkPolicy，如果为空，则该namespace下所有Pod都无法通信
  policyTypes: 
    - Ingress 
    - Egress 
  ingress: 
  - from: # from数组内元素是or关系，元素内可以包含其他Selector，他们是and关系
    - ipBlock:
        cidr: 172.17.0.0/16 # 只允许这个网段的节点访问，且不允许172.17.1.0/24网段的节点访问
        except:
          - 172.17.1.0/24
    - namespaceSelector: 
        matchLabels: # 只允许标签为 project: myproject 的Pod访问
          project: myproject 
    - podSelector: 
        matchLabels: 
          role: frontend 
    ports: # 只允许访问这些pod的6379端口
    - protocol: TCP 
      port: 6379 
  egress: 
  - to:
    - ipBlock: 
        cidr: 10.0.0.0/24 # 只允许这些Pod对外访问这个网段的节点，且端口是5978
    ports: 
    - protocol: TCP 
      port: 5978
```

## PV、PVC、StoageClass

* PVC（Persistent Volume Claim）：定义持久化卷的声明，作用类似于接口，开发人员直接使用而不用知道其具体实现，比如定义了数据库的用户名和密码之类的属性。

  PVC的命名方式：`<PVC名字>-<StatefulSet名字>-<编号>`，StatefulSet创建出来的所有Pod都会使用此PVC，Kubernetes通过Dynamic Provisioning的方式为该PVC匹配对应的PV。

* PV（Persistent Volume）：持久化卷的具体实现，即定义了持久化数据的相关属性，如数据库类型、用户名密码。

* StorageClass：创建PV的模板，只有同属于一个StorageClass的PV和PVC，才可以绑定在一起，K8s内默认有一个名字为空串的DefaultStorageClass。

  StorageClass用于**自动**创建PV，StorageClass的定义比PV更加通用，一个StorageClass可以对应多个PV，这样就无需手动创建多个PV了。

集群中的每一个卷在被Pod使用时都会经历四个操作：附着Attach、挂载Mount、卸载Unmount和分离Detach；

> 如果 Pod 中使用的是 EmptyDir、HostPath 这种类型的卷，那么这些卷并不会经历附着和分离的操作，它们只会被挂载和卸载到某一个的 Pod 中； EmptyDir、HostPath、ConfigMap 和 Secret，这些卷与所属的 Pod 具有相同的生命周期，对于ConfigMap和Sercet，在挂载时会创建临时的volume，随着Pod的删除而删除，而不会影响本身的ConfigMap和Secret对象；
>
> 如果使用的云服务商提供的存储服务，这些持久卷只有附着到某一个节点之后才可以被挂在到相应的目录下，不过在其他节点使用这些卷时，该存储资源需要先与当前的节点分离。

### PVC与PV的绑定条件

1. PV和PVC的spec字段要匹配，比如存储的大小
2. PV和PVC的storageClassName字段必须一样

> Volume 的创建和管理在 Kubernetes 中主要由卷管理器 VolumeManager 和 AttachDetachController 和 PVController三个组件负责。其中VolumeManager 会负责卷的创建和管理的大部分工作，而 AttachDetachController 主要负责对集群中的卷进行 Attach 和 Detach，PVController 负责处理持久卷的变更。
>
> 当用户提交请求创建pod，Kubernetes发现这个pod声明使用了PVC，那就靠PersistentVolumeController帮它找一个PV配对。
>
> 如果没有现成的PV，就去找对应的StorageClass，帮它新创建一个PV，然后和PVC完成绑定。
>
> 新创建的PV，还只是一个API 对象，需要经过“两阶段处理”变成宿主机上的“持久化 Volume”才真正有用：
> 第一阶段Attach：由运行在master上的AttachDetachController负责，为这个PV完成 Attach 操作，为宿主机挂载远程磁盘；
> 第二阶段Mount：运行在每个节点上kubelet组件的内部，把第一步attach的远程磁盘 mount 到宿主机目录。这个控制循环叫VolumeManagerReconciler，运行在独立的Goroutine，不会阻塞kubelet主循环。
>
> 完成这两步，PV对应的“持久化 Volume”就准备好了，Pod可以正常启动，将“持久化 Volume”挂载在容器内指定的路径。当需要卸载时，则先Unmount再进行Dettach。

### 访问模式

由卷的提供商提供，比如AWS的EBS中的GP2、GP3磁盘，是ReadWriteOnce模式，所以一个卷只能被一个节点使用，否则会出现写冲突，即使用pod的复制集只能设置为1，且更新策略为recreate，保证只有一个pod使用才行；而如果是ReadWriteMany模式，比如AWS的EFS（EFS本质上是AWS对NFS的一次封装），则允许多个pod共享同一个持久卷

* ReadWriteOnce：卷可以被一个节点以读写方式挂载，允许同一个节点上的多个Pod访问；
* ReadOnlyMany：卷可以被多个节点以只读方式挂载；
* ReadWriteMany：卷可以被多个节点以读写方式挂载；
* ReadWriteOncePod：卷可以被单个 Pod 以读写方式挂载；

### 回收策略

当PVC对象被删除时，kubernetes就需要对卷进行回收。

* Retain：保留PV中的数据，如果PV想被重新使用，系统管理员就需要删除被使用的PV对象，并手动清除存储和相关存储上的数据。
* Delete：PV和相关的存储会被自动删除，如果当前PV上的数据确实不再需要，使用此策略可以节省手动处理的时间并快速释放无用的资源。

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata: 
  name: web 
spec: 
  serviceName: "nginx" 
  replicas: 2 
  selector: 
    matchLabels: 
      app: nginx 
  # 声明Pod
  template: 
    metadata: 
      labels: 
        app: nginx 
    spec: 
      containers: 
      - name: nginx 
        image: nginx:1.9.1 
        ports: 
        - containerPort: 80 
          name: web 
        volumeMounts: 
        - name: www 
          mountPath: /usr/share/nginx/html
  # 声明挂载的PVC
  volumeClaimTemplates: 
  - metadata: 
      name: www 
    spec: 
      accessModes: 
        - ReadWriteOnce 
      resources: 
        requests: 
          storage: 10Gi
---
# 声明要挂载的PV
apiVersion: v1
kind: PersistentVolume
metadata:
  name: nginx-pv
  labels:
    app: nginx
spec:
  capacity:
    storage: 500Gi
  volumeMode: Filesystem
  accessModes:
    - ReadWriteMany
  persistentVolumeReclaimPolicy: Retain
  storageClassName: efs-sc
  csi:
    driver: efs.csi.aws.com
    volumeHandle: fs-09b712ab658f73e6f::fsap-xxx
---
# 声明要挂载的StorageClass
kind: StorageClass
apiVersion: storage.k8s.io/v1
metadata:
  name: efs-sc
provisioner: efs.csi.aws.com
```

## 控制器模型

常见的控制器有Deployment、Job、CronbJob、ReplicaSet、StatefulSet、DaemonSet等，他们是用于创建和管理Pod的实例，能够在集群层面提供复制、发布以及健康检查等功能，这些控制器都运行在Kubernetes集群的Master节点上，这些控制器会随Controller Manager的启动而运行，监听集群状态的变更来调整对应对象的状态。

### 原理

控制循环：在一个无限循环内不断的轮询集群中的对象，将其状态与期望的状态做对比后，对该对象采取相应的操作。实际状态来自集群本身，如Kubelet汇报的容器状态、节点状态，监控系统的监控数据；期望状态来自用户提交的yaml文件；对象X指的是Pod或是其他受控制器控制的对象。

```go
for { 
    实际状态 := 获取集群中对象X的实际状态（ Actual State）
    期望状态 := 获取集群中对象X的期望状态（ Desired State）
    if 实际状态 == 期望状态{
        什么都不做 
    } else { 
        执行编排动作，将实际状态调整为期望状态 
    } 
}
```

与事件驱动的区别：事件驱动是被动型，接收到事件就执行相应的操作，事件是一次性，因此操作失败比较难处理，控制循环是不断轮询判断实际状态是否与期望一致。

### Deployment

最基本的控制器对象，管理Pod的工具，比如管理多个相同Pod的实例，Deployment和ReplicaSet都能控制Pod的数量，但一般我们使用的还是Deployment，因为其还有其他功能，比如滚动更新、回滚、暂停和恢复等。

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment
spec:
  paused: false # 暂停：false
# 通过spec.selector.matchLabels根据Pod的标签选择Pod
  selector:
    matchLabels:
      app: nginx
  replicas: 2
# 上面的部分定义了控制内容，判断实际与期望，并进行相应的操作，下面是被控制的对象
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
  strategy:
    type: RollingUpdate
    rollingUpdate:
      # 可使用值也可以使用百分比，使用百分比时，与replca值相乘得到最终的值
      maxUnavailable: 25% # 在更新过程中能够进入不可用状态的Pod的最大值
      maxSurge: 25% # 能额外创建的Pod的个数
```

Deployment想要实现水平扩展/收缩，实际操控的是ReplicaSet对象，而ReplicaSet管理着定义数量的Pod，所以它是一种三层结构，Deployment -> ReplicaSet -> 多个平行的Pod，Deployment是一个两层控制器，Deployment控制的是RepliocaSet的版本，ReplicaSet控制的是Pod的数量。

ReplicaSet表示版本，比如上面那份配置，`replicas: 2`是一个版本，`replicas: 3`是一个版本，这里是因为数量不同产生两个版本，每一个版本对应着一个ReplicaSet，由Deployment管理。

ReplicaSet是ReplicationController的升级版，都用于确保运行指定数量的Pod副本（像滚动更新、回滚、启动、暂停之类的功能则由Deployment提供），ReplicationController只支持基于等式的selector，比如`xxx = yyy或xxx != yyy`，而ReplicaSet支持基于集合的selector，比如 `matchExpressions: {key: tier, operator: In, values: [frontend]}`。

当我们修改Deployment的replicas字段时，会触发水平扩展/收缩，修改`template.Image`或者版本号时，就会触发滚动更新，默认是滚动更新RollingUpdate，另外还能设置成重建Recreate，表示Deployment在更新Pod时，先kill掉所有在运行的Pod，然后创建新的Pod。

Controller Manager中的DeploymentController作为管理Deployment资源的控制器，会在启动时通过Informer监听Pod、ReplicaSet和Deployment的变更，一旦变更就会触发DeploymentController中的回调；

初始化创建Deployment时，Kubernetes会创建了一个ReplicaSet，并按用户的需求创建了对应数量的Pod副本，当Deployment更新时，Kubernetes会再创建一个新的ReplicaSet，然后按照相同的更新策略逐个调整，将老的ReplicaSet的副本数-1，新ReplicaSet的副本数+1，直到新的ReplicaSet的副本数符合预期，老的ReplicaSet副本数缩减为0。

```yaml
# 设置更新策略
...
spec:
  ...
  strategy: 
    type: RollingUpdate # 滚动更新策略
    rollingUpdate: 
      maxSurge: 1  # 指定Desired数量，
      maxUnavailable: 1 # 一次更新中，可以删除的旧的Pod的数量
```

Deployment只适合控制无状态的Pod，如果是Pod与Pod之间有依赖关系，或者有状态时，deployment就不能随便杀掉任意的Pod再起新的Pod，比如多个数据库实例，因为数据库数据是存在磁盘，如果杀掉后重建，会出现实例与数据关系丢失，因此就需要StatefulSet。

### StatefulSet

StatefulSet可以解决两种情况下的状态：

* 拓扑状态，如果PodA和PodB有启动的先后顺序，当它们被再次创建出来时也会按照这个顺序进行启动，且新创建的Pod和原来的Pod拥有同样的网络标识（比如DNS记录），保证在使用原来的方式通信也可行。

  StatefulSet通过Headless Service，使用这个DNS记录维持Pod的拓扑状态。在声明StatefulSet时，在`spec.serviceName`里指定Headless Service的名称，因为serviceName的值是固定的，StatefulSet在为Pod起名字的时候又会按顺序编号，为每个Pod生成一条DNS记录，通过DNS记录里的Pod编号来进行顺序启动。

  StatefulSet只会保证DNS记录不变，Pod对应的IP还是会随着重启发生改变的。

* 存储状态，PodA第一次读取到的数据，隔了一段时间后读取到的仍然是同一份，无论其他有没有被重建过。

  StatefulSet通过PVC + PV + 编号的方式，实现 数据存储与Pod的绑定。每个Pod都会根据编号与对应的PVC绑定，当Pod被删除时，并不会删掉对应的PV，因此在起新的Pod的时候，会根据PVC找到原来的PV。

**StatefulSet直接管理Pod**，每个Pod不再认为只是复制集，而是会有hostname、名字、编号等的不同，并生成对应的带有相同编号的DNS记录，对应的带有相同编号的PVC，保证每个Pod都拥有独立的Volume。

对于一个副本数为n的StatefulSet，Pod被部署时是按照0~n-1的序号顺序创建的，会等待前一个Pod变为Running和Ready才会启动下一个Pod。使用配置`spec.podManagementPolicy: "Parallel"`则可以使得Pod同时创建。

StatefulSet的滚动更新，会按照与Pod编号相反的顺序，逐一更新，如果发生错误，滚动更新会停止；

StatefulSet支持按条件更新，通过对`spec.updateStrategy.rollingUpdate的partition字段`进行配置，可实现金丝雀部署或灰度发布。比如：`{"spec":{"updateStrategy":{"type":"RollingUpdate","rollingUpdate":{"partition":3}}}}`进行设置后，再继续对`spec.template`里进行修改，StatefulSet会对序号大于或等于此序号的Pod进行更新，小于此序号的保存旧版本不变，即使这些Pod被删除和重建，也是用的旧版本。

也可以设置`spec.updateStrategy.type: OnDelete`时，当`spec.template`发生变化时，Pod不会进行更新，需要手动删除，让StatefulSet来重新产生Pod，才会使用新的设置。

StatefulSet可用于部署有状态的应用，比如有主从节点MySQL集群，在这个case中，虽然Pod会有相同的template，但是主从Pod里的sidecar执行的动作不一样，而主从Pod可以根据编号来实现，不同类型的Pod存储通过PVC + PV实现。

StatefulSet的删除支持级联和非级联两种，非级联删除时，只会删除StatefulSet控制器，不会删除对应的Pod，需要使用参数`--cascade=orphan`。当StatefulSet被重新创建时，会再执行一遍Pod的创建，同时下掉之前非级联删除的Pod。**非级联删除StatefulSet时，不会删除其对应的Pod相关联的PV卷**。

StatefulSet主要由StatefulSetController、StatefulSetControl和StatefulPodControl三个组件协作来完成；StatefulSet的管理，StatefulSetController会同时从PodInformer和ReplicaSetInformer中接受增删查改事件并将事件推送到队列中。

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: web
spec:
  serviceName: "nginx"
  replicas: 2
  selector:
    matchLabels:
      app: nginx
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: k8s.gcr.io/nginx-slim:0.8
        volumeMounts:
        - name: www
          mountPath: /usr/share/nginx/html
  volumeClaimTemplates:
  - metadata:
      name: www
    spec:
      accessModes: [ "ReadWriteOnce" ]
      resources:
        requests:
          storage: 1Gi
```

### DaemonSet

一般的Pod都需要节点准备好了(即node的状态是Ready)才可以调度上去，但是有些Pod需要在节点还没准备好的时候就需要部署上去，比如网络相关的Pod，因此需要使用DaemonSet。

DaemonSet 的会在Kubernetes 集群里的每个节点都运行一个 Daemon Pod，每个节点只允许一个，当有新的节点加入集群后，该Pod会在新节点上被创建出来，节点被删除，该Pod也被删除。

DaemonSet Controller通过 控制循环，在etcd上获取所有Node列表，判断节点上是否已经运行了标签为xxx的Pod，来保证每个节点上只有一个。可以通过在Pod上声明nodeSelector、nodeAffinity、tolerations字段告诉控制器如何选择node。

* 在node上打上标签，即可通过nodeSelector选择对应的node；
* nodeAffinity的功能比nodeSelector强大，支持更加灵活的表达式来选择节点；
* tolerations来容忍Pod在被打上污点标签的节点也可以部署，因为一般有污点的节点是不允许将Pod部署在上面的。

**DaemonSet是直接管理Pod的**，DaemonSet所管理的Pod的调度过程，都由它自己完成，而不是通过Kube-Scheduler完成， 是因为DaemonSet在创建Pod时，会为其增加`spce.nodeName`字段，此时以及明确了该Pod要运行在哪个节点，就不需要kube-scheduler来调度了，但也带了问题，无论节点可不可用，DaemonSet都会将该Pod往上面调度。

DaemonSet的应用一般是网络插件的Agent组件、存储插件的Agent组件、节点监控组件、节点日志收集等。

```yaml
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: fluentd-elasticsearch
  namespace: kube-system
spec:
  selector:
    matchLabels:
      name: fluentd-elasticsearch
  template:
    metadata:
      labels:
        name: fluentd-elasticsearch
    spec:
      containers:
      - name: fluentd-elasticsearch
        image: k8s.gcr.io/fluentd-elasticsearch:1.20
        volumeMounts:
        - name: varlog
          mountPath: /var/log
          readOnly: true
      volumes:
      - name: varlog
        hostPath:
          path: /var/log
```

### Job

Job是一种特殊的Pod，即那些计算完成之后就退出的Pod，状态变为complated。

Job 会使用这种携带了 UID 的 Label，为了避免不同 Job 对象所管理的 Pod 发生重合，Job是直接控制Pod的。

```yaml
spec:
 backoffLimit: 5 //默认是6
 activeDeadlineSeconds: 100 //单位：秒
 parallelism: 2
 completions: 4
```

* backoffLimit表示失败后的重试次数，下一次重试的动作分别发生在10s、20s、40s

* activeDeadlineSeconds表示最长运行的时间，如果超过该限定时间，则会立即结束

* parallelism表示一个 Job 在任意时间最多可以启动多少个 Pod 同时运行

* completions表示 Job 至少要完成的 Pod 数目，即 Job 的最小完成数

Job Controller 在控制循环中进行的调谐（Reconcile）操作，是根据实际在 Running 状态 Pod 的数目、已经成功退出的 Pod 的数目，以及 parallelism、completions 参数的值共同计算出在这个周期里，应该创建或者删除的 Pod 数目，然后调用 Kubernetes API 来执行这个操作，当Job执行完处于complate状态时，并不会退出。

### CronJob

如果仍然使用Deployment管理，因为它会对退出的Pod进行滚动更新，所以并不合适，因此需要使用CronJob，作用类似于Job类似于Pod，CronJob类似于Deployment；

Cronjob会每隔10s从api-server中取出资源并进行检查是否触发调度创建新的资源，所以**Cronjob并不能保证在准确的目标时间执行，有一定的滞后性**；

CronJob使用 `spec.schedule` 来控制，使用jobTemplate来定义job模板，`spec.concurrencyPolicy`来控制并行策略

`spec.concurrencyPolicy`：=Allow：一个Job没执行完，新的Job就能产生、=Forbi：新Job不会被创建、Replace：新的Job会替换旧的，没有执行完的Job；

```yaml
apiVersion: batch/v1beta1
kind: CronJob
metadata:
  name: order-cronjob-auto-close-order
  namespace: regoo
  labels:
    app: order
spec:
  schedule: "*/5 * * * *"
  successfulJobsHistoryLimit: 1
  failedJobsHistoryLimit: 1
  completions: 1 # 当前任务要等待1个pod的成功执行
  parallelism: 1 # 最多有1个并发执行的任务，此时所有任务会依次顺序进行，只有前一个成功才能执行下一个
  concurrencyPolicy: Forbid
  startingDeadlineSeconds: 120
  suspend: false # 是否暂停
  jobTemplate:
    spec:
      template:
        metadata:
          annotations:
            sidecar.istio.io/inject: "false"
          labels:
            app: order
        spec:
          containers:
            - image: curlimages/curl:7.77.0
              name: curl
              imagePullPolicy: IfNotPresent
              command: ["curl", "-X", "POST", "http://order:8080/order/schedule/auto_close_order"]
          restartPolicy: OnFailure
```

### Operator 与 控制器原理

本质是一个控制器，控制自定义的CRD的流程控制，执行自定义CRD的行为。

CRD（ Custom Resource Definition），一种API插件机制，允许用户在k8s中添加一个跟Pod、Node类型的，新的API资源，即kind为CustomResourceDefinition，类似于类的概念，这样就可以通过这个类，来创建属于这个类的实例(编写yaml文件)，这个实例就称为CR。

比如有CRD为

```yaml
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

```yaml
apiVersion: samplecrd.k8s.io/v1
kind: Network
metadata:
  name: example-network
spec:
  cidr: "192.168.0.0/16"
  gateway: "192.168.0.1"
```

其中的资源类型、组、版本号要一一对应

上面这些操作只是告诉k8s怎么认识yaml文件，接着就需要编写代码，让k8s能够通过yaml配置生成API对象，以及如何使用这些配置的字段属性了，接着，还需要编写操作该API对象的控制器。

**关于Webhook**

Operator中的Webhook，外部对CRD资源的变更，在Controller处理之前都会交给Webhook提前处理，即：Webhook的处理早于Controller的Reconcile方法执行。

Webhook的职责是修改和验证，可以集成在Operator中。

#### Client-Go

编写一个Operator时，需要用到Client-Go，实现与API-Server的交互。Client-Go提供了4种交互对象：

* RESTClient：最基础的客户端，对HTTP请求做封装，支持Json和Protobuf格式，是对DiscoveryClient、ClientSet、DynamicClient的封装；

* DiscoveryClient：发现客户端，负责发现API-Server支持的支援组、版本和资源信息，等同于`kubectl api-resources`；由于GVR数据很少变动，所以可以将GVR数据缓存在本地，减少Client与API-Server的交互。使用CachedDiscoveryClient和MemCacheClient请求时，会先从缓存里取，取不到才请求API-Server。

  在使用kubectl命令时，也会进行缓存，以json文件的形式缓存存在 `~/.kube/cache` 中。

* ClientSet：负责操作Kubernetes内置的资源对象，如Pod、Service等；

* DynamicClient：动态客户端，可以对任意的Kubernetes资源对象进行通用操作，包括CRD；本质是通过一个嵌套的`map[string]interface{}`对象存储API-Server的返回值，再使用反射机制进行数据绑定，虽然灵活，但无法获取强数据类型的检查和验证。

本质上都是发送HTTP请求给API-Server，只是RESTClient比较原始，其他Client都对具体资源做了结构体封装。

#### 控制器的原理

![](https://github.com/Nixum/Java-Note/raw/master/picture/控制器工作流程.png)

**Informer**：

控制器通过Watch机制，与API-Server交互，获取它所关心的对象，依靠Informer通知器来完成，Informer与API对象一一对应；

Informer是一个自带缓存和索引机制，通过增量里的事件触发 Handler 的客户端库，查询时，优先从本地缓存里查找数据，而创建、更新、删除操作，则根据事件通知机制写入队列DeltaFIFO中，同时，当对应的事件处理后，更新本地缓存，使得本地缓存与etcd里的数据一致；

这个本地缓存在 Kubernetes 中一般被称为 LocalStore，索引一般被称为 Indexer；一般是一个资源一个Informer；

* **Reflector**：通过 **List-Watch** 机制监听并获取API-Server中资源的create、update、delete事件，并针对事件类型调用对应的处理函数，是k8s统一的异步消息处理机制，保证了消息的实时性、可靠性，保证本地缓存数据的准确性、顺序性和一致性。

  * List：获取资源的全量列表数据，并同步到本地缓存中；List基于HTTP短连接实现；

  * Watch：负责监听变化的数据，当Watch的资源发生变化时，并将资源对象的变化事件存放到本地队列DeltaFIFO中，触发对应的变更事件进行处理，同时更新本地缓存，使得本地缓存与etcd里的数据一致。

    **Watch的原理**：

    Watch基于HTTP长链接，1.5以前使用HTTP1.1，并采用分块传输编码`响应头加上Transfer-Encoding=chunked基于HTTP/1.1，因为在短连接里，客户端需要通过Content-Length或连接是否关闭来判断是否接收完成，可以关掉，使用chunked后，服务端无需使用来告诉客户端响应体的结束位置，结束位置通过/n, /r或0来表示`，将数据分解成一系列数据块，一个或多个地发送。

    1.5以后使用HTTP/2，引入Frame二进制帧为单位进行传输，etcd v3的gRPC stream协议。

  * 定时同步：定时器触发同步机制，定时更新缓存数据，定时同步的周期时间可配。

  数据更新的依据来源于 ResourceVersion，当资源发生变化时，ResourceVersion就会以递增的形式更新，保证事件的顺序性，通过比较这个值，来判断资源是否有变化。

  List-Watch必须一起配合才能保证消息的可靠性，如果仅依靠Watch，当连接断开后，消息就有可能丢失，此时通过List来获取所有数据，还有判断ResourceVersion的值，纠正数据不一致，保证事件不丢失。

* **DeltaFIFO**：增量队列，记录资源变化，Relector相当于队列的生产者；

  Delta是资源对象存储，保存存储对象的消费类型，作为队列里的消息体，有两个属性，Type表示事件类型，比如`Added、Updated、Deleted、Replaced、Sync`，Object表示资源对象，如Pod、Service；

  FIFO队列负责接收Reflector传递过来的事件，并将其按顺序存储，然后等待事件的处理函数进行处理，如果出现多个相同事件，则只会被处理一次；

  DeltaFIFO只会存储Watch返回的各种事件，而LocalStorae只会被Lister的List/gGet方法访问。

* **Indexer**：用来存储资源对象，并自带索引功能的本地存储LocalStore，可以理解为里面有很多map组成的倒排索引和对应的索引处理器；

  IndexFunc：索引器函数，用于计算资源对象的索引列表；

  Index：存储数据，比如：要查找某个命名空间下的Pod，对应的Index类型可以是`map[{namespace}]{sets.pod}`；

  Indexers：存储索引器，key为索引器名称，value为 IndexFunc， 比如`map["namespace"]{IndexFunc}`

  Indices：存储缓存器，key为索引器名称，value为缓存的数据，比如：`map["namespace"]map[{namespace}]{sets.pod}`

**SharedInformer**

在K8s中，每一个资源都有一个Informer，Informer使用Reflector来监听资源，如果统一资源的Informer实例化太多次，就会有很多重复的步骤。为了解决多个控制器操作同一资源的问题，导致对同一资源的重复操作，比如重复缓存。使用SharedInformer后，不管有多少个控制器同时读取事件，都只会调用一个Watch API来Watch上游的API-Server，降低Api-Server的负载，比如 Controller-Manager。

Informer是一种机制，SharedInformer是其中一种实现。

通常自定义Controller要求只有一个实例在运行，因为当出现多个实例的时候，会出现并发问题，重复消费事件，如果要实现高可用，需要主从部署，只有主服务处于工作状态，从服务处于暂停状态，当主服务出现问题时，实现主从切换。Client-Go本身提供了leaderelection机制来实现。

**WorkQueue**

> WorkQueue支持三种队列：FIFO队列、延时队列、限速队列，供不同场景下使用。
>
> 从event handler触发的事件会先放入WorkQueue中，WorkQueue是一个去重队列，内部除了 Queue队列 外还带有 Processing Set 和 Dirty Set记录，用来实现同一个资源对象的多次事件触发，入队列后会去重，不会被多个worker同时处理。
>
> * Queue队列：实际存储元素的地方，保证元素有序，本质是一个slice；
>
> * Dirty Set：保证去重，还能保证处理一个元素之前哪怕被添加多次（并发），也只会被处理一次；
> * Processing Set：标记机制，标记一个元素是否被处理，保证只有一个元素在Queue队列里被处理；

并发处理流程：

Add方法：

1. 元素被Add时，首先会检查其是否存在于Dirty Set中，如果存在则直接丢弃，不存在才加入，保证Dirty Set中不会有多个指向同一resource的相同元素存在；
2. 元素被添加进Dirty Set后，再判断Processing Set是否存在，如果存在则跳出，防止同一元素被并发处理，不存在则加入Queue队列；

Get方法：

1. 元素被Get时，Reconcile Loop从Queue队列队首中取出元素，并放入Processing Set中，防止并发处理同一元素；
2. 最后从Dirty Set中删除该元素，因为 Dirty Set 是为了实现的待消费去重, 既然从 Queue队列 拿走元素, Dirty Set 也需要删除；

Done方法：

1. Reconcile Loop处理结束后，从Processing Set中删除元素；

2. 如果 Dirty Set 中存在相同resource的元素，就会被放入Queue队列，此时Dirty Set和Queue持有同一份元素；

   这里的意思是，如果一个元素正在被处理，又再次加入了相同的元素，由于该元素还没处理完，只能把该元素先放入Dirty Set中，因为Queue队列会被多个协程消费，只放在Dirty Set中保证同一元素不会被并发消费，当元素处理完时，再重新进入Queue队列，因为此时该元素已经被处理过，是最新的了。

> 这样就解决了去重的问题，还解决了同一个对象并发处理的顺序问题，只不过可能和原来请求的顺序不太一致，相同资源的请求被delay了，但是不影响最终结果，也实现了无锁的操作。

**整体流程**

1. Reflector通过List罗列资源，通过Watch监听资源的变更事件，将结果放到DeltaFIFO队列中；
2. 然后从 DeltaFIFO中 将消费出来的资源对象存储到Indexer中，Indexer把增量里的API对象保存到本地缓存，并创建索引；
3. Indexer与etcd中的数据完全保持一致，handler处理时，先从本地缓存里拿，拿不到再请求API-Server，减少Client-Go与API-Server交互的压力。
4. Informer 与我们要编写的控制循环ControlLoop之间，则使用了一个工作队列WorkQ来进行协同，实际应用中，informers、listers、clientset都是通过CRD代码生成，开发者只需要关注控制循环的具体实现就行。

#### leaderelection机制

一些特定的组件需要确保同一时刻只有一个实例在工作，就需要通过leaderelection机制来实现。

原理是通过K8s的 `endpoints、configmap或lease`实现一个分布式锁（通过resourceVersion + 乐观锁实现，判断资源的id和自己所持有的id是否一致），只有抢到锁的服务才能成为leader，并定期更新（比如2s一次），而抢不到的节点会周期性的检查是否能更新以成为新Leader。

抢锁update时，采用乐观锁机制，通过resourceVersion字段判断对象是否已被修改。

当 leader 因为某些异常原因挂掉后，租约到期，其他节点会尝试抢锁，成为新的 leader，成为leader时，对应pod的annotations会更新`control-plane.alpha.kubernetes.io/leader`字段，值为该Pod的一些信息，如：

```json
{
    // Leader的Id
    "holderIdentity": "instance-o24xykos-3_9d68-33ec5eadb906",
 	// Follower获得leadership需要的等待LeaseDuration时间，Leader以leaseDuration为周期不断的更新renewTime的值
    "leaseDurationSeconds": 15,
    "acquireTime": "2020-04-23T06:45:07Z", 
    "renewTime": "2020-04-25T07:55:58Z", 
    "leaderTransitions": 1
}
```

所谓的选主，就看哪个Follower能将自己的信息更新到`endpoints、configmap或lease`的`control-plane.alpha.kubernetes.io/leader`上。

## Service

* 工作在第四层，传输层，一般转发TCP、UDP流量。

* 每次Pod的重启都会导致IP发生变化，导致IP是不固定的，Service可以为一组相同的Pod套上一个固定的IP地址和端口，让我们能够以**TCP/IP**负载均衡的方式进行访问。

  虽然Service每次重启IP也会发生变化，但是相比Pod会更加稳定。

* 一般是pod指定一个访问端口和label，Service的selector指明绑定的Pod，配置端口映射，Service并不直接连接Pod，而是在selector选中的Pod上产生一个Endpoints资源对象，通过Service的VIP就能访问它代理的Pod了。

* 创建一个新的Service对象需要两大模块同时协作，一个是控制器，它需要在每次客户端创建新的Service对象时，生成其他用于暴露一组Pod的对象，即Endpoint；另一个是kube-proxy，它运行在集群的每个节点上，根据Service和Endpoint的变动改变节点上iptables或者ipvs中保存的规则。

* service负载分发策略有两种模式：

  * RoundRobin：轮询模式，即轮询将请求转发到后端的各个pod上（默认模式）
  * SessionAffinity：基于客户端IP地址进行会话保持的模式，第一次客户端访问后端某个pod，之后的请求都转发到这个pod上

```yaml
kind: Service
apiVersion: v1
metadata:
  name: order
  namespace: regoo
  labels:
    app: order
spec:
  ports:
    - name: http
      protocol: TCP
      port: 8080
      targetPort: 8080
  selector:
    app: order
  type: NodePort
  sessionAffinity: None
  externalTrafficPolicy: Cluster
```

### kube-proxy

Kubernetes集群中每一个节点都运行着一个kube-proxy，这个进程负责监听**Service的增加和删除事件并修改运行代理的配置，比如节点的iptables或ipvs，为节点内的客户端提供流量转发和负载均衡等功能**，kube-proxy有三种模式：

* userspace：运行在用户空间的代理，对于每一个Service都会在当前的节点上开启一个端口，所有连接到当前代理端口的请求都会被转发到service背后的一组pod上，本质上是在节点的iptable上添加一条规则，通过iptables将流量转发给kube-proxy处理。

  如果选择userspace模式，每当有新的service被创建时，kube-proxy就会增加一条iptables记录并启动一个goroutine，前者用于将节点中服务对外发出的流量转发给kube-proxy，再由后者一系列goroutine将流量转发到目标的pod上。

* iptables：（默认）直接使用iptables转发当前节点上的全部流量，比userspace模式有更高的吞吐量，相比userspace模式，是直接转发流量，不用经过Kube-proxy转发。

  如果选择iptables模式，所有流量会先经过PREROUTING或者OUTPUT链 ，随后进入Kubernetes自定义的链入口KUBE_SERVICES、单个Service对应的链KUBE-SVC-XXX以及每个pod对应的链KUBE-SEP-XXX，经过这些链的处理，最终才能访问一个服务真正的IP地址。

* ipvs：解决在大量 Service 时，iptables 规则同步变得不可用的性能问题，ipvs 的实现虽然也基于 netfilter 的钩子函数，但是它却使用哈希表作为底层的数据结构，使用ipset来生成规则链（ipset可以理解为一个IP段的集合，这个集合可以是IP地址、网段、端口等，可以动态修改），并且工作在内核态，而iptables规则链是一个线性的数据结构，查找是需要遍历查找，这也就是说 ipvs 在重定向流量和同步代理规则有着更好的性能。

  除了能够提升性能之外，ipvs 也提供了多种类型的负载均衡算法，除了最常见的 Round-Robin 之外，还支持最小连接、目标哈希、最小延迟等算法，能够很好地提升负载均衡的效率。

### Endpoints的作用

当service使用了selector指定带有对应label的pod时，endpoint controller才会自动创建对应的endpoint对象，产生一个endpoints，endpoints信息存储在etcd中，用来记录一个service对应的所有pod的访问地址；

说白了，因为pod ip比较容易变化，而endpoint的作用就是维护service和一组pod的映射，不用频繁修改service。

endpoints controller的作用：

* 负责生成和维护所有endpoint对象的控制器；
* 负责监听service和对应pod的变化；
* 监听到service被删除，则删除和该service同名的endpoint对象；
* 监听到新的service被创建，则根据新建service信息获取相关pod列表，然后创建对应endpoint对象；
* 监听到service被更新，则根据更新后的service信息获取相关pod列表，然后更新对应endpoint对象；
* 监听到pod事件，则更新对应的service的endpoint对象，将pod IP记录到endpoint中；

### Service的实现

Service由kube-proxy组件 + kube-dns组件(coreDNS) + iptables或IPVS共同实现。

1. coreDNS创建时会调用kubelet修改每个节点的`/etc/resolv.conf`文件，添加coreDNS的service的clusterIP作为DNS服务的IP；
2. coreDNS会监听service和endpoints的变化，缓存到内存中，主要是service名称与其clusterIP的映射；
3. kube-proxy也会监听service和endpoints的变化，然后更新由service到pod路由规则，并添加到宿主机的iptables中；
4. 通过service域名请求时，会先请求coreDNS服务获取对应service的clusterIP，再根据这个ip在iptables中转发到对应的pod，iptables会负责负载均衡；

kube-proxy只是controller，对iptables进行更新，基于iptables的kube-proxy的主要职责包括两大块：

* 监听service更新事件，并更新service相关的iptables规则；

* 监听endpoint更新事件，更新endpoint相关的iptables规则，然后将包请求转入endpoint对应的Pod；

  如果某个service尚没有Pod创建，那么针对此service的请求将会被丢弃。

> kube-proxy对iptables的链进行了扩充，自定义了KUBE-SERVICES，KUBE-NODEPORTS，KUBE-POSTROUTING，KUBE-MARK-MASQ和KUBE-MARK-DROP五个链，并主要通过为KUBE-SERVICES chain增加rule来配制traffic routing 规则。通过iptables，修改流入的IP包的目的地址和端口，从而实现转发。
>
> 1. 首先给 Service 分配一个VIP，然后增加 iptables 规则将访问该 IP 的请求转发到后续的 iptables 链。
>
>    KUBE-SERVICES 或者 KUBE-NODEPORTS 规则对应的 Service 的入口链，这个规则应该与 VIP 和 Service 端口一一对应；
>
> 2. iptables 链实际是一个集合，包含了各个 Pod 的IP（这些称为 Service 的 Endpoints），使用 Round Robin 方式的负载均衡。
>
>    KUBE-SEP-(hash) 规则对应的 DNAT 链，这些规则应该与 Endpoints 一一对应；
>    KUBE-SVC-(hash) 规则对应的负载均衡链，这些规则的数目应该与 Endpoints 数目一致；
>
> 3. 然后，这些 Endpoints 对应的 iptables 规则，正是 kube-proxy 通过监听 Pod 的变化事件，在宿主机上生成并维护的。

大量的Pod会产生大量的iptables导致性能问题，因为其实现方式是全量的，会对文件上锁，把iptables里的内容拷贝出来进行修改，然后再保存回去，当有5000个服务时，就要耗时11分钟了，所以kube-proxy使用IPVS模式来解决这个问题。IPVS 是建立于 Netfilter之上的高效四层负载均衡器，支持 TCP 和 UDP 协议，支持3种负载均衡模式：NAT、直接路由（通过 MAC 重写实现二层路由）和IP 隧道。

### 关于Headless

Service在集群内部被访问，有两种方式

* Service的VIP，即clusterIP，访问该IP时，Service会把请求转发到其代理的某个Pod上。

* Service的DNS方式，比如Service有`my-svc.my-namespace.svc.cluster.local`这条DNS记录，访问这条DNS记录，根据这条DNS记录，查询出对应的clusterIP，根据clusterIP + iptables转发给对应的pod，实现负载均衡。

  如果这条DNS记录没有对应的Service VIP，即Service的clusterIP是None，则称为Headless Service，此时的DNS记录格式为`<pod-name>.<svc-name>.<namespace >.svc.cluster.local`，直接映射到被代理的某个Pod的IP，由客户端来决定自己要访问哪个pod，并直接访问。通过headless service访问不会进行负载均衡。

Service和Pod都会被Kubernetes分配对应的DNS A记录。

ClusterIp模式下的A记录

* Service：.. svc.cluster.local，对应Cluster IP
* Pod：.. pod.cluster.local，对应Pod的IP

Headless模型下的A记录

* Service：.. svc.cluster.local，对应的是所有被代理的Pod的IP地址的集合
* Pod：.. svc.cluster.local，对应Pod的IP

### Service的几种设置

Service的本质是通过kube-proxy在宿主机上设置iptables规则、DNS映射，工作在第四层传输层。

* **type=ClusterIP，此时该service只能在集群内部被访问**，如果集群外想访问，只能通过kubernetes的proxy模式来访问，比如先开启`kubectl proxy --port=8080`在本机打开集群访问的代理，然后通过该端口进行访问，kubernetes会帮我们转发到对应的pod上：`curl http://localhost:8080/api/v1/proxy/namespaces/{名称空间}/services/{service名称}/{path}`

* **type=NodePort，集群外部使用 `{任何一个节点的IP}:{nodePort}` 访问这个service**，会将请求转发到对应的Pod，本质上是kube-proxy为该模式配置一条iptables规则；

  默认下nodePort会使用主机的端口范围：30000 - 32767；

  在这里，请求可能会经过SNAT操作，因为当client通过node2的地址访问一个Service，node2可能会负载均衡将请求转发给node1，node1处理完，经过SNAT，将响应返回给node2，再由node2响应回client，保证了client请求node2后得到的响应也是node2的，此时Pod只知道请求的来源，并不知道真正的发起方；

  如果Service设置了`spec.extrnalTrafficPolicy=local`，Pod接收到请求，可以知道真正的外部发起方是谁了。

* **type=LoadBalancer，设置一个外部均衡服务，这个一般由公有云提供**，比如aws的NLB，阿里云的LB服务，此时会有ExternalIP，执行LB的域名。

* **type=ExternalName，**并设置externalName的值，此时不会产生EndPoints、clusterIP，**作用是为这个externalName设置了CName，主要用于集群内的Pod访问集群外的服务**。比如ExternalName设置为www.google.com，此时pod可以直接通过` [svc名称].[namespace名称].svc.cluster.local`即可访问www.google.com

* 设置externalIPs的值，这样也能通过该ip进行访问，前提是该IP对公网暴露，比如aws的弹性ip。

* 通过port forward的方式，将Pod端口转发到执行命令的那台机器上，通过端口映射提供访问，而不需要Service。

检查网络是否有问题，一般先检查Master节点的Service DNS是否正常、kube-dns(coreDns)的运行状态和日志；

当无法通过ClusterIP访问Service访问时，检查Service是否有Endpoints，检查Kube-proxy是否正确运行；

最后检查宿主机上的iptables。

### 关于服务发现

Kubernetes支持两种服务发现：环境变量和DNS服务

#### 环境变量

创建Service时，会在Service selector的Pod中的容器注入同一namespace下所有service的IP和端口作为环境变量，该环境变量会随着Service的IP和端口的改变而改变，但设置到容器里的环境变量不会。

可以使用`kubectl exec -it {pod名字} -n {名称空间} env`查看，HOST为service的cluster-IP

Kubernetes会设置两类环境变量：

* service环境变量

  ```ini
  {service名称}_SERVICE_HOST=172.20.97.162
  {service名称}_SERVICE_PORT=8080
  ```

* Docker Link环境变量

  DockerLink的作用：

  > 同一个宿主机上的多个docker容器之间如果想进行通信，可以通过使用容器的ip地址来通信，也可以通过宿主机的ip加上容器暴露出的端口号来通信，前者会导致ip地址的硬编码，不方便迁移，并且容器重启后ip地址会改变，除非使用固定的ip，后者的通信方式比较单一，只能依靠监听在暴露出的端口的进程来进行有限的通信。通过docker的link机制可以通过一个name来和另一个容器通信，link机制方便了容器去发现其它的容器并且可以安全的传递一些连接信息给其它的容器。
  >
  > 使用时需要先连接容器，使用命令`docker run -d --name {容器名称} --link {另一个容器名称}`，之后在容器内即可使用对方容器的名称进行通信

  ```ini
  {service名称}_PORT_8080_TCP_ADDR=172.20.97.162
  {service名称}_PORT_8080_TCP_PORT=8080
  {service名称}_PORT_8080_TCP_PROTO=tcp
  {service名称}_PORT=tcp://10.100.251.57:8080
  {service名称}_PORT_8080_TCP=tcp://10.100.251.57:8080
  ```

然后，服务即可通过环境变量获取要访问的服务的IP和端口，但是这种服务发现方式有一些**缺点**：

* 先前的服务必须先运行起来，否则环境变量无法被注入，导致Pod无法使用该环境变量；
* 如果依赖的service被删了或绑定了新地址，环境变量不会被修改，仍然使用旧有地址；
* 只有在同一namespace下的Pod内才可以使用此环境变量进行访问，不同namespace下无法通过变量访问；

#### DNS服务

一般情况下，会使用CoreDNS作为Kubernetes集群的DNS，**CoreDNS默认配置的缓存时间是30s**，增大cache时间对域名解析TTL敏感型的应用有一定的影响，会延缓应用感知域名解析配置变更时间。

**Kubernetes 通过修改每个 Pod 中每个容器的域名解析配置文件`/etc/resolv.conf` 来达到服务发现的目的**。

比如：

```ini
# /etc/resolv.conf
# nameserver表示DNS服务器
nameserver 172.20.0.10
# 主机名查询列表，默认只包含本地域名，阈值为6个，256个字符，会以 {名称空间}.svc.cluster.local、svc.cluster.local、cluster.local 3个后缀，最多进行8次查询 (IPV4和IPV6查询各四次) 才能得到正确解析结果
search {名称空间}.svc.cluster.local svc.cluster.local cluster.local
# 阈值为 15
options ndots:5
# 等待 DNS 服务器响应的超时时间，单位为秒。阈值为 30 s。
options timeout: 30
# 向同一个 DNS 服务器发起重试的次数，单位为次。阈值为 5。
options attempts: 1
```

nameserver指向的IP，就是 coreDNS 的service的ClusterIP，所有的域名解析都需要经过coreDNS的ClusterIP来进行解析，不论是Kubernetes内部域名还是外部域名；

coreDNS会监听集群内所有ServiceAPI，以在服务不可用时移除记录，在新服务创建时插入新记录，记录存储在coreDNS的本地缓存中。

在这个`/etc/resolv.conf`里配置含义为：DNS服务器为172.20.0.10，当查询的关键词中的 `.` 的数量少于5个，则根据search中配置的域名进行查询，当查询都没有返回正确响应时再尝试查询关键词本身，比如执行`host -v cn.bing.com`，会得到

```ini
Trying "cn.bing.com.{名称空间}.svc.cluster.local"
Trying "cn.bing.com.svc.cluster.local"
Trying "cn.bing.com.cluster.local"
Trying "cn.bing.com.us-west-2.compute.internal"
Trying "cn.bing.com"
...
```

所以，直接请求集群内部同一namespace下的域名时效率最高，因为只需要查询一次即可找到，比如在同一namespace下直接`curl {服务service名} `的效率是比`curl {服务service名}.{namespace}`要高，因为少了一次域名查询。这种DNS查询解析叫**非全限定域名查找**，因为我们配置了`ndots=5`，所以当域名中`.` 的个数小于5个，就会走search查询解析，如果要使用全限定域名查找，则需要在域名后面加`.`，就不会走search查询解析了，比如当`cn.bing.com.`此时就只会查询一次了，查不到才会走search查询。也可以通过修改`ndots`的数量，来减少域名查询次数的问题，可以在Pod的配置中添加`dnsConfig`修改`ndots`的值。当容器内部的`/etc/resolv.config`都找不到时，就会使用宿主机的`/etc/resolv.config`进行查找。

> Kubernetes 集群中支持通过 dnsPolicy 字段为每个 Pod 配置不同的 DNS 策略。目前支持四种策略：
>
> * ClusterFirst：通过集群 DNS 服务来做域名解析，Pod 内 /etc/resolv.conf 配置的 DNS 服务地址是集群 DNS 服务的 CoreDNS 地址。该策略是集群工作负载的默认策略；
> * None：忽略集群 DNS 策略，需要您提供 dnsConfig 字段来指定 DNS 配置信息；
> * Default：Pod 直接继承集群节点的域名解析配置。即在集群直接使用节点的 /etc/resolv.conf 文件；
> * ClusterFirstWithHostNetwork：强制在 hostNetWork 网络模式下使用 ClusterFirst 策略（默认使用 Default 策略）

另外，Kubernates的DNS服务支持Service的A记录、SRV记录（用于通用化地定位服务）和CNAME记录。

当设置了service并select了一类pod之后，DNS服务就会产生以下A记录和SRV记录

```ini
{service名称}.{名称空间}.svc.cluster.local. 5 IN A 10.100.71.56
_http._tcp.{service名称}.{名称空间}.svc.cluster.local. 30 IN SRV 10 100 443 {service名称}.{名称空间}.svc.cluster.local.
```

如果是headless service，则生成的A记录如下

```ini
{service名称}.{名称空间}.svc.cluster.local. 5 IN A 192.168.62.111
{service名称}.{名称空间}.svc.cluster.local. 5 IN A 192.168.62.112
{service名称}.{名称空间}.svc.cluster.local. 5 IN A 192.168.62.113
```

## Ingress

工作在第七层，应用层，即一般代理HTTP流量。

作用与Service类似，主要是用于对多个service的包装，作为service的service，设置一个统一的负载均衡（这样可以避免每个Service都设置一个LB，也可以避免因为节点扩缩容导致service的端口变化需要修改访问配置），设置Ingress rule，进行反向代理，实现的是HTTP负责均衡。

Ingress是反向代理的规则，Ingress Controller是负责解析Ingress的规则后进行转发。可以理解为Nginx，本质是将请求通过不同的规则进行路由转发。常见的Ingress Class实现有Nginx-Ingress-Controller、AWS-LB-Ingress-Controller，使用Ingress时会在集群中创建对应的controller pod。

Ingress Controller可基于Ingress资源定义的规则将客户端请求流量直接转发到Service对应的后端Pod资源上（比如aws-lb-ingress-controller 的 IP mode），其会绕过Service资源，直接转发到Pod上，省去了kube-proxy实现的端口代理开销。

```yaml
# 使用aws alb作为ingress，并将流量转发istio-ingressgateway service
apiVersion: extensions/v1beta1
kind: Ingress
metadata:
  namespace: istio-system
  name: istio-api
  labels:
    app: istio-api
  annotations:
    kubernetes.io/ingress.class: alb
    alb.ingress.kubernetes.io/load-balancer-name: "online-api"
    alb.ingress.kubernetes.io/target-type: "instance"
    alb.ingress.kubernetes.io/listen-ports: '[{"HTTPS":443}, {"HTTP":80}]'
    alb.ingress.kubernetes.io/certificate-arn: "xxx"
    alb.ingress.kubernetes.io/scheme: "internet-facing"
    alb.ingress.kubernetes.io/tags: "app=eks"
    alb.ingress.kubernetes.io/load-balancer-attributes: "deletion_protection.enabled=true,access_logs.s3.enabled=true,access_logs.s3.bucket=regoo-logs,access_logs.s3.prefix=alb"
spec:
  rules:
    - http:
        paths:
          - path: /*
            backend:
              serviceName: istio-ingressgateway
              servicePort: 80
          - path: /*
            backend:
              serviceName: istio-ingressgateway
              servicePort: 443
```

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

## RBAC

* **Role和RoleBinding：只针对某一namespace的操作授权，namespace只是逻辑上的隔离**。

以下配置就是给用户名为example-user绑定了一个example-role的角色，该角色只能操作namespace为mynamespace下的pod的所有读操作。

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role 
metadata: 
  namespace: mynamespace 
  name: example-role 
rules: 
  - apiGroups: [""] 
    resources: ["pods"]
    verbs: ["get", "watch", "list"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata: 
  name: example-rolebinding 
  namespace: mynamespace 
subjects: 
  - kind: User  # 这里的User，是Kubernetes内置的用户类型，只是一个授权的逻辑概念
    name: example-user 
    apiGroup: rbac.authorization.k8s.io
roleRef: 
  kind: Role 
  name: example-role 
  apiGroup: rbac.authorization.k8s.io
```

* **ClusterRole和ClusterRoleBinding：针对的是整个集群的操作的授权**

以下配置就是给用户名为example-user绑定了一个example-role的角色，该角色能对整个集群下的Pod进行读操作。

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole 
metadata: 
  name: example-clusterrole 
rules: 
  - apiGroups: [""] 
    resources: ["pods"]
    verbs: ["get", "watch", "list"]
--- 
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata: 
  name: example-clusterrolebinding 
subjects: 
  - kind: User 
    name: example-user 
    apiGroup: rbac.authorization.k8s.io 
roleRef: 
  kind: ClusterRole 
  name: example-clusterrole 
  apiGroup: rbac.authorization.k8s.io
```

verbs操作可以有get、list、watch、create、update、patch、delete。

resources指的是configmaps、pods、services、deployments、nodes，即所有定义的CRD。

resourceNames指定是对应资源的名称。

subjects的类型不止有User，也可以是ServiceAccount，创建该service后，k8s会自动为其分配一个Secret对象，该字段保存一个用来与API Server交互的token，token的内容可以是证书或者密码，以Secret对象的方式保存在etcd中。

在Pod中也可以使用ServiceAccount，通过serviceAccountName字段配置使用，如果Pod没有显式使用ServiceAccount，K8s会使用默认的拥有绝大多数权限的default ServiceAccount。创建完ServiceAccount后，会在K8s中产生一个用户名和用户组，

用户名：system:serviceaccount:<Namespace名字>:<ServiceAccount名字>

用户组：system:serviceaccount:<Namespace名字>

```yaml
apiVersion: v1 
kind: ServiceAccount 
metadata: 
  namespace: mynamespace 
  name: example-user
```

在K8s内，已经内置了很多系统保留的ClusterRole，给Master节点内的组件使用，以system:开头；此外，还有一些权限粒度比较大的ClusterRole，如cluster-admin(拥有最高权限)、admin、edit、view

## 声明式API

通过编排对象，在为它们定义服务的这种方法，就称为声明式API。

Pod就是一种API对象，每一个API对象都有一个Metadata字段，表示元数据，通过里面的labels字段（键值对）来找到这个API对象；每一个API对象都有一个Spec字段，来配置这个对象独有的配置，如为Pod添加挂载的Volume。

命令式配置文件操作：编写一个yaml配置，使用`kubectl create -f config.yaml`创建controller和Pod，然后修改yaml配置，使用`kubectl replace -f config.yaml`，更新controller和Pod，API-Server一次只能处理一个命令；或者直接使用命令 `kubectl set image ... `或 `kubectl edit ...` 这些都属于命令式的。

声明式配置文件操作：编写yaml配置和更新yaml配置均使用`kubectl apply -f config.yaml`，API-Server一次处理多个命令，并且具备merge能力。

命令式是告诉API-Server我要做什么操作，而声明式是告诉API-Server我要达到什么样的效果，幂等的操作。

所以声明式API可以解决命令式API因为反复重试导致的错误，或者 并发执行时产生的问题。

### 工作原理

k8s根据我们提交的yaml文件创建出一个API对象，一个API对象在etcd里的完整资源路径是由Group（API 组）、Version（API 版本）和 Resource（API 资源类型）三个部分组成的。

一个资源可以有多个版本，版本和资源的信息会存储在etcd中，但**etcd只会存储资源的一个指定版本**，当客户端传入的yaml文件中指定的资源版本和客户端向API-Server请求的资源版本可能并不是etcd中存储的版本时，会进行版本转换（通过在资源中定义 `served：控制某个版本是否可读写；storage：判断是否有多个版本`）。

API-Server会维护一个internal版本，当需要版本转换时，任意版本都会先转换为internal版本，再由internal版本转换为指定的目标版本，所以只要保证每个版本都能转换成internal版本，即可支持任意版本间的转换了，对应的CRD代码也要支持新老版本的切换。需要编写conversion webhook，这样请求在到达controller时，就会经过webhook进行转换。

![](https://github.com/Nixum/Java-Note/raw/master/picture/API对象树形结构.png)

G：group、V：version、R：resource、K：kind

分为有组名，比如`/apis/app/v1/deployment` 和 无组名，比如`/api/v1/pods`，无组名也被称为核心资源组，CoreGroup。

接口调用时，只需要知道GVR即可，通过GVR操作资源对象。通过GVK信息获取要读取的资源对象的GVR，进而构建RESTful API请求获取对应的资源，通过GVK和GVR的映射叫做RESTMapper，其作用是在ListerWatcher时，根据Schema定义的类型GVK解析出GVR，向API-Server发起HTTP请求获取资源，然后Watch。

```yaml
apiVersion: batch/v2alpha1
kind: CronJob
...
```

以上面的配置为例，CronJob是API对象的资源类型、batch是组、v2alpha1是版本

核心API对象如Pod、Node是没有Group的，k8s是直接在/api这个层级进行下一步的匹配

过程步骤：

1. yaml文件被提交给API-Server，API-Server接收到后完成前置工作，如授权、超时处理、审计；
2. 进入MUX和Routes流程，API-Server根据yaml提供的信息，使用上述的匹配过程，找到CronJob的类型定义；
3. API-Server根据这个类型定义，根据yaml里CronJob的相关字段，创建一个CronJob对象，同时也会创建一个SuperVersion对象，它是API资源类型所有版本的字段全集，用于处理不同版本的yaml转成的CronJob对象；
4. API-Server 会先后进行 Admission() 和 Validation() 操作，进行初始化和校验字段合法性，验证过后保存在Registry的数据结构中；
5. API-Server把验证过的API对象转换成用户最初提交的版本，进行序列化操作，保存在etcd中；

## 配置相关

### Pod级别下的一些配置，即当kind: Pod

**NodeSelector**：将Pod和Node进行绑定的字段

```yaml
apiVersion: v1
kind: Pod
...
spec:
 nodeSelector:
   disktype: ssd
```

表示该Pod只能运行在携带了disktype:ssd标签的节点上，否则它将调度失败

**HostAliases**：定义了Pod的hosts文件（比如/etc/hosts）里的内容

```yaml
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

```yaml
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

```yaml
对于处于terminating的pod或namespace的删除方法
1. 将对应的pod或namespace转成json形式，将属性finalizers改为[]
kubectl get [pod或namespace] [对应的名称]  -o json |jq '.spec = {"finalizers":[]}' > json文件名.json
2.打开另一个窗口，起一个代理
kubectl proxy --port=8081
3.访问kubelet接口进行删除
curl -k -H "Content-Type: application/json" -X PUT --data-binary @json文件.json 127.0.0.1:8081/api/v1/[pod或namespaces]/[对应的名称]/finalize
或者：
删除状态为Terminating的命名空间
kubectl get ns [命名空间的值] -o json > xxxx.json，修改finialize为空数组[]
kubectl replace --raw "/api/v1/namespaces/[命名空间的值]/finalize" -f xxx.json

删除指定命名空间下所有资源例如pod、deployment、svc，包括里面所有副本
kubectl delete all -n [命名空间] --all

删除命名空间下的event
kubectl delete events -n [命名空间] --all

启动busybox调试网络
kubectl run -it --rm --restart=Never busybox --image=busybox sh

查看node标签
kubectl get nodes --show-labels

给node设置标签
kubectl label nodes <your-node-name> disktype=ssd

删除节点
1. 先排干上面的pod
kubectl drain node名称 --delete-local-data --force --ignore-daemonsets
2. 删除
kubectl delete node node名称 

查看滚动更新的流程
kubectl describe deploy [deploy名称] -n [名称空间]或者kubectl rollout status deploy [deploy名称] -n [名称空间]

当新的版本有问题时，回滚到上个版本
kubectl rollout undo deploy [deploy名称] [--to-revision=n]

查看每次变更的版本，但最好在执行apply -f deployment.yaml后加上-record参数；
kubectl rollout history

如果不想每次修改都触发滚动更新，可以先使用`kubectl rollout pause deploy xx -n yy`暂停Ddeployment的行为，修改为yaml后使用`kubectl rollout resume deploy xx -n yy`恢复，让其只触发一次修改。

将镜像打成压缩包
docker save -o 压缩包名字  镜像名字:标签
还原成镜像
docker load < 压缩包名字
直接使用命令，不保持容器启动
docker run --rm --name kubectl bitnami/kubectl:latest version

启动时修改entrypoint
docker run --rm -it --entrypoint env 镜像:tag /bin/bash

构建镜像
docker build -t xxx:tag .

从容器里复制文件出来
docker cp 容器id:/容器内文件路径/文件名 /宿主机路径/文件名

滚动更新
kubectl patch deployment $APP-stable -n $NAMESPACE --patch '{"spec": {"template": {"metadata": {"labels":{"cm":"stable"},"annotations": {"version/config": "'$DATE'" }}}}}'

复制pod里的文件到当前机器
kubectl cp [pod名字]:[work_dir]/文件名 -n [名称空间] -c [容器名称] ./复制后的文件名
```

# ServiceMersh

## 基本

Service Mesh本质上是分布式的微服务**网络控制的代理**，以side car的方式实现：通过envoy+iptable对流量进行劫持，通过对流量的控制，实现诸如注册中心、负载均衡器、路由策略、熔断降级、限流、服务追踪等功能，而不需要耦合在具体的业务服务中，而Istio是其中一种实现。

优点：

1. 业务无需感知微服务组件，诸如限流、熔断、容错、降级、负载均衡等服务治理相关的中间件，这些功能全由ServiceMersh提供
2. 不限制开发语言和框架，提供多语言服务治理能力
3. 服务零成本升级，对服务无侵入，不会产生SDK依赖
4. 使得微服务体系统一管理和演进

缺点：

1. 部署复杂，比如跨集群、跨机房等一些复杂环境覆盖不易
2. 每次网络请求会多一跳，带来一定的资源消耗和延时

在没有Service Mesh的情况下，Kubernetes仅依靠kube-proxy + iptable + service + ingress来实现对流量的管理，管理的是进出集群的流量，而Service Mersh通过iptables + sidecar的方式控制的是进出pod的流量，控制的粒度会更细。

kube-proxy的设置是全局的，如果转发的pod不能正常服务，它不会自动尝试其他pod；另外，kube-proxy实现的是集群内多个pod实例之间的流量负载均衡，但无法对服务之间流量的精细化控制，比如灰度发布、蓝绿发布等。

# Istio

upstream和downstream是针对Envoy而言的，

upstream：Envoy发送请求给服务，发出的流量是upstream

downstream：发送请求给Envoy，Envoy接收的流量是downstream

![](https://github.com/Nixum/Java-Note/raw/master/picture/envoy流量模型.png)

xDS：控制平面与数据平面通信的统一API标准，包括 LDS（监听器发现服务）、CDS（集群发现服务）、EDS（节点发现服务）、SDS（密钥发现服务）和 RDS（路由发现服务）

Istio核心功能：

* 流量控制：路由（如灰度发布、蓝绿部署、AB测试）、流量转移、弹性（如超时重试、熔断）测试（如故障注入、流量镜像，模拟生产环境的流量进行测试）
* 安全：认证、授权
* 可观察：指标、日志、追踪
* 策略：限流、黑白名单

Istio分为控制平面control plane和数据平面data plane，控制面主要负责资源管理、配置下发、证书管理、控制数据面的行为等，数据面则负责流量出入口。

**控制平面**

在低版本中分为Pilot、Mixer、Citadel；

* Pilot负责配置下发，将配置文件转化为Istio可识别的配置项，分发给各个sidecar代理(piloy-agent)；Pilot能为sidecar提供服务发现、智能路由、流量管理、超时、重试、熔断灯功能，原理是将这些功能转化为路由规则配置，下发到各个sidecar中；

  pilot有两个子组件：

  * pilot-discovery：是控制面与数据面的桥梁，从kubernetes中获取服务信息，如service、endpoint、pod、node的资源信息；监控istio控制面的信息，如vs、gw、dr等，将服务信息和流量规则信息转化为数据面可以理解的格式，通过xDS下发到各个sidecar中；
  * pilot-agent：根据API-Server中配置的信息生成sidecar配置文件，负责启动、监控sidecar进程，是istio-proxy的一部分，注入到pod中；
  * pilot-proxy：即pod中的sidecar-istio-proxy，负责流量代理，直接连接pilot-discovery，间接获取集群中各个微服务的注册情况；

* Citadel负责安全、授权认证，比如证书的分发和轮换，让sidecar代理两端实现双向TLS认证、访问授权；

* Gallery负责配置管理，负责将istio组件与底层平台如Kubernetes获取用户配置的细节隔离开，对配置信息格式和正确性校验，将配置信息提供pilot使用，只与控制面的其他组件打交道，实现与底层平台解耦；
* sidecar injector：在为配置了istio自动注入的pod自动注入istio-proxy；
* Mixer负责从数据平面收集数据指标以及流量策略，是一种插件组件，插件提供了很好的扩展性，独立部署，但每次修改需要重新部署，之后1.1版本将插件模块独立一个adaptor和Gallery，但是Mixer由于需要频繁的与sidecar进行通信，又是部署在应用进程外的，因此性能不高。

高版本1.5后中分为将Pilot、Citadel、Gallery整合为istiod，同时istiod里也包含了CA、API-Server，配合ingressgateway、egressgateway管理流量。

**数据平面**：Pod中的每个Envoy容器，即istio-proxy；Envoy会以side car的方式运行在Pod中，利用Pod中的所有容器共享同一个Network Namespace的特性，通过配置Pod里的iptables规则，管理进出Pod的流量。

![](https://github.com/Nixum/Java-Note/raw/master/picture/Istio-架构.png)

istio-proxy的作用：

>  客户端功能：
>
> * 发现和负载平衡。代理可以使用几个标准的服务发现和负载平衡API，以有效地将流量分配给服务。
>
> * 凭证注入。代理可以通过连接隧道或特定于协议的机制（例如HTTP请求的JWT令牌）注入客户端身份。
>
> * 连接管理。代理管理与服务的连接，处理运行状况检查，重试，故障转移和流控制。
>
> * 监控和记录。代理可以报告客户端指标并记录到混合器。
>
> 服务器端功能：
>
> * 速率限制和流量控制。代理可以防止后端系统过载，并提供客户端感知的速率限制。
> * 协议翻译。代理是gRPC网关，提供JSON-REST和gRPC之间的转换。
> * 认证与授权。代理支持多种身份验证机制，并且可以使用客户端身份通过混合器执行授权检查。
> * 监控和记录。代理可以报告服务器端指标并记录到混合器。

## 自动注入实现

依赖Kubernetes中的Dynamic Admission Control的功能，也叫Initializer。

Istio会将Envoy容器本身的定义，以configMap的方式进行保存，当用户提交自己的Pod时，Kubernetes就会通过类似git merge的方式将两份配置进行合并。这个合并的操作会由envoy-initializer的Pod来实现，该Pod使用 循环控制，不断获取用户新创建的Pod，进行配置合并。

## 核心CRD

* **VirtualService**：路由规则，主要是把请求的流量路由到指定的目标地址，解耦请求地址与工作负载。

* **DistinationRule**：定义了VirtualService里配置的具体的目标地址形成子集，设置负载均衡模式，默认是随机策略。

上面两个主要是管理服务网格内部的流量。

* **Gateway（ingress gateway）**：是网格的边界，管理进出网格的流量，比如为进出的流量增加负载均衡的能力，增加超时重试的能力，有ingress gateway和egress gateway分别管理。

  **与k8s Ingress的区别**：

  1. k8s Ingress只支持7层协议，比如http/https，不支持tcp、udp这些，没有VirtualService，直接对的Service
  2. Gateway支持 4 - 6 层协议，只设置入口点，配合VirtualService解耦路由规则的绑定，实现路由规则复用。

* **ServiceEntry**：面向服务，将外部的服务注册到服务网格中，为其转发请求，添加超时重试等策略，扩展网格，比如连接不同的集群，使用同一个istio管理。

Sidecar使用Envoy，代理服务的端口和协议。

```yaml
apiVersion: networking.istio.io/v1alpha3
kind: Gateway
metadata:
  name: public-api-gateway
  namespace: regoo
spec:
  selector:
    app: istio-ingressgateway
    istio: ingressgateway
  # 入口配置
  servers:
    - port:
        number: 8080
        name: http
        protocol: HTTP
      hosts:
        - "api.xxx.com"
---
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: order
  namespace: regoo
  labels:
    app: order
spec:
  # 只允许请求来自该host的流量
  hosts:
    - "*"
  # 绑定Gateway
  gateways:
    - public-api-gateway
  http:
  	# 路由匹配
    - match:
        - uri:
            prefix: /order/v1
      route:
        - destination:
            port:
              number: 8080
            host: order
            subset: stable
---
apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: order
  namespace: regoo
  labels:
    app: order
spec:
  host: order
  subsets:
    - name: stable
      labels:
        version: stable
```

## 应用场景

1. VirtualService和DestinationRule：按服务版本路由、按比例切分流量、根据匹配规则进行路由(比如请求头必须包含xx)、路由策略(如负载均衡，连接池)

   蓝绿部署：同时准备两套环境，控制流量流向不同环境或版本：两套集群，一套是现有在使用的集群（蓝色），另一套用于上线前的测试和验证后进行切换的集群（绿色），都可以接受流量，先放少量流量到绿色集群，没问题之后再把蓝色集群的所有流量切到绿色集群，蓝色集群再进行升级；

   灰度发布(金丝雀发布)：小范围测试和发布，即按比例切分流量；让一部分用户先用老服务A，一部分用新服务B，后面再根据具体情况，全部迁移到B；

   A/B测试：类似灰度发布，只是侧重点不同，灰度发布最终流量会流向最新版本，而A/B测试只是用于测试A、B两个环境带来的影响。

   这两个可以用于ingressgateway或egressgateway

2. Gateway：暴露集群内的服务给外界访问、将集群内部的服务以https的方式暴露、作为统一应用入口、API聚合

3. ServiceEntry：添加外部的服务到网格，从而管理外部服务的请求，扩大网格，默认情况下，Istio允许网格内的服务访问外部服务，当全部禁止后，需要使用ServiceEntry注册外部服务，以供网格内部的服务使用，一般配合engressgateway，控制网格内部向外部服务发出的请求。

4. 超时和重试：通过virtualService的route配置`timemout`设置服务接收请求处理的超时时间，`retries.attempts`和`retries.perTryTimeout`设置重试次数和重试时间间隔，`retries.retryOn`设置重试条件

5. 熔断：通过DestinationRule的trafficPolicy里connectionPool和outlierDection的配置实现

   ```yaml
   apiVersion: networking.istio.io/v1alpha3
   kind: DestinationRule
   metadata:
     name: httpnin
   spec:
     host: httpbin
     trafficPolicy:
       connectionPool:
         tcp:
           maxConnections: 1 	# tcp最大连接数
         http:
           http1MaxPendingRequests: 1 # 每个连接能处理的请求数
           maxRequestsPerConnection: 1 # 最大被阻挡的请求数
       outlierDetection:
         consecutiveErrors: 1 # 允许出错的次数
         interval: 1s # 失败次数计数时间
         baseEjectionTime: 3m # 最小驱逐时间，经过此时间后将pod重新加入，默认30s，乘于触发次数后作为熔断持续时间
         maxEjectionPercent: 100 # 熔断触发时驱逐pod的比例
   ```

6. 故障注入：通过VirtualService的fault配置实现

7. 流量镜像：通过VirtualService的`mirror`和`mirrorPercentage`配置，比如将发送给v1版本的真实流量镜像一份给v2

8. 限流：1.5之前有Mixer提供，但是1.5之后移除了Mixer，只能使用Envoy + filter实现，不属于istio生态的了

9. 授权认证，Istio的认证更多的是服务间的访问认证，可根据namespace、具体的服务、服务的接口、请求方法、请求头、请求来源等进行设置

   对外提供HTTPS mTLS访问方式，设置域名证书和Gateway即可；设置网格内部的mTLS双向认证；设置JWT认证，使用RequestAuthentication资源进行认证配置，使用AuthorizationPolicy 资源进行授权配置；

## 调试

使用istioctl的dashboard工具、Envoy的admin接口、Pilot的debug接口等，查看网格的信息，比如资源使用率、日志级别、Envoy性能相关信息等

* `istioctl x describe pod [pod名称]`，查看pod是否在网格内，验证其VirtualService、DestinationRule、路由等

* `istioctl analyze [-n 命名空间名称] 或 [具体的yaml文件] 或 --use-kube=false [yaml文件]；只分析文件`进行网格配置的诊断

* `istioctl d [istiod的pod名称] -n istio-system`使用controlZ可视化自检工具，调整日志输出级别、内存使用情况、环境变量、内存信息

* `istioctl d envoy [pod名称].[命名空间] `使用Envoy的admin接口，进行Envoy的日志级别调整、性能数据分析、配置、指标信息的查看

* `kubectl port-forward service/istio-pilot -n istio-system 端口:端口`使用pilot的debug接口，查看xDS和配置信息、性能问题分析、配置同步情况

* `istioctl dashboard [controlZ/envoy/Grafana/jaeger/kiali/Prometheus/zipkin]`使用istio提供的工具

* `istioctl ps(proxy-status的缩写) [pod名称]`进行配置同步检查。

* `istioctl pc(proxy-config的缩写) [cluster/route...] [pod名称].[命名空间]`查看配置详情。

* `istioctl proxy-config routes [istio-ingressgateway pod的名字] -n istio-system`查看istio整个服务网格的配置

* envoy调试

  ```
  进到pod里之后，修改envoy的日志级别
  curl -XPOST http://localhost:15000/logging?level=debug
  curl -XPOST http://localhost:15000/logging?level=info
  ```

# 参考

极客时间-深入剖析k8s-张磊

极客时间-ServiceMesh实战-马若飞

[Service核心原理](https://www.lixueduan.com/post/kubernetes/04-service-core/)

[KubeDNS和CoreDNS](https://zhuanlan.zhihu.com/p/80141656)

[详解 Kubernetes Volume 的实现原理](https://draveness.me/kubernetes-volume/)

[详解 Kubernetes ReplicaSet 的实现原理](https://draveness.me/kubernetes-replicaset/)

[详解 Kubernetes 垃圾收集器的实现原理](https://draveness.me/kubernetes-garbage-collector/)

[详解 Kubernetes DaemonSet 的实现原理](https://draveness.me/kubernetes-daemonset/)

[详解 Kubernetes Deployment 的实现原理](https://draveness.me/kubernetes-deployment/)

[详解 Kubernetes Service 的实现原理](https://draveness.me/kubernetes-service/)

[详解 Kubernetes Pod 的实现原理](https://draveness.me/kubernetes-pod/)

[谈 Kubernetes 的架构设计与实现原理](https://draveness.me/understanding-kubernetes/)

[云计算K8s组件系列（一）---- K8s apiserver 详解](https://kingjcy.github.io/post/cloud/paas/base/kubernetes/k8s-apiserver/)

[源码分析 kubernetes client-go workqueue 的实现原理](https://github.com/rfyiamcool/notes/blob/main/kubernetes_client_go_workqueue_code.md)

[源码分析 kubernetes scheduler 核心调度器的实现原理](https://github.com/rfyiamcool/notes/blob/main/kubernetes_scheduler_code.md)

