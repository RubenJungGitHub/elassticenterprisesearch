package contain.opensource.enterprisesearch.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

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

    @JsonProperty("listItem")
    private ListItem listItem; // <-- Toegevoegd

    // Inner class voor listItem en fields
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListItem {
        @JsonProperty("fields")
        private FieldValues fields;

        public FieldValues getFields() { return fields; }
        public void setFields(FieldValues fields) { this.fields = fields; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FieldValues {
        // Gebruik een map zodat alle custom kolommen (zoals classificationexternaltax) direct beschikbaar zijn
        private Map<String, Object> additionalData;

        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void set(String key, Object value) {
            if (this.additionalData == null) {
                this.additionalData = new java.util.HashMap<>();
            }
            this.additionalData.put(key, value);
        }

        public Map<String, Object> getAdditionalData() {
            return additionalData;
        }
    }

    // Getters en Setters voor de rest
    public String getCreatedDateTime() { return createdDateTime; }
    public void setCreatedDateTime(String createdDateTime) { this.createdDateTime = createdDateTime; }

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

    public ListItem getListItem() { return listItem; }
    public void setListItem(ListItem listItem) { this.listItem = listItem; }
}