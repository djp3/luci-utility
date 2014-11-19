package edu.uci.ics.luci.utility.webserver.output.channel;

import java.net.Socket;
import java.util.Map;

import edu.uci.ics.luci.utility.webserver.Channel;
import edu.uci.ics.luci.utility.webserver.output.response.Response;

public abstract class Output extends Channel{
	
	public abstract void send_OK(Response response);
	public abstract void send_Redirect(Response response);
	public abstract void send_Proxy(Response response);
	public abstract void send_NotFound();
	
	public abstract Response makeOutputChannelResponse();
	
	/**TODO These only make sense for sockets, needs refactoring **/
	public abstract Socket getSocket();
	public abstract Map<String,String> getServerHeaders();

}
