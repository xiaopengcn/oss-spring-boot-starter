package cn.cloudscope.oss.service;

import cn.cloudscope.oss.bean.DocumentReturnCodeEnum;
import cn.cloudscope.oss.bean.DocumentUrlResult;
import cn.cloudscope.oss.bean.PreSingUploadParam;
import cn.cloudscope.oss.bean.UploadResult;
import cn.cloudscope.oss.utils.FileUtil;
import cn.cloudscope.oss.utils.ImageUtil;
import cn.cloudscope.oss.utils.PathUtil;
import cn.cloudscope.oss.utils.UUIDUtil;
import cn.cloudscope.oss.utils.VideoUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 文件存储接口
 * @author wupanhua
 * @date 2018-09-11
 */
public interface StorageWorker {

    Logger log = LoggerFactory.getLogger(StorageWorker.class);

    String SUFFIX_THUMBNAIL = "-thumbnail";

    String SUFFIX_BACKUP = "-backup";

    /**
     * 上传文件
     * @param inputStream       文件流
     * @param fileName          文件名
     * @param folder            目标文件夹
     * @param thumbnail         是否生成缩略图
     * @author wenxiaopeng
     * @date 2021/07/09 12:07
     * @return 文件上传后的路径
     **/
    default UploadResult upload(InputStream inputStream, String fileName, String folder, boolean thumbnail) {
        return upload(inputStream, fileName, folder, thumbnail, false);
    }

    /**
     * 上传文件
     * @param inputStream       文件流
     * @param fileName          文件名
     * @param folder            目标文件夹
     * @param thumbnail         是否生成缩略图
     * @param isPublic  上传到公开库
     * @author wenxiaopeng
     * @date 2021/07/09 12:07
     * @return 文件上传后的路径
     **/
    default UploadResult upload(InputStream inputStream, String fileName, String folder, boolean thumbnail, boolean isPublic)  {
        String path = generatePath(folder, UUIDUtil.buildUuid() + "." + FileUtil.getFileSuffix(fileName));
        String bucketName = getBucket(isPublic);
        UploadResult result = new UploadResult();
        File temp = new File(UUIDUtil.buildUuid() + "." + FileUtil.getFileSuffix(fileName));
        try(OutputStream outputStream = Files.newOutputStream(temp.toPath())) {
            IOUtils.copyLarge(inputStream, outputStream);
            String thumbnailUrl = buildThumbnail(path, bucketName, temp);
            result.setThumbnail(thumbnailUrl);
            String url = doUpload(Files.newInputStream(temp.toPath()), bucketName, path, fileName);
            result.setFileName(fileName);
            result.setPhyPath(url);
        } catch (Exception e) {
            log.error("无法生成缩略图: {}", e.getMessage());
        } finally {
                FileUtils.deleteQuietly(temp);
        }
        return result;
    }

    /**
     * 上传文件，若是图片的话，生成缩略图
     * @param inputStream       文件流
     * @param fileName          文件名
     * @param folder            目标文件夹
     * @author wenxiaopeng
     * @date 2021/07/09 12:07
     * @return 文件上传后的路径
     **/
    default UploadResult upload(InputStream inputStream, String fileName, String folder) {
        return upload(inputStream, fileName, folder, false);
    }

    /**
     * 上传文件
     * @param inputStream       文件流
     * @param fileName          文件名
     * @author wenxiaopeng
     * @date 2021/07/09 12:07
     * @return 文件上传后的路径
     **/
    default UploadResult upload(InputStream inputStream, String fileName) {
        return upload(inputStream, fileName, null, true);
    }

    /**
     * 上传文件
     * @param file  目标文件
     * @param folder    上传文件夹，可为空，且推荐为空
     * @author wenxiaopeng
     * @date 2022/7/27 14:22
     * @return cn.cloudscope.oss.bean.UploadResult
     **/
    default UploadResult upload(File file, String folder) {
        UploadResult result = new UploadResult();
        try(InputStream fis = Files.newInputStream(file.toPath())) {
            return upload(fis, file.getName(), folder, true);
        } catch (IOException e) {
            log.error("文件上传失败：{}", e.getMessage());
        }
        return result;
    }

    /**
     * 待实现的文件上传接口
     *
     * @param stream     文件流
     * @param bucket
     * @param path       远程路径
     * @param originName
     * @return 远程文件路径
     * @author wenxiaopeng
     * @date 2022/7/27 16:17
     */
    String doUpload(InputStream stream, String bucket, String path, String originName);

    /**
     * 待实现的文件上传接口
     * @param file    文件
     * @param path      远程路径
     * @author wenxiaopeng
     * @date 2022/7/27 16:17
     * @return  远程文件路径
     * @throws IOException File Not Found
     * */
    default String doUpload(File file, String bucket, String path) throws IOException {
        return doUpload(Files.newInputStream(file.toPath()), bucket, path, null);
    }

