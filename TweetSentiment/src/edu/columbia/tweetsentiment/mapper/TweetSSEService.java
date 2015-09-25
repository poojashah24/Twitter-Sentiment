package edu.columbia.tweetsentiment.mapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Session;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.json.JSONObject;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.ConfirmSubscriptionRequest;
import com.amazonaws.services.sns.model.ConfirmSubscriptionResult;

import edu.columbia.tweetsentiment.common.Constants;
import edu.columbia.tweetsentiment.model.Tweet;
import edu.columbia.tweetsentiment.model.TweetList;
import edu.columbia.tweetsentiment.sns.SNSMessage;
import edu.columbia.tweetsentiment.sns.SNSMessageType;
import edu.columbia.tweetsentiment.sns.SNSUtils;

@Path("/")
public class TweetSSEService extends HttpServlet{
	private static Connection connection;
	private TweetList tweetList = new TweetList();
	private Session session = null;
	private int lastRead = 0;
	private static EventOutput eventOuput;// = new EventOutput();
	private static final ScheduledExecutorService sch = Executors.newSingleThreadScheduledExecutor();
	private static Logger logger;
		
	public TweetSSEService() {
	}
	
	@GET
	@Produces("text/event-stream")
	@Path("/hang/{filter}")
	public EventOutput getMessages(@PathParam("filter") String filter) {
		
		System.out.println("client request!" + filter);
		if(connection != null) {
			System.out.println("The connection is " + connection.toString());
		}
		
		String[] params = filter.split(":");
		eventOuput = new EventOutput();
		lastRead = Integer.parseInt(params[params.length-1]);
		SearchTweetTask tweetTask = new SearchTweetTask(connection, eventOuput, filter, lastRead);
		sch.scheduleWithFixedDelay(tweetTask , 0, 15, TimeUnit.SECONDS);
		return eventOuput;
	}
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		String log4jLocation = config.getInitParameter("log4j-properties-location");
		if (log4jLocation == null) {
			System.err.println("*** No log4j-properties-location init param, so initializing log4j with BasicConfigurator");
			BasicConfigurator.configure();
		} else {
			System.out.print("found log4j config");
			ServletContext sc = config.getServletContext();
			String webAppPath = sc.getRealPath("/");
			String log4jProp = webAppPath + log4jLocation;
			File loggingConfig = new File(log4jProp);
			if (loggingConfig.exists()) {
				System.out.println("Initializing log4j with: " + log4jProp);
				PropertyConfigurator.configure(log4jProp);
			} else {
				System.err.println("*** " + log4jProp + " file not found, so initializing log4j with BasicConfigurator");
				BasicConfigurator.configure();
			}
			logger = Logger.getLogger(TweetSSEService.class);
		}

		String jdbcUrl = "jdbc:mysql://" + edu.columbia.tweetsentiment.common.Constants.DB_HOSTNAME + ":" + 
				edu.columbia.tweetsentiment.common.Constants.DB_PORT + "/" + 
				edu.columbia.tweetsentiment.common.Constants.DB_NAME + "?user=" + 
				edu.columbia.tweetsentiment.common.Constants.DB_USERNAME + "&password=" + 
				edu.columbia.tweetsentiment.common.Constants.DB_PASSWORD;
		
