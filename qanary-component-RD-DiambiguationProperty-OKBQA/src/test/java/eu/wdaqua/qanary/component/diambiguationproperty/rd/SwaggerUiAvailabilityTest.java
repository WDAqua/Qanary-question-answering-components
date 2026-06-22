package eu.wdaqua.qanary.component.diambiguationproperty.rd;

import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies the Swagger UI is reachable through /swagger-ui, /swagger, /openapi, /docs
 * and the OpenAPI JSON through /api-docs. Logic lives in the shared framework base.
 */
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SwaggerUiAvailabilityTest extends eu.wdaqua.qanary.component.AbstractSwaggerUiAvailabilityTest {
}
