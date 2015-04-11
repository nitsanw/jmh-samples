package psy.lob.saw.utf8;

import static psy.lob.saw.utf8.UnsafeDirectByteBuffer.getAddress;
import static psy.lob.saw.utf8.UnsafeDirectByteBuffer.putByte;
import static psy.lob.saw.utf8.UnsafeString.getChars;

import java.nio.ByteBuffer;
import java.nio.charset.CoderResult;

import sun.nio.cs.Surrogate;

/**
 * Customized version of the JDK7 UTF8 encoder targeting the use-case of encoding strings that should fit into a byte
 * buffer.
 * 
 * @author nitsan
 */
public class CustomUtf8Encoder {
    // as opposed to the JDK version where this is allocated lazily if required
    private final Surrogate.Parser sgp = new Surrogate.Parser();
    // taking these off the stack seems to make it go faster
    private int lastSp;
    private int lastDp;

    /**
     * Encodes a string into the byte buffer using the UTF-8 encoding. Like the JDK encoder this will return UNDERFLOW
     * on success and ERROR/OVERFLOW otherwise, but unlike the JDK encode it does not allow resuming the operation and
     * will not move the byte buffer position should the string not fit in it.
     * 
     * @param src
     * @param dst
     * @return
     */
    public final CoderResult encodeString(String src, ByteBuffer dst) {
        if (dst.hasArray())
            return encodeStringToHeap(src, dst);
        else
            return encodeStringToDirect(src, dst);
    }

    public final CoderResult encodeStringToDirect(String src, ByteBuffer dst) {
        lastDp = 0;
        int dp = dst.position();
        int dl = dst.limit();

        /*
         * in JDK7 offset is always 0, but earlier versions accomodated substrings pointing back to original array and
         * having a separate offset and length.
         */
        int spCurr = UnsafeString.getOffset(src);
        int sl = src.length();

        // pluck the chars array out of the String, saving us an array copy
        CoderResult result = encode(getChars(src), spCurr, sl, getAddress(dst), dp, dl);
        // only move the position if we fit the whole thing in.
        if (lastDp != 0)
            dst.position(lastDp);
        return result;

    }

    /**
     * The parameter naming is from the JDK source and I kept it to make diffing easier. The s stands for source, the d
     * for destination. It actually grew on me as I played with the code, but I agree longer names are more readable.
     * 
     * @param sa source char array
     * @param spCurr the source position starting point
     * @param sl source array length/limit
     * @param dAddress destination address(plucked out of Buffer using Unsafe)
     * @param dp destination position
     * @param dl destination limit
     * @return UNDERFLOW is successful, OVERFLOW/ERROR otherwise
     */
    private final CoderResult encode(char[] sa, int spCurr, int sl, long dAddress, int dp, int dl) {
        lastSp = spCurr;
        int dlASCII = Math.min(sl - lastSp, dl - dp);
        // handle ascii encoded strings in an optimised loop
        while (dp < dlASCII && sa[lastSp] < 128)
            // TODO: could arguably skip this utility and compute the target address directly...
            putByte(dAddress, dp++, (byte) sa[lastSp++]);

        while (lastSp < sl) {
            int c = sa[lastSp];
            if (c < 128) {
                if (dp >= dl)
                    return CoderResult.OVERFLOW;
                putByte(dAddress, dp++, (byte) c);
            } else if (c < 2048) {
                if (dl - dp < 2)
                    return CoderResult.OVERFLOW;
                putByte(dAddress, dp++, (byte) (0xC0 | (c >> 6)));
                putByte(dAddress, dp++, (byte) (0x80 | (c & 0x3F)));
            } else if (Surrogate.is(c)) {
                int uc = sgp.parse((char) c, sa, lastSp, sl);
                if (uc < 0) {
                    lastDp = dp;
                    return sgp.error();
                }
                if (dl - dp < 4)
                    return CoderResult.OVERFLOW;
                putByte(dAddress, dp++, (byte) (0xF0 | uc >> 18));
                putByte(dAddress, dp++, (byte) (0x80 | uc >> 12 & 0x3F));
                putByte(dAddress, dp++, (byte) (0x80 | uc >> 6 & 0x3F));
                putByte(dAddress, dp++, (byte) (0x80 | uc & 0x3F));
                ++lastSp;
            } else {
                if (dl - dp < 3)
                    return CoderResult.OVERFLOW;
                putByte(dAddress, dp++, (byte) (0xE0 | c >> 12));
                putByte(dAddress, dp++, (byte) (0x80 | c >> 6 & 0x3F));
                putByte(dAddress, dp++, (byte) (0x80 | c & 0x3F));
            }
            ++lastSp;
        }
        lastDp = dp;
        return CoderResult.UNDERFLOW;
    }

    public CoderResult encodeStringToHeap(String src, ByteBuffer dst) {
        lastDp = 0;
        int arrayOffset = dst.arrayOffset();
        int dp = arrayOffset + dst.position();
        int dl = arrayOffset + dst.limit();

        int spCurr = UnsafeString.getOffset(src);
        int sl = src.length();

        try {
            CoderResult result = encode(UnsafeString.getChars(src), spCurr, sl, dst.array(), dp, dl);
            dst.position(lastDp - arrayOffset);
            return result;
        } catch (ArrayIndexOutOfBoundsException e) {
            return CoderResult.OVERFLOW;
        }

    }

    private CoderResult encode(char[] sa, int spCurr, int sl, byte[] da, int dp, int dl) {
        lastSp = spCurr;
        int dlASCII = dp + Math.min(sl - lastSp, dl - dp);
        // handle ascii encoded strings in an optimised loop
        while (dp < dlASCII && sa[lastSp] < 128)
            da[dp++] = (byte) sa[lastSp++];

        /*
         * we are counting on the JVM array boundary checks to throw an exception rather then checking boundaries
         * ourselves... no nice, and potentially not that much of a performance enhancement.
         */
        while (lastSp < sl) {
            int c = sa[lastSp];
            if (c < 128) {
                da[dp++] = (byte) c;
            } else if (c < 2048) {
                da[dp++] = (byte) (0xC0 | (c >> 6));
                da[dp++] = (byte) (0x80 | (c & 0x3F));
            } else if (Surrogate.is(c)) {
                int uc = sgp.parse((char) c, sa, lastSp, sl);
                if (uc < 0) {
                    lastDp = dp;
                    return sgp.error();
                }
                da[dp++] = (byte) (0xF0 | uc >> 18);
                da[dp++] = (byte) (0x80 | uc >> 12 & 0x3F);
                da[dp++] = (byte) (0x80 | uc >> 6 & 0x3F);
                da[dp++] = (byte) (0x80 | uc & 0x3F);
                ++lastSp;
            } else {
                da[dp++] = (byte) (0xE0 | c >> 12);
                da[dp++] = (byte) (0x80 | c >> 6 & 0x3F);
                da[dp++] = (byte) (0x80 | c & 0x3F);
            }
            ++lastSp;
        }
        lastDp = dp;
        return CoderResult.UNDERFLOW;
    }
}
