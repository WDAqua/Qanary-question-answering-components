package eu.wdaqua.qanary.component.dbpediaspotlight.ned;

import eu.wdaqua.qanary.component.QanaryComponent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Regression test guarding against a duplicate {@link QanaryComponent} bean definition.
 * <p>
 * {@link DBpediaSpotlightNED} must be registered exactly once - either by the {@code qanaryComponent()}
 * factory method in {@link Application} or by the component scan, never by both. Registering it
 * twice makes the application fail to start with "expected single matching bean but found 2".
 * <p>
 * That failure is startup-order dependent and therefore easy to miss: a factory method declaring
 * {@code QanaryComponent} as its return type only reveals its concrete type once the bean has
 * been instantiated, so the ambiguity can stay hidden in tests and only surface on the deployment
 * server. The assertions below count bean definitions instead of relying on a particular
 * instantiation order.
 */
@SpringBootTest(classes = Application.class)
class QanaryComponentBeanUniquenessTest {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * The component must be registered exactly once - two definitions make every injection point
     * that asks for a {@link QanaryComponent} by type ambiguous.
     */
    @Test
    void exactlyOneQanaryComponentBeanIsRegistered() {
        String[] beanNames = applicationContext.getBeanNamesForType(QanaryComponent.class);

        assertEquals(1, beanNames.length,
                "expected exactly one QanaryComponent bean, but found: " + String.join(",", beanNames));
    }

    /**
     * Resolving the {@link QanaryComponent} first forces Spring to determine the concrete type of
     * every candidate - exactly the situation on the deployment server. A subsequent by-type lookup
     * of the concrete component class must still be unambiguous.
     */
    @Test
    void componentResolvesUnambiguouslyByItsConcreteType() {
        QanaryComponent qanaryComponent = applicationContext.getBean(QanaryComponent.class);
        assertNotNull(qanaryComponent);

        DBpediaSpotlightNED component = applicationContext.getBean(DBpediaSpotlightNED.class);

        assertSame(qanaryComponent, component,
                "the QanaryComponent and the DBpediaSpotlightNED must be the very same instance");
    }
}
