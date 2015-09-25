package edu.columbia.tweetsentiment.common;

public final class Constants {
	//public static final String TWEET_LIST_START = "{\"tweet\": [";
	public static final String TWEET_LIST_START = "\"tweet\": [";
	public static final String TWEET_LIST_END = "]}";
	public static final String TWEET_JSON = "{0},{1},{2},{3},{4},{5}";
	public static final String LATITUDE = "\"latitude\":\"{0}\"";
	public static final String LONGITUDE = "\"longitude\":\"{0}\"";	
	public static final String ID = "\"id\":\"{0,number,#}\"";
	public static final String USERNAME = "\"username\":\"{0}\"";
	public static final String CONTENT = "\"content\":\"{0}\"";
	public static final String SENTIMENT = "\"sentiment\":\"{0}\"";
	
	public static final String COUNT_LIST_START = "{\"counts\": [";
	public static final String COUNT_LIST_END = "]";
	public static final String TAG = "\"tag\":\"{0}\"";
	public static final String POS_COUNT = "\"positive_count\":\"{0}\"";
	public static final String NEG_COUNT = "\"negative_count\":\"{0}\"";
	public static final String COUNT_JSON = "{0},{1},{2}";
	
	public static final String DB_NAME = "tweetdatabase";
	public static final String DB_USERNAME = "tweetdbuser";
	public static final String DB_PASSWORD = "";
	public static final String DB_HOSTNAME = "tweetdb.ci8zhgzbiolo.us-east-1.rds.amazonaws.com";
	public static final String DB_PORT = "3306";
}
