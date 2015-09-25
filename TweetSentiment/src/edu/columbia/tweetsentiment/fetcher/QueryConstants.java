package edu.columbia.tweetsentiment.fetcher;

public final class QueryConstants {
	public static final String CREATE_QUERY = "CREATE TABLE TWEETS (tweet_id BIGINT, username VARCHAR(30) not null, content VARCHAR(500) not null, latitude DOUBLE not null, longitude DOUBLE not null, tweet_ts TIMESTAMP, primary key (tweet_id));";
	public static final String CREATE_TAGS_QUERY = "CREATE TABLE TAGS(tweet_id BIGINT not null, tag VARCHAR(30) not null, primary key(tweet_id, tag));";
	public static final String INSERT_QUERY = "insert into TEMP_TWEETS values (?, ?, ?, ?, ?, ?, ?);";	
	public static final String INSERT_TAG_QUERY = "insert into TEMP_TAGS values (?,?);";
	public static final String GET_INDEX = "select max(tweet_id) as maxid from TEMP_TWEETS;";
}
