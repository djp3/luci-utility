package edu.uci.ics.luci.utility.webserver;

public class InputChannelSocket implements InputChannel{
	
	private int port;
	private boolean secure;

	public InputChannelSocket(int port, boolean secure){
		this.port = port;
		this.secure = secure;
		
	}

	@Override
	public int getPort(){
		return this.port;
	}

	@Override
	public boolean getSecure() {
		return secure;
	}

	public void setSecure(Boolean secure) {
		this.secure = secure;
	}


}
