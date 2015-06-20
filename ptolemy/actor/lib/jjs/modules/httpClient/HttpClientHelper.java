/* A JavaScript helper for HttpClient.

@Copyright (c) 2015 The Regents of the University of California.
All rights reserved.

Permission is hereby granted, without written agreement and without
license or royalty fees, to use, copy, modify, and distribute this
software and its documentation for any purpose, provided that the
above copyright notice and the following two paragraphs appear in all
copies of this software.

IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
ENHANCEMENTS, OR MODIFICATIONS.

						PT_COPYRIGHT_VERSION_2
						COPYRIGHTENDKEY



 */
package ptolemy.actor.lib.jjs.modules.httpClient;

import java.util.Map;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;

import ptolemy.actor.lib.jjs.modules.VertxHelperBase;

///////////////////////////////////////////////////////////////////
//// HttpClientHelper

/**
   A helper class for the HttpClient module in JavaScript.
   
   @author Marten Lohstroh, Edward A. Lee
   @version $Id: HttpClientHelper.java 71938 2015-04-08 21:55:19Z hokeunkim $
   @since Ptolemy II 11.0
   @Pt.ProposedRating Yellow (eal)
   @Pt.AcceptedRating Red (cxh)
 */
public class HttpClientHelper extends VertxHelperBase {

    ///////////////////////////////////////////////////////////////////
    ////                     public methods                        ////

    /** Create a HttpClientHelper instance for the specified JavaScript
     *  Socket instance for the client side of the socket.
     *  @param currentObj The JavaScript instance of the Socket.
     *  @return A new HttpClientHelper instance.
     */
    public static HttpClientHelper createHttpClient(
            ScriptObjectMirror currentObj, Map<String, Object> options) {
        return new HttpClientHelper(currentObj, options);
    }

    /** End a request. */
    public void end() {
	if (_request != null) {
	    _request.end();
	}
    }

    ///////////////////////////////////////////////////////////////////
    ////                     private constructors                   ////

    /** Private constructor to open an HTTP client.
     *  @param currentObj The JavaScript instance of Socket that this helps.
     *  @param address The URL of the WebSocket host with an optional port number
     *   (e.g. 'ws://localhost:8000'). If no port number is given, 80 is used.
     */
    private HttpClientHelper(ScriptObjectMirror currentObj, Map<String, Object> options) {
        super(currentObj);
        HttpClient client = _vertx.createHttpClient();
        
        // NOTE: Vert.x documentation states about HttpClient:
        // "If an instance is instantiated from some other arbitrary Java thread
        // (i.e. when running embedded) then and event loop will be assigned
        // to the instance and used when any of its handlers are called."
        // Hence, the HttpClient we just created will be assigned its own
        // event loop. We need to ensure that callbacks are mutually exclusive
        // with other Java code here.
        
        // FIXME: Why does Vert.x require setHost and setPort and also a URI?
        client.setHost((String) options.get("host")); 
        client.setPort((int) options.get("port")); 
        client.exceptionHandler(new HttpClientExceptionHandler());
        if ((boolean)options.get("keepAlive")) {
            client.setKeepAlive(true);
        }
        // FIXME: Provide a timeout. Use setTimeout() of the client.
        
        String query = "";
        Object queryObject = options.get("query");
        if (queryObject != null) {
            String querySpec = queryObject.toString().trim();
            if(!querySpec.equals("") && !querySpec.startsWith("?")) {
        	query = "?" + querySpec;
            }
        }
        
        String uri = options.get("protocol")
        	+ "://"
        	+ options.get("host")
        	+ ":"
        	+ options.get("port")
        	+ options.get("path")
        	+ query;
        
        _request = client.request(
        	(String)options.get("method"),
        	uri,
        	new HttpClientResponseHandler());
   
        // Handle the headers.
        Map headers = (Map)options.get("headers");
        if (!headers.isEmpty()) {
            for (Object key : headers.keySet()) {
        	Object value = headers.get(key);
        	if (value instanceof String) {
        	    _request.putHeader((String)key, (String)value);
        	} else if (value instanceof Iterable) {
        	    _request.putHeader((String)key, (Iterable<String>)value);
        	}
            }
        }
    }

    ///////////////////////////////////////////////////////////////////
    ////                     private fields                        ////
    
    /** The request built in the constructor. */
    private HttpClientRequest _request;

    ///////////////////////////////////////////////////////////////////
    ////                     inner classes                         ////

    /** The event handler that is triggered when an error occurs in the HTTP connection.
     */
    private class HttpClientExceptionHandler implements Handler<Throwable> {
        @Override
        public void handle(Throwable throwable) {
            synchronized(_actor) {
        	_currentObj.callMember("emit", "error", throwable.getMessage());
            }
        }
    }
    
    /** The event handler that is triggered when a response arrives from the server.
     */
    private class HttpClientResponseHandler implements Handler<HttpClientResponse> {
        @Override
        public void handle(HttpClientResponse response) {
            // The response is not yet complete. Provide a handler to handle when it
            // is complete. Note that large response could fill up memory here, since
            // we do not chunk the response!
            HttpClientBodyHandler bodyHandler = new HttpClientBodyHandler(response);
            response.bodyHandler(bodyHandler);
        }
    }
    
    /** The body handler that is triggered when a complete response body
     *  has arrived from the server.
     */
    private class HttpClientBodyHandler implements Handler<Buffer> {
	public HttpClientBodyHandler(HttpClientResponse response) {
	    _response = response;
	}
        @Override
        public void handle(Buffer body) {
            // Need to synchronize because Vert.x ensures these callbacks are
            // atomic w.r.t. one another, but not w.r.t. the JavaScript engine
            // or this actor's methods.
            synchronized(_actor) {
        	// FIXME: This assumes the body is a string encoded in UTF-8.
        	_currentObj.callMember("_response", _response, body.toString());
            }
        }
        private HttpClientResponse _response;
    }
}