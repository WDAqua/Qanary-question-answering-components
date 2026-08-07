package eu.wdaqua.qanary.component.shuyo.ld;

import eu.wdaqua.qanary.component.QanaryComponent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Regression test for the duplicate {@link QanaryComponent} bean definition that broke the
 * deployment ("expected single matching bean but found 2: languageDetection,qanaryComponent").
 * <p>
 * {@link LanguageDetection} is annotated with {@code @Component} and was additionally created by
 * an {@code @Bean} factory method in {@link Application}, so the very same class was registered
 * twice. The failure was startup-order dependent and therefore invisible to the other tests: the
 * {@code @Bean} method declares {@code QanaryComponent} as its return type, so Spring only learns
 * that this bean is in fact a {@link LanguageDetection} once the bean has been instantiated. Only
 * then does {@link LanguageDetectorController}, which injects the concrete {@link LanguageDetection}
 * type, see two candidates and the context fails to start.
 * <p>
 * The assertions below are deliberately independent of that ordering: they count the registered
 * bean definitions instead of relying on a particular instantiation sequence.
 */
@SpringBootTest(classes = Application.class)
class QanaryComponentBeanUniquenessTest {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * The component must be registered exactly once. Two definitions make every injection point
     * that asks for a {@link QanaryComponent} by type ambiguous.
     */
    @Test
    void exactlyOneQanaryComponentBeanIsRegistered() {
        String[] beanNames = applicationContext.getBeanNamesForType(QanaryComponent.class);

        assertEquals(1, beanNames.length,
                "expected exactly one QanaryComponent bean, but found: " + String.join(",", beanNames));
    }

    /**
     * Reproduces the production failure directly. Resolving the {@link QanaryComponent} first
     * forces Spring to determine the concrete type of every candidate - exactly the situation on
     * the deployment server. The subsequent by-type lookup of the concrete {@link LanguageDetection}
     * threw a {@code NoUniqueBeanDefinitionException} before the fix.
     */
    @Test
    void languageDetectionResolvesUnambiguouslyAfterTheComponentWasInstantiated() {
        QanaryComponent qanaryComponent = applicationContext.getBean(QanaryComponent.class);
        assertNotNull(qanaryComponent);

        LanguageDetection languageDetection = applicationContext.getBean(LanguageDetection.class);

        assertSame(qanaryComponent, languageDetection,
                "the QanaryComponent and the LanguageDetection must be the very same instance");
    }

    /**
     * The controller injects {@link LanguageDetection} by its concrete type and was the constructor
     * that actually failed on startup, so it must be present in a fully wired context.
     */
    @Test
    void languageDetectorControllerIsWired() {
        assertNotNull(applicationContext.getBean(LanguageDetectorController.class));
    }
}
