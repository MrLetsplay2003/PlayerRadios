package me.mrletsplay.playerradios.util;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Arrays;
import java.util.stream.Collectors;

import me.mrletsplay.mrcore.json.JSONObject;

public class PasteText {
	
	public static String hastebin(String text) throws IOException, ParseException {
		return hastebin(text, "txt");
	}
	
	public static String hastebin(String text, String format) throws IOException, ParseException {
		JSONObject o = new JSONObject(httpPost(new URL("https://hastebin.com/documents"), text));
		return "https://hastebin.com/"+o.get("key")+(format!=null?"."+format:"");
	}
	
	public static String hastebin_Safe(String text, String format) {
		try {
			return hastebin(text, format);
		} catch (IOException | ParseException e) {
			return null;
		}
	}
	
	public static String hastebin_Safe(String text) {
		try {
			return hastebin(text);
		} catch (IOException | ParseException e) {
			return null;
		}
	}
	
	public static String httpPost(URL url, String... params) throws IOException {
		String urlParameters = Arrays.stream(params).collect(Collectors.joining("&"));
		byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
		int postDataLength = postData.length;
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();           
		conn.setDoOutput(true);
		conn.setInstanceFollowRedirects(false);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
		conn.setRequestProperty("charset", "utf-8");
		conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
		conn.setUseCaches(false);
		DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
		wr.write(postData);
		InputStream in = conn.getInputStream();
		byte[] dat = new byte[1024];
		in.read(dat);
		return new String(dat).trim();
	}

}
