package cn.cloudscope.oss.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Description: 文件工具类
 *
 * @author wenxiaopeng
 * @date 2022/7/26 18:01
 *
 * <pre>
 *              www.cloudscope.cn
 *      Copyright (c) 2021. All Rights Reserved.
 * </pre>
 */
@Slf4j
public class FileUtil {

    /**
     * 获取文件签名，用于判断文件类型
     * @param file   文件
     * @author wenxiaopeng
     * @date 2022/7/26 18:03
     * @return 文件签名 nullable
     **/
    public static String getFileSignature(File file) {
        try(InputStream fis = new FileInputStream(file)) {
            return getFileSignature(fis);
        } catch (IOException ignored) {
        }
        return null;
    }

    /**
     * 获取文件签名，用于判断文件类型
     * @param stream   文件流
     * @author wenxiaopeng
     * @date 2022/7/26 18:03
     * @return java.lang.String
     **/
    public static String getFileSignature(InputStream stream) {
        try {
            byte[] src = new byte[28];
            int size = stream.read(src, 0, 28);
            if (size <= 0) {
                return null;
            }
            StringBuilder stringBuilder = new StringBuilder();
            for (byte b : src) {
                int v = b & 0xFF;
                String hv = Integer.toHexString(v).toUpperCase();
                if (hv.length() < 2) {
                    stringBuilder.append(0);
                }
                stringBuilder.append(hv);
            }
            return stringBuilder.toString();
        } catch (Exception e) {
            log.error("获取文件签名异常：{}", e.getMessage());
        } finally {
            IOUtils.closeQuietly(stream);
        }
        return null;
    }

    /**
     * 获取文件签名，用于判断文件类型
     * @param filePath   文件路径
     * @author wenxiaopeng
     * @date 2022/7/26 18:03
     * @return java.lang.String
     **/
    public static String getFileSignature(String filePath) {
        return getFileSignature(new File(filePath));
    }

    /**
     * 获取文件后缀
     * @param fileName  文件名
     * @author wenxiaopeng
     * @date 2022/7/27 14:03
     * @return java.lang.String
     **/
    public static String getFileSuffix(String fileName) {
        if(StringUtils.isNotBlank(fileName)) {
            return StringUtils.substringAfterLast(fileName, ".");
        }
        return StringUtils.EMPTY;
    }

    /**
     * 获取oss路径中的文件名
     * @param ossPath  oss
     * @author wenxiaopeng
     * @date 2022/9/6 11:33
     * @return java.lang.String
     **/
    public static String getFileName(String ossPath) {
        if(StringUtils.isNotBlank(ossPath)) {
            return StringUtils.substringAfterLast(ossPath, "/");
        }
        return ossPath;
    }
}
