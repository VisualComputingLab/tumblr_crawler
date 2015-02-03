package gr.iti.vcl.tumblrCrawler.impl;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/*
 *
 * @author  Samaras Dimitris 
 * June 3rd, 2014
 * dimitris.samaras@iti.gr
 * 
 */
public class TumblrCrawler {

    public static final String PREFIX_API_SITE = "http://api.tumblr.com";
    public static final String PREFIX_SERVICE = "/v2/tagged";
    public Connection connection = null;
    public Channel channel = null;

    public TumblrCrawler() {
    }

    @SuppressWarnings("empty-statement")
    public JSONObject parseOut(JSONObject jsonObject) throws Exception, IOException {

        // Create the JSONObject desplay operation status
        JSONObject object = new JSONObject();
        // Create the JSONObject to construct the response that will be saved to RabbitMQ
        JSONObject resultObject = new JSONObject();

        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        try {

                // retrieve credentials for Tumblr API & RabbitMQ connection
                String consumerKey = jsonObject.getJSONObject("tumblr").getString("consumerKey");
                String host = jsonObject.getJSONObject("rabbit").getString("host");
                String qName = jsonObject.getJSONObject("rabbit").getString("queue");

                //Validatecertain crusial params 
                //keywords
                String keywords = jsonObject.getJSONObject("tumblr").getString("topic");
                keywords=keywords.replace(" ","+");

                if (keywords == null || keywords.isEmpty()) {
                    err("No keywords given, aborting");
                    object.put("Status", "Error");
                    object.put("Message", "No keyword/s given");
                    return object;
                }

                //limit -- num of results
                // if no limit is defined set 20...as defined by default on Tumblr API
                int limit = jsonObject.getJSONObject("tumblr").optInt("limit", 20);

                //before -- before certain time period
                // if no before timestamp is defined set current...as defined by default on Tumblr API
                String curTime = formatter.format(System.currentTimeMillis());
                String before = jsonObject.getJSONObject("tumblr").optString("before", curTime);
                long date_before =formatter.parse(before).getTime()/1000L;
                
                log("preRESPONSE..." + curTime);
                log("preRESPONSE..." + before);
                log("preRESPONSE..." + date_before);
                String rsp = callGET(keywords, consumerKey, limit, date_before);

                // Create the JSONObject to be parsed
                System.out.println("RESPONSE..." + rsp);
                JSONObject jobj = new JSONObject(rsp);

                JSONArray msgArr = jobj.getJSONArray("response");

                for (int i = 0; i < msgArr.length(); i++) {
                    //Get separate responses from main response
                    JSONObject postResp = new JSONObject(msgArr.getString(i));

                    ///////////////////////// PRINT OUT
                    //System.out.println("Resp" + i + postResp);

                    resultObject.put("about", "Tumblr");

                    String blog_name = postResp.getString("blog_name");
                    resultObject.put("blog_name", blog_name);

                    int id = postResp.getInt("id");
                    resultObject.put("id", id);

                    String date = postResp.getString("date");
                    resultObject.put("date", date);

                    int timestamp = postResp.getInt("timestamp");
                    resultObject.put("timestamp", timestamp);

                    JSONArray msgTags = postResp.getJSONArray("tags");
                    List<String> tags = new ArrayList<String>();
                    for (int y = 0; y < msgTags.length(); y++) {
                        tags.add(msgTags.getString(y));
                    }
                    //////////////////////////// PRINT OUT
                    //System.out.println("Resp " + i +"tags "+ tags);
                    resultObject.put("tags", tags);

                    JSONArray msgPhotos = postResp.getJSONArray("photos");
                    //if it is not a photo post...
                    if (msgPhotos == null) {
                        err("No photos in this post, aborting");
                        resultObject.put("Status", "Error");
                        resultObject.put("Message", "No photos");
                        return resultObject;
                    }

                    try {
                        for (int z = 0; z < msgPhotos.length(); z++) {
                            //Store Photos to proper format.....caption,thumbnail(alt_sizes(5)) and original size

                            JSONObject photos = new JSONObject(msgPhotos.getString(z));
                            String caption = photos.optString("caption", "No caption over that img");
                            resultObject.put("caption", caption);
                            //////////////////////////// PRINT OUT
                            //System.out.println("Photo " + z + " caption "+caption);

                            JSONObject original = photos.getJSONObject("original_size");
                            resultObject.put("original_size", original);
                            //////////////////////////// PRINT OUT
                            //System.out.println("Photo " + z + " original" + original);

                            JSONArray altSizes = photos.getJSONArray("alt_sizes");
                            try {
                                JSONObject thumb = new JSONObject(altSizes.getString(5));

                                resultObject.put("thumbnail", thumb);
                            } catch (JSONException e) {
                                String noThumb = "No thumbnail available";
                                resultObject.put("thumbnail", noThumb);
                            }
                            //////////////////////////// PRINT OUT
                            //System.out.println("Photo " + z + " thumb" + thumb);

                            //////////////////////////// PRINT OUT
                            //System.out.println("The new obj " + resultObject);

                            //Store result to RabbitMQ....EveryPhoto is a seperate message to be processed 
                            writeToRMQ(resultObject, host, qName);
                        }
                    } catch (JSONException e) {
                        err("JSONException getting photos from blog : " + e);
                    }
                    
                }
                closeRMQ();
                object.put("Status", 200);
                    object.put("Message", "OK");

        } catch (JSONException e) {
            err("JSONException : " + e);
        }

        //resultObjects can be pilled up to a JSONArray and displayed as object....
        return object;

    }

