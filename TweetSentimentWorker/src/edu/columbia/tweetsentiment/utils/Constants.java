package edu.columbia.tweetsentiment.utils;

public final class Constants {
	public static final String API_KEY = "";
	public static final String QUERY_URL = "http://access.alchemyapi.com/calls/text/TextGetTextSentiment";
	public static final String METHOD = "POST";
	public static final String CONTENT_TYPE = "Content-Type";
	public static final String CONTENT_LENGTH = "Content-Length";
	public static final String CONTENT_LANGUAGE = "Content-Language";
	public static final String CONTENT_TYPE_VALUE = "application/x-www-form-urlencoded";
	public static final String CONTENT_LANGUAGE_VALUE = "en-US";
	public static final String OUTPUT_MODE = "outputMode=json";
	public static final String POSITIVE = "positive";
	public static final String NEGATIVE = "negative";
	public static final String NEUTRAL = "neutral";
	
	public static final String SNS_ENDPOINT = "arn:aws:sns:us-east-1:494645163223:TweetSentiment";
	public static final String SUBJECT = "Sentiment Update";
	public static final String CONTENT_FIELD = "content";
	public static final String USERNAME_FIELD = "username";
	public static final String SENTIMENT_FIELD = "sentiment";
	
}
