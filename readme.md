## 开发背景

为了应对馈线计量数据的数量不断增长，需要对数据库做水平分库分表。

分库分表方案中有常用的方案，hash取模和range范围方案；分库分表方案最主要就是路由算法，把路由的key按照指定的算法进行路由存放。下边来介绍一下两个方案的特点。



## 方案选型

### hash取模

在我们设计系统之前，可以先预估一下大概这一年的馈线计量数据的量，如：4000万。每张表我们可以容纳1000万，也我们可以设计4张表进行存储。

hash的方案就是对指定的路由key（如：id）对分表总数进行取模。

- 优点：
  - 计量数据可以均匀的放到不同库表中，这样取计量数据进行操作时，就不会有热点问题。

- 缺点：
  - 将来的数据迁移和扩容，会很难。
  - 如：随着时间推移我们会从系统中获得越来越多的计量数据，数据量很大，超出了原先设定的量，那我们就需要增加库表数。一旦我们增加了分表的总数，取模的基数发生改变，会导致了数据查不到。
  - 遇到这个情况，首先想到的方案就是做数据迁移，把之前的数据，重新做一个hash方案，放到新的规划分表中。也就是我们要做数据迁移。这个是很痛苦的事情。带来了很多工作量，每次扩容都要做数据迁移



### range取范围

range方案也就是以范围进行拆分数据。

range方案比较简单，就是把一定范围内的订单，存放到一个表中。

- 优点
  - 此方案比较有利于将来的扩容，不需要做数据迁移。即时再增加4张表，之前的4张表的范围不需要改变，id=12的还是在0表，id=1300万的还是在1表，新增的4张表他们的范围肯定是大于4000万之后的范围划分的。

- 缺点
  - 有热点问题，因为id的值会一直递增变大，那这段时间的馈线计量数据会一直在某一张表中，如id=1000万 ～ id=2000万之间，这个就导致1表过热，压力过大，而其他的表没有什么压力。



### 最终思路

hash是可以解决数据均匀的问题，range可以解决数据迁移问题，那我们可以不可以两者相结合呢？利用这两者的特性呢？

我们考虑一下数据的扩容代表着，路由key（如id）的值变大了，这个是一定的，那我们先保证数据变大的时候，首先用range方案让数据落地到一个范围里面。这样以后id再变大，那以前的数据是不需要迁移的。

但又要考虑到数据均匀，那是不是可以在一定的范围内数据均匀的呢？因为我们每次的扩容肯定会事先设计好这次扩容的范围大小，我们只要保证这次的范围内的数据均匀是不是就ok了。



我们先定义一个group组概念，这组里面包含了一些分库以及分表，

![image-20230914125426883](/example1.png)



按照上面的流程，我们就可以根据此规则，确定了同一个数据范围被分配到同一个组别的数据库当中，解决了难以扩容的问题。同时定位一个id，避免了热点问题。

我们看一下，id在【0，1000万】范围内的，根据上面的流程设计，1000万以内的id都均匀的分配到DB_0,DB_1两个数据库中的Table表中。



## 技术选型

因为不太想维护proxy，所以一开始是打算使用现成的shardingsphere-jdbc，但是后面学习使用发现它很难实现我的构想，所以就自己试着去写了一个路由小组件。



## 其他

路由配置

```yml
mybatis:
  mapper-locations: classpath:/mybatis/mapper/*.xml
  config-location:  classpath:/mybatis/config/mybatis-config.xml

# 路由配置
mini-db-router:
  jdbc:
    datasource:
     # 全局配置
      global:
        type-class-name: com.alibaba.druid.pool.DruidDataSource
        pool:
          maximum-pool-size: 50
     # 分库数     
      dbCount: 2
     # 分表数
      tbCount: 4
     # 分组数
      groupCount: 2
     # 组别可承载数据大小
      groupSize: 10000000
     # 数据库列表
      list: db01,db02,db03,db04
     # 若无需路由，转到默认db
      default: db01
     # 各数据库配置
      db01:
        driver-class-name: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://127.0.0.1:3306/measure_01?useUnicode=true
        username: root
        password: 123
      db02:
        driver-class-name: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://127.0.0.1:3306/measure_02?useUnicode=true
        username: root
        password: 123
      db03:
        driver-class-name: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://127.0.0.1:3306/measure_03?useUnicode=true
        username: root
        password: 123
      db04:
        driver-class-name: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://127.0.0.1:3306/measure_04?useUnicode=true
        username: root
        password: 123
```

