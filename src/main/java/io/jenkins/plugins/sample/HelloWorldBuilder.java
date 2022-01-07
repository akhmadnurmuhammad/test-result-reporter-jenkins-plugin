package io.jenkins.plugins.sample;

import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HelloWorldBuilder extends Builder implements SimpleBuildStep {

    private final String name;
    private final String Token;
    private final String ReportName;
    private final String ProjectID;
    private final String ProjectName;
    private final String AppVersion;
    private final String Environment;
    private final String FileLocation;
    private final String Type;
    private boolean useFrench;

    @DataBoundConstructor
    public HelloWorldBuilder(String name,String Token,String ReportName,String ProjectID,String ProjectName,String AppVersion,String Environment,String FileLocation,String Type) {
        this.name = name;
        this.Token = Token;
        this.ReportName = ReportName;
        this.ProjectID = ProjectID;
        this.ProjectName = ProjectName;
        this.AppVersion = AppVersion;
        this.Environment = Environment;
        this.FileLocation = FileLocation;
        this.Type = Type;
    }

    public String getName() {
        return name;
    }

    public String getToken() {
        return Token;
    }

    public String getReportName() {
        return ReportName;
    }

    public String getProjectID() {
        return ProjectID;
    }

    public String getProjectName() {
        return ProjectName;
    }

    public String getAppVersion() {
        return AppVersion;
    }

    public String getEnvironment() {
        return Environment;
    }

    public String getFileLocation() {
        return FileLocation;
    }

    public String getType() {
        return Type;
    }

    public boolean isUseFrench() {
        return useFrench;
    }

    @DataBoundSetter
    public void setUseFrench(boolean useFrench) {
        this.useFrench = useFrench;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        if (useFrench) {
            listener.getLogger().println("Bonjour, " + name + "!");
        } else {
            listener.getLogger().println("Performing post report, " + ReportName + "!");
        }

        this.postReport(listener);
    }

    public void postReport(TaskListener listener) throws IOException,
            InterruptedException {
        
        String url = "http://localhost:3001/api/v1/report/"+this.getType()+"/store";
        
        String boundary = "-------------oiawn4tp89n4e9p5";
        Map<Object, Object> data = new HashMap<>();
        
        // some form fields
        data.put("Token", this.getToken());
        data.put("ReportName", this.getReportName());
        data.put("ProjectID", this.getProjectID());
        data.put("ProjectName", this.getProjectName());
        data.put("AppVersion", this.getAppVersion());
        data.put("Environment", this.getEnvironment());
        // file upload
        data.put("Data", Paths.get(this.getFileLocation()));
        
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
                .headers("Content-Type",
                        "multipart/form-data; boundary=" + boundary)
                .POST(oMultipartData(data, boundary))
                .build();
        
        HttpClient client = HttpClient.newHttpClient();
 
        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());
        System.out.println("response statusCode = " + response.statusCode());
        System.out.println("response body = " + response.body());

        listener.getLogger().println("response statusCode = " + response.statusCode());
        listener.getLogger().println("response body = " + response.body());
 
    }
 
    public static BodyPublisher oMultipartData(Map<Object, Object> data,
            String boundary) throws IOException {
        ArrayList<byte[]> byteArrays = new ArrayList<byte[]>();
        byte[] separator = ("--" + boundary
                + "\r\nContent-Disposition: form-data; name=")
                        .getBytes(StandardCharsets.UTF_8);
        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            byteArrays.add(separator);
 
            if (entry.getValue() instanceof Path) {
                Path path = (Path) entry.getValue();
                String mimeType = Files.probeContentType(path);
                byteArrays.add(("\"" + entry.getKey() + "\"; filename=\""
                        + path.getFileName() + "\"\r\nContent-Type: " + mimeType
                        + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                byteArrays.add(Files.readAllBytes(path));
                byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
            } else {
                byteArrays.add(
                        ("\"" + entry.getKey() + "\"\r\n\r\n" + entry.getValue()
                                + "\r\n").getBytes(StandardCharsets.UTF_8));
            }
        }
        byteArrays
                .add(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));
        return BodyPublishers.ofByteArrays(byteArrays);
    }

    @Symbol("greet")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public FormValidation doCheckName(@QueryParameter String value, @QueryParameter boolean useFrench)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error(Messages.HelloWorldBuilder_DescriptorImpl_errors_missingName());
            if (value.length() < 4)
                return FormValidation.warning(Messages.HelloWorldBuilder_DescriptorImpl_warnings_tooShort());
            if (!useFrench && value.matches(".*[éáàç].*")) {
                return FormValidation.warning(Messages.HelloWorldBuilder_DescriptorImpl_warnings_reallyFrench());
            }
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.HelloWorldBuilder_DescriptorImpl_DisplayName();
        }

    }

}
