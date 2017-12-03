/* Log.java


	Purpose: The logging utilities
	Description:
	History:
	2001/11/07 14:55:06, Create, Tom M. Yeh.

Copyright (C) 2001 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under LGPL Version 2.1 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/
package org.zkoss.util.logging;

import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.logging.LogManager;
import java.util.logging.Level;
import java.util.logging.Handler;
import java.io.ByteArrayInputStream;

import org.zkoss.lang.Library;
import org.zkoss.lang.Objects;
import org.zkoss.lang.Exceptions;
import org.zkoss.util.Locales;
import org.zkoss.mesg.Messages;

/**
 * The logger. Usage:
 *
 * <p><code>private static final Log log = Log.lookup(MyClass.class);<br>
 * ...<br>
 * if (log.debugable()) log.debug("the information to log:"+some);</code>
 *
 * <p>{@link Log} is designed to minimize the memory usage by avoiding
 * unnecessary allocation of java.util.logging.Logger.
 * In additions, it is possible to use different logger, e.g., log4j,
 * without affecting the client codes.
 * 
 * <p>Since this object is very light-weight, it is OK to have
 * the following statement without using it.<br>
 * private static final Log log = Log.lookup(MyClass.class);
 *
 * <p>To log error or warning, simple use the error and warning method.
 *
 * <p>To log info, depending on the complexity you might want to test infoable
 * first.<br>
 * <pre><code>log.info("a simple info");
 *if (log.infoable())
 *  log.info(a + complex + string + operation);</code></pre>
 *
 * <p>To log debug information, we usually also test with D.<br>
 * <pre><code>log.debug("a simple info");
 *if (log.debugable())
 *  log.debug(a + complex + string + operation);</code></pre>
 *
 * <p>There is a special level called FINER whose priority is lower than DEBUG.
 * It is generally used
 * to log massive debug message under certain situation. In other words,
 * it is suggested to use the debug methods to log messages as follows:
 *
 * <pre><code>if (log.finerable()) {
 *  ... do massive testing and/or printing (use finer)
 *}</code></pre>
 *
 * @author tomyeh
 * @deprecated As of release 7.0.0, use SLF4J API for logging instead.
 */
public class Log {
	/** All levels. */
	public static final Level ALL     = Level.ALL;
	/** The ERROR level. */
	public static final Level ERROR   = Level.SEVERE;
	/** The WARNING level. */
	public static final Level WARNING = Level.WARNING;
	/** The INFO level. */
	public static final Level INFO    = Level.INFO;
	/** The DEBUG level. */
	public static final Level DEBUG   = Level.FINE;
	/** The FINER level. */
	public static final Level FINER   = Level.FINER;
	/** The OFF level used to turn of the logging. */
	public static final Level OFF     = Level.OFF;

	/** The default name when the loggers don't support hierarchy. */
	private static final String DEFAULT_NAME = "org.zkoss";
		//don't change its value since DHtmlLayoutServlet depends on it

	/** Whether the loggers supports the hierarchy. */
	private static boolean _hierarchy;
	/** ZK-694: we have to store it to avoid being GCed. */
	private static Logger[] _configedLoggers;
	private static Logger _default;

	/** The category that this log belongs.
	 * Note: it is temporary and set to null when {@link #logger} is called.
	 */
	private String _name;
	/** Used if useHierachy() is true. */
	private Logger _logger;

