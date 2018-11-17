package edu.uci.ics.luci.utility.webclient;

import java.security.InvalidParameterException;

import javax.net.ssl.SSLSession;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;

import edu.uci.ics.luci.utility.webclient.Fetch.Configuration;

public class FetchHostnameVerifier implements javax.net.ssl.HostnameVerifier{
	
	private javax.net.ssl.HostnameVerifier hv = null;
	private Configuration config = null;
	
	FetchHostnameVerifier(Fetch.Configuration config){
		this.config = config;
		this.hv = SSLConnectionSocketFactory.getDefaultHostnameVerifier();
	}


	@Override
	public boolean verify(String hostname, SSLSession session) {
		boolean protocolOK;
		boolean cipherOK;
		boolean hostnameOK;
		
		//First check TLS levels
		protocolOK = true;
		switch(session.getProtocol()){
			case "TLSv1.2": if(config.accept_SSL_PROTOCOL_TLSV1_2){
								this.config.getResult().SSL_CONNECTED_WITH_PROTOCOL_TLSV1_2 = true;
							} else {	
								protocolOK = false;
							}
							break;
			case "TLSv1.1": if(config.accept_SSL_PROTOCOL_TLSV1_1){
								this.config.getResult().SSL_CONNNECTED_WITH_PROTOCOL_TLSV1_1 = true;
							} else{
								protocolOK = false;
							}
							break;
			case "TLSv1"  : if(config.accept_SSL_PROTOCOL_TLSV1_0){
								this.config.getResult().SSL_CONNECTED_WITH_PROTOCOL_TLSV1_0 = true;
							} else{
								protocolOK = false;
							}
							break;
			default: throw new InvalidParameterException("Unhandled protocol: "+session.getProtocol());
		}
		
		//Check Ciphers
		cipherOK = true;
		if( session.getCipherSuite().contains("_CBC_")) {
			if(config.accept_SSL_CIPHER_CBC){
				this.config.getResult().SSL_CONNECTED_WITH_CIPHER_CBC = true;
			} else {	
				cipherOK = false;
			}
		}
				
		boolean hostnameMatches = hv.verify(hostname, session);
		if(!hostnameMatches && config.accept_SSL_with_hostname_not_matching_cert) {
			this.config.getResult().SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT = true;
			hostnameOK = true;
		}
		else {
			hostnameOK = hostnameMatches;
		}
		
		if(!protocolOK) {
			throw new IllegalStateException("Protocol "+session.getProtocol()+" not allowed by configuration");
		}
		if(!cipherOK) {
			throw new IllegalStateException("Cipher "+session.getCipherSuite()+" not allowed by configuration");
		}
		return hostnameOK;
	}
}
