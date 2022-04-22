# oss-spring-boot-starter

#### 介绍
OSS存储服务starter，默认实现Minio及阿里云OSS。

#### 软件架构
基于SpringBoot 2.7.15


#### 使用方式

1. pom.xml中直接引入：
    ```xml
    <dependency>
        <groupId>cn.cloudscope</groupId>
        <artifactId>oss-spring-boot-starter</artifactId>
        <version>1.0.0</version>
    </dependency> 
    ```
2. 默认使用minio作为实现，默认配置为本地minio服务
3. 通过修改配置
    ```yaml
    yk:
      storage:
        provider: minio/aliyun
        minio:
          end-point: http://127.0.0.1:9000
          access-key: minioadmin
          secret-key: minioadmin
    ```
4. 修改provider以切换实现
5. 自定义实现需要：
   - 自定义类实现`StorageWorker`
   - 自定义`xxConfiguration`以引入配置
   - 在`StorageAutoConfiguration`的注解中引入上述配置类
6. 在service中注入`StorageWorker`以使用它的接口方法
#### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request
