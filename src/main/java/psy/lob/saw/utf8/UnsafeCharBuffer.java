package psy.lob.saw.utf8;

import java.nio.Buffer;
import java.nio.CharBuffer;

import psy.lob.saw.util.UnsafeAccess;

public class UnsafeCharBuffer {
	private static final long hbOffset;
	private static final long offsetOffset;
	private static final long capacityOffset;
	private static final long markOffset;

	static {
		try {
			hbOffset = UnsafeAccess.UNSAFE.objectFieldOffset(CharBuffer.class
			        .getDeclaredField("hb"));
			offsetOffset = UnsafeAccess.UNSAFE
			        .objectFieldOffset(CharBuffer.class
			                .getDeclaredField("offset"));
			capacityOffset = UnsafeAccess.UNSAFE.objectFieldOffset(Buffer.class
			        .getDeclaredField("capacity"));
			markOffset = UnsafeAccess.UNSAFE.objectFieldOffset(Buffer.class
			        .getDeclaredField("mark"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void wrap(CharBuffer buffy, char[] chars, int offset,
	        int length) {
		UnsafeAccess.UNSAFE.putObject(buffy, hbOffset, chars);
		UnsafeAccess.UNSAFE.putInt(buffy, offsetOffset, 0); // see
		// CharBuffer.wrap
		// doc
		UnsafeAccess.UNSAFE.putInt(buffy, capacityOffset, chars.length);
		UnsafeAccess.UNSAFE.putInt(buffy, markOffset, -1);
		buffy.position(offset);
		buffy.limit(offset + length);
	}
}
