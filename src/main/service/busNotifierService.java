package ie.tcd.scss.busnotifier.service;

import com.google.transit.realtime.GtfsRealtime.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.net.URL;
import java.net.URI;

@Service
public class busnotifierService {

    private final String gtfsRealtimeUrl = "https://www.transportforireland.ie/transitData/Data/GTFS_Dublin_Bus.zip";

    public void getTimetableForStop(String stopId) {
        try {
            URL url = new URL(gtfsRealtimeUrl);
            FeedMessage feed = FeedMessage.parseFrom(url.openStream());
            for (FeedEntity entity : feed.getEntityList()) {
                if (entity.hasTripUpdate()) {
                    System.out.println(entity.getTripUpdate());
                }
            }
        } catch (Exception e) {
            System.out.println(e.getStackTrace());
        }

    }
}