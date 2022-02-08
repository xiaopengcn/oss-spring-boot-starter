package cn.cloudscope.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Description: 上传结果
 *
 * @author wangkp
 * @date 2022/1/25 11:07
 *
 * <pre>
 *              www.cloudscope.cn
 *      Copyright (c) 2019. All Rights Reserved.
 * </pre>
 */
@Data
@AllArgsConstructor
@Builder
public class DocumentUrlResult {

    /** 源文件访问路径 */
    private String url;
    /** 若是图片，缩略图访问路径 */
    private String thumbnail;
    /** 以秒计的过期时间 */
    private Integer expiresIn;
}
