package ch.fhnw.util;

import java.util.EnumSet;
import java.util.Map;
import java.util.Random;

public class EnumUtilities {
	private static final Map<Class<?>, String[]> cls2stringList = new IdentityHashMap<Class<?>, String[]>();
	private static final Map<Class<?>, String[]> cls2nameList   = new IdentityHashMap<Class<?>, String[]>();
	private static final Random                  rnd            = new Random();
	
	@SuppressWarnings("unchecked")
	private static <E extends Enum<?>> Class<E> fixClass(Class<E> cls) {
		Class<?> enclosing = ClassUtilities.getEnclosingClass(cls);
		if(enclosing != null && ClassUtilities.CLS_Enum.isAssignableFrom(enclosing))
			cls = (Class<E>) enclosing;
		return cls;
	}

	public static <E extends Enum<E>> String[] toStringArrayName(Class<E> cls) {
		return toStringArray(cls, true);
	}
	
	public static String[] toStringArray(Class<? extends Enum<?>> cls) {
		return toStringArray(cls, false);
	}

	public static <E extends Enum<?>> String[] toStringArray(Class<E> cls, boolean usingName) {
		cls = fixClass(cls);
		Map<Class<?>, String[]> map    = usingName ? cls2nameList : cls2stringList;
		String[]                result = map.get(cls);
		if(result == null) {
			E[] set = cls.getEnumConstants();
			result  = new String[set.length];
			int i = 0;
			for(E e : set) {
				result[i++] = usingName ? e.name() : e.toString();
			}
			map.put(cls, result);
		}
		return result;
	}

	public static <E extends Enum<?>> E valueOf(Class<E> cls, int ordinal) {
		cls = fixClass(cls);
		int i = 0;
		for(E e : cls.getEnumConstants()) {
			if(i == ordinal)
				return e;
			i++;
		}
		return null;
	}

	public static <E extends Enum<?>> E valueOf(Class<E> cls, String name) {
		cls = fixClass(cls);
		for(E e : cls.getEnumConstants())
			if(e.name().equals(name))
				return e;
		return null;
	}

	public static <E extends Enum<?>> E valueOfIgnoreCase(Class<E> cls, String name) {
		cls = fixClass(cls);
		for(E e : cls.getEnumConstants())
			if(e.name().equalsIgnoreCase(name))
				return e;
		return null;
	}
	
	public static <E extends Enum<?>> E startsWithIgnoreCase(Class<E> cls, String name) {
		cls = fixClass(cls);
		for(E e : cls.getEnumConstants())
			if(name.toLowerCase().startsWith( e.name().toLowerCase() ) )
				return e;
		return null;
	}
	
	public static <E extends Enum<?>> E nocaseValueOf(Class<E> cls, String name) {
		cls = fixClass(cls);
		for(E e : cls.getEnumConstants())
			if(e.name().equalsIgnoreCase(name))
				return e;
		return null;
	}

	public static <E extends Enum<?>> E valueOfToString(Class<E> cls, String name) {
		cls = fixClass(cls);
		for(E e : cls.getEnumConstants())
			if(e.toString().equals(name))
				return e;
		return null;
	}

	public static <E extends Enum<E>> E randomValueOf(Class<E> cls) {
		cls = fixClass(cls);
		EnumSet<E> set = EnumSet.allOf(cls);
		int ordinal = rnd.nextInt(set.size());
		for(E e : set) {
			if(ordinal-- == 0)
				return e;
		}
		return set.iterator().next();
	}

	public static <T extends Enum<?>> T randomValueOf(T[] types) {
		return types[rnd.nextInt(types.length)];
	}

	@SafeVarargs
	public static <T extends Enum<T>> EnumSet<T> setOf(Class<T> cls, T ... enums) {
		return enums.length == 0 ? EnumSet.noneOf(cls) : EnumSet.of(enums[0], ArrayUtilities.dropFirst(1, enums));
	}

	@SuppressWarnings("unchecked")
	public static void setValueOf(Class<? extends Enum<?>> cls, String name, Enum<?> value) {
		Object o = valueOf(cls, name);
		if(o == null) {
			Map<String, Enum<?>> map;
			try {
				map = (Map<String, Enum<?>>) ClassUtilities.getMethod(ClassUtilities.CLS_Class, "enumConstantDirectory").invoke(cls);
				map.put(name, value);
			} catch (Throwable t) {
				throw new RuntimeException(t);
			}
		}
	}
}
