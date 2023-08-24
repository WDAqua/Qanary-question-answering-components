package eu.wdaqua.qanary.component.dateofdeathrealpersons.qb.controller;

import org.apache.tomcat.util.buf.EncodedSolidusHandling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

/**
 * Service to allow encoded "/" with "%2" in URLs
 */
@Service
public class WebServerFActoryCustomizer {

    private final Logger logger = LoggerFactory.getLogger(WebServerFActoryCustomizer.class);

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        logger.info("Configuring Tomcat to allow encoded slashes.");
        return factory -> factory.addConnectorCustomizers(connector -> connector.setEncodedSolidusHandling(
                EncodedSolidusHandling.DECODE.getValue()));
    }

}