		try {
			//System.out.println("Loading driver...");
			logger.info("Loading driver in TweetSSEService...");
			Class.forName("com.mysql.jdbc.Driver");
			//System.out.println("Driver loaded!");
			logger.info("Driver loaded in TweetSSEService!");
			connection = DriverManager.getConnection(jdbcUrl);
			if (connection != null) {
				System.out.println("Connected to RDS instance in TweetSSEService");
				logger.info("Connected to RDS instance");
				System.out.println(connection.toString());
			} else {
				System.out.println("Could not connect to the db in TweetSSEService");
			}
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(
					"Cannot find the driver in the classpath!", e);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("Error occurred while connecting to db");
			e.printStackTrace();
		}
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setStatus(200);
		response.getWriter().write("doGet called!");
		response.flushBuffer();
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setStatus(200);
		response.getWriter().write("doPost called!");
		logger.info("doPost called!");
		String messageType = request.getHeader("x-amz-sns-message-type");
		if(messageType == null) {
			System.out.println("This message hasn't been sent by AWS");
			logger.info("This message hasn't been sent by AWS");
			return;
		} else { 
			System.out.println("------>Got a message from AWS!");
			logger.info("------>Got a message from AWS!");
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
		StringBuilder builder = new StringBuilder();
		String ip = null;
		while((ip = reader.readLine()) != null) {
			builder.append(ip);
		}
		
		logger.info("Before creating SNS message");
		SNSMessage snsMessage = SNSUtils.createSNSMessage(builder.toString(), messageType); 
		logger.info("After creating SNS message");
		
		if(snsMessage.getSignatureVersion().equals("1")) {
			if(SNSUtils.isMessageSignatureValid(snsMessage)) {
				logger.info(">>Signature verification succeeded");
			} else {
				logger.info(">>Signature verification failed");
				throw new SecurityException("Signature verification failed.");
			}
		} else {
			logger.info(">>Unexpected signature version. Unable to verify signature.");
			throw new SecurityException("Unexpected signature version. Unable to verify signature.");
		}
		
		logger.info("After verifying signature");
		
		if (messageType.equals(SNSMessageType.SNS_NOTIFICATION.toString())) {
			//TODO: Do something with the Message and Subject.
			//Just log the subject (if it exists) and the message.
			String logMsgAndSubject = ">>Notification received from topic " + snsMessage.getTopicArn();
			if (snsMessage.getSubject() != null)
				logMsgAndSubject += " Subject: " + snsMessage.getSubject();
			
			JSONObject sentimentMsg = new JSONObject(snsMessage.getMessage());
			String sentiment = sentimentMsg.getString(edu.columbia.tweetsentiment.mapper.Constants.SENTIMENT_FIELD);
			String content = sentimentMsg.getString(edu.columbia.tweetsentiment.mapper.Constants.CONTENT_FIELD);
			String userName = sentimentMsg.getString(edu.columbia.tweetsentiment.mapper.Constants.USERNAME_FIELD);
			
			try {
				System.out.println("sentiment:"+sentiment);
				System.out.println("content:"+content);
				System.out.println("username:"+userName);
				
				String query = edu.columbia.tweetsentiment.mapper.Constants.UPDATE_TEMP_TWEETS;
				//query += "\'%" + content + "%\'" + Constants.UPDATE_TEMP_TWEETS_USERNAME;
				PreparedStatement statement = connection.prepareStatement(query);
				statement.setString(1, sentiment);
				statement.setInt(2, Integer.parseInt(userName.trim()));
				
				System.out.println("update statement:"+statement);
				int res = statement.executeUpdate();
				System.out.println("number of rows updated:" + res);
				System.out.println("updated the sentiment in the db");
			}catch(SQLException e) {
				e.printStackTrace();
			}
			logMsgAndSubject += " Message: " + snsMessage.getMessage();
			logger.info(logMsgAndSubject);
				
		} else if(messageType.equals(SNSMessageType.SNS_SUBSCRIPTION.toString())) {
			//TODO: You should make sure that this subscription is from the topic you expect. Compare topicARN to your list of topics 
			//that you want to enable to add this endpoint as a subscription.

			//Confirm the subscription by going to the subscribeURL location 
			//and capture the return value (XML message body as a string)
			logger.info("Got subscription message!");

			Scanner sc = new Scanner(new URL(snsMessage.getSubscribeURL()).openStream());
			StringBuilder sb = new StringBuilder();
			while (sc.hasNextLine()) {
				sb.append(sc.nextLine());
			}
			logger.info(">>Subscription confirmation (" + snsMessage.getSubscribeURL() +") Return value: " + sb.toString());
			//TODO: Process the return value to ensure the endpoint is subscribed.
			confirmTopicSubmission(snsMessage);
		} else if(messageType.equals(SNSMessageType.SNS_UNSUBSCRIBE.toString())) {
			//TODO: Handle UnsubscribeConfirmation message. 
			//For example, take action if unsubscribing should not have occurred.
			//You can read the SubscribeURL from this message and 
			//re-subscribe the endpoint.
			logger.info(">>Unsubscribe confirmation: " + snsMessage.getMessage());
		} else {
			logger.info(">>Unknown message type.");
		}
		response.flushBuffer();
		logger.info(">>Done processing message: " + snsMessage.getMessageId());
	}
	
