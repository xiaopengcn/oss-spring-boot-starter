package cn.cloudscope.oss.utils;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.apache.tika.mime.MediaType;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import static com.drew.metadata.eps.EpsDirectory.TAG_ORIENTATION;

/**
 *  图片工具
 *
 * @author wupanhua
 * @date 2020-03-03 10:47
 *
 * <pre>
 *              www.cloudscope.cn
 *      Copyright (c) 2019. All Rights Reserved.
 * </pre>
 */
@Slf4j
public class ImageUtil {

    private static final String SUFFIX_THUMBNAIL = "-thumbnail";

    private ImageUtil() {
    }

    /**
     * 获取图片拍摄角度
     * @param inputStream   输入图片流
     * @author wenxiaopeng
     * @date 2023/9/16 14:57
     * @return java.lang.Double 图片拍摄角度
     **/
    public static Double getRotate(BufferedInputStream inputStream) {
        try {
            inputStream.mark(inputStream.available() + 1);
            Metadata metadata = ImageMetadataReader.readMetadata(inputStream);
            Iterable<Directory> directories = metadata.getDirectories();
            for (Directory directory : directories) {
                if ("Exif IFD0".equals(directory.getName())) {
                    Collection<Tag> tags = directory.getTags();
                    for (Tag tag : tags) {
                        if (tag.getTagType() == TAG_ORIENTATION) {
                            return getRealRotate(tag.getDescription());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取图片角度异常: {}", e.getMessage());
        } finally {
            try {
                inputStream.reset();
            } catch (IOException ignored) {
            }
        }
        return 0d;
    }

    /**
     * 获取真实角度
     * @param description   角度描述
     * @author wenxiaopeng
     * @date 2022/8/2 15:55
     * @return java.lang.Double
     **/
    private static Double getRealRotate(String description) {
        // Right side, top (Rotate 90 CW)
        // Top, left side (Horizontal / normal)
        // Bottom, right side (Rotate 180)
        // Right side, bottom (Mirror horizontal and rotate 90 CW)
        if(description.startsWith("Right side")) {
            return 90d;
        }
        if(description.startsWith("Bottom, right side")) {
            return 180d;
        }
        if(description.startsWith("Right side, bottom")) {
            return -90d;
        }
        return 0d;
    }

    /**
     * 快速判断是否图片
     * @param suffix    文件后缀
     * @author wenxiaopeng
     * @date 2021/10/09 16:25
     * @return boolean
     **/
    public static boolean isImage(String suffix) {
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
    public static InputStream buildThumbnail(File file, String suffix) {

        try(ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            BufferedImage bufferedImage = ImageIO.read(file);
            // 将图片进行缩小处理, 并对文件加入后缀名"-thumbnail"
            Thumbnails.of(bufferedImage)
                    .scale(Math.min(1, 200 * 1024F / file.length()))
                    .outputFormat(suffix)
                    .toOutputStream(os);
            return new ByteArrayInputStream(os.toByteArray());
        } catch (Exception e) {
            log.error("创建缩略图异常: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 
     * <添加文件后缀>
     *
     * @param fileName 文件名（可以包含路径）
     * @author wupanhua
     * @date 11:14 2020-03-03
     */
    public static String appendSuffixHyphenThumbnail(String fileName) {

        int indexOfDot = fileName.lastIndexOf('.');
        if (indexOfDot != -1) {
            return fileName.substring(0, indexOfDot) + SUFFIX_THUMBNAIL + fileName.substring(indexOfDot);
        } else {
            return fileName + SUFFIX_THUMBNAIL;
        }
    }

    /**
     * 判断文件流是否是图片
     * @param file   file
     * @author wenxiaopeng
     * @date 2022/7/27 15:29
     * @return boolean
     **/
    public static boolean isImage(File file) {
        Tika tika = new Tika();
        // 检测内容类型
        String detectedType;
        try {
            detectedType = tika.detect(file);
        } catch (Exception e) {
            log.error("detect image error: {}", e.getMessage());
            return false;
        }
        // 判断是否为图片
        MediaType mediaType = MediaType.parse(detectedType);
        return mediaType.getType().equals("image");
    }

    /**
     * 旋转图片
     * @param originImage     待旋转图片
     * @param degree    旋转角度
     * @author wenxiaopeng
     * @date 2023/9/16 14:53
     * @return java.awt.image.BufferedImage 旋转后图片
     **/
    public static BufferedImage rotateImage(BufferedImage originImage, int degree){
        int width = originImage.getWidth(null);
        int height = originImage.getHeight(null);
        int type = originImage.getColorModel().getTransparency();
        Rectangle rectangle = calcRotatedSize(new Rectangle(new Dimension(width, height)), degree);
        BufferedImage bi = new BufferedImage(rectangle.width, rectangle.height, type);
        Graphics2D g2 = bi.createGraphics();
        g2.translate((rectangle.width - width) / 2d, (rectangle.height - height) / 2d);
        g2.rotate(Math.toRadians(degree), width / 2d, height / 2d);
        g2.drawImage(originImage, 0, 0, null);
        g2.dispose();
        return bi;
    }

    public static Rectangle calcRotatedSize(Rectangle src, int angel) {
        if (angel >= 90) {
            if(angel / 90 % 2 == 1) {
                int temp = src.height;
                src.height = src.width;
                src.width = temp;
            }
            angel = angel % 90;
        }
        double r = Math.sqrt(src.height * src.height + src.width * src.width) / 2;
        double len = 2 * Math.sin(Math.toRadians(angel) / 2) * r;
        double angelAlpha = (Math.PI - Math.toRadians(angel)) / 2;
        double angelDeltaWidth = Math.atan((double) src.height / src.width);
        double angelDeltaHeight = Math.atan((double) src.width / src.height);
        int lenDeltaWidth = (int) (len * Math.cos(Math.PI - angelAlpha - angelDeltaWidth));
        int lenDeltaHeight = (int) (len * Math.cos(Math.PI - angelAlpha - angelDeltaHeight));
        int desWidth = src.width + lenDeltaWidth * 2;
        int desHeight = src.height + lenDeltaHeight * 2;
        return new Rectangle(new Dimension(desWidth, desHeight));
    }
}

/**
 *  内部类，图片文件类型枚举类
 *
 * @author zhanglijun
 * @date 2020-04-22 10:47
 *
 * <pre>
 *              www.cloudscope.cn
 *      Copyright (c) 2019. All Rights Reserved.
 * </pre>
 */
@Getter
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

}

/**
 *  内部类，文件类型枚举类
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
