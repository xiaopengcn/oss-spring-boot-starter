package cn.cloudscope.config;


import cn.cloudscope.config.properties.MinioProperties;
import cn.cloudscope.service.Impl.MinioWorker;
import cn.cloudscope.service.StorageWorker;
import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;

/**
 * Description: TODO
 *
 * @author wangkp
 * @date 2022/1/24 14:36
 *
 * <pre>
 *              www.cloudscope.cn
 *      Copyright (c) 2019. All Rights Reserved.
 * </pre>
 */
@ConditionalOnMissingBean(StorageWorker.class)
@Import(MinioProperties.class)
@ConditionalOnProperty(prefix = "yk.storage", name = "provider", havingValue = "minio" , matchIfMissing = true)
public class MinioConfiguration {

    @Resource
    private MinioProperties minioProperties;

    @Bean
    public StorageWorker minioWorker(){

        MinioClient minioClient = MinioClient.builder().credentials(minioProperties.getAccessKey(),minioProperties.getSecretKey())
                .endpoint(minioProperties.getEndPoint()).build();
        return new MinioWorker(minioClient,minioProperties);
    }
}
