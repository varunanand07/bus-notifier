package ie.tcd.scss.busnotifier.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DublinBusSubscription {

    @ManyToOne
    public User user;

    /**
     * The browser endpoints to which notifications for this bus stop will be sent.
     */
    @ManyToOne
    public BrowserEndpoint browserEndpoint;

    @Id
    private String busStopId;

}
