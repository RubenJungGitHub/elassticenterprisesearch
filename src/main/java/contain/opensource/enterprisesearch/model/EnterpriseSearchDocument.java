package contain.opensource.enterprisesearch.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class EnterpriseSearchDocument {

    private String id;
    private String title;
    private String content;
    private String classification; // <-- Toegevoegd

    @JsonProperty("source_system")
    private String sourceSystem = "Alfresco";

    @JsonProperty("file_type")
    private String fileType;

    private String url;

    @JsonProperty("created_date")
    private String createdDate;

    @JsonProperty("access_control")
    private List<String> accessControl;

    // Constructors
    public EnterpriseSearchDocument() {}

    public EnterpriseSearchDocument(String id, String title, String content, String classification, String fileType, String url, String createdDate, List<String> accessControl) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.classification = classification; // <-- Toegevoegd
        this.sourceSystem = "Alfresco";
        this.fileType = fileType;
        this.url = url;
        this.createdDate = createdDate;
        this.accessControl = accessControl;
    }

    // Getters en Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getClassification() { return classification; } // <-- Toegevoegd
    public void setClassification(String classification) { this.classification = classification; } // <-- Toegevoegd

    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getCreatedDate() { return createdDate; }
    public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }

    public List<String> getAccessControl() { return accessControl; }
    public void setAccessControl(List<String> accessControl) { this.accessControl = accessControl; }
}