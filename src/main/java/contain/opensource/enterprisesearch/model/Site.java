package contain.opensource.enterprisesearch.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Site {
    private String id;
    private String name;
    
    @JsonProperty("webUrl")
    private String webUrl; // <-- Toegevoegd

    public String getId() { 
        return id; 
    }
    public void setId(String id) { 
        this.id = id; 
    }

    public String getName() { 
        return name; 
    }
    public void setName(String name) { 
        this.name = name; 
    }

    public String getWebUrl() { 
        return webUrl; 
    }
    public void setWebUrl(String webUrl) { 
        this.webUrl = webUrl; 
    }
}