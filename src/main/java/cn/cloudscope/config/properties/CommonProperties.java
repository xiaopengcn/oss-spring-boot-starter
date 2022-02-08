package cn.cloudscope.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Description: 配置文件父类
 *
 * @author wangkp
 * @date 2022/1/24 14:24
 *
 * <pre>
 *              www.cloudscope.cn
 *      Copyright (c) 2019. All Rights Reserved.
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "yk.storage")
public class CommonProperties {

    /** 是否启用 */
    private boolean enabled = true;
    /**
     * 存储方式（oss/minio),无配置，默认为minio
     */
    private Provider provider = Provider.minio;

    /**
     * 编号
     */
    private String accessKey = "minioadmin";

    /**
     * 密钥
     */
    private String secretKey = "minioadmin";

    /**
     * 应用名称
     */
    private String bucketName = "basic";

    /**
     * 服务端上传链接
     */
    private String endPoint = "http://127.0.0.1:9000";

    /**
     * 默认链接过期时间(s)
     */
    private Duration expiresIn;

    public enum Provider {
        /** 由minio提供存储服务 */
        minio,
        /** 由阿里云oss提供存储服务 */
        aliyun,
        ;
    }

}


