/**  
 * @Title: PathUtil.java
 * @Package: com.cloudscope.shield.tools
 * @Author: 吴盼华
 * @Date: 2017年8月17日
 * @Time: 下午5:30:26
 * @Copyright: 云科凯创@2017
*/

package cn.cloudscope.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * Description: 生成散列路径
 *
 * @author wupanhua
 * @date 2019/8/6 15:28
 *
 * <pre>
 *              www.cloudscope.cn
 *      Copyright (c) 2019. All Rights Reserved.
 * </pre>
 */
@Slf4j
public class PathUtil {

	private PathUtil(){
		// make constructor private
	}

	/**
	 * Description:
	 * <无法补充>
	 * @author wupanhua
	 * @date 15:39 2019/8/8
	 * @param upload 1
     * @param fileName 2
	 * @return java.lang.String
	 **/
	public static String generatePath(String upload, String fileName) {
		
		// 获得文件名的hash值
		int hashCode = fileName.hashCode();
		// 按位与获得一级目录
		int dir1 = hashCode & 0xF;
		// 移动四位按位与
		int dir2 = (hashCode >> 4) & 0xF;
		// 创建文件目录
		upload = upload + dir1 + "/" + dir2;
		File path = new File(upload);
		if (!path.exists() && path.mkdirs()) {
			log.debug("folder created: [{}]", upload);
		}
		
		return dir1 + "/" + dir2;
	}

	/**
	 * Description:
	 * <无法补充>
	 * @author wupanhua
	 * @date 15:40 2019/8/8
	 * @param fileName 1
	 * @return java.lang.String
	 **/
	public static String generatePath(String fileName) {

		// 获得文件名的hash值
		int hashCode = fileName.hashCode();
		// 按位与获得一级目录
		int dir1 = hashCode & 0xF;
		// 移动四位按位与
		int dir2 = (hashCode >> 4) & 0xF;

		return dir1 + "/" + dir2;
	}

}
