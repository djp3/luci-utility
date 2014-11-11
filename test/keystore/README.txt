In order to test the secure file service it is necessary to install a bunch of
self-signed certificates of authority and then server certs signed by that
certificate authority.

If you want to do this for production, then you need to get your ISP (like
GoDaddy) to sign your csr.  Then you need to import their chain of certificates
in to your keystore instead. You can skip everything on the linux box.

I followed http://www.zytrax.com/tech/survival/ssl.html#root-ca "Method 3" to create a self-signed root certificate:
	1) logged into a linux box
	2) made a working directory with a copy of CA.pl and openssl.conf that I modified based on the tutorial
	3) enabled my user account to get randomness:
			sudo chown djp3:djp3 ~/.rnd
	4) I backed up /etc/ssl/openssl.cnf and put my modified version in place
	5) Then I created a new certificate authority:
			./ca.pl -newca
				pem password was "password"
				challenge password was blank
			result:
				Certificate Details:
					Serial Number:
			   			c6:0d:1c:26:e0:5b:54:0f
					Validity
			   			Not Before: Nov 11 22:20:04 2014 GMT
			   			Not After : Nov  8 22:20:04 2024 GMT
					Subject:
			      		countryName               = US
						stateOrProvinceName       = CA
					    organizationName          = LUCI
			      		commonName                = ca.luci.ics.uci.edu
					X509v3 extensions:
			      		X509v3 Subject Key Identifier: 
					   		03:D6:A4:B2:00:75:1B:59:E5:D6:B5:87:56:C6:C1:5F:19:33:30:9C
						X509v3 Authority Key Identifier: 
							keyid:03:D6:A4:B2:00:75:1B:59:E5:D6:B5:87:56:C6:C1:5F:19:33:30:9C
		
						X509v3 Basic Constraints: 
							CA:TRUE
				Certificate is to be certified until Nov  8 22:20:04 2024 GMT (3650 days)

	6) Then back on my Mac I created a new server keystore:
			keytool -keysize 4096 -genkey -alias server -keystore ./mySrvKeystore -keyalg RSA -validity 3650
	7) The I created a certificate signing request:
			keytool -keystore ./mySrvKeystore -certreq -alias server -keyalg rsa -file server.csr
	8) Then I moved the request to the linux box and signed it
			openssl ca -policy policy_anything -in server.csr -out server.cer
			result:
				Certificate Details:
        			Serial Number:
			            c6:0d:1c:26:e0:5b:54:11
			        Validity
			            Not Before: Nov 11 22:45:41 2014 GMT
			            Not After : Nov  8 22:45:41 2024 GMT
			        Subject:
			            countryName               = US
			            stateOrProvinceName       = CA
			            localityName              = Irvine
			            organizationName          = LUCI
			            organizationalUnitName    = LUCI
			            commonName                = luci.ics.uci.edu
			        X509v3 extensions:
			            X509v3 Basic Constraints: 
			                CA:FALSE
			            Netscape Comment: 
			                OpenSSL Generated Certificate
			            X509v3 Subject Key Identifier: 
			                DC:D8:71:5B:07:37:FF:E0:B4:13:E5:02:9B:23:B2:B9:06:B1:AC:27
			            X509v3 Authority Key Identifier: 
			                keyid:03:D6:A4:B2:00:75:1B:59:E5:D6:B5:87:56:C6:C1:5F:19:33:30:9C

				Certificate is to be certified until Nov  8 22:45:41 2024 GMT (3650 days)

	9) Then I moved the self-signed ca certificate back to my Mac
			ca/cacert.pem -> ca.cer
	10) Then I imported and trusted the self-signed ca certificate on my Mac
			keytool -import -keystore ./mySrvKeystore -file ./ca.cer -alias theCARoot
	11) Then I imported my signed servercertificate
			keytool -import -keystore ./mySrvKeystore -file server.cer -alias server
	12) Then I created a truststore for the client:
			keytool -import -file ./ca.cer -alias client -keystore ./myClientTrustStore
				password was "password"
	13) Then I restored the /etc/ssl/openssl.cnf on my linux box
	14) The truststore and keystore were then usable by the Java code to execute a https connection
