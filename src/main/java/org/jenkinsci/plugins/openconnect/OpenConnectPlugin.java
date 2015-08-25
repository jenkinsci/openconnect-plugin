package org.jenkinsci.plugins.openconnect;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.util.Scrambler;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author Greg Peters
 */
public class OpenConnectPlugin extends Builder {

    private final String username;
    private final String password;
    private final String url;
    private final String authGroup;
    private final String connectTimeout;
    private final boolean noCertCheck;

    @DataBoundConstructor
    public OpenConnectPlugin(String username,
                                 String password,
                                 String url,
                                 String authGroup,
                                 String connectTimeout,
                                 boolean noCertCheck) {

    	this.username = Scrambler.scramble(username);
    	this.password = Scrambler.scramble(password);
    	this.url = url;
    	this.authGroup = authGroup;
    	this.noCertCheck = noCertCheck;
    	this.connectTimeout = connectTimeout;
    }
    
    public String getUsername() {
    	return Scrambler.descramble(username);
    }
    
    public String getPassword() {
    	return Scrambler.descramble(password);
    }
    
    public String getConnectTimeout() {
    	return connectTimeout;
    }
    
    public String getUrl() {
    	return url;
    }
    
    public String getAuthGroup() {
    	return authGroup;
    }
    
    public boolean isNoCertCheck() {
    	return noCertCheck;
    }


    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {    	
    	OpenConnect vpn = OpenConnect.getInstance();
        vpn.setUsername(getUsername());
       	vpn.setPassword(getPassword());
       	vpn.setNoCertCheck(isNoCertCheck());
       	vpn.setAuthGroup(getAuthGroup());
       	vpn.setUrl(getUrl());
       	listener.getLogger().println("Connecting to VPN @ "+vpn.getUrl());
       	vpn.connect();
   		int timeoutInSeconds = StringUtils.trimToNull(getConnectTimeout()) == null?30:Integer.parseInt(getConnectTimeout().trim());
   		while(!vpn.isError() && !vpn.isConnected()) {
   			try {
   				Thread.sleep(1000);
   				if(--timeoutInSeconds == 0) {
   					vpn.setError(true);
   				}
   			} catch (InterruptedException e) {
   				e.printStackTrace();
   				vpn.setError(true);
   				listener.getLogger().println("Error while detecting if VPN is connected: "+e.getMessage());
   			}
   		}
   		if(vpn.isError()) {
   			vpn.disconnect();
   			build.setResult(Result.FAILURE);
   			return false;
   		}
        return true;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {


        public DescriptorImpl() {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "Connect To Cisco AnyConnect VPN";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req,formData);
        }
    }
}
