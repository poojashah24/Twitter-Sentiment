package edu.columbia.tweetsentiment.model;

public class Tweet {
	private String content;
	private String userName;
	private String sentiment;
	
	public Tweet(String content, String userName, String sentiment) {
		this.content = content;
		this.userName = userName;
		this.sentiment = sentiment;
	}

	public String getContent() {
		return content;
	}

	public String getUserName() {
		return userName;
	}

	public String getSentiment() {
		return sentiment;
	}
}