	/**
	 * Configures based the properties.
	 *
	 * <p>The key is a logger name and the value is the level.
	 *
	 * @param props the properties
	 * @since 6.0.0
	 */
	public final static void configure(Properties props) {
		Log log = null;
		final StringBuffer sb = new StringBuffer();
		String[] handlers = null;
		
		// read handlers first - ZK-1893
		final String values = (String) props.remove("handlers");
		if (values != null) {
			handlers = values.split("[, ]");
			for (int j = handlers.length; --j >= 0;) {
				handlers[j] = handlers[j].trim();
				handlers[j] = handlers[j].length() > 0 ? handlers[j] + '.'
						: null;
			}
			sb.append("handlers").append('=').append(values).append('\n');
		}
		
		for (Iterator<Map.Entry<Object, Object>> it = props.entrySet().iterator();
		it.hasNext();) {
			final Map.Entry<Object, Object> me = it.next();
			final String key = (String)me.getKey();
			final String val = (String)me.getValue();
			boolean matched = false;
			if (handlers != null) {
				for (String h: handlers) {
					if (h != null && key.startsWith(h)) {
						matched = true;
						break;
					}
				}
			}

			if (matched) {
				sb.append(key).append('=').append(val).append('\n');
				it.remove();
			}
		}

		if (sb.length() > 0) {
			try {
				LogManager.getLogManager().readConfiguration(
					new ByteArrayInputStream(sb.toString().getBytes()));
			} catch (Throwable ex) { //GAE doesn't support LogManager
				lookup(Log.class).warningBriefly("Failed to configure LogManager", ex);
			}
		}

		final List<Logger> configed = new LinkedList<Logger>();
		for (Map.Entry<Object, Object> me: props.entrySet()) {
			String key = (String)me.getKey();
			final String val = ((String)me.getValue()).trim();

			if (key.endsWith(".level")) //compatible with LogManager
				key = key.substring(0, key.length() - 6);

			final Level level = Log.getLevel(val);
			if (level != null) {
				final Logger logger = Logger.getLogger(key);
				logger.setLevel(level);
				configed.add(logger);
			} else {
				if (log == null)
					log = lookup(Log.class);
				log.warning("Illegal log level, "+val+", for "+key);
			}
		}

		//ZK-694: we have to store them. Otherwise, they will be GCed
		if (!configed.isEmpty()) {
			setHierarchy(true); //turn on the hierarchy
			_configedLoggers = configed.toArray(new Logger[configed.size()]);
		}
	}

	/** Returns whether the loggers support hierarchy.
	 * If hierarchy is supported, a {@link Log} instance is mapped to
	 * a {@link Logger} instance with the same name. Therefore, it
	 * forms the hierarchical relationship among {@link Logger} instances.
	 * It has the best resolution to control which logger to enable.
	 *
	 * <p>On the other hand, if the loggers don't support hierarchy,
	 * all {@link Log} instances are actually mapped to the same
	 * {@link Logger} called "org.zkoss".
	 * The performance is better in this mode.
	 *
	 * <p>Default: false.
	 *
	 * <p>Note: {@link #configure} will invoke {@link #setHierarchy} with
	 * true automatically to turn on the hierarchy support, if any level
	 * is defined.
	 */
	public static final boolean isHierarchy() {
		return _hierarchy;
	}
	/** Sets whether to support the hierarchical loggers.
	 */
	public static final void setHierarchy(boolean hierarchy) {
		_hierarchy = hierarchy;
	}

	/** This property is deprecated in ZK 6, since it is longer required for GAE.
	 * HOwever, we keep it here for backward compatibility.
	 * We might remove it in the future.
	 */
	private static final boolean hierarchyDisabled() {
		if (_hierarchyDisabled == null)
			_hierarchyDisabled = Boolean.valueOf("true".equals(
				Library.getProperty("org.zkoss.util.logging.hierarchy.disabled")));
		return _hierarchyDisabled.booleanValue();
	}
	private static Boolean _hierarchyDisabled;

	/**
	 * Gets the logger based on the class.
	 * @param cls the class that identifies the logger.
	 */
	public static final Log lookup(Class cls) {
		return new Log(cls.getName());
	}
	/**
	 * Gets the logger based on the giving name.
	 * <p>Since 5.0.7, this constructor, unlike others, ignores
	 * {@link #isHierarchy} and always assumes the hierarchy name.
	 * Notice the hierarchy is always disabled if a library property called
	 * <code>org.zkoss.util.logging.hierarchy.disabled</code> is set to true.
	 */
	public static final Log lookup(String name) {
		return new HierLog(name);
	}
	/** Gets the logger based on the package.
	 */
	public static final Log lookup(Package pkg) {
		return new Log(pkg.getName());
	}

