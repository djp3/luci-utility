package edu.uci.ics.luci.utility.webserver.input.channel;

import java.util.concurrent.Callable;

import edu.uci.ics.luci.utility.datastructure.Pair;
import edu.uci.ics.luci.utility.webserver.Channel;
import edu.uci.ics.luci.utility.webserver.input.request.Request;
import edu.uci.ics.luci.utility.webserver.output.channel.Output;


public abstract class Input extends Channel{
	
	abstract public Callable<Pair<Request, Output>> waitForIncomingRequest();
	
	//TODO:These only makes sense for sockets and need to be refactored
	abstract public int getPort();
	abstract public boolean getSecure();
}
