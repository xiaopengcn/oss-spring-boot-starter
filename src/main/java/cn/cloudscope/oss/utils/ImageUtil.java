package cn.cloudscope.oss.utils;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

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
@Slf4j
public class ImageUtil {

    /**
     * Description:
     * <判别文件是否为图片>
     * @author wupanhua
     * @date 11:14 2020-03-03
     * @param srcFilePath 文件对象
     */
    public static boolean isImage(File srcFilePath) {
        String signature = FileUtil.getFileSignature(srcFilePath);
        return isImageSignature(signature);
    }

    private static boolean isImageSignature(String signature) {
        if(null != signature) {
            ImageType[] imageTypes = ImageType.values();
            for (ImageType imageType : imageTypes) {
                if (signature.startsWith(imageType.getValue())) {
                    return true;
                }
            }
        }
        return false;
    }

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
            } catch (IOException e) {
                e.printStackTrace();
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
     * @param inputStream 原图片流
     * @author wenxiaopeng
     * @date 2021/10/09 17:29
     * @return java.io.InputStream
     **/
    public static InputStream buildThumbnail(InputStream inputStream, String suffix) {

        if(!inputStream.markSupported()) {
            inputStream = new BufferedInputStream(inputStream);
        }
        try(ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            inputStream.mark(inputStream.available() + 1);
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            inputStream.reset();
//            Double rotate = getRotate((BufferedInputStream) inputStream);
            // 将图片进行缩小处理, 并对文件加入后缀名"-thumbnail"
            Thumbnails.of(bufferedImage)
                    .scale(Math.min(1, 20 * 1024F / inputStream.available()))
//                    .rotate(rotate)
                    .outputFormat(suffix)
                    .toOutputStream(os);
            return new ByteArrayInputStream(os.toByteArray());
        } catch (Exception e) {
            log.error("创建缩略图异常: {}", e.getMessage());
        } finally {
            IOUtils.closeQuietly(inputStream);
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

    /**
     * 判断文件流是否是图片
     * @param bis   bis
     * @author wenxiaopeng
     * @date 2022/7/27 15:29
     * @return boolean
     **/
    public static boolean isImage(InputStream bis) {
        return isImageSignature(FileUtil.getFileSignature(bis));
    }

    /**
     * 将图片旋转指定度
     * @param bufferedimage   原图
     * @param degree     角度
     * @author wenxiaopeng
     * @date 2022/8/2 13:31
     **/
//    public static BufferedImage rotateImage(BufferedImage bufferedimage, int degree){
//        // 得到图片尺寸
//        int w = bufferedimage.getWidth();
//        int h = bufferedimage.getHeight();
//        log.info("原图width:{}, height:{}", w, h);
//        // 得到图片透明度。
//        int type= bufferedimage.getColorModel().getTransparency();
//        BufferedImage img = new BufferedImage(h, w, type);
//        Graphics2D graphics2d = img.createGraphics();
//        graphics2d.setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
//        // 旋转，degree是整型，度数，比如垂直90度。
//        graphics2d.rotate(Math.toRadians(degree), w / 2, h / 2);
//        // 从bufferedimage copy图片至img，0,0是img的坐标。
//        graphics2d.drawImage(bufferedimage, 0, 0, null);
//        graphics2d.dispose();
//        // 返回复制好的图片，原图片依然没有变，没有旋转，下次还可以使用。
//        log.info("旋转后width:{}, height:{}", img.getWidth(), img.getHeight());
//        return img;
//    }

    public static BufferedImage rotateImage(BufferedImage bufferedimage, int degree){
        int src_width = bufferedimage.getWidth(null);
        int src_height = bufferedimage.getHeight(null);
        int type = bufferedimage.getColorModel().getTransparency();
        Rectangle rect_des = calcRotatedSize(new Rectangle(new Dimension(src_width, src_height)), degree);
        BufferedImage bi = new BufferedImage(rect_des.width, rect_des.height, type);
        Graphics2D g2 = bi.createGraphics();
        g2.translate((rect_des.width - src_width) / 2, (rect_des.height - src_height) / 2);
        g2.rotate(Math.toRadians(degree), src_width / 2, src_height / 2);
        g2.drawImage(bufferedimage, 0, 0, null);
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
        double angel_alpha = (Math.PI - Math.toRadians(angel)) / 2;
        double angel_dalta_width = Math.atan((double) src.height / src.width);
        double angel_dalta_height = Math.atan((double) src.width / src.height);
        int len_dalta_width = (int) (len * Math.cos(Math.PI - angel_alpha - angel_dalta_width));
        int len_dalta_height = (int) (len * Math.cos(Math.PI - angel_alpha - angel_dalta_height));
        int des_width = src.width + len_dalta_width * 2;
        int des_height = src.height + len_dalta_height * 2;
        return new Rectangle(new Dimension(des_width, des_height));
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
