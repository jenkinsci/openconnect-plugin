package org.jenkinsci.plugins.openconnect;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenConnectStream extends BufferedInputStream implements Runnable {
	
	private Pattern invalidCertPattern = Pattern.compile("Certificate from VPN server \".*?\" failed verification.");
	private Pattern usernamePattern = Pattern.compile("Username:");
	private Pattern passwordPattern = Pattern.compile("Password:");
	private Pattern connectedPattern = Pattern.compile("Established DTLS connection");
	private Pattern pidPattern = Pattern.compile("Continuing in background; pid (\\d+)");
	private String pid;
	private boolean terminate;
    private static final String className = OpenConnectStream.class.getName();
    private static Logger log = Logger.getLogger(className);	
		
	public OpenConnectStream(InputStream inputStream) {
		super(inputStream);
	}

	@Override
	public void run() {
		try {
			byte[] data = new byte[4096];
			while(!terminate) {				
				int available = available();
				if(available > 0) {
					if(read(data,0,available) != -1) {
						processData(new String(data,0,available));
					}
				} else {
					Thread.sleep(300); //sleep to reduce CPU cycles
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
			log.severe("Stream shutdown unexpectedly: "+e.getMessage());			
		}
	}
	
	private void processData(String data) {
		log.info(data);
		OpenConnect process = OpenConnect.getInstance();
		Matcher invalidCertMatcher = invalidCertPattern.matcher(data);
		if(invalidCertMatcher.find()) {
			log.warning("Invalid certificate found, shutting down");
			OpenConnect.getInstance().disconnect();
			return;
		}
		Matcher usernameMatcher = usernamePattern.matcher(data);
		if(usernameMatcher.find()) {
			process.writeString(process.getUsername()+"\n");
			return;
		}
		Matcher passwordMatcher = passwordPattern.matcher(data);
		if(passwordMatcher.find()) {
			process.writeString(process.getPassword()+"\n");
			return;
		}
		Matcher pidMatcher = pidPattern.matcher(data);
		if(pidMatcher.find()) {
			pid = pidMatcher.group(1);
			process.setProcessId(pid);
			process.setConnected(true);		
		}
		Matcher connectedMatcher = connectedPattern.matcher(data);
		if(connectedMatcher.find()) {
			process.setConnected(true);
		}		
	}
	
	public void terminate() {
		this.terminate = true;
	}

}
