package ie.tcd.scss.busnotifier.domain;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DublinBusSubscriptionActiveTimeRange {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Integer id;

    @Column
    @Enumerated(EnumType.STRING)
    public DayOfWeek day;

    @Column
    public Integer startHour;

    @Column
    public Integer startMinute;

    @Column
    public Integer endHour;

    @ManyToOne
    public DublinBusSubscription dublinBusSubscription;

    @Column
    public Integer endMinute;

    public boolean overlaps(DublinBusSubscriptionActiveTimeRange other) {
        return overlaps(other.day, other.startHour, other.endHour, other.startMinute, other.endMinute);
    }

    public boolean overlaps(DayOfWeek day, int startHour, int endHour, int startMinute, int endMinute) {
        return this.day == day
                && (this.startHour <= startHour && startHour <= this.endHour
                || startHour <= this.startHour && this.startHour <= endHour)
                && (this.startMinute <= startMinute && startMinute <= this.endMinute
                || startMinute <= this.startMinute && this.startMinute <= endMinute);
    }
    public boolean contains(DayOfWeek day, int hour, int minute) {
        return this.day == day
                && this.startHour <= hour && hour <= this.endHour
                && (!this.startHour.equals(this.endHour) || this.startMinute <= minute && minute <= this.endMinute);
    }
}
