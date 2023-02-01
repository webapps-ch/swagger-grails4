package swagger.grails4.openapi.builder

import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.parameters.RequestBody

class RequestBodyBuilder implements AnnotationBuilder<RequestBody> {

    RequestBody model = new RequestBody()

    /**
     * needed by AnnotationBuilder trait
     */
    @SuppressWarnings("unused")
    static Class openApiAnnotationClass = io.swagger.v3.oas.annotations.parameters.RequestBody

    RequestBodyBuilder(){
        initPrimitiveElements()
    }

    /**
     * Build Content object like "content 'application/json': {...} "
     * @param closure content config closure, delegate to ContentBuilder
     */
    def content(Map<String, Closure> closureMap) {
        if (!model.content) {
            model.content = new Content()
        }
        closureMap.each { mime, closure ->
            MediaTypeBuilder mediaTypeBuilder = new MediaTypeBuilder(reader: reader)
            model.content.addMediaType(mime, evaluateClosure(closure, mediaTypeBuilder))
        }
    }
}
