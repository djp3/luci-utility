In order to test the secure file service it is necessary to install a bunch of
self-signed certificates of authority and then server certs signed by that
certificate authority.

If you want to do this for production, then you need to get your ISP (like
GoDaddy) to sign your csr.  Then you need to import their chain of certificates
in to your keystore instead. You can skip everything on the linux box. Some hints at the end.

I followed http://www.zytrax.com/tech/survival/ssl.html#root-ca "Method 3" to create a self-signed root certificate:
	1) logged into a linux box (e.g., the linux virtual machine on my mac )
	2) moved to a working directory (/etc/ssl) with a copy of CA.pl and openssl.conf that I modified based on the tutorial
		added a subdirectory "ca" that was reflected in CA.pl and openssl.conf and was owned by djp3:djp3
	3) (did not need to do this on the mac vm)
			enabled my user account to get randomness:
			sudo chown djp3:djp3 ~/.rnd
	4) I backed up /etc/ssl/openssl.cnf and put my modified version in place
	5) Then I created a new certificate authority:
			export SSLEAY_CONFIG="-config /etc/ssl/openssl.cnf";export OPENSSL="/usr/bin/openssl"; ./ca.pl -newca
				pem password was logged in password file
				challenge password was blank
			result:
			Certificate Details:
				Serial Number: 18242003245052226246 (0xfd289d4d14dc6ac6)
					Validity
						Not Before: Oct  8 23:00:58 2018 GMT
						Not After : Oct  5 23:00:58 2028 GMT
			        Subject:
						countryName               = US
						stateOrProvinceName       = CA
						organizationName          = X.509
						organizationalUnitName    = CA ROOT
						commonName                = witnessthismedia.org
						emailAddress              = djp3@witnessthismedia.org
						localityName              = Santa Barbara
					X509v3 extensions:
						X509v3 Subject Key Identifier: 
							58:8D:BE:37:9D:8D:A5:10:A0:79:37:3A:F6:0C:F8:E2:AA:D0:A1:AF
						X509v3 Authority Key Identifier: 
							keyid:58:8D:BE:37:9D:8D:A5:10:A0:79:37:3A:F6:0C:F8:E2:AA:D0:A1:AF

					X509v3 Basic Constraints: 
						CA:TRUE
			Certificate is to be certified until Oct  5 23:00:58 2028 GMT (3650 days)

	6) Then back on my Mac I created a new server keystore:
			keytool -keysize 4096 -genkey -alias server -keystore ./mySrvKeystore -keyalg RSA -validity 3650
	7) The I created a certificate signing request:
			keytool -keystore ./mySrvKeystore -certreq -alias server -keyalg rsa -file server.csr
	8) Then I moved the request (server.csr) to the linux box and signed it
			openssl ca -config /etc/ssl/openssl.cnf -policy policy_anything -in server.csr -out ca/server.cer
			result:
			Certificate Details:
				Serial Number:
					fd:28:9d:4d:14:dc:6a:c7
				Validity
					Not Before: Oct  8 23:08:56 2018 GMT
					Not After : Sep  1 23:08:56 2021 GMT
				Subject:
					countryName               = US
					stateOrProvinceName       = CA
					localityName              = Santa Barbara
					organizationName          = X.509
					organizationalUnitName    = Keystore
					commonName                = Donald Patterson
				X509v3 extensions:
					X509v3 Basic Constraints: 
						CA:FALSE
					Netscape Comment: 
						OpenSSL Generated Certificate
					X509v3 Subject Key Identifier: 
						12:DC:49:E1:2D:2C:2A:22:3B:AA:87:66:62:4E:26:2C:63:0B:B0:9A
					X509v3 Authority Key Identifier: 
						keyid:58:8D:BE:37:9D:8D:A5:10:A0:79:37:3A:F6:0C:F8:E2:AA:D0:A1:AF
			Certificate is to be certified until Sep  1 23:08:56 2021 GMT (1059 days)

	9) Then I moved the self-signed ca certificate back to my Mac
			ca/cacert.pem -> ca.cer
	10) Then I imported and trusted the self-signed ca certificate on my Mac
			keytool -import -keystore ./mySrvKeystore -file ./ca.cer -alias theCARoot
	11) Then I imported my signed servercertificate
			keytool -import -keystore ./mySrvKeystore -file server.cer -alias server
	12) Then I created a truststore for the client:
			keytool -import -file ./ca.cer -alias client -keystore ./myClientTrustStore
				password was stored in my password file
	13) Then I restored the /etc/ssl/openssl.cnf on my linux box
	14) The truststore and keystore were then usable by the Java code to execute a https connection
	
	
	
Hints for setting up a production environment:	
	I was in the middle of trying to do the certificates and I'm trying to get the keystore to work with a Go Daddy certificate.
	All the files that start with "gd" came after gd signed my request
	
	I was using the instructions on this page
		http://weblogs.java.net/blog/kalali/archive/2010/03/01/how-prepare-and-install-godaddy-ssl-certificate-glassfish-v3

	These are some relevant commands that I issued:

	keytool -keysize 4096 -genkey -alias swayr.com -keyalg RSA -keystore ./mySrvKeystore -validity 3650
	keytool -certreq -alias swayr.com -keystore ./mySrvKeystore  -file server.cer 
	keytool -import -alias root -keystore ./mySrvKeystore -trustcacerts -file temp/gd_bundle.crt 
	keytool -import -alias cross -keystore ./mySrvKeystore  -trustcacerts -file ./temp/gd_cross_intermediate.crt 
	keytool -import -alias intermed -keystore ./mySrvKeystore -trustcacerts -file ./temp/gd_intermediate.crt 
	keytool -import -alias swayr.com -keystore ./mySrvKeystore -trustcacerts -file temp/swayr.com.crt 


	
