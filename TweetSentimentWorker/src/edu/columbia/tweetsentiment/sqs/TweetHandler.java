package edu.columbia.tweetsentiment.sqs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.json.JSONObject;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

import edu.columbia.tweetsentiment.model.Tweet;
import edu.columbia.tweetsentiment.utils.AlchemySentimentAnalyzer;
import edu.columbia.tweetsentiment.utils.Constants;

public class TweetHandler extends TimerTask implements ServletContextListener{
	private static AmazonSQS sqs;
	private static ExecutorService executor = Executors.newFixedThreadPool(50);
	private static AlchemySentimentAnalyzer analyzer = new AlchemySentimentAnalyzer();
	private static AmazonSNSClient amazonSNSClient;
	
	//private static List<Future<Tweet>> futuresList = new ArrayList<Future<Tweet>>();
	private static List<Future<Tweet>> futuresList = new CopyOnWriteArrayList<Future<Tweet>>();
	/*private static int positiveTweetsCount = 0;
	private static int negativeTweetsCount = 0;*/
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		initSQS();
		initSNS();
		initFetch();
		//initResultFetch();
		
	}
	
	public void initSQS() {
		System.out.println("before initSQS");
		try {
			AWSCredentials credentials = new BasicAWSCredentials("", "");
			sqs = new AmazonSQSClient(credentials);
		} catch(Exception e) {
			e.printStackTrace();
		}
		System.out.println("after initSQS");
	}
	
	public void initSNS() {
		System.out.println("before initSNS");
		AWSCredentials credentials = new BasicAWSCredentials("", "");
		amazonSNSClient = new AmazonSNSClient(credentials);
		System.out.println("after initSNS");
	}
	
	public void initFetch() {
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(this, 0, 5*1000);
	}
	
	/*public void initResultFetch() {
		Timer timer = new Timer();
		TweetResultPoller poller = new TweetResultPoller();
		timer.scheduleAtFixedRate(poller, 0, 15*1000);
	}*/

	@Override
	public synchronized void run() {
		//System.out.println("before run");
		ReceiveMessageResult result = sqs.receiveMessage("https://sqs.us-east-1.amazonaws.com/494645163223/TweetQueue");
		List<Message> messages = result.getMessages();
		for (Message m : messages) {
			SQSWorker worker = new SQSWorker(m.getBody(), analyzer, amazonSNSClient);
			Future<Tweet> f = executor.submit(worker);
			futuresList.add(f);
			
			sqs.deleteMessage(new DeleteMessageRequest(
					"https://sqs.us-east-1.amazonaws.com/494645163223/TweetQueue",
					m.getReceiptHandle()));
		}		
		//System.out.println("after run");
	}
	
	/*class TweetResultPoller extends TimerTask {

		@Override
		public synchronized void run() {
			List<Future<Tweet>> toRemove = new ArrayList<Future<Tweet>>();
			System.out.println("--->Running result fetch timer now!");			
			
			//for(Future<Tweet> f : futuresList){
			try {
				//Iterator<Future<Tweet>> iter = futuresList.iterator();
				//while(iter.hasNext()) {
				for(Future<Tweet> f : futuresList) {
					//Future<Tweet> f = iter.next();
					if(f.isDone()) {
						System.out.println("found a result");					
						Tweet result = f.get();
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
						toRemove.add(f);
						//iter.remove();
					}
				}
				for(Future<Tweet> f : toRemove) {
					futuresList.remove(f);
				}
				//futuresList.removeAll(toRemove);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			} catch(ConcurrentModificationException e) {
				e.printStackTrace();
			} catch(Exception e) {
				e.printStackTrace();
			}
			//futuresList.removeAll(toRemove);
		}
	}*/

	
}
