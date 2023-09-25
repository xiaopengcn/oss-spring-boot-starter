package cn.cloudscope.oss.config;

import cn.cloudscope.oss.config.properties.OssProperties;
import cn.cloudscope.oss.service.StorageWorker;
import cn.cloudscope.oss.service.impl.AliyunWorker;
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
@Import(OssProperties.class)
@ConditionalOnProperty(prefix = "oss.storage", name = "provider", havingValue = "aliyun")
public class OssConfiguration {

    @Resource
    private OssProperties ossProperties;

    @Bean
    @ConditionalOnMissingBean(StorageWorker.class)
    public StorageWorker aliyunOSSWorker(){

        return new AliyunWorker(ossProperties);
    }
}
