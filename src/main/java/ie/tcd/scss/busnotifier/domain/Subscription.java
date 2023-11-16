package ie.tcd.scss.busnotifier.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Subscription {
    @Id
    public String endpoint;

    @Column
    public String userPublicKey;

    @Column
    public String userAuth;
}
