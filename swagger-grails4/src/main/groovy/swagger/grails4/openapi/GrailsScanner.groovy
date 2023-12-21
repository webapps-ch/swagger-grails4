package swagger.grails4.openapi

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsControllerClass
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration
import io.swagger.v3.oas.integration.api.OpenApiScanner

class GrailsScanner implements OpenApiScanner {

    GrailsApplication grailsApplication
    OpenAPIConfiguration openApiConfiguration
    String namespace

    @Override
    void setConfiguration(OpenAPIConfiguration openApiConfiguration) {
        this.openApiConfiguration = openApiConfiguration
    }

    @Override
    Set<Class<?>> classes() {
        def classes = []
        for (GrailsControllerClass cls in grailsApplication.controllerClasses) {
            Boolean isDefaultNamespace = !namespace && null == cls.getNamespace()
            Boolean isCustomNamespace = namespace && namespace == cls.getNamespace()
            if (isDefaultNamespace || isCustomNamespace) {
            classes << cls.clazz
            }
        }
        return classes.sort{it.name}
    }

    @Override
    Map<String, Object> resources() {
        return new HashMap<>();
    }
}
