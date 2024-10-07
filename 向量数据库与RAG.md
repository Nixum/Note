---
title: 向量数据库与RAG
description: 简单介绍向量数据库以及在RAG中的应用，附带小demo
date: 2024-06-22
weight: -2
categories: ["向量数据库", "RAG", "AI"]
tags: ["向量数据库", "RAG", "AI"]
---

[TOC]

## 一、从场景出发

虽然目前ChatGPT等大语言模型已经十分好用了，无论是响应速度和回答的质量，基本上能解决我们日常一些问题和简单的工作，但不可否认，目前的大语言模型仍然有很多缺陷，比如：

- 回答幻觉：大语言模型回答问题的本质上是基于其已有的训练数据，**预测**(推理)出哪些可能的文字作为答案，所以难免会出现张冠李戴、胡说八道的情况，最典型的比如你在 ChatGPT-3.5问他”西红柿炒钢丝球要怎么做“，它会十分正经的回答，又或者问一些代码问题，它有时也会回答出一些不存在的语法或者方法的调用，产生不正确的答案；
- 上下文限制：比如Chat GPT-3.5 Turbo的上下文限制是4K tokens（大概3000字），GPT-4 Turbo的上下文限制是128K tokens（大概9.6万字），这意味着其最多只能处理（记忆）这么多字的内容，且随着处理的上下文越多，响应速度也会越来越慢，成本越来越高；
- 训练的语料更新不够及时：比如Chat GPT-3.5 Turbo训练的语料库只记录了2021年9月之前的数据，GPT-4 Turbo则是2023年4月，这意味着在此之后产生的数据模型是不知道的；
- 在某些领域还不够专业：比如某些垂直领域的训练语料往往比较封闭，不对外公开，GPT训练的语料不够丰富准确，进而回答的质量就会大打折扣；

为了优化上述问题，提升大语言模型回答的质量，其中一种解决方案就是在提问时向大语言模型提供更加准确且核心的资料供其参考，这个时候向量数据库就派上用场了。

## 二、向量数据库的作用

向量数据库并不是什么特别新的技术，早在机器学习场景中就有广泛应用，比如人脸识别、以图搜图、音乐软件的听音识曲等都有应用到，只是最近被大模型带火了一把。

> 向量数据库用专门的数据结构和算法来处理向量之间的相似性计算和查询。 通过构建索引结构，向量数据库可以快速找到最相似的向量，以满足各种应用场景中的查询需求。

上面是AWS上找到的对向量数据库的描述，以此代入到提升大模型回答质量的场景下，向量数据库的核心作用，就是将用户准备好的强相关性的**文本转成向量，存储到数据库中**，当用户输入问题时，也将问题转成向量，然后在数据库中进行**相似性搜索**，找到相关联的向量和上下文，进而找到对应的文本，最后跟着问题一起发送给大语言模型，从而达到减少模型的计算量，提升理解能力和响应速度，降低成本，绕过tokens限制，提高回答质量的目的，这种方式也被称为RAG（Retrieval Augmented Generation）检索增强生成。

