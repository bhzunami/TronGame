/*
 * Copyright (c) 2013 - 2016 Stefan Muller Arisona, Simon Schubiger
 * Copyright (c) 2013 - 2016 FHNW & ETH Zurich
 * All rights reserved.
 *
 * Contributions by: Filip Schramka, Samuel von Stachelski
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *  Neither the name of FHNW / ETH Zurich nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package ch.fhnw.util;

import java.io.File;
import java.lang.ref.Reference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;

public final class TextUtilities {
	private static final DateFormat GMT_FORMATTER         = new SimpleDateFormat("d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
	private static final DateFormat DMY_FORMATTER         = new SimpleDateFormat("d MMM yyyy",   Locale.US);
	private static final DateFormat COMPACT_FORMATTER     = new SimpleDateFormat("yyyyMMddHHmm", Locale.US);
	private static final DateFormat COMPACT_SPC_FORMATTER = new SimpleDateFormat("dd-MMM-yyyy HH:mm");
	public  static boolean          DUMP_IDX              = false;
	public  static final String     AZ                    = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	public  static final String     az                    = "abcdefghijklmnopqrstuvwxyz";
	public  static final String     AZaz                  = AZ + az;
	public  static final String     NUMBERS               = "0123456789";
	public  static final String		DASHDOT				  = "_-.";
	public  static final String		WHITESPACE			  = " ";
	public  static final String     EMAIL_LOCAL_PART      = AZaz + NUMBERS + "!#$%&'*+-/=?^_`{|}~.";
	public  static final String     DOMAIN                = ClassUtilities.EMPTY_String;

	public static String cat(String[] strings, char catchar) {
		return cat(strings, 0, strings.length, catchar);
	}

	public static String cat(String[] strings, int start, char catchar) {
		return cat(strings, start, strings.length - start, catchar);
	}

	public static String cat(String[] strings, int start, int count, char catchar) {
		String result = ClassUtilities.EMPTY_String;
		for(int i = start; i < start + count; i++)
			result += (i != 0 ? ClassUtilities.EMPTY_String + catchar : ClassUtilities.EMPTY_String) + strings[i];
		return result;
	}

	static {
		GMT_FORMATTER.setTimeZone(TimeZone.getTimeZone("GMT"));
		COMPACT_FORMATTER.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	public static String toGMTString(Date date) {
		synchronized(GMT_FORMATTER) {
			return GMT_FORMATTER.format(date);
		}
	}

	public static String toDayMonthYearString(Date date) {
		synchronized(DMY_FORMATTER) {
			return DMY_FORMATTER.format(date);
		}
	}

	public static String toCompactNumberString(Number number) {
		if(number.longValue() == number.doubleValue())
			return Long.toString(number.longValue());
		return number.toString();
	}

	public static String toCompactDateString(Date date) {
		synchronized(COMPACT_FORMATTER) {
			return COMPACT_FORMATTER.format(date);
		}
	}

	public static String toCompactDateString2(Date date) {
		synchronized(COMPACT_SPC_FORMATTER) {
			return COMPACT_SPC_FORMATTER.format(date);
		}
	}

	public static Date fromGMTString(String string) throws ParseException {
		synchronized(GMT_FORMATTER) {
			return GMT_FORMATTER.parse(string);
		}
	}

	static DecimalFormat[] DECIMAL_FORMATS_FIX = new DecimalFormat[0];
	static DecimalFormat[] DECIMAL_FORMATS = new DecimalFormat[0];


	public static DecimalFormat decimalFormat(int numDecimals) {
		if(DECIMAL_FORMATS_FIX.length < numDecimals + 1) {
			for(int i = DECIMAL_FORMATS_FIX.length; i < numDecimals + 1; i++) {
				DecimalFormatSymbols symbols = new DecimalFormatSymbols();
				symbols.setDecimalSeparator('.');
				DecimalFormat format = new DecimalFormat(i == 0 ? "0" : "0." + TextUtilities.repeat("0", i), symbols);
				DECIMAL_FORMATS_FIX = ArrayUtilities.append(DECIMAL_FORMATS_FIX, format);
			}
		}
		return DECIMAL_FORMATS_FIX[numDecimals];
	}

	public static DecimalFormat decimalFormat(int numDecimals, String NaN) {
		if(DECIMAL_FORMATS.length < numDecimals + 1) {
			for(int i = DECIMAL_FORMATS.length; i < numDecimals + 1; i++) {
				DecimalFormatSymbols symbols = new DecimalFormatSymbols();
				symbols.setDecimalSeparator('.');
				symbols.setNaN( NaN );
				DecimalFormat format = new DecimalFormat(i == 0 ? "0" : "0." + TextUtilities.repeat("#", i), symbols);
				DECIMAL_FORMATS = ArrayUtilities.append(DECIMAL_FORMATS, format);
			}
		}
		return DECIMAL_FORMATS[numDecimals];
	}

	public static String repeat(String str, int n) {
		StringBuilder result =  new StringBuilder();
		while(--n >= 0)
			result.append(str);
		return result.toString();
	}

	private static SortedLongMap<String> repeatMap = new SortedLongMap<>();
	public static String repeat(char ch, int n) {
		if(n == 0) return ClassUtilities.EMPTY_String;
		long key = ch;
		key <<= 32;
		key |= n;
		String result = repeatMap.get(key);
		if(result == null) {
			StringBuilder tmp =  new StringBuilder();
			while(--n >= 0)
				tmp.append(ch);
			result = tmp.toString();
			repeatMap.put(key, result);
		}
		return result;
	}

	public static void dump(String prefix, byte ... msg) {
		System.out.println(toDumpString(prefix, msg));
	}

	public static String toDumpString(String prefix, byte ... msg) {
		if(msg == null) msg = new byte[0];
		int len = Math.min(msg.length, 1024);
		StringBuilder result = new StringBuilder('(' + len + "/" + msg.length + ")");
		if(len <= 4) {
			for(int i = 0; i < len; i++)
				result.append(byteToHex(msg[i]) + ":" + (isPrintable(msg[i] & 0xFF) ? (char)(msg[i] & 0xFF) : ' ') + " ");
			return result.toString();
		}
		int prefixLen = result.length();
		for(int i = 0; i < len; i++)
			result.append((DUMP_IDX ? "[" + i + "]" : " ") + byteToHex(msg[i]));
		result.append("\n");
		for(int i = 0; i <= prefixLen; i++)
			result.append(' ');
		for(int i = 0; i < len; i++)
			result.append((isPrintable(msg[i] & 0xFF) ? (char)(msg[i] & 0xFF) : ' ') + "  ");

		return result.append('\n').toString();
	}

	static public final String HEXTAB   = "0123456789ABCDEF";
	static public final String HEXCHARS = HEXTAB + "abcdef";

	public static String byteToHex(int b) {
		return HEXTAB.charAt((b & 0xF0) >> 4) + ClassUtilities.EMPTY_String + HEXTAB.charAt(b & 0xF);
	}

	public static String shortToHex(int i) {
		return byteToHex(i >> 8) + byteToHex(i);
	}

	public static String intToHex(int i) {
		return byteToHex(i >> 24) + byteToHex(i >> 16) + byteToHex(i >> 8) + byteToHex(i);
	}

	public static String intToBits(int i, int bitcount) {
		StringBuilder result = new StringBuilder();
		while(--bitcount >= 0)
			result.append((i & (1<<bitcount)) == 0 ?'0' : '1');
		return result.toString();
	}

	public static String charToHex(char c) {
		return byteToHex(c >> 8) + byteToHex(c);
	}

	public static String longToHex(long i) {
		return intToHex((int)(i >> 32)) + intToHex((int)i);
	}


	public static String toHex(byte[] buffer) {
		return toHex(buffer, 0, buffer.length, 0);
	}

	public static String toHex(byte[] buffer, int off, int len) {
		return toHex(buffer, off, len, 0);
	}

	public static String toHex(byte[] buffer, int off, int len, int split) {
		return toHex(buffer, off, len, split, 0);
	}

	public static String toHex(byte[] buffer, int off, int len, int split, int newline) {
		StringBuffer result = new StringBuffer();
		for(int i = 0; i <len; i++) {
			if(split   != 0 && i != 0 && i % split   == 0) result.append(' ');
			if(newline != 0 && i != 0 && i % newline == 0) result.append('\n');
			result.append(HEXTAB.charAt((buffer[i + off] >> 4) & 0xF));
			result.append(HEXTAB.charAt(buffer[i + off] & 0xF));
		}
		return result.toString();
	}

	public static byte[] fromHexString(String hex) {
		ByteList result = new ByteList(hex.length() / 2);
		byte     curr   = 0;
		boolean  high   = true;
		for(int i = 0; i < hex.length(); i++) {
			int val;
			switch(hex.charAt(i)) {
			case '0': val = 0x0; break;
			case '1': val = 0x1; break;
			case '2': val = 0x2; break;
			case '3': val = 0x3; break;
			case '4': val = 0x4; break;
			case '5': val = 0x5; break;
			case '6': val = 0x6; break;
			case '7': val = 0x7; break;
			case '8': val = 0x8; break;
			case '9': val = 0x9; break;
			case 'a': val = 0xa; break;
			case 'b': val = 0xb; break;
			case 'c': val = 0xc; break;
			case 'd': val = 0xd; break;
			case 'e': val = 0xe; break;
			case 'f': val = 0xf; break;
			case 'A': val = 0xA; break;
			case 'B': val = 0xB; break;
			case 'C': val = 0xC; break;
			case 'D': val = 0xD; break;
			case 'E': val = 0xE; break;
			case 'F': val = 0xF; break;
			default:             continue;
			}
			if(high) {
				curr = (byte) (val << 4);
				high = false;
			} else {
				curr |= val;
				result.add(curr);
				high = true;
			}
		}
		return result.toArray();
	}

	static boolean isPrintable(int c) {
		return c > 32 && c < 256 && c != 127;
	}

	private static String escape(String s) {
		return s.replace("\\", "\\\\");
	}

	public static String pack(String[] items) {
		StringBuilder result = new StringBuilder();

		for(String i : items)
			result.append(escape(i)).append("\\;");

		return result.toString();
	}

	public static String[] unpack(String stringList) {
		if(stringList == null) return null;

		ArrayList<String> result = new ArrayList<>();
		StringBuilder     item   = new StringBuilder();

		for(int i = 0; i < stringList.length(); i++) {
			char c = stringList.charAt(i);
			if(c == '\\') {
				i++;
				c = stringList.charAt(i);
				if(c == '\\')
					item.append('\\');
				else if(c == ';') {
					result.add(item.toString());
					item.setLength(0);
				}
			} else
				item.append(c);
		}
		return result.toArray(new String[result.size()]);
	}

	/**
	 * Remove all occurrences of the characters in <code>filter</code>.
	 * 
	 * @param string
	 *            The source string.
	 * @param filter
	 *            The list of chars to remove as string.
	 * @return The source string with all occurrences of the characters in <code>filter</code> removed.
	 */
	public static String remove(String string, String filter) {
		StringBuilder result = new StringBuilder();
		for(int i = 0; i < string.length(); i++)
			if(filter.indexOf(string.charAt(i)) == -1)
				result.append(string.charAt(i));

		return result.toString();
	}

	/**
	 * Return only the chars contained in <code>filter</code>.
	 * 
	 * @param string
	 *            The source string.
	 * @param allowed
	 *            The list of chars that will pass the filter.
	 * @return The source string consisting only of chars from <code>filter</code>.
	 */
	public static String filter(String string, String allowed) {
		StringBuilder result = new StringBuilder();
		for(int i = 0; i < string.length(); i++)
			if(allowed.indexOf(string.charAt(i)) >= 0)
				result.append(string.charAt(i));

		return result.toString();
	}

	/**
	 * Replace all chars contained in <code>invalidChars</code>.
	 * 
	 * @param string
	 *            The source string
	 * @param invalidChars
	 *            The list of chars to replace
	 * @param c
	 *            The char to insert
	 * @return The new string
	 */
	public static String replace(String string, String invalidChars, char c) {
		StringBuilder result = new StringBuilder();
		for(int i = 0; i < string.length(); i++) {
			int idx = invalidChars.indexOf(string.charAt(i));
			if(idx < 0) {
				result.append(string.charAt(i));
			} else {
				result.append(c);
			}
		}
		return result.toString();
	}

	public static String urlDecodeUTF8(String s) {
		try {
			return URLDecoder.decode(s, "UTF-8");
		} catch(Exception ex) {
			return s;
		}
	}
	
	public static String[] split(String str, char splitchar) {
		if(str          == null) return ClassUtilities.EMPTY_StringA;
		if(str.length() == 0)    return new String[] {ClassUtilities.EMPTY_String};

		final int len   = str.length();
		int       count = 1 + count(str, splitchar);

		String[] result = new String[count];

		count     = 0;
		int start = 0;
		for(int i = 0; i < len; i++)
			if(str.charAt(i) == splitchar) {
				result[count++] = start == i ? ClassUtilities.EMPTY_String : str.substring(start, i);
				start = i + 1;
			}
		result[count] = str.substring(start, len);

		int dropCount = 0;
		while(count >= 0 && result[count].length() == 0) {
			dropCount++;
			count--;
		}
		if(dropCount > 0)
			result = ArrayUtilities.dropLast(dropCount, result);

		return result;
	}

	public static String[] tokens(String str) {
		if(str          == null) return ClassUtilities.EMPTY_StringA;
		if(str.length() == 0)    return new String[] {ClassUtilities.EMPTY_String};
		String[] result = new String[(str.length() + 1) / 2];
		int      len    = str.length();
		int      start  = 0;
		int      count  = 0;
		boolean  ws     = Character.isWhitespace(str.charAt(0));
		for(int i = 0; i < len; i++) {
			if(ws) {
				if(Character.isWhitespace(str.charAt(i)))
					start++;
				else
					ws = false;
			} else {
				if(Character.isWhitespace(str.charAt(i))) {
					result[count++] = str.substring(start, i);
					start = i + 1;
					ws    = true;
				}
			}
		}
		if(!ws && start < str.length())
			result[count++] = str.substring(start);

		return Arrays.copyOf(result, count);
	}

	public static byte[] paddedCString(String str, int size) {
		byte[] result = new byte[size];
		if(str.length() > size - 1)
			str = str.substring(0, size - 1);
		System.arraycopy(str.getBytes(), 0, result, 0, str.length());
		return result;
	}

	public static String fromCString(byte[] data, int off) {
		if(data == null) return null;
		int len = 0;
		while(data[off + len] != 0)
			len++;
		return new String(data, off, len);
	}

	public static byte[] toCString(String str) {
		if(str == null) return null;
		byte[] result = new byte[str.length() + 1];
		for (int i = 0; i < str.length(); i++)
			result[i] = (byte) str.charAt(i);
		result[str.length()] = 0;
		return result;
	}

	public static String prefix(int value, int count, char prefix) {
		return prefix(String.valueOf(value), count, prefix);
	}

	public static String prefix(String result, int count, char prefix) {
		while(result.length() < count)
			result = prefix + result;
		return result;
	}

	public static final String ID_FILTER        = AZaz + NUMBERS + "_."; 
	public static final String FILENAME_FILTER  = AZaz + NUMBERS + DASHDOT; 
	public static final String PATH_FILTER_NOWS = ":/\\" + AZaz + NUMBERS + "_-."; 
	public static final String PATH_FILTER      = " " + PATH_FILTER_NOWS; 

	public static String cleanForFilename(String name) {
		return filter(name, FILENAME_FILTER);
	}

	public static String cleanForPath(String path) {
		return filter(path, PATH_FILTER);
	}

	public static String cleanForId(String text) {
		String result = filter(text.replace('/', '.').replace('\\','.'), ID_FILTER);
		if(result.length() == 0)
			return "_";
		return Character.isDigit(result.charAt(0)) ? '_' + result : result;
	}

	public static String cleanForIdNoDots(String text) {
		String result = filter(text.replace('/', '_').replace('\\','_'), ID_FILTER).replace('.', '_');
		if(result.length() == 0)
			return "_";
		return Character.isDigit(result.charAt(0)) ? '_' + result : result;
	}

	public static boolean isValidPath(String path) {
		return cleanForPath(path).equals(path);
	}

	public static String stripFileExtension(String path) {
		if(path == null) return null;
		int idx = path.lastIndexOf('.');
		return idx > 0 ? path.substring(0, idx) : path;
	}
	
	public static String getFileName(String path) {
		return path.substring(path.replace(File.separatorChar, '/').lastIndexOf('/') + 1, path.length());
	}

	public static String getParentPath(String path) {
		return path.substring(0, path.replace(File.separatorChar, '/').lastIndexOf('/'));
	}

	public static String getParentPathAndName(String path, StringBuilder parent) {
		final int  len = path.length();
		final char sep = File.separatorChar;
		int   end      = 0;
		for(int i = 0; i < len; i++) {
			char ch = path.charAt(i);
			if(ch == sep) {
				ch  = '/';
				end = i;
			} else if(ch == '/') 
				end = i;
			parent.append(ch);
		}
		parent.setLength(end);
		return path.substring(end + 1);
	}
	
	public static String getFileNameWithoutExtension(String path) {
		return stripFileExtension(getFileName(path));
	}

	public static String getFileNameWithoutExtension(File file) {
		return stripFileExtension(file.getName());
	}

	public static String getFileExtensionWithoutDot(File file) {
		return getFileExtensionWithoutDot(file.getName());
	}
	
	public static String getFileExtensionWithoutDot(String name) {
		int idx = name.lastIndexOf('.');
		return idx > 0 ? name.substring(idx + 1) : ClassUtilities.EMPTY_String;
	}

	public static boolean hasFileExtension(String name, String ... extensions) {
		if(name == null)
			return false;
		for(String extension : extensions) {
			if(!extension.startsWith("."))
				extension = "." + extension;
			if(name.toLowerCase().endsWith(extension))
				return true;
		}
		return false;
	}

	public static boolean hasFileExtension(File file, String ... extensions) {
		return hasFileExtension(file.getName(), extensions);
	}

	public static int count(String str, char ch) {
		int result = 0;
		for(int i = str.length(); --i >= 0;)
			if(str.charAt(i) == ch)
				result++;
		return result;
	}

	public static int count(String string, String sub) {
		int start = 0;
		int end   = string.length();
		int n     = sub.length();
		if(n == 0)
			return 0;
		//return end - start + 1;

		int result = 0;
		while(true){
			int index = string.indexOf(sub, start);
			start = index + n;
			if(start > end || index == -1)
				break;
			result++;
		}
		return result;
	}

	public static int toInt(String string) {
		if(string.length() != 4) throw new IllegalArgumentException("length must be 4, got \"" + string + "\", length=" + string.length());
		byte[] bytes = string.getBytes();
		return ((bytes[0] & 0xFF) << 24) | ((bytes[1] & 0xFF) << 16) | ((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF);
	}

	public static String toString(Object o) {
		if(o instanceof URL) {
			String result = urlDecodeUTF8(((URL)o).toExternalForm());
			if(result.startsWith("file:/") && result.charAt(6) != '/')
				result = result.replace("file:/", "file:///");
			return result;
		}
		return toString("[", ", ", "]", o);
	}

	public static final int NONE            = 0;
	public static final int QUOTE_STRINGS   = 1;
	public static final int FORMAT_NUMBERS  = 2;
	public static final int OBJECT_BY_FIELD = 4;
	public static final int INDENT          = 8;

	public static String toString(String gOpen, String gSep, String gClose, Object o) {
		return toString(gOpen, gSep, gClose, o, QUOTE_STRINGS);
	}

	public static String toString(String gOpen, String gSep, String gClose, Object array, int flags, int offset, int length) {
		StringBuilder result = new StringBuilder(gOpen);
		for(int i = 0; i < length; i++)
			result.append((i == 0 ? ClassUtilities.EMPTY_String : gSep)).append(toString(gOpen, gSep, gClose, Array.get(array, offset + i), flags));
		return result.append(gClose).toString();
	}

	public static String toString(String gOpen, String gSep, String gClose, Iterable<?> iterable, int flags, int offset, int length) {
		StringBuilder result = new StringBuilder(gOpen);
		int           i      = 0;
		for(Object o : iterable) {
			if(i >= offset + length)
				break;
			if(i >= offset)
				result.append((i == offset ? ClassUtilities.EMPTY_String : gSep)).append(toString(gOpen, gSep, gClose, o, flags));
			i++;
		}
		return result.append(gClose).toString();
	}

	public static String toString(String gOpen, String gSep, String gClose, Object o, int flags) {
		return toString(gOpen, gSep, gClose, o, flags, ClassUtilities.EMPTY_String, new IdentityHashSet<>());
	}

	private static String toString(String gOpen, String gSep, String gClose, Object o, int flags, String prefix, Set<Object> visited) {
		if(o == null) return "<null>";
		else if(!ClassUtilities.isPrimitiveOrWrapper(o.getClass()) && !visited.add(o))
			return "<cycle>";
		else if(o instanceof Throwable) {
			return toString((Throwable)o, new StringBuilder()).toString();
		}
		else if(o instanceof Reference<?>)
			return "Ref:" + toString(gOpen, gSep, gClose, ((Reference<?>)o).get(), flags, prefix, visited);
		else if(o instanceof String)
			return (flags & QUOTE_STRINGS) != 0 ? "\"" + o + "\"" : o.toString();
		else if(o.getClass().isArray()) {
			return toString(gOpen, gSep, gClose, o, flags, 0, Array.getLength(o));
		} else if(o instanceof Iterable<?>) {
			StringBuilder result = new StringBuilder(gOpen);
			int i = 0;
			for(Object item : ((Iterable<?>)o))
				result.append((i++ == 0 ? ClassUtilities.EMPTY_String : gSep) + toString(gOpen, gSep, gClose, item, flags, prefix, visited));				
			return result.append(gClose).toString();
		} else if(o instanceof Map<?, ?>) {
			Map<?,?> m = (Map<?,?>)o;
			StringBuilder result = new StringBuilder(gOpen);
			int i = 0;
			for(Entry<?,?> entry : m.entrySet())
				result.append((i++ == 0 ? ClassUtilities.EMPTY_String : gSep) + toString(gOpen, gSep, gClose, entry.getKey(), flags) + "=" + toString(gOpen, gSep, gClose, entry.getValue(), flags, prefix, visited));
			return result.append(gClose).toString();
		} else if((flags & FORMAT_NUMBERS) != 0 && o instanceof Number) 
			return new DecimalFormat(ClassUtilities.isIntegral(o.getClass()) ? "#,##0" : "#,##0.0000").format(((Number)o).doubleValue());
		if((flags & OBJECT_BY_FIELD) != 0) {
			if(ClassUtilities.isPrimitiveOrWrapper(o.getClass()))
				return o.toString();
			else if(o instanceof String ||
					o instanceof StringBuilder ||
					o instanceof StringBuffer)
				return o.toString();
			StringBuilder result = new StringBuilder(getShortClassName(o));
			result.append('{');
			prefix += "  ";
			for(Field f : ClassUtilities.getAllFields(o.getClass())) {
				if((f.getModifiers() & Modifier.STATIC) != 0)
					continue;
				if((flags & INDENT) != 0) {
					result.append('\n');
					result.append(prefix);
				}
				result.append(f.getName()).append(':');
				try {
					result.append(toString(gOpen, gSep, gClose, f.get(o), flags, prefix, visited));
				} catch(Throwable t) {
					result.append("<???>");
				}	
				result.append(',');
			}
			if((flags & INDENT) != 0) {
				result.append('\n');
				result.append(prefix.substring(0, prefix.length() - 2));
			}
			result.append('}');
			return result.toString();
		}
		return o.toString();
	}

	private static StringBuilder toString(Throwable t, StringBuilder result) {
		if(t.getMessage() != null) {
			result.append(t.getMessage());
			result.append(" (");
		} else {
			String name = t.getClass().getName();
			name        = name.substring(name.indexOf('.') + 1);
			for(int i = 1; i < name.length(); i++) {
				result.append(name.charAt(i - 1));
				if(         Character.isLowerCase(name.charAt(i - 1)) 
						&& (Character.isUpperCase(name.charAt(i)) || Character.isDigit(name.charAt(i))))
					result.append(' ');
			}
			result.append(name.charAt(name.length() - 1));
		}
		if(t.getMessage() != null)
			result.append(")");
		if(t.getCause() != null) {
			result.append('/');
			result.append(toString(t.getCause(), result));
		}
		return result;
	}

	public static BitSet bitSet(String string) {
		BitSet result = new BitSet();
		for(int i = string.length(); --i >= 0;)
			result.set(string.charAt(i));
		return result;
	}

	public static String makeUniqueByNumber(String prefix, String[] names) {
		return makeUniqueByNumber(prefix, names, false);
	}

	public static String makeUniqueByNumber(String prefix, String[] names, boolean incrementOnlyIfNotUnique) {
		Set<String> nameSet = new HashSet<>();
		CollectionUtilities.addAll(nameSet, names);
		return makeUniqueByNumber(prefix, ClassUtilities.EMPTY_String, nameSet, incrementOnlyIfNotUnique);
	}	

	public static String makeUniqueByNumber(String prefix, String infix, Set<String> names) {
		return makeUniqueByNumber(prefix, infix, names, false);
	}

	public static String makeUniqueByNumber(String prefix, Set<String> names, boolean incrementOnlyIfNotUnique) {
		return makeUniqueByNumber(prefix, ClassUtilities.EMPTY_String, names, incrementOnlyIfNotUnique);
	}

	public static String makeUniqueByNumber(String prefix, String infix, Set<String> names, boolean incrementOnlyIfNotUnique) {
		while(prefix.length() > 0 && Character.isDigit(prefix.charAt(prefix.length() - 1)))
			prefix = prefix.substring(0, prefix.length() - 1);
		if(infix.length() > 0 && prefix.endsWith(infix))
			prefix = prefix.substring(0, prefix.length() - infix.length());
		prefix += infix;
		if(incrementOnlyIfNotUnique)
			if(!names.contains(prefix))
				return prefix;
		for(int i = 1; ; i++) {
			String result = prefix + i;
			if(!names.contains(result))
				return result;
		}
	}	

	/**
	 * Formats the given string with the given argument.
	 *
	 * @param message the message to format, must not be <code>null</code>
	 * @param arguments the arguments used to format the string
	 * @return the formatted string
	 */
	public static String format(String message, Object ... arguments) {
		return MessageFormat.format(message, arguments);
	}

	/**
	 * Escapes the standard C/Java escapes such as "\n", "\t" etc.
	 * 
	 * @param string The string with escapes
	 * @return The escaped strings
	 */
	public static String escapeAsSourceString(String string) {
		StringBuilder result = new StringBuilder();
		for(int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			switch(c) {
			case '\0' : result.append("\\0");  break;
			case '\n' : result.append("\\n");  break;
			case '\r' : result.append("\\r");  break;
			case '\t' : result.append("\\t");  break;
			case '\b' : result.append("\\b");  break;
			case '\f' : result.append("\\f");  break;
			case '\\' : result.append("\\\\"); break;
			case '"'  : result.append("\\\""); break;
			case '\'' : result.append("\\'"); break;
			default:
				if(c < ' ')
					result.append("\\u").append(TextUtilities.charToHex(c));
				else if(c > 127)
					result.append("\\u").append(TextUtilities.charToHex(c));
				else
					result.append(c);
			}
		}
		return result.toString();
	}

	/**
	 * Un-escapes the standard C/Java escapes such as "\n", "\t" etc.
	 * 
	 * @param string The string with escapes
	 * @return The un-escaped strings
	 */
	public static String unescapeAsSourceString(String string) {
		if(string.indexOf('\\') >= 0) {
			StringBuilder result = new StringBuilder();
			for(int i = 0; i < string.length(); i++)
				if(string.charAt(i) == '\\') {
					i++;
					switch(string.charAt(i)) {
					case '0':  result.append('\0'); break;
					case 'n':  result.append('\n'); break;
					case 'r':  result.append('\r'); break;
					case 't':  result.append('\t'); break;
					case 'b':  result.append('\b'); break;
					case 'f':  result.append('\f'); break;
					case 'u':
						String hex = ClassUtilities.EMPTY_String;
						hex += string.charAt(++i);
						hex += string.charAt(++i);
						hex += string.charAt(++i);
						hex += string.charAt(++i);
						result.append((char)Integer.parseInt(hex, 16));
						break;
					default:   
						result.append(string.charAt(i));    break;
					}
				} else
					result.append(string.charAt(i));
			return result.toString();
		}
		return string;
	}

	public static String commonPrefix(String s1, String s2) {
		int maxLen = Math.min(s1.length(), s2.length());
		StringBuilder result = new StringBuilder(maxLen);
		for(int i = 0; i < maxLen; i++)
			if(s1.charAt(i) == s2.charAt(i))
				result.append(s1.charAt(i));
			else
				break;
		return result.toString();
	}

	public static String ensureExtension(String fileName, String extension) {
		if(!extension.startsWith(".")) extension = "." + extension;
		return hasFileExtension(fileName, extension) ? fileName : fileName + extension;			
	}

	private static final Map<String, String> name2Key = new HashMap<>();

	public static String cleanForPrefsKey(String name) {
		String result = name2Key.get(name);
		if(result == null) {
			StringBuilder tresult = new StringBuilder();
			for(byte b : name.getBytes()) {
				int val = b & 0xFF;
				tresult.append((char)('A' + (val >> 4)));
			}	
			result = tresult.toString();
			name2Key.put(name, result);
		}
		return result;
	}

	public static String cleanForEmail(String email) {
		String[] parts = split(email, '@');
		if(parts.length != 2)
			return null;
		parts[0] = cleanForEmailLocal(parts[0]);
		parts[1] = cleanForDomain(parts[1]);
		if(parts[0] == null || parts[1] == null)
			return null;
		return parts[0] + "@" + parts[1];
	}

	private static String cleanForDomain(String string) {
		StringBuilder  result = new StringBuilder();
		boolean first  = true;
		for(String part : split(string, '.')) {
			result.append((first ? ClassUtilities.EMPTY_String : ".")).append(cleanPart(part));
			first = false;
		}
		return result.toString();
	}

	private static String cleanPart(String part) {
		StringBuilder result = new StringBuilder();
		for(int i = 0; i < part.length(); i++) {
			char c = part.charAt(i);
			if(Character.isLetterOrDigit(c))
				result.append(c);
			else if(c == '-')
				result.append(c);
		}
		return result.toString();
	}

	private static String cleanForEmailLocal(String string) {
		String result = filter(string, EMAIL_LOCAL_PART);
		if(result.startsWith(".")) 
			result = result.substring(1);
		result = result.replace("..", ".");
		return result;
	}

	public static int indexOf(String string, String ... strs) {
		int result = -1;
		for(String str : strs) {
			int idx = string.indexOf(str);
			if(idx >= 0)
				result = result < 0 ? idx : Math.min(result, idx);
		}
		return result;
	}

	public static String firstLetterUppercase(String text) {
		return text == null || text.length() == 0 ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);

	}

	public static StringBuilder set(StringBuilder result, String string) {
		result.setLength(0);
		result.append(string);
		return result;
	}

	public static StringBuilder set(StringBuilder result, StringBuilder string) {
		result.setLength(0);
		result.append(string);
		return result;
	}

	public static String[] makeUniqueByNumber(String[] result, char delimiter) {
		return makeUniqueByNumber( result, delimiter, false );
	}

	public static String[] makeUniqueByNumber(String[] result, char delimiter, boolean ignoreCase ) {
		Set<String>          sset     = new HashSet<>(result.length);
		Map<String, Integer> counters = new HashMap<>();
		for(int k = 0; k < result.length; k++) {
			String s = result[k];
			if( !sset.add( ignoreCase ? s.toLowerCase() : s ) ){
				StringBuilder sb   = new StringBuilder(s);
				int           dIdx = s.lastIndexOf(delimiter) + 1;
				if(dIdx <= 0) {
					sb.append(delimiter);
					dIdx = sb.length();
				} else {
					for(int i = dIdx; i < sb.length(); i++)
						if(!Character.isDigit(sb.charAt(i))) {
							sb.append(delimiter);
							dIdx = sb.length();
							break;
						}
				}
				sb.setLength(dIdx);
				String  ckey    = sb.toString();
				Integer counter = counters.get(ckey); 
				int     c       = counter == null ? 1 : counter.intValue();
				do {
					sb.setLength(dIdx);
					sb.append(c++);
					s = sb.toString();
				} while( !sset.add( ignoreCase ? s.toLowerCase() : s ) );
				result[k] = s;
				counters.put(ckey, Integer.valueOf(c));
			}
		}
		return result;
	}

	public static ArrayList<String> makeUniqueByNumber(ArrayList<String> result, char delimiter) {
		String[] r = result.toArray(new String[result.size()]);
		r = makeUniqueByNumber(r, delimiter);
		return new ArrayList<>(Arrays.asList(r));
	}

	public static String toMemSize(long size) {
		if(size > 1024 * 1024 * 1024)
			return decimalFormat(3).format(size / (1024.0 * 1024.0 * 1024.0)) + " GB";
		else if(size > 1024 * 1024) 
			return decimalFormat(3).format(size / (1024.0 * 1024.0)) + " MB";
		else if(size > 1024) 
			return decimalFormat(3).format(size / 1024.0) + " kB";
		return Long.toString(size) + "B";
	}

	public static boolean isNumber(String string) {
		try {
			Double.parseDouble(string);
			return true;
		} catch(Throwable t) {
			return false;
		}
	}

	/**
	 * Returns a short class name for the specified class. This method will omit the package name. For example, it will
	 * return "String" instead of "java.lang.String" for a {@link String} object. It will also name array according Java
	 * language usage, for example "double[]" instead of "[D".
	 *
	 * @param cls
	 *            The object class (may be {@code null}).
	 * @return A short class name for the specified object.
	 */
	public static String getShortName(Class<?> cls) {
		if (cls == null) {
			return "<*>";
		}
		String name = cls.getSimpleName();
		Class<?> enclosing = cls.getEnclosingClass();
		if (enclosing != null) {
			final StringBuilder buffer = new StringBuilder();
			do {
				buffer.insert(0, '.').insert(0, enclosing.getSimpleName());
			} while ((enclosing = enclosing.getEnclosingClass()) != null);
			name = buffer.append(name).toString();
		}
		return name;
	}

	/**
	 * Returns a short class name for the specified object. This method will omit the package name. For example, it will
	 * return "String" instead of "java.lang.String" for a {@link String} object.
	 *
	 * @param object
	 *            The object (may be {@code null}).
	 * @return A short class name for the specified object.
	 */
	public static String getShortClassName(final Object object) {
		return getShortName(ClassUtilities.getClass(object));
	}

	public static String singleQuote(String s) {
		return "'" + s + "'";
	}

	public static String doubleQuote(String s) {
		return "\"" + s + "\"";
	}


	private static final long K = 1024;
	private static final long M = 1024 * K;
	private static final long G = 1024 * M;
	public static String formatSize(long bytes, String b, String kb, String mb, String gb) {
		if(bytes > 16L * G)
			return (bytes / G) + gb;
		else if(bytes > 16L * M)
			return (bytes / M) + mb;
		else if(bytes > 16L * K)
			return (bytes / K) + kb;
		else
			return bytes + b;
	}

	public static String formatDuration(double durationInSeconds, String s, String m, String h) {
		if(durationInSeconds > 120.0 * 60.0)
			return Math.round((durationInSeconds / 3600.0)) + h;
		else if(durationInSeconds > 120.0)
			return Math.round((durationInSeconds / 60.0)) + m;
		else
			return Math.round(durationInSeconds) + s;
	}

	public static String getRelativePath(String ref, String rel) {
		String[] refs = TextUtilities.split(ref.replace('\\', '/'), '/');
		String[] rels = TextUtilities.split(rel.replace('\\', '/'), '/');
		for(int i = 0; i < Math.min(refs.length, rels.length); i++) {
			if(!(refs[i].equals(rels[i]))) {
				StringBuilder result = new StringBuilder();
				result.append(repeat("../", refs.length - (i + 1)));
				for(int j = i; j < rels.length; j++) {
					if(i != j)
						result.append('/');
					result.append(rels[j]);
				}
				return result.toString();
			}
		}
		return rel;
	}	
}
