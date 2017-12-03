/* StringFns.java

	Purpose:
		
	Description:
		
	History:
		Thu Mar 31 12:25:57     2005, Created by tomyeh

Copyright (C) 2005 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under LGPL Version 2.1 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/
package org.zkoss.xel.fn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.lang.Objects;
import org.zkoss.util.Locales;

/**
 * Functions to manipulate strings in EL.
 *
 * @author tomyeh
 */
public class StringFns {
	private static Logger log = LoggerFactory.getLogger(StringFns.class);

	/** Concatenates two objects.
	 * Note: null is considered as empty.
	 */
	public static String cat(Object o1, Object o2) {
		return (o1 == null ? "" : String.valueOf(o1)) + (o2 == null ? "" : String.valueOf(o2));
	}
	/** Concatenates three objects.
	 * Note: null is considered as empty.
	 */
	public static String cat3(Object o1, Object o2, Object o3) {
		return cat(cat(o1, o2), o3);
	}
	/** Concatenates four objects.
	 * Note: null is considered as empty.
	 */
	public static String cat4(Object o1, Object o2, Object o3, Object o4) {
		return cat(cat(cat(o1, o2), o3), o4);
	}
	/** Concatenates five objects.
	 * Note: null is considered as empty.
	 */
	public static String cat5(Object o1, Object o2, Object o3, Object o4, Object o5) {
		return cat(cat(cat(cat(o1, o2), o3), o4), o5);
	}

	/** Converts all of the characters in this String to upper case using the rules of the current Locale.
	 * @see Locales#getCurrent
	 * @since 5.0.7
	 */
	public static String toLowerCase(String s) {
		return s != null ? s.toLowerCase(Locales.getCurrent()): null;
	}
	/** Converts all of the characters in this String to upper case using the rules of the current Locale.
	 * @see Locales#getCurrent
	 * @since 5.0.7
	 */
	public static String toUpperCase(String s) {
		return s != null ? s.toUpperCase(Locales.getCurrent()): null;
	}
	/** Returns a copy of the string, with leading and trailing whitespace omitted.
	 * @since 5.0.7
	 */
	public static String trim(String s) {
		return s != null ? s.trim(): null;
	}
	/** Splits a string.
	 * @since 5.0.7
	 */
	public static String[] split(String s, String separator) {
		return s != null ? s.split(separator): null;
	}
	/** Joins an array of string.
	 * since 5.0.7
	 */
	public static String join(Object[] ss, String separator) {
		if (ss == null) return null;

		final StringBuffer sb = new StringBuffer();
		for (int j = 0; j < ss.length; ++j) {
			if (j != 0)
				sb.append(separator);
			sb.append(ss[j]);
		}
		return sb.toString();
	}
	/** Tests if this string starts with the specified prefix.
	 * @since 5.0.7
	 */
	public static boolean startsWith(String s1, String s2) {
		return s1 != null && s2 != null && s1.startsWith(s2);
	}
	/** Tests if this string ends with the specified suffix.
	 * @since 5.0.7
	 */
	public static boolean endsWith(String s1, String s2) {
		return s1 != null && s2 != null && s1.endsWith(s2);
	}
	/** Returns a new string that is a substring of this string.
	 * @since 5.0.7
	 */
	public static String substring(String s, int from, int to) {
		return s != null ? s.substring(from, to): null;
	}
	/** Replaces all occurrences of 'from' in 'src' with 'to'
	 */
	public static String replace(String src, String from, String to) {
		if (Objects.equals(from, to))
			return src;

		final StringBuffer sb = new StringBuffer(src);
		if ("\n".equals(from) || "\r\n".equals(from)) {
			replace0(sb, "\r\n", to);
			replace0(sb, "\n", to);
		} else {
			replace0(sb, from, to);
		}
		return sb.toString();
	}
	private static void replace0(StringBuffer sb, String from, String to) {
		final int len = from.length();

		for (int j = 0; (j = sb.indexOf(from, j)) >= 0;) {
			sb.replace(j, j + len, to);
			j += to.length()  ; //ZK-929 : update index to prevent infinite loop
		}
	}

	/** Eliminates single and double quotations to avoid JavaScript
	 * injection.
	 * It eliminates all quotations. In other words, the specified string
	 * shall NOT contain any quotations.
	 *
	 * <p>It is used to avoid JavaScript injection.
	 * For example, in DSP or JSP pages, the following codes is better
	 * to escape with this method.
	 * <code><input value="${c:eatQuot(param.some)}"/></code>
	 *
	 * @since 3.5.2
	 */
	public static String eatQuot(String s) {
		final int len = s != null ? s.length(): 0;
		StringBuffer sb = null;
		for (int j = 0; j < len; ++j) {
			final char cc = s.charAt(j);
			if (cc == '\'' || cc == '"') {
				if (sb == null) {
					log.warn("JavaScript Injection? Unexpected string detected: "+s);
					sb = new StringBuffer(len);
					if (j > 0) sb.append(s.substring(0, j));
				}
			} else if (sb != null)
				sb.append(cc);
		}
		return sb != null ? sb.toString(): s;
	}
}
