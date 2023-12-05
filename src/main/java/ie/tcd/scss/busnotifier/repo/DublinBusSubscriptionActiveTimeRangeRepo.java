package ie.tcd.scss.busnotifier.repo;

import ie.tcd.scss.busnotifier.domain.DublinBusSubscription;
import ie.tcd.scss.busnotifier.domain.DublinBusSubscriptionActiveTimeRange;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DublinBusSubscriptionActiveTimeRangeRepo extends CrudRepository<DublinBusSubscriptionActiveTimeRange, String> {

    List<DublinBusSubscriptionActiveTimeRange> findByDublinBusSubscription(DublinBusSubscription dublinBusSubscription);
}
