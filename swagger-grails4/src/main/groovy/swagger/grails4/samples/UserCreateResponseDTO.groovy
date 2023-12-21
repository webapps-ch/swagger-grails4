package swagger.grails4.samples

import grails.validation.Validateable
import swagger.grails4.openapi.ApiDoc
import swagger.grails4.samples.MyEnum
import swagger.grails4.samples.UserCreateResponseKeyDTO

@ApiDoc("The command contains User properties")
class UserCreateResponseDTO {

    String username

    String password

    List<UserCreateResponseKeyDTO> keys

    public UserCreateResponseDTO() {
    }
}
