package cn.cloudscope.oss.utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Description: 视频工具
 *
 * @author wenxiaopeng
 * @date 2022/7/26 17:48
 *
 * <pre>
 *              www.cloudscope.cn
 *      Copyright (c) 2021. All Rights Reserved.
 * </pre>
 */
@Slf4j
public class VideoUtil {

    public static final Double STAND_FRAME_RATE = 56d;

    /**
     * <a href="https://filesignatures.net/index.php?page=all">视频类文件签名列表</a>
     * @date 2022/7/26 17:49
     **/
    private static final String[] SIGNATURE_VIDEO = new String[]{
            // 3gp
            "0000001466747970", "0000002066747970", "0000001866747970",
            // 4xm
            "52494646",
            // mov
            "6D6F6F76", "66726565", "6D646174", "77696465", "706E6F74", "736B6970",
            // rmvb
            "2E524D46",
            // wmv
            "3026B2758E66CF11",
    };

    /**
     * 是否是视频文件
     * @param fileStream    文件流
     * @author wenxiaopeng
     * @date 2022/7/26 18:29
     * @return boolean
     **/
    public static boolean isVideo(InputStream fileStream) {
        String fileSignature = FileUtil.getFileSignature(fileStream);
        if(null != fileSignature) {
            for (String signature : SIGNATURE_VIDEO) {
                if(fileSignature.startsWith(signature)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 是否是视频文件
     * @param file  文件
     * @author wenxiaopeng
     * @date 2022/7/26 18:29
     * @return boolean
     **/
    public static boolean isVideo(File file) {
        try (InputStream fis = new FileInputStream(file)) {
            return isVideo(fis);
        } catch (IOException e) {
            log.error("无法确定传入文件是否为视频文件：{}", e.getMessage());
        }
        return false;
    }

    /**
     * 视频截帧
     * @param video   视频文件
     * @param frameNo   帧数
     * @author wenxiaopeng
     * @date 2022/7/26 18:39
     * @return java.io.InputStream
     **/
    public static InputStream captureFrame(File video, int frameNo) {

        try(ByteArrayOutputStream os = new ByteArrayOutputStream();
            FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(video)) {

            frameGrabber.start();
            frameGrabber.setFrameNumber(frameNo);
            Frame frame = frameGrabber.grabImage();
            BufferedImage bufferedImage = frameToBufferedImage(frame);
            //视频旋转角度，可能是null
            String rotate = frameGrabber.getVideoMetadata("rotate");
            if(StringUtils.isNotBlank(rotate)){
                int rotateNum = Integer.parseInt(rotate);
                bufferedImage = ImageUtil.rotateImage(bufferedImage, rotateNum);
            }
            ImageIO.write(bufferedImage, "jpg", os);
            return new ByteArrayInputStream(os.toByteArray());
        } catch (Exception e) {
            log.error("无法截取视频帧：{}", e.getMessage());
        }
        return null;
    }

    /**
     * 帧转BufferedImage
     * @param frame     视频帧
     * @author wenxiaopeng
     * @date 2022/7/26 18:39
     * @return java.awt.image.BufferedImage
     **/
    public static BufferedImage frameToBufferedImage(Frame frame) {
        //创建BufferedImage对象
        Java2DFrameConverter converter = new Java2DFrameConverter();
        return converter.getBufferedImage(frame);
    }

    /**
     * 将视频文件帧处理并以“jpg”格式进行存储。
     * 依赖FrameToBufferedImage方法：将frame转换为bufferedImage对象
     * @param video     原视频
     * @param imagesFolder  图片文件夹
     * @param deleteVideo   处理完后是否删除video
     */
    public static VideoInfo grabberVideoFramer(File video, File imagesFolder, boolean deleteVideo){

        //最后获取到的视频的图片的路径
        try(FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(video)) {
            imagesFolder.mkdir();
            frameGrabber.start();
            Double frameRate = frameGrabber.getFrameRate();
            VideoInfo videoInfo = new VideoInfo();
            // 若帧数小于56帧，每帧取一张，否则每两帧取一张
//            int step = (int) (frameRate / (STAND_FRAME_RATE / 2));
            videoInfo.setHeight(frameGrabber.getImageHeight());
            videoInfo.setWidth(frameGrabber.getImageWidth());
            videoInfo.setFrameRateOrigin(frameRate);
            // 视频时长 微秒转秒
            videoInfo.setDuration((double) (frameGrabber.getLengthInTime() / 1000000));
            videoInfo.setRotate(frameGrabber.getVideoMetadata("rotate"));
            //获取视频总帧数
            int ftp = frameGrabber.getLengthInFrames();
            log.info("视频基本信息: {}", videoInfo);
            int totalFrame = 0;
            int rotate = 0;
            // 从第二秒开始
            try {
                rotate = Integer.parseInt(videoInfo.getRotate());
            } catch (Exception ignored) {
            }
            for(int i=0; i<ftp; i+=1) {
                //滚动获取 滚动的时候时间戳一直是变得
                Frame frame = frameGrabber.grabImage();
                if (frame != null) {
                    totalFrame++;
                    //文件绝对路径+名字
                    File image = new File(imagesFolder, String.format("%08d.jpg", i));
                    BufferedImage bufferedImage = frameToBufferedImage(frame);
                    // 抽帧完图片信息意思丢失，需要转到合适角度
                    if(rotate > 0) {
                        bufferedImage = ImageUtil.rotateImage(bufferedImage, rotate);
                    }
                    ImageIO.write(bufferedImage, "jpg", image);
                }
            }
//            videoInfo.setFrameRateAdjust(totalFrame / videoInfo.getDuration());
            return videoInfo;
        } catch (Exception e) {
            log.error("视频抽帧异常：{}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        } finally {
            if(deleteVideo) {
                try {
                    FileUtils.forceDelete(video);
                } catch (IOException e) {
                    log.error("删除视频异常：{}", e.getMessage());
                }
            }
        }
    }

    public static boolean isVideo(String key) {
        return key.endsWith("mp4") || key.endsWith("mov");
    }

    /**
     * 视频信息
     * @author wenxiaopeng
     * @date 2022/8/9 20:14
     **/
    @Data
    public static class VideoInfo {

        private Integer height;

        private Integer width;

        /** 帧率 */
        private Double frameRateOrigin;

        /** 新帧率 */
//        private Double frameRateAdjust;

        /** 时长 s */
        private Double duration;
        /** 角度，AI平台支持90°视频传入 */
        private String rotate;
    }

}
