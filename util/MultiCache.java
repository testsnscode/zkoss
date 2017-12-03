/* MultiCache.java

	Purpose:
		
	Description:
		
	History:
		Wed Aug 29 17:29:45     2007, Created by tomyeh

Copyright (C) 2007 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under LGPL Version 2.1 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/
package org.zkoss.util;

import org.zkoss.lang.Objects;

/**
 * A {@link CacheMap} that uses multiple instanceof {@link CacheMap} to speed up
 * the performance.
 * It creates multiple instances of {@link CacheMap}, called
 * the internal caches, and then distributes the access across them.
 * Thus, the performance is proportional to the number of internal caches.
 *
 * <p>Thread safe.
 *
 * @author tomyeh
 * @since 3.0.0
 */
public class MultiCache<K, V> implements Cache<K, V>, java.io.Serializable, Cloneable {
	private final CacheMap<K, V>[] _caches;
	private int _maxsize, _lifetime;

	/** Constructs a multi cache with 17 initial caches.
	 */
	public MultiCache() {
		this(17);
	}
	/** Constructs a multi cache with the specified number of internal caches,
	 * the max size and the lifetime.
	 *
	 * @param nCache the positive number of the internal caches.
	 * The large the number the fast the performance.
	 * @param maxSize the maximal allowed size of each cache
	 */
	@SuppressWarnings("unchecked")
	public MultiCache(int nCache, int maxSize, int lifetime) {
		if (nCache <= 0)
			throw new IllegalArgumentException();
		_caches = new CacheMap[nCache];
		_maxsize = maxSize;
		_lifetime = lifetime;
	}
	/** Constructs a multi cache with the specified number of internal caches.
	 *
	 * <p>The default lifetime is {@link #DEFAULT_LIFETIME}, and
	 * the default maximal allowed size of each cache is
	 * ({@link #DEFAULT_MAX_SIZE} / 10).
	 *
	 * @param nCache the positive number of the internal caches.
	 * The large the number the fast the performance.
	 */
	public MultiCache(int nCache) {
		this(nCache, DEFAULT_MAX_SIZE / 10, DEFAULT_LIFETIME);
	}

	//Cache//
	public boolean containsKey(Object key) {
		final CacheMap<K, V> cache = getCache(key);
		synchronized (cache) {
			return cache.containsKey(key);
		}
	}
	public V get(Object key) {
		final CacheMap<K, V> cache = getCache(key);
		synchronized (cache) {
			return cache.get(key);
		}
	}
	public V put(K key, V value) {
		final CacheMap<K, V> cache = getCache(key);
		synchronized (cache) {
			return cache.put(key, value);
		}
	}
	public V remove(Object key) {
		final CacheMap<K, V> cache = getCache(key);
		synchronized (cache) {
			return cache.remove(key);
		}
	}
	public void clear() {
		synchronized (this) {
			for (int j = 0; j < _caches.length; ++j)
				_caches[j] = null;
		}
	}

	/** Returns an integer used to identify the instance of inner caches to use.
	 * By default, this method returns the hash code of the given key
	 * and the current thread.
	 * It means the value of the same key might be stored in different
	 * cache (in favor of performance).
	 * If different threads of your application usually access
	 * different keys, you can override this method to return
	 * the hash code of the given key only.
	 * @since 6.0.0
	 */
	protected int getInnerCacheHashCode(Object key) {
		return Objects.hashCode(key) ^ Objects.hashCode(Thread.currentThread());
	}
	private CacheMap<K, V> getCache(Object key) {
		int j = getInnerCacheHashCode(key);
		j = (j >= 0 ? j: -j) % _caches.length;

		CacheMap<K, V> cache = _caches[j];
		if (cache == null)
			synchronized (this) {
				cache = _caches[j];
				if (cache == null) {
					cache = new CacheMap<K, V>(4);
					cache.setMaxSize(_maxsize);
					cache.setLifetime(_lifetime);
					_caches[j] = cache;
				}
			}
		return cache;
	}

	public int getLifetime() {
		return _lifetime;
	}
	public void setLifetime(int lifetime) {
		_lifetime = lifetime;

		for (int j = 0; j < _caches.length; ++j)
			if (_caches[j] != null)
				synchronized (_caches[j]) {
					_caches[j].setLifetime(lifetime);
				}
	}
	public int getMaxSize() {
		return _maxsize;
	}
	public void setMaxSize(int maxsize) {
		_maxsize = maxsize;

		for (int j = 0; j < _caches.length; ++j)
			if (_caches[j] != null)
				synchronized (_caches[j]) {
					_caches[j].setMaxSize(maxsize);
				}
	}

	//Cloneable//
	public Object clone() {
		MultiCache clone = new MultiCache(_caches.length, _maxsize, _lifetime);
		for (int j = 0; j < _caches.length; ++j)
			if (_caches[j] != null)
				synchronized (_caches[j]) {
					clone._caches[j] = (CacheMap)_caches[j].clone();
				}
		return clone;
	}
}
