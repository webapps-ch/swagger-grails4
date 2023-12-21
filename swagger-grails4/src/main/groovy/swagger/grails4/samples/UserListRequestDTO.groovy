package swagger.grails4.samples

import grails.validation.Validateable
import io.swagger.v3.oas.models.Operation
import swagger.grails4.openapi.ApiDoc
import swagger.grails4.openapi.builder.AnnotationBuilder
import swagger.grails4.samples.MyEnum
import swagger.grails4.samples.UserCreateSecretDTO

@ApiDoc(description = "DTO para listar usuários")
class UserListRequestDTO {

    @ApiDoc(description = "User name", required = true, maximum = 15)
    String name

    @ApiDoc(description = "max per page", maximum = 100, minimum = 1)
    int max

    @ApiDoc(description = "offset", minimum = 0)
    int offset
}
