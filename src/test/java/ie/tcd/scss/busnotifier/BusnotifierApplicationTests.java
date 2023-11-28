package ie.tcd.scss.busnotifier;

import ie.tcd.scss.busnotifier.schema.*;
import ie.tcd.scss.busnotifier.service.JwtService;
import nl.martijndwars.webpush.Subscription;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BusnotifierApplicationTests {
	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private JwtService jwtService;

	private Random random = new Random();

	private String route(Object... parts) {
		return "http://localhost:" + port + "/" + String.join("/", Arrays.stream(parts).map(Object::toString).toList());
	}

	/**
	 * Needed as we want parallel tests but we don't want to slow things down by cleaning up the DB
	 * after each test
	 * @return A register request DTO with a randomised username
	 */
	private RegisterRequestDTO generateRegisterDTO() {
		return RegisterRequestDTO
				.builder()
				.firstname("john")
				.lastname("squires")
				.username("xx_johnsquires_" + random.nextInt())
				.password("johnsquires123")
				.build();
	}

	@Test
	public void verifyJWTContents() {
		var registerRequest = generateRegisterDTO();
		var authenticationResponse = restTemplate.postForEntity(
				route("register"),
				registerRequest,
				AuthenticationResponseDTO.class
		);
		assertThat(authenticationResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		var token = authenticationResponse.getBody().getToken();
		var claims = jwtService.extractAllClaims(token);
		assertThat(claims.getSubject()).isEqualTo(registerRequest.getUsername());
	}


	interface WithTokenAble<T> {
		ResponseEntity<T> withToken(String route, HttpEntity<Object> request, Class<T> responseClass);
	}
	public <T> ResponseEntity<T> withToken(WithTokenAble<T> withTokenAble, String token, String route, Object body, Class<T> responseClass) {
		var headers = new HttpHeaders();
		headers.setBearerAuth(token);
		var request = new HttpEntity<>(body, headers);
		return withTokenAble.withToken(route, request, responseClass);
	}

	private Subscription generateSubscription(String endpoint) {
		var subscription = new Subscription();
		subscription.endpoint = endpoint;
		subscription.keys = new Subscription.Keys();
		subscription.keys.auth = "auth";
		subscription.keys.p256dh = "p256dh";
		return subscription;
	}

	@Test
	public void canRegisterAndAddBrowserEndpoints() {
		var registerRequest = generateRegisterDTO();
		var authenticationResponse = restTemplate.postForEntity(
				route("register"),
				registerRequest,
				AuthenticationResponseDTO.class
		);
		assertThat(authenticationResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		var token = authenticationResponse.getBody().getToken();

		var subscription = generateSubscription("endpoint");
		var browserEndpointDTOsResponse = withToken(
				restTemplate::postForEntity,
				token,
				route("browserEndpoints"),
				subscription,
				BrowserEndpointDTO[].class
		);
		assertThat(browserEndpointDTOsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		var browserEndpointDTOs = browserEndpointDTOsResponse.getBody();
		assertNotNull(browserEndpointDTOs);
		assertEquals(browserEndpointDTOs.length, 1);
		assertThat(browserEndpointDTOs[0].endpoint).isEqualTo(subscription.endpoint);
	}

	@Test
	public void canRegisterAndAddBusStopAssociations() {
		var registerRequest = generateRegisterDTO();
		var authenticationResponse = restTemplate.postForEntity(
				route("register"),
				registerRequest,
				AuthenticationResponseDTO.class
		);
		assertThat(authenticationResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		var token = authenticationResponse.getBody().getToken();

		var subscription = generateSubscription("endpoint");
		var browserEndpointDTOsResponse = withToken(
				restTemplate::postForEntity,
				token,
				route("browserEndpoints"),
				subscription,
				BrowserEndpointDTO[].class
		);
		assertThat(browserEndpointDTOsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		var browserEndpointDTOs = browserEndpointDTOsResponse.getBody();
		assertNotNull(browserEndpointDTOs);
		assertEquals(browserEndpointDTOs.length, 1);
		assertThat(browserEndpointDTOs[0].endpoint).isEqualTo(subscription.endpoint);

		var request = new AddDublinBusSubscriptionDTO(subscription.endpoint, "66a");
		var dublinBusSubscriptionDTOsResponse = withToken(
				restTemplate::postForEntity,
				token,
				route("dublinBusSubscriptions"),
				request,
				DublinBusSubscriptionDTO[].class
		);
		assertEquals(dublinBusSubscriptionDTOsResponse.getStatusCode(), HttpStatus.OK);
		var dublinBusSubscriptionDTOs = dublinBusSubscriptionDTOsResponse.getBody();
		assertNotNull(dublinBusSubscriptionDTOs);
		assertEquals(dublinBusSubscriptionDTOs.length, 1);
		assertEquals(dublinBusSubscriptionDTOs[0].busStopId, request.busStopId);
		assertEquals(dublinBusSubscriptionDTOs[0].endpoints.get(0), request.endpoint);
	}

	@Test
	public void canManipulateActiveTimeRanges() {
		var registerRequest = generateRegisterDTO();
		var authenticationResponse = restTemplate.postForEntity(
				route("register"),
				registerRequest,
				AuthenticationResponseDTO.class
		);
		assertThat(authenticationResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		var token = authenticationResponse.getBody().getToken();

		var subscription = generateSubscription("endpoint");
		var browserEndpointDTOsResponse = withToken(
				restTemplate::postForEntity,
				token,
				route("browserEndpoints"),
				subscription,
				BrowserEndpointDTO[].class
		);
		assertThat(browserEndpointDTOsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		var browserEndpointDTOs = browserEndpointDTOsResponse.getBody();
		assertNotNull(browserEndpointDTOs);
		assertEquals(browserEndpointDTOs.length, 1);
		assertThat(browserEndpointDTOs[0].endpoint).isEqualTo(subscription.endpoint);

		var request = new AddDublinBusSubscriptionDTO(subscription.endpoint, "66a");
		var dublinBusSubscriptionDTOsResponse = withToken(
				restTemplate::postForEntity,
				token,
				route("dublinBusSubscriptions"),
				request,
				DublinBusSubscriptionDTO[].class
		);
		assertEquals(dublinBusSubscriptionDTOsResponse.getStatusCode(), HttpStatus.OK);
		var dublinBusSubscriptionDTOs = dublinBusSubscriptionDTOsResponse.getBody();
		assertNotNull(dublinBusSubscriptionDTOs);
		assertEquals(1, dublinBusSubscriptionDTOs.length);
		assertEquals(request.busStopId, dublinBusSubscriptionDTOs[0].busStopId);
		assertEquals(1, dublinBusSubscriptionDTOs[0].endpoints.size());
		assertEquals(request.endpoint, dublinBusSubscriptionDTOs[0].endpoints.get(0));

		var activeTimeRangeRequest = new DublinBusSubscriptionActiveTimeRangeDTO(DayOfWeek.MONDAY, 7, 0, 8, 30);
		var dublinBusSubscriptionActiveTimeRangeDTOsResponse = withToken(
				restTemplate::postForEntity,
				token,
				route("dublinBusSubscriptions", request.busStopId, "activeTimeRanges"),
				activeTimeRangeRequest,
				DublinBusSubscriptionActiveTimeRangeDTO[].class
		);
		assertEquals(HttpStatus.OK, dublinBusSubscriptionActiveTimeRangeDTOsResponse.getStatusCode());
		var dublinBusActiveTimeRangeDTOs = dublinBusSubscriptionActiveTimeRangeDTOsResponse.getBody();
		assertNotNull(dublinBusSubscriptionDTOs);
		assertEquals(1, dublinBusActiveTimeRangeDTOs.length);
	}

	@Test
	public void noSpuriousBusStopsAreGenerated() {

	}
}
