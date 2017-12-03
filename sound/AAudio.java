/* AAudio.java

	Purpose:
		
	Description:
		
	History:
		Wed Nov 16 15:15:40     2005, Created by tomyeh

Copyright (C) 2005 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under LGPL Version 2.1 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/
package org.zkoss.sound;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.Reader;
import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.zkoss.lang.SystemException;
import org.zkoss.util.media.ContentTypes;
import org.zkoss.io.NullInputStream;

/**
 * Represents an audio.
 * Unlike javax.sound.AudioClip, this class is used only to hold the raw
 * data as opaque rather than manipulate the sound.
 *
 * <p>In other words, it is used to retrieve and store the opaque data
 * as polymorphic thru the {@link org.zkoss.util.media.Media} interface.
 *
 * <p>AAudio is serializable, but, if you are using InputStream,
 * you have to extend this class, and provide the implementation to
 * serialize and deserialize {@link #_isdata}. (Since 5.0.11)
 * 
 * @author tomyeh
 */
public class AAudio implements Audio, java.io.Serializable {
	/** Used if you want to implement a media whose input stream is created
	 * dynamically each time {@link #getStreamData} is called.
	 * @see #AAudio(String,,InputStream)
	 */
	protected static final InputStream DYNAMIC_STREAM = new NullInputStream();

	/** The raw data in byte array.
	 * Exactly one of {@link #_data} and {@link #_isdata} is not null.
	 */
	private final byte[] _data;
	/** The raw data in stream (or {@link #DYNAMIC_STREAM}).
	 * Exactly one of {@link #_data} and {@link #_isdata} is not null.
	 */
	protected final transient InputStream _isdata;
	/** The URL of the data.
	 */
	private final URL _url;
	/** The file of the data.
	 */
	private final File _file;

	/** The format name, e.g., "jpeg", "gif" and "png". */
	private String _format;
	/** The content type. */
	private String _ctype;
	/** The name (usually filename). */
	private final String _name;

	public AAudio(String name, byte[] data) throws IOException {
		if (data == null)
			throw new IllegalArgumentException("null data");
		_name = name;
		_data = data;
		_isdata = null;
		_url = null;
		_file = null;
	}
	/**
	 * Creates an instance of an audio with an input stream.
	 * If the stream shall be created each time {@link #getStreamData} is called,
	 * you can pass {@link #DYNAMIC_STREAM} to the data argument, and then
	 * override {@link #getStreamData}.
	 *
	 * <p>Note: the caller of {@link #getStreamData} has to close
	 * the returned input stream.
	 */
	public AAudio(String name, InputStream isdata) throws IOException {
		if (isdata == null)
			throw new IllegalArgumentException("null stream");
		_name = name;
		_isdata = isdata;
		_data = null;
		_url = null;
		_file = null;
	}
	/** Constructs an audio with an URL.
	 */
	public AAudio(URL url) {
		if (url == null)
			throw new IllegalArgumentException("null url");
		_name = getName(url);
		_url = url;
		_isdata = DYNAMIC_STREAM;
		_data = null;
		_file = null;
	}
	/** Constructs an audio with a file.
	 */
	public AAudio(File file) {
		if (file == null)
			throw new IllegalArgumentException("null url");
		_name = file.getName();
		_file = file;
		_isdata = DYNAMIC_STREAM;
		_data = null;
		_url = null;
	}
	/** Constructs an audio with a file name.
	 */
	public AAudio(String filename) throws IOException {
		this(new File(filename));
	}
	/**
	 * Creates an instance of an audio with an input stream.
	 * If the stream shall be created each time {@link #getStreamData} is called,
	 * you can pass {@link #DYNAMIC_STREAM} to the data argument, and then
	 * override {@link #getStreamData}.
	 *
	 * <p>Note: the caller of {@link #getStreamData} has to close
	 * the returned input stream.
	 */
	public AAudio(InputStream is) throws IOException {
		this(null, is);
	}

