package edu.columbia.tweetsentiment.sqs;

import java.util.concurrent.Callable;

import org.json.JSONObject;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;

import edu.columbia.tweetsentiment.model.Tweet;
import edu.columbia.tweetsentiment.utils.AlchemySentimentAnalyzer;
import edu.columbia.tweetsentiment.utils.Constants;


public class SQSWorker implements Callable<Tweet>{
	
	private String message;
	private AlchemySentimentAnalyzer analyzer;
	private AmazonSNSClient amazonSNSClient;

	public SQSWorker(String msg, AlchemySentimentAnalyzer analyzer, AmazonSNSClient amazonSNSClient){
		this.message = msg;
		this.analyzer = analyzer;
		this.amazonSNSClient = amazonSNSClient;
	}

	@Override
	public Tweet call() throws Exception {
		String body = message;
		String[] tokens = body.split(":");
		String sentiment = analyzer.getSentiment(tokens[1]);
		
		System.out.println("username:" + tokens[0]);
		System.out.println("message:" + tokens[1]);
		System.out.println("sentiment:" + sentiment);
		
		Tweet tweet = new Tweet(tokens[1], tokens[0], sentiment);
		publishResult(tweet);
		return tweet;
	}
	
	private void publishResult(Tweet result) {
		JSONObject tweetObj = new JSONObject();
		tweetObj.put(Constants.CONTENT_FIELD, result.getContent());
		tweetObj.put(Constants.USERNAME_FIELD, result.getUserName());
		tweetObj.put(Constants.SENTIMENT_FIELD, result.getSentiment());
		
		PublishRequest req = new PublishRequest();
		req.setSubject(Constants.SUBJECT);
		req.setMessage(tweetObj.toString());
		req.setTopicArn(Constants.SNS_ENDPOINT);
		PublishResult r = amazonSNSClient.publish(req);
		System.out.println("published to amazon SNS " + tweetObj.toString());
	}
}
