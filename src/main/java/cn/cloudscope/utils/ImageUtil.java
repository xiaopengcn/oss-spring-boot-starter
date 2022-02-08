package cn.cloudscope.utils;

import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Description: 图片工具
 *
 * @author wupanhua
 * @date 2020-03-03 10:47
 *
 * <pre>
 *              www.cloudscope.cn
 *      Copyright (c) 2019. All Rights Reserved.
 * </pre>
 */
public class ImageUtil {

    /**
     * Description:
     * <判别文件是否为图片>
     * @author wupanhua
     * @date 11:14 2020-03-03
     * @param srcFilePath 文件对象
     */
    public static boolean isImageType(File srcFilePath) throws IOException {
        FileInputStream is = new FileInputStream(srcFilePath);
        try {
            byte[] src = new byte[28];
            int size = is.read(src, 0, 28);
            if (size <= 0) {
                return false;
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
            ImageType[] imageTypes = ImageType.values();
            for (ImageType imageType : imageTypes) {
                if (stringBuilder.toString().startsWith(imageType.getValue())) {
                    return true;
                }
            }
            return false;
        } finally {
            IOUtils.closeQuietly(is);
        }

    }


    /**
     * 快速判断是否图片
     * @param suffix    文件后缀
     * @author wenxiaopeng
     * @date 2021/10/09 16:25
     * @return boolean
     **/
    public static boolean isImageType(String suffix) {
        if(suffix.contains(".")) {
            suffix = StringUtils.substringAfterLast(suffix, ".");
        }
        ImageType[] imageTypes = ImageType.values();
        for (ImageType imageType : imageTypes) {
            if (imageType.getExt().equalsIgnoreCase(suffix)) {
                return true;
            }
        }
        return false;
    }


    /**
     * 获取文件缩略图
     * @param file 原图片
     * @author wenxiaopeng
     * @date 2021/10/09 17:29
     * @return java.io.InputStream
     **/
    public static InputStream buildThumbnail(File file) {

        try(ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            BufferedImage bufferedImage = Thumbnails.of(file).scale(1d).asBufferedImage();
            // 图片高度
            int height = bufferedImage.getHeight();
            // 计算缩放比列
            double rate = Math.round((200d / height) * 100) * 0.01d;
            // 将图片进行缩小处理, 并对文件加入后缀名"-thumbnail"
            Thumbnails.of(file).scale(rate).outputQuality(0.7).toOutputStream(os);
            return new ByteArrayInputStream(os.toByteArray());
        } catch (Exception ignored) {
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
    public static String appendSuffixHyphenThumbnail(String fileName) {

        int indexOfDot = fileName.lastIndexOf('.');
        if (indexOfDot != -1) {
            return fileName.substring(0, indexOfDot) + "-thumbnail" + fileName.substring(indexOfDot);
        } else {
            return fileName + "-thumbnail";
        }
    }
}

/**
 * Description: 内部类，图片文件类型枚举类
 *
 * @author zhanglijun
 * @date 2020-04-22 10:47
 *
 * <pre>
 *              www.cloudscope.cn
 *      Copyright (c) 2019. All Rights Reserved.
 * </pre>
 */
enum ImageType {
    /**
     * JPEG
     */
    JPEG("FFD8FF", "jpg"),

    /**
     * PNG
     */
    PNG("89504E47", "png"),

    /**
     * GIF
     */
    GIF("47494638", "gif"),

    /**
     * TIFF
     */
    TIFF("49492A00", "tiff"),

    /**
     * Adobe photoshop
     */
    PSD("38425053"),

    /**
     * Windows bitmap
     */
    BMP("424D", "bmp");

    private String value = "";
    private String ext = "";

    ImageType(String value) {
        this.value = value;
    }

    ImageType(String value, String ext) {
        this(value);
        this.ext = ext;
    }

    public String getExt() {
        return ext;
    }

    public String getValue() {
        return value;
    }
}

/**
 * Description: 内部类，文件类型枚举类
 *
 * @author wupanhua
 * @date 2020-03-03 10:47
 *
 * <pre>
 *              www.cloudscope.cn
 *      Copyright (c) 2019. All Rights Reserved.
 * </pre>
 */
enum FileType {

    /**
     * JPEG
     */
    JPEG("FFD8FF", "jpg"),

    /**
     * PNG
     */
    PNG("89504E47", "png"),

    /**
     * GIF
     */
    GIF("47494638", "gif"),

    /**
     * TIFF
     */
    TIFF("49492A00"),

    /**
     * Windows bitmap
     */
    BMP("424D"),

    /**
     * CAD
     */
    DWG("41433130"),

    /**
     * Adobe photoshop
     */
    PSD("38425053"),

    /**
     * Rich Text Format
     */
    RTF("7B5C727466"),

    /**
     * XML
     */
    XML("3C3F786D6C"),

    /**
     * HTML
     */
    HTML("68746D6C3E"),

    /**
     * Outlook Express
     */
    DBX("CFAD12FEC5FD746F "),

    /**
     * Outlook
     */
    PST("2142444E"),

    /**
     * doc;xls;dot;ppt;xla;ppa;pps;pot;msi;sdw;db
     */
    OLE2("0xD0CF11E0A1B11AE1"),

    /**
     * Microsoft Word/Excel
     */
    XLS_DOC("D0CF11E0"),

    /**
     * Microsoft Access
     */
    MDB("5374616E64617264204A"),

    /**
     * Word Perfect
     */
    WPB("FF575043"),

    /**
     * Postscript
     */
    EPS_PS("252150532D41646F6265"),

    /**
     * Adobe Acrobat
     */
    PDF("255044462D312E"),

    /**
     * Windows Password
     */
    PWL("E3828596"),

    /**
     * ZIP Archive
     */
    ZIP("504B0304"),

    /**
     * ARAR Archive
     */
    RAR("52617221"),

    /**
     * WAVE
     */
    WAV("57415645"),

    /**
     * AVI
     */
    AVI("41564920"),

    /**
     * Real Audio
     */
    RAM("2E7261FD"),

    /**
     * Real Media
     */
    RM("2E524D46"),

    /**
     * Quicktime
     */
    MOV("6D6F6F76"),

    /**
     * Windows Media
     */
    ASF("3026B2758E66CF11"),

    /**
     * MIDI
     */
    MID("4D546864");

    private String value = "";
    private String ext = "";

    FileType(String value) {
        this.value = value;
    }

    FileType(String value, String ext) {
        this(value);
        this.ext = ext;
    }

    public String getExt() {
        return ext;
    }

    public String getValue() {
        return value;
    }

}
