package swagger.grails4.openapi

import io.swagger.v3.oas.models.parameters.Parameter
import main.swagger.grails4.openapi.ApiDocExampleValue
import main.swagger.grails4.openapi.ApiDocGenericValue
import main.swagger.grails4.openapi.builder.ApiDocExampleValueBuilder
import main.swagger.grails4.openapi.builder.ApiDocGenericValueBuilder

// import grails.artefact.DomainClass
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsControllerClass
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import grails.web.Action
import org.codehaus.groovy.grails.web.mapping.UrlCreator
import org.codehaus.groovy.grails.web.mapping.UrlMapping
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder
import groovy.util.logging.Slf4j
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration
import io.swagger.v3.oas.integration.api.OpenApiReader
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import org.codehaus.groovy.grails.web.mapping.RegexUrlMapping
import swagger.grails4.UrlMappings
import swagger.grails4.openapi.ApiDoc
import swagger.grails4.openapi.builder.AnnotationBuilder
import swagger.grails4.openapi.builder.OperationBuilder
import swagger.grails4.openapi.builder.TagBuilder

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

@Slf4j
class Reader implements OpenApiReader {

    final static String JSON_MIME = "application/json"
    final static String MULTIPART_FORM_DATA_MIME = "multipart/form-data"


    OpenAPIConfiguration config
    GrailsApplication application

    private OpenAPI openAPI = new OpenAPI()

    @Override
    void setConfiguration(OpenAPIConfiguration openApiConfiguration) {
        this.config = openApiConfiguration
    }

    @Override
    OpenAPI read(Set<Class<?>> classes, Map<String, Object> resources) {
        openAPI.setInfo(config.openAPI.getInfo())
        openAPI.setServers(config.openAPI.getServers())
        openAPI.setSecurity(config.openAPI.getSecurity())
        openAPI.setComponents(config.openAPI.getComponents())

        classes.each {
            processApiDocAnnotation(it)
        }
        // sort controller by tag name
        openAPI.tags = openAPI.tags?.sort { it.name }

        // append server information if not yet configured by application configuration
        String url = application.config.grails.serverURL
        if (url && !openAPI.getServers()) {
            openAPI.servers([new Server(url: url)])
        }

        return openAPI
    }

    def processApiDocAnnotation(Class controllerClass) {
        log.debug("Scanning class: ${controllerClass.simpleName}")
        // get all controller grails artifacts
        def allControllerArtifacts = application.getArtefacts("Controller")
        // find controller artifact with the same controller class
        GrailsControllerClass controllerArtifact = allControllerArtifacts.find { it.clazz == controllerClass } as GrailsControllerClass
        if (!controllerArtifact) {
            log.error("No grails controller found for class ${controllerClass}")
            return
        }

        if (controllerArtifact.clazz.getAnnotation(ApiDoc) == null) {
            return
        }

        def applicationContext = application.mainContext
        def urlMappingsHolder = applicationContext.getBean("grailsUrlMappingsHolder", UrlMappingsHolder)

        if (!openAPI.paths) {
            openAPI.paths(new Paths())
        }

        Tag controllerTag = buildControllerTag(controllerArtifact)

        controllerArtifact.viewNames.each {
            String actionName = it.key

            Method method = controllerClass.methods.find { it.name == actionName }
            if (!method) {
                return
            }

            def annotation = method.getAnnotation(ApiDoc)
            if (!annotation) {
                return
            }

            Class closureClass = annotation.operation()
            OperationBuilder operationBuilder = new OperationBuilder(reader: this)

            operationBuilder.model.requestBody = buildRequestBody(actionName, controllerArtifact, urlMappingsHolder)
            operationBuilder.model.responses = buildResponses(annotation)

            Operation operation = processClosure(closureClass, operationBuilder) as Operation

            if (!operation.parameters) {
                operation.parameters = buildQueryParameters(actionName, controllerArtifact, urlMappingsHolder)
            }

            if (!operation.tags) {
                operation.addTagsItem(controllerTag.name)
            }

            if (!operation.description) {
                operation.description = annotation.description()
            }

            if (!operation.summary) {
                operation.summary = annotation.summary()
            }

            buildPathItem(operation, actionName, controllerArtifact, urlMappingsHolder)
        }
    }

