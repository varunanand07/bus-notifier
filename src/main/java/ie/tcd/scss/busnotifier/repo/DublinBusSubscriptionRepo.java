package ie.tcd.scss.busnotifier.repo;

import ie.tcd.scss.busnotifier.domain.DublinBusSubscription;
import ie.tcd.scss.busnotifier.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface DublinBusSubscriptionRepo extends CrudRepository<DublinBusSubscription, Integer> {

    @EntityGraph(attributePaths = { "user" })
    Iterable<DublinBusSubscription> findAll();
    Optional<DublinBusSubscription> findByUserAndBusStopId(User user, String busStopId);
    List<DublinBusSubscription> findByUser(User user);

    boolean existsByUserAndBusStopId(User user, String busStopIdentifier);
}
