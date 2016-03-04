package com.gravityrd.recengclient.webshop;

import com.gravityrd.receng.web.webshop.jsondto.GravityItem;
import com.gravityrd.receng.web.webshop.jsondto.GravityNameValue;
import com.gravityrd.receng.web.webshop.jsondto.GravityRecEngException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class GravityClientTest {

	private GravityClient client;

	@Before
	public void setUp() throws Exception {
		client = new GravityClient();

	}

	@Test
	public void testEncoding() throws IOException {
		final GravityNameValue nameValue = new GravityNameValue("test", "\u0002Maël\u0030Hörz\u0019");
		final String json = GravityClient.mapper.writeValueAsString(nameValue);
		final ObjectMapper mapper = new ObjectMapper();
		final GravityNameValue response = mapper.readValue(json, GravityNameValue.class);
	}

	@Test
	@Ignore
	public void testEncoding2() throws GravityRecEngException, IOException {
		GravityItem item = new GravityItem();
		item.title = "\u0002Maël\u0030Hörz\u0019";
		item.hidden = true;
		item.itemId = "random_test_for_testing";
		GravityItem[] items = new GravityItem[] { item };
		client.addItems(items, true);
	}
}