    public String callGET(String tagval, String apikeyval, int limit, long before) {
        String output;
        int code = 0;
        String msg = null;

        try {
            URL url = new URL(PREFIX_API_SITE+ PREFIX_SERVICE + "?tag=" + tagval + "&api_key=" + apikeyval + "&limit=" + limit + "&before=" + before);
            HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
            // you need the following if you pass server credentials
            // httpCon.setRequestProperty("Authorization", "Basic " + new BASE64Encoder().encode(servercredentials.getBytes()));
            httpCon.setDoOutput(true);
            httpCon.setRequestMethod("GET");
            output = convertStreamToString(httpCon.getInputStream());
            code = httpCon.getResponseCode();
            msg = httpCon.getResponseMessage();
            //output = "" + httpCon.getResponseCode() + "\n" + httpCon.getResponseMessage() + "\n" + output;

        } catch (IOException e) {
            output = "IOException during GET: " + e;
            err(output);
        }
        // Check for Response 
        if ((code != 200 || code != 201) && !("OK".equals(msg))) {
            //output = "NOT OK RESPONSE";
            err("Failed : HTTP error code : " + code);

        }
        return output;
    }

    private static String convertStreamToString(InputStream is) throws IOException {
        //
        // To convert the InputStream to String we use the
        // Reader.read(char[] buffer) method. We iterate until the
        // Reader return -1 which means there's no more data to
        // read. We use the StringWriter class to produce the string.
        //
        if (is != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }

            return writer.toString();
        } else {
            return "";
        }
    }

    public void writeToRMQ(JSONObject json, String host, String qName) throws IOException {
        //Pass the queue name here from the RESQUEST JSON

        //Create queue, connect and write to rabbitmq
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);

        log("connected to rabbitMQ on localhost ...");

        try {
            connection = factory.newConnection();
            channel = connection.createChannel();

            channel.queueDeclare(qName, true, false, false, null);
        } catch (IOException ex) {
            err("IOException during queue creation: " + ex);
        }
        channel.basicPublish("", qName,
                MessageProperties.PERSISTENT_TEXT_PLAIN,
                json.toString().getBytes());
        log(" [x] Sent to queue '" + json + "'");
    }

    public void closeRMQ() throws IOException {

        if (connection != null) {
            log("Closing rabbitmq connection and channels");
            try {
                connection.close();
                connection = null;
            } catch (IOException ex) {
                err("IOException during closing rabbitmq connection and channels: " + ex);

            }
        }
    }

    private void log(String message) {
        System.out.println("TumblrCrawler:INFO:" + message);
    }

    private void err(String message) {
        System.err.println("TumblrCrawler:ERROR:" + message);
    }
}
