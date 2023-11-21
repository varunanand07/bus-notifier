package ie.tcd.scss.busnotifier.repo;

import ie.tcd.scss.busnotifier.domain.BrowserEndpoint;
import ie.tcd.scss.busnotifier.domain.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface BrowserEndpointRepo extends CrudRepository<BrowserEndpoint, String> {
    List<BrowserEndpoint> findByUser(User user);
}