	/**
	 * The constructor.
	 */
	protected Log(String name) {
		if (name == null)
			throw new IllegalArgumentException(name);
		_name = name;
	}

	/** Returns the name of this logger.
	 */
	public final String getName() {
		return _name;
	}

	/** Returns the logger (never null).
	 * <p>If not found, it created a new one.
	 */
	private final Logger getLogger() {
		if (!useHierarchy()) {
			if (_default != null)
				return _default;
			return _default = Logger.getLogger(DEFAULT_NAME);
		}
		if (_logger != null)
			return _logger;
		return _logger = Logger.getLogger(_name);
		//Note: we have to t save Logger.getLogger(). Otherwise, it will be GCed
	}

	/**
	 * Returns the logging level.
	 */
	public final Level getLevel() {
		return getLogger().getLevel();
	}
	/**
	 * Sets the logging level.
	 */
	public final void setLevel(Level level) {
		getLogger().setLevel(level);
	}
	/**
	 * Sets the logging level.
	 * @since 5.0.7
	 */
	public final void setLevel(String level) {
		Level l = getLevel(level);
		if (l == null)
			throw new IllegalArgumentException("Unknown level: "+level);
		setLevel(l);
	}

	/** Return the logging level of the specified string.
	 * @return the level; null if no match at all
	 */
	public static final Level getLevel(String level) {
		if (level != null) {
			level = level.toUpperCase();
			if (level.equals("DEBUG") || level.equals("FINE"))
				return Log.DEBUG;
			if (level.equals("ERROR") || level.equals("SEVERE"))
				return Log.ERROR;
			if (level.equals("FINER")) return Log.FINER; 
			if (level.equals("INFO")) return Log.INFO;
			if (level.equals("WARNING")) return Log.WARNING;
			if (level.equals("OFF")) return Log.OFF;
		}
		return null;
	}

	/**
	 * Tests whether the {@link #ERROR} level is loggable.
	 */
	public final boolean errorable() {
		return getLogger().isLoggable(ERROR);
	}
	/**
	 * Tests whether the {@link #WARNING} level is loggable.
	 */
	public final boolean warningable() {
		return getLogger().isLoggable(WARNING);
	}
	/**
	 * Tests whether the {@link #INFO} level is loggable.
	 */
	public final boolean infoable() {
		return getLogger().isLoggable(INFO);
	}
	/**
	 * Tests whether the {@link #DEBUG} level is loggable.
	 */
	public final boolean debugable() {
		return getLogger().isLoggable(DEBUG);
	}
	/**
	 * Tests whether the {@link #FINER} level is loggable.
	 */
	public final boolean finerable() {
		return getLogger().isLoggable(FINER);
	}

	/**
	 * Logs a message and a throwable object at the giving level.
	 *
	 * <p>All log methods eventually invokes this method to log messages.
	 *
	 * @param t the throwable object; null to ignore
	 */
	public final void log(Level level, String msg, Throwable t) {
		final Logger logger = getLogger();
		if (logger.isLoggable(level)) {
			//We have to unveil the stack frame to find the real source
			//Otherwise, Logger.log will report the wrong source
			//We cannot skip the first few frames because optimizer might
			//preserve all frames
			StackTraceElement[] stack = new Throwable().getStackTrace();
			String cname = "", mname = "";
			for (int j = 0; j < stack.length; ++j) {
				if (!stack[j].getClassName()
				.equals("org.zkoss.util.logging.Log")) {
					cname = stack[j].getClassName();
					mname = stack[j].getMethodName() + ':'
						+ stack[j].getLineNumber();
					break;
				}
			}

			if (t != null)
				logger.logp(level, cname, mname, msg, t);
			else
				logger.logp(level, cname, mname, msg);
		}
	}
	/**
	 * Logs any object and a throwable object at the giving level.
	 *
	 * @param obj the object whose toString method is called to get the message
	 */
	public final void log(Level level, Object obj, Throwable t) {
		log(level, Objects.toString(obj), t);
	}
	/**
	 * Logs a message and a throwable object at the giving level
	 * by giving a message code and multiple format arguments.
	 *
	 * @param t the throwable object; null to ignore
	 */
	public final void
	log(Level level, int code, Object[] fmtArgs, Throwable t) {
		final Locale l = Locales.setThreadLocal(null);
		try {
			log(level, Messages.get(code, fmtArgs), t);
		} finally {
			Locales.setThreadLocal(l);
		}
	}
	/**
	 * Logs a message and a throwable object at the giving level
	 * by giving a message code and ONE format argument.
	 *
	 * @param t the throwable object; null to ignore
	 */
	public final void log(Level level, int code, Object fmtArg, Throwable t) {
		final Locale l = Locales.setThreadLocal(null);
		try {
			log(level, Messages.get(code, fmtArg), t);
		} finally {
			Locales.setThreadLocal(l);
		}
	}
	/**
	 * Logs a message and a throwable object at the giving level
	 * by giving a message code and NO format argument.
	 *
	 * @param t the throwable object; null to ignore
	 */
	public final void log(Level level, int code, Throwable t) {
		final Locale l = Locales.setThreadLocal(null);
		try {
			log(level, Messages.get(code), t);
		} finally {
			Locales.setThreadLocal(l);
		}
	}

