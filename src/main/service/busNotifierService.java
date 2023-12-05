package ie.tcd.scss.busnotifier.service;

import com.google.transit.realtime.GtfsRealtime.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.net.URL;
import java.net.URI;
import java.io.*;
import java.util.zip.*;
import java.nio.file.*;

@Service
public class busnotifierService {

    private final String gtfsRealtimeUrl = "https://www.transportforireland.ie/transitData/Data/GTFS_Dublin_Bus.zip";
    private final String dataDirectory = 'data/';

    public TripUpdate getTimetableForStop(String stopId) {

        try {
            File localDataFile = new File(dataDirectory)
            if (!localDataFile.exists()) {
                Files.createDirectories(localDataFile)
                downloadZipFile();
            }
            FeedMessage feed = FeedMessage.parseFrom(
                new FileInputStream(localDataFile)
            );
            for (FeedEntity entity : feed.getEntityList()) {
                if (entity.hasTripUpdate()) {
                    return entity.getTripUpdate();
                }
            }
        } catch (Exception e) {
            System.out.println(e.getStackTrace());
            return null;
        }

    }

    private void downloadZipFile() {

        URL url = new URL(gtfsRealtimeUrl);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod('GET');

        InputStream in = connection.getInputStream();
        ZipInputStream zipIn = new ZipInputStream(in);

        ZipEntry entry = zipIn.getNextEntry();
        String entryName = entry.getName();

        while (entry != null) {
            if (!entry.isDirectory()) {
                FileOutputStream fos = new FileOutputStream(dataDirectory + entryName);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
            String entryName = entry.getName();
        }

    }

}