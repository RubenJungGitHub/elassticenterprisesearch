package contain.opensource.enterprisesearch.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DrivesResponse {
    private List<Drive> value;

    public List<Drive> getValue() {
        return value;
    }

    public void setValue(List<Drive> value) {
        this.value = value;
    }
}