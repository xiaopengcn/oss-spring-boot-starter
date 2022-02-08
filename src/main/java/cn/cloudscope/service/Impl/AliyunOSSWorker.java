package cn.cloudscope.service.Impl;

import cn.cloudscope.bean.DocumentReturnCodeEnum;
import cn.cloudscope.bean.DocumentUrlResult;
import cn.cloudscope.bean.YKUPResult;
import cn.cloudscope.config.OssConfiguration;
import cn.cloudscope.config.properties.OssProperties;
import cn.cloudscope.service.StorageWorker;
import cn.cloudscope.utils.DateUtil;
import cn.cloudscope.utils.ImageUtil;
import cn.cloudscope.utils.PathUtil;
import cn.cloudscope.utils.UUIDUtil;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.name.Rename;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wkp
 */
@Data
@Slf4j
@ConditionalOnBean(OssConfiguration.class)
public class AliyunOSSWorker implements StorageWorker {

    private OSSClient ossClient;
    private OssProperties ossProperties;

    public AliyunOSSWorker(OSSClient ossClient, OssProperties ossProperties){
        this.ossClient = ossClient;
        this.ossProperties = ossProperties;
    }

    /**
     * 上传文件
     *
     * @param inputStream 文件流
     * @param fileName    文件名
     * @param folder      目标文件夹
     * @return 文件上传后的路径
     * @author wenxiaopeng
     **/
    @Override
    public YKUPResult upload(InputStream inputStream, String fileName, String folder) {
        try {
            if(inputStream.available() <= 0) {
                throw new RuntimeException(DocumentReturnCodeEnum.DOCUMENT_EMPTY.getMsg());
            }
            String path;
            if(StringUtils.isNotBlank(folder)) {
                path = folder.endsWith("/") ? (folder + fileName) : (folder + "/" + fileName);
            } else {
                path = this.generatePath(fileName);
            }
            this.putObjectCommonFunction(inputStream,path);
            YKUPResult result = YKUPResult.createResult(path, fileName);
            return result;
        } catch (Exception e) {
            log.error("上传阿里云OSS服务器异常." + e.getMessage(), e);
            throw new RuntimeException(DocumentReturnCodeEnum.SERVER_UNAVAILABLE.getMsg());
        }
    }


    /**
     * 单文件上传
     * @param file 需要上传的文件
     * @return 上传的结果: 文件路径和文件真实名称(带后缀名)
     * @author wupanhua
     */
    @Override
    public YKUPResult uploadSingleFile(File file) {
        try (FileInputStream fileOutPutStream = new FileInputStream(file)) {
            return this.uploadSingleFile(fileOutPutStream, file.length(), file.getName());
        } catch (Exception e) {
            log.error("AliyunOSS上传文件异常", e);
        }
        return null;
    }

    /**
     * 上传单文件
     * @param inputStream 需要上传的文件流
     * @return 上传成功后返回的结果
     * @author wupanhua
     */
    @Override
    public YKUPResult uploadSingleFile(InputStream inputStream) {
        ossClient.putObject(ossProperties.getBucketName(),ossProperties.getAccessKey(),inputStream);
        return null;
    }

    /**
     * 上传单文件
     * @param inputStream 需要上传的文件流
     * @param size 文件大小
     * @param fileName 文件名称
     * @return 上传成功后返回的结果
     * @author wupanhua
     */
    @Override
    public YKUPResult uploadSingleFile(InputStream inputStream, long size, String fileName) throws Exception {
        //将文件名变为uuid 防止重复
        String[] strs = fileName.split("\\.");
        strs[0] = UUIDUtil.get32UUID();
        fileName = StringUtils.join(".", strs);

        // 将文件存储到本地
        File file = new File(fileName);
        if (!file.exists()) {
            boolean newFile = file.createNewFile();
            log.debug("创建文件状态: {}", newFile);
            if (!newFile) {
                throw new RuntimeException("file system permission denied");
            }
        }
        IOUtils.copy(inputStream, new FileOutputStream(file));
        Map<String, String> headerMap = new HashMap<>(1);
        headerMap.put("Content-Type", "application/octet-stream");
        String path = DateUtil.format(new Date(), DateUtil.DateStyle.YEARMONTHDAY) + "/" + PathUtil.generatePath(UUIDUtil.get32UUID()) + "/" + fileName;
//        this.putObjectCommonFunction(inputStream,path);
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
        YKUPResult result = YKUPResult.createResult(path, fileName);
        if (ImageUtil.isImageType(file)) {
            // 构建缩略图
            String thumbnailUri = this.buildThumbnail(path, file);
            result.setThumbnail(thumbnailUri);
        }
        // 删除文件
        if (file.delete()) {
            log.info("本地文件[{}]未成功删除，可能造成存储浪费", file.getAbsolutePath());
        }
        return result;
    }

    /**
     * Description:
     * <上传文件>
     * @param path 存储文件的路径
     * @author wupanhua
     */
    @Override
    public String buildThumbnail(String path, File file) {
        try {
            BufferedImage bufferedImage = Thumbnails.of(file).scale(1d).asBufferedImage();
            // 图片高度
            int height = bufferedImage.getHeight();
            // 计算缩放比列
            double rate = Math.round((200d / height) * 100) * 0.01d;

            // 将图片进行缩小处理,并对文件加入后缀名"-thumbnail"
            Thumbnails.of(file).scale(rate).outputQuality(0.7).toFiles(Rename.SUFFIX_HYPHEN_THUMBNAIL);
            String tumbnailLocate = appendSuffixHyphenThumbnail(file.getAbsolutePath());
            File thumbnail = new File(tumbnailLocate);

            // 将缩略图上传到文件同目录
            try (FileInputStream fileInputStream = new FileInputStream(thumbnail)) {
                String thumbnailUri = appendSuffixHyphenThumbnail(path);
                Map<String, String> headerMap = new HashMap<>(1);
                headerMap.put("Content-Type", "application/octet-stream");

                this.putObjectCommonFunction(new FileInputStream(file),path);
                fileInputStream.close();
                // 删除本地缩略图文件
                if (thumbnail.delete()) {
                    log.debug("{}删除成功", thumbnailUri);
                }
                return thumbnailUri;
            }

        } catch (IOException e) {
            log.error("生成缩略图异常", e);
        }

        return null;
    }

