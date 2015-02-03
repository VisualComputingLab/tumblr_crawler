# tumblr_crawler
A java web crawler for the ‘Tumblr.’ micro-blogging social network API


## About this project 

Project name: TumblrCrawler
Architecture: Restfull application
Programming language: java 
Structuring and output format: json
Application server: Apache Tomcat
Messaging system: RabbitMQ, based on the AMQP standard

A java wrapper for the ‘Tumblr.’ micro-blogging social network API, a platform for blogging allowing users either to generate or share content.
With the TumblrCrawler we try to gather images posted or shared by Tumblr. users over a specific topic, as well as the metadata surrounding that image.
A topic may refer to various blogs or on various pictures to a blog. Every image and its metadata form a separate message that is delivered to the RabbitMQ.
The process is initiated by posting (POST request) our request to the Tomcat using a rest client (i.e. Advanced Rest Client for Google Chrome browser) followed by the .json file containing the request payload. The result of the request is written to the RabbitMQ and we get a server response about the operation status.

#Users -- REST calls 

The user in order to search the tumblr. social network for images over a specific topic has to post a request with a specific payload to indicate the search parameters. 

i.e.
POST  http://localhost:8084/TumblrCrawler/resources/crawl
Content-Type: "application/json"

Payload
{
"tumblr": {
"consumerKey": "yourApiKey",
		"topic":"fashion",
		"limit":3,
		"before": "2014-06-04"

},
"rabbit": {
		"host": "localhost",
		"queue": "TUMBLR_CRAWLER_IN_QUEUE"
}
}

•	The url defines where the service runs
•	The content-type defines what type is the request payload we are about to send to the application server
•	tumblr object:
o	Consumer key primitive is the consumer key provided to us when we register a new application when a new application exploiting the Tumbl API is registered to the site.
o	Topic primitive is the search term “topic” we want to search for.
o	Limit primitive (optional) limits the results that are returned to us, default value if the parameter is not used in he payload is ‘20’ as defined by the API. (since the results returned are already limited the ‘limit’ parameter fit to use in case we have reached quota limit)
o	Before primitive (optional) is a string (YYYY-mm-dd) defining the certain date before which we want the results to refer to.
•	rabbit object:
o	Host primitive defines where the RabbitMQ server is hosted 
o	Queue primitive defined how the queue that will hold the messages should be named.


Since the server returns a 200, OK message the json objects that have been created can be accessed through the RabbitMQ server platform (localhost:15672…guest,guest)
	 

# Developers 

Package: gr.iti.vcl.tumblrCrawler.impl
TumblCrawl.java methods documentation

The output of this class is zero or more messages written to a specific RabbitMQ queue containing information about images posted on Tumblr and a json object over the operation status.

parseOut

Responsible method for parsing the response from the GET request to the Tumblr API.
Opens and closes connection and creates queues to the RabbitMQ service and writes multiple objects to the queues.
Returns a json object that notifies the user over the operation status.
Its operation time depends on the length of the response that the call to API returns 

@param jsonObject 	The paylod of the initial POST request that the user provides and. defines the parameters to form the GET request to the Tumblr API. 
@return 		The json object containing information about process status.
@throws IOException 	If an input or output exception occurred.

callGET 

Responsible for passing the user defined parameters to the GET request to the tumblr API containing and passing the response back so that processing is initiated.

@param tagval 		The topic search term parameter of the request.
@param apikeyval		The Tumblr  API key.
@param limit		The integer defining the limit parameter of the request.
@param before		The long number defining the before parameter of the request.
@return 		The response of the GET request as String. 

convertStreamToString

Responsible for parsing the inputstream created by the GET request to a String 

@param is 		The inputStream.
@return 		The String. 
@throws IOException 	If an input or output exception occurred.


writeToRMQ

Responsible for creating a connection with the RabbitMQ server, creating a queue and writing messages to the queue.

@param json 		The json object that will be stored to the messages queue (bytes).
@param host		The host of the RabitMQ.
@param qName		The qName that the message will be stored to.
@throws IOException 	If an input or output exception occurred.


closeRMQ

Responsible for closing the connection and channels to the RabbitMQ queue

log & err

Logging and error messaging methods

Package: gr.iti.vcl.tumblrCrawler. rest
TumblrCrawl_Rest.java methods documentation

@POST
@Consumes("application/json")
@Produces("application/json")

postJson

The rest implementation for the Tumblr crawler.
@param json 	The json object containing the payload for the Post request provided by the user.
@return json	The json object containing the operation status.
@throws Exception	if json object not provided to method 


#Problems met

When sending a GET request the maximum amount of Tumblr posts that will be returned is 20. No next page token is provided to the response message to iterate to next results. In order to get better results the topic term must be as accurate as possible. Descriptive terms should return broader results. Also responses can be contained over a specific subject using the before parameter (Posts before a certain date), while an ‘After’ parameter is not defined through the API.

The API console is malfunctioning to crosscheck the results 

#Future work

In the post json payload accept the date as simple date format and not as unix timestamp

Display all the messages created as a single object as a server log message.
 
Try Catch statements surrounding all object parsing methods to prevent user from malformed and erroneous input…. (Maybe! restrict it from the UI).

