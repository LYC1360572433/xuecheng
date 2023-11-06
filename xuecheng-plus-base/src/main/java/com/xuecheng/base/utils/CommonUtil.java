package com.xuecheng.base.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <P>
 * 通用工具类
 * </p>
 *
 */
public class CommonUtil {
	/**
	 * 将隐藏手机号码
	 * @param mobile
	 * @return
	 */
	public static String hiddenMobile(String mobile) {
		if (StringUtils.isBlank(mobile)) {
			return "";
		}
		return mobile.replaceAll("(\\d{3})\\d{4}(\\d{4})","$1****$2");
	}


	/**
	 * BigDecimal用来对超过16位有效位的数进行精确的运算
	 * @param s
	 * @param isFen
	 * @return
	 */
	public static BigDecimal strToBigDecimal(String s, Boolean isFen) {
		if (StringUtils.isBlank(s)) {
			return null;
		}
		if (!NumberUtils.isNumber(s)) {
			return null;
		}
		BigDecimal bd = new BigDecimal(s);
		if (isFen != null && isFen.booleanValue()){
			// 第一个参数是 除数；第二个参数舍入模式
			bd = bd.divide(new BigDecimal(100), 2);
		}
		return bd;
	}

	// 正则表达式对象 匹配时间格式 如：1:30或1:30:45
	public static final Pattern shiFenMiaoPattern = Pattern.compile("(\\d{1,2}[：:]){0,2}\\d{1,2}");

	/**
	 * 时分秒转换为 秒数
	 * @param shifenmiao
	 * @return
	 */
	public static Long shiFenMiaoToSeconds (String shifenmiao) {
		if (StringUtils.isBlank(shifenmiao)) {
			return 0L;
		}
		Long totalSeconds = 0L;
		// 将字符串中所有的空格字符替换为空字符串（即删除所有的空格)
		shifenmiao = shifenmiao.replaceAll(" ", "");
		boolean matched = shiFenMiaoPattern.matcher(shifenmiao).matches();
		if (matched) {
			List<String> sfmList = new ArrayList<>();
			// 用于分隔字符串
			StringTokenizer st = new StringTokenizer(shifenmiao,":：");
			// 当没有分隔符时，循环终止
			while (st.hasMoreTokens()) {
				// 返回从当前位置到下一个分隔符的字符串
				sfmList.add(st.nextToken());
			}
			// 逆序
			Collections.reverse(sfmList);
			// 将列表List中的元素转导出为数组（指定类型数组）
			String[] sfmArr = sfmList.toArray(new String[0]);
			for (int i = 0; i < sfmArr.length; i++) {
				if (i == 0) {
					totalSeconds += Long.valueOf(sfmArr[i]);
				} else if (i == 1) {
					totalSeconds += Long.valueOf(sfmArr[i]) * 60;
				} else if (i == 2) {
					totalSeconds += Long.valueOf(sfmArr[i]) * 3600;
				}
			}
		}
		return totalSeconds;
	}

	/**
	 * 将下划线映射到骆驼命名使用的正则表达式, 预编译正则用于提高效率
	 */
	private static Pattern patternForUTC = Pattern.compile("_([a-z]){1}");

	/**
	 * 将下划线映射到骆驼命名
	 *
	 * @param str
	 * @return
	 */
	public static String mapUnderscoreToCamelCase(String str) {
		// 先转成全小写
		str = str.toLowerCase();
		final Matcher matcher = patternForUTC.matcher(str);
		while (matcher.find()) {
			str = str.replaceAll(matcher.group(), matcher.group(1).toUpperCase());
		}
		return str;
	}

	/**
	 * 将骆驼命名映射到下划线, 必须是标准的驼峰命名, 否则会出现奇怪的结果
	 *
	 * @param str
	 * @return
	 */
	public static String mapCamelCaseToUnderscore(String str) {
		return str.replaceAll("([A-Z]){1}","_$1").toUpperCase();
	}

	public static void main(String[] args) {
		Instant start = Instant.parse("2017-10-03T10:15:30.00Z");
		Instant end = Instant.parse("2018-10-04T10:16:30.00Z");

		LocalDateTime time1 = LocalDateTime.of(2019,8,8,8,8);
		LocalDateTime time2 = LocalDateTime.of(2019,9,9,8,8);
		Duration duration = Duration.between(time1, time2);
		System.out.println(duration);
		System.out.println(duration.getSeconds());
		System.out.println(duration.isNegative());
		System.out.println(time1.until(time2, ChronoUnit.SECONDS));
	}
}

