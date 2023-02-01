package swagger.grails4.openapi.builder

import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.tags.Tag

/**
 * Operation model builder.
 *
 * Delegate object is OpenAPI.
 *
 * @see io.swagger.v3.oas.annotations.Operation
 * @author bo.yang <bo.yang@telecwin.com>
 */
class OperationBuilder implements AnnotationBuilder<Operation> {

    Operation model = new Operation()
    /**
     * needed by AnnotationBuilder trait
     */
    @SuppressWarnings("unused")
    static Class openApiAnnotationClass = io.swagger.v3.oas.annotations.Operation

    OperationBuilder(){
        initPrimitiveElements()
    }

    /**
     * The "parameters" member of @ApiDoc.
     * @param parameterClosures closure of parameters, delegate to ParameterBuilder.
     * @return void
     */
    def parameters(List<Closure> parameterClosures) {
        if (!model.parameters) {
            model.parameters = []
        }
        parameterClosures.each { closure ->
            ParameterBuilder builder = new ParameterBuilder(reader: reader)
            model.parameters << evaluateClosure(closure, builder)
        }
    }

    def tags(List<Closure> tagClosures) {
        if (!model.tags) {
            model.tags = []
        }
        tagClosures.each { closure ->
            TagBuilder builder = new TagBuilder(reader: reader)
            Tag t = evaluateClosure(closure, builder)
            model.tags << t.name
        }
    }

    def security(Map<String, List<String>> securityMap) {
        if (!model.security) {
            model.security = []
        }

        securityMap.each { name, entries ->
            def secReq = new SecurityRequirement()
            secReq.addList(name, entries)
            model.security << secReq
        }
    }

    def requestBody(Closure requestBodyClosure) {
        if (!requestBodyClosure) {
            return
        }

        if (!model.requestBody) {
            RequestBody body = evaluateClosure(requestBodyClosure, new RequestBodyBuilder(reader: reader))
            if (body) {
                model.requestBody = body
            }
        }
    }

    def responses(Map<String, Closure> responsesClosures) {
        if (!model.responses) {
            model.responses = []
        }
        responsesClosures.each { code, closure ->
            ResponseBuilder builder = new ResponseBuilder(reader: reader)
            def resp = evaluateClosure(closure, builder)
            model.responses.put(code, resp)
        }
    }
}
