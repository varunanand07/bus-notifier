package ie.tcd.scss.busnotifier.schema;

import java.util.List;

public class DeleteDublinBusSubscriptionsDTO {
    public static class DublinBusSubscriptionDTO {
        public String endpoint;
        public String busStopIdentifier;
    }
    public List<DublinBusSubscriptionDTO> endpoints;
}