    private void buildPathItem(Operation operation, String actionName, GrailsControllerClass controllerArtifact, UrlMappingsHolder urlMappingsHolder) {
        UrlMapping urlMappingOfAction = getUrlMappingOfAction(urlMappingsHolder, controllerArtifact, actionName)
        PathItem.HttpMethod httpMethod = PathItem.HttpMethod.GET
        String url
        if (urlMappingOfAction) {
            httpMethod = buildHttpMethod(urlMappingOfAction, actionName)
            url = urlMappingOfAction.urlData.urlPattern
            //Try to replace asterisk placeholders of path parameters
            if (urlMappingOfAction instanceof RegexUrlMapping) {
                urlMappingOfAction.constraints.each { def constrainedProperty ->
                    //Replace named wildcard with double * first
                    url = url.replaceFirst("\\(\\*\\*\\)", "\\(\\*\\)")
                    //Then replace optional placeholder
                    url = url.replaceFirst("\\(\\(\\*\\)\\)\\?", "\\(\\*\\)")
                    //Then replace variables

                    if (constrainedProperty.propertyName == "namespace") {
                        url = url.replaceFirst("\\(\\*\\)", urlMappingOfAction.namespace)
                    } else {
                        url = url.replaceFirst("\\(\\*\\)", '{' + ((ConstrainedProperty) constrainedProperty).propertyName + '}')
                    }
                }
            }
        } else {
            def allowedMethods = controllerArtifact.getPropertyValue("allowedMethods")
            if (allowedMethods && allowedMethods[actionName]) {
                httpMethod = PathItem.HttpMethod.valueOf(allowedMethods[actionName] as String)
            }
            def controllerName = controllerArtifact.logicalPropertyName
            UrlCreator urlCreator = urlMappingsHolder.getReverseMapping(controllerName, actionName, null, [:])
            url = urlCreator.createURL([controller: controllerName, action: actionName], "utf-8")
        }

        def pathItem = openAPI.paths[url] ?: new PathItem()
        pathItem.operation(httpMethod, operation)
        openAPI.paths.addPathItem(url, pathItem)
    }

    private Tag buildControllerTag(GrailsControllerClass grailsControllerClass) {
        Tag tag = new Tag()
        tag.name = grailsControllerClass.logicalPropertyName.capitalize()

        if (!grailsControllerClass.viewNames) {
            return tag
        }

        ApiDoc annotation = grailsControllerClass.clazz.getAnnotation(ApiDoc) as ApiDoc
        if (!annotation) {
            return tag
        }

        def tagClosure = annotation.tag()
        if (tagClosure) {
            def tagFromClosure = processClosure(tagClosure, new TagBuilder(reader: this)) as Tag

            if (!tagFromClosure.name) {
                tagFromClosure.name = tag.name
            }
            tag = tagFromClosure
        }

        openAPI.addTagsItem(tag)
        return tag
    }

    private def processClosure(Class closureClass, AnnotationBuilder builder) {
        if (closureClass) {
            Closure closure = closureClass.newInstance(openAPI, openAPI) as Closure
            closure.delegate = builder
            closure.resolveStrategy = Closure.DELEGATE_FIRST
            def result = closure()

            if (result instanceof Closure) {
                result.delegate = builder
                result.resolveStrategy = Closure.DELEGATE_FIRST
                result = result()
            }
        }

        return builder.model
    }

