package com.gravityrd.recengclient.webshop;
/**
 * The com.gravityrd.recengclient.webshop.GravityClient and related classes are used to connect to and communicate with the
 * Gravity recommendation engine.
 */

import com.gravityrd.receng.web.webshop.jsondto.GravityEvent;
import com.gravityrd.receng.web.webshop.jsondto.GravityItem;
import com.gravityrd.receng.web.webshop.jsondto.GravityItemRecommendation;
import com.gravityrd.receng.web.webshop.jsondto.GravityRecEngException;
import com.gravityrd.receng.web.webshop.jsondto.GravityRecommendationContext;
import com.gravityrd.receng.web.webshop.jsondto.GravityScenario;
import com.gravityrd.receng.web.webshop.jsondto.GravityUser;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * The com.gravityrd.recengclient.webshop.GravityClient class can be used to send events, item and user information to
 * the recommendation engine and get recommendations.
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * 		public com.gravityrd.recengclient.webshop.GravityClient createGravityClient() {
 * 			com.gravityrd.recengclient.webshop.GravityClient client = new com.gravityrd.recengclient.webshop.GravityClient();
 * 			client.setRemoteUrl("https://saas.gravityrd.com/grrec-CustomerID-war/WebshopServlet");
 * 			client.setUserName("sampleUser");
 * 			client.setPassword("samplePasswd");
 * 			return client;
 *        }
 * 		com.gravityrd.recengclient.webshop.GravityClient client = createGravityClient();
 * 		GravityRecommendationContext context = new GravityRecommendationContext();
 * 		context.numberLimit = 1;
 * 		context.scenarioId = "ITEM_PAGE";
 * 		client.getItemRecommendation("user1", context);
 * </pre>
 */
public final class GravityClient {

	private static final ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
	/**
	 * The version info of the client.
	 */
	@SuppressWarnings("FieldCanBeLocal")
	private final String VERSION = "1.2.4";
	/**
	 * The URL of the server side interface. It has no default value, must be specified.
	 */
	private String remoteUrl;
	/**
	 * The timeout for the operations in millisecs. The default value is 3000 millisecs.
	 */
	private int readTimeout = 3000;
	/**
	 * The user name for the http authenticated connection. Leave it blank in case of
	 * connection without authentication.
	 */
	private String userName;
	/**
	 * The password for the http authenticated connection. Leave it blank in case of
	 * connection without authentication.
	 */
	private String password;

	/**
	 * Query the list of available recommendation scenarios for the backend
	 * @return array of scenarios
	 * @throws GravityRecEngException cannot process answer
	 * @throws IOException cannot connect to server
	 */
	public GravityScenario[] getScenarioInformation() throws GravityRecEngException, IOException {
		return (GravityScenario[]) sendRequest("scenarioInfo", null, null, true, GravityScenario[].class);
	}

	private Object sendRequest(String methodName, Map<String, String> queryStringParams, Object requestBody, boolean hasAnswer, Class answerClass) throws GravityRecEngException, IOException {

		HttpURLConnection connection = sendRequest(methodName, queryStringParams, requestBody);

		if (connection.getResponseCode() / 100 != 2) {
			try {
				throw mapper.readValue(getBodyAsString(connection.getErrorStream()), GravityRecEngException.class);
			} catch (Exception e) {
				throw new IllegalStateException(getBodyAsString(connection.getErrorStream()), e);
			}
		}
		if (hasAnswer) {
			try (InputStream inputStream = connection.getInputStream()) {
				final String bodyString = getBodyAsString(inputStream);
				try {
					return mapper.readValue(bodyString, answerClass);
				} catch (Exception e) {
					throw new IllegalStateException(bodyString, e);
				}
			}
		} else {
			return null;
		}
	}

	private HttpURLConnection sendRequest(String methodName, Map<String, String> queryStringParams, Object requestBody) throws IOException {
		HttpURLConnection connection = createConnection(methodName, queryStringParams);
		setRequestHeaders(connection);
		Authenticator.setDefault(new UserPasswordAuthenticator(userName, password));
		sendPostRequest(requestBody, connection);

		return connection;
	}

	private String getBodyAsString(InputStream input) throws IOException {
		StringBuilder sb = new StringBuilder();
		char[] buffer = new char[2048];
		int readChars;
		BufferedReader reader = new BufferedReader(new InputStreamReader(input, Charset.forName("UTF-8")));
		while ((readChars = reader.read(buffer)) != -1) {
			sb.append(buffer, 0, readChars);
		}
		return sb.toString();
	}

