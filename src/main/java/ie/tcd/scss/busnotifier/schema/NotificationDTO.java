package ie.tcd.scss.busnotifier.schema;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class NotificationDTO {
    public String route;
    public String stop;
    public String eta;
    public boolean realTime;
}
