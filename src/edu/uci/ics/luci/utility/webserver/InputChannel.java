package edu.uci.ics.luci.utility.webserver;

import java.net.ServerSocket;

public interface InputChannel {
	
	//TODO:These only makes sense for sockets and need to be refactored
	int getPort();
	boolean getSecure();
	public ServerSocket getServerSocket();


}
