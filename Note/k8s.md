[TOC]

# docker

## 底层原理

容器技术的核心功能，就是通过约束和修改进程的动态表现，从而为其创造出一个边界。

**Namespace 技术**：进程只能看到被规定的视图，即 隔离，比如通过docker启动一个/bin/sh，再在容器里通过ps命令查看该/bin/sh进程的pid，会发现它的pid是1，但是实际上它在外部的宿主机里的pid是10，使得让在容器里运行的进程以为自己就在一个独立的空间里，实际上只是进行了逻辑的划分，本质还是依赖宿主机

与虚拟化的区别：虚拟化是在操作系统和硬件上进行隔离，虚拟机上的应用需要经过虚拟机在经过宿主机，有两个内核，本身就有消耗，而容器化后的应用仅仅只是宿主机上的进程而已，只用到宿主机一个内核

因为namespace隔离的并不彻底，由于内核共享，容器化应用仍然可以把宿主机的所有资源都吃掉，有些资源不同通过namespace隔离，比如修改了容器上的时间，宿主机上的时间也会被改变，因此需要Cgroups

**Cgroups 技术**：是用来制造约束的主要手段，即对进程设置资源能够使用的上限，如CPU、内存

比如，限定容器只能使用宿主机20%的CPU

```shell
docker run -it --cpu-period=100000 --cpu-quota=20000 ubuntu /bin/bash
```

**Mount namespace与rootfs(根文件系统)**：挂载在容器根目录上、用来为容器进程提供隔离后执行环境的文件系统，即容器镜像。

镜像可以理解为是容器的文件系统（一个操作系统的所有文件和目录），它是只读的，挂载在宿主机的一个目录上。同一台机器上的所有容器，都共享宿主机操作系统的内核，如果容器内应用修改了内核参数，会影响到所有依赖的应用。而虚拟机则都是独立的内核和文件系统，共享宿主机的硬件资源。

> 上面的读写层通常也称为容器层，下面的只读层称为镜像层，所有的增删查改操作都只会作用在容器层，相同的文件上层会覆盖掉下层。知道这一点，就不难理解镜像文件的修改，比如修改一个文件的时候，首先会从上到下查找有没有这个文件，找到，就复制到容器层中，修改，修改的结果就会作用到下层的文件，这种方式也被称为copy-on-write。

## 注意点

容器是“单进程模型”，单进程模型并不是指容器只能运行一个进程，而是指容器没有管理多个进程的能力，它只能管理一个进程，即如果在容器里启动了一个Web 应用和一个nginx，如果nginx挂了，你是不知道的。

另外，直到JDK 8u131以后，java应用才能很好的运用在docker中，在此之前可能因为docker隔离出的配置和环境，导致JVM初始化默认数值出错，因此如果使用以前的版本，需要显示设置默认配置，比如直接规定堆的最大值和最小值、线程数之类的

## 容器网络

容器网络一个Network Namespace网络栈包括：网卡、回环设备、路由表、iptables规则。

### 同一节点下容器间的通信

在 Linux 中能够起到虚拟交换机作用的网络设备，是网桥。它是一个工作在数据链路层的设备，主要功能是根据 MAC 地址来将数据包转发到网桥的不同端口（Port）上，因此Docker 项目会默认在宿主机上创建一个名叫 docker0 的网桥，凡是连接在 docker0 网桥上的容器，就可以通过它来进行通信。

容器里会有一个eth0网卡，作为默认的路由设备，连接到宿主机上一个叫vethxxx的虚拟网卡，而vethxxx网卡又插在了docker0网桥上，这一套虚拟设备就叫做Veth Pair。每个容器对应一套VethPair设备，多个容器会将其Veth Pair注册到宿主机的docker0网桥上，即Veth Pair相当于是连接不同network namespace的网线，一端在容器，一端在宿主机。

网络请求实际上就是在这些虚拟设备上进行映射（经过路由表，IP转MAC，MAC转IP）和转发，到达目的地。

**同一节点内，容器间通信一般流程**：容器A往容器B的IP发出请求，请求先经过容器A的eth0网卡，发送一个ARP广播，找到容器B IP对应的MAC地址，宿主机上的docker0网桥，把广播转发到注册到其身上的其他容器的eth0，容器B收到该广播后把MAC地址发给docker0，docker0回传给容器A，容器A发送数据包给docker0，docker0接收到数据包后，根据数据包的目的MAC地址，将其转发到容器B的eth0，

**同一节点内，宿主机与容器通信一般流程**：宿主机往容器的IP发出请求，这个请求的数据包先根据路由规则到达docker0网桥，转发到对应的Veth Pair设备上，由Veth Pair转发给容器内的应用。

### 容器访问另一节点

一个节点内的容器访问另一个节点一般流程：先经过docker0网桥，出现在宿主机上，根据路由表知道目标节点是其他机器，则将数据转发到宿主机的eth0网卡上，再发往目标节点。

其实跟同一节点内，宿主机与容器通信类似，最终转化为节点间的通信。

所以当容器无法访问外网时，就可以检查docker0网桥是否能ping通，查看docker0和Veth Pair设备的iptables规则是否有异常。

