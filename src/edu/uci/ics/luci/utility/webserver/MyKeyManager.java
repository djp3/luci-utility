/*
	Copyright 2007-2014
		University of California, Irvine (c/o Donald J. Patterson)
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

package edu.uci.ics.luci.utility.webserver;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.X509KeyManager;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public class MyKeyManager implements X509KeyManager {
    private KeyStore keyStore;
    private String alias;
    private char[] password;

    MyKeyManager(String keyStoreFile, char[] password, String alias) throws IOException, GeneralSecurityException
    {
    	InputStream stream = null;
    	try{
    		this.alias = alias;
    		this.password = password;
    		stream = new FileInputStream(keyStoreFile);
    		keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    		keyStore.load(stream, password);
    	}
    	finally{
    		if(stream != null){
    			stream.close();
    		}
    		
    	}
    }

    public PrivateKey getPrivateKey(String alias) {
        try {
        	return (PrivateKey) keyStore.getKey(alias, password);
        } catch (UnrecoverableKeyException e) {
        	return null;
		} catch (KeyStoreException e) {
        	return null;
		} catch (NoSuchAlgorithmException e) {
        	return null;
        } catch (RuntimeException e) {
        	return null;
        }
    }

    public X509Certificate[] getCertificateChain(String alias) {
    	X509Certificate[] dummy = new X509Certificate[0];
        try {
            Certificate[] certs = keyStore.getCertificateChain(alias);
            if (certs == null || certs.length == 0){
                return dummy;
            }
            X509Certificate[] x509 = new X509Certificate[certs.length];
            for (int i = 0; i < certs.length; i++){
                x509[i] = (X509Certificate)certs[i];
            }
            return x509;
        } catch (KeyStoreException e) {
        	return dummy;
        } catch (RuntimeException e) {
        	return dummy;
        }
    }

    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return alias;
    }

    public String[] getClientAliases(String parm1, Principal[] parm2) {
        throw new UnsupportedOperationException("Method getClientAliases() not yet implemented.");
    }

    public String chooseClientAlias(String keyTypes[], Principal[] issuers, Socket socket) {
        throw new UnsupportedOperationException("Method chooseClientAlias() not yet implemented.");
    }

    public String[] getServerAliases(String parm1, Principal[] parm2) {
        return new String[] { alias };
    }

    public String chooseServerAlias(String parm1, Principal[] parm2) {
        return alias;
    }
}
