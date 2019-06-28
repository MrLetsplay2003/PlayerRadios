package me.mrletsplay.playerradios.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import me.mrletsplay.mrcore.http.HttpGeneric;
import me.mrletsplay.mrcore.http.HttpRequest;
import me.mrletsplay.mrcore.json.JSONArray;
import me.mrletsplay.mrcore.json.JSONObject;

public class PasteText {
	
	public static String glotSafe(String... files) {
		try {
			return glotSnippet(files);
		} catch (IOException e) {
			return null;
		}
	}
	
	public static String glotSnippet(String... files) throws IOException {
		if(files.length % 2 != 0) throw new IllegalArgumentException();
		JSONObject o = new JSONObject();
		o.put("language", "plaintext");
		o.put("title", "bugreport");
		o.put("public", false);
		JSONArray fa = new JSONArray();
		for(int i = 0; i < files.length; i += 2) {
			JSONObject f = new JSONObject();
			f.put("name", files[i]);
			f.put("content", files[i + 1]);
			fa.add(f);
		}
		o.put("files", fa);
		HttpGeneric p = HttpRequest.createGeneric("POST", "https://snippets.glot.io/snippets");
		p.setHeaderParameter("Content-Type", "application/json");
		p.setContent(o.toString().getBytes(StandardCharsets.UTF_8));
		return "https://glot.io/snippets/" + p.execute().asJSONObject().getString("id");
	}
	
}
