package ie.tcd.scss.busnotifier.repo;

import ie.tcd.scss.busnotifier.domain.Subscription;
import org.springframework.data.repository.CrudRepository;

public interface SubscriptionRepo extends CrudRepository<Subscription, String> {
}
