package tbservice

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification
import tbservice.dto.org.FileInfo
import tbservice.dto.org.ProcessedUserRowDTO
import tbservice.enums.OrgUploadStatus

class ElasticSearchServiceSpec extends Specification implements DataTest, ServiceUnitTest<ElasticSearchService> {

    def esConfig = new ElasticSearchConfig("esEndpoint", "esIndex")
    FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload-publish', new Date().getTime(), "55", 'gfgfdg', 'filename.csv', OrgUploadStatus.PUBLISHING.value, 21, [])

    def setup() {}

    def cleanup() {}

    void "searchAllInIndex should return null if incorrect parameters are supplied"() {
        when:
        def results1 = service.searchAllInIndex(new ElasticSearchConfig(null, "index"), fileInfo, ['email'] as String[])
        def results2 = service.searchAllInIndex(new ElasticSearchConfig("endpoint", null), fileInfo, ['email'] as String[])
        def results3 = service.searchAllInIndex(esConfig, null, ['email'] as String[])

        then:
        !results1
        !results2
        !results3
    }

    void "searchInIndex should return null if incorrect parameters are supplied"() {
        when:
        def results1 = service.searchInIndex(new ElasticSearchConfig(null, "index"), fileInfo, 0, 50)
        def results2 = service.searchInIndex(new ElasticSearchConfig("endpoint", null), fileInfo, 0, 50)
        def results3 = service.searchInIndex(esConfig, null, 0, 0)
        def results4 = service.searchInIndex(esConfig, fileInfo, 0, 0)

        then:
        !results1
        !results2
        !results3
        !results4
    }

    void "bulkUpdateRows should return false if incorrect parameters are supplied"() {
        when:
        def results1 = service.bulkUpdateRows(new ElasticSearchConfig(null, "index"), [new ProcessedUserRowDTO()], OrgUploadStatus.PROCESSING, false)
        def results2 = service.bulkUpdateRows(new ElasticSearchConfig("endpoint", null), [new ProcessedUserRowDTO()], OrgUploadStatus.PROCESSING, false)
        def results3 = service.bulkUpdateRows(esConfig, [], OrgUploadStatus.PROCESSING, false)
        def results4 = service.bulkUpdateRows(esConfig, [new ProcessedUserRowDTO()], null, false)
        def results5 = service.bulkUpdateRows(esConfig, [new ProcessedUserRowDTO()], OrgUploadStatus.UPLOADED, false)

        then:
        !results1
        !results2
        !results3
        !results4
        !results5
    }

}