	private String format(String format,Object... args){
		String d;
		try{
			d = String.format(format, args);
		}catch(Exception x){
			d = x.getMessage()+": "+format;
		}
		return d;
	}

	//-- ERROR --//
	/** Logs a debug message with a format and arguments.
	 * The message is formatted by use of String.format.
	 * @since 6.0.0
	 */
	public void error(String format,Object... args){
		error(format(format,args));
	}
	/**
	 * Logs an error message and a throwable object.
	 *
	 * @see #errorable
	 */
	public final void error(String msg, Throwable t) {
		log(ERROR, msg, t);
	}
	/**
	 * Logs an error message.
	 */
	public final void error(String msg) {
		log(ERROR, msg, null);
	}
	/**
	 * Logs an object, whose toString returns the error message,
	 * and a throwable object.
	 *
	 * @param obj the object whose toString method is called to get the message
	 */
	public final void error(Object obj, Throwable t) {
		log(ERROR, obj, t);
	}
	/**
	 * Logs an object, whose toString returns the error message.
	 *
	 * @param obj the object whose toString method is called to get the message
	 */
	public final void error(Object obj) {
		log(ERROR, obj, null);
	}
	/**
	 * Logs an error throwable object.
	 */
	public final void error(Throwable t) {
		log(ERROR, "", t);
	}
	/**
	 * Logs an error message and a throwable object by giving message code.
	 */
	public final void error(int code, Object[] fmtArgs, Throwable t) {
		log(ERROR, code, fmtArgs, t);
	}
	/**
	 * Logs an error message and a throwable object by giving message code.
	 */
	public final void error(int code, Object fmtArg, Throwable t) {
		log(ERROR, code, fmtArg, t);
	}
	/**
	 * Logs an error message and a throwable object by giving message code.
	 */
	public final void error(int code, Throwable t) {
		log(ERROR, code, t);
	}
	/**
	 * Logs an error message by giving message code.
	 */
	public final void error(int code, Object[] fmtArgs) {
		log(ERROR, code, fmtArgs, null);
	}
	/**
	 * Logs an error message by giving message code.
	 */
	public final void error(int code, Object fmtArg) {
		log(ERROR, code, fmtArg, null);
	}
	/**
	 * Logs an error message by giving message code.
	 */
	public final void error(int code) {
		log(ERROR, code, null);
	}

