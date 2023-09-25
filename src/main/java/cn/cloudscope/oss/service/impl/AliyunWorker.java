package cn.cloudscope.oss.service.impl;

import cn.cloudscope.oss.bean.DocumentReturnCodeEnum;
import cn.cloudscope.oss.bean.UploadResult;
import cn.cloudscope.oss.config.properties.OssProperties;
import cn.cloudscope.oss.service.StorageWorker;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.CopyObjectRequest;
import com.aliyun.oss.model.CopyObjectResult;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

/**
 * @author wkp
 */
@Slf4j
public class AliyunWorker implements StorageWorker {

    private final OSSClient ossClient;
    private final OssProperties ossProperties;

    public AliyunWorker(OssProperties ossProperties) {
        this.ossProperties = ossProperties;
        this.ossClient = (OSSClient) new OSSClientBuilder()
                .build(ossProperties.getEndPoint(), ossProperties.getAccessKey(), ossProperties.getSecretKey());
    }

    @Override
    public String doUpload(InputStream stream, String bucket, String path, String originName) {
        try {
            if (null == stream) {
                return null;
            }
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(stream.available());
            metadata.setCacheControl("no-cache");
            metadata.setHeader("Pragma", "no-cache");
            metadata.setContentEncoding("utf-8");
            metadata.setContentType(ContentType.APPLICATION_OCTET_STREAM.getMimeType());
            if (StringUtils.isNotBlank(originName)) {
                metadata.setContentDisposition("attachment;filename=" + originName);
            }
            PutObjectResult putObjectResult = ossClient.putObject(ossProperties.getBucketName(), path, stream, metadata);
            log.info("文件上传完成: {}", putObjectResult.getETag());
            return path;
        } catch (Exception e) {
            log.error("上传失败：" + e, e);
            throw new RuntimeException(DocumentReturnCodeEnum.SERVER_UNAVAILABLE.getMsg());
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    @Override
    public String copyObject(String originPath, String target, boolean isPublic) {
        try {
            String bucket = getBucket(isPublic);
            CopyObjectResult copyObjectResult =
                    ossClient.copyObject(new CopyObjectRequest(bucket, originPath, bucket, target));
            if (null != copyObjectResult && copyObjectResult.getResponse().isSuccessful()) {
                return target;
            }
        } catch (Exception e) {
            log.error("OSS复制失败" + e, e);
            throw new RuntimeException(DocumentReturnCodeEnum.BACKUP_FAILED.getMsg());
        }
        return null;
    }

    @Override
    public String getEndpoint() {
        return ossProperties.getEndPoint();
    }

    @Override
    public boolean deleteFile(String path) {
        ossClient.deleteObject(ossProperties.getBucketName(), path);
        return true;
    }

    @Override
    public InputStream download(String key) {
        if (!ossClient.doesObjectExist(ossProperties.getBucketName(), key)) {
            throw new RuntimeException(String.valueOf(HttpStatus.SC_NOT_FOUND));
        }
        OSSObject ossObject = ossClient.getObject(ossProperties.getBucketName(), key);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        if (ossObject != null) {
            try (InputStream inputStream = ossObject.getObjectContent()) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, length);
                }
            } catch (IOException e) {
                log.error("error info ", e);
            } finally {
                try {
                    ossObject.close();
                } catch (IOException e) {
                    log.error("error info ", e);
                }
            }
            return new ByteArrayInputStream(outputStream.toByteArray());
        }
        return null;
    }

    @Override
    public String crateFileExpireUrl(String path, int expire) {
        Date expiresIn = new Date(System.currentTimeMillis() + (expire * 1000L));
        String url = ossClient.generatePresignedUrl(ossProperties.getBucketName(), path, expiresIn).toString();
        return url;
    }

    @Override
    public String getBucket(boolean isPublic) {
        return isPublic ? ossProperties.getBucketPublic() : ossProperties.getBucketName();
    }

}
