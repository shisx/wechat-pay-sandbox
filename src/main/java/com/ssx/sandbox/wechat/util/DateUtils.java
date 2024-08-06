package com.ssx.sandbox.wechat.util;

import lombok.Getter;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 日期 工具类
 *
 * <p>
 * <p>
 * 注意：
 * 1.请勿在并发情况下使用同一 java.text.SimpleDateFormat 实例，
 * 因为内部使用了成员变量设置时间，会影响其它线程的输出结果
 * <p>
 * <p>
 * 测试结果
 * > 串行，100万次，3轮 的平均耗时
 * java.time.format.DateTimeFormatter 2008 ms
 * java.text.SimpleDateFormat 2249 ms
 * org.apache.commons.lang3.time.FastDateFormat 2317 ms
 * <p>
 * > 并行下的格式化结果是否存在错误
 * java.time.format.DateTimeFormatter 不存在
 * java.text.SimpleDateFormat 存在
 *
 * @version 1.4
 */
public class DateUtils {

    public static final String FMT_yyyyMMddHHmmss14 = "yyyyMMddHHmmss";
    public static final String FMT_yyyyMMddHHmmss19 = "yyyy-MM-dd HH:mm:ss";
    public static final String FMT_yyyyMMddHHmmss19_b = "yyyy/MM/dd HH:mm:ss";
    public static final String FMT_yyyyMMdd10_b = "yyyy/MM/dd";
    public static final String FMT_yyyyMMdd8 = "yyyyMMdd";
    public static final String FMT_yyyyMMdd10 = "yyyy-MM-dd";
    public static final String FMT_HHmmss = "HHmmss";
    public static final String FMT_HHmmss8 = "HH:mm:ss";

    // region ------------------------------------------------------ 格式化

    public static String format(long milli, String pattern) {
        return getFormatter(pattern).format(toLocalDateTime(milli));
    }

    public static String format(Date time, String pattern) {
        return getFormatter(pattern).format(toLocalDateTime(time));
    }

    public static String format(TemporalAccessor time, String pattern) {
        return getFormatter(pattern).format(time);
    }
    // endregion ------------------------------------------------------ 格式化

    // region ------------------------------------------------------ 解析

    public static long parse(String time) {
        return toMilli(LocalDateTime.parse(time, FmtMatcher.matherFormatter(time)));
    }

    public static long parse(String time, String pattern) {
        return toMilli(LocalDateTime.parse(time, getFormatter(pattern)));
    }

    public static Date parse2Date(String time) {
        return toDate(LocalDateTime.parse(time, FmtMatcher.matherFormatter(time)));
    }

    public static Date parse2Date(String time, String pattern) {
        return toDate(LocalDateTime.parse(time, getFormatter(pattern)));
    }

    public static LocalDate parse2LocalDate(String time) {
        return LocalDate.parse(time, FmtMatcher.matherFormatter(time));
    }

    public static LocalDate parse2LocalDate(String time, String pattern) {
        return LocalDate.parse(time, getFormatter(pattern));
    }

    public static LocalTime parse2LocalTime(String time) {
        return LocalTime.parse(time, FmtMatcher.matherFormatter(time));
    }

    public static LocalTime parse2LocalTime(String time, String pattern) {
        return LocalTime.parse(time, getFormatter(pattern));
    }

    public static LocalDateTime parse2LocalDateTime(String time) {
        return LocalDateTime.parse(time, FmtMatcher.matherFormatter(time));
    }

    public static LocalDateTime parse2LocalDateTime(String time, String pattern) {
        return LocalDateTime.parse(time, getFormatter(pattern));
    }
    // endregion ------------------------------------------------------ 解析

    // region ------------------------------------------------------ 类型转换

