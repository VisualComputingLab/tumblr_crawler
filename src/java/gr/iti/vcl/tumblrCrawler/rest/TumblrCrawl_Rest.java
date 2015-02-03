package gr.iti.vcl.tumblrCrawler.rest;

import gr.iti.vcl.tumblrCrawler.impl.TumblrCrawler;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import org.codehaus.jettison.json.JSONObject;

/**
 * REST Web Service
 *
 * @author dimitris.samaras@iti.gr
 */
@Path("crawl")
public class TumblrCrawl_Rest {

    @Context
    private UriInfo context;
    private static TumblrCrawler impl = new TumblrCrawler();

    /**
     * Creates a new instance of CrawlerRest
     */
    public TumblrCrawl_Rest() {
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public JSONObject postJson(JSONObject content) throws Exception {
        return impl.parseOut(content);

        //return null;
    }
}
