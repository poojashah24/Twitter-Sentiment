package edu.columbia.tweetsentiment.mapper;

public final class Constants {
	public static final String FETCH_TWEETS = "SELECT * from TWEETS WHERE tweet_id > ? ORDER BY tweet_id";
	public static final String FETCH_TWEETS_TAG = "SELECT * from TWEETS, TAGS WHERE TWEETS.tweet_id > ? AND TWEETS.tweet_id = TAGS.tweet_id AND TAGS.tag like ";
	public static final String FETCH_TWEETS_TAG_MULTI = "SELECT * from TWEETS, TAGS WHERE TWEETS.tweet_id > ? AND TWEETS.tweet_id = TAGS.tweet_id AND ";
	public static final String OPENING_BRACE = "(";
	public static final String CLOSING_BRACE = ")";
	public static final String LIKE_CLAUSE = "TAGS.tag like ";
	public static final String OR_CLAUSE = " OR ";
	
	public static final String CONTENT_FIELD = "content";
	public static final String USERNAME_FIELD = "username";
	public static final String SENTIMENT_FIELD = "sentiment";
	
	//public static final String UPDATE_TEMP_TWEETS = "update TEMP_TWEETS set sentiment = ? where content like ";
	public static final String UPDATE_TEMP_TWEETS = "update TEMP_TWEETS set sentiment = ? where tweet_id = ?;";
	public static final String UPDATE_TEMP_TWEETS_USERNAME = " AND username = ?";

	public static final String GET_COUNT_POSITIVE = "select count(*) as tweet_count from TWEETS, TAGS where TWEETS.sentiment = 'positive' and TWEETS.tweet_id = TAGS.tweet_id and TAGS.tag like ?;";
	public static final String GET_COUNT_NEGATIVE = "select count(*) as tweet_count from TWEETS, TAGS where TWEETS.sentiment = 'negative' and TWEETS.tweet_id = TAGS.tweet_id and TAGS.tag like ?;";
}
