package ie.tcd.scss.busnotifier.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class GeneralBusStopUpdateDTO {
    public String eta;
    public String stop;
    public String stopName;
    public boolean realTime;
    public String route;
    public String trip;
}
