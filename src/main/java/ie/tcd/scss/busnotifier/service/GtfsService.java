package ie.tcd.scss.busnotifier.service;

import com.google.transit.realtime.GtfsRealtime;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvToBeanBuilder;
import ie.tcd.scss.busnotifier.repo.StopTimesRepo;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class GtfsService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${gtfs.dir}")
    private String gtfsDir;

    @Value("${nta.api.key}")
    private String ntaApiKey;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private StopTimesRepo stopTimesRepo;
    private GtfsRealtime.FeedMessage staleFeed;

    public static class StopRow {
        @CsvBindByName(column = "stop_id")  String id;
        @CsvBindByName(column = "stop_code")  String code;
        @CsvBindByName(column = "stop_name")  String name;
    }

    public static class RouteRow {
        @CsvBindByName(column = "route_id")  String id;
        @CsvBindByName(column = "route_short_name")  String shortName;
    }

    public static class TripRow {
        @CsvBindByName(column = "trip_id")  String id;
        @CsvBindByName(column = "trip_headsign")  String tripHeadSign;
        @CsvBindByName(column = "service_id")  String serviceId;

        @CsvBindByName(column = "route_id") String routeId;
    }

    public static class CalendarRow {
        @CsvBindByName(column = "service_id")  String id;
        @CsvBindByName  String monday;
        @CsvBindByName  String tuesday;
        @CsvBindByName  String thursday;
        @CsvBindByName  String friday;
        @CsvBindByName  String saturday;
        @CsvBindByName  String sunday;
        @CsvBindByName(column = "start_date")  String startDate;
        @CsvBindByName(column = "end_date")    String endDate;
    }

    HashSet<String> activeServicesSet;

    List<StopRow> stops;
    Map<String, StopRow> stopsByCode;
    ConcurrentHashMap<String, StopRow> stopsById;

    List<RouteRow> routes;
    Map<String, RouteRow> routesByName;
    Map<String, RouteRow> routesById;

    List<TripRow> trips;
    Map<String, TripRow> tripsById;
    Map<String, Integer> tripsByIdToIndex;
    @Autowired
    public JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() throws IOException {
        loadStaticData();
        logger.info("Going to start loading stop times");
        new Thread(() -> {
            logger.info("Loading stop times into postgres in worker thread");
            jdbcTemplate.execute("copy stop_times from '/transposed/stop_times.txt' with (header match, format csv)");
            logger.info("Finished loading stop times into postgres");
        })
                .start();
        logger.info("GTFS service partially initialised");
    }
    public void loadStaticData() throws IOException {
        var stopPath = Path.of(gtfsDir, "stops.txt").toString();
        var routePath = Path.of(gtfsDir, "routes.txt").toString();
        var tripPath = Path.of(gtfsDir, "trips.txt").toString();
        var calendarPath = Path.of(gtfsDir, "calendar.txt").toString();

        var now = LocalDate.now().atTime(12, 0);
        var queryDay = now.getDayOfWeek();
        var queryDayBefore = now.minusDays(1).getDayOfWeek();

        List<CalendarRow> calendar;
        try (var calendarFile = new FileReader(calendarPath)) {
            calendar = new CsvToBeanBuilder<CalendarRow>(calendarFile)
                    .withType(CalendarRow.class)
                    .build()
                    .parse();
        }

        activeServicesSet = new HashSet<>();

        for (var service : calendar) {
            var start = LocalDate
                    .parse(service.startDate, DateTimeFormatter.BASIC_ISO_DATE)
                    .atTime(0, 0);
            var end = LocalDate
                    .parse(service.endDate, DateTimeFormatter.BASIC_ISO_DATE)
                    .atTime(23, 59);
            assert start.isBefore(end);

            String activeToday, activeYesterday;
            try {
                activeToday = (String) service.getClass()
                        .getDeclaredField(String.valueOf(queryDay).toLowerCase())
                        .get(service);
                assert activeToday.equals("1") || activeToday.equals("0");
                activeYesterday = (String) service.getClass()
                    .getDeclaredField(String.valueOf(queryDayBefore).toLowerCase())
                    .get(service);
                assert activeYesterday.equals("1") || activeYesterday.equals("0");
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }

            if ((activeToday.equals("1") || activeYesterday.equals("1"))
                    && now.isBefore(end)
                    && now.isAfter(start))
            {
                logger.info("Service " + service.id + " is active");
                activeServicesSet.add(service.id);
            } else {
                // activeServicesSet.add(service.id);
            }
        }

        logger.info("Determined active services");

        try (var stopFile = new FileReader(stopPath)) {
            this.stops = new CsvToBeanBuilder<StopRow>(stopFile)
                    .withType(StopRow.class)
                    .build()
                    .parse();
        }
        this.stopsByCode = stops
                .stream()
                .collect(Collectors.toMap(s -> s.code, s -> s, (a, b) -> a));
        this.stopsById = new ConcurrentHashMap<>(stops.stream().collect(Collectors.toMap(s -> s.id, s -> s)));

        try (var routeFile = new FileReader(routePath)) {
            this.routes = new CsvToBeanBuilder<RouteRow>(routeFile)
                    .withType(RouteRow.class)
                    .build()
                    .parse();
        }
        this.routesByName = routes
                .stream()
                .collect(Collectors.toMap(s -> s.shortName, s -> s, (a, b) -> a));
        this.routesById = routes
                .stream()
                .collect(Collectors.toMap(s -> s.id, s -> s));

        try (var tripFile = new FileReader(tripPath)) {
            this.trips = new CsvToBeanBuilder<TripRow>(tripFile)
                    .withType(TripRow.class)
                    .build()
                    .stream()
                    .filter(trip -> {
                        if (activeServicesSet.contains(trip.serviceId))
                            return true;
                        else {
                            // logger.info("Filtering out " + trip.serviceId + " for lack of service");
                            return false;
                        }
                    })
                    .toList();
        }
        this.tripsById = trips
                .stream()
                .collect(Collectors.toMap(s -> s.id, s -> s, (a, b) -> a));
        this.tripsByIdToIndex = IntStream
                .range(0, trips.size())
                .boxed()
                .collect(Collectors.toMap(i ->  trips.get(i).id, i -> i));
    }

    public record StopAndRoute(String stop, String route) {}

    /**
     * @param tripId see GTFS-R specification
     * @param stopTime the estimated time of the stop
     * @param realTime indicates whether the stopTime has real-time GTFS-R delay taken into account.
     *                 this is most often false when the webserver is being rate limited or blocked.
     */
    public record TripIdAndStopTime(String tripId, LocalDateTime stopTime, boolean realTime) {}

    public String getNameForStopCode(String code) {
        var stop = stopsByCode.get(code);
        return stop == null ? null : stop.name;
    }

    public enum FeedSource {
        NETWORK,
        STALE,
        NONE
    };

    public Map<StopAndRoute, List<TripIdAndStopTime>> fetchTripUpdates(List<StopAndRoute> queryStopsAndRoutes) throws IOException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        return fetchTripUpdates(FeedSource.NETWORK, queryStopsAndRoutes);
    }

    public Map<StopAndRoute, List<TripIdAndStopTime>> fetchTripUpdates(FeedSource feedSource, List<StopAndRoute> queryStopsAndRoutes) throws IOException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        return switch (feedSource) {
            case NETWORK -> {
                try {
                    var headers = new HttpHeaders();
                    headers.set("x-api-key", ntaApiKey);
                    var response = restTemplate.exchange(
                            "https://api.nationaltransport.ie/gtfsr/v2/gtfsr",
                            HttpMethod.GET,
                            new HttpEntity<>(headers),
                            byte[].class
                    );
                    logger.info("Done geting protobuf files from the net");
                    this.staleFeed = GtfsRealtime.FeedMessage.parseFrom(response.getBody());
                    yield fetchTripUpdates(this.staleFeed, queryStopsAndRoutes);
                } catch (RestClientException e) {
                    yield  fetchTripUpdates(FeedSource.NONE, queryStopsAndRoutes);
                }
            }
            case STALE -> fetchTripUpdates(this.staleFeed, queryStopsAndRoutes);
            default -> fetchTripUpdates((GtfsRealtime.FeedMessage) null, queryStopsAndRoutes);
        };
    }

    public Map<StopAndRoute, List<TripIdAndStopTime>> fetchTripUpdates(GtfsRealtime.FeedMessage feed, List<StopAndRoute> queryStopsAndRoutes) throws IOException, InterruptedException, NoSuchFieldException, IllegalAccessException {

        var result = new HashMap<StopAndRoute, List<TripIdAndStopTime>>();
        if (feed != null) {

            for (var queryStopAndRoute : queryStopsAndRoutes) {
                var tripIdAndStopTimes = new ArrayList<TripIdAndStopTime>();
                var queryRouteName = queryStopAndRoute.route;
                var queryStopCode = queryStopAndRoute.stop;
                logger.info("Regarding bus stop: " + stopsByCode.get(queryStopCode).name);
                for (var entity : feed.getEntityList()) {
                    var trip = entity.getTripUpdate();
                    if (!trip.getTrip().getRouteId().equals(routesByName.get(queryRouteName).id)) {
                        continue;
                    }

                    // Step #1 (find index)
                    var stopId = stopsByCode.get(queryStopCode).id;
                    var stopInTrip = stopTimesRepo.findByTripIdAndStopId(trip.getTrip().getTripId(), stopId);
                    if (stopInTrip == null) {
                        logger.warn(
                                "Route was matched correctly, but this update is for another direction (Is {} → {}?)\n",
                                queryStopCode,
                                tripsById.get(trip.getTrip().getTripId()).tripHeadSign
                        );
                        continue;
                    }
                    var sequence = Integer.parseInt(stopInTrip.stopSequence);

                    // Step #2 (crete delay-bearing set)
                    var stopMap = trip.getStopTimeUpdateList()
                            .stream()
                            .collect(Collectors.toUnmodifiableMap(s -> s.getStopId(), s -> s));

                    var firstSequence = trip.getStopTimeUpdate(0).getStopSequence();

                    if (sequence < firstSequence) {
                        logger.warn("It's over (no update for this trip)");
                        continue;
                    }

                    // Step #3 (back track to find most recent delay)
                    for (int i = sequence; i >= 1; i--) {
                        var stop = stopTimesRepo.findByTripIdAndStopSequence(trip.getTrip().getTripId(), String.format("%d", i));
                        var stopRow = stopsById.get(stop.stopId);
                        if (stopMap.containsKey(stop.stopId) && stopMap.get(stop.stopId).hasDeparture()) {
                            var hour = Byte.parseByte(stop.departureTime.substring(0, 2));
                            var minute = Byte.parseByte(stop.departureTime.substring(3, 5));
                            var delay = stopMap.get(stop.stopId).getDeparture().getDelay();
                            logger.info(
                                    "assume delay of {} ({}) which is {} → {}:{} + {}s",
                                    stopRow.name, stopRow.code, delay,
                                    hour, minute, delay
                            );
                            var tripIdAndStopTime = new TripIdAndStopTime(
                                    trip.getTrip().getTripId(),
                                    gtfsrHourAndMinuteToTime(hour, minute),
                                    true
                            );
                            tripIdAndStopTimes.add(tripIdAndStopTime);
                            break;
                        } else {
                            // System.out.printf("%s (%s), ", stopRow.code, stopRow.name);
                        }
                    }
                    result.put(queryStopAndRoute, tripIdAndStopTimes);
                }
            }
        } else {
            for (var queryStopAndRoute : queryStopsAndRoutes) {
                var tripIdAndStopTimes = new ArrayList<TripIdAndStopTime>();
                var queryRouteName = queryStopAndRoute.route;
                var queryStopCode = queryStopAndRoute.stop;
                if (!stopsByCode.containsKey(queryStopCode)) {
                    logger.warn("Unrecognised stop code {} - Skipping", queryStopCode);
                    continue;
                }
                logger.info("Regarding bus stop: " + stopsByCode.get(queryStopCode).name);
                var tripsForRotue = trips
                        .stream()
                        .filter(t -> t.routeId.equals(routesByName.get(queryRouteName).id) && activeServicesSet.contains(t.serviceId))
                        .map(t -> t.id)
                        .collect(Collectors.toUnmodifiableSet());
                var stopId = stopsByCode.get(queryStopCode).id;
                var suitableStops = stopTimesRepo.findByTripIdInAndStopId(tripsForRotue, stopId);
                for (var stop : suitableStops) {
                    var now = LocalDateTime.now();
                    var hour = Byte.parseByte(stop.departureTime.substring(0, 2));
                    var minute = Byte.parseByte(stop.departureTime.substring(3, 5));
                    var time = gtfsrHourAndMinuteToTime(hour, minute);
                    if (time.isBefore(now) || time.isAfter(now.plusMinutes(90)) ) continue;
                    tripIdAndStopTimes.add(new TripIdAndStopTime(stop.tripId, time, false));
                }
                result.put(queryStopAndRoute, tripIdAndStopTimes);
            }
        }
        logger.info("[finished]");
        return result;
    }

    private LocalDateTime gtfsrHourAndMinuteToTime(byte hour, byte minute) {
        return hour >= 24
                ? LocalDate.now().plusDays(1).atTime(hour - 24, minute)
                : LocalDate.now().atTime(hour, minute);
    }
}
