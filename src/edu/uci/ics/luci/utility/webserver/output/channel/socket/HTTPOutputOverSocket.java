package edu.uci.ics.luci.utility.webserver.output.channel.socket;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpServerConnection;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.Globals;
import edu.uci.ics.luci.utility.webserver.WebUtil;
import edu.uci.ics.luci.utility.webserver.output.channel.Output;
import edu.uci.ics.luci.utility.webserver.output.response.Response;
import edu.uci.ics.luci.utility.webserver.output.response.http.HTTP;

/**
 * This class is the implementation for the Webserver to respond on the normal internet using
 *  sockets and http
 * @author djp3
 *
 */
public class HTTPOutputOverSocket extends Output{
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(HTTPOutputOverSocket.class);
		}
		return log;
	}
	
	private Socket socket;
	private HashMap<String, String> headers;
	private HttpServerConnection conn;

	HTTPOutputOverSocket(Socket socket){
		this.socket = socket;
		
		headers = new HashMap<String,String>();
		headers.put("Server", this.getClass().getName()+" "+Globals.getGlobals().getSystemVersion());
		headers.put("Access-Control-Allow-Origin","*");
	}

	public HTTPOutputOverSocket(HttpServerConnection conn) {
		this.conn = conn;
	}
	

	@Override
	public Socket getSocket() {
		return socket;
	}
	
	@Override
	public Map<String,String> getServerHeaders() {
		return headers;
	}
	

	@Override
	public Response makeOutputChannelResponse() {
		return new HTTP();
	}
	
	

	private void composeHeadersSingle(final HttpResponse httpResponse, Map<String, String> httpHeaders) {
		if(httpHeaders != null){
			for(Entry<String, String> e:httpHeaders.entrySet()){
				httpResponse.addHeader(e.getKey(),e.getValue());
			}
		}
	}

	private void composeHeadersMultiple(final HttpResponse httpResponse, Map<String, Set<String>> httpHeaders) {
		if(httpHeaders != null){
			for(Entry<String, Set<String>> e:httpHeaders.entrySet()){
				for(String s:e.getValue()){
					httpResponse.addHeader(e.getKey(),s);
				}
			}
		}
	}
	
	
	
	@Override
	public void send_OK(Response response) {
		if(!(response instanceof HTTP)){
			throw new IllegalArgumentException("response is wrong type: "+response.getClass().getCanonicalName());
		}	
		else{
			HTTP corh = (HTTP) response;
			
			HttpResponse httpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, corh.getHttpStatus(), "OK") ;
			try {
				composeHeadersMultiple(httpResponse,corh.getHttpHeaders());
				composeHeadersSingle(httpResponse,getServerHeaders());
				
				httpResponse.setEntity(new StringEntity(corh.getBody()));
				conn.sendResponseHeader(httpResponse);
				conn.sendResponseEntity(httpResponse);
			} catch (HttpException e) {
				getLog().error(e.toString());
			} catch (IOException e) {
				getLog().error(e.toString());
			}
			finally{
				closeConnection();
			}
		}
	}
	
	

	@Override
	public void send_Redirect(Response response){
		if(!(response instanceof HTTP)){
			throw new IllegalArgumentException("response is wrong type: "+response.getClass().getCanonicalName());
		}	
		else{
			HTTP corh = (HTTP) response;
			HttpResponse httpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, corh.getHttpStatus(), "Found") ;
			try {
				composeHeadersMultiple(httpResponse,corh.getHttpHeaders());
				composeHeadersSingle(httpResponse,getServerHeaders());
				
				httpResponse.setEntity(new StringEntity(corh.getBody()));
				conn.sendResponseHeader(httpResponse);
				conn.sendResponseEntity(httpResponse);
			} catch (HttpException e) {
				getLog().error(e.toString());
			} catch (IOException e) {
				getLog().error(e.toString());
			}
			finally{
				closeConnection();
			}
		}
	}
	
	
	@Override
	public void send_Proxy(Response response){
		String responseString = null;
		try {
			URIBuilder uriBuilder = new URIBuilder(response.getBody());
			responseString = WebUtil.fetchWebPage(
					uriBuilder,
					null,
					null,
					null,
					30*1000);
			response.setBody(responseString);
			send_OK(response);
		} catch (MalformedURLException e) {
			getLog().error(e.toString());
		} catch (IOException e) {
			getLog().error(e.toString());
		} catch (URISyntaxException e) {
			getLog().error(e.toString());
		}
		finally{
			closeConnection();
		}
	}
	

	@Override
	public void send_Error() {
		HttpResponse httpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND, "Not Found - Internal Error") ;
		try {
			composeHeadersSingle(httpResponse,getServerHeaders());
			httpResponse.setEntity(new StringEntity(""));
			conn.sendResponseHeader(httpResponse);
			conn.sendResponseEntity(httpResponse);
		} catch (HttpException e) {
			getLog().error(e.toString());
		} catch (IOException e) {
			getLog().error(e.toString());
		}
		finally{
			closeConnection();
		}
	}
	
	
	public void closeConnection(){
		if((conn != null) && (conn.isOpen())){
			try {
				conn.flush();
			} catch (IOException e) {
			}
			finally{
				try {
					conn.shutdown();
				} catch (IOException e) {
				}
			}
		}
	}
	
	public void closeSocket(){
		if(socket != null && (!socket.isClosed())){
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}
	
	
	@Override
	public void closeChannel(){
		closeConnection();
		conn = null;
		closeSocket();
		socket = null;
	}

}
