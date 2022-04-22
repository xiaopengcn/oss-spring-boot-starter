package cn.cloudscope.oss.utils;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Description: DateUtil
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
public class DateUtil {

    private static final ThreadLocal<Map<String, SimpleDateFormat>> safeFormat = new ThreadLocal<>();
    private static final ThreadLocal<Calendar> safeCalendar = ThreadLocal.withInitial(Calendar::getInstance);

    /**
     * Description:
     * <从ThreadLocal中获取一个SimpleDateFormat,线程安全的，一定要在执行一次safeFormat.remove()方法>
     * @author wupanhua
     * @date 15:27 2019/8/8
     * @param dateStyle 1
     * @return java.text.SimpleDateFormat
     **/
    private static SimpleDateFormat getformatTools(DateStyle dateStyle) {
        try{
            String style = dateStyle.getStyle();
            Map<String, SimpleDateFormat> tool = safeFormat.get();

            if (tool == null) {
                tool = Maps.newHashMap();
            }

            SimpleDateFormat sdf = tool.get(style);
            if (sdf == null) {
                sdf = new SimpleDateFormat(style, Locale.getDefault());
                tool.put(style, sdf);
                safeFormat.set(tool);
            }

            return sdf;
        }catch (Exception ex){
            log.error("getformatTools exception", ex);
            throw ex;
        }finally{
            safeFormat.remove();
        }
    }

    /**
     * Description:
     * <将日期格式化成指定的格式>
     * @author wupanhua
     * @date 15:28 2019/8/8
     * @param date 1
     * @param dateStyle 2
     * @return java.lang.String
     **/
    public static String format(Date date, DateStyle dateStyle) {

        if (Objects.isNull(date)) {
            return null;
        }

        return getformatTools(dateStyle).format(date);
    }

    /**
     * Description:
     * <日期字符串格式化为Date类型>
     * @author wupanhua
     * @date 15:28 2019/8/8
     * @param dateStr 1
     * @param dateStyle 2
     * @return java.util.Date
     **/
    public static Date parse(String dateStr, DateStyle dateStyle) {

        if (StringUtils.isEmpty(dateStr)) {
            return null;
        }

        Date date = null;
        try {
            date = getformatTools(dateStyle).parse(dateStr);
        } catch (ParseException e) {
            log.error("格式化日期失败 error", e);
        }
        return date;
    }

    /**
     * Description:
     * <获取指定日期的年份>
     * @author wupanhua
     * @date 15:28 2019/8/8
     * @param date 1
     * @return int
     **/
    public static int getYear(Date date) {

        try {
            if(null!=date) {
                safeCalendar.get().setTime(date);
                return safeCalendar.get().get(Calendar.YEAR);
            }
        } finally {
            safeCalendar.remove();
        }
        return 0;
    }

    /**
     * Description:
     * <获取月份>
     * @author wupanhua
     * @date 15:29 2019/8/8
     * @param date 1
     * @return int
     **/
    public static int getMounth(Date date) {

        try {
            if(null!=date) {
                safeCalendar.get().setTime(date);
                return safeCalendar.get().get(Calendar.MONTH) + 1;
            }
        } finally {
            safeCalendar.remove();
        }
        return 0;
    }

    /**
     * Description:
     * <获取两日期间的间隔天数，不包含周六日，相同日期算1天>
     * @param start 1
     * @param end 2
     * @author wenxiaopeng
     * @date 15:47 2021/05/06
     * @return java.lang.Integer
     **/
    public static Integer getDayDuration(Date start, Date end) {
        Integer duration = null;
        // TODO 自定义节假日
        if(null!= end && null!= start) {
            duration = 1;
            Calendar startCalendar = DateUtils.toCalendar(start);
            Calendar endCalendar = DateUtils.toCalendar(end);
            while (startCalendar.compareTo(endCalendar) < 0) {
                int weekDay = startCalendar.get(Calendar.DAY_OF_WEEK);
                if (weekDay < Calendar.SATURDAY && weekDay > Calendar.SUNDAY){
                    duration++;
                }
                startCalendar.add(Calendar.DAY_OF_YEAR, 1);
            }
        }

        return duration;
    }

    /**
     * Description:
     * <日期风格>
     * @author wupanhua
     * @date 15:29 2019/8/8
     **/
    public enum DateStyle {
        /**
         * 年/月/日/时/分/秒
         */
        ALL("yyyy-MM-dd HH:mm:ss"),
        /**
         * 年/月/日
         */
        YEAE_MONTH_DAY("yyyy-MM-dd"),
        /**
         * 时/分/秒
         */
        HH_DOT_MM_DOT_SS("HH:mm:ss"),
        /**
         * 新格式/年/月/日/时/分/秒
         */
        NEW_DATE("_yyyy_MM_dd_HH_mm_ss"),
        /**
         *
         */
        YYYYMMDDHH_mm("yyyyMMddHH_mm"),
        /**
         * 年/月/日
         */
        YEARMONTHDAY("yyyyMMdd"),

        /**
         * 年/月/日
         */
        YYYYMMDDHHMM("yyyyMMddHHmm");

        private String style;

        DateStyle() {
        }

        DateStyle(String style) {
            this.style = style;
        }

        public String getStyle() {
            return style;
        }
    }

    /**
     * Description:
     * <计算时间差>
     * @author yangliu
     * @date 15:31 2019/8/8
     * @param starDate 1
     * @param endDate 2
     * @return java.lang.String
     **/
    public static String getDatePoor(Date starDate, Date endDate) {

        long nd = 1000 * 24 * 60 * 60L;
        long nh = 1000 * 60 * 60L;
        long nm = 1000 * 60L;
        long ns = 1000L;
        // 获得两个时间的毫秒时间差异
        long diff = endDate.getTime() - starDate.getTime();
        // 计算差多少天
        long day = diff / nd;
        // 计算差多少小时
        long hour = diff % nd / nh;
        // 计算差多少分钟
        long min = diff % nd % nh / nm;
        // 计算差多少秒//输出结果
        long sec = diff % nd % nh % nm / ns;

        return day + "天" + hour + "小时" + min + "分钟" + sec + "秒";
    }

}
