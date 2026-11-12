package contain.opensource.enterprisesearch.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DriveItem {
    private String id;
    private String name;
    private Object file;
    private Object folder;
    
    @JsonProperty("webUrl")
    private String webUrl;


    @JsonProperty("createdDateTime")
    private String createdDateTime;

    // Getters en Setters
    public String getCreatedDateTime() { 
        return createdDateTime; 
    }
    public void setCreatedDateTime(String createdDateTime) { 
        this.createdDateTime = createdDateTime;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Object getFile() { return file; }
    public void setFile(Object file) { this.file = file; }
    public Object getFolder() { return folder; }
    public void setFolder(Object folder) { this.folder = folder; }
    public String getWebUrl() { return webUrl; }
    public void setWebUrl(String webUrl) { this.webUrl = webUrl; }
}