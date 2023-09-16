package cn.cloudscope.oss.service.impl;

import cn.cloudscope.oss.bean.DocumentReturnCodeEnum;
import cn.cloudscope.oss.bean.DocumentUrlResult;
import cn.cloudscope.oss.bean.UploadResult;
import cn.cloudscope.oss.config.OssConfiguration;
import cn.cloudscope.oss.config.properties.OssProperties;
import cn.cloudscope.oss.service.StorageWorker;
import cn.cloudscope.oss.utils.FileUtil;
import cn.cloudscope.oss.utils.ImageUtil;
import cn.cloudscope.oss.utils.UUIDUtil;
import cn.cloudscope.oss.utils.VideoUtil;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.CopyObjectRequest;
import com.aliyun.oss.model.CopyObjectResult;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;

/**
 * @author wkp
 */
@Data
@Slf4j
@ConditionalOnBean(OssConfiguration.class)
public class AliyunWorker implements StorageWorker {

    private OSSClient ossClient;
    private OssProperties ossProperties;

    public AliyunWorker(OSSClient ossClient, OssProperties ossProperties) {
        this.ossClient = ossClient;
        this.ossProperties = ossProperties;
    }

    /**
     * 上传文件
     *
     * @param inputStream 文件流
     * @param fileName    文件名
     * @param folder      目标文件夹
     * @param thumbnail
     * @return 文件上传后的路径
     * @author wenxiaopeng
     **/
    @Override
    public UploadResult upload(InputStream inputStream, String fileName, String folder, boolean thumbnail) {
        File file = new File(UUIDUtil.buildUuid() + "." + FileUtil.getFileSuffix(fileName));
        try {
            if (inputStream.available() <= 0) {
                throw new RuntimeException(DocumentReturnCodeEnum.DOCUMENT_EMPTY.getMsg());
            }
            file.createNewFile();
            IOUtils.copyLarge(inputStream, Files.newOutputStream(file.toPath()));
            String path;
            if (StringUtils.isNotBlank(folder)) {
                path = folder.endsWith("/") ? (folder + fileName) : (folder + "/" + fileName);
            } else {
                path = this.generatePath(fileName);
            }
            this.putObjectCommonFunction(Files.newInputStream(file.toPath()), path);
            UploadResult result = UploadResult.createResult(path, fileName);
            if (thumbnail) {
                try {
                    String suffix = FileUtil.getFileSuffix(file.getName());
                    if (ImageUtil.isImage(file)) {
                        InputStream thumbnailStream = ImageUtil.buildThumbnail(new FileInputStream(file), suffix);
                        this.putObjectCommonFunction(thumbnailStream, ImageUtil.appendSuffixHyphenThumbnail(path));
                    } else {
                        if (VideoUtil.isVideo(file)) {
                            InputStream frameStream = VideoUtil.captureFrame(file, 20);
                            String framePath = StringUtils.substringBeforeLast(path, ".") + ".jpg";
                            if (null != frameStream) {
                                this.putObjectCommonFunction(frameStream, framePath);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("无法生成缩略图: {}", e.getMessage());
                }
            }
            return result;
        } catch (Exception e) {
            log.error("上传阿里云OSS服务器异常." + e.getMessage(), e);
            throw new RuntimeException(DocumentReturnCodeEnum.SERVER_UNAVAILABLE.getMsg());
        }
    }

    @Override
    public UploadResult upload(File file, String folder) {
        return StorageWorker.super.upload(file, folder);
    }

    @Override
    public String doUpload(InputStream stream, String path, String originName) {
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
            log.info("文件上传完成: {}, {}, {}", putObjectResult.getETag());
        } catch (Exception e) {
            log.error("上传失败：" + e, e);
            throw new RuntimeException(DocumentReturnCodeEnum.SERVER_UNAVAILABLE.getMsg());
        } finally {
            IOUtils.closeQuietly(stream);
        }
        return null;
    }

    @Override
    public UploadResult upload(String filePath, String folder) {
        return StorageWorker.super.upload(filePath, folder);
    }

    /**
     * 下载文件
     *
     * @param key
     * @param response
     * @return java.io.InputStream
     * @author wangkp
     * @date 13:28 2022/1/25
     * [key, response]
     */
    @Override
    public void download(String key, OutputStream response) {
        if (!ossClient.doesObjectExist(ossProperties.getBucketName(), key)) {
            throw new RuntimeException("target file not found...");
        }
        try {
            OSSObject ossObject = ossClient.getObject(ossProperties.getBucketName(), key);
            if (ossObject != null) {
                InputStream inputStream = ossObject.getObjectContent();
                int buffer = 1024 * 10;
                byte[] data = new byte[buffer];
                try {
                    int read;
                    while ((read = inputStream.read(data)) != -1) {
                        response.write(data, 0, read);
                    }
                    response.flush();
                    response.close();
                    ossObject.close();
                } catch (IOException e) {
                    log.error("error info ", e);
                }
            }
        } catch (Exception e) {
            log.error("下载失败", e);
            throw new RuntimeException(DocumentReturnCodeEnum.SERVER_UNAVAILABLE.getMsg());
        }
    }

    @Override
    public String backupFile(String originPath, boolean deleteOrigin) {
        try {
            String destinationKey = appendSuffix(originPath, SUFFIX_BACKUP);
            String sourceBucketName = ossProperties.getBucketName();
            String destinationBucketName = ossProperties.getBucketName();
            CopyObjectResult copyObjectResult = ossClient.copyObject(new CopyObjectRequest(sourceBucketName, originPath,
                    destinationBucketName, destinationKey));
            if (null != copyObjectResult) {
                if (deleteOrigin) {
                    this.deleteFile(originPath);
                }
                return copyObjectResult.getETag();
            }
            return null;
        } catch (Exception e) {
            log.error("OSS复制失败" + e, e);
            throw new RuntimeException(DocumentReturnCodeEnum.BACKUP_FAILED.getMsg());
        }
    }


    /**
     * 根据路径删除文件
     *
     * @param path 1
     * @return boolean
     * @author songcx
     * @date 14:31 2021/2/2
     **/
    @Override
    public boolean deleteFile(String path) {
        ossClient.deleteObject(ossProperties.getBucketName(), path);
        return true;
    }

    /**
     * 下载文件
     *
     * @param key 文件路径
     * @return java.io.InputStream
     * @author wenxiaopeng
     * @date 2021/07/09 12:08
     **/
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
            }finally {
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


    /**
     * 获取一个可访问的文件链接
     *
     * @param key       文件路径
     * @param expiresIn 过期时间（秒），默认7天
     * @return DocumentUrlResult
     * @author wenxiaopeng
     * @date 2021/07/09 12:11
     **/
    @Override
    public DocumentUrlResult getDocumentUrl(String key, int expiresIn) {
        if (ObjectUtils.isEmpty(key)) {
            return null;
        }
        try {
            Date expires = new Date(System.currentTimeMillis() + expiresIn * 1000L);
            String url = ossClient.generatePresignedUrl(ossProperties.getBucketName(), key, expires).toString();
            DocumentUrlResult result = DocumentUrlResult.builder()
                    .expiresIn(expiresIn)
                    .url(url)
                    .build();
            try {
                if (ImageUtil.isImage(key)) {
                    String thumbnailPath = ossClient.generatePresignedUrl(ossProperties.getBucketName(), key, expires).toString();
                    result.setThumbnail(thumbnailPath);
                }
            } catch (Exception e) {
                log.error("generate thumbnail error: {}", e.getMessage());
            }
            return result;
        } catch (Exception e) {
            log.error("下载失败", e);
            throw new RuntimeException(DocumentReturnCodeEnum.SERVER_UNAVAILABLE.getMsg());
        }
    }

    /**
     * 多文件上传
     *
     * @param files 需要上传的文件位置
     * @return 上传的结果: 文件路径和文件真实名称(带后缀名)
     * @author wupanhua
     */
    @Override
    public UploadResult uploadMultipleFile(List<File> files) {
        try {
            if (files == null || files.isEmpty()) {
                throw new RuntimeException(DocumentReturnCodeEnum.DOCUMENT_EMPTY.getMsg());
            }
            for (File file : files) {
                this.upload(file);
            }
            return null;
        } catch (Exception e) {
            log.error("多文件上传失败" + e, e);
            throw new RuntimeException(DocumentReturnCodeEnum.SERVER_UNAVAILABLE.getMsg());
        }
    }

    /**
     * 
     * <创建一个指定有效期的数据访问链接>
     *
     * @param path   oss存储路径
     * @param expire 有效时间（s）
     * @return void
     * @throws Exception 文件创建异常
     * @author wupanhua
     * @date 11:30 2020-03-03
     */
    @Override
    public String crateFileExpireUrl(String path, int expire) {
        Date expiresIn = new Date(System.currentTimeMillis() + (expire * 1000L));
        String url = ossClient.generatePresignedUrl(ossProperties.getBucketName(), path, expiresIn).toString();
        return url;
    }

    /**
     * 
     * <创建一个指定有效期的图片访问链接>
     *
     * @param path   oss存储路径
     * @param expire 有效时间（s）
     * @return void
     * @author wupanhua
     * @date 11:30 2020-03-03
     */
    @Override
    public UploadResult createImgExpireUrl(String path, int expire) {
        String originalImgUrl = this.crateFileExpireUrl(path, expire);
        String hyphenThumbnail = appendSuffix(path, SUFFIX_THUMBNAIL);
        String thumbnailUrl = this.crateFileExpireUrl(hyphenThumbnail, expire);
        return UploadResult.createThumbnailResult(originalImgUrl, thumbnailUrl);
    }


    private void putObjectCommonFunction(InputStream inputStream, String path) throws IOException {
        //创建上传Object的Metadata
        ObjectMetadata metadata = new ObjectMetadata();
        //上传的文件的长度
        metadata.setContentLength(inputStream.available());
        //指定该Object被下载时的网页的缓存行为
        metadata.setCacheControl("no-cache");
        //指定该Object下设置Header
        metadata.setHeader("Pragma", "no-cache");
        //指定该Object被下载时的内容编码格式
        metadata.setContentEncoding("utf-8");
        //文件的MIME，定义文件的类型及网页编码，决定浏览器将以什么形式、什么编码读取文件。如果用户没有指定则根据Key或文件名的扩展名生成，
        //如果没有扩展名则填默认值application/octet-stream
        metadata.setContentType(ContentType.APPLICATION_OCTET_STREAM.getMimeType());
        ossClient.putObject(ossProperties.getBucketName(), path, inputStream, metadata);
    }
}
