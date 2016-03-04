package com.gravityrd.recengclient.webshop;

import com.gravityrd.receng.web.webshop.jsondto.GravityItem;
import com.gravityrd.receng.web.webshop.jsondto.GravityNameValue;
import com.gravityrd.receng.web.webshop.jsondto.GravityRecEngException;
import com.gravityrd.receng.web.webshop.jsondto.GravityRecommendationContext;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

@Ignore
public class GravityClientTest {

	private final static String customerName = "";
	private final static String password = "";
	private final static String cluster = "bud";

	private final static String remoteUrl = String.format("http://%s-%s.gravityrd-services.com/grrec-%s-war/WebshopServlet", customerName, cluster, customerName);
	private final static String user = customerName;

	private GravityClient client;

	@Before
	public void setUp() throws Exception {
		client = new GravityClient();
		client.setRemoteUrl(remoteUrl);
		client.setUserName(user);
		client.setPassword(password);
	}

	@Test
	public void testEncoding() throws IOException {
		final GravityNameValue nameValue = new GravityNameValue("test", "\u0002Maël\u0030Hörz\u0019");
		final String json = GravityClient.mapper.writeValueAsString(nameValue);
		final ObjectMapper mapper = new ObjectMapper();
		final GravityNameValue response = mapper.readValue(json, GravityNameValue.class);
	}

	@Test
	public void testAddItemEncoding() throws GravityRecEngException, IOException {
		GravityItem item = new GravityItem();
		item.title = "\u0002Maël\u0030Hörz\u0019";
		item.hidden = true;
		item.itemId = "random_test_for_testing";
		GravityItem[] items = new GravityItem[] { item };
		client.addItems(items, true);
	}

	@Test
	public void testRecommendationRequest() throws GravityRecEngException, IOException {
		GravityRecommendationContext request = new GravityRecommendationContext();
		request.numberLimit = 5;
		request.resultNameValues = new String[] { "title", "description" };
		request.nameValues = new GravityNameValue[] { new GravityNameValue("test", "1") };
		request.scenarioId = "basic";
		System.out.println(client.getItemRecommendation("userId", "cookieId", request));
	}

	@Test(expected = GravityRecEngException.class)
	public void testException() throws GravityRecEngException, IOException {
		client.testException();
	}
}