    /**
     * Date 对象转 LocalDateTime
     *
     * @param date
     * @return
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    /**
     * 毫秒值转 LocalDateTime
     *
     * @param milli
     * @return
     */
    public static LocalDateTime toLocalDateTime(long milli) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(milli), ZoneId.systemDefault());
    }

    /**
     * Date 对象转 LocalDate
     *
     * @param date
     * @return
     */
    public static LocalDate toLocalDate(Date date) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        return localDateTime.toLocalDate();
    }

    /**
     * 时间戳 对象转 LocalDate
     *
     * @param milli
     * @return
     */
    public static LocalDate toLocalDate(long milli) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(milli), ZoneId.systemDefault());
        return localDateTime.toLocalDate();
    }

    /**
     * 毫秒值 转 Date
     *
     * @param milli
     * @return
     */
    public static Date toDate(long milli) {
        return new Date(milli);
    }

    /**
     * LocalDate 对象转 Date
     *
     * @param localDate
     * @return
     */
    public static Date toDate(LocalDate localDate) {
        return toDate(LocalDateTime.of(localDate, LocalTime.MIN));
    }

    /**
     * LocalDateTime 对象转 Date
     *
     * @param localDateTime
     * @return
     */
    public static Date toDate(LocalDateTime localDateTime) {
        return new Date(localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    /**
     * 转成秒
     *
     * @param localDateTime
     * @return
     */
    public static long toSecond(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    /**
     * 转成秒
     *
     * @param date
     * @return
     */
    public static long toSecond(Date date) {
        return date.getTime() / 1000L;
    }

    /**
     * 转成毫秒
     *
     * @param localDateTime
     * @return
     */
    public static long toMilli(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 转成毫秒
     *
     * @param date
     * @return
     */
    public static long toMilli(Date date) {
        return date.getTime();
    }

    public static Calendar toCalendar(long milli) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milli);
        return calendar;
    }

    public static Calendar toCalendar(Date date) {
        return toCalendar(date.getTime());
    }

    public static Calendar toCalendar(LocalDate localDate) {
        return toCalendar(toMilli(LocalDateTime.of(localDate, LocalTime.MIN)));
    }

    public static Calendar toCalendar(LocalDateTime localDateTime) {
        return toCalendar(toMilli(localDateTime));
    }
    // endregion ------------------------------------------------------ 类型转换

    // region ------------------------------------------------------ 计算

    /**
     * 计算两个时间点的持续时间
     *
     * @param a
     * @param b
     * @return
     */
    public static Duration duration(LocalDateTime a, LocalDateTime b) {
        return duration(toMilli(a), toMilli(b));
    }

    /**
     * 计算两个时间点的持续时间
     *
     * @param a
     * @param b
     * @return
     */
    public static Duration duration(Date a, Date b) {
        return duration(toMilli(a), toMilli(b));
    }

    /**
     * 计算两个时间点的持续时间
     *
     * @param a
     * @param b
     * @return
     */
    public static Duration duration(long a, long b) {
        return Duration.ofMillis(Math.abs(a - b));
    }

    /**
     * <pre>
     * 获取两个时间相差的年份
     *
     * 2017-12-01 - 2020-01-01 = 3
     * 2019-01-01 - 2019-12-01 = 0
     * </pre>
     *
     * @param c1
     * @param c2
     * @return
     */
    public static int yearDiff(Calendar c1, Calendar c2) {
        return Math.abs(c1.get(Calendar.YEAR) - c2.get(Calendar.YEAR));
    }

    public static int yearDiff(Date d1, Date d2) {
        return yearDiff(toCalendar(d1), toCalendar(d2));
    }

    public static int yearDiff(LocalDate d1, LocalDate d2) {
        return yearDiff(toCalendar(d1), toCalendar(d2));
    }

    public static int yearDiff(LocalDateTime d1, LocalDateTime d2) {
        return yearDiff(toCalendar(d1), toCalendar(d2));
    }

    public static int yearDiff(long mills1, long mills2) {
        return yearDiff(toCalendar(mills1), toCalendar(mills2));
    }

    /**
     * <pre>
     * 获取两个时间相差的月份
     *
     * 2017-12-01 - 2020-01-01 = 25
     * 2019-01-01 - 2019-11-01 = 10
     * </pre>
     */
    public static int monthDiff(Calendar c1, Calendar c2) {
        int yearDiff = yearDiff(c1, c2);
        if (c1.compareTo(c2) > 0) {
            Calendar c0 = c1;
            c1 = c2;
            c2 = c0;
        }
        int monthDiff;
        if (yearDiff > 0) {
            monthDiff = (yearDiff * 12) + (c2.get(Calendar.MONTH) - c1.get(Calendar.MONTH));
        } else {
            monthDiff = c2.get(Calendar.MONTH) - c1.get(Calendar.MONTH);
        }
        return monthDiff;
    }

    public static int monthDiff(Date d1, Date d2) {
        return monthDiff(toCalendar(d1), toCalendar(d2));
    }

    public static int monthDiff(LocalDate d1, LocalDate d2) {
        return monthDiff(toCalendar(d1), toCalendar(d2));
    }

    public static int monthDiff(LocalDateTime d1, LocalDateTime d2) {
        return monthDiff(toCalendar(d1), toCalendar(d2));
    }

    public static int monthDiff(long mills1, long mills2) {
        return monthDiff(toCalendar(mills1), toCalendar(mills2));
    }

    /**
     * <pre>
     * 获取两个时间相差的天数
     *
     * 2019-11-01 - 2019-11-03 = 2
     * 2019-12-27 - 2020-01-03 = 7
     * </pre>
     */
    public static int dayDiff(Calendar c1, Calendar c2) {
        return dayDiff(toLocalDate(c1.getTimeInMillis()), toLocalDate(c2.getTimeInMillis()));
    }

    public static int dayDiff(Date d1, Date d2) {
        return dayDiff(toLocalDate(d1), toLocalDate(d2));
    }

    public static int dayDiff(LocalDate d1, LocalDate d2) {
        return (int) Math.abs(d1.toEpochDay() - d2.toEpochDay());
    }

    public static int dayDiff(LocalDateTime d1, LocalDateTime d2) {
        return dayDiff(d1.toLocalDate(), d2.toLocalDate());
    }

    public static int dayDiff(long mills1, long mills2) {
        return dayDiff(toLocalDate(mills1), toLocalDate(mills2));
    }
    // endregion ------------------------------------------------------ 计算

    // region ------------------------------------------------------ 其它

    /**
     * 当前系统时间（秒）
     *
     * @return
     */
    public static long currentTimeSecond() {
        return System.currentTimeMillis() / 1000L;
    }

    /**
     * 跳转到月末
     *
     * @param localDate
     * @return
     */
    public static LocalDate toMonthEnd(LocalDate localDate) {
        return localDate.plusMonths(1).withDayOfMonth(1).minusDays(1);
    }

    /**
     * 跳转到月末
     *
     * @param localDate
     * @return
     */
    public static LocalDate toMonthStart(LocalDate localDate) {
        return localDate.withDayOfMonth(1);
    }

    /**
     * 根据表达式获取格式化实例
     *
     * @param pattern
     * @return
     */
    public static DateTimeFormatter getFormatter(String pattern) {
        return FORMATTER_CACHE.computeIfAbsent(pattern, DateTimeFormatter::ofPattern);
    }
    // endregion ------------------------------------------------------ 其它

    private static final Map<String, DateTimeFormatter> FORMATTER_CACHE = new ConcurrentHashMap<>();

    /**
     * 格式化匹配器
     */
    private static class FmtMatcher {
        private static final List<Format> FORMAT_LIST;

        static {
            FORMAT_LIST = new ArrayList<>();
            FORMAT_LIST.add(new Format("\\d{14}", FMT_yyyyMMddHHmmss14));
            FORMAT_LIST.add(new Format("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}", FMT_yyyyMMddHHmmss19));
            FORMAT_LIST.add(new Format("\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}", FMT_yyyyMMddHHmmss19_b));
            FORMAT_LIST.add(new Format("\\d{8}", FMT_yyyyMMdd8));
            FORMAT_LIST.add(new Format("\\d{4}-\\d{2}-\\d{2}", FMT_yyyyMMdd10));
            FORMAT_LIST.add(new Format("\\d{4}/\\d{2}/\\d{2}", FMT_yyyyMMdd10_b));
            FORMAT_LIST.add(new Format("\\d{6}", FMT_HHmmss));
            FORMAT_LIST.add(new Format("\\d{2}:\\d{2}:\\d{2}", FMT_HHmmss8));
        }

        private static DateTimeFormatter matherFormatter(String time) {
            for (Format format : FORMAT_LIST) {
                if (format.getPattern().matcher(time).matches()) {
                    return getFormatter(format.getStr());
                }
            }
            throw new RuntimeException("不支持的时间格式");
        }

        @Getter
        private static final class Format {
            private final Pattern pattern;
            private final String str;

            public Format(String regex, String str) {
                this.pattern = Pattern.compile(regex);
                this.str = str;
            }
        }
    }

}
