/* Converter.java

	Purpose:
		
	Description:
		
	History:
		Wed Aug  3 12:00:16 TST 2011, Created by tomyeh

Copyright (C) 2011 Potix Corporation. All Rights Reserved.

*/
package org.zkoss.util;

import java.util.Collection;

/**
 * A converter used to convert a value to another.
 *
 * @author tomyeh
 * @since 6.0.0
 * @see CollectionsX#comodifiableIterator(Collection, Converter)
 */
public interface Converter<F, T> {
	public T convert(F obj);
}
