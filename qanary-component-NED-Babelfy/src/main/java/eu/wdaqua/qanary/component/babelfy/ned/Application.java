package eu.wdaqua.qanary.component.babelfy.ned;

import com.google.gson.JsonArray;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.babelfy.ned.exception.ApiLiveTestFaildException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.util.ArrayList;

@SpringBootApplication
@ComponentScan(basePackages = {"eu.wdaqua.qanary"})
/**
 * basic class for wrapping functionality to a Qanary component
 * note: there is no need to change something here
 */
public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    /**
     * this method is needed to make the QanaryComponent in this project known
     * to the QanaryServiceController in the qanary_component-template
     *
     * @return
     */
    @Bean
    public QanaryComponent qanaryComponent(
            @Autowired BabelfyServiceFetcher babelfyServiceFetcher, //
            @Value("${spring.application.name}") String applicationName, //
            @Value("${babelfy.api.live.test.active}") boolean apiLiveTestActive, //
            @Value("${babelfy.api.live.test.question}") String testQuestion //
    ) throws ApiLiveTestFaildException {
        if (apiLiveTestActive) {
            // check functionality with live API
            for (int i = 0; i < 10; i++) {
                try {
                    this.testFunctionality(babelfyServiceFetcher, testQuestion);
                    logger.info("Functionality works as expectted");
                    break;
                } catch (Exception e) {
                    logger.warn("Functionality does not work as expected on attempt no. {}:{}", i, e.toString());
                }
                if (i > 8) {
                    logger.error("Functionality does not work after maximum tries. Exiting...");
                    throw new ApiLiveTestFaildException("Could not start component");
                }
            }
        }

        return new BabelfyNED(applicationName, babelfyServiceFetcher);
    }

    // test the internal functionality of this component, using live APIs
    private void testFunctionality(BabelfyServiceFetcher babelfyServiceFetcher, String testQuestion)
            throws Exception {
        // THIS COMPONENT IS DEPRECATED
        // because the intended functionality cannot be reconstructed
        // only the availability of the API endpoint can be tested
        JsonArray response = babelfyServiceFetcher.sendRequestToApi(testQuestion);
        ArrayList<BabelfyServiceFetcher.Link> links = babelfyServiceFetcher.getLinksForQuestion(response);

        if (links.size() == 0) {
            throw new Exception("No links found");
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
