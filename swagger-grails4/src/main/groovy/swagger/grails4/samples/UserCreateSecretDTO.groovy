package swagger.grails4.samples

import grails.validation.Validateable
import swagger.grails4.openapi.ApiDoc
import swagger.grails4.samples.MyEnum

@ApiDoc("The command contains Secret properties")
class UserCreateSecretDTO {

    @ApiDoc(required = true, description = "its")
    String its

    @ApiDoc(maximum = 15, minimum = 5, required = true, description = "seecreet")
    String secret
}
