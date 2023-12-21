package main.swagger.grails4.openapi.builder

import main.swagger.grails4.openapi.ApiDocGenericValue
import swagger.grails4.openapi.builder.AnnotationBuilder

class ApiDocGenericValueBuilder implements AnnotationBuilder<ApiDocGenericValue> {

    ApiDocGenericValue model = new ApiDocGenericValue()
    /**
     * needed by AnnotationBuilder trait
     */
    @SuppressWarnings("unused")
    static Class openApiAnnotationClass = ApiDocGenericValue

    ApiDocGenericValueBuilder(){
        initPrimitiveElements()
    }

    def value(Object value) {
        model.value = value
    }
}
