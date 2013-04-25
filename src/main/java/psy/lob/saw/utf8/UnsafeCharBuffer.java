package psy.lob.saw.utf8;

import java.nio.Buffer;
import java.nio.CharBuffer;

public class UnsafeCharBuffer {
	private static final long hbOffset;
	private static final long offsetOffset;
	private static final long capacityOffset;
	private static final long markOffset;

	static {
		try {
			hbOffset = UnsafeAccess.unsafe.objectFieldOffset(CharBuffer.class
			        .getDeclaredField("hb"));
			offsetOffset = UnsafeAccess.unsafe
			        .objectFieldOffset(CharBuffer.class
			                .getDeclaredField("offset"));
			capacityOffset = UnsafeAccess.unsafe.objectFieldOffset(Buffer.class
			        .getDeclaredField("capacity"));
			markOffset = UnsafeAccess.unsafe.objectFieldOffset(Buffer.class
			        .getDeclaredField("mark"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void wrap(CharBuffer buffy, char[] chars, int offset,
	        int length) {
		UnsafeAccess.unsafe.putObject(buffy, hbOffset, chars);
		UnsafeAccess.unsafe.putInt(buffy, offsetOffset, 0); // see
		// CharBuffer.wrap
		// doc
		UnsafeAccess.unsafe.putInt(buffy, capacityOffset, chars.length);
		UnsafeAccess.unsafe.putInt(buffy, markOffset, -1);
		buffy.position(offset);
		buffy.limit(offset + length);
	}
}
