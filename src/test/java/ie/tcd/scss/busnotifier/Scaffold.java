package ie.tcd.scss.busnotifier;

import ie.tcd.scss.busnotifier.schema.RegisterRequestDTO;
import nl.martijndwars.webpush.Subscription;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.Random;

public class Scaffold {
    private static Random random = new Random();

    /**
     * Needed as we want parallel tests but we don't want to slow things down by cleaning up the DB
     * after each test
     * @return A register request DTO with a randomised username
     */
    public static RegisterRequestDTO generateRegisterDTO() {
        return RegisterRequestDTO
                .builder()
                .firstname("john")
                .lastname("squires")
                .username("xx_johnsquires_" + random.nextInt())
                .password("johnsquires123")
                .build();
    }

    public interface WithTokenAble<T> {
        ResponseEntity<T> withToken(String route, HttpEntity<Object> request, Class<T> responseClass);
    }
    public static <T> ResponseEntity<T> withToken(WithTokenAble<T> withTokenAble, String token, String route, Object body, Class<T> responseClass) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(token);
        var request = new HttpEntity<>(body, headers);
        return withTokenAble.withToken(route, request, responseClass);
    }

    public static Subscription generateSubscription(String endpoint) {
        var subscription = new Subscription();
        subscription.endpoint = endpoint;
        subscription.keys = new Subscription.Keys();
        subscription.keys.auth = "auth";
        subscription.keys.p256dh = "p256dh";
        return subscription;
    }
}
