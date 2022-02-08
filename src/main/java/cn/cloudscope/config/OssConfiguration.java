package cn.cloudscope.config;

import cn.cloudscope.config.properties.OssProperties;
import cn.cloudscope.service.Impl.AliyunOSSWorker;
import cn.cloudscope.service.StorageWorker;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;

/**
 * Description: 项目配置类
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
@ConditionalOnProperty(prefix = "yk.storage", name = "provider", havingValue = "aliyun" , matchIfMissing = true)
public class OssConfiguration {

    @Resource
    private OssProperties ossProperties;

    @Bean
    public StorageWorker aliyunOSSWorker(){
        OSSClient ossClient = (OSSClient) new OSSClientBuilder().build(ossProperties.getEndPoint(),ossProperties.getAccessKey(),ossProperties.getSecretKey());
        return new AliyunOSSWorker(ossClient,ossProperties);
    }
}
