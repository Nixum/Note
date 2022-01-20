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

## 基本

![](https://github.com/Nixum/Java-Note/raw/master/picture/k8s项目架构.jpg)

**容器的本质是进程，Kubernetes相当于操作系统**，管理这些进程组。

* CNI：Container Network Interface，容器网络接口规范，如 Flannel、Calico、AWS VPC CNI
* kubelet：负责创建、管理各个节点上运行时的容器和Pod，这个交互依赖CRI的远程调用接口，通过Socket和容器运行时通信。
  kubelet 还通过 gRPC 协议同一个叫作 Device Plugin 的插件进行交互。这个插件，是 Kubernetes 项目用来管理 GPU 等宿主机物理设备的主要组件
  kubelet 的另一个重要功能，则是调用网络插件和存储插件为容器配置网络和持久化存储，交互的接口是CNI和CSI
* CRI：Container Runtime Interface，容器运行时的各项核心操作的接口规范，是一组gRPC接口。包含两类服务，镜像服务和运行时服务。镜像服务提供下载、检查和删除镜像的RPC接口；运行时服务包含用于管理容器生命周期，与容器交互的调用的RPC接口（exec / attach / port-forward等）。dockershim、containerd、cri-o都是遵循CRI的容器运行时，称为高层级运行时。
* CSI：Container Storage Interface，容器存储的接口规范，如PV、PVC
* OCI：Open Container Initiative，容器运行时和镜像操作规范，镜像规范规定高层级运行时会下载一个OCI镜像，并把它解压称OCI运行时文件系统包；运行时规范描述如何从OCI运行时文件系统包运行容器程序，并且定义其配置、运行环境和生命周期。定义新容器的namespaces、cgroups和根文件系统；它的一个参考实现是runC，称为底层级运行时。
* CRD：Custom Resource Definition，自定义的资源对象，即yaml文件中的Kind，如Operator就是实现CRD的控制器，之后直接使用Operator创建的CRD声明对象即可使用
* Master节点作用：编排、管理、调度用户提交的作业
  * Scheduler：编排和调度Pod，基本原理是通过监听api-server获取待调度的pod，然后基于一系列筛选和评优，为pod分配最佳的node节点。
  * APIServer：提供集群对外访问的API接口实现对集群资源的CRUD以及watch，是集群中各个组件数据交互和通信的枢纽，当收到一个创建pod的请求时会进行认证、限速、授权、准入机制等检查后，写入etcd。
  * Controller Manager：管理控制器的，比如Deployment、Job、CronbJob、RC、StatefulSet、Daemon等，核心思想是监听、比较资源实际状态与期望状态是否一致，否则进行协调。
* Device Plugin：管理节点上的硬件设备，比如GPU
* Kube-Proxy：作为daemonset部署在每个节点上，主要用于为Pod创建代理服务，从API-Server获取所有service信息，创建Endpoints，转发service到Pod间的请求，默认使用iptables模式，但当service数量变多时有性能问题，1.8版本后使用IPVS模式提升性能
* coreDNS：低版本的kubernetes使用kube-dns，1.12后默认使用coreDNS，用于实现域名查找功能

## 调度器Scheduler

主要职责就是为新创建的Pod寻找合适的节点，默认调度器会先调用一组叫Predicate的调度算法检查每个Node，再调用一组叫Priority的调度算法为上一步结果里的每个Node打分，将新创建的Pod调度到得分最高的Node上。

### 原理

![](https://github.com/Nixum/Java-Note/raw/master/picture/k8s默认调度原理.png)

* 第一个控制循环叫Informer Path，它会启动一系列Informer，监听etcd中的Pod、Node、Service等与调度相关的API对象的变化，将新创建的Pod添加进调度队列，默认的调度队列是优先级队列；

  此外，还会对调度器缓存进行更新，因为需要尽最大可能将集群信息Cache化，以提高两个调度算法组的执行效率，调度器只有在操作Cache时，才会加锁。

* 第二个控制循环叫Scheduling Path，是负责Pod调度的主循环，它会不断从调度队列里出队一个Pod，调用Predicate算法进行过滤，得到可用的Node，Predicate算法需要的Node信息，都是从Cache里直接拿到；

  然后调用Priorities算法为这些选出来的Node进行打分，得分最高的Node就是此次调度的结果。

* 得到可调度的Node后，调度器就会将Pod对象的nodeName字段的值，修改为Node的名字，实现绑定，此时修改的是Cache里的值，只会才会创建一个goroutine异步向API Server发起更新Pod的请求，完成真正的绑定工作；这个过程称为乐观绑定。

* Pod在Node上运行起来之前，还会有一个叫Admit的操作，调用一组GeneralPredicates的调度算法验证Pod是否真的能够在该节点上运行，比如资源是否可用，端口是否占用之类的问题。

### 调度策略

调度的本质是过滤，通过筛选所有节点组，选出符合条件的节点。

Predicates阶段：

* GeneralPredicates算法组，最基础的调度策略，由Admit操作执行
  * PodFitsResources：检查节点是否有Pod的requests字段所需的资源
  * PodFitsHost：检查节点的宿主机名称是否和Pod的spec.nodeName一致
  * PodFitsHostPorts：检查Pod申请的宿主机端口spec.nodePort是否已经被占用
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

* LeastRequestedPriority：选出空闲资源（CPU和Memory）最多的宿主机。

  `score = (cpu((capacity - sum(requested))10 / capacity) + memory((capacity-sum(requested))10 / capacity)) / 2`

* BalancedResourceAllocation：调度完成后，节点各种资源分配最均衡的节点，避免出现有些节点资源被大量分配，有些节点则很空闲。

  `score = 10 - variance(cpuFraction, memoryFraction, volumeFraction) * 10，Fraction=Pod请求资源 / 节点上可用资源，variance=计算每两种Faction资源差最小的节点`

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

当一个高优先级的Pod调度失败时，调度器会试图从当前集群里寻找一个节点，当该节点上的一个或多个低优先级的Pod被删除后，待调度的高优先级的Pod可用被调度到该节点上，但是抢占过程不是立即发生，而只是在待调度的高优先级的Pod上先设置spec.nominatedNodeName=Node名字，等到下一个调度周期再决定是否针对要运行在该节点上，之所以这么做是因为被删除的Pod有默认的30秒优雅退出时间，在这个过程中，可能有新的更加被适合给高优先级调度的节点加入。

#### 抢占原理

抢占算法的实现基于两个队列，一个是activeQ队列，存放下一个调度周期里需要调度的Pod；另一个是unschedulableQ队列，存放调度失败的Pod，当一个unschedulableQ里的Pod被更新之后，调度器就会把该Pod移动到activeQ里；

1. 当调度失败时，抢占算法开始工作，首先会检查调度失败的原因，判断是否可以为调度失败的Pod寻找一个新节点；
2. 之后调度器会把自己缓存的所有节点信息复制一份，通过这份副本模拟抢占过程，找出可以使用的节点和需要被删除的Pod列表，确定抢占是否可以发生；
3. 执行真正的抢占工作，检查要被删除的Pod列表，把这些Pod的nominatedNodeName字段清除，为高优先级的Pod的nominatedNodeName字段设置为节点名称，之后启动一个协程，移除需要删除的Pod。

第2步和第3步都执行了抢占算法Predicates，只有这两遍抢占算法都通过，才算抢占成功，之所以要两次，是因为需要满足InterPodAntiAffinity规则和下一次调度周期不一定会调度在该节点的情况。

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

## etcd

etcd作为Kubernetes的元数据存储，kube-apiserver是唯一直接跟etcd交互的组件，kube-apiserver对外提供的监听机制底层实现就是etcd的watch。

api-server在收到请求后，会进行一系列的执行链路。

1. 认证：校验发起请求的用户身份是否合法，支持多种方式如x509客户端证书认证、静态token认证、webhook认证
2. 限速：默认读 400/s，写 200/s，1.19版本以前不支持根据请求类型进行分类、按优先级限速，1.19版本以后支持将请求按重要程度分类限速，支持多租户，可有效保障Leader选举之类的高优先级请求得到及时响应，防止一个异常client导致整个集群被限速。
3. 审计：记录用户对资源的详细操作行为
4. 授权：检查用户是否有权限对其访问的资源进行相关操作，支持RBAC、ABAC、webhook，1.12版本后默认授权机制是RBAC
5. 准入控制：提供在访问资源前拦截请求的静态和动态扩展能力，如镜像拉取策略。
6. 与etcd交互

### 资源存储格式

资源以 `prefix + / + 资源类型 + / + namespace + / + 具体资源名称` 组成，作为key。基于etcd提供的范围查询能力，支持按具体资源名称查询、namespace查询。默认的prefix是 /registry。

对于基于label查询，是由api-server通过范围查询遍历etcd获取原始数据，然后再通过label过滤。

### 资源创建流程

创建资源时，会经过 BeforeCreate 策略做etcd的初始化工作，Storgae.Create接口调用etcd进行存储，最后在经过AfterCreate和Decorator。

由于put并不是并发安全的接口，并发时可能导致key覆盖，所以api-server会调用Txn接口将数据写入etcd。

当资源信息写入etcd后，api-server就会返回给客户端了。资源的真正创建是基于etcd的Watch机制。

controller-manager内配备多种资源控制器，当触发etcd的Watch机制时会通知api-server，api-server在调用对应的控制器进行资源的创建。比如，当我们创建一个deployment资源写入etcd，通过Watch机制，通知给api-server，api-server调用controller-manager里的deployment-controller创建ReplicaSet资源对象，写入etcd，再次触发Watch机制，经过api-server调用ReplicaSet-controller，创建一个待调度的Pod资源，写入etcd，触发Watch机制，经过api-server，scheduler监听到待调度的Pod，就会为其分配节点，通过api-server的Bind接口，将调度后的节点IP绑定到Pod资源上。kubelet通过同样的Watch机制感知到新建的Pod，发起Pod创建流程。

Kubernetes中使用Resource Version实现增量监听逻辑，避免客户端因为网络等异常出现中断后，数据无法同步的问题；同时，客户端可通过它来判断资源是否发生变化。

在Get请求中ResourceVersion有三种取值：

* 未指定，默认为空串：api-server收到后会向etcd发起共识读/线性读，获取集群最新数据，所以在集群规模较大时，未指定查的性能会比较差。

* =字符串0：api-server收到后会返回任意资源版本号的数据，优先返回最新版本；一般情况下先从api-server缓存中获取数据返回给客户端，有可能读到过期数据，适用于一致性要求不高的场景。

* =非0字符串：api-server收到后，会保证Cache中的最新ResouorceVersion大于等于请求中的ResourceVersion，然后从Cache中查询返回。

  Cache的原理是基于etcd的Watch机制来更新，=非0字符串且Cache中的ResourceVersion没有大于请求中的ResourceVersion时，会进行最多3秒的等待。

> 若使用的Get接口，那么kube-apiserver会取资源key的ModRevision字段填充Kubernetes资源的ResourceVersion字段（ v1. meta/ ObjectMeta.ResourceVersion）。若你使用的是List接口，kube-apiserver会在查询时，使用etcd当前版本号填充ListMeta.ResourceVersion字段（ v1. meta/ ListMeta.ResourceVersion）。
>

在Watch请求中ResourceVersion的三种取值：

* 未指定，默认为空串：一是为了帮助客户端建立初始状态，将当前已存在的资源通过Add事件返回给客户端；二是会从当前版本号开始监听，后续新增写请求导致数据变化时会及时推给客户端。
* =字符串0：帮助客户端建立初始状态，但它会从任意版本号开始监听，接下来的行为和 未指定 时一致。
* =非0字符串：从精确的版本号开始监听，只会返回大于等于精确版本号的变更事件。

## Pod

Pod是最小的API对象。

由于不同容器间需要共同协作，如war包和tomcat，就需要把它们包装成一个pod，概念类似于进程与进程组，pod并不是真实存在的，只是逻辑划分。

同一个pod里的所有容器，共享同一个Network Namespace，也可以共享同一个Volume。

这种把多个容器组合打包在一起管理的模式也称为容器设计模式。

### 原理

由于不同容器间可能存在依赖关系（如启动顺序的依赖），因此k8s会起一个中间容器Infra容器，来关联其他容器，infra容器一定是最先起的，其他容器通过Join Network Namespace的方式与Infa容器进行关联。

一个 Pod 只有一个 IP 地址，由Pod内容器共享，Pod 的生命周期只跟 Infra 容器一致，Infra管理共享资源。

对于initContainer命令的作用是按配置顺序最先执行，执行完之后才会执行container命令，例如，对war包所在容器使用initContainer命令，将war包复制到挂载的卷下后，再执行tomcat的container命令启动tomcat以此来启动web应用，这种先启动一个辅助容器来完成一些独立于主进程（主容器）之外的工作，称为sidecar，边车

Pod可以理解为一个机器，容器是里面的进程，凡是调度、网络、储存、安全相关、跟namespace相关的属性，都是Pod级别的

### Pod在K8s中的生命周期

* Pending：Pod的yaml文件已经提交给k8s了，API对象已经被创建保存在etcd中，但是这个Pod里有容器因为某些原因导致不能被顺利创建。
* Running：Pod已经调度成功，跟一个具体的节点绑定，内部容器创建成功，并且至少有一个正在运行。
* Succeeded：Pod里所有容器都正常运行完毕，并且已经退出，在运行一次性任务时比较常见。
* Failed：Pod里至少有一个容器以不正常的状态退出，需要查看Events和日志查看原因。
* Unknown：异常状态，Pod的状态不能持续通过kubelet汇报给kube-apiserver，可能是主从节点间通信出现问题。

除此之外，Pod的status字段还能细分一组Conditions，主要是描述造成当前status的具体原因，比如PodScheduled、Ready、Initialized以及Unschedulable。

在Pod的containers定义中，有个lifecycle字段，用于定义容器的状态发生变化时产生的hook。

### Side Car

在声明容器时，使用initContainers声明，用法同containers，initContainers作为辅助容器，必定比containers先启动，如果声明了多个initContainers，则会按顺序先启动，之后再启动containers。

因为Pod内所有容器共享同一个Network Namespace的特性i，nitContainers辅助容器常用于与Pod网络相关的配置和管理，比如常见的实现是Istio。

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

serviceAccountToken是一种特殊的Secret，一般用于访问Kubernetes API Server时提供token作为验证的凭证。Kubernets提供了一个默认的default Service Account，任何运行在Kubernets中的Pod都可以使用，无需显示声明挂载了它。

默认挂载路径：/var/run/secrets/kubernetes.io/serviceaccount，里面包含了ca.crt，namespace，token三个文件，用于授权当前Pod访问API Server。

如果要让Pod拥有不同访问API Server的权限，就需要不同的service account，也就需要不同的token了。

#### 挂载宿主机上的目录

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: two-containers
spec:
  restartPolicy: Never
  volumes:
  - name: shared-data
    hostPath:
      path: /data
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
```

声明了两个容器，都挂载了shared-data这个Volume，且该Volume是hostPath，对应宿主机上的/data目录，所以么，nginx-container 可 以 从 它 的/usr/share/ nginx/html 目 录 中， 读取到debian-container生 成 的 index.html文件。

### Pod的资源分配

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

Matser的kube-scheduler会根据requests的值进行计算，根据limits设置cgroup的限制，不同的requests和limits设置方式，会将Pod划分为不同的QoS类型，用于对Pod进行资源回收和调度。

1. Guaranteed：只设置了limits或者limits和requests的值一致；

   在保证是Guaranteed类型的情况下，requests和limits的CPU设置相等，此时是cpuset设置，容器会绑到某个CPU核上，不会与其他容器共享CPU算力，减少CPU上下文切换的次数，提升性能。

2. Burstable：不满足Guaranteed级别，但至少有一个Container设置了requests；

3. BestEffort：requests和limits都没有设置；

**kubelet默认的资源回收阈值**：

memory.available < 100Mi；nodefs.available < 10%；nodefs.inodesFree < 5%；imagefs.available < 15%；

达到阈值后，会对node设置状态，避免新的Pod被调度到这个node上。

> 当发生资源（Eviction）回收时的策略：
>
> 首当其冲的，自然是BestEffort类别的Pod。
>
> 其次，是属于Burstable类别、并且发生“饥饿”的资源使用量已经超出了requests的Pod。 
>
> 最后，才是Guaranteed类别。并且，Kubernetes会保证只有当Guaranteed类别的Pod的资源使用量超过了其limits的限制，或者宿主机本身正处于Memory Pressure状态时，Guaranteed的Pod才可能被选中进行Eviction操 作。

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

Pod的恢复过程，永远发生在当前节点，即跟着API对象定义的spec.node的对应的节点，如果要发生在其他节点，则需要deployment的帮助。

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

在由fannel管理的容器网络里，一个节点上的所有容器，都属于该宿主机被分配的一个子网。flannel会在宿主机上注册一个flannel0设备，保存各个节点的容器子网信息，flanneld进程会处理由flannel0传入的IP包，匹配到对应的子网，从etcd中找到该子网对应的宿主机的IP，封装成一个UDP包，交由flannel0，接着就跟节点间的网络通信一样，发送给目标节点了。因为多了一步flanneld的处理，涉及到了多次用户态与内核态间的数据拷贝，导致性能问题，优化的原则是减少切换次数，所以有了VXLAN模式、host-gw模式。

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

  StorageClass用于自动创建PV，StorageClass的定义比PV更加通用，一个StorageClass可以对应多个PV，这样就无需手动创建多个PV了。

PVC与PV的绑定条件：

1. PV和PVC的spec字段要匹配，比如存储的大小
2. PV和PVC的storageClassName字段必须一样

> 用户提交请求创建pod，Kubernetes发现这个pod声明使用了PVC，那就靠PersistentVolumeController帮它找一个PV配对。
>
> 如果没有现成的PV，就去找对应的StorageClass，帮它新创建一个PV，然后和PVC完成绑定。
>
> 新创建的PV，还只是一个API 对象，需要经过“两阶段处理”变成宿主机上的“持久化 Volume”才真正有用：
> 第一阶段Attach：由运行在master上的AttachDetachController负责，为这个PV完成 Attach 操作，为宿主机挂载远程磁盘；
> 第二阶段Mount：运行在每个节点上kubelet组件的内部，把第一步attach的远程磁盘 mount 到宿主机目录。这个控制循环叫VolumeManagerReconciler，运行在独立的Goroutine，不会阻塞kubelet主循环。
>
> 完成这两步，PV对应的“持久化 Volume”就准备好了，Pod可以正常启动，将“持久化 Volume”挂载在容器内指定的路径。当需要卸载时，则先Unmount再进行Dettach。

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
```

注意点：一般来说，一个卷只允许被挂在一个pod上，如果静态分配的PV，虽然也可以设置PVC和PV，但是pod的复制集只能有一个，且需要设置更新策略为recreate，保证只有一个pod使用该pv，否则会有写冲突；一般每个pod挂的是不同的卷。

## 控制器模型

常见的控制器有Deployment、Job、CronbJob、ReplicaSet、StatefulSet、DaemonSet等

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

与事件驱动的区别：事件驱动是被动型，接收到事件就执行相应的操作，事件是一次性，因此操作失败比较难处理，控制循环是不对轮询操作值只状态与期望一致的

### Deployment

最基本的控制器对象，管理Pod的工具，比如管理多个相同Pod的实例，滚动更新

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment
spec:
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
```

Deployment想要实现水平扩展/收缩，实际操控的是ReplicaSet对象，而ReplicaSet管理着定义数量的Pod，所以它是一种三层结构，Deployment -> ReplicaSet -> 多个平行的Pod，Deployment是一个两层控制器，Deployment控制的是ReplocaSet的版本，ReplicaSet控制的是Pod的数量。

ReplicaSet表示版本，比如上面那份配置，replicas:2是一个版本，replicas:3是一个版本，这里是因为数量不同产生两个版本，每一个版本对应着一个ReplicaSet，由Deployment管理。

当我们修改Deployment的replicas字段时，会触发水平扩展/收缩，修改template.Image或者版本号时，就会触发滚动更新。

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

滚动更新相关命令

* 使用`kubectl describe deploy xxx -n yyy`或者`kubectl rollout status deploy xxx -n yyy`即可查看滚动更新的流程；
* 当新的版本有问题时，使用`kubectl rollout undo deploy xxx [--to-revision=n]`回滚到上个版本；
* 使用`kubectl rollout history`查看每次变更的版本，但最好在执行apply -f deployment.yaml后加上-record参数；
* 如果不想每次修改都触发滚动更新，可以先使用`kubectl rollout pause deploy xx -n yy`暂停Ddeployment的行为，修改为yaml后使用`kubectl rollout resume deploy xx -n yy`恢复，让其只触发一次修改。

Deployment只适合控制无状态的Pod，如果是Pod与Pod之间有依赖关系，或者有状态时，deployment就不能随便杀掉任意的Pod再起新的Pod，比如多个数据库实例，因为数据库数据是存在磁盘，如果杀掉后重建，会出现实例与数据关系丢失，因此就需要StatefulSet。

### StatefulSet

StatefulSet可以解决两种情况下的状态：

* 拓扑状态，如果PodA和PodB有启动的先后顺序，当它们被再次创建出来时也会按照这个顺序进行启动，且新创建的Pod和原来的Pod拥有同样的网络标识（比如DNS记录），保证在使用原来的方式通信也可行。

  StatefulSet通过Headless Service，使用这个DNS记录维持Pod的拓扑状态。在声明StatefulSet时，在spec.serviceName里指定Headless Service的名称，因为serviceName的值是固定的，StatefulSet在为Pod起名字的时候又会按顺序编号，为每个Pod生成一条DNS记录，通过DNS记录里的Pod编号来进行顺序启动。

  StatefulSet只会保证DNS记录不变，Pod对应的IP还是会随着重启发生改变的。

* 存储状态，PodA第一次读取到的数据，隔了一段时间后读取到的仍然是同一份，无论其他有没有被重建过。

  StatefulSet通过PVC + PV + 编号的方式，实现 数据存储与Pod的绑定。每个Pod都会根据编号与对应的PVC绑定，当Pod被删除时，并不会删掉对应的PV，因此在起新的Pod的时候，会根据PVC找到原来的PV。

StatefulSet**直接管理**Pod，每个Pod不再认为只是复制集，而是会有hostname、名字、编号等的不同，并生成对应的带有相同编号的DNS记录，对应的带有相同编号的PVC，保证每个Pod都拥有独立的Volume。

StatefulSet的滚动更新，会按照与Pod编号相反的顺序，逐一更新，如果发生错误，滚动更新会停止；StatefulSet支持按条件更新，通过对`spec.updateStrategy.rollingUpdate的partition字段`进行配置，也可实现金丝雀部署或灰度发布。

StatefulSet可用于部署有状态的应用，比如有主从节点MySQL集群，在这个case中，虽然Pod会有相同的template，但是主从Pod里的sidecar执行的动作不一样，而主从Pod可以根据编号来实现，不同类型的Pod存储通过PVC + PV实现。

### DaemonSet

DaemonSet 的会在Kubernetes 集群里的每个节点都运行一个 Daemon Pod，每个节点只允许一个，当有新的节点加入集群后，该Pod会在新节点上被创建出来，节点被删除，该Pod也被删除。

一般的Pod都需要节点准备好了(即node的状态是Ready)才可以调度上去，但是有些Pod需要在节点还没准备好的时候就需要部署上去，比如网络相关的Pod，因此需要使用DaemonSet。

DaemonSet Controller通过 控制循环，在etcd上获取所有Node列表，判断节点上是否已经运行了标签为xxx的Pod，来保证每个节点上只有一个。可以通过在Pod上声明nodeSelector、nodeAffinity、tolerations字段告诉控制器如何选择node。

* 在node上打上标签，即可通过nodeSelector选择对应的node；
* nodeAffinity的功能比nodeSelector强大，支持更加灵活的表达式来选择节点；
* tolerations来容忍Pod在被打上污点标签的节点也可以部署，因为一般有污点的节点是不允许将Pod部署在上面的。

DaemonSet是**直接管理**Pod的，DaemonSet所管理的Pod的调度过程，都由它自己完成，而不是通过Kube-Scheduler完成， 是因为DaemonSet在创建Pod时，会为其增加spce.nodeName字段，此时以及明确了该Pod要运行在哪个节点，就不需要kube-scheduler来调度了，但也带了问题，无论节点可不可用，DaemonSet都会将该Pod往上面调度。

DaemonSet的应用一般是网络插件的Agent组件、存储插件的Agent组件、节点监控组件、节点日志收集等。

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

### CronJob

如果仍然使用Deployment管理，因为它会对退出的Pod进行滚动更新，所以并不合适，因此需要使用CronJob

作用类似于Job类似于Pod，CronJob类似于Deployment

CronJob使用 spec.schedule来控制，使用jobTemplate来定义job模板，spec.concurrencyPolicy来控制并行策略

spec.concurrencyPolicy=Allow（一个Job没执行完，新的Job就能产生）、Forbid（新Job不会被创建）、Replace（新的Job会替换旧的，没有执行完的Job）

### Operator

本质是一个Deployment，会创建一个CRD，常用于简化StatefulSet的部署，用来管理有状态的Pod，维持拓扑状态和存储状态。需要编写与Kubernetes Matser交互的代码，才能实现自定义CRD的行为。

## Service

* 工作在第四层，传输层，一般转发TCP、UDP流量。

* 每次Pod的重启都会导致IP发生变化，导致IP是不固定的，Service可以为一组相同的Pod套上一个固定的IP地址和端口，让我们能够以**TCP/IP**负载均衡的方式进行访问。

  虽然Service每次重启IP也会发生变化，但是相比Pod会更加稳定。

* 一般是pod指定一个访问端口和label，Service的selector指明绑定的Pod，配置端口映射，Service并不直接连接Pod，而是在selector选中的Pod上产生一个Endpoints资源对象，通过Service的VIP就能访问它代理的Pod了。

* service负载分发策略有两种模式：

  * RoundRobin：轮询模式，即轮询将请求转发到后端的各个pod上（默认模式）
  * SessionAffinity：基于客户端IP地址进行会话保持的模式，第一次客户端访问后端某个pod，之后的请求都转发到这个pod上

### endpoints的作用

当service使用了selector指定带有对应label的pod时，endpoint controller才会自动创建对应的endpoint对象，产生一个endpoints，endpoints信息存储在etcd中，用来记录一个service对应的所有pod的访问地址。

endpoints controller的作用

* 负责生成和维护所有endpoint对象的控制器；
* 负责监听service和对应pod的变化；
* 监听到service被删除，则删除和该service同名的endpoint对象；
* 监听到新的service被创建，则根据新建service信息获取相关pod列表，然后创建对应endpoint对象；
* 监听到service被更新，则根据更新后的service信息获取相关pod列表，然后更新对应endpoint对象；
* 监听到pod事件，则更新对应的service的endpoint对象，将pod IP记录到endpoint中；

### Service的实现

Service由kube-proxy组件 + kube-dns组件(coreDNS) + iptables或IPVS共同实现。

1. coreDNS创建时会调用kubelet修改每个节点的`/etc/resolv.conf`文件，添加coreDNS的service的clusterIP作为DNS服务的IP。
2. 当kube-proxy监听到service和endpoints对象的创建和修改后，会更新一条由service到pod路由规则，并添加到宿主机的iptables中。
3. 通过service域名请求时，会先请求coreDNS服务获取对应service的clusterIP，再根据这个ip在iptables中转发到对应的pod，iptables会负责负载均衡。

kube-proxy只是controller，对iptables进行更新，基于iptables的kube-proxy的主要职责包括两大块：

* 侦听service更新事件，并更新service相关的iptables规则；

* 侦听endpoint更新事件，更新endpoint相关的iptables规则，然后将包请求转入endpoint对应的Pod；

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

* type=NodePort，暴露virtual IP，访问这个virtual IP的时候，会将请求转发到对应的Pod，默认下nodePort会使用主机的端口范围：30000 - 32767。

  在这里，请求可能会经过SNAT操作，因为当client通过node2的地址访问一个Service，node2可能会负载均衡将请求转发给node1，node1处理完，经过SNAT，将响应返回给node2，再由node2响应回client，保证了client请求node2后得到的响应也是node2的，此时Pod只知道请求的来源，并不知道真正的发起方；

  如果Service设置了spec.extrnalTrafficPolicy=local，Pod接收到请求，可以知道真正的外部发起方是谁了。

* type=LoadBalancer，设置一个外部均衡服务，这个一般由公有云提供，比如aws的NLB，阿里云的LB服务，此时会有ExternalIP，执行LB的域名。

* type=ExternalName，并设置externalName的值，此时不会产生EndPoints、clusterIP，作用是为这个externalName设置了CName。主要用于集群内的Pod访问集群外的服务，比如ExternalName设置为www.google.com，此时pod可以直接通过` [svc名称].[namespace名称].svc.cluster.local`即可访问www.google.com

* 设置externalIPs的值，这样也能通过该ip进行访问，前提是该IP对公网暴露，比如aws的弹性ip。

* 通过port forward的方式，将Pod端口转发到执行命令的那台机器上，通过端口映射提供访问，而不需要Service。

检查网络是否有问题，一般先检查Master节点的Service DNS是否正常、kube-dns(coreDns)的运行状态和日志；

当无法通过ClusterIP访问Service访问时，检查Service是否有Endpoints，检查Kube-proxy是否正确运行；

最后检查宿主机上的iptables。

### 关于服务发现

Kubernetes支持两种服务发现：环境变量和DNS服务

#### 环境变量

创建Service时，会在Service selector的Pod中的容器注入同一namespace下所有service的IP和端口作为环境变量，该环境变量会随着Service的IP和端口的改变而改变，但设置到容器里的环境变量不会。

可以使用`kubectl exec -it {pod名字} -n {名称空间 env}`查看，HOST为service的cluster-IP

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

* 先前的服务必须先运行起来，否则环境变量无法被注入，导致Pod无法使用该环境变量
* 如果依赖的service被删了或绑定了新地址，环境变量不会被修改，仍然使用旧有地址
* 只有在同一namespace下的Pod内才可以使用此环境变量进行访问，不同namespace下无法通过变量访问

#### DNS服务

一般情况下，会使用CoreDNS作为Kubernetes集群的DNS，CoreDNS默认配置的缓存时间是30s，增大cache时间对域名解析TTL敏感型的应用有一定的影响，会延缓应用感知域名解析配置变更时间。

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

nameserver指向的IP，就是 coreDNS的service的ClusterIP，所有的域名解析都需要经过coreDNS的ClusterIP来进行解析，不论是Kubernetes内部域名还是外部域名；

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

所以，直接请求集群内部同一namespace下的域名时效率最高，因为只需要查询一次即可找到，比如在同一namespace下直接`curl {服务service名} `的效率是比`curl {服务service名}.{namespace}`要高，因为少了一次域名查询。这种DNS查询解析叫非全限定域名查找，因为我们配置了`ndots=5`，所以当域名中`.` 的个数小于5个，就会走search查询解析，如果要使用全限定域名查找，则需要在域名后面加`.`，就不会走search查询解析了，比如当`cn.bing.com.`此时就只会查询一次了，查不到才会走search查询。也可以通过修改`ndots`的数量，来减少域名查询次数的问题，可以在Pod的配置中添加`dnsConfig`修改`ndots`的值。当容器内部的`/etc/resolv.config`都找不到时，就会使用宿主机的`/etc/resolv.config`进行查找。

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

工作在第七层，应用层，即一般代理Http流量。

作用与Service类似，主要是用于对多个service的包装，作为service的service，设置一个统一的负载均衡（这样可以避免每个Service都设置一个LB，也可以避免因为节点扩缩容导致service的端口变化需要修改访问配置），设置Ingress rule，进行反向代理，实现的是Http负责均衡。

Ingress是反向代理的规则，Ingress Controller是负责解析Ingress的规则后进行转发。可以理解为Nginx，本质是将请求通过不同的规则进行路由转发。常见的Ingress Class实现有Nginx-Ingress-Controller、AWS-LB-Ingress-Controller，使用Ingress时会在集群中创建对应的controller pod。

Ingress Controller可基于Ingress资源定义的规则将客户端请求流量直接转发到Service对应的后端Pod资源上（比如aws-lb-ingress-controller 的 IP mode），其会绕过Service资源，直接转发到Pod上，省去了kube-proxy实现的端口代理开销。

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

* Role和RoleBinding：只针对某一namespace的操作授权，namespace只是逻辑上的隔离。

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

* ClusterRole和ClusterRoleBinding：针对的是整个集群的操作的授权

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

resources指的是configmaps、pods、services、deployments、nodes。

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

通过编排对象，在为它们定义服务的这种方法，就称为声明式API，

Pod就是一种API对象，每一个API对象都有一个Metadata字段，表示元数据，通过里面的labels字段（键值对）来找到这个API对象；每一个API对象都有一个Spec字段，来配置这个对象独有的配置，如为Pod添加挂载的Volume。

命令式配置文件操作：编写一个yaml配置，使用kubectl create -f config.yaml创建controller和Pod，然后修改yaml配置，使用kubectl replace -f config.yaml，更新controller和Pod，kube-apiserver一次只能处理一个命令；或者直接使用命令 kubectl set image ... 或 kubectl edit ... 这些都属于命令式的

声明式配置文件操作：编写yaml配置和更新yaml配置均使用kubectl apply -f config.yaml，kube-apiserver一次处理多个命令，并且具备merge能力。

### 工作原理

k8s根据我们提交的yaml文件创建出一个API对象，一个API对象在etcd里的完整资源路径是由Group（API 组）、Version（API 版本）和 Resource（API 资源类型）三个部分组成的

![](https://github.com/Nixum/Java-Note/raw/master/picture/API对象树形结构.png)

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

![](https://github.com/Nixum/Java-Note/raw/master/picture/控制器工作流程.png)

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

```
查看所有Pod
kubectl get pod -A

查看所有configMap
kubectl get configmap -A

查看某个configmap的内容
kubectl describe configmap confing名字 -n 命名空间

将更新的configmap内容更新到etcd中
kubectl apply -f 文件名

删除名字为xxx，namespace为yyy的pod
kubectl delete pods xxx -n yyy

查看所有Pod以及ip之类的信息
kubectl get pods --all-namespaces -o wide

进入pod里，并打开sh命令行
kubectl exec -it pod名称 -n [名称空间] sh
进入pod里的指定容器AA
kubectl exec -it pod名称 -c 容器名称 sh

查看pod 的event
kubectl get event -n [名称空间]

查看容器日志
kubectl logs [pod名称] -n [名称空间] -c[pod内容器名称] -f

查看pod的yaml内容
kubectl get pod [pod名称] -n osaas -o yaml

以yaml格式查看configmap的内容
kubectl get cm [configmap的名称] -n [命名空间] -o yaml


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
delete all -n [命名空间] --all

删除命名空间下的event
kubectl delete events -n [命名空间] --all

启动busybox调试网络
kubectl run -it --rm --restart=Never busybox --image=busybox sh

查看node标签
kubectl get nodes --show-labels

给node设置标签
kubectl label nodes [node名称] disktype=ssd

删除节点
1. 先排干上面的pod
kubectl drain [node名称] --delete-local-data --force --ignore-daemonsets
2. 删除
kubectl delete node [node名称]
如果误驱逐节点，进行恢复
kubectl uncordon [node名称]


将镜像打成压缩包
docker save -o 压缩包名字  镜像名字:标签
还原成镜像
docker load < 压缩包名字
直接使用命令，不保持容器启动
docker run --rm --name kubectl bitnami/kubectl:latest version

启动时修改entrypoint
docker run --rm -it --entrypoint env 镜像:tag /bin/bash
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

Pilot负责配置下发，将配置文件转化为Istio可识别的配置项，分发给各个sidecar代理(piloy-agent)；

Citadel负责安全、授权认证，比如证书的分发和轮换，让sidecar代理两端实现双向TLS认证、访问授权；

Mixer负责从数据平面收集数据指标以及流量策略，是一种插件组件，插件提供了很好的扩展性，独立部署，但每次修改需要重新部署，之后1.1版本将插件模块独立一个adaptor和Gallery，但是Mixer由于需要频繁的与sidecar进行通信，又是部署在应用进程外的，因此性能不高。

Gallery负责对配置信息格式和正确性校验，将配置信息提供pilot使用。

高版本1.5后中分为将Pilot、Citadel、Gallery整合为istiod，同时istiod里也包含了CA、API-Server，配合ingressgateway、egressgateway

**数据平面**：Pod中的每个Envoy容器，即istio-proxy；Envoy会以side car的方式运行在Pod中，利用Pod中的所有容器共享同一个Network Namespace的特性，通过配置Pod里的iptables规则，管理进出Pod的流量。

![](https://github.com/Nixum/Java-Note/raw/master/picture/Istio-架构.png)

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

## 应用场景

1. VirtualService和DestinationRule：按服务版本路由、按比例切分流量、根据匹配规则进行路由(比如请求头必须包含xx)、路由策略(如负载均衡，连接池)

   蓝绿部署：同时准备两套环境，控制流量流向不同环境或版本

   灰度发布(金丝雀发布)：小范围测试和发布，即按比例切分流量

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

*  `istioctl d [istiod的pod名称] -n istio-system`使用controlZ可视化自检工具，调整日志输出级别、内存使用情况、环境变量、内存信息
* `istioctl d envoy [pod名称].[命名空间] `使用Envoy的admin接口，进行Envoy的日志级别调整、性能数据分析、配置、指标信息的查看
* `kubectl port-forward service/istio-pilot -n istio-system 端口:端口`使用pilot的debug接口，查看xDS和配置信息、性能问题分析、配置同步情况
* `istioctl dashboard [controlZ/envoy/Grafana/jaeger/kiali/Prometheus/zipkin]`使用istio提供的工具
* `istioctl ps(proxy-status的缩写) [pod名称]`进行配置同步检查。
* `istioctl pc(proxy-config的缩写) [cluster/route...] [pod名称].[命名空间]`查看配置详情。

# 参考

极客时间-深入剖析k8s-张磊

极客时间-ServiceMesh实战-马若飞

[Service核心原理](https://www.lixueduan.com/post/kubernetes/04-service-core/)