    /**
     * Description:
     * <添加文件后缀>
     *
     * @param fileName 文件名（可以包含路径）
     * @author wupanhua
     * @date 11:14 2020-03-03
     */
    @Override
    public String appendSuffixHyphenThumbnail(String fileName) {
        String newFileName = "";

        int indexOfDot = fileName.lastIndexOf('.');

        if (indexOfDot != -1) {
            newFileName = fileName.substring(0, indexOfDot);
            newFileName += "-thumbnail";
            newFileName += fileName.substring(indexOfDot);
        } else {
            newFileName = fileName + "-thumbnail";
        }

        return newFileName;
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
        if(! ossClient.doesObjectExist(ossProperties.getBucketName(),key)){
            throw new RuntimeException("target file not found...");
        }
        try {
            OSSObject ossObject = ossClient.getObject(ossProperties.getBucketName(),key);
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
                    log.error("error info ",e);
                }
            }
        }catch (Exception e){
            log.error("下载失败", e);
            throw new RuntimeException(DocumentReturnCodeEnum.SERVER_UNAVAILABLE.getMsg());
        }
    }


    /**
     * Description:根据路径删除文件
     * <>
     * @author songcx
     * @date 14:31 2021/2/2
     * @param path 1
     * @return boolean
     **/
    @Override
    public boolean deleteFile(String path) {
        ossClient.deleteObject(ossProperties.getBucketName(),path);
        return true;
    }

    /**
     * 生成一个路径
     * @param fileName 1
     * @author wenxiaopeng
     * @date 2021/07/09 11:24
     * @return java.lang.String
     **/
    private String generatePath(String fileName) {
        return DateUtil.format(new Date(), DateUtil.DateStyle.YEARMONTHDAY) + "/" + PathUtil.generatePath(UUIDUtil.get32UUID()) + "/" + fileName;
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
        if(! ossClient.doesObjectExist(ossProperties.getBucketName(),key)){
            throw new RuntimeException(String.valueOf(HttpStatus.SC_NOT_FOUND));
        }
        OSSObject ossObject = ossClient.getObject(ossProperties.getBucketName(),key);
        if (ossObject != null) {
            InputStream inputStream = ossObject.getObjectContent();
            try {
                ossObject.close();
            } catch (IOException e) {
                log.error("error info ", e);
            }
            return inputStream;
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

        if(expiresIn < 0 || expiresIn > ossProperties.getExpiresIn().getSeconds()) {
            expiresIn = (int) ossProperties.getExpiresIn().getSeconds();
        }
        try {
            Date expires = new Date(System.currentTimeMillis() + expiresIn * 1000L);
            String url = ossClient.generatePresignedUrl(ossProperties.getBucketName(),key,expires).toString();
            DocumentUrlResult result = DocumentUrlResult.builder()
                    .expiresIn(expiresIn)
                    .url(url)
                    .build();
            try {
                if(ImageUtil.isImageType(key)) {
                    String thumbnailPath = ossClient.generatePresignedUrl(ossProperties.getBucketName(),key,expires).toString();
                    result.setThumbnail(thumbnailPath);
                }
            } catch (Exception ignored) {
            }
            return result;
        } catch (Exception e) {
            log.error("下载失败", e);
            throw new RuntimeException(DocumentReturnCodeEnum.SERVER_UNAVAILABLE.getMsg());
        }
    }

    /**
     * 多文件上传
     * @param files 需要上传的文件位置
     * @return 上传的结果: 文件路径和文件真实名称(带后缀名)
     * @author wupanhua
     */
    @Override
    public YKUPResult uploadMultipleFile(List<File> files) {
        return null;
    }

    /**
     * Description:
     * <创建一个指定有效期的数据访问链接>
     * @author wupanhua
     * @date 11:30 2020-03-03
     * @param path oss存储路径
     * @param expire 有效时间（s）
     * @exception Exception 文件创建异常
     * @return void
     */
    @Override
    public String crateFileExpireUrl(String path, int expire){
        Date expiresIn = new Date(System.currentTimeMillis() + (expire * 1000L));
        String url = ossClient.generatePresignedUrl(ossProperties.getBucketName(),path,expiresIn).toString();
        return url;
    }

    /**
     * Description:
     * <创建一个指定有效期的图片访问链接>
     * @author wupanhua
     * @date 11:30 2020-03-03
     * @param path oss存储路径
     * @param expire 有效时间（s）
     * @return void
     */
    @Override
    public YKUPResult createImgExpireUrl(String path, int expire){
        String originalImgUrl = this.crateFileExpireUrl(path, expire);
        String hyphenThumbnail = appendSuffixHyphenThumbnail(path);
        String thumbnailUrl = this.crateFileExpireUrl(hyphenThumbnail, expire);
        return YKUPResult.createThumbnailResult(originalImgUrl, thumbnailUrl);
    }

    private void putObjectCommonFunction(InputStream inputStream,String path) throws IOException {
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
