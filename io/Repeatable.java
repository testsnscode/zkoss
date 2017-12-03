/* Repeatable.java

	Purpose:
		
	Description:
		
	History:
		Fri Mar 14 14:30:52     2008, Created by tomyeh

Copyright (C) 2008 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under LGPL Version 2.1 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/
package org.zkoss.io;

/**
 * Used to decorate java.io.InputStream or java.io.Reader to denote
 * that it can be read repeatedly.
 * By repeatable-read we mean, after close(), the next invocation of
 * read() will re-open the input stream or reader.
 *
 * @author tomyeh
 * @since 3.0.4
 */
public interface Repeatable {
}
