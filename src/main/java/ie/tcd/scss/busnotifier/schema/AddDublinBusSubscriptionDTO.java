package ie.tcd.scss.busnotifier.schema;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class AddDublinBusSubscriptionDTO {
    public String endpoint;
    public String busStopId;
}
