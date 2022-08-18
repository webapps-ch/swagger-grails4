package swagger.grails4

/**
 * Url mapping config of grails
 *
 * @author bo.yang <bo.yang@telecwin.com>
 */
class UrlMappings {

    static mappings = {
        "/api/doc/$ns?"(controller: "openApi", action: "document")
        "/api/$ns?"(controller: "openApi", action: "index")
    }
}
