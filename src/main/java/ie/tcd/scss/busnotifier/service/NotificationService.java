package ie.tcd.scss.busnotifier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ie.tcd.scss.busnotifier.domain.BrowserEndpoint;
import ie.tcd.scss.busnotifier.domain.DublinBusSubscription;
import ie.tcd.scss.busnotifier.domain.DublinBusSubscriptionActiveTimeRange;
import ie.tcd.scss.busnotifier.domain.User;
import ie.tcd.scss.busnotifier.repo.BrowserEndpointRepo;
import ie.tcd.scss.busnotifier.repo.DublinBusSubscriptionActiveTimeRangeRepo;
import ie.tcd.scss.busnotifier.repo.DublinBusSubscriptionRepo;
import ie.tcd.scss.busnotifier.schema.DublinBusSubscriptionActiveTimeRangeDTO;
import ie.tcd.scss.busnotifier.schema.DeleteDublinBusSubscriptionsDTO;
import ie.tcd.scss.busnotifier.schema.GeneralBusStopUpdateDTO;
import ie.tcd.scss.busnotifier.schema.NotificationDTO;
import jakarta.annotation.PostConstruct;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class NotificationService {

    private final static DateTimeFormatter JS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    /**
     * Notification rate limiter.
     */
    private static final long NOTIFICATION_SPACING = 5;
    private final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final static ObjectMapper objectMapper = new ObjectMapper();

    @Value("${vapid.public.key}")
    private String publicKey;
    @Value("${vapid.private.key}")
    private String privateKey;

    @Autowired
    private BrowserEndpointRepo browserEndpointRepo;

    @Autowired
    private DublinBusSubscriptionRepo dublinBusSubscriptionRepo;

    @Autowired
    private DublinBusSubscriptionActiveTimeRangeRepo dublinBusSubscriptionActiveTimeRangeRepo;

    @Autowired
    private GtfsService gtfsService;
    private PushService pushService;

    private HashMap<String, Integer> tripIdToNotificationsGiven;
    private Map<GtfsService.StopAndRoute, List<GtfsService.TripIdAndStopTime>> mostRecentUpdate;

    @PostConstruct
    private void postConstruct() throws GeneralSecurityException {
        Security.addProvider(new BouncyCastleProvider());
        pushService = new PushService(publicKey, privateKey);
        // furryfox is in violation of the web push and requries
        // a subject parameter otherwise it returns error 109
        pushService.setSubject("mailto:uzoukwuc@tcd.ie");
        tripIdToNotificationsGiven = new HashMap<>();
    }

    private boolean isActive(DublinBusSubscription subscription) {
        var now = LocalDateTime.now();
        return  dublinBusSubscriptionActiveTimeRangeRepo
                .findByDublinBusSubscription(subscription)
                .stream()
                .anyMatch(r -> r.contains(now.getDayOfWeek(), now.getHour(), now.getMinute()));
    }

    // Send a notification to users every second
    @Scheduled(fixedRate = 30_000)
    private void push() {
        final var now = LocalDateTime.now();
        final var day = now.getDayOfWeek();
        final var hour = now.getHour();
        final var minute = now.getMinute();
        var query = new HashSet<GtfsService.StopAndRoute>();
        var activeSubscriptions = new HashMap<GtfsService.StopAndRoute, List<DublinBusSubscription>>();

        // First of all update the routes that we are currently tracking
        for (var subscription : dublinBusSubscriptionRepo.findAll()) {
            final var active = isActive(subscription);
            logger.info(
                    "Bus `{}` at bus stop `{}` for user `{}` is {} (now={}, {}:{})",
                    subscription.getBusStopId(),
                    subscription.getBusId(),
                    subscription.getUser().getUsername(),
                    active ? "active" : "NOT ACTIVE!",
                    day, hour, minute
            );

            var stopAndRoute = new GtfsService.StopAndRoute(subscription.getBusStopId(), subscription.getBusId());
            query.add(stopAndRoute);
            if (active) {
                if (!activeSubscriptions.containsKey(stopAndRoute))
                    activeSubscriptions.put(stopAndRoute, new ArrayList<>());
                activeSubscriptions.get(stopAndRoute).add(subscription);
            }
        }

        // Then send push notifications
        try {
            // This is safe to use even when stop times have not been fully digested by postgres - this array will
            // just be empty in that case. This may be confusing at first as for the first ~60 seconds the output
            // of this program and the output of the TFI live app will differ severely.
            var updates = gtfsService.fetchTripUpdates(query.stream().toList());
            this.mostRecentUpdate = updates;
            for (var update : updates.entrySet()) {
                var stopAndRoute = update.getKey();
                var trips = update.getValue();
                trips.sort(Comparator.comparing(GtfsService.TripIdAndStopTime::stopTime));
                trips.removeIf(f -> f.stopTime().isBefore(now));
                // consider the closest trip
                if (trips.isEmpty()) continue;
                var closestTrip = trips.get(0);
                var minutesToArrival = now.until(closestTrip.stopTime(), ChronoUnit.MINUTES);
                var minutesToArrivalAtLastNotification = this.tripIdToNotificationsGiven.getOrDefault(closestTrip.tripId(), Integer.MAX_VALUE);
                var shouldSendPushNotification = minutesToArrival < minutesToArrivalAtLastNotification
                        && minutesToArrivalAtLastNotification - minutesToArrival > NOTIFICATION_SPACING;
                if (!shouldSendPushNotification) continue;
                for (var subscription : activeSubscriptions.get(stopAndRoute)) {
                    var user = subscription.getUser();
                    for (var browserEndpoint : browserEndpointRepo.findByDublinBusSubscriptions(subscription)) {
                        // We use five minute time intervals
                        logger.info(
                                "Sending to `{}` for user `{}` (bus=`{}`, stop=`{}`)",
                                browserEndpoint.getEndpoint(),
                                browserEndpoint.getUser().getUsername(),
                                stopAndRoute.route(),
                                stopAndRoute.stop()
                        );
                        try {
                            var notification = new Notification(
                                    browserEndpoint.getEndpoint(),
                                    browserEndpoint.getUserPublicKey(),
                                    browserEndpoint.getUserAuth(),
                                    objectMapper.writeValueAsBytes(
                                            trips.stream()
                                                    .map(trip -> NotificationDTO.builder()
                                                            .stop(stopAndRoute.stop())
                                                            .route(stopAndRoute.route())
                                                            .eta(trip.stopTime().format(JS))
                                                            .realTime(trip.realTime())
                                                    .build())
                                                    .toList()
                                    )
                            );
                            var response = pushService.send(notification);
                            if (response.getStatusLine().getStatusCode() >= 300) {
                                logger.warn("Failed to send push notification! {}", response.getStatusLine());
                            }
                        } catch (GeneralSecurityException | IOException | JoseException | ExecutionException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                tripIdToNotificationsGiven.put(closestTrip.tripId(), (int) minutesToArrival);
            }
        } catch (IOException | InterruptedException | NoSuchFieldException | IllegalAccessException e ) {
            e.printStackTrace();
            logger.error("Bruh");
        }
    }

    /**
     * @param user The user who is adding the subscription
     * @param subscription The browser-generated subscription data structure (not used internally)
     */
     public void addBrowserEndpoint(User user, Subscription subscription) {
        var browserEndpoint = new BrowserEndpoint();
        browserEndpoint.endpoint = subscription.endpoint;
        browserEndpoint.userAuth = subscription.keys.auth;
        browserEndpoint.userPublicKey = subscription.keys.p256dh;
        browserEndpoint.user = user;
        browserEndpointRepo.save(browserEndpoint);
    }

    /**
     * Remove a browser endpoint entity from the database
     * @param endpoint The push vendor URL generated by the browser
     * @return true if an element was deleted, false otherwise
     */
    public boolean deleteBrowserEndpoint(User user, String endpoint) {
        // This is not thread safe (time of check vs. time of use problem) and requires
        // two database queries when it should only take one.
        if (browserEndpointRepo.existsByUserAndEndpoint(user, endpoint)) {
            browserEndpointRepo.deleteByUserAndEndpoint(user, endpoint);
            return true;
        } else {
            return false;
        }
    }

    public List<GeneralBusStopUpdateDTO> getMostRecentTripUpdate() {
        var subs = dublinBusSubscriptionRepo.findAll().stream().map( sub -> new GtfsService.StopAndRoute(sub.getBusStopId(), sub.getBusId())).toList();
        Map<GtfsService.StopAndRoute, List<GtfsService.TripIdAndStopTime>> update = null;
        try {
            update = gtfsService.fetchTripUpdates(GtfsService.FeedSource.STALE, subs);
        } catch (IOException | IllegalAccessException | NoSuchFieldException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return update
                    .entrySet()
                    .stream()
                    .flatMap(stopAndCode -> stopAndCode
                            .getValue()
                            .stream()
                            .map(tripIdAndStopTime -> GeneralBusStopUpdateDTO.builder()
                                .trip(tripIdAndStopTime.tripId())
                                .eta(tripIdAndStopTime.stopTime().format(JS))
                                .route(stopAndCode.getKey().route())
                                .stopName(gtfsService.getNameForStopCode(stopAndCode.getKey().stop()))
                                .realTime(tripIdAndStopTime.realTime())
                                .stop(stopAndCode.getKey().stop())
                                .build()
                            )
                    )
                    .toList();
    }

    /**
     * @return The VAPID public key for use in the browser
     */
    public String getPublicKey() {
        return publicKey;
    }

    public List<BrowserEndpoint> getBrowserEndpointsForUser(User user) {
        return browserEndpointRepo.findByUser(user);
    }

    public void addDublinBusSubscription(User user, String endpoint, String busStopId, String busId) {
        var browserEndpoint = browserEndpointRepo.findByUserAndEndpoint(user, endpoint).orElseThrow();
        if (!dublinBusSubscriptionRepo.existsByUserAndBusStopIdAndBusId(user, busStopId, busId)) {
            var dublinBusSubscription = DublinBusSubscription.builder()
                    .user(user)
                    .busStopId(busStopId)
                    .busId(busId)
                    .browserEndpoints(List.of(browserEndpoint))
                    .activeTimeRanges(List.of())
                    .build();
            dublinBusSubscriptionRepo.save(dublinBusSubscription);
        }
    }

    public List<DublinBusSubscription> getDublinBusSubscriptions(User user) {
        return dublinBusSubscriptionRepo.findByUser(user);
    }

    public void deleteDublinBusSubscriptions(User user, List<DeleteDublinBusSubscriptionsDTO.DublinBusSubscriptionDTO> toDelete) {
        for (var deletionCandidate : toDelete) {
            var busStopIdentifier = deletionCandidate.busStopIdentifier;
            browserEndpointRepo
                    .findByUserAndEndpoint(user, deletionCandidate.endpoint)
                    .ifPresent(endpoint -> {
                        var changed = false;
                        for (int i = endpoint.dublinBusSubscriptions.size() - 1; i >= 0; i--) {
                            if (endpoint.dublinBusSubscriptions.get(i).getBusStopId().equals(busStopIdentifier)) {
                                endpoint.dublinBusSubscriptions.remove(i);
                                changed = true;
                            }
                        }
                        if (changed) {
                            browserEndpointRepo.save(endpoint);
                        }
                    });
        }
    }

    public void deleteDublinBusSubscriptionActiveTimeRange(User user, String busStopId, String busId, DublinBusSubscriptionActiveTimeRangeDTO request) {
        var validatedTimeRange = request.validate();
        dublinBusSubscriptionRepo.findByUserAndBusStopIdAndBusId(user, busStopId, busId).ifPresent(sub -> {
            var changed = false;
            for (int i = sub.activeTimeRanges.size() - 1; i >= 0; i--) {
                if (sub.activeTimeRanges.get(i).overlaps(validatedTimeRange)) {
                    sub.activeTimeRanges.remove(i);
                    changed = true;
                }
            }
            if (changed)
                dublinBusSubscriptionRepo.save(sub);
        });
    }
    public void addDublinBusSubscriptionActiveTimeRange(User user, String busStopId, String busId, DublinBusSubscriptionActiveTimeRangeDTO request) {
        var transientTimeRange = request.validate();
        var busStop = dublinBusSubscriptionRepo.findByUserAndBusStopIdAndBusId(user, busStopId, busId);
        var dublinBusSubscription = busStop.orElseGet(() -> dublinBusSubscriptionRepo.save(DublinBusSubscription
                .builder()
                .user(user)
                .busStopId(busStopId)
                .busId(busId)
                .build()));
        transientTimeRange.dublinBusSubscription = dublinBusSubscription;
        var savedTimeRange = dublinBusSubscriptionActiveTimeRangeRepo.save(transientTimeRange);
        dublinBusSubscription.getActiveTimeRanges().add(savedTimeRange);
        dublinBusSubscriptionRepo.save(dublinBusSubscription);
    }

    public List<DublinBusSubscriptionActiveTimeRange> getDublinBusActiveTimeRanges(
            User user,
            String busStopId,
            String busId
    ) throws NoSuchElementException {
        return dublinBusSubscriptionRepo
                .findByUserAndBusStopIdAndBusId(user, busStopId, busId)
                .map(DublinBusSubscription::getActiveTimeRanges)
                .orElseThrow();
    }
}
