package ie.tcd.scss.busnotifier.schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenericErrorDTO {
    public String code;
    public String message;
}
