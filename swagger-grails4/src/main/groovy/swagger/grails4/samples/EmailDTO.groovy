package swagger.grails4.samples

import grails.validation.Validateable
import swagger.grails4.openapi.ApiDoc
import swagger.grails4.samples.MyEnum

@ApiDoc("The command contains Secret properties")
class EmailDTO {

    @ApiDoc(description = "Email do usuário")
    String email

    @ApiDoc(description = "Senha do usuário")
    String senha
}
