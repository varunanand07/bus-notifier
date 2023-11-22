package ie.tcd.scss.busnotifier.schema;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class AddDublinBusSubscriptionDTO {
    @NotBlank
    public String endpoint;

    @NotBlank
    public String busStopId;
}
