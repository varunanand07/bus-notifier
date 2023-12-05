package ie.tcd.scss.busnotifier.repo;

import ie.tcd.scss.busnotifier.domain.DublinBusSubscription;
import ie.tcd.scss.busnotifier.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface DublinBusSubscriptionRepo extends CrudRepository<DublinBusSubscription, Integer> {

    @EntityGraph(attributePaths = { "user", "browserEndpoints"})
    List<DublinBusSubscription> findAll();
    Optional<DublinBusSubscription> findByUserAndBusStopIdAndBusId(User user, String busStopId, String busId);
    List<DublinBusSubscription> findByUser(User user);

    boolean existsByUserAndBusStopIdAndBusId(User user, String busStopIdentifier, String busId);
}
