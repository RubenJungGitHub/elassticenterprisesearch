package contain.opensource.enterprisesearch.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DriveItemsResponse {
    private List<DriveItem> value;

    public List<DriveItem> getValue() {
        return value;
    }

    public void setValue(List<DriveItem> value) {
        this.value = value;
    }
}