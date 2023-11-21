package ie.tcd.scss.busnotifier.schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenericErrorDTO {
    private String code;
    private String message;
}
