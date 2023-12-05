package ie.tcd.scss.busnotifier.repo;

import ie.tcd.scss.busnotifier.domain.BrowserEndpoint;
import ie.tcd.scss.busnotifier.domain.DublinBusSubscription;
import ie.tcd.scss.busnotifier.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface BrowserEndpointRepo extends CrudRepository<BrowserEndpoint, Integer> {
    List<BrowserEndpoint> findByUser(User user);
    
    Optional<BrowserEndpoint> findByUserAndEndpoint(User user, String endpoint);

    void deleteByUserAndEndpoint(User user, String endpoint);

    boolean existsByUserAndEndpoint(User user, String endpoint);

    @EntityGraph(attributePaths = { "dublinBusSubscriptions", "user" })
    List<BrowserEndpoint> findByDublinBusSubscriptions(DublinBusSubscription subscription);
}