    private Map buildResponses(ApiDoc annotation) {
        Class responseType = annotation.responseType()

        Boolean isClosureResponseType = responseType.simpleName.contains("closure")

        if (isClosureResponseType) return null

        Map responses = [:]

        responses["200"] = new io.swagger.v3.oas.models.responses.ApiResponse(
                description: "OK",
                content: new Content(
                        "${JSON_MIME}": new MediaType(schema: buildSchema(responseType))
                )
        )

        ApiDocExampleValue exampleValue = processClosure(annotation.examples(), new ApiDocExampleValueBuilder(reader: this))
        if (!exampleValue.responses) return responses

        for (String key : exampleValue.responses.keySet()) {
            def responseExamples = exampleValue.responses[key]

            String jsonMimeType = responses[key].content.keySet().first()

            if (responseExamples instanceof List) {
                int count = 0

                responses[key].content[jsonMimeType].examples = [:]

                for (def responseExample : responseExamples) {
                    count++
                    responses[key].content[jsonMimeType].examples."${count}" = responseExample
                }
            } else {
                responses[key].content[jsonMimeType].example = exampleValue.responses[key]
            }
        }

        return responses
    }

    private RequestBody buildRequestBody(String actionName, GrailsControllerClass grailsControllerClass, UrlMappingsHolder urlMappingsHolder) {
        Class plainClass = grailsControllerClass.clazz
        def actionMethods = plainClass.methods.find { it.name == actionName && it.getAnnotation(Action) }
        def actionAnnotation = actionMethods.getAnnotation(Action)
        def commandClasses = actionAnnotation.commandObjects()
        if (commandClasses) {
            def commandClass = commandClasses[0]

            UrlMapping urlActionMapping = getUrlMappingOfAction(urlMappingsHolder, grailsControllerClass, actionName)
            PathItem.HttpMethod httpMethod = buildHttpMethod(urlActionMapping, actionName)

            if (httpMethod == PathItem.HttpMethod.GET) return null

            Boolean containsInPathProperties = listInPathProperties(commandClass).size() > 0

            Schema schema
            if (!containsInPathProperties) {
                schema = buildSchema(commandClass)
                String ref = getRef(schema)
                schema = new Schema($ref: ref)
            } else {
                schema = buildSchema(commandClass, null, true)
            }

            Boolean containsFileProperty = listClassProperties(commandClass).any({
                ApiDoc annotation = it.field?.field.getAnnotation(ApiDoc)

                return annotation && annotation.isFile()
            })

            String mime
            if (containsFileProperty) {
                mime = MULTIPART_FORM_DATA_MIME
            } else {
                mime = JSON_MIME
            }

            Content content = new Content()
            content.addMediaType(mime, new MediaType(schema: schema))

            ApiDoc annotation = actionMethods.getAnnotation(ApiDoc)
            if (annotation) {
                ApiDocExampleValue exampleValue = processClosure(annotation.examples(), new ApiDocExampleValueBuilder(reader: this))

                if (exampleValue.request) {
                    String jsonMimeType = content.keySet().first()

                    content[jsonMimeType].example = exampleValue.request
                }
            }

            return new RequestBody(content: content)
        } else {
            return null
        }
    }

    private List<Parameter> buildQueryParameters(String actionName, GrailsControllerClass grailsControllerClass, UrlMappingsHolder urlMappingsHolder) {
        Class plainClass = grailsControllerClass.clazz
        def actionMethods = plainClass.methods.find { it.name == actionName && it.getAnnotation(Action) }
        def actionAnnotation = actionMethods.getAnnotation(Action)
        def commandClasses = actionAnnotation.commandObjects()
        if (commandClasses) {
            UrlMapping urlActionMapping = getUrlMappingOfAction(urlMappingsHolder, grailsControllerClass, actionName)

            def commandClass = commandClasses[0]
            List<Parameter> parameters = []

            if (commandClass == String && urlActionMapping?.constraints) {
                for (def constraint : urlActionMapping.constraints) {
                    String paramName = constraint.propertyName

                    if (paramName == "namespace") {
                        continue
                    }

                    parameters.add(new Parameter(
                        name: paramName,
                        in: "path",
                        required: true,
                        schema: new Schema(type: "string")
                    ))
                }
            } else {
                Map<String, Schema> properties = buildClassProperties(commandClass, null)

                PathItem.HttpMethod httpMethod = buildHttpMethod(urlActionMapping, actionName)

                if (httpMethod == PathItem.HttpMethod.POST) {
                    List<MetaProperty> inPathProperties = listInPathProperties(commandClass)

                    properties = properties.findAll { inPathProperties.any({metaProperty -> metaProperty.name == it.key }) }
                }

                for (String key : properties.keySet()) {
                    Schema schema = properties[key]

                    String zin = httpMethod == PathItem.HttpMethod.GET ? "query" : "path"

                    parameters.add(new Parameter(
                            name: key,
                            in: zin,
                            example: schema.example,
                            description: schema.description,
                            required: schema.required,
                            schema: new Schema(type: schema.type,
                                    maximum: schema.maximum,
                                    minimum: schema.minimum,
                                    maxLength: schema.maxLength,
                                    minLength: schema.minLength
                            )
                    ))
                }
            }

            return parameters
        } else {
            return null
        }
    }

