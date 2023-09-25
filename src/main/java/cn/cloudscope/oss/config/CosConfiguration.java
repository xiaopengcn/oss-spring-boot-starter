package cn.cloudscope.oss.config;

import cn.cloudscope.oss.config.properties.CosProperties;
import cn.cloudscope.oss.config.properties.MinioProperties;
import cn.cloudscope.oss.config.properties.OssProperties;
import cn.cloudscope.oss.service.StorageWorker;
import cn.cloudscope.oss.service.impl.AliyunWorker;
import cn.cloudscope.oss.service.impl.TencentCosWorker;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSClientBuilder;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import org.springframework.beans.BeanUtils;
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
@Import(CosProperties.class)
@ConditionalOnProperty(prefix = "oss.storage", name = "provider", havingValue = "cos")
public class CosConfiguration {

    @Resource
    private CosProperties cosProperties;

    @Bean
    @ConditionalOnMissingBean(StorageWorker.class)
    public StorageWorker tencentCosWorker(){
        return new TencentCosWorker(cosProperties);
    }
}
