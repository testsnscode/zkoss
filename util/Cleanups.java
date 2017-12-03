/* Cleanups.java

	Purpose:
		
	Description:
		
	History:
		Fri Mar 12 11:55:59 TST 2010, Created by tomyeh

Copyright (C) 2010 Potix Corporation. All Rights Reserved.

*/
package org.zkoss.util;

import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities to clean up resources when the application is stopping.
 * Currently, when a ZK application is stopping, ZK will invoke {@link #cleanup}.
 * Thus you can register an implementation of {@link Cleanup}
 * to release the resources.
 * <p>Notice that this utility is introduced mainly to resolve
 * the memory lead issue if an application is hot re-deployed.
 * A typical example is to stop any pooled threads.
 * <h3>How to use:</h3>
 * <p>First, register the cleanup with {@link #add}.</p>
 * <p>Second, invoke {@link #cleanup} when necessary, such as when the application
 * is stopping</p>.
 *
 * @author tomyeh
 * @since 3.6.5
 */
public class Cleanups {
	private static final Logger log = LoggerFactory.getLogger(Cleanups.class);
	private static final List<Cleanup> _cleanups = new LinkedList<Cleanup>();

	/** Registers a cleanup.
	 * @return true if it is added successfully, or false if the cleanup
	 * has been registered before.
	 */
	public static boolean add(Cleanup cleanup) {
		if (cleanup == null)
			throw new IllegalArgumentException();

		synchronized (_cleanups) {
			if (_cleanups.contains(cleanup))
				return false;
			return _cleanups.add(cleanup);
		}
	}
	/** Un-registers a cleanup.
	 * @return true if it is removed successfully, or false if not registered before.
	 */
	public static boolean remove(Cleanup cleanup) {
		synchronized (_cleanups) {
			return _cleanups.remove(cleanup);
		}
	}

	/** Invokes all cleanups registered with {@link #add}.
	 */
	public static void cleanup() {
		final List<Cleanup> cleanups;
		synchronized (_cleanups) {
			cleanups = new ArrayList<Cleanup>(_cleanups);
		}

		for (Cleanup cleanup: cleanups) {
			try {
				cleanup.cleanup();
			} catch (Throwable ex) {
				log.error("Failed to invoke "+cleanup);
			}
		}
	}
	/** The interface to implement for each cleanup.
	 * It is used with {@link Cleanups#add}.
	 * @since 3.6.5
	 */
	public static interface Cleanup {
		/** Cleanups the thread local variables.
		 */
		public void cleanup();
	}
}
