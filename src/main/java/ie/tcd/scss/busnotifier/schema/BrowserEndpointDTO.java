package ie.tcd.scss.busnotifier.schema;

import ie.tcd.scss.busnotifier.domain.BrowserEndpoint;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.asynchttpclient.request.body.generator.PushBody;

@NoArgsConstructor
@AllArgsConstructor
public class BrowserEndpointDTO {
    public String endpoint;
    public String device;

    public Integer userId;

    public BrowserEndpointDTO(BrowserEndpoint browserEndpoint) {
        this.endpoint = browserEndpoint.endpoint;
        this.device = browserEndpoint.description;
        this.userId = browserEndpoint.user.getId();
    }
}
