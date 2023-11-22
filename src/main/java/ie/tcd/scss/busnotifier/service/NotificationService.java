package ie.tcd.scss.busnotifier.service;

import ie.tcd.scss.busnotifier.domain.BrowserEndpoint;
import ie.tcd.scss.busnotifier.domain.DublinBusSubscription;
import ie.tcd.scss.busnotifier.domain.DublinBusSubscriptionActiveTimeRange;
import ie.tcd.scss.busnotifier.domain.User;
import ie.tcd.scss.busnotifier.repo.BrowserEndpointRepo;
import ie.tcd.scss.busnotifier.repo.DublinBusSubscriptionActiveTimeRangeRepo;
import ie.tcd.scss.busnotifier.repo.DublinBusSubscriptionRepo;
import ie.tcd.scss.busnotifier.schema.DublinBusSubscriptionActiveTimeRangeDTO;
import ie.tcd.scss.busnotifier.schema.DeleteDublinBusSubscriptionsDTO;
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
import java.security.GeneralSecurityException;
import java.security.Security;
import java.time.DayOfWeek;
import java.util.Calendar;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

@Service
public class NotificationService {

    private final Logger logger = LoggerFactory.getLogger(NotificationService.class);

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

    private PushService pushService;

    @PostConstruct
    private void postConstruct() throws GeneralSecurityException {
        Security.addProvider(new BouncyCastleProvider());
        pushService = new PushService(publicKey, privateKey);
    }

    // Send a notification to users every second
    @Scheduled(fixedRate = 10_000)
    private void push() {
        for (var subscription : dublinBusSubscriptionRepo.findAll()) {
            var now = Calendar.getInstance();
            final var day = DayOfWeek.of((now.get(Calendar.DAY_OF_WEEK) + 6) % 7);
            final var hour = now.get(Calendar.HOUR_OF_DAY);
            final var minute = now.get(Calendar.MINUTE);
            if (dublinBusSubscriptionActiveTimeRangeRepo
                    .findByDublinBusSubscription(subscription)
                    .stream()
                    .anyMatch(r -> r.contains(day, hour, minute))) {
                var message = String.format(
                        "Bus stop `%s` for user `%s` is active (now=%s, %02d:%02d)",
                        subscription.getBusStopId(),
                        subscription.getUser().getUsername(),
                        day.toString(),
                        hour,
                        minute
                );
                logger.info(message);
                for (var browserEndpoint : browserEndpointRepo.findByDublinBusSubscriptions(subscription)){
                    message = String.format(
                            "Sending to `%s` (`%s`#`%s`)",
                            browserEndpoint.getEndpoint(),
                            browserEndpoint.getUser().getUsername(),
                            subscription.getBusStopId()
                    );
                    logger.info(message);
                    try {
                        var notification = new Notification(
                                browserEndpoint.getEndpoint(),
                                browserEndpoint.getUserPublicKey(),
                                browserEndpoint.getUserAuth(),
                                "\"TODO: USE DUBLIN BUS API LOGIC\""
                        );
                        pushService.send(notification);
                    } catch (GeneralSecurityException | IOException | JoseException | ExecutionException |
                         InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            } else {
                var message = String.format(
                        "Bus stop `%s` for user `%s` is not active (now=%s, %02d:%02d)",
                        subscription.getBusStopId(),
                        subscription.getUser().getUsername(),
                        day.toString(),
                        hour,
                        minute
                );
                logger.info(message);
            }
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

    /**
     * @return The VAPID public key for use in the browser
     */
    public String getPublicKey() {
        return publicKey;
    }

    public List<BrowserEndpoint> getBrowserEndpointsForUser(User user) {
        return browserEndpointRepo.findByUser(user);
    }

    public void addDublinBusSubscription(User user, String endpoint, String busStopId) {
        var browserEndpoint = browserEndpointRepo.findByUserAndEndpoint(user, endpoint).orElseThrow();
        if (!dublinBusSubscriptionRepo.existsByUserAndBusStopId(user, busStopId)) {
            var dublinBusSubscription = DublinBusSubscription.builder()
                    .user(user)
                    .busStopId(busStopId)
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

    public void deleteDublinBusSubscriptionActiveTimeRange(User user, String busStopId, DublinBusSubscriptionActiveTimeRangeDTO request) {
        var validatedTimeRange = request.validate();
        dublinBusSubscriptionRepo.findByUserAndBusStopId(user, busStopId).ifPresent(sub -> {
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
    public void addDublinBusSubscriptionActiveTimeRange(User user, String busStopId, DublinBusSubscriptionActiveTimeRangeDTO request) {
        var transientTimeRange = request.validate();
        var busStop = dublinBusSubscriptionRepo.findByUserAndBusStopId(user, busStopId);
        var dublinBusSubscription = busStop.orElseGet(() -> dublinBusSubscriptionRepo.save(DublinBusSubscription
                .builder()
                .user(user)
                .busStopId(busStopId)
                .build()));
        transientTimeRange.dublinBusSubscription = dublinBusSubscription;
        var savedTimeRange = dublinBusSubscriptionActiveTimeRangeRepo.save(transientTimeRange);
        dublinBusSubscription.getActiveTimeRanges().add(savedTimeRange);
        dublinBusSubscriptionRepo.save(dublinBusSubscription);
    }

    public List<DublinBusSubscriptionActiveTimeRange> getDublinBusActiveTimeRanges(User user, String busStopId) throws NoSuchElementException {
        return dublinBusSubscriptionRepo
                .findByUserAndBusStopId(user, busStopId)
                .map(DublinBusSubscription::getActiveTimeRanges)
                .orElseThrow();
    }
}
