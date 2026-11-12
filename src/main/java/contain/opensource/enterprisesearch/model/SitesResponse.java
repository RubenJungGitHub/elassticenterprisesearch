package contain.opensource.enterprisesearch.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SitesResponse {
    private List<Site> value;

    public List<Site> getValue() {
        return value;
    }

    public void setValue(List<Site> value) {
        this.value = value;
    }
}