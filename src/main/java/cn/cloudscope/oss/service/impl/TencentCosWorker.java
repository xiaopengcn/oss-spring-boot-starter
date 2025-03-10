package cn.cloudscope.oss.service.impl;

import cn.cloudscope.oss.bean.UploadResult;
import cn.cloudscope.oss.config.properties.CosProperties;
import cn.cloudscope.oss.service.StorageWorker;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.VoidResult;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.CopyObjectResult;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.region.Region;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 腾讯COS实现
 *
 * @author wenxiaopeng
 * @date 2023/9/25 13:49
 *
 * <pre>
 *              <a href="www.cloudscope.cn">www.cloudscope.cn</a>
 *      Copyright (c) 2021. All Rights Reserved.
 * </pre>
 */
@Slf4j
public class TencentCosWorker implements StorageWorker {

    private final CosProperties cosProperties;

    private final COSClient cosClient;

    public TencentCosWorker(CosProperties cosProperties) {
        this.cosProperties = cosProperties;
        COSCredentials cred = new BasicCOSCredentials(cosProperties.getAccessKey()
                , cosProperties.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(cosProperties.getRegion()));
        clientConfig.setHttpProtocol(HttpProtocol.https);
        cosClient = new COSClient(cred, clientConfig);
    }

    @Override
    public String doUpload(InputStream stream, String bucket, String path, String originName) {

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(stream.available());
            metadata.setCacheControl("no-cache");
            metadata.setHeader("Pragma", "no-cache");
            metadata.setContentEncoding("utf-8");
//            metadata.setContentType(ContentType.APPLICATION_OCTET_STREAM.getMimeType());
            if (StringUtils.isNotBlank(originName)) {
                metadata.setContentDisposition("attachment;filename=" + originName);
            }
            PutObjectResult result = cosClient.putObject(bucket, path, stream, metadata);
            if(log.isDebugEnabled()) {
                log.debug("cos upload success: {}", result);
            }
            return path;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream download(String key) {
        COSObject object = cosClient.getObject(cosProperties.getBucketName(), key);
        return object.getObjectContent();
    }

    @Override
    public String copyObject(String source, String target, boolean isPublic) {
        String bucket = getBucket(isPublic);
        CopyObjectResult result = cosClient.copyObject(bucket, source, bucket, target);
        if(null != result && result.getETag() != null) {
            return target;
        }
        return null;
    }

    @Override
    public boolean deleteFile(String path) {
        cosClient.deleteObject(cosProperties.getBucketName(), path);
        return !cosClient.doesObjectExist(cosProperties.getBucketName(), path);
    }

    @Override
    public String crateFileExpireUrl(String path, int expire) {
        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(cosProperties.getBucketName(), path);
        long time = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(expire);
        generatePresignedUrlRequest.setExpiration(new Date(time));
        URL url = cosClient.generatePresignedUrl(generatePresignedUrlRequest);
        return url.toString();
    }

    @Override
    public String getEndpoint() {
        return cosProperties.getEndPoint();
    }

    @Override
    public String getBucket(boolean isPublic) {
        return isPublic ? cosProperties.getBucketPublic() : cosProperties.getBucketName();
    }
}
