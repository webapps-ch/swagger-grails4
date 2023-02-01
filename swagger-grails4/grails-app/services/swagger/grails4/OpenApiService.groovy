package swagger.grails4

import grails.core.GrailsApplication
import grails.web.mapping.LinkGenerator
import io.swagger.v3.oas.integration.GenericOpenApiContext
import io.swagger.v3.oas.integration.SwaggerConfiguration
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration
import io.swagger.v3.oas.integration.api.OpenApiContext
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
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
        Info info = new Info().title(config?.title ?: null).description(config?.description ?: null).version(config?.version ?: null)
        OpenAPI openAPI = new OpenAPI()
        openAPI.info(info)

        //Set server if configured
        def serverConfig = grailsApplication.config.navigate('openApi', 'doc', namespace ?: 'default', 'servers')
        if (serverConfig) {
            List<Server> servers = serverConfig.collect { serverMap -> new Server().url(serverMap?.url ?: null).description(serverMap?.description ?: null)}
            openAPI.servers(servers)
        }

        //Set security schemes if configured
        def securitySchemes = grailsApplication.config.navigate('openApi', 'doc', namespace ?: 'default', 'components', 'securitySchemes')
        securitySchemes?.each { name, map ->
            if (!openAPI.components) {
                openAPI.components(new Components())
            }
            def secScheme = new SecurityScheme(map)
            openAPI.components.addSecuritySchemes(name, secScheme)
        }

        //Set global security requirement if configured
        def globalSecurity = grailsApplication.config.navigate('openApi', 'doc', namespace ?: 'default', 'security')
        globalSecurity?.each { name, map ->
            if (!openAPI.security) {
                openAPI.security([])
            }
            openAPI.security.add(new SecurityRequirement().addList(name, map ?: []))
        }

        return openAPI
    }
}
