package edu.columbia.tweetsentiment.fetcher;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;

import edu.columbia.tweetsentiment.model.Tweet;
import edu.columbia.tweetsentiment.model.TweetList;
import twitter4j.FilterQuery;
import twitter4j.HashtagEntity;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Servlet implementation class StartupServlet
 */
public class StartupServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger("StartupServlet");
	private static Connection connection;
	private static AmazonSQS sqs;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public StartupServlet() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
		initDb();
		initSQS();
		fetchTweets();
	}

	private boolean initDb() {
		String jdbcUrl = "jdbc:mysql://" + edu.columbia.tweetsentiment.common.Constants.DB_HOSTNAME + ":" + 
				edu.columbia.tweetsentiment.common.Constants.DB_PORT + "/" + 
				edu.columbia.tweetsentiment.common.Constants.DB_NAME + "?user=" + 
				edu.columbia.tweetsentiment.common.Constants.DB_USERNAME + "&password=" + 
				edu.columbia.tweetsentiment.common.Constants.DB_PASSWORD;

		// Load the JDBC Driver
		try {
			System.out.println("Loading driver...");
			Class.forName("com.mysql.jdbc.Driver");
			System.out.println("Driver loaded!");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(
					"Cannot find the driver in the classpath!", e);
		}

		try {
			connection = DriverManager.getConnection(jdbcUrl);
			if (connection != null) {
				System.out.println("Connected to RDS instance");
				java.util.logging.Logger.getLogger("WorkerServlet").info(
						"Connected to RDS instance");
			}
			java.sql.Statement setupStatement = null;

			try {
				setupStatement = connection.createStatement();
				setupStatement.execute(QueryConstants.CREATE_TAGS_QUERY);
				setupStatement.execute(QueryConstants.CREATE_QUERY);
				setupStatement.close();

			} catch (SQLException ex) {
				ex.printStackTrace();
				System.out.println("SQLException: " + ex.getMessage());
				System.out.println("SQLState: " + ex.getSQLState());
				System.out.println("VendorError: " + ex.getErrorCode());
			} finally {
				// System.out.println("Closing the connection.");
				// if (conn != null) try { conn.close(); } catch (SQLException
				// ignore) {}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	private void initSQS() {
		try {
			/*AWSCredentials credentials = new PropertiesCredentials(
					StartupServlet.class.getResourceAsStream("credentials.properties"));*/
			AWSCredentials credentials = new BasicAWSCredentials("", "");
			sqs = new AmazonSQSClient(credentials);
		} catch(Exception e) {
			e.printStackTrace();
		}
	
		sqs.sendMessage("https://sqs.us-east-1.amazonaws.com/494645163223/TweetQueue", "Hi there");		
	}
	
	private void fetchTweets() {
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true).setOAuthConsumerKey(Constants.O_AUTH_CONSUMER_KEY)
				.setOAuthConsumerSecret(Constants.O_AUTH_CONSUMER_SECRET)
				.setOAuthAccessToken(Constants.O_AUTH_ACCESS_TOKEN)
				.setOAuthAccessTokenSecret(Constants.O_AUTH_ACCESS_TOKEN_SECRET);

		TweetList tweetList = new TweetList();
		TwitterStream twitterStream = new TwitterStreamFactory(cb.build())
				.getInstance();
		StatusListener listener = new TwitterStatusListener(connection, sqs,
				tweetList);
		TwitterFetcher fetcher = new TwitterFetcher(twitterStream);
		twitterStream.addListener(listener);
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(fetcher, 0, 210 * 1000);
	}
}

class TwitterStatusListener implements StatusListener {
	private TweetList tweetList;
	Connection connection = null;
	AmazonSQS sqs;
	Logger logger = Logger.getLogger("TwitterStatusListener");
	private int tweet_id;

