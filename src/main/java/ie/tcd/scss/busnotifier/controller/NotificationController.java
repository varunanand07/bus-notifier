package ie.tcd.scss.busnotifier.controller;

import ie.tcd.scss.busnotifier.service.NotificationService;
import nl.martijndwars.webpush.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    @Autowired
    private NotificationService notificationService;

    @GetMapping("publicKey")
    public ResponseEntity<String> getPublicKey() {
        return ResponseEntity.ok(notificationService.getPublicKey());
    }

    @PostMapping("subscribe")
    public void subscribe(@RequestBody Subscription subscription) {
        notificationService.addSubscription(subscription);
    }

    private record UnsubscribeBody(String endpoint) {}

    @PostMapping("unsubscribe")
    public ResponseEntity<String> unsubscribe(@RequestBody UnsubscribeBody unsubscribeBody) {
        return notificationService.removeSubscription(unsubscribeBody.endpoint)
                ? ResponseEntity.ok("")
                : ResponseEntity.notFound().build();
    }
}
