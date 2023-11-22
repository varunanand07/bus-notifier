package ie.tcd.scss.busnotifier.schema;

import ie.tcd.scss.busnotifier.domain.BrowserEndpoint;
import ie.tcd.scss.busnotifier.domain.DublinBusSubscription;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
public class DublinBusSubscriptionDTO {

    public List<String> endpoints;
    public String busStopId;
    public DublinBusSubscriptionDTO(DublinBusSubscription dublinBusSubscription) {
        this.endpoints = dublinBusSubscription.getBrowserEndpoints().stream().map(BrowserEndpoint::getEndpoint).toList();
        this.busStopId = dublinBusSubscription.getBusStopId();
    }
}
