package tbservice

import grails.testing.gorm.DataTest
import spock.lang.Specification

class ElasticSearchConfigSpec extends Specification implements DataTest {

    void "a client should be made for a host"() {
        setup:
        def esEndpoint = "endpoint"
        def esIndex = "index"

        when:
        def esConfig = new ElasticSearchConfig(esEndpoint, esIndex)

        then:
        esConfig
    }
}
