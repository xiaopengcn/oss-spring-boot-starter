package cn.cloudscope.config;

import cn.cloudscope.config.properties.CommonProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Description: 存储模块自动配置类
 *
 * @author wenxiaopeng
 * @date 2022/02/08 19:18
 *
 * <pre>
 *              www.cloudscope.cn
 *      Copyright (c) 2021. All Rights Reserved.
 * </pre>
 */
@Configuration
@EnableConfigurationProperties(CommonProperties.class)
@ConditionalOnProperty(prefix = "yk.storage", name = "enabled", havingValue = "true")
@Import({MinioConfiguration.class, OssConfiguration.class})
public class StorageAutoConfiguration {

}