	public void confirmTopicSubmission(SNSMessage message) {
		AWSCredentials credentials = new BasicAWSCredentials("AKIAI2PCLP7B56QYHWUA", "Lu3+QL6P7cSFwl/IT4/l5vnXPOxzGM5dbHgPe7rP");
		AmazonSNSClient amazonSNSClient = new AmazonSNSClient(credentials);
		
		ConfirmSubscriptionRequest confirmSubscriptionRequest = new ConfirmSubscriptionRequest()
		 							.withTopicArn(message.getTopicArn())
									.withToken(message.getToken());
		ConfirmSubscriptionResult resutlt = amazonSNSClient.confirmSubscription(confirmSubscriptionRequest);
		logger.info("subscribed to " + resutlt.getSubscriptionArn());		
	}
}

class SearchTweetTask implements Runnable {
	private Connection connection;
	private EventOutput eventOutput;
	private String filter;
	private int lastRead = 0;
	private TweetList tweetList = new TweetList();
	Map<String, List<Integer>> cntMap = new HashMap<String, List<Integer>>();

	private final String[] tags = { "%music%", "%apple%", "%love%",
		"%friends%", "%samsung%", "%holiday%", "%party%", "%nyc%" };
	
	public SearchTweetTask(Connection connection, EventOutput eventOutput, String filter, int lastRead) {
		this.connection = connection;
		this.eventOutput = eventOutput;
		this.filter = filter;
		this.lastRead = lastRead;
	}
	
