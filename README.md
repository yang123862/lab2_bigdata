# lab2_bigdata
```
# Hadoop MapReduce Inverted Index
Hadoop MapReduce 实现英文文本倒排索引实验

## 项目介绍
本项目使用 Hadoop MapReduce 分布式编程模型实现英文文本倒排索引。
程序读取多份英文文档，统计单词在各个文档中的出现频次，按照实验要求格式输出倒排索引：
`单词 <文档名,词频>;<文档名,总词频>;.`

输出样例：
```

so <pg15.txt,2>;<total,2>.
taste <pg19.txt,1>;<total,1>.
twill <pg24.txt,1>;<pg20.txt,2>;<pg22.txt,3>;<total,6>.

```

## 环境
- Hadoop 3.x
- JDK 8
- Ubuntu Linux

## 项目目录结构
```

├── InvertedIndex.java    # MapReduce 主程序源码
├── stopwords.txt         # 英文停用词表
└── README.md

```

## 运行步骤
1. 将文本数据集上传到 HDFS：`/user/hadoop/inverted/input`
2. 将停用词 stopwords.txt 上传至 HDFS：`/user/hadoop/inverted/stopwords.txt`

### 编译代码
```bash
javac -cp $(hadoop classpath) InvertedIndex.java
```

### 打包 jar

```
jar cvf inverted.jar *.class
```

### 提交 MapReduce 任务

```
# 删除旧输出目录
hdfs dfs -rm -r /user/hadoop/inverted/out
# 提交任务
hadoop jar inverted.jar InvertedIndex /user/hadoop/inverted/input /user/hadoop/inverted/out
# 查看结果
hdfs dfs -cat /user/hadoop/inverted/out/part-r-00000
```

## 实现原理

1. **Map 阶段**
读取文本分片，获取文档名称；文本转为小写，清洗引号、标点；分词，过滤停用词；输出键值对 `(单词, 文档名)`。
同一个单词每出现一次，就发射一条记录。
2. **Reduce 阶段**
接收同一个单词对应的所有文档记录，统计单词在每个文档的词频，计算单词全局总词频；按照规定格式拼接字符串输出最终倒排索引。

## 实验说明

这是 Hadoop 课程实验：基于 MapReduce 构建倒排索引，理解分布式 “分而治之” 的计算思想，掌握 HDFS、分布式缓存、Mapper/Reducer 开发流程。

```