    private static String getRef(Schema schema) {
        return "#/components/schemas/${schema.name}"
    }

    private Map<String, Schema> buildClassProperties(Class<?> aClass, Schema parentSchema, Boolean ignoreInPathProperties = false) {
        SortedMap<String, Schema> propertiesMap = new TreeMap<>()

        List<MetaProperty> properties = listClassProperties(aClass)

        if (ignoreInPathProperties) {
            properties.removeAll(listInPathProperties(aClass))
        }

        for (MetaProperty metaProperty : properties) {
            String fieldName = metaProperty.name
            Class fieldType = metaProperty.type
            Field field = null

            if (metaProperty instanceof MetaBeanProperty) {
                field = metaProperty.field?.field
            }

            Schema schema = getSchemaFromOpenAPI(fieldType)
            if (!schema) {
                schema = buildSchema(fieldType, field?.genericType)

                ApiDoc annotation = field?.getAnnotation(ApiDoc)
                if (annotation) {
                    def comments = annotation.value()

                    if (comments) {
                        if (schema.description) {
                            schema.description = comments + " \n" + schema.description
                        } else {
                            schema.description = comments
                        }
                    } else {
                        schema.description = annotation.description() ?: ""
                    }

                    if (annotation.isFile()) {
                        schema.format = "binary"
                    }

                    BigDecimal maximum = annotation.maximum() >= 0 ? annotation.maximum() : null
                    BigDecimal minimum = annotation.minimum() >= 0 ? annotation.minimum() : null

                    if (schema.type == 'string') {
                        schema.maxLength = maximum
                        schema.minLength = minimum
                    } else {
                        schema.maximum = maximum
                        schema.minimum = minimum
                    }

                    if (annotation.required()) {
                        if (parentSchema) {
                            List<String> requiredList = parentSchema.required ?: []
                            parentSchema.required(requiredList + fieldName)
                        }

                        schema.required = [fieldName]
                    }

                    ApiDocGenericValue defaultValue = processClosure(annotation.defaultValue(), new ApiDocGenericValueBuilder(reader: this))

                    if (defaultValue.value) {
                        schema.default = defaultValue.value
                        schema.example = defaultValue.value
                    }

                    ApiDocGenericValue exampleValue = processClosure(annotation.example(), new ApiDocGenericValueBuilder(reader: this))

                    if (exampleValue.value) {
                        schema.example = exampleValue.value
                    }
                }
            }

            propertiesMap[fieldName] = schema
        }

        return propertiesMap
    }

