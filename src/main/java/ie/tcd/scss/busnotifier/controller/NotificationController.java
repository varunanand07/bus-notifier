package ie.tcd.scss.busnotifier.controller;

import ie.tcd.scss.busnotifier.schema.DeleteDublinBusSubscriptionsDTO;
import ie.tcd.scss.busnotifier.domain.User;
import ie.tcd.scss.busnotifier.schema.AddDublinBusSubscriptionDTO;
import ie.tcd.scss.busnotifier.schema.BrowserEndpointDTO;
import ie.tcd.scss.busnotifier.schema.DublinBusSubscriptionDTO;
import ie.tcd.scss.busnotifier.service.NotificationService;
import nl.martijndwars.webpush.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class NotificationController {
    @Autowired
    private NotificationService notificationService;

    @GetMapping("publicKey")
    public ResponseEntity<String> getPublicKey() {
        return ResponseEntity.ok(notificationService.getPublicKey());
    }

    @GetMapping("browserEndpoints")
    public ResponseEntity<Object> getBrowserEndpoints(@AuthenticationPrincipal User user) {
        var browserEndpointDTOS = notificationService
                .getBrowserEndpointsForUser(user)
                .stream()
                .map(BrowserEndpointDTO::new)
                .toList();
        return ResponseEntity.ok(browserEndpointDTOS);
    }

    @PostMapping("browserEndpoints")
    public ResponseEntity<Object> subscribe(@AuthenticationPrincipal User user, @RequestBody Subscription subscription) {
        notificationService.addBrowserEndpoint(user, subscription);
        return getBrowserEndpoints(user);
    }

    private record UnsubscribeBody(String endpoint) {}

    @DeleteMapping("browserEndpoints")
    public ResponseEntity<String> unsubscribe(@RequestBody UnsubscribeBody unsubscribeBody) {
        return notificationService.removeBrowserEndpoints(unsubscribeBody.endpoint)
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
    public ResponseEntity<Object> addDublinBusSubscriptions(@AuthenticationPrincipal User user, @RequestBody AddDublinBusSubscriptionDTO request) {
        notificationService.addDublinBusSubscription(user, request.endpoint, request.busStopId);
        return getDublinBusSubscriptions(user);
    }
}
