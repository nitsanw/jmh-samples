package psy.lob.saw.utf8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.List;

import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Thread)
public class Utf8EncodingBenchmark  {
	// experiment test input
	private List<String> strings = new ArrayList<String>();
	
	// CharsetEncoder helper buffers
	private char[] chars;
	private CharBuffer charBuffer;
	private CharsetEncoder encoder;
	
	// My own encoder
	private CustomUtf8Encoder customEncoder;
	
	// Destination buffer, the slayer
	private ByteBuffer buffySummers;

	@Setup
	public void init() {
		boolean useDirectBuffer = Boolean.getBoolean("Utf8EncodingBenchmark.directBuffer");
		InputStream testTextStream = null;
		InputStreamReader inStreamReader = null;
		BufferedReader buffReader = null;
		try {
			testTextStream = getClass().getResourceAsStream("/Utf8Samples.txt");
			inStreamReader = new InputStreamReader(
					testTextStream, "UTF-8");
			buffReader = new BufferedReader(inStreamReader);
			String line;
			while ((line = buffReader.readLine()) != null) {
				strings.add(line);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		finally{
			closeStream(testTextStream);
			closeReader(inStreamReader);
			closeReader(buffReader);
		}

		if (useDirectBuffer) {
			buffySummers = ByteBuffer.allocateDirect(4096);
		} else {
			buffySummers = ByteBuffer.allocate(4096);
		}
		chars = new char[4096];
		charBuffer = CharBuffer.wrap(chars);
		encoder = Charset.forName("UTF-8").newEncoder();
		customEncoder = new CustomUtf8Encoder();
	}

	private void closeStream(InputStream inStream) {
	    if(inStream != null){
	    	try {
	            inStream.close();
	        } catch (IOException e) {
	        	throw new RuntimeException(e);
	        }
	    }
    }

	private void closeReader(Reader buffReader) {
	    if(buffReader != null){
	    	try {
	            buffReader.close();
	        } catch (IOException e) {
	        	throw new RuntimeException(e);
	        }
	    }
    }

	@GenerateMicroBenchmark
	public int customEncoder() {
		int countBytes = 0;
		for (int stringIndex = 0; stringIndex < strings.size(); stringIndex++) {
			customEncoder.encodeString(strings.get(stringIndex), buffySummers);
			countBytes += buffySummers.position();
			buffySummers.clear();
		}
		return countBytes;
	}
	
	@GenerateMicroBenchmark
	public int stringGetBytes() throws UnsupportedEncodingException {
		int countBytes = 0;
		for (int stringIndex = 0; stringIndex < strings.size(); stringIndex++) {
			buffySummers.put(strings.get(stringIndex).getBytes("UTF-8"));
			countBytes += buffySummers.position();
			buffySummers.clear();
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
			encoder.encode(charBuffer, buffySummers, true);
			countBytes += buffySummers.position();
			buffySummers.clear();
		}
		return countBytes;
	}
}
