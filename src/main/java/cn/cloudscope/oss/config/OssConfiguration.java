package cn.cloudscope.oss.config;

import cn.cloudscope.oss.config.properties.OssProperties;
import cn.cloudscope.oss.service.StorageWorker;
import cn.cloudscope.oss.service.impl.AliyunWorker;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;

/**
 *  项目配置类
 *
 * @author wupanhua
 * @date 2020-02-05 14:41
 *
 * <pre>
 *              www.cloudscope.cn
 *      Copyright (c) 2019. All Rights Reserved.
 * </pre>
 */
@ConditionalOnMissingBean(StorageWorker.class)
@Import(OssProperties.class)
@ConditionalOnProperty(prefix = "oss.storage", name = "provider", havingValue = "aliyun" , matchIfMissing = true)
public class OssConfiguration {

    @Resource
    private OssProperties ossProperties;

    @Bean
    public StorageWorker aliyunOSSWorker(){
        OSSClient ossClient = (OSSClient) new OSSClientBuilder().build(ossProperties.getEndPoint(),ossProperties.getAccessKey(),ossProperties.getSecretKey());
        return new AliyunWorker(ossClient,ossProperties);
    }
}
