package ie.tcd.scss.busnotifier.schema;

import ie.tcd.scss.busnotifier.domain.DublinBusSubscription;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class DublinBusSubscriptionDTO {

    public String endpoint;
    public String busStopId;
    public DublinBusSubscriptionDTO(DublinBusSubscription dublinBusSubscription) {
        this.endpoint = dublinBusSubscription.getBrowserEndpoint().endpoint;
        this.busStopId = dublinBusSubscription.getBusStopId();
    }
}
