package ie.tcd.scss.busnotifier;

import ie.tcd.scss.busnotifier.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BusnotifierApplication {

	@Autowired
	private NotificationService notificationService;
	public static void main(String[] args) {
		SpringApplication.run(BusnotifierApplication.class, args);
	}

}
