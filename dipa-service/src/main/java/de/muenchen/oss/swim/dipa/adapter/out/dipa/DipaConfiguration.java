package de.muenchen.oss.swim.dipa.adapter.out.dipa;

import com.fabasoft.schemas.websvc.mucsdipabai_15_1700_giwsd.MUCSDIPABAI151700GIWSD;
import com.fabasoft.schemas.websvc.mucsdipabai_15_1700_giwsd.MUCSDIPABAI151700GIWSDSoap;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.soap.SOAPBinding;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DipaConfiguration {
    @Bean
    protected MUCSDIPABAI151700GIWSD clientService() {
        return new MUCSDIPABAI151700GIWSD();
    }

    @Bean
    protected MUCSDIPABAI151700GIWSDSoap soapClient(final MUCSDIPABAI151700GIWSD clientService, final DipaProperties properties) {
        final MUCSDIPABAI151700GIWSDSoap soapClient = clientService.getMUCSDIPABAI151700GIWSDSoap();
        ((BindingProvider) soapClient).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, properties.getEndpointUrl());
        ((BindingProvider) soapClient).getRequestContext().put(BindingProvider.USERNAME_PROPERTY, properties.getUsername());
        ((BindingProvider) soapClient).getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, properties.getPassword());
        // enable MTOM
        final SOAPBinding binding = (SOAPBinding) ((BindingProvider) soapClient).getBinding();
        binding.setMTOMEnabled(true);
        // enable chunking
        @SuppressWarnings("PMD.CloseResource")
        final Client client = ClientProxy.getClient(soapClient);
        final HTTPConduit conduit = (HTTPConduit) client.getConduit();
        final HTTPClientPolicy policy = conduit.getClient();
        policy.setAllowChunking(true);
        policy.setReceiveTimeout(properties.getSendTimeout().toMillis());
        return soapClient;
    }
}
