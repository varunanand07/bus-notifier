package ie.tcd.scss.busnotifier.repo;

import ie.tcd.scss.busnotifier.domain.BrowserEndpoint;
import ie.tcd.scss.busnotifier.domain.DublinBusSubscription;
import ie.tcd.scss.busnotifier.domain.User;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface DublinBusSubscriptionRepo extends CrudRepository<DublinBusSubscription, String> {
    void deleteByUserIdAndBrowserEndpointEndpointAndBusStopId(Integer userId, String browserEndpointId, String busStopId);
    List<DublinBusSubscription> findByUser(User user);
}
