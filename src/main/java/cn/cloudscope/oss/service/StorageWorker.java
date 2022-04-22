package cn.cloudscope.oss.service;

import cn.cloudscope.oss.bean.DocumentUrlResult;
import cn.cloudscope.oss.bean.UploadResult;
import cn.cloudscope.oss.utils.FileUtil;
import cn.cloudscope.oss.utils.ImageUtil;
import cn.cloudscope.oss.utils.PathUtil;
import cn.cloudscope.oss.utils.VideoUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
    UploadResult upload(InputStream inputStream, String fileName, String folder, boolean thumbnail);

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
     * @return cn.net.idso.bean.UploadResult
     **/
    default UploadResult upload(File file, String folder) {
        UploadResult result = new UploadResult();
        try(InputStream fis = new FileInputStream(file)) {
            return upload(fis, file.getName(), folder, true);
        } catch (IOException e) {
            log.error("文件上传失败：{}", e.getMessage());
        }
        return result;
    }

    /**
     * 待实现的文件上传接口
     * @param stream    文件流
     * @param path      远程路径
     * @param originName
     * @author wenxiaopeng
     * @date 2022/7/27 16:17
     * @return  远程文件路径
     * */
    String doUpload(InputStream stream, String path, String originName);

    /**
     * 待实现的文件上传接口
     * @param file    文件
     * @param path      远程路径
     * @author wenxiaopeng
     * @date 2022/7/27 16:17
     * @return  远程文件路径
     * */
    default String doUpload(File file, String path) throws IOException {
        return doUpload(Files.newInputStream(file.toPath()), path, null);
    }

    /**
     * 上传文件
     * @param filePath  目标文件
     * @param folder    上传文件夹，可为空，且推荐为空
     * @author wenxiaopeng
     * @date 2022/7/27 14:22
     * @return cn.net.idso.bean.UploadResult
     **/
    default UploadResult upload(String filePath, String folder) {
        return upload(new File(filePath), folder);
    }

    /**
     * 上传文件
     * @param file  目标文件
     * @author wenxiaopeng
     * @date 2022/7/27 14:22
     * @return cn.net.idso.bean.UploadResult
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
    default String buildThumbnail(String path, File file) {
        try {
            String suffix = FileUtil.getFileSuffix(file.getName());

            if(ImageUtil.isImage(file)) {
                InputStream thumbnailStream = ImageUtil.buildThumbnail(new FileInputStream(file), suffix);
                if(null != thumbnailStream && thumbnailStream.available() > 0) {
                    return this.doUpload(thumbnailStream, ImageUtil.appendSuffixHyphenThumbnail(path), null);
                }

            } else {
                if (VideoUtil.isVideo(file)) {
                    InputStream frameStream = VideoUtil.captureFrame(file, 20);
                    String framePath = StringUtils.substringBeforeLast(path, ".") + ".jpg";
                    if(null != frameStream) {
                        this.doUpload(frameStream, framePath, null);
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
     * Description:
     * <添加文件后缀>
     *
     * @param fileName 文件名（可以包含路径）
     * @author wupanhua
     * @date 11:14 2020-03-03
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
    void download(String key, OutputStream response);

    /**
     * 复制文件
     * @author WZW
     * @date 14:02 2022/7/5
     * @param originPath 文件前缀路径
     * @param deleteOrigin  是否删除原文件
     **/
    String backupFile(String originPath, boolean deleteOrigin);

    /**
     * Description:根据路径删除文件
     * <>
     * @author songcx
     * @date 14:31 2021/2/2
     * @param path 1
     * @return boolean
     **/
    boolean deleteFile(String path) throws Exception;


    /**
     * 获取一个可访问的文件链接
     * @param key           文件路径
     * @param expiresIn     过期时间（秒），默认7天
     * @author wenxiaopeng
     * @date 2021/07/09 12:11
     * @return DocumentUrlResult
     **/
    DocumentUrlResult getDocumentUrl(String key, int expiresIn);


    /**
     * 上传多个文件
     * @param files
     * @return 上传文件成功后的结果集
     * @author wupanhua
     */
    UploadResult uploadMultipleFile(List<File> files);

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
    String crateFileExpireUrl(String path, int expire);

    /**
     * Description:
     * <创建一个指定有效期的图片访问链接>
     * @author wupanhua
     * @date 11:30 2020-03-03
     * @param path oss存储路径
     * @param expire 有效时间（s）
     * @return void
     */
    UploadResult createImgExpireUrl(String path, int expire);

    default String generatePath(String fileName) {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "/" + PathUtil.generatePath(fileName) + "/" + fileName;
    }
}
