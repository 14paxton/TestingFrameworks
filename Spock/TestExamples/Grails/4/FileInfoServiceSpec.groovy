package tbservice

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification
import tbservice.dto.org.FileInfo
import tbservice.enums.OrgUploadStatus

class FileInfoServiceSpec extends Specification implements DataTest, ServiceUnitTest<FileInfoService> {

    def setup() {
    }

    def cleanup() {
    }

    void "saveFileInfo should return false if incorrect parameters supplied on fileInfo"() {
        setup:
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "", '', 'filename.csv', OrgUploadStatus.UPLOADED.value, 21, [])

        when:
        def result = service.saveFileInfo(fileInfo)

        then:
        !result
    }

    void "updateOrgUploadStatus should return null fileInfo after unsuccessful save"() {
        setup:
        service.metaClass.saveFileInfo = { FileInfo fileInfo -> return false } //unsuccessful save
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.UPLOADED.value, 21, [])

        when:
        fileInfo = service.updateOrgUploadStatus(fileInfo, OrgUploadStatus.PRE_PROCESSING)

        then:
        !fileInfo
    }

    void "updateOrgUploadStatus should update the fileInfo to the new status and return updated fileInfo after successful save"() {
        setup:
        service.metaClass.saveFileInfo = { FileInfo fileInfo -> return true } //successful save
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.UPLOADED.value, 21, [])

        when:
        fileInfo = service.updateOrgUploadStatus(fileInfo, OrgUploadStatus.PRE_PROCESSING)

        then:
        fileInfo
        fileInfo.status == OrgUploadStatus.PRE_PROCESSING.value
    }

}
