package org.markware.oupscrk.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.zip.DataFormatException;

/**
 * Http Response parser
 * @author citestra
 *
 */
public class HttpResponseParser {

	private final static int BUFFER_SIZE = 65536;
	/**
	 * Parse response from UrlConnection
	 * @return Http response
	 * @throws IOException 
	 * @throws DataFormatException 
	 */
	public static HttpResponse parseResponse(HttpURLConnection connection) throws IOException, DataFormatException {
		HttpResponse httpResponse = new HttpResponse();
		
		// Get the response stream
		InputStream serverToProxyStream = null;
		int responseCode = connection.getResponseCode();
		if (responseCode >= 400) {
			serverToProxyStream = connection.getErrorStream();
		} else {
			serverToProxyStream = connection.getInputStream();
		}
		
		if (serverToProxyStream != null) {
			// Status line
			httpResponse.setStatusLine(connection.getHeaderField(0));
			httpResponse.setResponseCode(connection.getResponseCode());
			
			// Headers
			httpResponse.setHeaders(connection.getHeaderFields());
			
			// Read body
			String contentEncoding = connection.getContentEncoding();
			httpResponse.setContentEncoding(contentEncoding);
			
			ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
			byte by[] = new byte[ BUFFER_SIZE ];
			int index = serverToProxyStream.read(by, 0, BUFFER_SIZE);
			while ( index != -1 ) {
				responseBuffer.write(by, 0, index);
				index = serverToProxyStream.read( by, 0, BUFFER_SIZE );
			}
			responseBuffer.flush();

			// Decode body
			byte[] responsePlain = CompressionUtils.decodeContentBody(responseBuffer.toByteArray(), contentEncoding);
			httpResponse.setRawResponseBody(responsePlain);
			
			// Close Remote Server -> Proxy Stream
			serverToProxyStream.close();
		}
		return httpResponse;
	}
}