![](https://github.com/Nixum/Java-Note/raw/master/picture/rag-flow.png)

## 三、与传统数据库功能上的差异

或许你可能会疑惑，如果用传统数据库或者es等搜索出关联的信息，再跟着问题一起发送给大语言模型，也能实现类似的效果，这样行不行？答案当然是可以，但它不是最优的，出来的效果也并不好，原因在于传统数据库的搜索功能都是基于关键字搜索，只能匹配出对应的文本，语义上的联系其实非常弱。

传统数据库都是基于B+树或者分词+倒排索引的方式进行关键字匹配和排序，得到最终结果，例如，通过传统数据库搜索”布偶猫“，只能匹配得到带有”布偶猫“这个关键字相关的结果，无法得到”银渐层“、”蓝猫“等结果，因为他们是不同的词，传统数据库无法识别他们的语义关系。

![](https://github.com/Nixum/Java-Note/raw/master/picture/rag-维度1.png)

而向量数据库是基于向量搜索的，需要我们事先将”蓝猫“，”银渐层“，”布偶“，根据他们的特征比如大小、毛发长短、颜色、习性、脸型等维度，计算出一组数字后作为他们的代表进行存储（这组数字也被称为向量），只要分解的维度足够多，就能将所有猫区分出来，然后通过计算向量间的距离来判断他们的相似度，产生语义上的联系；

![](https://github.com/Nixum/Java-Note/raw/master/picture/rag-维度2.png)

## 四、向量数据库的核心要点

- 将事物根据特征转换为不同维度的向量的过程，就叫做**vector embedding向量嵌入：**
- 通过计算多个向量之间的距离来判断它们的相似度，就叫做**similarity search相似性搜索；**

这两个步骤，都决定着搜索质量的好坏。

### 4.1 向量嵌入

以大语言模型对话的场景来说，涉及的语料就是大量的文本了，而文本包含的特征可以是词汇、语法、语义、情感、情绪、主题、上下文等等，这些特征太多了，可能需要几百上千个特征才能区分出一段文本表达的含义，很难进行人为的标注，因此需要有一种自动化的方式来提取这些特征，这就可以通过 vector embedding来实现。

这一步其实并不属于向量数据库的功能，更像是数据入库前的前置操作，向量数据库本事只提供存储向量和搜索的功能。

现在常用的大语言模型，基本都提供了embedding接口，供用户把文本转换为向量，比如 OpenAI的text-embedding-ada-002模型可以把文本分解成1536维的向量，网易的bce-embedding-base_v1模型，可以把文本分解为768维的向量等等，具体排名可以参考[HuggingFace的大文本嵌入排行](https://huggingface.co/spaces/mteb/leaderboard)，多少维其实就是一个长度多少的浮点类型数组，数组内的每个元素，则代表被分解的文本的特征，共同组成一个信息密集的表示。

那对于给定的文本，要如何分割，以及分解出多少个向量合适呢？

如果文本分割的粒度把控不好，可能会导致分割出来的无用信息太多，或者语义丢失，语义关联性不大等问题。对于文本分割，这里找到了一篇写得很好的文章：[文本分割的五个层次](https://baoyu.io/translations/rag/5-levels-of-text-splitting)：

- 第 1 层：字符分割，比如按一定的字符数、块大小分割文本，不考虑其内容和形式。
- 第 2 层：基于分隔符分割，比如按句号、换行符、空格等进行文本切割。
- 第 3 层：文档类型分割，比如PDF、Markdown都有特定的语法表示语义分割，使得分割出来的文本关联性更强。
- 第 4 层：语义分割，比如每三句话转成向量，然后去掉第一句，加上下一句，再转成向量，比较两个向量的距离，如果距离超过一定的阈值，说明找到分割点。
- 第 5 层：使用大语言模型分割，使用适合的prompt指导大语言模型推理分割。

简单来说，我们更倾向于把上下文关联性强的文本合一起分割，得到的整体效果最好，下面的demo，就是按第4层的分割方式，可以参考一下。

### 4.2 相似性搜索

具体可以看这个[视频](https://www.bilibili.com/video/BV11a4y1c7SW)，有上下两集，讲得非常容易理解，这里仅作简单归纳。

现在我们已经将文本转换为向量存储在向量数据库中，如果想要在海量的数据里找到某个相似的向量，计算量会非常大，因此需要一种高效的算法来解决这个问题，类比到传统数据库，就是通过B+树建立索引进行查找，本质都是减少查询范围，从而快速找到结果。

在向量数据库中有两种主要的搜索方式：

1. 减少向量大小，对向量降维；
2. 减少搜索范围，通过聚类或者将向量组织成树形、图形结构实现，限制搜索范围只在特定的聚类中过滤；

这里简单介绍几种算法：

- K-Means：

在保存向量数据后，对向量数据先进行聚类（比如随机选择某几个点），然后将这几个点最近的向量分配到这个聚类中，然后不断调整聚类的质心，形成不同的簇，这样，每次搜索时，只要先判断要搜索的向量属于哪个簇，然后再在簇中进行搜索，从而减少搜索范围。如果要搜索的向量刚好处在两个聚类的边界上，则只能动态调整搜索范围，搜索其他簇

如下图，在一个二维坐标系中划定4个聚类中心，形成4个簇；

![](https://github.com/Nixum/Java-Note/raw/master/picture/rag-簇.png)

- 积量化Product Quantization，PQ：

随着数据规模的增大和维度的增加，数据点间的距离也会呈指数级增长，聚类算法需要分割更多的聚类（否则会导致向量和自己聚类的中心距离太远，降低搜索速度和质量），而且消耗的内存也会增加，解决这个问题的方法是将向量分解为多个子向量，然后再对每个子向量独立进行量化（量化的意思就是通过质心进行编号形成码本，在此聚类中的向量都对应这个编号，从而不用存储完整向量），从而实现降维，但代价就是搜索的质量会下降。

如下图，在一个二维的坐标系中的四个聚类，每个聚类中的向量都用质心向量来替代表示，这样就只剩下4个向量了，然后只要维护好这4个向量形成的码本，就能极大的降低内存开销。(码本的作用是记录原始向量对质心的映射，有点类似操作系统中的内存多级分页算法)

![](https://github.com/Nixum/Java-Note/raw/master/picture/rag-聚类1.png)

![](https://github.com/Nixum/Java-Note/raw/master/picture/rag-聚类2.png)

当向量的维度越高，向量分布越稀疏，形成的聚类也就越多，单纯根据质心构建码本的方式会导致码本的存储开销越来越大，比如一个128维的向量空间，如果直接按聚类分需要分为2^64个质心才能保证搜索质量，此时的质心编码和向量值的码本的内存消耗将巨大，甚至大于量化本身所节省下来的内存；

此时就需要降维，将128维的向量分成8个16维的子向量，再对8个16维的子空间中进行k-means聚类训练，从而降低聚类的数量，此时一个向量被量化为8个编码值，同时每个子空间也会构建自己的子码本（此时只需要保存这8个子码本即可），使用时用8个编码值分别从对应的子码本中查询出8个16维的子向量再拼起来复原出一个128维的向量。

![](https://github.com/Nixum/Java-Note/raw/master/picture/rag-降维1.png)

![](https://github.com/Nixum/Java-Note/raw/master/picture/rag-降维2.png)

- 局部敏感哈希Locality Sensitive Hashing，LSH

可以理解为反向哈希，以往我们都期望往哈希表里添加数据，都期望减少哈希碰撞的次数，即桶上的数据越少越好，这样方便我们快速找到对应的value，但是在向量搜索中，因为是为了找到相似的向量，所以我们期望哈希碰撞的次数尽可能的高，这样相似的向量都会落在一个桶上。

这些算法本身就在查询速度、查询质量、内存开销上进行取舍，做出一个权衡。

### 4.3 相似性度量

判断两个向量是否相似，其实就是计算出两个向量间的距离，根据距离来判断他们的相似度，常见的有三种相似度算法：

- 欧几里得距离：

欧几里得距离是指两个向量之间的距离，它的计算公式为：𝑑(𝐴,𝐵)=∑𝑖=1𝑛(𝐴𝑖−𝐵𝑖)2*d*(**A**,**B**)=*i*=1∑*n*(*Ai*−*Bi*)2

![](https://github.com/Nixum/Java-Note/raw/master/picture/rag-欧几里得.png)

- 余弦相似度：

余弦相似度是指两个向量之间的夹角余弦值，它的计算公式为：cos⁡(𝜃)=𝐴⋅𝐵∣𝐴∣∣𝐵∣cos(*θ*)=∣A∣∣B∣A⋅B

![](https://github.com/Nixum/Java-Note/raw/master/picture/rag-余弦.png)

- 点积相似度：

向量的点积相似度是指两个向量之间的点积值，它的计算公式为：𝐴⋅𝐵=∑𝑖=1𝑛𝐴𝑖𝐵𝑖**A**⋅**B**=*i*=1∑*nAiBi*

![](https://github.com/Nixum/Java-Note/raw/master/picture/rag-点积.png)

都是纯数学的直接代入公式即可得出结果，计算出一个数值，然后跟我们设定的相似度阈值做比较，小于该阈值说明非常相似，大于阈值说明不相似。

### 4.4 过滤

向量数据库也具备传统数据库那种可以根据部分业务字段进行过滤，之后再进行相似性查询，这些字段构成的就称为元数据，所以向量数据库通常需要维护两个索引，一个是向量索引，另一个是元数据索引，两者相结合从而快速找到需要的数据。

## 五、常见的向量数据库

| DB                                                           | 是否开源               | 功能简述                                                     |
| ------------------------------------------------------------ | ---------------------- | ------------------------------------------------------------ |
| [Chroma](https://github.com/chroma-core/chroma)              | 是                     | 简单：类型完整、测试全面、文档完整整合：支持LangChain（python和js）、LlamaIndex等等 |
| [Pinecone](https://www.pinecone.io/)                         | 否                     | 相似性搜索、推荐系统、个性化和语义搜索免费版可以支持500w的向量存储，其用法简单，价格低廉，可以快速支持向量检索业务的验证与尝试。 |
| [Weaviate](https://github.com/weaviate/weaviate)             | 是                     | 向量搜索，语义搜索、推荐系统可以存储对象、向量，支持将矢量搜索与结构化过滤与云原生数据库容错和可拓展性等能力相结合。 支持GraphQL、REST和各种语言的客户端访问 |
| [Milvus](https://github.com/milvus-io/milvus)                | 是，云原生版本为zilliz | 对包含数百万、数十亿甚至数万亿个向量的密集向量数据集进行相似性搜索；支持万亿向量数据集上的毫秒级搜索：在万亿向量数据集上测试平均延迟（毫秒级）云原生版本有免费额度，不过只支持创建两个collection |
| [Faiss](https://github.com/facebookresearch/faiss)           | 是                     | 图像识别、语义搜索Facebook背书                               |
| [Annoy](https://github.com/spotify/annoy)                    | 是                     | Spotify背书，基于随机投影和树的算法，支持多种相似算法低维度效果会更好（比如<=100），但即使是1000维的维度，它的表现也还是非常优秀 |
| [Elasticsearch  8.0 以上版本](https://github.com/elastic/elasticsearch) | 是                     | 实现文本的语义搜索或者图像、视频或音频的相似度搜索提供了基础 |
| [PostgreSQL + pgvector插件](https://supabase.com/blog/openai-embeddings-postgres-vector) | 是                     | 支持精确和近似最近搜索（ANN)，提供三种距离即使方法：欧几里得距离、余弦距离、内积 |

## 六、RAG demo

强烈建议可以看[langchain的官方文档](https://python.langchain.com/v0.2/docs/introduction/)，写得非常详细且清晰，demo也很容易跑起来，而且也可以在他们的官方文档上体验一下相似性搜索。

这里也基于langchain框架，使用chroma作为向量数据库，使用ollama管理的本地大模型llama2-chinese（也用它作为vector embedding），文本预处理使用[文本分割的五个层次](https://baoyu.io/translations/rag/5-levels-of-text-splitting)提到的第四场的方法，然后按照下面的流程，实现一个简单的RAG demo。

只是一个小demo，体验一下RAG的流程而已，出来的效果不一定很好哈。

![](https://github.com/Nixum/Java-Note/raw/master/picture/rag-flow.png)

- 安装ollama作为本地LLM，跟着它[github](https://github.com/ollama/ollama)上的步骤进行安装即可，我测试的时候是用docker进行安装的，然后指定它跑在GPU上，不然推理答案的速度太慢了；
- 准备一篇markdown文章，就可以开始写代码了，这里把整个流程分割成多个步骤，每个步骤可以独立运行；

1. 将markdown文章进行预处理，分割成文本块：

```Python
from langchain_community.llms import Ollama
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser
from langchain_community.document_loaders import UnstructuredMarkdownLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_community.vectorstores import Chroma
from langchain_community.embeddings import OllamaEmbeddings
from langchain.chains import create_retrieval_chain
from langchain.chains.combine_documents import create_stuff_documents_chain
import json
import codecs

# 解析 md 内容，并llama2-chinese作为嵌入模型计算向量，然后转成 json 文件

markdown_path = "markdown文章的路径"
loader = UnstructuredMarkdownLoader(markdown_path, mode="elements")
data = loader.load()

embeddings = OllamaEmbeddings(model="llama2-chinese")
sentences = []
for i, item in enumerate(data):
        vectors = embeddings.embed_documents(item.page_content)
        sentences.append({
                'sentence': item.page_content,
                'index': i,
                'vectors': vectors
        })
        # 打印内容及其转换的向量
        print(item.page_content + "  " + vectors + "\n")

# 将转换结果写入json文件中
sentencesJson = json.dumps(sentences, ensure_ascii=False)
with codecs.open('sentences.json',"w","utf-8") as f:
    f.write(sentencesJson)
```

2. 将上一步得到的文本 + 嵌入的向量，存入到 chroma DB中：

```Python
from langchain_community.llms import Ollama
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser
from langchain_community.document_loaders import UnstructuredMarkdownLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_community.vectorstores import Chroma
from langchain_community.embeddings import OllamaEmbeddings
from langchain.chains import create_retrieval_chain
from langchain.chains.combine_documents import create_stuff_documents_chain
import json
import codecs
import chromadb

# 加载上一步拆分出来的文本及其向量值
sentences = []
with open("sentences.json", "rb") as f:
    sentence = json.load(f)

# 构建一个chroma db实例
client = chromadb.PersistentClient(path="./chroma_db")
# 获取要存储的collection
collection = client.get_or_create_collection("demo")
# 构建要保存的内容
embeddingList = []
docList = []
ids = []
for i, item in enumerate(sentences):
        embeddingList.append(item["vectors"])
        docList.append(item["sentence"])
        ids.append("1-" + str(item["index"]))
# 保存到chroma中
collection.add(
    embeddings=embeddingList,
    documents=docList,
    ids=ids
)
```

3. 向 ollama 提问，此时会先基于问题，使用llama2-chinese嵌入模型转换为向量，在chroma 中进行相似性搜索，查找出对应的文本，然后将检索得到的文本 + 问题，一起发给ollama，由ollama的llama2-chinese模型推理给出答案；

```Python
from langchain_community.llms import Ollama
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser
from langchain_community.document_loaders import UnstructuredMarkdownLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_community.vectorstores import Chroma
from langchain_community.embeddings import OllamaEmbeddings
from langchain.chains import create_retrieval_chain
from langchain.chains.combine_documents import create_stuff_documents_chain

# 加载嵌入模型，将问题转换成向量，这一步只是为了打印相似性搜索的结果
embeddings = OllamaEmbeddings(model="llama2-chinese")
db = Chroma(persist_directory="./chroma_db",
        collection_name="demo",
        embedding_function=embeddings)
query = "这里改成想要问的问题"
docs = db.similarity_search(query)
print("根据问题从db中相似性搜索出来的文本：" + docs[0].page_content)

# ----------------------------------------------------------
# 加载LLM模型
llm = Ollama(model="llama2-chinese")
prompt = ChatPromptTemplate.from_template("""仅依据下面提供的上下文，回答我接下来的问题:
<context>
{context}
</context>
问题: {input}""")

# 这里langchain已经帮我们整合了 “根据问题从db中相似性搜索出来的答案” 这个步骤了
document_chain = create_stuff_documents_chain(llm, prompt)
retriever = db.as_retriever()
retrieval_chain = create_retrieval_chain(retriever, document_chain)
# 发送问题+相关联的文本，从而实现检索增强，即RAG
response = retrieval_chain.invoke({"input": query})
print(response["answer"])
```

## 七、其他参考

[RAG + 向量数据库科普](https://www.bilibili.com/video/BV1JF4m177Wd)

[基于langchain 的文档问答 最佳实践](https://juejin.cn/post/7250794190120353847)

[用GPT-4和ChromaDB与文本文件对话教程](https://cloud.tencent.com/developer/article/2311302)

[基于LLM+向量库的文档对话痛点及解决方案](https://zhuanlan.zhihu.com/p/651179780)

[LLM+Embedding构建问答系统的局限性及优化方案](https://zhuanlan.zhihu.com/p/641132245)

[langchain - How to split Markdown by Headers](https://python.langchain.com/v0.2/docs/how_to/markdown_header_metadata_splitter/)

[langchain入门](https://aitutor.liduos.com/02-langchain/02-1.html)

[chatgpt原理1](https://www.wehelpwin.com/article/4387)

[chatgpt原理2](https://brightliao.com/2023/04/25/chatgpt-a-technical-summary/)