	private static String getName(URL url) {
		String name = url.getPath();
		if (name != null) {
			{
				final int j = name.lastIndexOf(File.pathSeparatorChar);
				if (j >= 0) name = name.substring(j + 1);
			}
			if (File.pathSeparatorChar != '/') {
				final int j = name.lastIndexOf('/');
				if (j >= 0) name = name.substring(j + 1);
			}
		}
		return name;
	}
	private static String getContentType(String format) {
		final String ctype = ContentTypes.getContentType(format);
		return ctype != null ? ctype: "audio/" + format;
	}
	private static String getFormatByName(String name) {
		if (name != null) {
			final int j = name.lastIndexOf('.') + 1,
				k = name.lastIndexOf('/') + 1;
			if (j > k && j < name.length())
				return name.substring(j); 
		}
		return null;
	}

	//-- Media --//
	public final boolean isBinary() {
		return true;
	}
	public final boolean inMemory() {
		return _data != null;
	}
	public byte[] getByteData() {
		if (_data == null)
			throw new IllegalStateException("Use getStreamData() instead");
		return _data;
	}
	/** Always throws IllegalStateException.
	 */
	public final String getStringData() {
		throw newIllegalStateException();
	}
	/** Returns the data in the input stream.
	 *
	 * <p>Note: the caller has to invoke {@link InputStream#close}
	 * after using the input stream returned by {@link #getStreamData}.
	 */
	public InputStream getStreamData() {
		try {
			if (_url != null) {
				InputStream is = _url.openStream();
				return is != null ? new BufferedInputStream(is): null;
			}
			if (_file != null)
				return new BufferedInputStream(new FileInputStream(_file));
		} catch (java.io.IOException ex) {
			throw new SystemException("Unable to read "
				+(_url != null ? _url.toString(): _file.toString()), ex);
		}
		if (_isdata != null) return _isdata;
		return new ByteArrayInputStream(_data);
	}
	/** Not supported. It always throws IllegalStateException.
	 */
	public final Reader getReaderData() {
		throw newIllegalStateException();
	}
	private final IllegalStateException newIllegalStateException() {
		return new IllegalStateException(
			_isdata != null ? "Use getStreamData() instead":
				"Use getByteData() instead");
	}

	public final String getName() {
		return _name;
	}
	public String getFormat() {
		if (_format == null) {
			try {
				_format = getFormat0();
			} catch (IOException ex) {
				throw new SystemException("Unable to read", ex);
			}
		}
		return _format;
	}
	
	private String getFormat0() throws IOException {
		String format = null;
		if (_data != null) {
			try {
				format =
					AudioSystem.getAudioFileFormat(new ByteArrayInputStream(_data))
					.getType().getExtension();
			} catch (UnsupportedAudioFileException ex) {
				format = getFormatByName(_name);
				if (format == null)
					throw (IOException)new IOException().initCause(ex);
			}
		} else if (_file != null) {
			try {
				format = AudioSystem.getAudioFileFormat(_file)
					.getType().getExtension();
			} catch (UnsupportedAudioFileException ex) {
				format = getFormatByName(_name);
				if (format == null)
					throw (IOException)new IOException().initCause(ex);
			}
		} else if (_url != null) {
			try {
				format = AudioSystem.getAudioFileFormat(_url)
					.getType().getExtension();
			} catch (UnsupportedAudioFileException ex) {
				format = getFormatByName(_name);
				if (format == null)
					throw (IOException)new IOException().initCause(ex);
			}
		} else {
			try {
				format = AudioSystem.getAudioFileFormat(
						_isdata == DYNAMIC_STREAM ? getStreamData(): _isdata)
					.getType().getExtension();
			} catch (UnsupportedAudioFileException ex) {
				format = getFormatByName(_name);
				if (format == null)
					throw (IOException)new IOException().initCause(ex);
			}
		}
		return format;
	}
	public String getContentType() {
		if (_ctype == null) {
			_ctype = getContentType(getFormat());
		}
		return _ctype;
	}
	public boolean isContentDisposition() {
		return true;
	}
}
