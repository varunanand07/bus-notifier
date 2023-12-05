package ie.tcd.scss.busnotifier.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stop_times")
@IdClass(StopTime.StopTimeId.class)
public class StopTime {
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StopTimeId {
        public String tripId;
        public String stopId;
    }

    @Id @Column(name="trip_id") public String tripId;
    @Id @Column(name="stop_id") public String stopId;

    @Column(name="timepoint")      public String timepoint;
    @Column(name="drop_off_type")  public String dropOffType;
    @Column(name="pickup_type")    public String pickupType;
    @Column(name="stop_headsign")  public String stopHeadsign;
    @Column(name="stop_sequence")  public String stopSequence;
    @Column(name="departure_time") public String departureTime;
    @Column(name="arrival_time")   public String arrivalTime;
}
