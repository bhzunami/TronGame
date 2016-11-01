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
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class ClassUtilities {
	public static final Class<Object>          CLS_Object          = Object.class;
	public static final Class<Object[]>        CLS_ObjectA         = Object[].class;
	public static final Object[]               EMPTY_ObjectA       = new Object[0];
	public static final Class<?>               CLS_void            = void.class;
	public static final Class<?>               CLS_Class           = Class.class;
	public static final Class<?>[]             EMPTY_ClassA        = new Class[0];
	public static final Class<?>               CLS_boolean         = boolean.class;
	public static final Class<boolean[]>       CLS_booleanA        = boolean[].class;
	public static final Class<Boolean>         CLS_Boolean         = Boolean.class;
	public static final boolean[]              EMPTY_booleanA      = new boolean[0];
	public static final Class<?>               CLS_byte            = byte.class;
	public static final Class<byte[]>          CLS_byteA           = byte[].class;
	public static final byte[]                 EMPTY_byteA         = new byte[0];
	public static final Class<Byte>            CLS_Byte            = Byte.class;
	public static final Class<?>               CLS_char            = char.class;
	public static final Class<char[]>          CLS_charA           = char[].class;
	public static final char[]                 EMPTY_charA         = new char[0];
	public static final Class<Character>       CLS_Character       = Character.class;
	public static final Class<?>               CLS_short           = short.class;
	public static final Class<short[]>         CLS_shortA          = short[].class;
	public static final Class<Short>           CLS_Short           = Short.class;
	public static final Class<?>               CLS_int             = int.class;
	public static final Class<int[]>           CLS_intA            = int[].class;
	public static final int[]                  EMPTY_intA          = new int[0];
	public static final Class<Integer>         CLS_Integer         = Integer.class;
	public static final Class<?>               CLS_long            = long.class;
	public static final Class<long[]>          CLS_longA           = long[].class;
	public static final long[]                 EMPTY_longA         = new long[0];
	public static final Class<Long>            CLS_Long            = Long.class;
	public static final Class<?>               CLS_float           = float.class;
	public static final Class<float[]>         CLS_floatA          = float[].class;
	public static final float[]                EMPTY_floatA        = new float[0];
	public static final Class<Float>           CLS_Float           = Float.class;
	public static final Class<?>               CLS_double          = double.class;
	public static final double[]               EMPTY_doubleA       = new double[0];
	public static final Class<double[]>        CLS_doubleA         = double[].class;
	public static final Class<Double>          CLS_Double          = Double.class;
	public static final Class<String>          CLS_String          = String.class;
	public static final Class<String[]>        CLS_StringA         = String[].class;
	public static final String                 EMPTY_String        = "";
	public static final String[]               EMPTY_StringA       = new String[0];
	public static final Class<BitSet>          CLS_BitSet          = BitSet.class;
	public static final Class<UUID>            CLS_UUID            = UUID.class;
	public static final UUID[]                 EMPTY_UUIDA         = new UUID[0];
	public static final Class<File>            CLS_File            = File.class;
	public static final File[]                 EMPTY_FileA         = new File[0];

	@SuppressWarnings("rawtypes")
	public static final Class<Enum>            CLS_Enum            = Enum.class;
	@SuppressWarnings("rawtypes")
	public static final Class<Map>             CLS_Map             = Map.class;
	@SuppressWarnings("rawtypes")
	public static final Class<List>            CLS_List            = List.class;
	@SuppressWarnings("rawtypes")
	public static final Class<HashMap>         CLS_HashMap         = HashMap.class;
	@SuppressWarnings("rawtypes")
	public static final Class<IdentityHashMap> CLS_IdentityHashMap = IdentityHashMap.class;
	@SuppressWarnings("rawtypes")
	public static final Class<ArrayList>       CLS_ArrayList       = ArrayList.class;

	static final Hashtable<Class<?>, byte[]>            cls2md5               = new Hashtable<>();
	static final Hashtable<String, Class<?>>            clsMap                = new Hashtable<>();
	static final Set<Class<?>>                          IS_INTEGRAL           = new IdentityHashSet<>();           
	static final Set<Class<?>>                          IS_FLOAT              = new IdentityHashSet<>();           
	static final Set<Class<?>>                          IS_PRIMITIVE          = new IdentityHashSet<>();           
	static final Set<Class<?>>                          IS_WRAPPER            = new IdentityHashSet<>();           
	static final Map<Class<?>, HashMap<String, Field>>  fieldMap              = new IdentityHashMap<>();
	static final Map<Class<?>, Field[]>                 cls2fields            = new IdentityHashMap<>();
	static final Map<Class<?>, Method[]>                cls2methods           = new IdentityHashMap<>();
	static final Map<Class<?>, Map<Class<?>, Field[]>>  clsAnnotation2fields  = new IdentityHashMap<>();
	static final Map<Class<?>, Map<Class<?>, Method[]>> clsAnnotation2methods = new IdentityHashMap<>();
	static final Map<Class<?>, Class<?>>                inner2outer           = new IdentityHashMap<>();

	static {
		clsMap.put("boolean", CLS_boolean);
		clsMap.put("byte",    CLS_byte);
		clsMap.put("char",    CLS_char);
		clsMap.put("short",   CLS_short);
		clsMap.put("int",     CLS_int);
		clsMap.put("long",    CLS_long);
		clsMap.put("float",   CLS_float);
		clsMap.put("double",  CLS_double);

		IS_INTEGRAL.add(CLS_byte);
		IS_INTEGRAL.add(CLS_Byte);
		IS_INTEGRAL.add(CLS_char);
		IS_INTEGRAL.add(CLS_Character);
		IS_INTEGRAL.add(CLS_short);
		IS_INTEGRAL.add(CLS_Short);
		IS_INTEGRAL.add(CLS_int);
		IS_INTEGRAL.add(CLS_Integer);
		IS_INTEGRAL.add(CLS_long);
		IS_INTEGRAL.add(CLS_Long);

		IS_FLOAT.add(CLS_float);
		IS_FLOAT.add(CLS_Float);
		IS_FLOAT.add(CLS_double);
		IS_FLOAT.add(CLS_Double);

		IS_PRIMITIVE.add(CLS_boolean);
		IS_PRIMITIVE.add(CLS_void);
		IS_PRIMITIVE.add(CLS_byte);
		IS_PRIMITIVE.add(CLS_char);
		IS_PRIMITIVE.add(CLS_short);
		IS_PRIMITIVE.add(CLS_int);
		IS_PRIMITIVE.add(CLS_long);
		IS_PRIMITIVE.add(CLS_float);
		IS_PRIMITIVE.add(CLS_double);

		IS_WRAPPER.add(CLS_Boolean);
		IS_WRAPPER.add(CLS_Byte);
		IS_WRAPPER.add(CLS_Character);
		IS_WRAPPER.add(CLS_Short);
		IS_WRAPPER.add(CLS_Integer);
		IS_WRAPPER.add(CLS_Long);
		IS_WRAPPER.add(CLS_Float);
		IS_WRAPPER.add(CLS_Double);
	}

	public static byte[] getMD5(Class<?> cls) throws NoSuchAlgorithmException, IOException {
		byte[] result = cls2md5.get(cls);
		if(result == null) {
			MessageDigest md     = MessageDigest.getInstance("MD5");
			String        name   = cls.getName();
			name                 = name.substring(name.lastIndexOf('.') + 1) + ".class";
			try (InputStream  in = cls.getResourceAsStream(name)) {
				byte[]  buffer = new byte[8192];
				while(true) {
					int r = in.read(buffer);
					if(r == -1) break;
					md.update(buffer, 0, r);
				} 
			}
			result = md.digest();
			cls2md5.put(cls, result);
		}
		return result;
	}

	public static Class<?> toPrimitiveType(Class<?> type) {
		if(type == Boolean.class)
			return Boolean.TYPE;
		else if(type == Byte.class)
			return Byte.TYPE;
		else if(type == Short.class)
			return Short.TYPE;
		else if(type == Integer.class)
			return Integer.TYPE;
		else if(type == Long.class)
			return Long.TYPE;
		else if(type == Float.class)
			return Float.TYPE;
		else if(type == Double.class)
			return Double.TYPE;
		else
			return type;
	}

	public static Class<?> forName(String name) throws ClassNotFoundException {
		Class<?> result = clsMap.get(name);
		return result == null ? getGlobalClassloader().loadClass(name) : result;
	}

	public static Class<?>[] getGenericTypes(Type genericType) throws ClassNotFoundException {
		String[]   gtypes = genericType.toString().split("[<>]");
		Class<?>[] result = new Class<?>[gtypes.length - 1];
		for(int i = 0; i < result.length; i++) {
			String cls    = gtypes[i + 1];
			int dimension = TextUtilities.count(cls, '[');
			if(dimension == 0)
				result[i] = forName(cls);
			else {
				cls = cls.substring(0, cls.indexOf('['));
				int[] dims = new int[dimension];
				result[i] = Array.newInstance(forName(cls), dims).getClass();
			}
		}
		return result;
	}

	public static boolean isBoolean(Class<?> cls) {
		return CLS_boolean == cls || CLS_Boolean == cls;
	}

	public static boolean isIntegral(Class<?> cls) {
		return IS_INTEGRAL.contains(cls);
	}

	public static boolean isFloat(Class<?> cls) {
		return IS_FLOAT.contains(cls);
	}

	public static boolean isPrimitiveOrWrapper(Class<?> cls) {
		return IS_PRIMITIVE.contains(cls) || IS_WRAPPER.contains(cls);
	}

	public static Class<?> getComponentType(Class<?> type) {
		if(type.isArray()) return getComponentType(type.getComponentType());
		return type;
	}

	public static Field[] getAllFields(Class<?> type) {
		Field[] result = cls2fields.get(type);
		if(result == null) {
			Collection<Field> fields = getAllFields(type, new ArrayList<>());
			result = fields.toArray(new Field[fields.size()]);
			cls2fields.put(type, result);
			AccessibleObject.setAccessible(result, true);			
		}
		return result;
	}

	private static Collection<Field> getAllFields(Class<?> type, List<Field> result) {
		if(type == CLS_Object) return result;
		try {
			Collections.addAll(result, type.getDeclaredFields());
		} catch(Throwable t) {
			t.printStackTrace();
		}
		return getAllFields(type.getSuperclass(), result);
	}

	public static Method[] getAllMethods(Class<?> type) {
		Method[] result = cls2methods.get(type);
		if(result == null) {
			Collection<Method> methods = getAllMethods(type, new ArrayList<>());
			result = methods.toArray(new Method[methods.size()]);
			cls2methods.put(type, result);
			AccessibleObject.setAccessible(result, true);			
		}
		return result;
	}

	public static Method getMethod(String type, String method, Class<?> ... argTypes) throws ClassNotFoundException {
		return getMethod(getGlobalClassloader().loadClass(type), method, argTypes);
	}

	public static Method getMethod(Class<?> type, String method, Class<?> ... argTypes) {
		for(Method m : getAllMethods(type)) {
			if(m.getName().equals(method) && Arrays.equals(m.getParameterTypes(), argTypes))
				return m;
		}
		throw new NoSuchMethodError(method + TextUtilities.toString("(", ",", ")", argTypes));
	}

	private static Collection<Method> getAllMethods(Class<?> type, List<Method> result) {
		if(type == CLS_Object) return result;
		Collections.addAll(result, type.getDeclaredMethods());
		return getAllMethods(type.getSuperclass(), result);
	}

	public static String getCurrentMethodName() {
		return Thread.currentThread().getStackTrace()[2].getMethodName();
	}

	public static String getCallerMethodName() {
		return Thread.currentThread().getStackTrace()[3].getMethodName();
	}

	public static String getCallerClassName() {
		return Thread.currentThread().getStackTrace()[3].getClassName();
	}
	
	public static String getCallerClassAndMethodName(int n) {
		StackTraceElement[] elements = Thread.currentThread().getStackTrace();
		if (n + 4 >= elements.length)
			return "none";
		return elements[n + 3].getClassName() + "." + elements[n + 3].getMethodName();
	}

	public static Field getField(Class<?> type, String field) {
		HashMap<String, Field> fields = fieldMap.get(type);
		if(fields == null) {
			fields = new HashMap<>();
			Field[] fieldsa = getAllFields(type); 
			for(int i = fieldsa.length; --i >= 0;)
				fields.put(fieldsa[i].getName(), fieldsa[i]);
			fieldMap.put(type, fields);
		}
		Field result = fields.get(field);
		if(result == null)
			throw new NoSuchFieldError(type.getName() + "." + field);
		return result;
	}

	public static Field[] getAllAnotatedFields(Class<?> cls, Class<? extends Annotation> annotationCls) {
		Map<Class<?>, Field[]> annotation2field = clsAnnotation2fields.get(cls);
		if(annotation2field == null) {
			annotation2field = new IdentityHashMap<>();
			clsAnnotation2fields.put(cls, annotation2field);
		}
		Field[] result = annotation2field.get(annotationCls);
		if(result == null) {
			List<Field> fields = new LinkedList<>();
			for(Field field : getAllFields(cls)) {
				if(field.getAnnotation(annotationCls) != null)
					fields.add(field);
			}
			result = fields.toArray(new Field[fields.size()]);
			annotation2field.put(annotationCls, result);
		}

		return result;
	}

	public static Method[] getAllAnnotatedMethods(Class<?> cls, Class<? extends Annotation> annotationCls) {
		Map<Class<?>, Method[]> annotation2method = clsAnnotation2methods.get(cls);
		if(annotation2method == null) {
			annotation2method = new IdentityHashMap<>();
			clsAnnotation2methods.put(cls, annotation2method);
		}
		Method[] result = annotation2method.get(annotationCls);
		if(result == null) {
			List<Method> methods = new LinkedList<>();
			for(Method method : getAllMethods(cls)) {
				if(method.getAnnotation(annotationCls) != null)
					methods.add(method);
			}
			result = methods.toArray(new Method[methods.size()]);
			annotation2method.put(annotationCls, result);
		}

		return result;
	}

	private static final Map<Method, Class<?>[]> method2paramTypes = new IdentityHashMap<>();
	public synchronized static Class<?>[] getParameterTypes(Method m) {
		Class<?>[] result = method2paramTypes.get(m);
		if(result == null) {
			result = m.getParameterTypes();
			method2paramTypes.put(m, result);
		}
		return result;
	}

	public synchronized static ClassLoader getGlobalClassloader() {
		return ClassUtilities.class.getClassLoader();
	}

	public static Class<?>[] getTypeHierarchy(Class<?> cls) {
		ArrayList<Class<?>> result = getTypeHierarchy(cls, new ArrayList<>());
		return result.toArray(new Class<?>[result.size()]);
	}

	private static ArrayList<Class<?>> getTypeHierarchy(Class<?> cls, ArrayList<Class<?>> result) {
		result.add(cls);
		if(cls == CLS_Object)
			return result;
		return getTypeHierarchy(cls.getSuperclass(), result);
	}

	@SuppressWarnings("unchecked")
	public static <T> Class<T> forNameOrNull(String cls) {
		try {
			return (Class<T>) forName(cls);
		} catch (Throwable t) {
			return null;
		}
	}

	/**
	 * Returns the class of the specified object, or {@code null} if {@code object} is null.
	 * This method is also useful for fetching the class of an object known only by its bound
	 * type. As of Java 6, the usual pattern:
	 *
	 * <blockquote><pre>
	 * Number n = 0;
	 * Class<? extends Number> c = n.getClass();
	 * </pre></blockquote>
	 *
	 * doesn't seem to work if {@link Number} is replaced by a parametirez type {@code T}.
	 *
	 * @param  <T> The type of the given object.
	 * @param  object The object for which to get the class, or {@code null}.
	 * @return The class of the given object, or {@code null} if the given object was null.
	 */
	@SuppressWarnings("unchecked")
	public static <T> Class<? extends T> getClass(final T object) {
		return (object != null) ? (Class<? extends T>) object.getClass() : null;
	}

	public static int getDimension(Class<?> cls) {
		return cls.getComponentType() == null ? 0 : 1 + getDimension(cls.getComponentType());
	}

	private static final AtomicLong ID_COUNTER = new AtomicLong();

	public static long createObjectID() {
		return ID_COUNTER.addAndGet(1);
	}

	public static int identityHashCode(Object x) {
		if(x instanceof IObjectID)
			return (int) ((IObjectID) x).getObjectID();
		else if(x instanceof String || x instanceof Number || x instanceof UUID)
			return x.hashCode();
		return System.identityHashCode(x);
	}

	/**
	 * Value representing null keys inside tables.
	 */
	private static final Object NULL_KEY = new IObjectID() {
		@Override
		public long getObjectID() {
			return 0;
		}
	};

	/**
	 * Use NULL_KEY for key if it is null.
	 */

	public static Object maskNull(Object key) {
		return (key == null ? NULL_KEY : key);
	}

	/**
	 * Returns internal representation of null key back to caller as null.
	 */
	public static Object unmaskNull(Object key) {
		return (key == NULL_KEY ? null : key);
	}

	@SuppressWarnings("unchecked")
	public static <T> T newInstance(String cls, Class<?>[] types, Object ... args) {
		try {
			return (T) getGlobalClassloader().loadClass(cls).getConstructor(types).newInstance(args);
		} catch(Throwable t) {
			t.printStackTrace();
		}
		return null;
	}

	public static <T> T newInstance(Class<? extends T> cls, Class<?>[] types, Object ... args) {
		try {
			return cls.getConstructor(types).newInstance(args);
		} catch(Throwable t) {
			t.printStackTrace();
		}
		return null;
	}	

	public static Class<?> getEnclosingClass(Class<?> cls) {
		Class<?> result = inner2outer.get(cls);
		if(result == null) {
			result = cls.getEnclosingClass();
			inner2outer.put(cls, result);
		}
		return result;
	}
}
