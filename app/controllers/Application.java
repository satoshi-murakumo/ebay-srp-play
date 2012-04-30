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

  public static Result index() {
    String query = "";
    if (request().body().asFormUrlEncoded() != null) {
      query = request().body().asFormUrlEncoded().get("_nkw")[0];
    }

    if(null != query && query.length() > 0) {
      String encoded;
      try {
        encoded = URLEncoder.encode(query, "UTF-8");
      }
      catch(UnsupportedEncodingException uee) {
        Logger.warn(uee.getLocalizedMessage(), uee);
        return ok(index.render(null));
      }

      final String mode = request().body().asFormUrlEncoded().get("_mode")[0];
      final String skipRender = request().body().asFormUrlEncoded().get("_skiprender")[0];
      if("async".equals(mode)) {
        return doAsync(encoded, "true".equals(skipRender));
      }
      else {
        return doSync(encoded, "true".equals(skipRender));
      }
    }

    return ok(index.render(null));
  }

  private static Result doSync(final String query, final boolean skipRender) {
    final String url = "http://svcs.ebay.com/services/search/FindingService/v1?OPERATION-NAME=findItemsByKeywords&" +
            "SERVICE-VERSION=1.8.0&SECURITY-APPNAME=Foobat96e-1d23-4ae7-8e15-e5874c9cd58&" +
            "RESPONSE-DATA-FORMAT=JSON&REST-PAYLOAD&" +
            "keywords=" + query + "&paginationInput.entriesPerPage=50&" +
            "outputSelector%280%29=SellerInfo&outputSelector%281%29=CategoryHistogram";
    final JsonNode results = WS.url(url).get().get().asJson();
    if(!skipRender) {
      return ok(index.render(results));
    }
    else {
      return ok();
    }
  }

  private static Result doAsync(final String encodedQuery, boolean skipRender) {
    final String url = "http://svcs.ebay.com/services/search/FindingService/v1?OPERATION-NAME=findItemsByKeywords&" +
            "SERVICE-VERSION=1.8.0&SECURITY-APPNAME=Foobat96e-1d23-4ae7-8e15-e5874c9cd58&" +
            "RESPONSE-DATA-FORMAT=JSON&REST-PAYLOAD&" +
            "keywords=" + encodedQuery + "&paginationInput.entriesPerPage=50&" +
            "outputSelector%280%29=SellerInfo&outputSelector%281%29=CategoryHistogram";

    return async(
            WS.url(url).get().map(
                    new F.Function<WS.Response, Result>() {
                      public Result apply(WS.Response response) {
                        return ok(index.render(response.asJson()));
                      }
                    }
            )
    );
  }
  
}