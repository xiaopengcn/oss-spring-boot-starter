package cn.cloudscope.oss.config;


import cn.cloudscope.oss.config.properties.MinioProperties;
import cn.cloudscope.oss.service.StorageWorker;
import cn.cloudscope.oss.service.impl.MinioWorker;
import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;

/**
 *  minio配置
 *
 * @author wangkp
 * @date 2022/1/24 14:36
 *
 * <pre>
 *              www.cloudscope.cn
 *      Copyright (c) 2019. All Rights Reserved.
 * </pre>
 */
@Import(MinioProperties.class)
@ConditionalOnProperty(prefix = "oss.storage", name = "provider", havingValue = "minio" , matchIfMissing = true)
public class MinioConfiguration {

    @Resource
    private MinioProperties minioProperties;

    @Bean
    @ConditionalOnMissingBean(StorageWorker.class)
    public StorageWorker minioWorker(){
        return new MinioWorker(minioProperties);
    }
}
