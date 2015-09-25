package edu.columbia.tweetsentiment.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import org.json.JSONObject;

public class AlchemySentimentAnalyzer implements ISentimentAnalyzer {

	private static Random r = new Random();
	@Override
	public String getSentiment(String message) {
		StringBuffer buffer = null;
		String sentimentType = null;
		try {
			StringBuffer buff = new StringBuffer();
			buff.append("apikey=");
			buff.append("&");
			buff.append("text=" + message);
			buff.append("&").append("outputMode=json");
			
			URL url = new URL(Constants.QUERY_URL);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod(Constants.METHOD);
			conn.setRequestProperty(Constants.CONTENT_TYPE, Constants.CONTENT_TYPE_VALUE);
			conn.setRequestProperty(Constants.CONTENT_LENGTH, "" + Integer.toString(buff.toString().getBytes().length));
		    conn.setRequestProperty(Constants.CONTENT_LANGUAGE, Constants.CONTENT_LANGUAGE_VALUE); 
		    conn.setDoOutput(true);
		    
		    DataOutputStream ds = new DataOutputStream(conn.getOutputStream());
		    ds.writeBytes(buff.toString());
		    ds.flush();
		    ds.close();
		    
			int res = conn.getResponseCode();
			System.out.println("res:" + res);
			
			String ip = null;
			buffer = new StringBuffer();
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));			
			while((ip = reader.readLine()) != null) {
				buffer.append(ip);
			}
			reader.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		try {
			System.out.println(buffer.toString());
			JSONObject obj = new JSONObject(buffer.toString());
			String status = obj.getString("status");
			if(!status.equalsIgnoreCase("ERROR")) {
				JSONObject sentiment = obj.getJSONObject("docSentiment");
				System.out.println(sentiment.toString());
				if(sentiment != null) {
					String score = null;
					sentimentType = sentiment.getString("type");
					if(!sentimentType.equalsIgnoreCase("neutral")) {
						 score = sentiment.getString("score");
					}				
					System.out.println("sentimentType:" +sentimentType);
					System.out.println("score:" +score);
				}
			}			
		} catch(Exception e) {
			e.printStackTrace();
		}

		
		return sentimentType;
		/*try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		int res = r.nextInt(2);
		System.out.println("res is:" + res);
		return res == 1 ? "positive" : "negative";*/
	}
	
}
