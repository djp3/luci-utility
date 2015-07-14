package edu.uci.ics.luci.utility.webserver.output.channel;

import java.net.Socket;
import java.util.Map;

import edu.uci.ics.luci.utility.webserver.Channel;
import edu.uci.ics.luci.utility.webserver.event.result.api.APIEventResult;

public abstract class Output extends Channel{
	
	public abstract void send_OK(APIEventResult response);
	public abstract void send_Redirect(APIEventResult response);
	public abstract void send_Proxy(APIEventResult response);
	public abstract void send_Error();
	
	public abstract APIEventResult makeOutputChannelResponse();
	
	/**TODO These only make sense for sockets, needs refactoring **/
	public abstract Socket getSocket();
	public abstract Map<String,String> getServerHeaders();

}
