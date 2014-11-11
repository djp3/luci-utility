package edu.uci.ics.luci.utility.webserver;

public interface InputChannel {
	
	//TODO:These only makes sense for sockets and need to be refactored
	int getPort();
	boolean getSecure();

}
