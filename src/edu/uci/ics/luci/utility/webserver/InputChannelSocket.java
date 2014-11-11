package edu.uci.ics.luci.utility.webserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.X509KeyManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InputChannelSocket implements InputChannel{
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(InputChannelSocket.class);
		}
		return log;
	}
	
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
	
	@Override
	public ServerSocket getServerSocket(){
		ServerSocket serverSoc = null;
		try{
			if(getSecure()){
				SSLContext sctx1 = null;
				try{
					/* See the README.txt in test/keystore for information on how to make the credentials*/
					sctx1 = SSLContext.getInstance("SSLv3");
					sctx1.init(new X509KeyManager[] { 
						new MyKeyManager(
								System.getProperty("javax.net.ssl.keyStore"),
								System.getProperty("javax.net.ssl.keyStorePassword").toCharArray(),
								System.getProperty("edu.uci.ics.luci.webserver.Alias")
								)
						}
					,null,null);
				} catch (NoSuchAlgorithmException e) {
					getLog().fatal("I'm not into this error:\n"+e);
				} catch (KeyManagementException e) {
					getLog().fatal("Problem managing keys:\n"+e);
				} catch (GeneralSecurityException e) {
					getLog().fatal("Security Exception:\n"+e);
				}
			
				//SSLServerSocketFactory ssocketFactory = (SSLServerSocketFactory) sctx1.getServerSocketFactory();
				ServerSocketFactory ssocketFactory = SSLServerSocketFactory.getDefault();
				serverSoc = ssocketFactory.createServerSocket(port);
				serverSoc.setSoTimeout(1000);
			}
			else{
				serverSoc = new ServerSocket(port);
				serverSoc.setSoTimeout(1000);
			}
		} catch (IOException e) {
			getLog().fatal(e.toString());
		}
		return serverSoc;
	}

}
