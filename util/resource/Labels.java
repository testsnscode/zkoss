/* Labels.java

	Purpose:
		
	Description:
		
	History:
		Tue Sep 21 10:55:09     2004, Created by tomyeh

Copyright (C) 2004 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under LGPL Version 2.1 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/
package org.zkoss.util.resource;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.lang.Classes;
import org.zkoss.lang.Library;
import org.zkoss.lang.SystemException;
import org.zkoss.util.resource.impl.LabelLoader;
import org.zkoss.util.resource.impl.LabelLoaderImpl;
import org.zkoss.text.MessageFormats;
import org.zkoss.xel.VariableResolver;

/**
 * Utilities to access labels. A label is a Locale-dependent string
 * that is stored in a Locale-dependent file (*.properties).
 * <p> Specify the library property of <code>org.zkoss.util.resource.LabelLoader.class</code>
 * in zk.xml to provide a customized label loader for debugging purpose. (since 7.0.1)
 *
 * @author tomyeh
 */
public class Labels {
	private static final Logger log = LoggerFactory.getLogger(Labels.class);
	private Labels() {} //prevent form misuse

	private static LabelLoader _loader;

	static {
		try {
			_loader = (LabelLoader) Classes.newInstanceByThread(Library.getProperty("org.zkoss.util.resource.LabelLoader.class", "org.zkoss.util.resource.impl.LabelLoaderImpl"));
		} catch (Exception e) {
			log.warn("", e);
			if (_loader == null)
				_loader = new LabelLoaderImpl();
		}
	}
	/** Returns the label of the specified key based
	 * on the current Locale, or null if no found.
	 *
	 * <p>The current locale is given by {@link org.zkoss.util.Locales#getCurrent}.
	 * @see #getSegmentedLabels
	 */
	public static final String getLabel(String key) {
		return _loader.getLabel(key);
	}
	/** Returns the label of the specified key and formats it
	 * with the specified argument, or null if not found.
	 *
	 * <p>It first uses {@link #getLabel(String)} to load the label.
	 * Then, it, if not null, invokes {@link MessageFormats#format} to format it.
	 *
	 * <p>The current locale is given by {@link org.zkoss.util.Locales#getCurrent}.
	 * @since 3.0.6
	 */
	public static final String getLabel(String key, Object[] args) {
		final String s = getLabel(key);
		return s != null ? MessageFormats.format(s, args, null): null;
	}

	/** Returns the label of the specified key based
	 * on the current Locale, or the default value if no found.
	 *
	 * <p>The current locale is given by {@link org.zkoss.util.Locales#getCurrent}.
	 *
	 * @param defValue the value being returned if the key is not found
	 * @since 3.6.0
	 */
	public static final String getLabel(String key, String defValue) {
		final String s = _loader.getLabel(key);
		return s != null ? s: defValue;
	}
	/** Returns the label of the specified key and formats it
	 * with the specified argument, or the default value if not found.
	 *
	 * <p>It first uses {@link #getLabel(String, String)} to load the label.
	 * Then, it, if not null, invokes {@link MessageFormats#format} to format it.
	 *
	 * <p>The current locale is given by {@link org.zkoss.util.Locales#getCurrent}.
	 * @param defValue the value being returned if the key is not found
	 * @since 3.6.0
	 */
	public static final String getLabel(String key, String defValue, Object[] args) {
		final String s = getLabel(key, defValue);
		return s != null ? MessageFormats.format(s, args, null): null;
	}

	/** Returns the label or a map of labels associated with the key.
	 * Unlike {@link #getLabel}, if a key of the label contains dot, it will
	 * be split into multiple keys and then grouped into map.
	 * For example, the following property file will parsed into a couple of maps,
	 * and <code>getSegmentedLabels()</code> returns a map containing
	 * a single entry. The entry's key is <code>"a"</code> and the value
	 * is another map with two entries <code>"b"</code> and <code>"c"</code>.
	 * And, the value for <code>"b"</code> is another two-entries map (containing
	 * <code>"c"</code> and <code>"d"</code>).
	 * <pre><code>
	 * a.b.c=1
	 * a.b.d=2
	 * a.e=3</pre></code>
	 * <p>This method is designed to make labels easier to be accessed in
	 * EL expressions.
	 * <p>On the other hand, {@link #getLabel} does not split them, and
	 * you could access them by, say, <code>getLabel("a.b.d")</code>.
	 * @since 5.0.7
	 */
	public static final Map<String, Object> getSegmentedLabels() {
		return _loader.getSegmentedLabels();
	}

	/** Returns the label of the specified key based on the current locale.
	 * Unlike {@link #getLabel(String)}, it throws an exception if not found.
	 *
	 * @exception SystemException if no such label
	 * @since 3.0.6
	 */
	public static final String getRequiredLabel(String key)
	throws SystemException {
		final String s = getLabel(key);
		if (s == null)
			throw new SystemException("label not found: "+key);
		return s;
	}
	/** Returns the label of the specified key and formats it
	 * with the specified argument, or null if not found.
	 * Unlike {@link #getLabel(String, Object[])}, it throws an exception if not found.
	 *
	 * <p>The current locale is given by {@link org.zkoss.util.Locales#getCurrent}.
	 * @exception SystemException if no such label
	 * @since 3.0.6
	 */
	public static final String getRequiredLabel(String key, Object[] args) {
		final String s = getLabel(key);
		if (s == null)
			throw new SystemException("label not found: "+key);
		return MessageFormats.format(s, args, null);
	}

	/** Resets all cached labels and next call to {@link #getLabel(String)}
	 * will cause re-loading the Locale dependent files.
	 */
	public static final void reset() {
		_loader.reset();
	}
	/** Sets the variable resolver, which is used if an EL expression
	 * is specified.
	 *
	 * <p>Default: no resolver at all.
	 *
	 * @return the previous resolver, or null if no resolver.
	 */
	public static final
	VariableResolver setVariableResolver(VariableResolver resolv) {
		return _loader.setVariableResolver(resolv);
	}
	/** Registers a locator which is used to load the Locale-dependent
	 * labels from other resource, such as servlet contexts.
	 */
	public static final void register(LabelLocator locator) {
		_loader.register(locator);
	}
	/** Registers a locator which is used to load the Locale-dependent
	 * labels from other resource, such as database.
	 * @since 5.0.5
	 */
	public static final void register(LabelLocator2 locator) {
		_loader.register(locator);
	}
}
