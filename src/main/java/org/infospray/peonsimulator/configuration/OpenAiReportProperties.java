package org.infospray.peonsimulator.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "simulator.reports")
public class OpenAiReportProperties {
    private String apiKey = "";
    private String model = "gpt-6.1-sol";
    private String endpoint = "https://api.openai.com/v1/responses";
    private String directory = "./reports";

    public String getApiKey() { return this.apiKey; }
    public void setApiKey(String value) { this.apiKey = value; }
    public String getModel() { return this.model; }
    public void setModel(String value) { this.model = value; }
    public String getEndpoint() { return this.endpoint; }
    public void setEndpoint(String value) { this.endpoint = value; }
    public String getDirectory() { return this.directory; }
    public void setDirectory(String value) { this.directory = value; }
}
