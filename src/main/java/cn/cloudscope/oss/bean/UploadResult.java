package cn.cloudscope.oss.bean;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * 上传文件后返回的结果
 * Created by wupanhua on 2018/9/11.
 * @author xiaopeng
 */
@Data
public class UploadResult {

    /**
     * 文件路径
     */
    private String phyPath;
    /**
     * 文件名称
     */
    private String fileName;
    /**
     * 缩略图地址，如果当前文件为图片则有缩略图
     */
    private String thumbnail;

    public UploadResult() {
    }

    public UploadResult(String phyPath, String fileName) {
        this.phyPath = phyPath;
        this.fileName = fileName;
    }

    /**
     * 创建一个返回对象
     * @param path  文件路径
     * @param name  文件名
     * @return 上传返回值
     */
    public static UploadResult createResult(String path, String name) {
        return new UploadResult(path, name);
    }

    /**
     * 创建图片返回类型
     * @param phyPath   原文件路径
     * @param thumbnail 缩略图路径
     * @return  含缩略图路径的返回结果
     */
    public static UploadResult createThumbnailResult(String phyPath, String thumbnail) {
        UploadResult uploadResult = new UploadResult();
        uploadResult.setPhyPath(phyPath);
        uploadResult.setThumbnail(thumbnail);

        return uploadResult;
    }

    public static boolean ok(UploadResult result) {
        return null != result && StringUtils.isNotBlank(result.phyPath);
    }

    public static final UploadResult EMPTY = new UploadResult();
}