    private Schema buildSchema(Class aClass, Type genericType = null, Boolean ignoreInPathProperties = false) {
        TypeAndFormat typeAndFormat = buildType(aClass)

        Schema schema = getSchemaFromOpenAPI(aClass)
        if (schema) {
            return schema
        }
        String name = schemaNameFromClass(aClass)
        Map args = [name       : name,
                    type       : typeAndFormat.type,
                    format     : typeAndFormat.format,
                    description: buildSchemaDescription(aClass)]
        schema = typeAndFormat.type == "array" ? new ArraySchema(args) : new Schema(args)
        if (typeAndFormat.type in ["object", "enum"]) {
            openAPI.schema(name, schema)
        }

        switch (typeAndFormat.type) {
            case "object":
                // skip java.xxx/org.grails.xxx/org.springframework.xxx package class
                String packageName = aClass.package?.name
                if (packageName?.startsWith('java.') ||
                        packageName?.startsWith('org.grails.') ||
                        packageName?.startsWith('org.springframework.')
                ) {
                    return schema
                }
                schema.properties = buildClassProperties(aClass, schema, ignoreInPathProperties)

                if (!ignoreInPathProperties) {
                    schema = getSchemaFromOpenAPI(aClass)
                }

                break
            case "array":
                // try to get array element type
                Class itemClass = aClass.componentType
                // extract item type for collections
                if (!itemClass && Collection.isAssignableFrom(aClass) && genericType instanceof ParameterizedType) {
                    itemClass = genericType.actualTypeArguments[0] as Class
                } else {
                    itemClass = itemClass ?: Object
                }
                if (itemClass && schema instanceof ArraySchema) {
                    // Build object schema if not already done, and assign reference to it
                    schema.items =  buildSchema(itemClass)
                }
                break
            case "enum":
                schema.type = "string"
                schema.setEnum(buildEnumItems(aClass))
                buildEnumDescription(aClass, schema)
                break
        }
        return schema
    }

    private static TypeAndFormat buildType(Class aClass) {
        TypeAndFormat typeAndFormat = new TypeAndFormat()
        switch (aClass) {
            case String:
            case GString:
                typeAndFormat.type = "string"
                break
            case short:
            case Short:
                typeAndFormat.type = "integer"
                break
            case int:
            case Integer:
                typeAndFormat.type = "integer"
                typeAndFormat.format = "int32"
                break
            case long:
            case Long:
                typeAndFormat.type = "integer"
                typeAndFormat.format = "int64"
                break
            case boolean:
            case Boolean:
                typeAndFormat.type = "boolean"
                break
            case double:
            case Double:
                typeAndFormat.type = "number"
                typeAndFormat.format = "double"
                break
            case float:
            case Float:
                typeAndFormat.type = "number"
                typeAndFormat.format = "float"
                break
            case Number:
                typeAndFormat.type = "number"
                break
            case Collection:
            case { aClass.isArray() }:
                typeAndFormat.type = "array"
                break
            case Enum:
                typeAndFormat.type = "enum"
                break
            case Date:
                typeAndFormat.type = "string"
                typeAndFormat.format = "date-time"
                break
            default:
                typeAndFormat.type = "object"
                break
        }
        return typeAndFormat
    }

    private static String buildSchemaDescription(Class aClass) {
        ApiDoc apiDocAnnotation = aClass.getAnnotation(ApiDoc) as ApiDoc

        def description = apiDocAnnotation?.description()
        if (description) {
            return description
        }

        return apiDocAnnotation?.value() ?: ""
    }

    private Schema getSchemaFromOpenAPI(Class aClass, boolean clone = true) {
        String name = schemaNameFromClass(aClass)
        Schema schema = openAPI.components?.getSchemas()?.get(name)

        if (schema && clone) {
            schema = cloneSchema(schema)
            schema.$ref = getRef(schema)
            schema.properties = [:]
        }

        return schema
    }

    private static List buildEnumItems(Class enumClass) {
        enumClass.values()?.collect {
            if (it.hasProperty("id")) {
                it.id
            } else {
                it.name()
            }
        }
    }

    private static void buildEnumDescription(Class aClass, Schema schema) {
        StringBuilder builder = new StringBuilder(schema.description)
        if (schema?.description?.trim()) {
            char endChar = schema.description.charAt(schema.description.length() - 1)
            if (Character.isAlphabetic(endChar) || Character.isIdeographic(endChar)) {
                builder.append(". ")
            }
        }

        builder.append("Enum of: ")
        aClass.values()?.eachWithIndex { enumValue, idx ->
            String idPart = ""
            if (enumValue.hasProperty("id")) {
                idPart = "(${enumValue.id})"
            }
            // append ", " if idx > 0
            if (idx > 0) {
                builder.append(", ")
            }
            builder.append("${enumValue.name()}${idPart}")
        }

        schema.description = builder.toString()
    }

