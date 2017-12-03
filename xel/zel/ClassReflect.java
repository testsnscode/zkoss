/* ClassReflect.java

	History:
		Fri, Sep 27, 2013 11:03:08 PM, Created by tomyeh

Copyright (C) 2013 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.xel.zel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.zkoss.lang.Classes;

/**
 * The implementation for using ZEL.
 *
 * Notice that it is instantiated in zel.jar.
 *
 * @author tomyeh
 */
public class ClassReflect implements org.zkoss.zel.impl.util.ClassReflect {
	/**
	 * Gets one of the close methods -- a close method is a method
	 * with the same name and the compatible argument type.
	 */
	public Method
	getCloseMethod(Class<?> cls, String name, Class<?>[] argTypes)
	throws NoSuchMethodException {
		return Classes.getCloseMethod(cls, name, argTypes);
	}

	/**
	 * Instantiates a new instance of the specified class with the
	 * specified argument.
	 */
	public Object newInstance(Class<?> cls, Object[] args)
	throws NoSuchMethodException, InstantiationException,
	InvocationTargetException, IllegalAccessException {
		return Classes.newInstance(cls, args);
	}

	/**
	 * Returns the context ClassLoader for the reference class.
	 * @param reference the reference class where it is invoked from.
	 * @since 8.0.2
	 */
	public ClassLoader getContextClassLoader(Class<?> reference) {
		return Classes.getContextClassLoader(reference);
	}
}
