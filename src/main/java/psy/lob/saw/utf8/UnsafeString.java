package psy.lob.saw.utf8;

import java.lang.reflect.Field;
import java.nio.CharBuffer;

import sun.misc.Unsafe;

public class UnsafeString {
	private static final Unsafe unsafe;
	private static final long valueOffset;
	private static final long offsetOffset;
	private static final long countOffset;

	static {
		try {
			// This is a bit of voodoo to force the unsafe object into
			// visibility and acquire it.
			// This is not playing nice, but as an established back door it is
			// not likely to be
			// taken away.
			Field field = Unsafe.class.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			unsafe = (Unsafe) field.get(null);
			valueOffset = unsafe.objectFieldOffset(String.class
			        .getDeclaredField("value"));
			Field declaredField;
			try {
				declaredField = String.class.getDeclaredField("count");
			}
			// this will happen for jdk7 as these fields have been removed
			catch (NoSuchFieldException e) {
				declaredField = null;
			}
			if (declaredField != null) {
				countOffset = unsafe.objectFieldOffset(declaredField);
			} else {
				countOffset = -1L;
			}
			declaredField = null;
			try {
				declaredField = String.class.getDeclaredField("offset");
			}
			// this will happen for jdk7 as these fields have been removed
			catch (NoSuchFieldException e) {
				declaredField = null;
			}
			if (declaredField != null) {
				offsetOffset = unsafe.objectFieldOffset(declaredField);
			} else {
				offsetOffset = -1L;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public final static String buildUnsafe(char[] chars) {
		String mutable = new String();// an empty string to hack
		unsafe.putObject(mutable, valueOffset, chars);
		if (countOffset != -1L) {
			unsafe.putInt(mutable, countOffset, chars.length);
		}
		return mutable;
	}

	public final static String buildUnsafe(char[] chars, int offset, int length) {
		String mutable = new String();// an empty string to hack
		unsafe.putObject(mutable, valueOffset, chars);
		if (countOffset != -1L) {
			unsafe.putInt(mutable, countOffset, length);
			unsafe.putIntVolatile(mutable, offsetOffset, offset);
		}
		return mutable;
	}

	public final static char[] getChars(String s) {
		return (char[]) unsafe.getObject(s, valueOffset);
	}

	public final static int getOffset(String s) {
		if (offsetOffset == -1L)
			return 0;
		else
			return unsafe.getInt(s, offsetOffset);
	}

	public final static CharBuffer getStringAsCharBuffer(String s) {
		CharBuffer buffy = CharBuffer.wrap(getChars(s));
		return buffy;
	}

	public final static void wrapStringWithCharBuffer(String s, CharBuffer buffy) {
		UnsafeCharBuffer.wrap(buffy, getChars(s), 0, s.length());
	}

	public static void main(String[] args) {
		String text = buildUnsafe(new char[] { 'W', 'O', 'W' });
		if (text.equals("WOW")) {
			System.out.println("WOW");
		}
	}
}