	private HttpURLConnection createConnection(String methodName, Map<String, String> queryStringParams) throws IOException {
		final String urlString = remoteUrl + "/" + methodName + getRequestQueryString(methodName, queryStringParams);
		URL url = new URL(urlString);
		return (HttpURLConnection) url.openConnection();
	}

	private void setRequestHeaders(HttpURLConnection connection) throws ProtocolException {
		connection.setRequestMethod("POST");
		connection.addRequestProperty("User-Agent", "Gravity-RecEng-JavaClient-Webshop");
		connection.addRequestProperty("X-Gravity-RecEng-JavaClient-Webshop-Version", VERSION);
		connection.setReadTimeout(readTimeout);
		connection.setConnectTimeout(readTimeout);
		connection.addRequestProperty("Content-Type", "application/json; charset=utf-8");
	}

	private void sendPostRequest(Object requestBody, HttpURLConnection connection) throws IOException {
		connection.setDoOutput(true);
		try (OutputStream outputStream = connection.getOutputStream()) {
			try (final DataOutputStream wr = new DataOutputStream(outputStream)) {
				final String requestJson = mapper.writeValueAsString(requestBody);
				wr.writeBytes(requestJson);
				wr.flush();
			}
		}
	}

	private String getRequestQueryString(String methodName, Map<String, String> queryStringParams) throws UnsupportedEncodingException {
		StringBuilder queryString = new StringBuilder();

		if (queryStringParams != null) {
			for (Entry<String, String> pair : queryStringParams.entrySet()) {
				queryString.append(URLEncoder.encode(pair.getKey(), "UTF-8")).append("=");
				queryString.append(URLEncoder.encode(pair.getValue(), "UTF-8")).append("&");
			}
		}

		if (queryString.length() > 0) {
			queryString.deleteCharAt(queryString.length() - 1);
			queryString.insert(0, "&");
		}

		return "?method=" + URLEncoder.encode(methodName, "UTF-8") + queryString.toString();
	}

	public String getRemoteUrl() {
		return remoteUrl;
	}

	/**
	 * Set the URL of the server side interface. It has no default value, must be specified.
	 * @param remoteUrl the server url provided by Gravity integration team
	 */
	public void setRemoteUrl(String remoteUrl) {
		this.remoteUrl = remoteUrl;
	}

	public String getUserName() {
		return userName;
	}

	/**
	 * Set the user name for the http authenticated connection. Leave it blank in case of
	 * connection without authentication.
	 * @param  userName user authentication name provided by Gravity
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	/**
	 * Set the password for the http authenticated connection. Leave it blank in case of
	 * connection without authentication.
	 * @param  password user authentication password provided by Gravity
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	public int getReadTimeout() {
		return readTimeout;
	}

	/**
	 * Set the timeout for the operations in millisecs. The default value is 3000 millisecs.
	 * @param  readTimeout wait up to this millisecond for the request answers
	 */
	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

	/**
	 * Adds events to the recommendation engine.
	 *
	 * @param events The events to add.
	 * @param async  true if the call is asynchronous. An asynchronous call
	 *               returns immediately after an input data checking,
	 *               a synchronous call returns only after the data is saved to database.
	 * @throws IOException if cannot connect
	 * @throws GravityRecEngException if cannot process the answer files
	 */
	public void addEvents(GravityEvent[] events, boolean async) throws GravityRecEngException, IOException {
		HashMap<String, String> queryStringParams = new HashMap<>();
		queryStringParams.put("async", Boolean.toString(async));
		sendRequest("addEvents", queryStringParams, events, false, null);
	}

	/**
	 * Adds users to the recommendation engine. The existing users will be updated.
	 * If a user already exists with the specified userId,
	 * the entire user will be replaced with the new user specified here.
	 *
	 * @param users The user to add.
	 * @param async true if the call is asynchronous. An asynchronous call
	 *              returns immediately after an input data checking,
	 *              a synchronous call returns only after the data is saved to database.
	 * @throws IOException if cannot connect
	 * @throws GravityRecEngException if cannot process the answer files
	 */
	public void addUsers(GravityUser[] users, boolean async) throws GravityRecEngException, IOException {
		HashMap<String, String> queryStringParams = new HashMap<>();
		queryStringParams.put("async", Boolean.toString(async));
		sendRequest("addUsers", queryStringParams, users, false, null);
	}

