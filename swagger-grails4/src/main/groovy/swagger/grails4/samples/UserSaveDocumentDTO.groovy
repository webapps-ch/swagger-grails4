package main.swagger.grails4.samples

import swagger.grails4.openapi.ApiDoc

@ApiDoc(description = "DTO para salvar documentos")
class UserSaveDocumentDTO {

    @ApiDoc(required = true, description = "Id da cobrança", inPath = true)
    String paymentId

    @ApiDoc(required = true, description = "Id do documento", inPath = true)
    String documentId

    @ApiDoc(description = "Nome do documento")
    String documentName

    @ApiDoc(description = "Tipo do documento")
    String documentType

}
