package controllers;

import org.codehaus.jackson.JsonNode;
import play.*;
import play.libs.F;
import play.libs.WS;
import play.mvc.*;

import views.html.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;


public class Application extends Controller {

    private static String getParam(Http.Request request, String key, String def) {
        String[] values = request.queryString().get(key);
        if (values != null) {
            return values[0];
        } else {
            return def;
        }
    }

    public static Result index() {
        String query = "";
        if (request().queryString() != null) {
            query = getParam(request(), "_nkw", "");
        }

        if(null != query && query.length() > 0) {
            String encoded;
            try {
                encoded = URLEncoder.encode(query, "UTF-8");
            }
            catch(UnsupportedEncodingException uee) {
                Logger.warn(uee.getLocalizedMessage(), uee);
                return ok(index.render(null, null));
            }

            final String mode = getParam(request() ,"_mode", "");
            final String skipRender = getParam(request(), "_skiprender", "");
            if("async".equals(mode)) {
                return doAsync(query, encoded, "true".equals(skipRender));
            }
            else {
                return doSync(query, encoded, "true".equals(skipRender));
            }
        }

        return ok(index.render(null, null));
    }

    private static Result doSync(final String query, final String encoded, final boolean skipRender) {
        final WS.Response wsresponse = WS.url("http://svcs.ebay.com/services/search/FindingService/v1")
            .setQueryParameter("OPERATION-NAME", "findItemsByKeywords")
            .setQueryParameter("SERVICE-VERSION", "1.8.0")
            .setQueryParameter("SECURITY-APPNAME", "Foobat96e-1d23-4ae7-8e15-e5874c9cd58")
            .setQueryParameter("RESPONSE-DATA-FORMAT", "JSON")
            .setQueryParameter("REST-PAYLOAD", "")
            .setQueryParameter("keywords", encoded)
            .setQueryParameter("paginationInput.entriesPerPage", "50")
            .setQueryParameter("outputSelector(0)", "SellerInfo")
            .setQueryParameter("outputSelector(1)", "CategoryHistogram")
            .get().get();
        final JsonNode results = wsresponse.asJson();
        if(!skipRender) {
            return ok(index.render(query, results));
        }
        else {
            return ok();
        }
    }

    private static Result doAsync(final String query, final String encoded, final boolean skipRender) {
        final WS.WSRequestHolder request = WS.url("http://svcs.ebay.com/services/search/FindingService/v1")
            .setQueryParameter("OPERATION-NAME", "findItemsByKeywords")
            .setQueryParameter("SERVICE-VERSION", "1.8.0")
            .setQueryParameter("SECURITY-APPNAME", "Foobat96e-1d23-4ae7-8e15-e5874c9cd58")
            .setQueryParameter("RESPONSE-DATA-FORMAT", "JSON")
            .setQueryParameter("REST-PAYLOAD", "")
            .setQueryParameter("keywords", encoded)
            .setQueryParameter("paginationInput.entriesPerPage", "50")
            .setQueryParameter("outputSelector(0)", "SellerInfo")
            .setQueryParameter("outputSelector(1)", "CategoryHistogram");

        return async(
                request.get().map(
                    new F.Function<WS.Response, Result>() {
                        public Result apply(WS.Response response) {
                            if (!skipRender) {
                                return ok(index.render(query, response.asJson()));
                            } else {
                                return ok();
                            }
                        }
                    }
                )
               );
    }

}
