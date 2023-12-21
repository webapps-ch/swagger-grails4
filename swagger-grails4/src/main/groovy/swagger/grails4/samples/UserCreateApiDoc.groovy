package main.swagger.grails4.samples

import swagger.grails4.samples.UserCreateRequestDTO
import swagger.grails4.samples.UserCreateResponseDTO
import swagger.grails4.samples.UserCreateResponseKeyDTO

class UserCreateApiDoc {

    public static def login = {
        request new UserCreateRequestDTO(
                username: "João",
                password: "00000"
        )

        responses "200": new UserCreateResponseDTO(
                username: "Novo usuário",
                password: "123456",
                keys: [new UserCreateResponseKeyDTO(
                        key: "1234"
                )]
        )
    }

}
