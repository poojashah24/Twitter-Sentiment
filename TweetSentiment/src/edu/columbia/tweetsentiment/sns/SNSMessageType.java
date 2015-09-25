package edu.columbia.tweetsentiment.sns;


public enum SNSMessageType {
	SNS_NOTIFICATION(Constants.SNS_NOTIFICATION), 
	SNS_SUBSCRIPTION(Constants.SNS_SUBSCRIPTION), 
	SNS_UNSUBSCRIBE(Constants.SNS_UNSUBSCRIBE);
	
	String message;
	private SNSMessageType(String message){
		this.message = message;
	}
	
	public String toString() {
		return this.message;
	}
}
