package psy.lob.saw.utf8;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;

import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Thread)
public class Utf8EncodingBenchmark  {
	private final static String stringsFile = "/Utf8Samples.txt";
	boolean direct = Boolean.getBoolean("Utf8EncodingBenchmark.directBuffer");
	private ArrayList<String> strings = new ArrayList<String>();
	private ByteBuffer dest;
	private char[] chars;
	private CharBuffer charBuffer;
	private CharsetEncoder encoder;
	private CustomUtf8Encoder customEncoder;

	@Setup
	public void init() {
		BufferedReader reader = null;
		try {
			InputStream resourceAsStream = getClass().getResourceAsStream(stringsFile);
			reader = new BufferedReader(new InputStreamReader(
					resourceAsStream, "UTF-8"));
			String line;
			while ((line = reader.readLine()) != null) {
				strings.add(line);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} 

		if (direct) {
			dest = ByteBuffer.allocateDirect(4096);
		} else {
			dest = ByteBuffer.allocate(4096);
		}
		chars = new char[4096];
		charBuffer = CharBuffer.wrap(chars);
		encoder = Charset.forName("UTF-8").newEncoder();
		customEncoder = new CustomUtf8Encoder();

	}

	@GenerateMicroBenchmark
	public int customEncoder() {
		int countBytes = 0;
		for (int stringIndex = 0; stringIndex < strings.size(); stringIndex++) {
			customEncoder.encodeString(strings.get(stringIndex), dest);
			countBytes += dest.position();
			dest.clear();
		}
		return countBytes;
	}
	
	@GenerateMicroBenchmark
	public int stringGetBytes() throws UnsupportedEncodingException {
		int countBytes = 0;
		for (int stringIndex = 0; stringIndex < strings.size(); stringIndex++) {
			dest.put(strings.get(stringIndex).getBytes("UTF-8"));
			countBytes += dest.position();
			dest.clear();
		}
		return countBytes;
	}
	@GenerateMicroBenchmark
	public int charsetEncoder() throws UnsupportedEncodingException {
		int countBytes = 0;
		for (int stringIndex = 0; stringIndex < strings.size(); stringIndex++) {
			String source = strings.get(stringIndex);
			int length = source.length();
			source.getChars(0, length, chars, 0);
			charBuffer.position(0);
			charBuffer.limit(length);
			encoder.reset();
			encoder.encode(charBuffer, dest, true);
			countBytes += dest.position();
			dest.clear();
		}
		return countBytes;
	}
}
