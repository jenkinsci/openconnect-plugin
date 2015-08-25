package org.jenkinsci.plugins.openconnect;

import java.io.BufferedInputStream;
import java.io.InputStream;
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
			System.err.println("Stream shutdown unexpectedly: "+e.getMessage());			
		}
	}
	
	private void processData(String data) {
		System.out.println(data);
		Matcher invalidCertMatcher = invalidCertPattern.matcher(data);
		if(invalidCertMatcher.find()) {
			System.err.println("Invalid certificate found, shutting down");
			OpenConnect.getInstance().disconnect();
			return;
		}
		Matcher usernameMatcher = usernamePattern.matcher(data);
		if(usernameMatcher.find()) {
			OpenConnect process = OpenConnect.getInstance();
			process.writeString(process.getUsername()+"\n");
			return;
		}
		Matcher passwordMatcher = passwordPattern.matcher(data);
		if(passwordMatcher.find()) {
			OpenConnect process = OpenConnect.getInstance();
			process.writeString(process.getPassword()+"\n");
			return;
		}
		Matcher pidMatcher = pidPattern.matcher(data);
		if(pidMatcher.find()) {
			pid = pidMatcher.group(1);
			OpenConnect process = OpenConnect.getInstance();
			process.setProcessId(pid);
			System.out.println("Connection established!");		
		}
		Matcher connectedMatcher = connectedPattern.matcher(data);
		if(connectedMatcher.find()) {
			System.out.println("Connection established!");
		}
		
	}
	
	public void terminate() {
		this.terminate = true;
	}

}