    private static Schema cloneSchema(Schema schema) {
        Schema clone = new Schema()
        schema.metaClass.properties.each { prop ->
            // only assign writable property
            def setMethod = Schema.methods.find {
                it.name == "set${prop.name.capitalize()}"
            }
            if (setMethod) {
                clone[prop.name] = schema[prop.name]
            }
        }

        return clone
    }

    private static String schemaNameFromClass(Class aClass) {
        return aClass.canonicalName
    }

    private UrlMapping getUrlMappingOfAction(UrlMappingsHolder urlMappingsHolder, controllerArtifact, String actionName) {
        UrlMapping urlMappingOfAction = urlMappingsHolder.urlMappings.find {
            if (it.actionName instanceof Map) {
                it.controllerName == controllerArtifact.logicalPropertyName && it.actionName.any { it.value == actionName }
            } else {
                it.controllerName == controllerArtifact.logicalPropertyName && it.actionName == actionName
            }
        }

        urlMappingOfAction.namespace = controllerArtifact.namespace

        return urlMappingOfAction
    }

    private PathItem.HttpMethod buildHttpMethod(UrlMapping urlMappingOfAction, String actionName) {
        String httpMethodName

        if (!urlMappingOfAction) return null

        if (urlMappingOfAction.actionName instanceof Map) {
            httpMethodName = urlMappingOfAction.actionName.find { it.value == actionName }.key
        } else {
            httpMethodName = urlMappingOfAction.httpMethod.toUpperCase()
        }

        if (httpMethodName == "*" || !PathItem.HttpMethod.values()
                .collect { it.name() }.contains(httpMethodName)) {
            httpMethodName = "GET"
        }

        return PathItem.HttpMethod.valueOf(httpMethodName)
    }

    private static List<MetaProperty> listInPathProperties(Class aClass) {
        List<MetaProperty> inPathProperties = listClassProperties(aClass).findAll( {
            ApiDoc annotation = it.field?.field.getAnnotation(ApiDoc)

            return annotation && annotation.inPath()
        })

        return inPathProperties
    }

    private static List<MetaProperty> listClassProperties(Class aClass) {
        List<MetaProperty> properties = []

        for (MetaProperty metaProperty : aClass.metaClass.properties) {
            if (!(metaProperty.modifiers & Modifier.PUBLIC)) {
                continue
            }

            switch (metaProperty.name) {
                case ~/.*(grails_|\$).*/:
                case "metaClass":
                case "properties":
                case "class":
                case "clazz":
                case "constraints":
                case "constraintsMap":
                case "mapping":
                case "log":
                case "logger":
                case "instanceControllersDomainBindingApi":
                case "instanceConvertersApi":
                case "errors":
                    // case { DomainClass.isAssignableFrom(aClass) && fieldName == "version" }:
                    // case { DomainClass.isAssignableFrom(aClass) && fieldName == "transients" }:
                    // case { DomainClass.isAssignableFrom(aClass) && fieldName == "all" }:
                    // case { DomainClass.isAssignableFrom(aClass) && fieldName == "attached" }:
                    // case { DomainClass.isAssignableFrom(aClass) && fieldName == "belongsTo" }:
                    // case { DomainClass.isAssignableFrom(aClass) && fieldName == "constrainedProperties" }:
                    // case { DomainClass.isAssignableFrom(aClass) && fieldName == "dirty" }:
                    // case { DomainClass.isAssignableFrom(aClass) && fieldName == "dirtyPropertyNames" }:
                    // case { DomainClass.isAssignableFrom(aClass) && fieldName == "gormDynamicFinders" }:
                    // case { DomainClass.isAssignableFrom(aClass) && fieldName == "gormPersistentEntity" }:
                    continue
            }

            properties.add(metaProperty)
        }

        return properties
    }

    private static class TypeAndFormat {
        String type = "object"
        String format = null
    }

}
