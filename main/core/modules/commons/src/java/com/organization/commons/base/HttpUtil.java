package com.organization.commons.base;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class HttpUtil {
    private static final Logger LOG = LoggerFactory.getLogger(HttpUtil.class);

    private HttpUtil() { }

    /*
      Developer Documentation:
        -RequestBuilder is used instead of the common Http request subclasses (e.g. HttpGet)
         so an HttpResponse can be used over CloseableHttpResponse, which requires the response
         to be closed by the calling code.
     */

    /**
     * Creates a get request
     * @return HttpResponse of the request; will be null if request fails
     */
    public static HttpResponse get(String url) throws IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpUriRequest request = RequestBuilder.create("GET")
                .setUri(url)
                .build();

        HttpResponse response = client.execute(request);
        int code = response.getStatusLine().getStatusCode();
        logStatus(code, request, response, url);

        return response;
    }

    /**
     * Create a post request
     * @param url Url of the request
     * @param headers Map of header; null for none
     */
    public static HttpResponse post(String url, Map<String, String> headers, String content) throws IOException {
        HttpClient client = HttpClientBuilder.create().build();
        RequestBuilder builder = RequestBuilder.create("POST")
                .setUri(url);

        if (headers != null) {
            for (Object obj : headers.entrySet()) {
                Map.Entry entry = (Map.Entry) obj;
                builder.addHeader(entry.getKey().toString(), entry.getValue().toString());
            }
        }

        if (content != null) {
            StringEntity entity = new StringEntity(content);
            builder.setEntity(entity);
        }

        HttpUriRequest request = builder.build();

        HttpResponse response = client.execute(request);
        int code = response.getStatusLine().getStatusCode();
        logStatus(code, request, response, url);

        return response;
    }




    private static void logStatus(int code, HttpUriRequest request, HttpResponse response, String url) {
        switch (code) {
            case 200:
                LOG.info("{} {}: 200", request.getMethod(), url);
                break;
            case 204:
                LOG.warn("{} {}: 204 ({})", request.getMethod(), url, response.getStatusLine().getReasonPhrase());
                break;
            default:
                LOG.error("{} {}: {} ({})", request.getMethod(),
                        url, code, response.getStatusLine().getReasonPhrase());
                break;
        }
    }




    /**
     * Parse an ArrayList returned as a JSON array from a response back to an ArrayList
     * @param httpResponse Response from which to get the content
     * @return ArrayList containing the items of the JSON array (as strings);
     * null if the ArrayList could not be created for any reason
     */
    public static ArrayList<String> parseRespContToArrayList(HttpResponse httpResponse) {
        String responseContent = getResponseContent(httpResponse);
        if (responseContent == null || responseContent.equals("")) {
            return null;
        }

        ArrayList<String> items = new ArrayList<>();
        try {
            JSONArray jsonArr = new JSONArray(responseContent);
            for (Object itemObj : jsonArr) {
                items.add(itemObj.toString());
            }
        } catch (JSONException e) {
            LOG.error("Cannot parse http response content: Content not in valid JSON array form");
            return null;
        }

        return items;
    }

    /**
     * Returns the content from the response
     * @param httpResponse Response from which to get the content
     * @return The content as a String; null if it could not be retrieved for any reason
     */
    public static String getResponseContent(HttpResponse httpResponse) {
        HttpEntity httpEntity = httpResponse.getEntity();
        if (httpEntity == null) {
            return null;
        }

        String responseContent;
        try {
            responseContent = EntityUtils.toString(httpResponse.getEntity());
        } catch (IOException e) {
            LOG.error("Failed to parse response content: {}", e.toString());
            return null;
        }

        return responseContent;
    }
}