	/**
	 * Adds items to the recommendation engine.
	 * If an item already exists with the specified itemId,
	 * the entire item along with its NameValue pairs will be replaced to the new item specified here.
	 *
	 * @param items The items to add
	 * @param async true if the call is asynchronous. An asynchronous call
	 *              returns immediately after an input data checking,
	 *              a synchronous call returns only after the data is saved to
	 * @throws IOException if cannot connect
	 * @throws GravityRecEngException if cannot process the answer filesdatabase.
	 */
	public void addItems(GravityItem[] items, boolean async) throws GravityRecEngException, IOException {
		HashMap<String, String> queryStringParams = new HashMap<>();
		queryStringParams.put("async", Boolean.toString(async));
		sendRequest("addItems", queryStringParams, items, false, null);
	}

	/**
	 * Returns a list of recommended items, based on the given context parameters.
	 *
	 * @param userId   The identifier of the logged in user. If no user is logged in, null should be specified.
	 * @param cookieId It should be a permanent identifier for the end users computer, preserving its value across browser sessions.
	 *                 It should be always specified.
	 * @param context  Additional information which describes the actual scenario.
	 * @return An object containing the recommended items and other information about the recommendation.
	 * @throws IOException if cannot connect
	 * @throws GravityRecEngException if cannot process the answer files
	 */
	public GravityItemRecommendation getItemRecommendation(String userId, String cookieId, GravityRecommendationContext context) throws GravityRecEngException, IOException {
		HashMap<String, String> queryStringParams = new HashMap<>();
		if (userId != null) {
			queryStringParams.put("userId", userId);
		}
		if (cookieId != null) {
			queryStringParams.put("cookieId", cookieId);
		}
		return (GravityItemRecommendation) sendRequest("getItemRecommendation",
				queryStringParams, context, true, GravityItemRecommendation.class);
	}

	/**
	 * Given the userId and the cookieId, we can request recommendations for multiple scenarios (described by the context).
	 * This function returns lists of recommended items for each of the given scenarios in an array.
	 *
	 * @param userId   The identifier of the logged in user. If no user is logged in, null should be specified.
	 * @param cookieId It should be a permanent identifier for the end users computer, preserving its value across browser sessions.
	 *                 It should be always specified.
	 * @param context  Additional Array of information which describes the actual scenarios.
	 * @return An Array containing the recommended items for each scenario with other information about the recommendation.
	 * @throws IOException if cannot connect
	 * @throws GravityRecEngException if cannot process the answer files
	 */
	public GravityItemRecommendation[] getItemRecommendationBulk(String userId, String cookieId, GravityRecommendationContext[] context) throws GravityRecEngException, IOException {
		HashMap<String, String> queryStringParams = new HashMap<>();
		if (userId != null) {
			queryStringParams.put("userId", userId);
		}
		if (cookieId != null) {
			queryStringParams.put("cookieId", cookieId);
		}
		return (GravityItemRecommendation[]) sendRequest("getItemRecommendationBulk",
				queryStringParams, context, true, GravityItemRecommendation[].class);
	}

	/**
	 * Simple test function to test without side effects whether the service is alive.
	 * @param name a test string
	 * @return "Hello " + <code>name</code>* @throws IOException if cannot connect
	 * @throws IOException if cannot connect
	 * @throws GravityRecEngException if cannot process the answer files
	 */
	public String test(String name) throws GravityRecEngException, IOException {
		HashMap<String, String> queryStringParams = new HashMap<>();
		queryStringParams.put("name", name);
		return (String) sendRequest("test", queryStringParams, name, true, String.class);
	}

	/**
	 * Simple test function to test throwing an exception.
	 * @throws IOException if cannot connect
	 * @throws GravityRecEngException if cannot process the answer files
	 */
	public void testException() throws GravityRecEngException, IOException {
		sendRequest("testException", null, null, true, null);
	}

	private static class UserPasswordAuthenticator extends Authenticator {
		private final String username, password;

		public UserPasswordAuthenticator(String user, String pwd) {
			username = user;
			password = pwd;
		}

		@Override
		protected PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(username, password.toCharArray());
		}
	}

}