	//-- WARNING --//
	/** Logs a warning message with a format and arguments.
	 * The message is formatted by use of String.format.
	 * @since 6.0.0
	 */
	public void warning(String format,Object... args){
		warning(format(format,args));
	}
	/**
	 * Logs a warning message and a throwable object.
	 *
	 * @see #warningable
	 */
	public final void warning(String msg, Throwable t) {
		log(WARNING, msg, t);
	}
	/**
	 * Logs a warning message.
	 */
	public final void warning(String msg) {
		log(WARNING, msg, null);
	}
	/**
	 * Logs an object, whose toString returns the warning message,
	 * and a throwable object.
	 *
	 * @param obj the object whose toString method is called to get the message
	 */
	public final void warning(Object obj, Throwable t) {
		log(WARNING, obj, t);
	}
	/**
	 * Logs an object, whose toString returns the warning message.
	 *
	 * @param obj the object whose toString method is called to get the message
	 */
	public final void warning(Object obj) {
		log(WARNING, obj, null);
	}
	/**
	 * Logs a warning throwable object.
	 */
	public final void warning(Throwable t) {
		log(WARNING, "", t);
	}
	/**
	 * Logs a warning message and a throwable object by giving message code.
	 */
	public final void warning(int code, Object[] fmtArgs, Throwable t) {
		log(WARNING, code, fmtArgs, t);
	}
	/**
	 * Logs a warning message and a throwable object by giving message code.
	 */
	public final void warning(int code, Object fmtArg, Throwable t) {
		log(WARNING, code, fmtArg, t);
	}
	/**
	 * Logs a warning message and a throwable object by giving message code.
	 */
	public final void warning(int code, Throwable t) {
		log(WARNING, code, t);
	}
	/**
	 * Logs a warning message by giving message code.
	 */
	public final void warning(int code, Object[] fmtArgs) {
		log(WARNING, code, fmtArgs, null);
	}
	/**
	 * Logs a warning message by giving message code.
	 */
	public final void warning(int code, Object fmtArg) {
		log(WARNING, code, fmtArg, null);
	}
	/**
	 * Logs a warning message by giving message code.
	 */
	public final void warning(int code) {
		log(WARNING, code, null);
	}

	//-- INFO --//
	/** Logs an info message with a format and arguments.
	 * The message is formatted by use of String.format.
	 * @since 6.0.0
	 */
	public void info(String format,Object... args){
		info(format(format,args));
	}
	/**
	 * Logs an info message and a throwable object.
	 *
	 * @see #infoable
	 */
	public final void info(String msg, Throwable t) {
		log(INFO, msg, t);
	}
	/**
	 * Logs an info message.
	 */
	public final void info(String msg) {
		log(INFO, msg, null);
	}
	/**
	 * Logs an object, whose toString returns the info message,
	 * and a throwable object.
	 *
	 * @param obj the object whose toString method is called to get the message
	 */
	public final void info(Object obj, Throwable t) {
		log(INFO, obj, t);
	}
	/**
	 * Logs an object, whose toString returns the info message.
	 *
	 * @param obj the object whose toString method is called to get the message
	 */
	public final void info(Object obj) {
		log(INFO, obj, null);
	}
	/**
	 * Logs an info throwable object.
	 */
	public final void info(Throwable t) {
		log(INFO, "", t);
	}
	/**
	 * Logs an info message and a throwable object by giving message code.
	 */
	public final void info(int code, Object[] fmtArgs, Throwable t) {
		log(INFO, code, fmtArgs, t);
	}
	/**
	 * Logs an info message and a throwable object by giving message code.
	 */
	public final void
	info(int code, Object fmtArg, Throwable t) {
		log(INFO, code, fmtArg, t);
	}
	/**
	 * Logs an info message and a throwable object by giving message code.
	 */
	public final void info(int code, Throwable t) {
		log(INFO, code, t);
	}
	/**
	 * Logs an info message by giving message code.
	 */
	public final void info(int code, Object[] fmtArgs) {
		log(INFO, code, fmtArgs, null);
	}
	/**
	 * Logs an info message by giving message code.
	 */
	public final void info(int code, Object fmtArg) {
		log(INFO, code, fmtArg, null);
	}
	/**
	 * Logs an info message by giving message code.
	 */
	public final void info(int code) {
		log(INFO, code, null);
	}

