package ie.tcd.scss.busnotifier;

import ie.tcd.scss.busnotifier.service.GtfsService;
import ie.tcd.scss.busnotifier.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LessThanRawTests {
	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private GtfsService gtfsService;

	@Test
	public void test() throws IOException, NoSuchFieldException, InterruptedException, IllegalAccessException {
		gtfsService.loadStaticData();
		gtfsService.fetchStopTimes(List.of("C1", "C2"));
		var query = List.of(
				new GtfsService.StopAndRoute("3368", "C2"),
				new GtfsService.StopAndRoute("3368", "C1")
		);
		var result = gtfsService.fetchTripUpdates(query);
		System.out.println("DONE");
		Thread.sleep(10_000);
	}
}
