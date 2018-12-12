package com.example.demo.util;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * UtilString
 * 
 * @author wuchenl
 */

public class UtilString extends org.apache.commons.lang3.StringUtils {


	//编译后的正则表达式缓存
	private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();
	private static final char SEPARATOR = '_';
	private static final String CHARSET_NAME = "UTF-8";



	public static String defaultIfNull(String object, String defaultValue) {
		return object == null ? defaultValue : object;
	}


	/**
	 * 编译一个正则表达式，并且进行缓存,如果换成已存在则使用缓存
	 *
	 * @param regex 表达式
	 * @return 编译后的Pattern
	 */
	public static final Pattern compileRegex(String regex) {
		Pattern pattern = PATTERN_CACHE.get(regex);
		if (pattern == null) {
			pattern = Pattern.compile(regex);
			PATTERN_CACHE.put(regex, pattern);
		}
		return pattern;
	}

	/**
	 * 对象是否为无效值
	 *
	 * @param obj 要判断的对象
	 * @return 是否为有效值（不为null 和 "" 字符串）
	 */
	public static boolean isEmptyObject(Object obj) {
		return obj == null || "".equals(obj.toString());
	}






	/**
	 * 对象是否为true
	 *
	 * @param obj
	 * @return
	 */
	public static boolean isTrue(Object obj) {
		return "true".equals(String.valueOf(obj));
	}


	/**
	 * 参数是否是有效数字 （整数或者小数）
	 *
	 * @param obj 参数（对象将被调用string()转为字符串类型）
	 * @return 是否是数字
	 */
	public static boolean isNumber(Object obj) {
		if (obj instanceof Number) {
			return true;
		}
		return isInt(obj) || isDouble(obj);
	}

	/**
	 * 参数是否是有效整数
	 *
	 * @param obj 参数（对象将被调用string()转为字符串类型）
	 * @return 是否是整数
	 */
	public static boolean isInt(Object obj) {
		if (isEmptyObject(obj)) {
			return false;
		}
		if (obj instanceof Integer) {
			return true;
		}
		return obj.toString().matches("[-+]?\\d+");
	}

	/**
	 * 字符串参数是否是double
	 *
	 * @param obj 参数（对象将被调用string()转为字符串类型）
	 * @return 是否是double
	 */
	public static boolean isDouble(Object obj) {
		if (isEmptyObject(obj)) {
			return false;
		}
		if (obj instanceof Double || obj instanceof Float) {
			return true;
		}
		return compileRegex("[-+]?\\d+\\.\\d+").matcher(obj.toString()).matches();
	}

	/**
	 * 判断一个对象是否为boolean类型,包括字符串中的true和false
	 *
	 * @param obj 要判断的对象
	 * @return 是否是一个boolean类型
	 */
	public static boolean isBoolean(Object obj) {
		if (obj instanceof Boolean) {
			return true;
		}
		String strVal = String.valueOf(obj);
		return "true".equalsIgnoreCase(strVal) || "false".equalsIgnoreCase(strVal);
	}

	/**
	 * 将对象转为int值,如果对象无法进行转换,则使用默认值
	 *
	 * @param object       要转换的对象
	 * @param defaultValue 默认值
	 * @return 转换后的值
	 */
	public static int toInt(Object object, int defaultValue) {
		if (object instanceof Number) {
			return ((Number) object).intValue();
		}
		if (isInt(object)) {
			return Integer.parseInt(object.toString());
		}
		if (isDouble(object)) {
			return (int) Double.parseDouble(object.toString());
		}
		return defaultValue;
	}

	/**
	 * 将对象转为int值,如果对象不能转为,将返回0
	 *
	 * @param object 要转换的对象
	 * @return 转换后的值
	 */
	public static int toInt(Object object) {
		return toInt(object, 0);
	}

	/**
	 * 将对象转为long类型,如果对象无法转换,将返回默认值
	 *
	 * @param object       要转换的对象
	 * @param defaultValue 默认值
	 * @return 转换后的值
	 */
	public static long toLong(Object object, long defaultValue) {
		if (object instanceof Number) {
			return ((Number) object).longValue();
		}
		if (isInt(object)) {
			return Long.parseLong(object.toString());
		}
		if (isDouble(object)) {
			return (long) Double.parseDouble(object.toString());
		}
		return defaultValue;
	}

	/**
	 * 将对象转为 long值,如果无法转换,则转为0
	 *
	 * @param object 要转换的对象
	 * @return 转换后的值
	 */
	public static long toLong(Object object) {
		return toLong(object, 0);
	}

	/**
	 * 将对象转为Double,如果对象无法转换,将使用默认值
	 *
	 * @param object       要转换的对象
	 * @param defaultValue 默认值
	 * @return 转换后的值
	 */
	public static double toDouble(Object object, double defaultValue) {
		if (object instanceof Number) {
			return ((Number) object).doubleValue();
		}
		if (isNumber(object)) {
			return Double.parseDouble(object.toString());
		}
		if (null == object) {
			return defaultValue;
		}
		return 0;
	}

	/**
	 * 将对象转为Double,如果对象无法转换,将使用默认值0
	 *
	 * @param object 要转换的对象
	 * @return 转换后的值
	 */
	public static double toDouble(Object object) {
		return toDouble(object, 0);
	}








