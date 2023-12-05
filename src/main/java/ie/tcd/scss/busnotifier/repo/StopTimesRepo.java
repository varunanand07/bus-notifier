package ie.tcd.scss.busnotifier.repo;

import ie.tcd.scss.busnotifier.domain.StopTime;
import ie.tcd.scss.busnotifier.service.GtfsService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface StopTimesRepo extends JpaRepository<StopTime, StopTime.StopTimeId> {

    StopTime findByTripIdAndStopId(String tripId, String stopId);
    StopTime findByTripIdAndStopSequence(String tripId, String stopSequence);

    List<StopTime> findByTripIdInAndStopId(Set<String> tripIds, String stopId);
}