	//-- DEBUG --//
	/** Logs a debug message with a format and arguments.
	 * The message is formatted by use of String.format.
	 * @since 6.0.0
	 */
	public void debug(String format,Object... args){
		debug(format(format,args));
	}
	/**
	 * Logs a debug message and a throwable object.
	 *
	 * @since #debugable
	 */
	public final void debug(String msg, Throwable t) {
		log(DEBUG, msg, t);
	}
	/**
	 * Logs a debug message.
	 */
	public final void debug(String msg) {
		log(DEBUG, msg, null);
	}
	/**
	 * Logs an object, whose toString returns the debug message,
	 * and a throwable object.
	 *
	 * @param obj the object whose toString method is called to get the message
	 */
	public final void debug(Object obj, Throwable t) {
		log(DEBUG, obj, t);
	}
	/**
	 * Logs an object, whose toString returns the debug message.
	 *
	 * @param obj the object whose toString method is called to get the message
	 */
	public final void debug(Object obj) {
		log(DEBUG, obj, null);
	}
	/**
	 * Logs a debug throwable object.
	 */
	public final void debug(Throwable t) {
		log(DEBUG, "", t);
	}
	/**
	 * Logs a debug message and a throwable object by giving message code.
	 */
	public final void debug(int code, Object[] fmtArgs, Throwable t) {
		log(DEBUG, code, fmtArgs, t);
	}
	/**
	 * Logs a debug message and a throwable object by giving message code.
	 */
	public final void debug(int code, Object fmtArg, Throwable t) {
		log(DEBUG, code, fmtArg, t);
	}
	/**
	 * Logs a debug message and a throwable object by giving message code.
	 */
	public final void debug(int code, Throwable t) {
		log(DEBUG, code, t);
	}
	/**
	 * Logs a debug message by giving message code.
	 */
	public final void debug(int code, Object[] fmtArgs) {
		log(DEBUG, code, fmtArgs, null);
	}
	/**
	 * Logs a debug message by giving message code.
	 */
	public final void debug(int code, Object fmtArg) {
		log(DEBUG, code, fmtArg, null);
	}
	/**
	 * Logs a debug message by giving message code.
	 */
	public final void debug(int code) {
		log(DEBUG, code, null);
	}

	//-- FINER --//
	/** Logs a finer message with a format and arguments.
	 * The message is formatted by use of String.format.
	 * @since 6.0.0
	 */
	public void finer(String format,Object... args){
		finer(format(format,args));
	}
	/**
	 * Logs a finer message and a throwable object.
	 */
	public final void finer(String msg, Throwable t) {
		log(FINER, msg, t);
	}
	/**
	 * Logs a finer message.
	 */
	public final void finer(String msg) {
		log(FINER, msg, null);
	}
	/**
	 * Logs an object, whose toString returns the finer message,
	 * and a throwable object.
	 *
	 * @param obj the object whose toString method is called to get the message
	 */
	public final void finer(Object obj, Throwable t) {
		log(FINER, obj, t);
	}
	/**
	 * Logs an object, whose toString returns the finer message.
	 *
	 * @param obj the object whose toString method is called to get the message
	 */
	public final void finer(Object obj) {
		log(FINER, obj, null);
	}
	/**
	 * Logs a finer throwable object.
	 */
	public final void finer(Throwable t) {
		log(FINER, "", t);
	}
	/**
	 * Logs a finer message and a throwable object by giving message code.
	 */
	public final void finer(int code, Object[] fmtArgs, Throwable t) {
		log(FINER, code, fmtArgs, t);
	}
	/**
	 * Logs a finer message and a throwable object by giving message code.
	 */
	public final void finer(int code, Object fmtArg, Throwable t) {
		log(FINER, code, fmtArg, t);
	}
	/**
	 * Logs a finer message and a throwable object by giving message code.
	 */
	public final void finer(int code, Throwable t) {
		log(FINER, code, t);
	}
	/**
	 * Logs a finer message by giving message code.
	 */
	public final void finer(int code, Object[] fmtArgs) {
		log(FINER, code, fmtArgs, null);
	}
	/**
	 * Logs a finer message by giving message code.
	 */
	public final void finer(int code, Object fmtArg) {
		log(FINER, code, fmtArg, null);
	}
	/**
	 * Logs a finer message by giving message code.
	 */
	public final void finer(int code) {
		log(FINER, code, null);
	}

