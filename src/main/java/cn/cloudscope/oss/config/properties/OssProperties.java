package cn.cloudscope.oss.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *  配置用户信息,可添加特有属性
 *
 * @author wangkp
 * @date 2022/1/25 19:24
 *
 * <pre>
 *              www.cloudscope.cn
 *      Copyright (c) 2019. All Rights Reserved.
 * </pre>
 */
@ConfigurationProperties(prefix = "oss.storage.aliyun")
public class OssProperties extends CommonProperties {
}
