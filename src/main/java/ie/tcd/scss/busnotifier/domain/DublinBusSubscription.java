package ie.tcd.scss.busnotifier.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DublinBusSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column
    private String busStopId;

    @Column
    private String busId;

    @ManyToOne
    public User user;

    @OneToMany
    @Builder.Default
    @Fetch(value = FetchMode.JOIN)
    public List<DublinBusSubscriptionActiveTimeRange> activeTimeRanges = new ArrayList<>();

    /**
     * The browser endpoints to which notifications for this bus stop will be sent.
     */
    @ManyToMany
    @Builder.Default
    public List<BrowserEndpoint> browserEndpoints = new ArrayList<>();
}
