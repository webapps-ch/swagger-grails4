package swagger.grails4.openapi

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

@Retention(RetentionPolicy.RUNTIME)
@interface ApiDoc {

    String value() default ""

    Class openAPIDefinition() default {}

    Class operation() default {}

    Class responseType() default {}

    Class defaultValue() default {}

    Class examples() default {}

    Class example() default {}

    Class tag() default {}

    Class securityScheme() default {}

    int maximum() default -1
    int minimum() default -1
    boolean required() default false
    boolean isFile() default false
    boolean inPath() default false
    String description() default ""
    String summary() default ""
}
