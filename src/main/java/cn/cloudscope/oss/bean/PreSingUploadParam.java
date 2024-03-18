package cn.cloudscope.oss.bean;

import lombok.Data;

import java.time.Duration;

/**
 * Description: 预上传文件参数
 *
 * @author wenxiaopeng
 * @date 2024/3/18 13:17
 *
 * <pre>
 *              www.cloudscope.cn
 *      Copyright (c) 2021. All Rights Reserved.
 * </pre>
 */
@Data
public class PreSingUploadParam {

    /** 文件名 */
    private String filename;
    /** 文件类型 */
    private String contentType;
    /** 文件大小 */
    private Integer size;
    /** 上传有效期 */
    private Duration expiresIn;
    /** 是否公开文件 */
    private boolean isPublic;
}