    /**
     * 生成远程文件路径
     *
     * @param folder   目标文件夹
     * @param fileName 文件名
     * @return java.lang.String
     * @author wenxiaopeng
     * @date 2022/7/27 16:37
     **/
    default String generatePath(String folder, String fileName) {
        if (StringUtils.isNotBlank(folder)) {
            return folder.endsWith("/") ? (folder + fileName) : (folder + "/" + fileName);
        } else {
            return this.generatePath(fileName);
        }
    }

    /**
     * 上传文件
     * @param filePath  目标文件
     * @param folder    上传文件夹，可为空，且推荐为空
     * @author wenxiaopeng
     * @date 2022/7/27 14:22
     * @return cn.cloudscope.oss.bean.UploadResult
     **/
    default UploadResult upload(String filePath, String folder) {
        return upload(new File(filePath), folder);
    }

    /**
     * 上传文件
     * @param file  目标文件
     * @author wenxiaopeng
     * @date 2022/7/27 14:22
     * @return cn.cloudscope.oss.bean.UploadResult
     **/
    default UploadResult upload(File file) {
        return upload(file, null);
    }

    /**
     * 单独创建文件缩略图，或可在上传时直接生成 {@link #upload(InputStream, String, String, boolean)}
     * @param path      图片OSS地址
     * @param file      原图片
     * @author wenxiaopeng
     * @date 2023/2/2 12:19
     * @return java.lang.String
     **/
    default String buildThumbnail(String path, String bucket, File file) {
        try {
            String suffix = FileUtil.getFileSuffix(file.getName());
            if(ImageUtil.isImage(file)) {
                InputStream thumbnailStream = ImageUtil.buildThumbnail(file, suffix);
                if(null != thumbnailStream && thumbnailStream.available() > 0) {
                    return this.doUpload(thumbnailStream, bucket, ImageUtil.appendSuffixHyphenThumbnail(path), null);
                }

            } else {
                if (VideoUtil.isVideo(file)) {
                    InputStream frameStream = VideoUtil.captureFrame(file, 20);
                    String framePath = StringUtils.substringBeforeLast(path, ".") + ".jpg";
                    if(null != frameStream) {
                        this.doUpload(frameStream, bucket, framePath, null);
                        return framePath;
                    }
                }
            }
        } catch (Exception e){
            log.error("无法生成缩略图: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 
     * <添加文件后缀>
     *
     * @param fileName 文件名（可以包含路径）
     * @param suffix 要添加的文件后缀
     * @author wupanhua
     * @date 11:14 2020-03-03
     * @return 添加后缀后的新文件名
     */
    default String appendSuffix(String fileName, String suffix) {

        String newFileName;
        int indexOfDot = fileName.lastIndexOf('.');
        if (indexOfDot != -1) {
            newFileName = fileName.substring(0, indexOfDot);
            newFileName += suffix;
            newFileName += fileName.substring(indexOfDot);
        } else {
            newFileName = fileName + suffix;
        }

        return newFileName;
    }

    /**
     * 下载文件
     * @param key       文件路径
     * @author wenxiaopeng
     * @date 2021/07/09 12:08
     * @return java.io.InputStream
     **/
    InputStream download(String key);

    /**
     * 下载文件到指定输出流
     * @param key   文件minio路径
     * @param response  响应流
     * @author wangkp
     * @date 13:28 2022/1/25
     **/
    default void download(String key, OutputStream response) {

        try(InputStream download = download(key);) {
            IOUtils.copyLarge(download, response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 复制文件
     *
     * @param originPath   文件前缀路径
     * @param deleteOrigin 是否删除原文件
     * @param isPublic
     * @return 备份后的文件路径
     * @author WZW
     * @date 14:02 2022/7/5
     **/
    default String backupFile(String originPath, boolean deleteOrigin, boolean isPublic) {
        try {
            String backup = appendSuffix(originPath, SUFFIX_BACKUP);
            String target = copyObject(originPath, backup, isPublic);
            if (null != target && deleteOrigin) {
                this.deleteFile(originPath);
            }
            return target;
        } catch (Exception e) {
            log.error("复制失败", e);
            throw new RuntimeException(DocumentReturnCodeEnum.BACKUP_FAILED.getMsg());
        }
    }

    /**
     * 相同桶间文件复制
     * @param source    源路径
     * @param target    目标路径
     * @param isPublic  是否公开库
     * @author wenxiaopeng
     * @since 2023/9/25 16:08
     * @return java.lang.String
     **/
    String copyObject(String source, String target, boolean isPublic);

    /**
     * 根据路径删除文件
     * @author songcx
     * @date 14:31 2021/2/2
     * @param path 1
     * @return boolean
     **/
    boolean deleteFile(String path);


    /**
     * 上传多个文件
     * @param files 待上传文件列表
     * @return 上传文件成功后的结果集
     * @author wupanhua
     */
    default List<UploadResult> uploadMultipleFile(List<File> files) {
        return files.stream().map(this::upload).collect(Collectors.toList());
    }

    /**
     * <创建一个指定有效期的数据访问链接>
     * @author wupanhua
     * @date 11:30 2020-03-03
     * @param path oss存储路径
     * @param expire 有效时间（s）
     * @return 签名后的路径，可直接访问
     */
    String crateFileExpireUrl(String path, int expire);

    /**
     * 预签名上传
     * @param param  预签名参数
     * @author wenxiaopeng
     * @date 2024/3/18 9:38
     * @return java.util.Map<java.lang.String,java.lang.String>
     **/
    default Map<String, String> preSignUpload(PreSingUploadParam param) {
        return null;
    }
    /**
     * 
     * <创建一个指定有效期的图片访问链接>
     * @author wupanhua
     * @date 11:30 2020-03-03
     * @param path oss存储路径
     * @param expire 有效时间（s）
     * @return void
     */
    default UploadResult createImgExpireUrl(String path, int expire) {
        String originalImgUrl = this.crateFileExpireUrl(path, expire);
        String hyphenThumbnail = appendSuffix(path, SUFFIX_THUMBNAIL);
        String thumbnailUrl = this.crateFileExpireUrl(hyphenThumbnail, expire);
        return UploadResult.createThumbnailResult(originalImgUrl, thumbnailUrl);
    }

    /**
     *
     * <创建一个指定有效期的图片访问链接>
     * @author wupanhua
     * @date 11:30 2020-03-03
     * @param path oss存储路径
     * @param expire 有效时间（s）
     * @return void
     */
    default DocumentUrlResult createImgExpireUrl(String path, int expire, String bucket) {
        if(StringUtils.isNotBlank(bucket) && getPublicBucket().equals(bucket)) {
            return getPublicDocumentUrl(path);
        }
        String originalImgUrl = this.crateFileExpireUrl(path, expire);
        String hyphenThumbnail = appendSuffix(path, SUFFIX_THUMBNAIL);
        String thumbnailUrl = this.crateFileExpireUrl(hyphenThumbnail, expire);
        return DocumentUrlResult.builder().url(originalImgUrl).thumbnail(thumbnailUrl).expiresIn(expire).build();
    }

    /**
     * 按规则生成文件路径（年月及文件名hash）
     * @param fileName  文件名
     * @author wenxiaopeng
     * @date 2023/9/16 14:45
     * @return java.lang.String
     **/
    default String generatePath(String fileName) {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "/" + PathUtil.generatePath(fileName) + "/" + fileName;
    }

    default UploadResult uploadFromUri(String uri, boolean keepPublic, boolean thumbnail) {

        HttpGet httpGet = new HttpGet(uri);
        try(CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse response = httpClient.execute(httpGet)) {
            Header[] headers = response.getHeaders(HttpHeaders.CONTENT_TYPE);
            String fileSuffix = FileUtil.getFileSuffix(StringUtils.substringAfterLast(uri, "/"));
            if(StringUtils.isBlank(fileSuffix) && headers.length > 0) {
                fileSuffix = suffixByContentType(headers[0].getValue());
            }
            HttpEntity entity = response.getEntity();
            return this.upload(entity.getContent(), UUID.randomUUID() + "." + fileSuffix, null, thumbnail);
        } catch (Exception e) {
            log.error("download from uri: {} error.", uri, e);
        }
        return null;
    }

    default String contentTypeByFileName(String name) {
        return TYPE_CACHE.getOrDefault(FileUtil.getFileSuffix(name), ContentType.APPLICATION_OCTET_STREAM.getMimeType());
    }

    default String suffixByContentType(String contentType) {
        AtomicReference<String> suffix = new AtomicReference<>(null);
        TYPE_CACHE.forEach((k, v) -> {
            if(v.equals(contentType)) {
                suffix.set(k);
            }
        });
        return suffix.get();
    }

    String getEndpoint();

    default String getPublicBucket() {
        return getBucket(true);
    }

    String getBucket(boolean isPublic);

    /**
     * 获取公开文档访问路径
     * @param key   文档路径
     * @author wenxiaopeng
     * @date 2023/6/16 10:30
     * @return cn.devcorp.bean.DocumentUrlResult
     **/
    default DocumentUrlResult getPublicDocumentUrl(String key) {
        if(StringUtils.isNotBlank(key)) {
            String endpoint = getEndpoint();
            String url = endpoint + "/" + getPublicBucket() + "/" + key;
            return DocumentUrlResult.builder()
                    .url(url)
                    .thumbnail(ImageUtil.appendSuffixHyphenThumbnail(url))
                    .build();
        }
        return DocumentUrlResult.builder().build();
    }

    Map<String, String> TYPE_CACHE = new HashMap<String, String>(){
        {
            put("png", ContentType.IMAGE_PNG.getMimeType());
            put("jpg", ContentType.IMAGE_JPEG.getMimeType());
            put("jpeg", ContentType.IMAGE_JPEG.getMimeType());
            put("gif", ContentType.IMAGE_GIF.getMimeType());
            put("mp4", "video/mp4");
            put("markdown", "text/markdown");
            put("pdf", "application/pdf");
            put("doc", "application/msword");
            put("docx", "application/msword");
        }
    };

}
