package tbservice

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import kong.unirest.json.JSONObject
import spock.lang.Specification
import tbservice.dto.org.FileInfo
import tbservice.enums.OrgUploadStatus

class OrgUploadKickoffStepServiceSpec extends Specification implements DataTest, ServiceUnitTest<OrgUploadKickoffStepService> {

    def esConfig = new ElasticSearchConfig("endpoint", "index")

    def setup() {
    }

    def cleanup() {
    }

    def setupSpec() {
        mockDomain User
        mockDomain UserRelationship
    }

    void "kickoffMainProcessing should return false if empty fileInfo is passed"() {
        setup:
        def jsonMessage = [fileInfo: [:]]

        when:
        def result = service.kickoffMainProcessing(jsonMessage)

        then:
        !result
    }

    void "kickoffMainProcessing should return false if org upload fails"() {
        setup:
        service.fileInfoService = new FileInfoService()
        service.fileInfoService.metaClass.getFileInfo = { String id, String clientSeupId ->
            FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.UPLOADED.value, 21, [])
            return fileInfo
        }
        service.metaClass.processCurrentStep = { FileInfo fileInfo, ElasticSearchConfig esConfig ->
            fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.FAILED.value, 21, [])
            return fileInfo //returning file info with failed status
        }
        def jsonMessage = new JSONObject([fileInfo: [esEndpoint: 'endpoint', esIndex: 'index', id: 'dfgjkle4', clientSetupId: '55', fileRowCount: 0, missingFields: [''], processStartTime: 0]])

        when:
        def result = service.kickoffMainProcessing(jsonMessage)

        then:
        !result
    }

    void "kickoffMainProcessing should return true if org upload becomes pre-verified"() {
        setup:
        service.fileInfoService = new FileInfoService()
        service.fileInfoService.metaClass.getFileInfo = { String id, String clientSeupId ->
            FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.UPLOADED.value, 21, [])
            return fileInfo
        }
        service.metaClass.processCurrentStep = { FileInfo fileInfo, ElasticSearchConfig esConfig ->
            fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.PRE_VERIFIED.value, 21, [])
            return fileInfo //returning file info with draft status
        }
        def jsonMessage = new JSONObject([fileInfo: [esEndpoint: 'endpoint', esIndex: 'index', id: 'dfgjkle4', clientSetupId: '55', fileRowCount: 0, missingFields: [''], processStartTime: 0]])

        when:
        def result = service.kickoffMainProcessing(jsonMessage)

        then:
        result
    }

    void "processCurrentStep should bump file info status to FAILED when provided bad value file info status"() {
        setup:
        service.orgUploadProcessService = new OrgUploadProcessService()
        service.fileInfoService = new FileInfoService()
        service.orgUploadProcessService.metaClass.preProcessOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.orgUploadProcessService.metaClass.processOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.orgUploadProcessService.metaClass.postProcessOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.orgUploadProcessService.metaClass.verifyOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.fileInfoService.metaClass.updateOrgUploadStatus = { FileInfo fileInfo, OrgUploadStatus nextStep ->
            fileInfo.status = nextStep.value
            return fileInfo
        }

        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', 'bad status value', 21, [])

        when:
        FileInfo processedFileInfo = service.processCurrentStep(fileInfo, esConfig)

        then:
        processedFileInfo
        processedFileInfo.status == OrgUploadStatus.FAILED.value
    }

    void "processCurrentStep should bump file info status to PRE_PROCESSING when provided UPLOADED file info status and step is successful"() {
        setup:
        service.orgUploadProcessService = new OrgUploadProcessService()
        service.fileInfoService = new FileInfoService()
        service.orgUploadProcessService.metaClass.preProcessOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.orgUploadProcessService.metaClass.processOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.orgUploadProcessService.metaClass.postProcessOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.orgUploadProcessService.metaClass.verifyOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.fileInfoService.metaClass.updateOrgUploadStatus = { FileInfo fileInfo, OrgUploadStatus nextStep ->
            fileInfo.status = nextStep.value
            return fileInfo
        }

        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.UPLOADED.value, 21, [])

        when:
        FileInfo processedFileInfo = service.processCurrentStep(fileInfo, esConfig)

        then:
        processedFileInfo
        processedFileInfo.status == OrgUploadStatus.PRE_PROCESSING.value
    }

    void "processCurrentStep should bump file info status to FAILED when preProcessing is unsuccessful"() {
        setup:
        service.orgUploadProcessService = new OrgUploadProcessService()
        service.fileInfoService = new FileInfoService()
        service.orgUploadProcessService.metaClass.preProcessOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return false }
        service.orgUploadProcessService.metaClass.processOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.orgUploadProcessService.metaClass.postProcessOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.orgUploadProcessService.metaClass.verifyOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.fileInfoService.metaClass.updateOrgUploadStatus = { FileInfo fileInfo, OrgUploadStatus nextStep ->
            fileInfo.status = nextStep.value
            return fileInfo
        }

        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.PRE_PROCESSING.value, 21, [])

        when:
        FileInfo processedFileInfo = service.processCurrentStep(fileInfo, esConfig)

        then:
        processedFileInfo
        processedFileInfo.status == OrgUploadStatus.FAILED.value
    }

    void "processCurrentStep should bump file info status to PROCESSING when provided PRE_PROCESSING file info status and preProcessing is successful"() {
        setup:
        service.orgUploadProcessService = new OrgUploadProcessService()
        service.fileInfoService = new FileInfoService()
        service.orgUploadProcessService.metaClass.preProcessOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.orgUploadProcessService.metaClass.processOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.orgUploadProcessService.metaClass.postProcessOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.orgUploadProcessService.metaClass.verifyOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.fileInfoService.metaClass.updateOrgUploadStatus = { FileInfo fileInfo, OrgUploadStatus nextStep ->
            fileInfo.status = nextStep.value
            return fileInfo
        }

        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.PRE_PROCESSING.value, 21, [])

        when:
        FileInfo processedFileInfo = service.processCurrentStep(fileInfo, esConfig)

        then:
        processedFileInfo
        processedFileInfo.status == OrgUploadStatus.PROCESSING.value
    }

    void "processCurrentStep should bump file info status to FAILED when processing is unsuccessful"() {
        setup:
        service.orgUploadProcessService = new OrgUploadProcessService()
        service.fileInfoService = new FileInfoService()
        service.orgUploadProcessService.metaClass.preProcessOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.orgUploadProcessService.metaClass.processOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return false }
        service.orgUploadProcessService.metaClass.postProcessOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.orgUploadProcessService.metaClass.verifyOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.fileInfoService.metaClass.updateOrgUploadStatus = { FileInfo fileInfo, OrgUploadStatus nextStep ->
            fileInfo.status = nextStep.value
            return fileInfo
        }

        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.PROCESSING.value, 21, [])

        when:
        FileInfo processedFileInfo = service.processCurrentStep(fileInfo, esConfig)

        then:
        processedFileInfo
        processedFileInfo.status == OrgUploadStatus.FAILED.value
    }

    void "processCurrentStep should bump file info status to POST_PROCESSING when provided PROCESSING file info status and processing is successful"() {
        setup:
        service.orgUploadProcessService = new OrgUploadProcessService()
        service.fileInfoService = new FileInfoService()
        service.orgUploadProcessService.metaClass.preProcessOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.orgUploadProcessService.metaClass.processOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.orgUploadProcessService.metaClass.postProcessOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.orgUploadProcessService.metaClass.verifyOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.fileInfoService.metaClass.updateOrgUploadStatus = { FileInfo fileInfo, OrgUploadStatus nextStep ->
            fileInfo.status = nextStep.value
            return fileInfo
        }

        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.PROCESSING.value, 21, [])

        when:
        FileInfo processedFileInfo = service.processCurrentStep(fileInfo, esConfig)

        then:
        processedFileInfo
        processedFileInfo.status == OrgUploadStatus.POST_PROCESSING.value
    }

    void "processCurrentStep should bump file info status to FAILED when postProcessing is unsuccessful"() {
        setup:
        service.orgUploadProcessService = new OrgUploadProcessService()
        service.fileInfoService = new FileInfoService()
        service.orgUploadProcessService.metaClass.preProcessOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.orgUploadProcessService.metaClass.processOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.orgUploadProcessService.metaClass.postProcessOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return false }
        service.orgUploadProcessService.metaClass.verifyOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.fileInfoService.metaClass.updateOrgUploadStatus = { FileInfo fileInfo, OrgUploadStatus nextStep ->
            fileInfo.status = nextStep.value
            return fileInfo
        }

        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.POST_PROCESSING.value, 21, [])

        when:
        FileInfo processedFileInfo = service.processCurrentStep(fileInfo, esConfig)

        then:
        processedFileInfo
        processedFileInfo.status == OrgUploadStatus.FAILED.value
    }

    void "processCurrentStep should bump file info status to VERIFYING when provided POST_PROCESSING file info status and postProcessing is successful"() {
        setup:
        service.orgUploadProcessService = new OrgUploadProcessService()
        service.fileInfoService = new FileInfoService()
        service.orgUploadProcessService.metaClass.preProcessOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.orgUploadProcessService.metaClass.processOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.orgUploadProcessService.metaClass.postProcessOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.orgUploadProcessService.metaClass.verifyOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.fileInfoService.metaClass.updateOrgUploadStatus = { FileInfo fileInfo, OrgUploadStatus nextStep ->
            fileInfo.status = nextStep.value
            return fileInfo
        }

        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.POST_PROCESSING.value, 21, [])
        fileInfo.processStartTime = new Date().getTime() - 1000

        when:
        FileInfo processedFileInfo = service.processCurrentStep(fileInfo, esConfig)

        then:
        processedFileInfo
        processedFileInfo.status == OrgUploadStatus.VERIFYING.value
    }

    void "processCurrentStep should bump file info status to FAILED when verifying is unsuccessful"() {
        setup:
        service.orgUploadProcessService = new OrgUploadProcessService()
        service.fileInfoService = new FileInfoService()
        service.orgUploadProcessService.metaClass.preProcessOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.orgUploadProcessService.metaClass.processOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.orgUploadProcessService.metaClass.postProcessOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.orgUploadProcessService.metaClass.verifyOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return false }
        service.fileInfoService.metaClass.updateOrgUploadStatus = { FileInfo fileInfo, OrgUploadStatus nextStep ->
            fileInfo.status = nextStep.value
            return fileInfo
        }

        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.VERIFYING.value, 21, [])

        when:
        FileInfo processedFileInfo = service.processCurrentStep(fileInfo, esConfig)

        then:
        processedFileInfo
        processedFileInfo.status == OrgUploadStatus.FAILED.value
    }

    void "processCurrentStep should bump file info status to PRE_VERIFIED when provided VERIFyiNG file info status and VERIFYING is successful"() {
        setup:
        service.orgUploadProcessService = new OrgUploadProcessService()
        service.fileInfoService = new FileInfoService()
        service.orgUploadProcessService.metaClass.preProcessOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.orgUploadProcessService.metaClass.processOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.orgUploadProcessService.metaClass.postProcessOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.orgUploadProcessService.metaClass.verifyOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig -> return true }
        service.fileInfoService.metaClass.updateOrgUploadStatus = { FileInfo fileInfo, OrgUploadStatus nextStep ->
            fileInfo.status = nextStep.value
            return fileInfo
        }

        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.VERIFYING.value, 21, [])
        fileInfo.processStartTime = new Date().getTime() - 1000

        when:
        FileInfo processedFileInfo = service.processCurrentStep(fileInfo, esConfig)

        then:
        processedFileInfo
        processedFileInfo.status == OrgUploadStatus.PRE_VERIFIED.value
    }

    void "kickOffVerifyStep should return false if empty fileInfo is passed"() {
        setup:
        def jsonMessage = [fileInfo: [:]]

        when:
        def result = service.kickoffVerifyStep(jsonMessage)

        then:
        !result
    }

    void "kickOffVerifyStep should return false if verify step fails"() {
        setup:
        service.fileInfoService = new FileInfoService()
        service.fileInfoService.metaClass.getFileInfo = { String id, String clientSeupId ->
            FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.UPLOADED.value, 21, [])
            return fileInfo
        }
        service.metaClass.processVerificationStep = { FileInfo fileInfo, ElasticSearchConfig esConfig ->
            fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.FAILED.value, 21, [])
            return fileInfo //returning file info with failed status
        }
        def jsonMessage = new JSONObject([fileInfo: [esEndpoint: 'endpoint', esIndex: 'index', id: 'dfgjkle4', clientSetupId: '55', fileRowCount: 0, missingFields: [''], processStartTime: 0]])

        when:
        def result = service.kickoffVerifyStep(jsonMessage)

        then:
        !result
    }

    void "kickOffVerifyStep should return true if verify step bumps file status to draft"() {
        setup:
        service.fileInfoService = new FileInfoService()
        service.fileInfoService.metaClass.getFileInfo = { String id, String clientSeupId ->
            FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.UPLOADED.value, 21, [])
            return fileInfo
        }
        service.metaClass.processVerificationStep = { FileInfo fileInfo, ElasticSearchConfig esConfig ->
            fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.DRAFT.value, 21, [])
            return fileInfo //returning file info with draft status
        }
        def jsonMessage = new JSONObject([fileInfo: [esEndpoint: 'endpoint', esIndex: 'index', id: 'dfgjkle4', clientSetupId: '55', fileRowCount: 0, missingFields: [''], processStartTime: 0]])

        when:
        def result = service.kickoffVerifyStep(jsonMessage)

        then:
        result
    }

    void "processVerificationStep should return file with failed status if file status is not pre-verified before starting"() {
        setup:
        service.fileInfoService = new FileInfoService()
        service.fileInfoService.metaClass.updateOrgUploadStatus = { FileInfo fileInfo, OrgUploadStatus nextStep ->
            fileInfo.status = nextStep.value
            return fileInfo
        }
        //file status not pre-verified
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.UPLOADED.value, 21, [])

        when:
        fileInfo = service.processVerificationStep(fileInfo, esConfig)

        then:
        fileInfo.status == OrgUploadStatus.FAILED.value
    }

    void "processVerificationStep should bump file status to failed if verify step fails"() {
        setup:
        service.orgUploadProcessService = new OrgUploadProcessService()
        service.fileInfoService = new FileInfoService()
        service.fileInfoService.metaClass.updateOrgUploadStatus = { FileInfo fileInfo, OrgUploadStatus nextStep ->
            fileInfo.status = nextStep.value
            return fileInfo
        }
        service.orgUploadProcessService.metaClass.verifyOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig ->
            return false //fails
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.PRE_VERIFIED.value, 21, [])

        when:
        fileInfo = service.processVerificationStep(fileInfo, esConfig)

        then:
        fileInfo.status == OrgUploadStatus.FAILED.value
    }

    void "processVerificationStep should bump file status to draft if verify step succeeds"() {
        setup:
        service.orgUploadProcessService = new OrgUploadProcessService()
        service.fileInfoService = new FileInfoService()
        service.fileInfoService.metaClass.updateOrgUploadStatus = { FileInfo fileInfo, OrgUploadStatus nextStep ->
            fileInfo.status = nextStep.value
            return fileInfo
        }
        service.orgUploadProcessService.metaClass.verifyOrgUpload = { FileInfo fileInfo, ElasticSearchConfig esConfig ->
            return true //succeeds
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.PRE_VERIFIED.value, 21, [])

        when:
        fileInfo = service.processVerificationStep(fileInfo, esConfig)

        then:
        fileInfo.status == OrgUploadStatus.DRAFT.value
    }

}