	public TwitterStatusListener(Connection con, AmazonSQS sqs, TweetList tweetList) {
		this.connection = con;
		this.sqs = sqs;
		this.tweetList = tweetList;

		try {
			Statement getStartIndexStatement = connection.createStatement();
			getStartIndexStatement.execute(QueryConstants.GET_INDEX);
			ResultSet rs = getStartIndexStatement.getResultSet();
			if (rs.next()) {
				tweet_id = rs.getInt("maxid") + 1;
			}
			System.out.println("tweet_id:" + tweet_id);
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void onStatus(Status status) {
		if (status.getGeoLocation() != null) {
			System.out.println("@" + status.getUser().getScreenName() + " - "
					+ status.getText());
			HashtagEntity[] he = status.getHashtagEntities();
			for (HashtagEntity e1 : he) {
				System.out.println("hashtag:" + e1.getText());
			}
			System.out.println("@geo-location: "
					+ status.getGeoLocation().getLatitude() + " "
					+ status.getGeoLocation().getLongitude());

			Tweet tweet = new Tweet(status.getUser().getScreenName(),
					status.getText(), status.getCreatedAt(), status
							.getGeoLocation().getLatitude(), status
							.getGeoLocation().getLongitude());

			for (HashtagEntity tagEntity : he) {
				tweet.addTag(tagEntity.getText());
			}

			tweetList.add(tweet);
			//if (tweetList.size() >= 5) {
				System.out.println("Sending to DB now");
				PreparedStatement insertStatement = null;
				PreparedStatement insertTagStatement = null;
				try {
					insertStatement = connection
							.prepareStatement(QueryConstants.INSERT_QUERY);
					insertTagStatement = connection
							.prepareStatement(QueryConstants.INSERT_TAG_QUERY);
					for (Tweet t : tweetList) {
						insertStatement.setInt(1, tweet_id);
						insertStatement.setString(2, t.getUserName());
						insertStatement.setString(3, t.getContent());
						insertStatement.setDouble(4, t.getLatitude());
						insertStatement.setDouble(5, t.getLongitude());
						insertStatement.setDate(6, t.getDate());
						insertStatement.setString(7, "");

						insertStatement.addBatch();

						for (String tag : t.getTags()) {
							insertTagStatement.setInt(1, tweet_id);
							insertTagStatement.setString(2, tag);

							insertTagStatement.addBatch();
						}
						sqs.sendMessage("https://sqs.us-east-1.amazonaws.com/494645163223/TweetQueue", tweet_id+":"+t.getContent());
						tweet_id++;
					}
					System.out.println("insert statement:"+insertStatement);
					int[] res = insertStatement.executeBatch();
					int[] res1 = insertTagStatement.executeBatch();
					System.out.println("result on insertion:" + res);
					insertStatement.close();
				} catch (SQLException sqle) {
					sqle.printStackTrace();
					logger.info("DB error occurred while inserting");
				} finally {
					tweetList.clear();
				}

			//}
		}

	}

	@Override
	public void onDeletionNotice(StatusDeletionNotice arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onScrubGeo(long arg0, long arg1) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onException(Exception arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onStallWarning(StallWarning arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onTrackLimitationNotice(int arg0) {
		// TODO Auto-generated method stub
	}
}

class TwitterFetcher extends TimerTask {

	private TwitterStream twitterStream;

	public TwitterFetcher(TwitterStream twitterStream) {
		this.twitterStream = twitterStream;
	}

	@Override
	public void run() {
		try {
			FilterQuery fd = new FilterQuery();
			String[] keywords = { "#music", "#apple", "#love", "#friends",
					"#samsung", "#holiday", "#party", "#nyc" };
			//String[] keywords = {"#love"};
			fd.track(keywords);

			twitterStream.filter(fd);
			Thread.sleep(180 * 1000);
			System.out.println("shutting down now");
			twitterStream.cleanUp();
			twitterStream.shutdown();
		} catch (Exception e) {
			System.err.println(e);
		}
	}

}
