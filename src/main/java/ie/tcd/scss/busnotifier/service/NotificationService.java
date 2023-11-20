package ie.tcd.scss.busnotifier.service;

import ie.tcd.scss.busnotifier.repo.SubscriptionRepo;
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
import java.util.concurrent.ExecutionException;

@Service
public class NotificationService {

    private Logger logger = LoggerFactory.getLogger(NotificationService.class);
    @Value("${vapid.public.key}")
    private String publicKey;
    @Value("${vapid.private.key}")
    private String privateKey;

    @Autowired
    private SubscriptionRepo subscriptionRepo;

    private PushService pushService;

    @PostConstruct
    private void postConstruct() {
        Security.addProvider(new BouncyCastleProvider());
        try {
            pushService = new PushService(publicKey, privateKey);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    // Send a notification to users every second
    @Scheduled(fixedRate = 1000)
    private void myJob() {
        for (var sub : subscriptionRepo.findAll()) {
            logger.info("Sending to to " +  sub.endpoint);
            try {
                pushService.send(new Notification(sub.endpoint, sub.userPublicKey, sub.userAuth, "\"hello there\""));
            } catch (GeneralSecurityException | IOException | JoseException | ExecutionException |
                     InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

     public void addSubscription(Subscription subscription) {
        var sub = new ie.tcd.scss.busnotifier.domain.Subscription();
        sub.endpoint = subscription.endpoint;
        sub.userAuth = subscription.keys.auth;
        sub.userPublicKey = subscription.keys.p256dh;
        subscriptionRepo.save(sub);
    }

    /**
     * Remove a subscription entity from the database
     * @param endpoint
     * @return true if an element was deleted, false otherwise
     */
    public boolean removeSubscription(String endpoint) {
        // This is not thread safe (TOCTOU) and requires two database queries when it should only take one.
        if (subscriptionRepo.existsById(endpoint)) {
            subscriptionRepo.deleteById(endpoint);
            return true;
        } else {
            return false;
        }
    }


    public String getPublicKey() {
        return publicKey;
    }
}
