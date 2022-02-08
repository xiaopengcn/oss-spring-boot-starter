package cn.cloudscope.service;

import cn.cloudscope.bean.DocumentUrlResult;
import cn.cloudscope.bean.YKUPResult;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * 文件存储接口
 * @author wupanhua
 * @date 2018-09-11
 */
public interface StorageWorker {

    /**
     * 上传文件
     * @param inputStream       文件流
     * @param fileName          文件名
     * @param folder            目标文件夹
     * @author wenxiaopeng
     * @date 2021/07/09 12:07
     * @return 文件上传后的路径
     **/
    YKUPResult upload(InputStream inputStream, String fileName, String folder);

    /**
     * 上传单文件
     * @param file 需要上传的文件
     * @return 上传成功后的结果集
     * @author wupanhua
     */
    YKUPResult uploadSingleFile(File file);

    /**
     * 上传单文件
     * @param inputStream 需要上传的文件流
     * @return 上传成功后返回的结果
     * @author wupanhua
     */
    YKUPResult uploadSingleFile(InputStream inputStream);

    /**
     * 上传单文件
     * @param inputStream 需要上传的文件流
     * @param size 文件大小
     * @param fileName 文件名称
     * @return 上传成功后返回的结果
     * @author wupanhua
     */
    YKUPResult uploadSingleFile(InputStream inputStream, long size, String fileName) throws Exception;

    /**
     * Description:
     * <上传文件>
     *
     * @param path 存储文件的路径
     * @author wupanhua
     * @date 11:14 2020-03-03
     */
    String buildThumbnail(String path, File file);

    /**
     * Description:
     * <添加文件后缀>
     *
     * @param fileName 文件名（可以包含路径）
     * @author wupanhua
     * @date 11:14 2020-03-03
     */
    String appendSuffixHyphenThumbnail(String fileName);


    /**
     * 下载文件
     * @param key       文件路径
     * @author wenxiaopeng
     * @date 2021/07/09 12:08
     * @return java.io.InputStream
     **/
    InputStream download(String key);

    /**
     * 下载文件
     * @author wangkp
     * @date 13:28 2022/1/25
      [key, response]
     * @return java.io.InputStream
     **/
    void download(String key, OutputStream response);

    //TODO 复制文件 minioclient.copyObject(),复制文件夹

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
    YKUPResult uploadMultipleFile(List<File> files);

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
    YKUPResult createImgExpireUrl(String path, int expire);


}
