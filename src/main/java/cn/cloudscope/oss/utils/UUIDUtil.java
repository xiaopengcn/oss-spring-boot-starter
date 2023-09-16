package cn.cloudscope.oss.utils;

import java.util.UUID;

/**
 *  无法补充
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

}
