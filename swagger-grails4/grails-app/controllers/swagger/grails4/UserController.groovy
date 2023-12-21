package swagger.grails4

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.Explode
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.enums.ParameterStyle
import io.swagger.v3.oas.annotations.extensions.Extension
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import main.swagger.grails4.samples.UserCreateApiDoc
import main.swagger.grails4.samples.UserSaveDocumentDTO
import swagger.grails4.openapi.ApiDocExampleDTO
import swagger.grails4.samples.MyEnum
import swagger.grails4.samples.UserCreateResponseDTO
import swagger.grails4.samples.UserCreateRequestDTO
import swagger.grails4.openapi.ApiDoc
import swagger.grails4.samples.UserCreateResponseKeyDTO
import swagger.grails4.samples.UserListRequestDTO

import java.lang.annotation.Annotation


@ApiDoc(tag = {
    description "User API???x"
})
class UserController {

    static namespace = "v3"
//
//    @ApiDoc(description = "Descrição", summary = "Sumário", responseType = UserCreateResponseDTO, examples = { UserCreateApiDoc.login })
//    public Map login(UserCreateRequestDTO command) {
//        return new UserCreateResponseDTO(username: "Novo usuário", password: "123456", keys: new UserCreateResponseKeyDTO(key: "1234")).properties
//    }
//
//    @ApiDoc(description = "Listar clientes", summary = "Lista")
//    public Map list(UserListRequestDTO command) {
//        return new UserCreateResponseDTO(username: "jose", password: "12345").properties
//    }
//
//    @ApiDoc(description = "Recuperar status do cliente")
//    public Map getStatus(String id) {
//        return [status: "OK"]
//    }
//
//    @ApiDoc(description = "Recuperar Doc")
//    public Map getDocument(String paymentId, String documentId) {
//        return [status: "OK"]
//    }

    @ApiDoc(description = "Salvar ou atualizar Doc")
    public Map saveDocument(UserSaveDocumentDTO userSaveDocumentDTO) {
        return [status: "OK"]
    }

}
