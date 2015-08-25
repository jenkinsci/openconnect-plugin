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
import hudson.tasks.Publisher;
import hudson.util.Scrambler;
import net.sf.json.JSONObject;

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
    private final boolean noCertCheck;

    @DataBoundConstructor
    public OpenConnectPlugin(String username,
                                 String password,
                                 String url,
                                 String authGroup,
                                 boolean noCertCheck) {

    	this.username = Scrambler.scramble(username);
    	this.password = Scrambler.scramble(password);
    	this.url = url;
    	this.authGroup = authGroup;
    	this.noCertCheck = noCertCheck;
    }
    
    public String getUsername() {
    	return Scrambler.descramble(username);
    }
    
    public String getPassword() {
    	return Scrambler.descramble(password);
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
