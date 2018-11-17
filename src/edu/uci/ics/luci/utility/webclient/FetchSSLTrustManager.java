/*
	Copyright 2007-2018
		Donald J. Patterson 
*/
/*
	This file is part of the Laboratory for Ubiquitous Computing java Utility package, i.e. "Utilities"

    Utilities is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Utilities is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Utilities.  If not, see <http://www.gnu.org/licenses/>.
*/

package edu.uci.ics.luci.utility.webclient;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.cache.CacheStats;

import edu.uci.ics.luci.utility.Quittable;
import edu.uci.ics.luci.utility.webclient.Fetch.Configuration;
import net.djp3.sslcert.CertificateVerificationException;
import net.djp3.sslcert.VerificationStatus;
import net.djp3.sslcert.crl.CRLVerifier;
import net.djp3.sslcert.ct.CTVerifier;
import net.djp3.sslcert.ocsp.OCSPVerifier;

public class FetchSSLTrustManager implements X509TrustManager,Quittable {
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(FetchSSLTrustManager.class);
		}
		return log;
	}
	
	static private int numberOfInstances = 0;
	static private OCSPVerifier ocspVerifier = null;
	static private CRLVerifier crlVerifier = null;
	static private CTVerifier ctVerifier = null;
	public final static OCSPVerifier.Configuration configurationOCSP = new OCSPVerifier.Configuration();
	public final static CRLVerifier.Configuration configurationCRL = new CRLVerifier.Configuration();
	public final static CTVerifier.Configuration configurationCT = new CTVerifier.Configuration();
	
	
	public static OCSPVerifier getOCSPVerifier() {
		if( ocspVerifier == null) {
			try {
				setOCSPVerifier(new OCSPVerifier(configurationOCSP));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
		return ocspVerifier;
	}
	
	
	public static void setOCSPVerifier(OCSPVerifier ocspVerifier) {
		FetchSSLTrustManager.ocspVerifier = ocspVerifier;
	}
	
	
	public static CRLVerifier getCRLVerifier() {
		if( crlVerifier == null) {
   			try {
				setCRLVerifier(new CRLVerifier(configurationCRL));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
		return crlVerifier;
	}
	
	
	public static void setCRLVerifier(CRLVerifier crlVerifier) {
		FetchSSLTrustManager.crlVerifier = crlVerifier;
	}
	
	
	public static CTVerifier getCTVerifier() {
		if( ctVerifier == null) {
   			try {
				setCTVerifier(new CTVerifier(configurationCT));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InvalidKeySpecException e) {
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
    	}
		return ctVerifier;
	}
	
	
	public static void setCTVerifier(CTVerifier ctVerifier) {
		FetchSSLTrustManager.ctVerifier = ctVerifier;
	}
	
	
	
	static void resetCaches() {
		getOCSPVerifier().resetCache();
		getCRLVerifier().resetCache();
		getCTVerifier().resetCache();
	}
	
	static CacheStats[] getCacheStats() {
		CacheStats[] results = new CacheStats[3];
		results[0] = getOCSPVerifier().getCacheStats();
		results[1] = getCRLVerifier().getCacheStats();
		results[2] = getCTVerifier().getCacheStats();
		return results;
	}
	
	public static void triggerGarbageCollection() {
		getOCSPVerifier().triggerGarbageCollection();
		getCRLVerifier().triggerGarbageCollection();
		getCTVerifier().triggerGarbageCollection();
	}
	
	
	public static void incrementNumberOfInstances() {
		numberOfInstances++;
	}
	
	
	public static void decrementNumberOfInstances() {
		numberOfInstances--;
	}
	

	private boolean isQuitting = false;

	@Override
	public void setQuitting(boolean quitting) {
		if(!isQuitting && quitting) {
			decrementNumberOfInstances();
			isQuitting = true;
			try {
				shutdownCaches();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean isQuitting() {
		return this.isQuitting;
	}
	
	public static void shutdownCaches() throws FileNotFoundException, IOException {
		if(numberOfInstances == 0) {
			try {
				getOCSPVerifier().shutdown();
			}
			finally {
				try {
					getCRLVerifier().shutdown();
				}
				finally {
					getCTVerifier().shutdown();
				}
			}
		}
		
	}
	 
    private X509TrustManager x509Tm;
	private Configuration config;
	

    public FetchSSLTrustManager(Fetch.Configuration config) throws NoSuchAlgorithmException, NoSuchProviderException, KeyStoreException {
    	if(config == null) {
    		throw new InvalidParameterException("config can't be null");
    	}
    	this.config = config;
    	
        TrustManagerFactory tmf = null;
        
        //System.out.println("Trust:"+TrustManagerFactory.getDefaultAlgorithm());
        
        //tmf = TrustManagerFactory.getInstance("X509");
        tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm(),"BCJSSE");
        
        //getLog().info(System.getProperty("keystore.type"));
        //getLog().info(KeyStore.KeyStore.getInstance("JKS"););
        
        tmf.init((KeyStore) null); // default ca certs from java
           
        // Get hold of the default trust manager
        for (TrustManager tm : tmf.getTrustManagers()) {
        	if (tm instanceof X509TrustManager) {
        		x509Tm = (X509TrustManager) tm;
        		break;
        	}
        }
        
        incrementNumberOfInstances();
    }

    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (x509Tm == null) {
            throw new CertificateException("Cacerts could not be loaded");
        }
/*
        String message = " Server Cert Issuer";
        for (X509Certificate cert : chain) {
            message += "\n"+ " " + cert.getIssuerDN().getName() + " " + cert.getSubjectDN().getName();
        }
        getLog().info(message);
        
        message =  " Client Trusted Issuers";
        X509Certificate trusts[] = x509Tm.getAcceptedIssuers();
        for (X509Certificate cert : trusts) {
            message += "\n"+ " " + cert.getIssuerDN().getName();
        }
        getLog().info(message);
        */

        List<String> errors = new ArrayList<String>(); 
        
        try {
       		boolean ocspErrorExists = false;
       		boolean crlErrorExists = false;
       		boolean ctErrorExists = false;
       		boolean brokenChainErrorExists = false;
       		
       		VerificationStatus ocsp_status = null;
       		VerificationStatus crl_status = null;
      		VerificationStatus ct_status = null;
      		
        	x509Tm.checkServerTrusted(chain, authType);
        	int n = chain.length;
        	for (int i = 0; i < n - 1; i++) {
        		X509Certificate cert = chain[i];       // child
        		X509Certificate issuer = chain[i + 1]; // parent
        		
        		if ( !brokenChainErrorExists ) { //Don't check if we already know the error exists
        			if ( !cert.getIssuerX500Principal().equals(issuer.getSubjectX500Principal()) ) {
        				brokenChainErrorExists = true;
        				if( !config.accept_SSL_with_broken_chain ) {
        					errors.add("Certificate do not chain");
        				}
        				else {
        					config.getResult().SSL_ACCEPTED_WITH_BROKEN_CHAIN = true;
        				}
        			}
        		}
       			
        		//OCSP revocation protocol
        		if( config.check_OCSP && !ocspErrorExists) {
   					ocsp_status = getOCSPVerifier().checkRevocationStatus(cert, issuer,chain);
       				
   					if (ocsp_status.getStatus() == VerificationStatus.BAD ) {
        				ocspErrorExists = true;
   						if(!config.accept_SSL_with_revoked_cert_by_OCSP) {
   							errors.add("Certificate revoked by OCSP on "+SimpleDateFormat.getInstance().format(ocsp_status.getRevokeDate()));
   						}
   						else {
   							config.getResult().SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP = true;
   						}
   					}
        		}
        		
        		//CRL revocation protocol
        		if( config.check_CRL && !crlErrorExists) {
       				crl_status = getCRLVerifier().checkRevocationStatus(cert, issuer,chain);
        				
       				if (crl_status.getStatus() == VerificationStatus.BAD ) {
       					crlErrorExists = true;
       					if(!config.accept_SSL_with_revoked_cert_by_CRL) {
       						errors.add("Certificate revoked by CRL on "+SimpleDateFormat.getInstance().format(crl_status.getRevokeDate()));
       					}
       					else {
       						config.getResult().SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL = true;
       					}
        			}
        		}
        		
        		if( (i ==0) && ( config.check_CT ) && !ctErrorExists) {
       				ct_status = getCTVerifier().checkRevocationStatus(cert, issuer,chain);
        					
       				if (ct_status.getStatus() == VerificationStatus.BAD ) {
        				ctErrorExists = true;
   						if(!config.accept_SSL_with_failed_CT) {
   							errors.add("Certificate did not have acceptable CT credentials");
   						}
   						else {
   							config.getResult().SSL_ACCEPTED_WITH_FAILED_CT = true;
   						}
        			}
        		}
        	}
        }catch(java.security.cert.CertificateException e) {
        	/* If we could identify different kinds of failures we could have finer grain control
        	 * over what the user can configuration for accepting, but as it stands several errors
        	 * all look the same here
        	 */
        	if(e.getCause() instanceof java.security.cert.CertPathBuilderException) {
        		//Something went wrong, check if we are going to let it continue to be wrong
        		
        		//Possible reasons include a self-signed key
        		//This code just demonstrates checking that
        		for(X509Certificate cert:chain) {
        			try {
        				PublicKey key = cert.getPublicKey();
        				cert.verify(key); 
       					getLog().info("SSL certificate has a self-signed component");
        			} catch (InvalidKeyException e1) {
        				//Okay, just checking the key
					} catch (NoSuchAlgorithmException e1) {
					} catch (NoSuchProviderException e1) {
					} catch (SignatureException e1) {
					}
        		}
        		
				if(!config.accept_SSL_bad_cert_types) {
					errors.add(e.getLocalizedMessage());
				}
				else {
					config.getResult().SSL_ACCEPTED_BAD_CERT_TYPE = true;
				}
        	}
        	else {
        		throw e;
        	}
        }
        
        if(errors.size() > 0) {
        	StringBuffer message = new StringBuffer("Certificate is not trusted:\n");
        	for(String error:errors) {
        		message.append("\t"+error+"\n");
        	}
        	throw new CertificateVerificationException(message.toString());
        }
        else {
        	getLog().info(" Certificate Validated");
        }
    }

    public X509Certificate[] getAcceptedIssuers() {
        return x509Tm.getAcceptedIssuers();
    }


}