	public void run() {
		String[] params = filter.split(":");
		String countJSON = null;
		//lastRead = Integer.parseInt(params[1]);
		System.out.println("lastRead:"+lastRead);
		
		try {
			PreparedStatement statement = null;
			String query = new String();
			System.out.print("params.length:" + params.length);
			if(params[0].equals("all"))
				if(connection == null) {
					System.out.println("connection is null");
				}
				else {
					statement = connection.prepareStatement(edu.columbia.tweetsentiment.mapper.Constants.FETCH_TWEETS);
				}			
			else {
				if(params.length == 2 && !params[0].trim().isEmpty()) {
					query = edu.columbia.tweetsentiment.mapper.Constants.FETCH_TWEETS_TAG + "\'%" + params[0] + "%\'";	
				} else if(params.length > 2){
					query = edu.columbia.tweetsentiment.mapper.Constants.FETCH_TWEETS_TAG_MULTI + edu.columbia.tweetsentiment.mapper.Constants.OPENING_BRACE;
					String clause = new String();
					for(int i=0; i<params.length-2; i++) {
						clause += edu.columbia.tweetsentiment.mapper.Constants.LIKE_CLAUSE + "\'%" + params[i] + "%\'";
						clause += edu.columbia.tweetsentiment.mapper.Constants.OR_CLAUSE;
					}
					clause += edu.columbia.tweetsentiment.mapper.Constants.LIKE_CLAUSE + "\'%" + params[params.length-2] + "%\'";
					query += clause;
					query += edu.columbia.tweetsentiment.mapper.Constants.CLOSING_BRACE;
				}
				else {
					System.out.println("No tag selected");
					OutboundEvent.Builder b = new OutboundEvent.Builder();
					b.mediaType(MediaType.APPLICATION_JSON_TYPE);
					b.data(String.class, "");
					OutboundEvent e = b.build();
					try {
						eventOutput.write(e);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					return;
				}
				statement = connection.prepareStatement(query);
			}
			System.out.println("query:" + query);
			statement.setInt(1, lastRead);
			System.out.println(statement.toString());
			ResultSet rs = statement.executeQuery();
			while(rs.next()) {
				//lastRead++;
				int id = rs.getInt("tweet_id");
				String username = rs.getString("username");
				String content = rs.getString("content").replaceAll("\\r\\n|\\r|\\n", " ");
				content = content.replaceAll("\r\n", " ");
				content = content.replace("\r"," ");
				content = content.replaceAll(" +"," ");	
				
				if(content.contains("\\")){
					System.out.println("content--->"+content);
					content = content.replace("\\", "");
					System.out.println("changed content--->"+content);

				}
				
				if(content.contains("\"")) {
					content = content.replaceAll("\"", "\\\\\"");
				}

				content = content.trim();
								
				double latitude = rs.getDouble("latitude");
				double longitude = rs.getDouble("longitude");
				String sentiment = rs.getString("sentiment");
				Date date = rs.getDate("tweet_ts");
				
				Tweet t = new Tweet(id, username, content, date, latitude, longitude, sentiment);
				tweetList.add(t);
				lastRead = rs.getInt("tweet_id");
				if(tweetList.size() == 1500)
					break;
				
			}
			PreparedStatement ps_positive = connection.prepareStatement(edu.columbia.tweetsentiment.mapper.Constants.GET_COUNT_POSITIVE);
			PreparedStatement ps_negative = connection.prepareStatement(edu.columbia.tweetsentiment.mapper.Constants.GET_COUNT_NEGATIVE);
			StringBuilder builder = new StringBuilder();
			builder.append(edu.columbia.tweetsentiment.common.Constants.COUNT_LIST_START);

			for(String tag : tags) {
				ps_positive.setString(1, tag);
				System.out.println("ps_positive:" + ps_positive.toString());
				ResultSet res = ps_positive.executeQuery();
				
				int positiveCount = 0;
				int negativeCount = 0;
				
				if(res.next()) {
					positiveCount = res.getInt(1);					
				}
				
				ps_negative.setString(1, tag);
				System.out.println("ps_negative:" + ps_negative.toString());
				res = ps_negative.executeQuery();
				if(res.next()) {
					negativeCount = res.getInt(1);
				}
				
				String t = MessageFormat.format(edu.columbia.tweetsentiment.common.Constants.TAG, tag); 
				String pCount = MessageFormat.format(edu.columbia.tweetsentiment.common.Constants.POS_COUNT, positiveCount);
				String nCount = MessageFormat.format(edu.columbia.tweetsentiment.common.Constants.NEG_COUNT, negativeCount);
				String cJSON = "{"
						+ MessageFormat
								.format(edu.columbia.tweetsentiment.common.Constants.COUNT_JSON,
										t, pCount, nCount) + "}";
				builder.append(cJSON + ",");
			}
			countJSON = builder.substring(0, builder.length()-1);
			countJSON += Constants.COUNT_LIST_END + ",";
			System.out.println("countJSON: "+countJSON);
			
		}
		catch(SQLException sqle) {
			sqle.printStackTrace();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		if (tweetList != null && !tweetList.isEmpty()) {
			//String json = tweetList.toString();
			String json = countJSON + tweetList.toString();
			tweetList.clear();
			OutboundEvent.Builder b = new OutboundEvent.Builder();
			b.mediaType(MediaType.APPLICATION_JSON_TYPE);
			b.data(String.class, json);
			OutboundEvent e = b.build();
			try {
				System.out.println("sending output:lastRead" + lastRead);
				eventOutput.write(e);
				System.out.println("sent output:lastRead" + lastRead);
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch(Exception e1) {
				e1.printStackTrace();
			}
		}
		else {
			OutboundEvent.Builder b = new OutboundEvent.Builder();
			b.mediaType(MediaType.APPLICATION_JSON_TYPE);
			b.data(String.class, "");
			OutboundEvent e = b.build();
			try {
				eventOutput.write(e);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
}