	/** Logs only the real cause of the specified exception.
	 * It is useful because sometimes the stack trace is too big.
	 */
	public final void realCause(Throwable ex) {
		realCause(null, ex);
	}
	/** Logs only the real cause of the specified exception with an extra
	 * message as an error message.
	 */
	public final void realCause(String message, Throwable ex) {
		realCause0(message, ex, true, 0);
	}
	/** Logs only the first few lines of the real cause as an error message.
	 * <p>To control the number of lines to log, you can specify a library
	 * property called org.zkoss.util.logging.realCauseBriefly.lines.
	 * If not specified, 6 is assumed. If nonpositive is specified, the full stack
	 * traces are logged.
	 * <p>Notice that # of lines don't include packages starting with java, javax or sun.
	 */
	public final void realCauseBriefly(String message, Throwable ex) {
		realCause0(message, ex, true,
			Library.getIntProperty("org.zkoss.util.logging.realCauseBriefly", 6));
	}

	/** Logs only the first few lines of the real cause as an error message.
	 */
	public final void realCauseBriefly(Throwable ex) {
		realCauseBriefly(null, ex);
	}
	private final
	void realCause0(String message, Throwable ex, boolean err, int maxcnt) {
		final StringBuffer sb = new StringBuffer(1024);
		if (message != null)
			sb.append(message).append('\n');

		while (true) {
			Throwable cause = Exceptions.getCause(ex);
			if (cause == null)
				break;
			sb.append(">>").append(ex.getClass().getName())
				.append(": ").append(ex.getMessage()).append('\n');
			ex = cause;
		}

		String s = Exceptions.getExtraMessage(ex);
		if (s != null)
			sb.append(s).append('\n');

		message = Exceptions.formatStackTrace(sb, ex, ">>", maxcnt).toString();
		if (err) error(message);
		else warning(message);
	}

	/** Logs only the first few lines of the real cause as an warning message.
	 * <p>To control the number of lines to log, you can specify a library
	 * property called org.zkoss.util.logging.warningBriefly.lines.
	 * If not specified, 3 is assumed. If nonpositive is specified, the full stack
	 * traces are logged.
	 * <p>Notice that # of lines don't include packages starting with java, javax or sun.
	 */
	public final void warningBriefly(String message, Throwable ex) {
		realCause0(message, ex, false,
			Library.getIntProperty("org.zkoss.util.logging.warningBriefly", 3));
	}
	/** Lo only the first few lines of the real cause.
	 */
	public final void warningBriefly(Throwable ex) {
		warningBriefly(null, ex);
	}
	/** Logs an exception as an warning message about being eaten
	 * (rather than thrown).
	 */
	public final void eat(String message, Throwable ex) {
		if (debugable()) {
			warningBriefly(message, ex);
		} else {
			warning(
BAR+(message != null ? "\n"+message: "")
+"\n"+"The exception:\n"+Exceptions.getMessage(ex)+"\n"
+"If you want to see the stack trace, turn the DEBUG level on for "+_name+"\n"+BAR);
		}
	}
	private final String BAR = "------------";

	/** Logs an exception as an warning message about being eaten
	 * (rather than thrown).
	 */
	public final void eat(Throwable ex) {
		eat(null, ex);
	}

	/** Returns whether to use hierarchy.
	 * <p>Default: {@link #isHierarchy}.
	 */
	/*package*/ boolean useHierarchy() {
		return _hierarchy && !hierarchyDisabled();
	}

	public int hashCode() {
		return _name.hashCode();
	}
	public boolean equals(Object o) {
		if (this == o) return true;
		return o instanceof Log && ((Log)o)._name.equals(_name);
	}
	public String toString() {
		return _name;
	}

	/**
	 * Used internally to represent a hierarchical log.
	 */
	private static class HierLog extends Log {
		private HierLog(String name) {
			super(name);
		}
		/*package*/ boolean useHierarchy() {
			return !hierarchyDisabled();
		}
	}
}