	/**
	 * 字符串连接，将参数列表拼接为一个字符串
	 *
	 * @return 返回拼接后的字符串
	 */
	public static String concatWithMarkAndSplit(String mark , String split , Collection more) {
		StringBuilder buf = new StringBuilder();
		if(more==null || more.isEmpty()){
			return buf.toString();
		}
		int i = 0;
		for (Object obj : more) {
			if (i != 0) {
				buf.append(mark).append(obj).append(mark).append(split);
			}
			buf.append(mark).append(obj).append(mark);
			i++;
		}
		return buf.toString();
	}

	public static String concat(Collection more) {
		return concatWithSpilt("", more);
	}

	public static String concatWithSpilt(String split , Collection more) {
		StringBuilder buf = new StringBuilder();
		int i = 0;
		for (Object obj : more) {
			if (i != 0) {
				buf.append(split);
			}
			buf.append(obj);
			i++;
		}
		return buf.toString();
	}

	public static String concat(Object... more) {
		return concatWithSpilt("", more);
	}

	public static String concatWithSpilt(String split , Object... more) {
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < more.length; i++) {
			if (i != 0) {
				buf.append(split);
			}
			buf.append(more[i]);
		}
		return buf.toString();
	}







	/**
	 * 驼峰命名法工具
	 * @return
	 * 		toCamelCase("hello_world") == "helloWorld"
	 * 		toCapitalizeCamelCase("hello_world") == "HelloWorld"
	 * 		toUnderScoreCase("helloWorld") = "hello_world"
	 */
	public static String toCamelCase(String s) {
		if (s == null) {
			return null;
		}

		s = s.toLowerCase();

		StringBuilder sb = new StringBuilder(s.length());
		boolean upperCase = false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);

			if (c == SEPARATOR) {
				upperCase = true;
			} else if (upperCase) {
				sb.append(Character.toUpperCase(c));
				upperCase = false;
			} else {
				sb.append(c);
			}
		}

		return sb.toString();
	}

	/**
	 * 驼峰命名法工具
	 * @return
	 * 		toCamelCase("hello_world") == "helloWorld"
	 * 		toCapitalizeCamelCase("hello_world") == "HelloWorld"
	 * 		toUnderScoreCase("helloWorld") = "hello_world"
	 */
	public static String toCapitalizeCamelCase(String s) {
		if (s == null) {
			return null;
		}
		s = toCamelCase(s);
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	/**
	 * 驼峰命名法工具
	 * @return
	 * 		toCamelCase("hello_world") == "helloWorld"
	 * 		toCapitalizeCamelCase("hello_world") == "HelloWorld"
	 * 		toUnderScoreCase("helloWorld") = "hello_world"
	 */
	public static String toUnderScoreCase(String s) {
		if (s == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		boolean upperCase = false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);

			boolean nextUpperCase = true;

			if (i < (s.length() - 1)) {
				nextUpperCase = Character.isUpperCase(s.charAt(i + 1));
			}

			if ((i > 0) && Character.isUpperCase(c)) {
				if (!upperCase || !nextUpperCase) {
					sb.append(SEPARATOR);
				}
				upperCase = true;
			} else {
				upperCase = false;
			}

			sb.append(Character.toLowerCase(c));
		}

		return sb.toString();
	}








	/**
	 * 缩略字符串（不区分中英文字符）
	 * @param str 目标字符串
	 * @param length 截取长度
	 * @return
	 */
	public static String abbr(String str, int length) {
		if (str == null) {
			return "";
		}
		try {
			StringBuilder sb = new StringBuilder();
			int currentLength = 0;
			for (char c : replaceHtml(StringEscapeUtils.unescapeHtml4(str)).toCharArray()) {
				currentLength += String.valueOf(c).getBytes("GBK").length;
				if (currentLength <= length - 3) {
					sb.append(c);
				} else {
					sb.append("...");
					break;
				}
			}
			return sb.toString();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return "";
	}



	/**
	 * 替换掉HTML标签方法
	 */
	public static String replaceHtml(String html) {
		if (isBlank(html)){
			return "";
		}
		String regEx = "<.+?>";
		Pattern p = Pattern.compile(regEx);
		Matcher m = p.matcher(html);
		String s = m.replaceAll("");
		return s;
	}



	/**
	 * 转换为JS获取对象值，生成三目运算返回结果
	 * @param objectString 对象串
	 *   例如：row.user.id
	 *   返回：!row?'':!row.user?'':!row.user.id?'':row.user.id
	 */
	public static String javascriptGetValueStyle(String objectString){
		StringBuilder result = new StringBuilder();
		StringBuilder val = new StringBuilder();
		String[] vals = split(objectString, ".");
		for (int i=0; i<vals.length; i++){
			val.append("." + vals[i]);
			result.append("!"+(val.substring(1))+"?'':");
		}
		result.append(val.substring(1));
		return result.toString();
	}




	/**
	 * 空位符格式化字符串
	 */
	public static String format(String format, Object... args) {
		FormattingTuple ft = MessageFormatter.arrayFormat(format, args);
		return ft.getMessage();
	}


	/**
	 *
	 * @param part
	 * @return
	 */
	public static String urlEncode(String part){
		try{
			return URLEncoder.encode(part,CHARSET_NAME);
		}catch (UnsupportedEncodingException e){
			throw ExceptionHelper.unchecked(e);
		}
	}

	private UtilString() {
	}


}