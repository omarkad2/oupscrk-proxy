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

	public static String gzipCompress(String data) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length());
		GZIPOutputStream gzip = new GZIPOutputStream(bos);
		gzip.write(data.getBytes());
		gzip.close();
		byte[] compressed = bos.toByteArray();
		bos.close();
		return new String(compressed, StandardCharsets.UTF_8);
	}
	
	public static String gzipDecompress(String compressed) throws IOException {
		ByteArrayInputStream bis = new ByteArrayInputStream(compressed.getBytes());
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
	
	public static String zlibCompress(String data) throws IOException {  
		Deflater deflater = new Deflater();  
		deflater.setInput(data.getBytes());  
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length());   
		deflater.finish();  
		byte[] buffer = new byte[1024];   
		while (!deflater.finished()) {  
			int count = deflater.deflate(buffer); // returns the generated code... index  
			outputStream.write(buffer, 0, count);   
		}  
		outputStream.close();  
		byte[] output = outputStream.toByteArray();  
		return new String(output, StandardCharsets.UTF_8);

	}  

	public static String zlibDecompress(String data) throws IOException, DataFormatException {  
		Inflater inflater = new Inflater();   
		inflater.setInput(data.getBytes());  
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length());  
		byte[] buffer = new byte[1024];  
		while (!inflater.finished()) {  
			int count = inflater.inflate(buffer);  
			outputStream.write(buffer, 0, count);  
		}  
		outputStream.close();  
		byte[] output = outputStream.toByteArray();  
		return new String(output, StandardCharsets.UTF_8);  

	}  
}
