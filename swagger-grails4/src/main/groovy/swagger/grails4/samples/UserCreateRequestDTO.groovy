package swagger.grails4.samples

import grails.validation.Validateable
import io.swagger.v3.oas.models.Operation
import swagger.grails4.openapi.ApiDoc
import swagger.grails4.openapi.builder.AnnotationBuilder
import swagger.grails4.samples.MyEnum
import swagger.grails4.samples.UserCreateSecretDTO

@ApiDoc(description = "DTO para criação de usuário")
class UserCreateRequestDTO {

    @ApiDoc(required = true, description = "User name")
    String username

    @ApiDoc(maximum = 15, minimum = 5, required = true, description = "User password")
    String password

    @ApiDoc(description = "User test", defaultValue = { value MyEnum.PAYMENT_RECEIVED })
    MyEnum paymentType

    @ApiDoc(description = "User test", defaultValue = { value true })
    Boolean canCreateNew

    @ApiDoc(description = "User secret")
    UserCreateSecretDTO secret

    List<EmailDTO> emails

    @ApiDoc(description = "Arquivo", isFile = true)
    String file

}