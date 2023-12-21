package main.swagger.grails4.openapi.builder

import main.swagger.grails4.openapi.ApiDocExampleValue
import swagger.grails4.openapi.builder.AnnotationBuilder

class ApiDocExampleValueBuilder implements AnnotationBuilder<ApiDocExampleValue> {

    ApiDocExampleValue model = new ApiDocExampleValue()
    /**
     * needed by AnnotationBuilder trait
     */
    @SuppressWarnings("unused")
    static Class openApiAnnotationClass = ApiDocExampleValue

    ApiDocExampleValueBuilder(){
        initPrimitiveElements()
    }

    def request(Object value) {
        model.request = value
    }

    def responses(Object value) {
        model.responses = value
    }
}
