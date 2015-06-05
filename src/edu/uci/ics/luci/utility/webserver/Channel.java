package edu.uci.ics.luci.utility.webserver;

public abstract class Channel {
	public enum Protocol{
		 UNKNOWN,HTTP_GET,HTTP_POST,P2P;
	}
	
	abstract public void closeChannel();
}
