package edu.columbia.tweetsentiment.sns;

import java.io.InputStream;
import java.net.URL;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.apache.tomcat.util.codec.binary.Base64;
import org.json.JSONObject;

public class SNSUtils {
	
	
	public static SNSMessage createSNSMessage(String jsonStr, String msgType) {
		SNSMessage snsMessage = null;
		try {
			JSONObject obj = new JSONObject(jsonStr);
			if(obj != null) {
				snsMessage = new SNSMessage();
				snsMessage.setType(obj.getString(Constants.SNS_TYPE));
				snsMessage.setMessageId(obj.getString(Constants.SNS_MSGID));
				if(msgType.equals(SNSMessageType.SNS_SUBSCRIPTION.toString())) {
					snsMessage.setToken(obj.getString(Constants.SNS_TOKEN));
					snsMessage.setSubscribeURL(obj.getString(Constants.SNS_SUBSCRIBE_URL));
				}			
				snsMessage.setTopicArn(obj.getString(Constants.SNS_TOPIC_ARN));			
				if(msgType.equals(SNSMessageType.SNS_NOTIFICATION.toString())) {
					snsMessage.setSubject(obj.getString(Constants.SNS_SUBJECT));				
					snsMessage.setUnsubscribeURL(obj.getString(Constants.SNS_UNSUBSCRIBE_URL));
				}				
				snsMessage.setMessage(obj.getString(Constants.SNS_MSG));
				snsMessage.setTimestamp(obj.getString(Constants.SNS_TS));
				snsMessage.setSignatureVersion(obj.getString(Constants.SNS_SIG_VERSION));
				snsMessage.setSignature(obj.getString(Constants.SNS_SIG));
				snsMessage.setSigningCertUrl(obj.getString(Constants.SNS_SIGN_CERT_URL));
				
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		
		return snsMessage;
	}
	
	public static boolean isMessageSignatureValid(SNSMessage msg) {

		try {
			URL url = new URL(msg.getSigningCertUrl());
			InputStream inStream = url.openStream();
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			X509Certificate cert = (X509Certificate)cf.generateCertificate(inStream);
			inStream.close();

			Signature sig = Signature.getInstance("SHA1withRSA");
			sig.initVerify(cert.getPublicKey());
			sig.update(getMessageBytesToSign(msg));
			return sig.verify(Base64.decodeBase64(msg.getSignature().getBytes()));
		}
		catch (Exception e) {
			throw new SecurityException("Verify method failed.", e);

		}
	}

	public static byte[] getMessageBytesToSign(SNSMessage msg) {

		byte [] bytesToSign = null;
		if (msg.getType().equals("Notification"))
			bytesToSign = buildNotificationStringToSign(msg).getBytes();
		else if (msg.getType().equals("SubscriptionConfirmation") || msg.getType().equals("UnsubscribeConfirmation"))
			bytesToSign = buildSubscriptionStringToSign(msg).getBytes();
		return bytesToSign;
	}
	
	//Build the string to sign for Notification messages.
	private static String buildNotificationStringToSign( SNSMessage msg) {
		String stringToSign = null;

		//Build the string to sign from the values in the message.
		//Name and values separated by newline characters
		//The name value pairs are sorted by name 
		//in byte sort order.
		stringToSign = "Message\n";
		stringToSign += msg.getMessage() + "\n";
		stringToSign += "MessageId\n";
		stringToSign += msg.getMessageId() + "\n";
		if (msg.getSubject() != null) {
			stringToSign += "Subject\n";
			stringToSign += msg.getSubject() + "\n";
		}
		stringToSign += "Timestamp\n";
		stringToSign += msg.getTimestamp() + "\n";
		stringToSign += "TopicArn\n";
		stringToSign += msg.getTopicArn() + "\n";
		stringToSign += "Type\n";
		stringToSign += msg.getType() + "\n";
		return stringToSign;
	}

	//Build the string to sign for SubscriptionConfirmation 
	//and UnsubscribeConfirmation messages.
	private static String buildSubscriptionStringToSign(SNSMessage msg) {
		String stringToSign = null;
		//Build the string to sign from the values in the message.
		//Name and values separated by newline characters
		//The name value pairs are sorted by name 
		//in byte sort order.
		stringToSign = "Message\n";
		stringToSign += msg.getMessage() + "\n";
		stringToSign += "MessageId\n";
		stringToSign += msg.getMessageId() + "\n";
		stringToSign += "SubscribeURL\n";
		stringToSign += msg.getSubscribeURL() + "\n";
		stringToSign += "Timestamp\n";
		stringToSign += msg.getTimestamp() + "\n";
		stringToSign += "Token\n";
		stringToSign += msg.getToken() + "\n";
		stringToSign += "TopicArn\n";
		stringToSign += msg.getTopicArn() + "\n";
		stringToSign += "Type\n";
		stringToSign += msg.getType() + "\n";
		return stringToSign;
	}
}
