package ie.tcd.scss.busnotifier.services;

import com.google.transit.realtime.GtfsRealtime.*;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.net.*;
import java.io.*;
import java.util.zip.*;
import java.nio.file.*;

@Service
public class busNotifierService {

    private final String gtfsRealtimeUrl = "https://www.transportforireland.ie/transitData/Data/GTFS_Dublin_Bus.zip";
    private final String dataDirectory = "data/";

    public TripUpdate getTimetableForStop(String stopId) {

        try {
            File localDataFile = new File(dataDirectory);
            if (!localDataFile.exists()) {
                Files.createDirectories(Paths.get(dataDirectory));
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
        }
        return null;
    }

    private void downloadZipFile() throws IOException {

        byte[] buffer = new byte[1024];

        URL url = new URL(gtfsRealtimeUrl);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        InputStream in = connection.getInputStream();
        ZipInputStream zipIn = new ZipInputStream(in);

        ZipEntry entry = zipIn.getNextEntry();
        String entryName = "";
        if (entry != null)
            entryName = entry.getName();

        while (entry != null) {
            if (!entry.isDirectory()) {
                FileOutputStream fos = new FileOutputStream(dataDirectory + entryName);
                int len;
                while ((len = zipIn.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
            entryName = entry.getName();
        }

    }

}