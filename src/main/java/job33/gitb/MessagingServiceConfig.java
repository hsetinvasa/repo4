package job33.gitb;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;

/**
 * Configuration class responsible for creating the Spring beans required by the service.
 */
@Configuration
public class MessagingServiceConfig {

    @Autowired
    private Bus cxfBus = null;

    @Autowired
    private MessagingServiceImpl messagingServiceImplementation = null;

    /**
     * The CXF endpoint that will serve service calls.
     *
     * @return The endpoint.
     */
    @Bean
    public Endpoint messagingService() {
        EndpointImpl endpoint = new EndpointImpl(cxfBus, messagingServiceImplementation);
        endpoint.setServiceName(new QName("http://www.gitb.com/ms/v1/", "MessagingServiceService"));
        endpoint.setEndpointName(new QName("http://www.gitb.com/ms/v1/", "MessagingServicePort"));
        endpoint.publish("/messaging");
        return endpoint;
    }

}
