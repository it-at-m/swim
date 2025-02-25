package de.muenchen.oss.swim.invoice.adapter.out.sap;

import de.lhm.pi.erechnung.swm.SIInvoiceDocumentSYOB;
import de.lhm.pi.erechnung.swm.SIInvoiceDocumentSYOBService;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import jakarta.xml.ws.soap.SOAPBinding;
import java.util.Collections;
import java.util.Set;
import javax.xml.namespace.QName;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
class SapConfiguration {
    private final SapProperties sapProperties;

    @Bean
    public SIInvoiceDocumentSYOB invoiceSoapClient() {
        final SIInvoiceDocumentSYOBService service = new SIInvoiceDocumentSYOBService();
        final SIInvoiceDocumentSYOB soapClient = service.getHTTPPort();
        ((BindingProvider) soapClient).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, sapProperties.getEndpoint());
        ((BindingProvider) soapClient).getRequestContext().put(BindingProvider.USERNAME_PROPERTY, sapProperties.getUsername());
        ((BindingProvider) soapClient).getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, sapProperties.getPassword());
        final SOAPBinding binding = (SOAPBinding) ((BindingProvider) soapClient).getBinding();
        binding.setMTOMEnabled(true);
        binding.setHandlerChain(Collections.singletonList(new InvoiceResponseSOAPHandler()));
        return soapClient;
    }

    /* default */ static class InvoiceResponseSOAPHandler implements SOAPHandler<SOAPMessageContext> {
        public static final String NS_URI = "http://sap.com/xi/XI/Message/30";
        public static final String NAME_RELIABLE_MESSAGING = "ReliableMessaging";
        public static final String NAME_SYSTEM = "System";
        public static final String NAME_DYNAMIC_CONFIGURATION = "DynamicConfiguration";
        public static final String NAME_PASSPORT = "Passport";
        public static final String NAME_HOP_LIST = "HopList";
        public static final String NAME_MAIN = "Main";

        @Override
        public Set<QName> getHeaders() {
            // This will tell CXF that the following headers are UNDERSTOOD
            return Set.of(new QName(NS_URI, NAME_RELIABLE_MESSAGING),
                    new QName(NS_URI, NAME_SYSTEM),
                    new QName(NS_URI, NAME_DYNAMIC_CONFIGURATION),
                    new QName(NS_URI, NAME_PASSPORT),
                    new QName(NS_URI, NAME_HOP_LIST),
                    new QName(NS_URI, NAME_MAIN));
        }

        @Override
        public boolean handleMessage(SOAPMessageContext context) {
            // allow any other handler to execute
            return true;
        }

        @Override
        public boolean handleFault(SOAPMessageContext context) {
            // allow any other handler to execute
            return true;
        }

        @Override
        public void close(MessageContext context) {
            //do nothing
        }
    }

}
