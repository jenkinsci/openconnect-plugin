package org.jenkinsci.plugins.openconnect;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class OpenConnect {

	private static OpenConnect instance = new OpenConnect();
	private String username;
	private String password;
	private String authGroup;
	private String url;
	private boolean noCertCheck;
	private String processId;
	private Process process;
	private Process disconnectProcess;
	private OpenConnectStream inputStreamReader;
	private OpenConnectStream errorStreamReader;
	private OpenConnectStream disconnectInputStreamReader;
	private OpenConnectStream disconnectErrorStreamReader;
	private Thread inputStreamThread;
	private Thread errorStreamThread;
	private Thread disconnectInputStreamThread;
	private Thread disconnectErrorStreamThread;
	
	private OpenConnect() {}
	
	public static OpenConnect getInstance() {
		return instance;
	}
	
	public void connect() {
		if(process != null) {
			throw new UnsupportedOperationException("VPN is already in use");
		}
		new Thread(new Runnable() {
			
			public void run() {
				List<String> commands = new ArrayList<String>();
				commands.add("/bin/bash");
				commands.add("-c");
				commands.add(buildCommand());
				try {
					process = Runtime.getRuntime().exec(commands.toArray(new String[commands.size()]));
					loadProcessId();
					process.getOutputStream();
					getInputStreamThread().start();
					getErrorStreamThread().start();
					process.waitFor();
					System.out.println("Exited VPN Process");
				} catch(Exception e) {
					e.printStackTrace();
					disconnect();
					System.err.println("Failed to execute VPN console: "+e.getMessage());
				}				
			}
		}).start();
	}
	
	private String buildCommand() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("sudo openconnect ");
		if(isNoCertCheck()) {
			buffer.append("--no-cert-check ");
		}
		if(getAuthGroup() != null) {
			buffer.append("--authgroup=");
			buffer.append(getAuthGroup());
			buffer.append(" ");
		}
		buffer.append(getUrl());
		return buffer.toString();
	}
	
	private void loadProcessId() {
		try {
			Field f = process.getClass().getDeclaredField("pid");
			f.setAccessible(true);
			setProcessId(String.valueOf(f.getInt(process)));
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void disconnect() {
		try {
			//only try to shutdown vpn if a process exists
			if(getProcessId() != null) {
				List<String> commands = new ArrayList<String>();
				commands.add("/bin/bash");
				commands.add("-c");
				commands.add("sudo kill -2 "+getProcessId());
				disconnectProcess = Runtime.getRuntime().exec(commands.toArray(new String[commands.size()]));
				disconnectProcess.getOutputStream();			
				getDisconnectInputStreamThread().start();
				getDisconnectErrorStreamThread().start();
				disconnectProcess.waitFor();
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			cleanup();
		}
	}
	
	public void writeString(String data) {
		writeBytes(data.getBytes());
	}
	
	public void writeBytes(byte[] data) {
		if(process != null) {
			try {
				process.getOutputStream().write(data);
				process.getOutputStream().flush();
			} catch(Exception e) {
				System.out.println("Failed to write to VPN console: "+e.getMessage());
			}
		}
	}
	
	private Thread getDisconnectInputStreamThread() {
		if(disconnectInputStreamThread == null) {
			disconnectInputStreamThread = new Thread(getDisconnectInputStreamReader());	
			disconnectInputStreamThread.setDaemon(true);
		}
		return disconnectInputStreamThread;
	}
	
	private Thread getDisconnectErrorStreamThread() {
		if(disconnectErrorStreamThread == null) {
			disconnectErrorStreamThread = new Thread(getDisconnectErrorStreamReader());	
			disconnectErrorStreamThread.setDaemon(true);
		}
		return disconnectErrorStreamThread;
	}		
	
	private Thread getInputStreamThread() {
		if(inputStreamThread == null) {
			inputStreamThread = new Thread(getInputStreamReader());	
			inputStreamThread.setDaemon(true);
		}
		return inputStreamThread;
	}
	
	private Thread getErrorStreamThread() {
		if(errorStreamThread == null) {
			errorStreamThread = new Thread(getErrorStreamReader());	
			errorStreamThread.setDaemon(true);
		}
		return errorStreamThread;
	}
	
	private OpenConnectStream getDisconnectInputStreamReader() {
		if(disconnectInputStreamReader == null) {
			disconnectInputStreamReader = new OpenConnectStream(disconnectProcess.getInputStream());	
		}
		return disconnectInputStreamReader;
	}	
	
	private OpenConnectStream getDisconnectErrorStreamReader() {
		if(disconnectErrorStreamReader == null) {
			disconnectErrorStreamReader = new OpenConnectStream(disconnectProcess.getErrorStream());	
		}
		return disconnectErrorStreamReader;
	}	
	
	private OpenConnectStream getInputStreamReader() {
		if(inputStreamReader == null) {
			inputStreamReader = new OpenConnectStream(process.getInputStream());	
		}
		return inputStreamReader;
	}
	
	private OpenConnectStream getErrorStreamReader() {
		if(errorStreamReader == null) {
			errorStreamReader = new OpenConnectStream(process.getErrorStream());	
		}
		return errorStreamReader;
	}	
	
	private void cleanup() {	
		//reset attributes for next connection
		System.out.println("Cleaning Up!");
		username = null;
		password = null;
		authGroup = null;
		url = null;
		noCertCheck = false;
		processId = null;		
		if(process != null) {	
			System.out.println("Cleaning Up Process!");
			getInputStreamReader().terminate();
			getErrorStreamReader().terminate();
			process.destroy();			
			inputStreamReader = null;
			errorStreamReader = null;
			inputStreamThread = null;
			errorStreamThread = null;
			process = null;
		}
		if(disconnectProcess != null) {
			System.out.println("Cleaning Up Disconnect Process!");
			getDisconnectErrorStreamReader().terminate();
			getDisconnectInputStreamReader().terminate();
			disconnectProcess.destroy();
			disconnectErrorStreamReader = null;
			disconnectInputStreamReader = null;
			disconnectInputStreamThread = null;
			disconnectErrorStreamThread = null;
			disconnectProcess = null;
		}
	}
	
	public boolean isNoCertCheck() {
		return noCertCheck;
	}

	public void setNoCertCheck(boolean noCertCheck) {
		this.noCertCheck = noCertCheck;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getAuthGroup() {
		return authGroup;
	}

	public void setAuthGroup(String authGroup) {
		this.authGroup = authGroup;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getProcessId() {
		return processId;
	}

	public void setProcessId(String processId) {
		this.processId = processId;
	}
}
