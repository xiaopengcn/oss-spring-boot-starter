package cn.cloudscope.bean;

/**
 * 上传文件后返回的结果
 * Created by wupanhua on 2018/9/11.
 */
public class YKUPResult {

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

    private YKUPResult() {
    }

    public YKUPResult(String phyPath, String fileName) {
        this.phyPath = phyPath;
        this.fileName = fileName;
    }

    public YKUPResult(String phyPath, String fileName, String thumbnail) {
        this.phyPath = phyPath;
        this.fileName = fileName;
        this.thumbnail = thumbnail;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getPhyPath() {
        return phyPath;
    }

    public void setPhyPath(String phyPath) {
        this.phyPath = phyPath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * 创建一个返回对象
     * @param pp
     * @param fn
     * @return
     */
    public static YKUPResult createResult(String pp, String fn) {
        return new YKUPResult(pp, fn);
    }

    /**
     * 创建图片返回类型
     * @param phyPath
     * @param thumbnail
     * @return
     */
    public static YKUPResult createThumbnailResult(String phyPath, String thumbnail) {
        YKUPResult ykupResult = new YKUPResult();
        ykupResult.setPhyPath(phyPath);
        ykupResult.setThumbnail(thumbnail);

        return ykupResult;
    }
}