### 不同节点下容器间的通信

默认配置下，不同节点间的容器、docker0网桥，是不知道彼此的，没有任何关联，想要跨主机容器通信，就需要在多主机间在建立一个公共网桥，所有节点的容器都往这个网桥注册，才能进行通信。通过每台宿主机上有一个特殊网桥来构成这个公用网桥，这个技术被称为overlay network（覆盖网络）。

常见的解决方案是Flannel、Calico等。

![](https://github.com/Nixum/Java-Note/raw/master/Note/picture/容器网络.png)

## Dockerfile

[指令详解](https://www.cnblogs.com/panwenbin-logs/p/8007348.html)

* RUN

后面一般接shell命令，但是会构建一层镜像

要注意RUN每执行一次指令都会在docker上新键一层，如果层数太多，镜像就会太过膨胀影响性能，虽然docker允许的最大层数是127层。

有多条命令可以使用&&连接

* CMD

要注意CMD只允许有一条，如果有多条只有最后一条会生效

# Kubernetes

![](https://github.com/Nixum/Java-Note/raw/master/Note/picture/k8s项目架构.jpg)

**容器的本质是进程，Kubernetes相当于操作系统**，管理这些进程组。

* CNI：Container Network Interface，容器网络接口规范，如 Flannel、Calico、AWS VPC CNI
* kubelet：负责管理运行时的容器，这个交互依赖CRI的远程调用接口
  kubelet 还通过 gRPC 协议同一个叫作 Device Plugin 的插件进行交互。这个插件，是 Kubernetes 项目用来管理 GPU 等宿主机物理设备的主要组件
  kubelet 的另一个重要功能，则是调用网络插件和存储插件为容器配置网络和持久化存储，交互的接口是CNI和CSI
* CRI：Container Runtime Interface，容器运行时的各项核心操作的接口规范
* CSI：Container Storage Interface，容器存储的接口规范，如PV、PVC
* OCI：Open Container Initiative，容器运行时对底层操作系统的规范，CRI就是将请求翻译成对底层系统的操作
* CRD：Custom Resource Definition，自定义的控制器对象，如Operator
* Master节点作用：编排、管理、调度用户提交的作业
  * Scheduler：编排和调度Pod
  * Controller Manager：管理控制器的，比如Deployment、Job、CronbJob、RC、StatefulSet、Daemon等

## 调度器

主要职责就是为新创建的Pod寻找合适的节点，默认调度器会先调用一组叫Predicate的调度算法检查每个Node，再调用一组叫Priority的调度算法为上一步结果里的每个Node打分，将新创建的Pod调度到得分最高的Node上。

### 原理

![](https://github.com/Nixum/Java-Note/raw/master/Note/picture/k8s默认调度原理.png)

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

![](https://github.com/Nixum/Java-Note/raw/master/Note/picture/同一节点下Pod间通信.png)

#### 不同节点下Pod间的通信

不同的节点下Pod间的通信，通过一套接口规范（CNI, Container Network Interface）来实现，常见的有CNI插件实现有Flannel、Calico以及AWS VPC CNI。

其中，flannel有VXLAN、host-gw、UDP三种实现。

**flannel UDP模式下的跨主机通信**

下图container-1发送请求给container-2流程:

1. container-1发送数据包，源：100.96.1.2，目标：100.96.2.3，经过docker0，发现目标IP不存在，此时会把该数据包交由宿主机处理。
2. 通过宿主机上的路由表，发现flannel0设备可以处理该数据包，宿主机将该数据包发送给flannel0设备。
3. flannel0设备（TUM）由flanneld进程管理，数据包的处理从内核态(Linux操作系统)转向用户态(flanneld进程)，flanneld进程知道目标IP在哪个节点，就把该数据包发往node2。
4. node2对该数据包的处理，则跟node1相反，最后container2收到数据包。

![](https://github.com/Nixum/Java-Note/raw/master/Note/picture/flannel_udp跨主机通信.png)

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

![](https://github.com/Nixum/Java-Note/raw/master/Note/picture/flannel_vxlan跨主机通信.png)

数据包的发送都要经过OSI那几层模型的，经过的每一层都需要进行包装和解封，才能得到原始数据，在这期间二层网络(数据链路层)是在内核态处理，三层网络(网络层)是在用户态处理。

**flannel host-gw模式（三层网络方案）**

二层指的是在知道下一跳的IP对应的MAC地址后，直接在数据链路层通信，如果不知道，就需要在网络层设置路由表，通过路由通信，此时就是三层网络。

将每个Flannel子网的下一跳设置成该子网对应的宿主机的IP地址，用这台宿主机充当网关，Flannel子网和主机信息都保存在ETCD中，由flanneld进程WATCH这些数据的变化，实时更新路由表，这种方案的性能最好。

![](https://github.com/Nixum/Java-Note/raw/master/Note/picture/flannel_host_gw跨主机通信.png)

**calico的跨主机通信**

类似flannel host-gw模式，calico会在宿主机上创建一个路由表，维护集群内各个物理机、容器的路由规则，通过这张路由表实现跨主机通信。通过边界网关协议BGP，在集群的各个节点中实现路由信息共享。

因此calico不需要在宿主机上创建任何网桥设备，通过Veth Pair设备 + 路由表的方式，即可完成节点IP寻找和转发。

但这种方案会遇到路由表的规模问题，且最优情况是集群节点在同一个子网。

![](https://github.com/Nixum/Java-Note/raw/master/Note/picture/calico跨主机通信.png)

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

  PVC的命名方式：<PVC名字>-<StatefulSet名字>-<编号>，StatefulSet创建出来的所有Pod都会使用此PVC，Kubernetes通过Dynamic Provisioning的方式为该PVC匹配对应的PV。

* PV（Persistent Volume）：持久化卷的具体实现，即定义了持久化数据的相关属性，如数据库类型、用户名密码。

* StorageClass：创建PV的模板，只有同属于一个StorageClass的PV和PVC，才可以绑定在一起，K8s内默认有一个名字为空串的DefaultStorageClass。

  StorageClass用于自动创建PV，StorageClass的定义比PV更加通用，一个StorageClass可以对应多个PV，这样就无需手动创建多个PV了。

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

本质是一个Deployment，是一个CRD，作用跟StatefulSet类似，用来管理有状态的Pod，维持拓扑状态和存储状态。

## Service

每次Pod的重启都会导致IP发生变化，导致IP是不固定的，Service可以为一组相同的Pod套上一个固定的IP地址和端口，让我们能够以**TCP/IP**负载均衡的方式进行访问。

虽然Service每次重启IP也会发生变化，但是相比Pod会更加稳定。

一般是pod指定一个访问端口和label，Service的selector指明绑定的Pod，配置端口映射，Service并不直接连接Pod，而是在selector选中的Pod上产生一个Endpoints资源对象，通过Service的VIP就能访问它代理的Pod了。

创建Service时，会在Service selector的Pod中的容器注入同一namespace下所有service的ip和端口作为环境变量，该环境变量会随着Pod或Service的ip和端口的改变而改变，可以实现基于环境变量的服务发现，但是只有在同一namespace下的Pod内才可以使用此环境变量进行访问。

Service由kube-proxy组件 + kube-dns组件 + iptables共同实现。kube-proxy会为创建的service创建一条路由规则（由service到pod），并添加到宿主机的iptables中，所以请求经过Service会进行kube-proxy的转发。

大量的Pod会产生大量的iptables导致性能问题，kube-proxy使用IPVS模式来解决。

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

### 如何被集群外部访问

Service的本质是在宿主机上设置iptables规则、DNS映射，工作在第四层传输层。

* Service设置type=NodePort，暴露virtual IP，访问这个virtual IP的时候，会将请求转发到对应的Pod。

  在这里，请求可能会经过SNAT操作，因为当client通过node2的地址访问一个Service，node2可能会负载均衡将请求转发给node1，node1处理完，经过SNAT，将响应返回给node2，再由node2响应回client，保证了client请求node2后得到的响应也是node2的，此时Pod只知道请求的来源，并不知道真正的发起方；

  如果Service设置了spec.extrnalTrafficPolicy=local，Pod接收到请求，可以知道真正的外部发起方是谁了。

* Service设置type=LoadBalancer，设置一个外部均衡服务，这个一般由公有云提供，比如aws，阿里云的LB服务。

* Service设置type=ExternalName，并设置externalName的值，这样就可以通过externalName访问，Service会暴露DNS记录，通过访问这个DNS，解析得到DNS对应的VIP，通过VIP再转发到对应的Pod；此时也不会产生EndPoints、clusterIP。

* Service设置externalIPs的值，这样也能通过该ip进行访问。

* 可以直接通过port forward的方式，将Pod端口转发到执行命令的那台机器上，通过端口映射提供访问，而不需要Service。

检查网络是否有问题，一般先检查Master节点的Service DNS是否正常、kube-dns(coreDns)的运行状态和日志；

当无法通过ClusterIP访问Service访问时，检查Service是否有Endpoints，检查Kube-proxy是否正确运行；

最后检查宿主机上的iptables。

## Ingress

工作在第七层，应用层。

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

# Istio

Istio分为控制面板control plane或数据面板data plane，在低版本中，控制面板分为Pilot、Mixer、Citadel，数据面板则是Pod中的每个Envoy容器，即istio-proxy。Envoy会以side car的方式运行在Pod中，利用Pod中的所有容器共享同一个Network Namespace的特性，通过配置Pod里的iptables规则，管理进出Pod的流量。

## 自动注入实现

依赖Kubernetes中的Dynamic Admission Control的功能，也叫Initializer。

Istio会将Envoy容器本身的定义，以configMap的方式进行保存，当用户提交自己的Pod时，Kubernetes就会通过类似git merge的方式将两份配置进行合并。这个合并的操作会由envoy-initializer的Pod来实现，该Pod使用 循环控制，不断获取用户新创建的Pod，进行配置合并。

# 参考

极客时间-深入剖析k8s-张磊
