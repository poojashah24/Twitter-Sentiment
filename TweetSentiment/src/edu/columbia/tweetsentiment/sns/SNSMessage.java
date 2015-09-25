package edu.columbia.tweetsentiment.sns;

public class SNSMessage {
	
    private String type = "";
    private String messageId = "";
    private String token = "";
    private String topicArn = "";
    private String subject;
    private String message;
    private String subscribeURL = "";
    private String timestamp = "";
    private String signatureVersion = "";
    private String signature = "";
    private String signingCertUrl = "";
    private String unsubscribeURL = "";

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getMessageId() {
        return messageId;
    }
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }
    public String getTopicArn() {
        return topicArn;
    }
    public void setTopicArn(String topicArn) {
        this.topicArn = topicArn;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String theMessage) {
        this.message = theMessage;
    }
    public String getSubscribeURL() {
        return subscribeURL;
    }
    public void setSubscribeURL(String subscribeURL) {
        this.subscribeURL = subscribeURL;
    }
    public String getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    public String getSignatureVersion() {
        return signatureVersion;
    }
    public void setSignatureVersion(String signatureVersion) {
        this.signatureVersion = signatureVersion;
    }
    public String getSignature() {
        return signature;
    }
    public void setSignature(String signature) {
        this.signature = signature;
    }
    public String getSigningCertUrl() {
        return signingCertUrl;
    }
    public void setSigningCertUrl(String signingCertUrl) {
        this.signingCertUrl = signingCertUrl;
    }
    public String getUnsubscribeURL() {
        return unsubscribeURL;
    }
    public void setUnsubscribeURL(String unsubscribeURL) {
        this.unsubscribeURL = unsubscribeURL;
    }
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	
}
