package org.markware.oupscrk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;  

public class CompressionUtils {  

	public static byte[] gzipCompress(byte[] data) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
		GZIPOutputStream gzip = new GZIPOutputStream(bos);
		gzip.write(data);
		gzip.close();
		byte[] compressed = bos.toByteArray();
		bos.close();
		return compressed;
	}
	
	public static byte[] gzipDecompress(byte[] compressed) throws IOException {
		ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
		GZIPInputStream gis = new GZIPInputStream(bis);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		
		byte[] buffer = new byte[1024];
		int read;
		while((read = gis.read(buffer)) > 0) {
			os.write(buffer, 0, read);
		}
		os.close();
		gis.close();
		bis.close();
		return os.toByteArray();
	}
	
	private static byte[] zlibCompress(byte[] data) throws IOException {  
		Deflater deflater = new Deflater();  
		deflater.setInput(data);  
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);   
		deflater.finish();  
		byte[] buffer = new byte[1024];   
		while (!deflater.finished()) {  
			int count = deflater.deflate(buffer); // returns the generated code... index  
			outputStream.write(buffer, 0, count);   
		}  
		outputStream.close();  
		byte[] output = outputStream.toByteArray();  
		return output;

	}  

	private static byte[] zlibDecompress(byte[] data) throws IOException, DataFormatException {  
		Inflater inflater = new Inflater();   
		inflater.setInput(data);  
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);  
		byte[] buffer = new byte[1024];  
		while (!inflater.finished()) {  
			int count = inflater.inflate(buffer);  
			outputStream.write(buffer, 0, count);  
		}  
		outputStream.close();  
		return outputStream.toByteArray(); 
	}  
	
	public static byte[] encodeContentBody(byte[] plainBody, String encodingAlg) throws IOException {
		byte[] result = plainBody;
		if (encodingAlg == null || encodingAlg.isEmpty() || encodingAlg == "identity") {
			return result;
		}
		if ("gzip".equals(encodingAlg) || "x-gzip".equals(encodingAlg)) {
			result = gzipCompress(plainBody);
		} else if ("deflate".equals(encodingAlg)) {
			result = zlibCompress(plainBody);
		} else {
			throw new RuntimeException("Encoding algorithm unknown" + encodingAlg);
		}
		return result;
	}
	
	public static byte[] decodeContentBody(byte[] encodedBody, String encodingAlg) throws IOException, DataFormatException {
		byte[] result = encodedBody;
		if (encodingAlg == null || encodingAlg.isEmpty() || encodingAlg == "identity") {
			return result;
		}
		if ("gzip".equals(encodingAlg) || "x-gzip".equals(encodingAlg)) {
			result = gzipDecompress(encodedBody);
		} else if ("deflate".equals(encodingAlg)) {
			result = zlibDecompress(encodedBody);
		} else {
			throw new RuntimeException("Encoding algorithm unknown" + encodingAlg);
		}
		return result;
	}
}
