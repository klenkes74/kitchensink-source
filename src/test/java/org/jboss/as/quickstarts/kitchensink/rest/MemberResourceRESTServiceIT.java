package org.jboss.as.quickstarts.kitchensink.rest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MemberResourceRESTServiceIT {

    @Test
    public void testRegisterMember() throws Exception {
        CloseableHttpClient client = HttpClients.createDefault();
        String hostName = System.getProperty("test.it.hostName", "localhost:8080");
        HttpPost httpPost = new HttpPost(hostName + "/rest/members");
        String name = RandomStringUtils.randomAlphabetic(8);
        String domain = RandomStringUtils.randomAlphabetic(8);
        String phone = RandomStringUtils.randomNumeric(12);
        String json = "{\"name\":\"" + name + "\",\"email\":\"" + name + "@" + domain + ".com\",\"phoneNumber\":\"" + phone + "\"}";
        StringEntity entity = new StringEntity(json);
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");
        CloseableHttpResponse response = client.execute(httpPost);
        MatcherAssert.assertThat(response.getStatusLine().getStatusCode(), IsEqual.equalTo(200));
        client.close();
    }

    @Test
    public void testRegisterMemberNoEmail() throws Exception {
        CloseableHttpClient client = HttpClients.createDefault();
        String hostName = System.getProperty("test.it.hostName", "localhost:8080");
        HttpPost httpPost = new HttpPost(hostName + "/rest/members");
        String name = RandomStringUtils.randomAlphabetic(8);
        String phone = RandomStringUtils.randomNumeric(12);
        String json = "{\"name\":\"" + name + "\",\"email\":\"\",\"phoneNumber\":\"" + phone + "\"}";
        StringEntity entity = new StringEntity(json);
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");
        CloseableHttpResponse response = client.execute(httpPost);
        MatcherAssert.assertThat(response.getStatusLine().getStatusCode(), IsEqual.equalTo(400));
        client.close();
    }

    @Test
    public void testRegisterEmailTaken() throws Exception {
        String hostName = System.getProperty("test.it.hostName", "localhost:8080");
        testRegisterMember();
        HttpUriRequest request = new HttpGet(hostName + "/rest/members");
        HttpResponse response = HttpClientBuilder.create().build().execute(request);
        List<Member> members = Arrays.asList(retrieveResourceFromResponse(response, Member[].class));
        Member member = members.get(members.size() - 1);
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(hostName + "/rest/members");
        String name = RandomStringUtils.randomAlphabetic(8);
        String phone = RandomStringUtils.randomNumeric(12);
        String json = "{\"name\":\"" + name + "\",\"email\":\"" + member.getEmail() + "\",\"phoneNumber\":\"" + phone + "\"}";
        StringEntity entity = new StringEntity(json);
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");
        CloseableHttpResponse closeableResponse = client.execute(httpPost);
        MatcherAssert.assertThat(closeableResponse.getStatusLine().getStatusCode(), IsEqual.equalTo(409));
        client.close();
    }

    @Test
    public void testListAllMembers() throws Exception {
        String hostName = System.getProperty("test.it.hostName", "localhost:8080");
        HttpUriRequest request = new HttpGet(hostName + "/rest/members");
        HttpResponse response = HttpClientBuilder.create().build().execute(request);
        MatcherAssert.assertThat(response.getStatusLine().getStatusCode(), IsEqual.equalTo(200));
        List<Member> members = Arrays.asList(retrieveResourceFromResponse(response, Member[].class));
        Assert.assertTrue(members.size() > 0);
    }

    @Test
    public void testLookupMemberById() throws Exception {
        String hostName = System.getProperty("test.it.hostName", "localhost:8080");
        testRegisterMember();
        HttpUriRequest request = new HttpGet(hostName + "/rest/members");
        HttpResponse response = HttpClientBuilder.create().build().execute(request);
        List<Member> members = Arrays.asList(retrieveResourceFromResponse(response, Member[].class));
        Member member = members.get(0);
        Assert.assertNotNull(member);
        request = new HttpGet(hostName + "/rest/members/" + member.getId().toString());
        response = HttpClientBuilder.create().build().execute(request);
        MatcherAssert.assertThat(response.getStatusLine().getStatusCode(), IsEqual.equalTo(200));
        Member lookup = retrieveResourceFromResponse(response, Member.class);
        Assert.assertNotNull(lookup);
        MatcherAssert.assertThat(lookup.getId(), IsEqual.equalTo(member.getId()));
        MatcherAssert.assertThat(lookup.getName(), IsEqual.equalTo(member.getName()));
    }

    public <T> T retrieveResourceFromResponse(HttpResponse response, Class<T> clazz) throws IOException {
        String jsonFromResponse = EntityUtils.toString(response.getEntity());
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.readValue(jsonFromResponse, clazz);
    }

}
