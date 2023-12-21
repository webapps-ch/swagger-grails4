package swagger.grails4.samples

import grails.validation.Validateable
import swagger.grails4.openapi.ApiDoc
import swagger.grails4.samples.MyEnum

@ApiDoc("The command contains User properties")
class UserCreateResponseKeyDTO {

    String key

    public UserCreateResponseKeyDTO() {
    }
}
