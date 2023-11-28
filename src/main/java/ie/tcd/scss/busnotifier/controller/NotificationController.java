package ie.tcd.scss.busnotifier.controller;

import ie.tcd.scss.busnotifier.domain.User;
import ie.tcd.scss.busnotifier.schema.*;
import ie.tcd.scss.busnotifier.service.NotificationService;
import jakarta.validation.Valid;
import nl.martijndwars.webpush.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class NotificationController {
    @Autowired
    private NotificationService notificationService;

    @GetMapping("publicKey")
    public ResponseEntity<String> getPublicKey() {
        return ResponseEntity.ok(notificationService.getPublicKey());
    }

    @GetMapping("browserEndpoints")
    public ResponseEntity<Object> getBrowserEndpoints(
            @AuthenticationPrincipal User user
    ) {
        var browserEndpointDTOS = notificationService
                .getBrowserEndpointsForUser(user)
                .stream()
                .map(BrowserEndpointDTO::new)
                .toList();
        return ResponseEntity.ok(browserEndpointDTOS);
    }

    @PostMapping("browserEndpoints")
    public ResponseEntity<Object> subscribe(
            @AuthenticationPrincipal User user,
            @RequestBody Subscription subscription
    ) {
        notificationService.addBrowserEndpoint(user, subscription);
        return getBrowserEndpoints(user);
    }

    private record UnsubscribeBody(String endpoint) {}

    @DeleteMapping("browserEndpoints")
    public ResponseEntity<String> unsubscribe(
            @AuthenticationPrincipal User user,
            @RequestBody UnsubscribeBody unsubscribeBody
    ) {
        return notificationService.deleteBrowserEndpoint(user, unsubscribeBody.endpoint)
                ? ResponseEntity.ok("")
                : ResponseEntity.notFound().build();
    }

    @DeleteMapping("dublinBusSubscriptions")
    public ResponseEntity<Object> deleteDublinBusSubscriptions(
            @AuthenticationPrincipal User user,
            @RequestBody DeleteDublinBusSubscriptionsDTO request
    ) {
        notificationService.deleteDublinBusSubscriptions(user, request.endpoints);
        return getDublinBusSubscriptions(user);
    }

    @GetMapping("dublinBusSubscriptions")
    public ResponseEntity<Object> getDublinBusSubscriptions(@AuthenticationPrincipal User user) {
        var dublinBusSubscriptionDTOs = notificationService
                .getDublinBusSubscriptions(user)
                .stream()
                .map(DublinBusSubscriptionDTO::new)
                .toList();
        return ResponseEntity.ok(dublinBusSubscriptionDTOs);
    }

    @PostMapping("dublinBusSubscriptions")
    public ResponseEntity<Object> addDublinBusSubscriptions(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AddDublinBusSubscriptionDTO request
    ) {
        notificationService.addDublinBusSubscription(user, request.endpoint, request.busStopId);
        return getDublinBusSubscriptions(user);
    }

    @PostMapping("dublinBusSubscriptions/{busStopId}/activeTimeRanges")
    public ResponseEntity<Object> addDublinBusSubscriptionActiveTimeRange(
            @AuthenticationPrincipal User user,
            @PathVariable String busStopId,
            @RequestBody DublinBusSubscriptionActiveTimeRangeDTO request
    ) {
        notificationService.addDublinBusSubscriptionActiveTimeRange(user, busStopId, request);
        return getDublinBusSubscriptionActiveTimeRanges(user, busStopId);
    }

    @GetMapping("dublinBusSubscriptions/{busStopId}/activeTimeRanges")
    private ResponseEntity<Object> getDublinBusSubscriptionActiveTimeRanges(
            @AuthenticationPrincipal  User user,
            @PathVariable String busStopId
    ) {
        var dublinBusActiveTimeRangeDTOs = notificationService.getDublinBusActiveTimeRanges(user, busStopId)
                .stream()
                .map(DublinBusSubscriptionActiveTimeRangeDTO::new)
                .toList();
        return ResponseEntity.ok(dublinBusActiveTimeRangeDTOs);
    }

    @DeleteMapping("dublinBusSubscriptions/{busStopId}/activeTimeRanges")
    private ResponseEntity<Object> deleteDublinBusSubscriptionActiveTimeRange(
            @AuthenticationPrincipal  User user,
            @PathVariable String busStopId,
            @RequestBody DublinBusSubscriptionActiveTimeRangeDTO request
    ) {
        notificationService.deleteDublinBusSubscriptionActiveTimeRange(user, busStopId, request);
        return getDublinBusSubscriptionActiveTimeRanges(user, busStopId);
    }
}
