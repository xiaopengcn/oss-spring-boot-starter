package cn.cloudscope.oss.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Description: 文档上传接口返回值
 *
 * @author wenxiaopeng
 * @date 2021/07/13 16:49
 *
 * <pre>
 *              www.cloudscope.cn
 *      Copyright (c) 2019. All Rights Reserved.
 * </pre>
 */
@Data
@Builder
@AllArgsConstructor
public class DocumentUploadResult {

	/** 原始文件名 */
	private String name;
	/** 文件类型 */
	private String fileType;
	/** 文件md5 */
	private String md5;
	/** 文件大小（kb） */
	private Integer size;
	/** 文件路径 */
	private String path;
	/** 文件名后缀 */
	private String suffix;

	private String url;
}
