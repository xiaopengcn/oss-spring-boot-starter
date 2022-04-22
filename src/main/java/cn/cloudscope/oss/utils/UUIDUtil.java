package cn.cloudscope.oss.utils;

import java.util.UUID;

/**
 * Description: 无法补充
 *
 * @author wupanhua
 * @date 2019/8/6 15:28
 *
 * <pre>
 *              www.cloudscope.cn
 *      Copyright (c) 2019. All Rights Reserved.
 * </pre>
 */
public class UUIDUtil {

    private UUIDUtil() {
        // make constructor private
    }

    public static String buildUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Description:
     * <返回无连接符号的uuid>
     * @author wupanhua
     * @date 15:57 2019/8/8
     * @return java.lang.String
     **/
    public static String get32UUID() {
        return UUID.randomUUID().toString().trim().replace("-", "");
    }
}
