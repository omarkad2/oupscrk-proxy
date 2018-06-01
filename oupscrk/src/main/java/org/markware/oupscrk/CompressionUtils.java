package org.markware.oupscrk;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;  

public class CompressionUtils {  

	private static byte[] gzipCompress(String data) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length());
		GZIPOutputStream gzip = new GZIPOutputStream(bos);
		gzip.write(data.getBytes("UTF-8"));
		gzip.close();
		byte[] compressed = bos.toByteArray();
		bos.close();
		return compressed;
	}
	
	private static String gzipDecompress(byte[] compressed) throws IOException {
		ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
		GZIPInputStream gis = new GZIPInputStream(bis);
		BufferedReader br = new BufferedReader(new InputStreamReader(gis, "UTF-8"));
		StringBuilder sb = new StringBuilder();
		String line;
		while((line = br.readLine()) != null) {
			sb.append(line);
		}
		br.close();
		gis.close();
		bis.close();
		return sb.toString();
	}
	
	private static byte[] zlibCompress(String data) throws IOException {  
		Deflater deflater = new Deflater();  
		deflater.setInput(data.getBytes("UTF-8"));  
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length());   
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

	private static String zlibDecompress(byte[] data) throws IOException, DataFormatException {  
		Inflater inflater = new Inflater();   
		inflater.setInput(data);  
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);  
		byte[] buffer = new byte[1024];  
		while (!inflater.finished()) {  
			int count = inflater.inflate(buffer);  
			outputStream.write(buffer, 0, count);  
		}  
		outputStream.close();  
		return outputStream.toString("UTF-8"); 
	}  
	
	public static byte[] encodeContentBody(String plainBody, String encodingAlg) throws IOException {
		byte[] result = plainBody.getBytes("UTF-8");
		if (encodingAlg == null || encodingAlg.isEmpty() || encodingAlg == "identity") {
			return result;
		}
		if ("gzip".equals(encodingAlg) || "x-gzip".equals(encodingAlg)) {
			result = CompressionUtils.gzipCompress(plainBody);
		} else if ("deflate".equals(encodingAlg)) {
			result = CompressionUtils.zlibCompress(plainBody);
		} else {
			throw new RuntimeException("Encoding algorithm unknown" + encodingAlg);
		}
		return result;
	}
	
	public static String decodeContentBody(byte[] encodedBody, String encodingAlg) throws IOException, DataFormatException {
		String result = new String(encodedBody, StandardCharsets.UTF_8);
		if (encodingAlg == null || encodingAlg.isEmpty() || encodingAlg == "identity") {
			return result;
		}
		if ("gzip".equals(encodingAlg) || "x-gzip".equals(encodingAlg)) {
			result = CompressionUtils.gzipDecompress(encodedBody);
		} else if ("deflate".equals(encodingAlg)) {
			result = CompressionUtils.zlibDecompress(encodedBody);
		} else {
			throw new RuntimeException("Encoding algorithm unknown" + encodingAlg);
		}
		return result;
	}
}
