package swagger.grails4

import grails.core.GrailsApplication
import grails.web.mapping.LinkGenerator
import grails.web.mapping.UrlMappingsHolder
import io.swagger.v3.oas.integration.GenericOpenApiContext
import io.swagger.v3.oas.integration.SwaggerConfiguration
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration
import io.swagger.v3.oas.integration.api.OpenApiContext
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.ApplicationContext
import swagger.grails4.openapi.GrailsScanner
import swagger.grails4.openapi.Reader

/**
 * 生成 OpenAPI 文档json的服务
 */
class OpenApiService {

    LinkGenerator linkGenerator

    GrailsApplication grailsApplication
    ApplicationContext applicationContext

    /**
     * 生成 OpenAPI 文档对象
     */
    def generateDocument(String namespace = null) {
        OpenAPIConfiguration config = new SwaggerConfiguration().openAPI(configOpenApi(namespace))
        config.setReaderClass("swagger.grails4.openapi.Reader")
        OpenApiContext ctx = new GenericOpenApiContext().openApiConfiguration(config)
        ctx.setOpenApiScanner(new GrailsScanner(grailsApplication: grailsApplication, namespace: namespace))
        ctx.setOpenApiReader(new Reader(application: grailsApplication, config: config))
        ctx.init()
        ctx.read()
    }

    /**
     * Create an OpenAPI object with configured ahead.
     * @return OpenAPI object has been configured.
     */
    OpenAPI configOpenApi(String namespace = null) {
        def config = grailsApplication.config.navigate('openApi', 'doc', namespace ?: 'default', 'info')
        Info info = new Info().title(config.title ?: null).description(config.description ?: null).version(config.version ?: null)
        new OpenAPI().info(info)
    }
}
