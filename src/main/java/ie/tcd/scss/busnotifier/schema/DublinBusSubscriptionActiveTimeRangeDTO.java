package ie.tcd.scss.busnotifier.schema;

import ie.tcd.scss.busnotifier.domain.DublinBusSubscriptionActiveTimeRange;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DublinBusSubscriptionActiveTimeRangeDTO {
    public DayOfWeek day;
    public int startHour;

    public int startMinute;

    public int endHour;
    public int endMinute;

    public DublinBusSubscriptionActiveTimeRangeDTO(DublinBusSubscriptionActiveTimeRange dublinBusSubscriptionActiveTimeRange) {
        this.day = dublinBusSubscriptionActiveTimeRange.day;
        this.startHour = dublinBusSubscriptionActiveTimeRange.startHour;
        this.endHour = dublinBusSubscriptionActiveTimeRange.endHour;
        this.startMinute = dublinBusSubscriptionActiveTimeRange.startMinute;
        this.endMinute = dublinBusSubscriptionActiveTimeRange.endMinute;
    }

    /**
     * @return the validated domain object
     * @throws IllegalArgumentException thrown if the user provided DTO is invalid
     */
    public DublinBusSubscriptionActiveTimeRange validate() {
        var valid = 0 <= startHour && startHour <= endHour && endHour < 24
                && 0 <= startMinute && startMinute < 60
                && 0 <= endMinute && endMinute < 60
                && (startHour != endHour || startMinute < endMinute);
        if (!valid) {
            var message = String.format("Could not validate user provided time range! (%02d:%02d → %02d:%02d)", startHour, startMinute, endHour, endMinute);
            throw new IllegalArgumentException(message);
        } else {
            return DublinBusSubscriptionActiveTimeRange
                    .builder()
                    .day(day)
                    .startHour(startHour)
                    .endHour(endHour)
                    .startMinute(startMinute)
                    .endMinute(endMinute)
                    .build();
        }
    }
}
