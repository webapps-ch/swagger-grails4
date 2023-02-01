package swagger.grails4.util

/**
 * Helper class to compare Swagger definitions against each other
 */
class SwaggerComparator {

    /**
     * Compare two parsed Swagger/OpenApi specs against each other.
     * Example:
     *   when: "Comparing OpenApi spec to provided specification"
     *     Map diff = evaluateDiff(reference, generated, '')
     *   then: "There must be no difference"
     *     !diff
     * @param val1 reference specification
     * @param val2 specification to check on deviations
     * @param keyName, must be empty '' for root node
     * @return the deviations including the key names
     */
    static Map<String, String> evaluateDiff(val1, val2, String keyName) {
        Map err = [:]
        if (val1.class != val2.class) {
            err.put("${keyName}", "Has different class: ${val1.class} vs ${val2.class}")
            return err
        }

        if (val1 instanceof Map) {
            if (!(val2 instanceof Map)) {
                err.put("${keyName}", "Missing map")
            } else if (val1.size() != val2.size()) {
                err.put("${keyName}", "Map size is not equal: ${val1.size()} vs ${val2.size()}")
                List<String> left = val1.keySet().asList()
                List<String> right = val2.keySet().asList()
                if (val1.size() < val2.size()) {
                    err.put("${keyName}-info", "Missing keys: ${right - left}")
                } else {
                    err.put("${keyName}-info", "Removed keys: ${left - right}")
                }
            } else {
                val1.keySet().each {key1 ->
                    if (!val2.containsKey(key1)) {
                        err.put("${keyName}", "Missing key ${key1}")
                    } else {
                        err.putAll(evaluateDiff(val1.get(key1), val2.get(key1), "${keyName}${keyName ? '.' : ''}${key1}"))
                    }
                }
            }
        } else if (val1 instanceof Collection) {
            if (!(val2 instanceof Collection)) {
                err.put("${keyName}", "Missing collection")
            } else if (val1.size() != val2.size()) {
                err.put("${keyName}", "Collection has different size: ${val1.size()} vs ${val2.size()}")
            } else {
                val1.eachWithIndex { def entry1, int i ->
                    err.putAll(evaluateDiff(entry1, val2[i], "${keyName}.[${i}]"))
                }
            }
        } else if (val1 != val2) {
            err.put("${keyName}", "Values are different: '${val1}' vs '${val2}'")
        }

        return err
    }
}


