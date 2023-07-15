package tbservice

import grails.testing.services.ServiceUnitTest
import org.elasticsearch.index.query.QueryBuilder
import spock.lang.Specification
import grails.testing.gorm.DataTest
import tbservice.dto.org.DuplicateStats
import tbservice.dto.org.FileInfo
import tbservice.dto.org.ProcessedUserRowDTO
import tbservice.dto.org.RawUser
import tbservice.dto.org.UserUniqueInfo
import tbservice.enums.DataLocation
import tbservice.enums.OrgUploadStatus
import tbservice.enums.UserField
import tbservice.enums.UserStatus

class OrgUploadProcessServiceSpec extends Specification implements DataTest, ServiceUnitTest<OrgUploadProcessService> {

    def esConfig = new ElasticSearchConfig("endpoint", "index")

    def setup() {}

    def cleanup() {}

    def setupSpec() {
        mockDomain User
        mockDomain UserRelationship
        mockDomain ClientSetup
    }

    void "if file info has missing fields preProcessOrgUpload should return false"() {
        setup:
        service.metaClass.assignEecsFromFile = { FileInfo fileInfo, ElasticSearchConfig esConfig ->
            return true
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.PRE_PROCESSING.value, 21, [])
        fileInfo.missingFields = ['firstName']

        when:
        def result = service.preProcessOrgUpload(fileInfo, new ElasticSearchConfig('esEndpoint', 'esIndex'))

        then:
        !result
    }

    void "if file info has no missing fields, but esIndex is null preProcessOrgUpload should return false"() {
        setup:
        service.metaClass.assignEecsFromFile = { FileInfo fileInfo, ElasticSearchConfig esConfig ->
            return true
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.PRE_PROCESSING.value, 21, [])
        fileInfo.missingFields = []

        when:
        def result = service.preProcessOrgUpload(fileInfo, new ElasticSearchConfig('', ''))

        then:
        !result
    }

    void "if assigning Eecs fails, preProcessOrgUpload should return false"() {
        setup:
        service.metaClass.assignEecsFromFile = { FileInfo fileInfo, ElasticSearchConfig esConfig ->
            return false
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.PRE_PROCESSING.value, 21, [])
        fileInfo.missingFields = []

        when:
        def result = service.preProcessOrgUpload(fileInfo, new ElasticSearchConfig('esEndpoint', 'esIndex'))

        then:
        !result
    }

    void "if file info has no missing and parameters are correct preProcessOrgUpload should return true"() {
        setup:
        service.metaClass.assignEecsFromFile = { FileInfo fileInfo, ElasticSearchConfig esConfig ->
            return true
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.PRE_PROCESSING.value, 21, [])

        fileInfo.missingFields = []

        when:
        def result = service.preProcessOrgUpload(fileInfo, new ElasticSearchConfig('esEndpoint', 'esIndex'))

        then:
        result
    }

    void "assignEecsFromFile should not update existing users EECs with no matching emails or externalPersonIds"() {
        setup:
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchAllInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, String[] includedFields = null, QueryBuilder query = null ->
            return [
                    [_source: [raw: [externalEmployeeCode: 'fdsa', email: 'fdsa@gmail.com', externalPersonId: 'fdsa']]], //email match
            ]
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.PRE_PROCESSING.value, 21, [])
        User existingUser = new User(
                active: true,
                clientSetupId: 55,
                email: "asdf@gmail.com", //no email match
                externalEmployeeCode: null, //no eec
                externalPersonId: 'asdf', //no externalPersonId match
                firstName: "as",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "df",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "asdf@gmail.com",
        )
        existingUser.save(flush: true)

        when:
        service.assignEecsFromFile(fileInfo, esConfig)

        then:
        !existingUser.externalEmployeeCode
    }

    void "assignEecsFromFile should update existing users EECs with matching emails"() {
        setup:
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchAllInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, String[] includedFields = null, QueryBuilder query = null ->
            return [
                    [_source: [raw: [externalEmployeeCode: 'asdf', email: 'asdf@gmail.com']]], //email match
            ]
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.PRE_PROCESSING.value, 21, [])
        User existingUser = new User(
                active: true,
                clientSetupId: 55,
                email: "asdf@gmail.com", //email match
                externalEmployeeCode: null, //no eec
                externalPersonId: null,
                firstName: "as",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "df",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "asdf@gmail.com",
        )
        existingUser.save(flush: true)

        when:
        service.assignEecsFromFile(fileInfo, esConfig)

        then:
        existingUser.externalEmployeeCode == 'asdf'
    }

    void "assignEecsFromFile should update existing users EECs with matching externalPersonIds"() {
        setup:
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchAllInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, String[] includedFields = null, QueryBuilder query = null ->
            return [
                    [_source: [raw: [externalEmployeeCode: 'asdf', externalPersonId: 'asdf1']]], //externalPersonId match
            ]
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.PRE_PROCESSING.value, 21, [])
        User existingUser = new User(
                active: true,
                clientSetupId: 55,
                email: "asdf1234@gmail.com", //not matching email match
                externalEmployeeCode: null, //no eec
                externalPersonId: 'asdf1', //externalPersonId match
                firstName: "as",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "df",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "asdf@gmail.com",
        )
        existingUser.save(flush: true)

        when:
        service.assignEecsFromFile(fileInfo, esConfig)

        then:
        existingUser.externalEmployeeCode == 'asdf'
    }

    void "processOrgUpload should return false if no client setup can be found"() {
        setup:
        ClientSetup.metaClass.static.get = { Serializable id ->
            return null
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, int start, int size, String[] includedFields = null, QueryBuilder query = null ->
            return [[_source: [raw: [externalEmployeeCode: 'userCode', 'firstName': 'Kevin', lastName: 'Vandy', mgrExternalEmployeeCode: 'manCode']]],
                    [_source: [raw: [externalEmployeeCode: 'manCode', 'firstName': 'Dustin', lastName: 'Pittack', mgrExternalEmployeeCode: '']]]]
        }
        service.elasticSearchService.metaClass.bulkUpdateRows = { ElasticSearchConfig esConfig, List<ProcessedUserRowDTO> bulkData, OrgUploadStatus step, boolean waitForCompletion ->
            return true
        }

        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", 'sdf', 'filename.csv', OrgUploadStatus.PROCESSING.value, 21, [])

        when:
        def result = service.processOrgUpload(fileInfo, esConfig)

        then:
        !result
    }

    void "processOrgUpload should return false if no pre-processed rows are found to process"() {
        setup:
        ClientSetup.metaClass.static.get = { Serializable id ->
            return new ClientSetup(id: 55)
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, int start, int size, String[] includedFields = null, QueryBuilder query = null ->
            return [] //no rows found
        }

        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.PROCESSING.value, 21, [])

        when:
        def result = service.processOrgUpload(fileInfo, esConfig)

        then:
        !result
    }

    void "processOrgUpload should return false if processing rows is unsuccessful"() {
        setup:
        ClientSetup.metaClass.static.get = { Serializable id ->
            return new ClientSetup(id: 55)
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, int start, int size, String[] includedFields = null, QueryBuilder query = null ->
            return [[_source: [raw: [externalEmployeeCode: 'userCode', 'firstName': 'Kevin', lastName: 'Vandy', mgrExternalEmployeeCode: 'manCode']]],
                    [_source: [raw: [externalEmployeeCode: 'manCode', 'firstName': 'Dustin', lastName: 'Pittack', mgrExternalEmployeeCode: '']]]]
        }
        service.elasticSearchService.metaClass.bulkUpdateRows = { ElasticSearchConfig esConfig, List<ProcessedUserRowDTO> bulkData, OrgUploadStatus step, boolean waitForCompletion ->
            return true
        }
        service.metaClass.bulkProcessRows = { List<Object> preProcessedRows, Map<String, User> existingUsers, Map<Long, UserRelationship> existingUserRelationships, Map<String, RawUser> managersInfo, Set<String> allowedDomains = [], Map<Long, List> userRoleGroupChanges = null ->
            return null //simulate error processing
        }

        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", 'sdf', 'filename.csv', OrgUploadStatus.PROCESSING.value, 21, [])

        when:
        def result = service.processOrgUpload(fileInfo, esConfig)

        then:
        !result
    }

    void "processOrgUpload should return false if after successfully processing rows, bulk update fails"() {
        setup:
        ClientSetup.metaClass.static.get = { Serializable id ->
            return new ClientSetup(id: 55)
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, int start, int size, String[] includedFields = null, QueryBuilder query = null ->
            return [[_source: [raw: [externalEmployeeCode: 'userCode', 'firstName': 'Kevin', lastName: 'Vandy', mgrExternalEmployeeCode: 'manCode']]],
                    [_source: [raw: [externalEmployeeCode: 'manCode', 'firstName': 'Dustin', lastName: 'Pittack', mgrExternalEmployeeCode: '']]]]
        }
        service.elasticSearchService.metaClass.bulkUpdateRows = { ElasticSearchConfig esConfig, List<ProcessedUserRowDTO> bulkData, OrgUploadStatus step, boolean waitForCompletion ->
            return false //simulate failed bulk update
        }

        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", 'sdf', 'filename.csv', OrgUploadStatus.PROCESSING.value, 21, [])

        when:
        def result = service.processOrgUpload(fileInfo, esConfig)

        then:
        !result
    }

    void "processOrgUpload should return true after successfully processing rows"() {
        setup:
        ClientSetup.metaClass.static.get = { Serializable id ->
            return new ClientSetup(id: 55)
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, int start, int size, String[] includedFields = null, QueryBuilder query = null ->
            return [[_source: [raw: [externalEmployeeCode: 'userCode', 'firstName': 'Kevin', lastName: 'Vandy', mgrExternalEmployeeCode: 'manCode']]],
                    [_source: [raw: [externalEmployeeCode: 'manCode', 'firstName': 'Dustin', lastName: 'Pittack', mgrExternalEmployeeCode: '']]]]
        }
        service.elasticSearchService.metaClass.bulkUpdateRows = { ElasticSearchConfig esConfig, List<ProcessedUserRowDTO> bulkData, OrgUploadStatus step, boolean waitForCompletion ->
            return true
        }

        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", 'sdf', 'filename.csv', OrgUploadStatus.PROCESSING.value, 21, [])

        when:
        def result = service.processOrgUpload(fileInfo, esConfig)

        then:
        result
    }

    void "bulkProcessRows should return null if invalid pre-processed strings are passed"() {
        setup:
        List<Object> preProcessedRows = ['emp1', 'emp2']
        Map<String, User> existingUsers = [:]
        Map<Long, UserRelationship> existingUserRelationships = [:]
        Map<String, RawUser> managersInfo = [:]

        when:
        def processedUserRows = service.bulkProcessRows(preProcessedRows, existingUsers, existingUserRelationships, managersInfo)

        then:
        processedUserRows == null
    }

    void "bulkProcessRows should return empty list if invalid pre-processed row object maps are passed"() {
        setup:
        List<Object> preProcessedRows = [[badProperty: ['emp1']]]
        Map<String, User> existingUsers = [:]
        Map<Long, UserRelationship> existingUserRelationships = [:]
        Map<String, RawUser> managersInfo = [:]

        when:
        def processedUserRows = service.bulkProcessRows(preProcessedRows, existingUsers, existingUserRelationships, managersInfo)

        then:
        processedUserRows == []
    }

    void "bulkProcessRows should return list of processed rows if valid map data is passed"() {
        setup:
        List<Object> preProcessedRows = [
                [_source: [raw: [
                        externalEmployeeCode   : 'asdf',
                        firstName              : 'John',
                        lastName               : 'Smith',
                        email                  : 'asdf@gmail.com',
                        externalPersonId       : 'asdf',
                        mgrExternalEmployeeCode: '',
                        delete                 : ''
                ]]],
                [_source: [raw: [
                        externalEmployeeCode   : 'fdsa',
                        firstName              : 'Jane',
                        lastName               : 'Doe',
                        email                  : 'fdsa@gmail.com',
                        externalPersonId       : 'fdsa',
                        mgrExternalEmployeeCode: '',
                        delete                 : ''
                ]]]
        ]
        Map<String, User> existingUsers = [:]
        Map<Long, UserRelationship> existingUserRelationships = [:]
        Map<String, RawUser> managersInfo = [:]

        when:
        def processedUserRows = service.bulkProcessRows(preProcessedRows, existingUsers, existingUserRelationships, managersInfo)

        then:
        processedUserRows.size() == 2
        processedUserRows instanceof List<ProcessedUserRowDTO>
    }

    void "postProcessOrgUpload shout return true if no duplicates are found"() {
        setup:
        service.metaClass.findDuplicates = { FileInfo fileInfo, ElasticSearchConfig esConfig ->
            return [externalEmployeeCodes: [:], emails: [:], externalPersonIds: [:]]
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.PROCESSING.value, 21, [])


        when:
        def result = service.postProcessOrgUpload(fileInfo, esConfig)

        then:
        result
    }

    void "postProcessOrgUpload should return false if duplicates are found and bulk update fails"() {
        setup:
        service.metaClass.findDuplicates = { FileInfo fileInfo, ElasticSearchConfig esConfig ->
            return [
                    externalEmployeeCodes: [
                            'asdf': new DuplicateStats('asdf', [2, 3], [4, 5]),
                            'fdsa': new DuplicateStats('fdsa', [77, 6], [55, 5])
                    ],
                    emails               : [
                            'asdf@gmail.com': new DuplicateStats(['asdf'], [2, 3], [4, 5]),
                            'fdsa@gmail.com': new DuplicateStats(['fdsa'], [77, 6], [55, 5])
                    ],
                    externalPersonIds    : [
                            'asdf1': new DuplicateStats(['asdf'], [2, 3], [4, 5]),
                            'fdsa1': new DuplicateStats(['fdsa'], [77, 6], [55, 5])
                    ]
            ]
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, int start, int size, String[] includedFields = null, QueryBuilder query = null ->
            return [
                    [_source: [
                            raw        : [
                                    externalEmployeeCode: 'asdf',
                                    email               : 'asdf@gmail.com',
                                    externalPersonId    : 'asdf',
                            ],
                            validation : [errors: [], warnings: [], duplicates: []],
                            differences: []
                    ]],
                    [_source: [
                            raw        : [
                                    externalEmployeeCode: 'fdsa',
                                    email               : 'fdsa@gmail.com',
                                    externalPersonId    : 'fdsa',
                            ],
                            validation : [errors: [], warnings: [], duplicates: []],
                            differences: []
                    ]]
            ]
        }
        service.elasticSearchService.metaClass.bulkUpdateRows = { ElasticSearchConfig esConfig, List<ProcessedUserRowDTO> bulkData, OrgUploadStatus step, boolean waitForCompletion ->
            return false //simulate failed bulk update
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.PROCESSING.value, 21, [])


        when:
        def result = service.postProcessOrgUpload(fileInfo, esConfig)

        then:
        !result
    }

    void "postProcessOrgUpload should return true if duplicates are found and bulk update succeeds"() {
        setup:
        service.metaClass.findDuplicates = { FileInfo fileInfo, ElasticSearchConfig esConfig ->
            return [
                    externalEmployeeCodes: [
                            'asdf': new DuplicateStats('asdf', [2, 3], [4, 5]),
                            'fdsa': new DuplicateStats('fdsa', [77, 6], [55, 5])
                    ],
                    emails               : [
                            'asdf@gmail.com': new DuplicateStats('asdf', [2, 3], [4, 5]),
                            'fdsa@gmail.com': new DuplicateStats('fdsa', [77, 6], [55, 5])
                    ],
                    externalPersonIds    : [
                            'asdf1': new DuplicateStats('asdf', [2, 3], [4, 5]),
                            'fdsa1': new DuplicateStats('fdsa', [77, 6], [55, 5])
                    ]
            ]
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, int start, int size, String[] includedFields = null, QueryBuilder query = null ->
            return [
                    [_source: [
                            raw        : [
                                    externalEmployeeCode: 'asdf',
                                    email               : 'asdf@gmail.com',
                                    externalPersonId    : 'asdf',
                            ],
                            validation : [errors: [], warnings: [], duplicates: []],
                            differences: []
                    ]],
                    [_source: [
                            raw        : [
                                    externalEmployeeCode: 'fdsa',
                                    email               : 'fdsa@gmail.com',
                                    externalPersonId    : 'fdsa',
                            ],
                            validation : [errors: [], warnings: [], duplicates: []],
                            differences: []
                    ]]
            ]
        }
        service.elasticSearchService.metaClass.bulkUpdateRows = { ElasticSearchConfig esConfig, List<ProcessedUserRowDTO> bulkData, OrgUploadStatus step, boolean waitForCompletion ->
            return true //simulate successful bulk update
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.PROCESSING.value, 21, [])


        when:
        def result = service.postProcessOrgUpload(fileInfo, esConfig)

        then:
        result
    }

    void "findDuplicates should return null if no client setup id is on file info"() {
        setup:
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "", '', 'filename.csv', OrgUploadStatus.UPLOADED.value, 21, [])

        when:
        def duplicates = service.findDuplicates(fileInfo, esConfig)

        then:
        duplicates == null
    }

    void "findDuplicates should return empty maps (no duplicates) if no data can be loaded to look for duplicates"() {
        setup:
        User.metaClass.static.findAllUserUniqueInfoByClientId = { Long clientSetupId ->
            return [] //simulate no rows found
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchAllInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, String[] includedFields = null, QueryBuilder query = null ->
            return [] //simulate no rows found
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.UPLOADED.value, 21, [])

        when:
        def duplicates = service.findDuplicates(fileInfo, esConfig)

        then:
        duplicates == [externalEmployeeCodes: [:], emails: [:], externalPersonIds: [:]]
    }

    void "findDuplicates should return empty maps (no duplicates) if no duplicates are found from users with data being updated"() {
        setup: //no duplicates should be found, db and csv should have identical data, join on EEC
        User.metaClass.static.findAllUserUniqueInfoByClientId = { Long clientSeupId ->
            return [
                    new User(clientSetupId: 1, id: 33, externalEmployeeCode: 'asdf', email: 'asdf@gmail.com', externalPersonId: 'asdf1'),
                    new User(clientSetupId: 1, id: 44, externalEmployeeCode: 'fdsa', email: 'fdsa@gmail.com', externalPersonId: 'fdsa1')
            ]
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchAllInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, String[] includedFields = null, QueryBuilder query = null ->
            return [
                    [_id    : "1",
                     _source: [
                             raw        : [
                                     externalEmployeeCode: 'asdf',
                                     email               : 'asdf@gmail.com',
                                     externalPersonId    : 'asdf1',
                             ],
                             validation : [errors: [], warnings: [], duplicates: []],
                             differences: []
                     ]],
                    [_id    : "2",
                     _source: [
                             raw        : [
                                     externalEmployeeCode: 'fdsa',
                                     email               : 'fdsa@gmail.com',
                                     externalPersonId    : 'fdsa1',
                             ],
                             validation : [errors: [], warnings: [], duplicates: []],
                             differences: []
                     ]]
            ]
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "1", '', 'filename.csv', OrgUploadStatus.UPLOADED.value, 21, [])

        when:
        def duplicates = service.findDuplicates(fileInfo, esConfig)

        then:
        duplicates == [externalEmployeeCodes: [:], emails: [:], externalPersonIds: [:]]
    }

    void "findDuplicates should return empty maps (no duplicates) if no duplicates are found from users that do not match from csv to db"() {
        setup: //no duplicates should be found, db and csv should have identical data, join on EEC
        User.metaClass.static.findAllUserUniqueInfoByClientId = { Long clientSeupId ->
            return [
                    new User(clientSetupId: 1, id: 33, externalEmployeeCode: 'asdf', email: 'asdf@gmail.com', externalPersonId: 'asdf1'),
                    new User(clientSetupId: 1, id: 44, externalEmployeeCode: 'fdsa', email: 'fdsa@gmail.com', externalPersonId: 'fdsa1')
            ]
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchAllInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, String[] includedFields = null, QueryBuilder query = null ->
            return [
                    [_id    : "1",
                     _source: [
                             raw        : [
                                     externalEmployeeCode: 'qwerty',
                                     email               : 'qwerty@gmail.com',
                                     externalPersonId    : 'qwerty1',
                             ],
                             validation : [errors: [], warnings: [], duplicates: []],
                             differences: []
                     ]],
                    [_id    : "2",
                     _source: [
                             raw        : [
                                     externalEmployeeCode: 'zxcvb',
                                     email               : 'zxcvb@gmail.com',
                                     externalPersonId    : 'zxcvb1',
                             ],
                             validation : [errors: [], warnings: [], duplicates: []],
                             differences: []
                     ]]
            ]
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "1", '', 'filename.csv', OrgUploadStatus.UPLOADED.value, 21, [])

        when:
        def duplicates = service.findDuplicates(fileInfo, esConfig)

        then:
        duplicates == [externalEmployeeCodes: [:], emails: [:], externalPersonIds: [:]]
    }

    void "findDuplicates should return maps of externalEmployeeCode db duplicates"() {
        setup: //externalEmployeeCode duplicate in db
        User.metaClass.static.findAllUserUniqueInfoByClientId = { Long clientSeupId ->
            return [
                    new User(clientSetupId: 1, externalEmployeeCode: 'asdf', email: 'asdf@gmail.com', externalPersonId: 'asdf1'),
                    new User(clientSetupId: 1, externalEmployeeCode: 'asdf', email: 'fdsa@gmail.com', externalPersonId: 'fdsa1')
            ]
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchAllInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, String[] includedFields = null, QueryBuilder query = null ->
            return []
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "1", '', 'filename.csv', OrgUploadStatus.UPLOADED.value, 21, [])

        when:
        def duplicates = service.findDuplicates(fileInfo, esConfig)

        then:
        duplicates.externalEmployeeCodes?.size() == 1
        duplicates.emails?.size() == 0
        duplicates.externalPersonIds?.size() == 0
        duplicates.externalEmployeeCodes['asdf'].dbIds?.size() == 2
        duplicates.externalEmployeeCodes['asdf'].fileIds?.size() == 0
    }

    void "findDuplicates should return maps of email db duplicates"() {
        setup: //email duplicate in db
        User.metaClass.static.findAllUserUniqueInfoByClientId = { Long clientSeupId ->
            return [
                    new User(clientSetupId: 1, externalEmployeeCode: 'asdf', email: 'asdf@gmail.com', externalPersonId: 'asdf1'),
                    new User(clientSetupId: 1, externalEmployeeCode: 'fdsa', email: 'asdf@gmail.com', externalPersonId: 'fdsa1')
            ]
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchAllInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, String[] includedFields = null, QueryBuilder query = null ->
            return []
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "1", '', 'filename.csv', OrgUploadStatus.UPLOADED.value, 21, [])

        when:
        def duplicates = service.findDuplicates(fileInfo, esConfig)

        then:
        duplicates.externalEmployeeCodes?.size() == 0
        duplicates.emails?.size() == 1
        duplicates.externalPersonIds?.size() == 0
        duplicates.emails['asdf@gmail.com'].dbIds?.size() == 2
        duplicates.emails['asdf@gmail.com'].fileIds?.size() == 0
    }

    void "findDuplicates should return maps of externalPersonId db duplicates"() {
        setup: //externalPersonId duplicate in db
        User.metaClass.static.findAllUserUniqueInfoByClientId = { Long clientSeupId ->
            return [
                    new User(clientSetupId: 1, externalEmployeeCode: 'asdf', email: 'asdf@gmail.com', externalPersonId: 'asdf1'),
                    new User(clientSetupId: 1, externalEmployeeCode: 'fdsa', email: 'fdsa@gmail.com', externalPersonId: 'asdf1')
            ]
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchAllInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, String[] includedFields = null, QueryBuilder query = null ->
            return []
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "1", '', 'filename.csv', OrgUploadStatus.UPLOADED.value, 21, [])

        when:
        def duplicates = service.findDuplicates(fileInfo, esConfig)

        then:
        duplicates.externalEmployeeCodes?.size() == 0
        duplicates.emails?.size() == 0
        duplicates.externalPersonIds?.size() == 1
        duplicates.externalPersonIds['asdf1'].dbIds?.size() == 2
        duplicates.externalPersonIds['asdf1'].fileIds?.size() == 0
    }

    void "findDuplicates should return maps of externalEmployeeCode csv duplicates"() {
        setup: //externalEmployeeCode duplicate in csv
        User.metaClass.static.findAllUserUniqueInfoByClientId = { Long clientSeupId ->
            return []
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchAllInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, String[] includedFields = null, QueryBuilder query = null ->
            return [
                    [_id    : "1",
                     _source: [
                             raw        : [
                                     externalEmployeeCode: 'asdf', //duplicate
                                     email               : 'asdf@gmail.com',
                                     externalPersonId    : 'asdf1',
                             ],
                             validation : [errors: [], warnings: [], duplicates: []],
                             differences: []
                     ]],
                    [_id    : "2",
                     _source: [
                             raw        : [
                                     externalEmployeeCode: 'asdf', //duplicate
                                     email               : 'fdsa@gmail.com',
                                     externalPersonId    : 'fdsa1',
                             ],
                             validation : [errors: [], warnings: [], duplicates: []],
                             differences: []
                     ]]
            ]
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "1", '', 'filename.csv', OrgUploadStatus.UPLOADED.value, 21, [])

        when:
        def duplicates = service.findDuplicates(fileInfo, esConfig)

        then:
        duplicates.externalEmployeeCodes?.size() == 1
        duplicates.emails?.size() == 0
        duplicates.externalPersonIds?.size() == 0
        duplicates.externalEmployeeCodes['asdf'].dbIds?.size() == 0
        duplicates.externalEmployeeCodes['asdf'].fileIds?.size() == 2
    }

    void "findDuplicates should return maps of email csv duplicates"() {
        setup: //email duplicate in csv
        User.metaClass.static.findAllUserUniqueInfoByClientId = { Long clientSeupId ->
            return []
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchAllInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, String[] includedFields = null, QueryBuilder query = null ->
            return [
                    [_id    : "1",
                     _source: [
                             raw        : [
                                     externalEmployeeCode: 'asdf',
                                     email               : 'asdf@gmail.com', //duplicate
                                     externalPersonId    : 'asdf1',
                             ],
                             validation : [errors: [], warnings: [], duplicates: []],
                             differences: []
                     ]],
                    [_id    : "2",
                     _source: [
                             raw        : [
                                     externalEmployeeCode: 'fdsa',
                                     email               : 'asdf@gmail.com', //duplicate
                                     externalPersonId    : 'fdsa1',
                             ],
                             validation : [errors: [], warnings: [], duplicates: []],
                             differences: []
                     ]]
            ]
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "1", '', 'filename.csv', OrgUploadStatus.UPLOADED.value, 21, [])

        when:
        def duplicates = service.findDuplicates(fileInfo, esConfig)

        then:
        duplicates.externalEmployeeCodes?.size() == 0
        duplicates.emails?.size() == 1
        duplicates.externalPersonIds?.size() == 0
        duplicates.emails['asdf@gmail.com'].dbIds?.size() == 0
        duplicates.emails['asdf@gmail.com'].fileIds?.size() == 2
    }

    void "findDuplicates should return maps of externalPersonId csv duplicates"() {
        setup: //externalPersonId duplicate in csv
        User.metaClass.static.findAllUserUniqueInfoByClientId = { Long clientSeupId ->
            return []
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchAllInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, String[] includedFields = null, QueryBuilder query = null ->
            return [
                    [_id    : "1",
                     _source: [
                             raw        : [
                                     externalEmployeeCode: 'asdf',
                                     email               : 'asdf@gmail.com',
                                     externalPersonId    : 'asdf1', //duplicate
                             ],
                             validation : [errors: [], warnings: [], duplicates: []],
                             differences: []
                     ]],
                    [_id    : "2",
                     _source: [
                             raw        : [
                                     externalEmployeeCode: 'fdsa',
                                     email               : 'fdsa@gmail.com',
                                     externalPersonId    : 'asdf1', //duplicate
                             ],
                             validation : [errors: [], warnings: [], duplicates: []],
                             differences: []
                     ]]
            ]
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "1", '', 'filename.csv', OrgUploadStatus.UPLOADED.value, 21, [])

        when:
        def duplicates = service.findDuplicates(fileInfo, esConfig)

        then:
        duplicates.externalEmployeeCodes?.size() == 0
        duplicates.emails?.size() == 0
        duplicates.externalPersonIds?.size() == 1
        duplicates.externalPersonIds['asdf1'].dbIds?.size() == 0
        duplicates.externalPersonIds['asdf1'].fileIds?.size() == 2
    }

    void "findDuplicates should find email duplicate across db and csv"() {
        setup: //no duplicates should be found, db and csv should have identical data, join on EEC
        User.metaClass.static.findAllUserUniqueInfoByClientId = { Long clientSeupId ->
            return [
                    new User(clientSetupId: 1, id: 33, externalEmployeeCode: 'asdf', email: 'asdf@gmail.com', externalPersonId: 'asdf1')
            ]
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchAllInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, String[] includedFields = null, QueryBuilder query = null ->
            return [
                    [_id    : "1",
                     _source: [
                             raw        : [
                                     externalEmployeeCode: 'qwerty',
                                     email               : 'asdf@gmail.com', //duplicate with other db user
                                     externalPersonId    : 'qwerty1',
                             ],
                             validation : [errors: [], warnings: [], duplicates: []],
                             differences: []
                     ]]
            ]
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "1", '', 'filename.csv', OrgUploadStatus.UPLOADED.value, 21, [])

        when:
        def duplicates = service.findDuplicates(fileInfo, esConfig)

        then:
        duplicates.externalEmployeeCodes?.size() == 0
        duplicates.emails?.size() == 1
        duplicates.externalPersonIds?.size() == 0
        duplicates.emails['asdf@gmail.com'].dbIds?.size() == 1
        duplicates.emails['asdf@gmail.com'].fileIds?.size() == 1
    }

    void "findDuplicates should find externalPersonId duplicate across db and csv"() {
        setup: //no duplicates should be found, db and csv should have identical data, join on EEC
        User.metaClass.static.findAllUserUniqueInfoByClientId = { Long clientSeupId ->
            return [
                    new User(clientSetupId: 1, id: 33, externalEmployeeCode: 'asdf', email: 'asdf@gmail.com', externalPersonId: 'asdf1')
            ]
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchAllInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, String[] includedFields = null, QueryBuilder query = null ->
            return [
                    [_id    : "1",
                     _source: [
                             raw        : [
                                     externalEmployeeCode: 'qwerty',
                                     email               : 'qwerty@gmail.com',
                                     externalPersonId    : 'asdf1', //duplicate with other db user
                             ],
                             validation : [errors: [], warnings: [], duplicates: []],
                             differences: []
                     ]]
            ]
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "1", '', 'filename.csv', OrgUploadStatus.UPLOADED.value, 21, [])

        when:
        def duplicates = service.findDuplicates(fileInfo, esConfig)

        then:
        duplicates.externalEmployeeCodes?.size() == 0
        duplicates.emails?.size() == 0
        duplicates.externalPersonIds?.size() == 1
        duplicates.externalPersonIds['asdf1'].dbIds?.size() == 1
        duplicates.externalPersonIds['asdf1'].fileIds?.size() == 1
    }

    void "findExternalEmployeeCodeDuplicates should return no duplicates if db and csv individually have no duplicates"() {
        setup: //no externalEmployeeCode duplicate in csv or db
        List<UserUniqueInfo> dbUserUniqueInfo = [
                new UserUniqueInfo(33, DataLocation.DB, 'asdf', 'asdf@gmail.com', 'asdf1'),
                new UserUniqueInfo(44, DataLocation.DB, 'fdsa', 'fdsa@gmail.com', 'fdsa1')
        ]
        List<UserUniqueInfo> csvUserUniqueInfo = [
                new UserUniqueInfo(1, DataLocation.CSV, 'asdf', 'asdf@gmail.com', 'asdf1'),
                new UserUniqueInfo(2, DataLocation.CSV, 'fdsa', 'fdsa@gmail.com', 'fdsa1')
        ]

        when:
        def duplicates = service.findExternalEmployeeCodeDuplicates(dbUserUniqueInfo, csvUserUniqueInfo)

        then:
        duplicates?.size() == 0
    }

    void "findExternalEmployeeCodeDuplicates should return duplicates if db and csv individually have duplicates"() {
        setup: //externalEmployeeCode duplicates in csv and db
        List<UserUniqueInfo> dbUserUniqueInfo = [
                new UserUniqueInfo(33, DataLocation.DB, 'asdf', 'asdf@gmail.com', 'asdf1'),
                new UserUniqueInfo(44, DataLocation.DB, 'asdf', 'fdsa@gmail.com', 'fdsa1')
        ]
        List<UserUniqueInfo> csvUserUniqueInfo = [
                new UserUniqueInfo(1, DataLocation.CSV, 'fdsa', 'asdf@gmail.com', 'asdf1'),
                new UserUniqueInfo(2, DataLocation.CSV, 'fdsa', 'fdsa@gmail.com', 'fdsa1')
        ]

        when:
        def duplicates = service.findExternalEmployeeCodeDuplicates(dbUserUniqueInfo, csvUserUniqueInfo)

        then:
        duplicates?.size() == 2
        duplicates['asdf']?.dbIds?.size() == 2
        duplicates['asdf']?.dbIds == [33, 44] as List<Long>
        duplicates['fdsa']?.fileIds?.size() == 2
        duplicates['fdsa']?.fileIds == [1, 2] as List<Long>
    }

    void "findDuplicatesByField should return null if invalid field name is passed"() {
        setup:
        UserField fieldName = UserField.JOB_TITLE //invalid
        Map<String, UserUniqueInfo> userMap = ['asdf': new UserUniqueInfo(1, DataLocation.CSV, 'asdf', 'asdf@gmail.com', 'asdf1')]

        when:
        def duplicates = service.findDuplicatesByField(fieldName, userMap)

        then:
        duplicates == null
    }

    void "findDuplicatesByField should return empty map if no duplicates found"() {
        setup:
        UserField fieldName = UserField.EMAIL
        Map<String, UserUniqueInfo> userMap = [
                'asdf': new UserUniqueInfo(1, DataLocation.CSV, 'asdf', 'asdf@gmail.com', 'asdf1'),
                'fdsa': new UserUniqueInfo(22, DataLocation.DB, 'fdsa', 'fdsa@gmail.com', 'fdsa1')
        ]

        when:
        def duplicates = service.findDuplicatesByField(fieldName, userMap)

        then:
        duplicates == [:]
    }

    void "findDuplicatesByField should find email duplicates"() {
        setup:
        UserField fieldName = UserField.EMAIL
        Map<String, UserUniqueInfo> userMap = [
                'asdf'  : new UserUniqueInfo(1, DataLocation.CSV, 'asdf', 'asdf@gmail.com', 'asdf1'),
                'fdsa'  : new UserUniqueInfo(22, DataLocation.DB, 'fdsa', 'asdf@gmail.com', 'fdsa1'),
                'qwerty': new UserUniqueInfo(23, DataLocation.DB, 'qwerty', 'qwerty@gmail.com', 'qwerty1')
        ]

        when:
        Map<String, DuplicateStats> duplicates = service.findDuplicatesByField(fieldName, userMap) as Map<String, DuplicateStats>

        then:
        duplicates.size() == 1
        duplicates.containsKey('asdf@gmail.com')
        duplicates['asdf@gmail.com'].externalEmployeeCodes == ['asdf', 'fdsa']
        duplicates['asdf@gmail.com'].dbIds == [22] as List<Long>
        duplicates['asdf@gmail.com'].fileIds == [1] as List<Long>
    }

    void "findDuplicatesByField should find externalPersonId duplicates"() {
        setup:
        UserField fieldName = UserField.EXTERNAL_PERSON_ID
        Map<String, UserUniqueInfo> userMap = [
                'asdf'  : new UserUniqueInfo(1, DataLocation.CSV, 'asdf', 'asdf@gmail.com', 'asdf1'),
                'fdsa'  : new UserUniqueInfo(22, DataLocation.DB, 'fdsa', 'fdsa@gmail.com', 'asdf1'),
                'qwerty': new UserUniqueInfo(23, DataLocation.DB, 'qwerty', 'qwerty@gmail.com', 'qwerty1')
        ]

        when:
        Map<String, DuplicateStats> duplicates = service.findDuplicatesByField(fieldName, userMap) as Map<String, DuplicateStats>

        then:
        duplicates.size() == 1
        duplicates.containsKey('asdf1')
        duplicates['asdf1'].externalEmployeeCodes == ['asdf', 'fdsa']
        duplicates['asdf1'].dbIds == [22] as List<Long>
        duplicates['asdf1'].fileIds == [1] as List<Long>
    }

    void "findDuplicatesByField should find all duplicate ids and externalEmployeeCodes that are related to the duplicate"() {
        setup:  //all have same externalPersonId
        UserField fieldName = UserField.EXTERNAL_PERSON_ID
        Map<String, UserUniqueInfo> userMap = [
                'asdf'  : new UserUniqueInfo(1, DataLocation.CSV, 'asdf', 'asdf@gmail.com', 'asdf1'),
                'fdsa'  : new UserUniqueInfo(22, DataLocation.DB, 'fdsa', 'fdsa@gmail.com', 'asdf1'),
                'qwerty': new UserUniqueInfo(23, DataLocation.DB, 'qwerty', 'qwerty@gmail.com', 'asdf1'),
                'zxcvb' : new UserUniqueInfo(3, DataLocation.CSV, 'zxcvb', 'zxcvb@gmail.com', 'asdf1')
        ]

        when:
        Map<String, DuplicateStats> duplicates = service.findDuplicatesByField(fieldName, userMap) as Map<String, DuplicateStats>

        then:
        duplicates.size() == 1
        duplicates.containsKey('asdf1')
        duplicates['asdf1'].externalEmployeeCodes == ['asdf', 'fdsa', 'qwerty', 'zxcvb']
        duplicates['asdf1'].dbIds == [22, 23] as List<Long>
        duplicates['asdf1'].fileIds == [1, 3] as List<Long>
    }

    void "verifyOrgUpload should return true if no invalid or fixed relationships are found to report"() {
        setup:
        UserRelationship.metaClass.static.findAllByClientSetupIdAsCodeMap = { Long clientSetupId ->
            return [:]
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchAllInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, String[] includedFields = null, QueryBuilder query = null ->
            return []
        }
        service.metaClass.findInvalidUserRelationships = { Map<String, String> userRelationships ->
            return [] //no errors
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.PROCESSING.value, 21, [])


        when:
        def result = service.verifyOrgUpload(fileInfo, esConfig)

        then:
        result
    }

    void "verifyOrgUpload should return false if reporting invalid relationships errors fails"() {
        setup:
        UserRelationship.metaClass.static.findAllByClientSetupIdAsCodeMap = { Long clientSetupId ->
            return [:]
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchAllInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, String[] includedFields = null, QueryBuilder query = null ->
            return []
        }
        service.metaClass.findInvalidUserRelationships = { Map<String, String> userRelationships ->
            return ['||asdf'] //1 new error
        }
        service.metaClass.updateRelationshipErrors = { ElasticSearchConfig esConfig, FileInfo fileInfo, List<String> eecToMarkErrors, List<String> eecToRemoveErrors ->
            return false //fails
        }
        service.metaClass.updateRelationshipWarnings = { ElasticSearchConfig esConfig, FileInfo fileInfo, List<String> eecToMarkErrors, List<String> eecToRemoveWarnings ->
            return true
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.PROCESSING.value, 21, [])


        when:
        def result = service.verifyOrgUpload(fileInfo, esConfig)

        then:
        !result
    }

    void "verifyOrgUpload should return false if reporting invalid relationships warnings fails"() {
        setup:
        UserRelationship.metaClass.static.findAllByClientSetupIdAsCodeMap = { Long clientSetupId ->
            return [:]
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchAllInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, String[] includedFields = null, QueryBuilder query = null ->
            return []
        }
        service.metaClass.findInvalidUserRelationships = { Map<String, String> userRelationships ->
            return ['||asdf'] //1 new error
        }
        service.metaClass.updateRelationshipErrors = { ElasticSearchConfig esConfig, FileInfo fileInfo, List<String> eecToMarkErrors, List<String> eecToRemoveErrors ->
            return true
        }
        service.metaClass.updateRelationshipWarnings = { ElasticSearchConfig esConfig, FileInfo fileInfo, List<String> eecToMarkErrors, List<String> eecToRemoveWarnings ->
            return false //fails
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.PROCESSING.value, 21, [])


        when:
        def result = service.verifyOrgUpload(fileInfo, esConfig)

        then:
        !result
    }

    void "verifyOrgUpload should return true if reporting invalid relationships succeeds"() {
        setup:
        UserRelationship.metaClass.static.findAllByClientSetupIdAsCodeMap = { Long clientSetupId ->
            return [:]
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchAllInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, String[] includedFields = null, QueryBuilder query = null ->
            return []
        }
        service.metaClass.findInvalidUserRelationships = { Map<String, String> userRelationships ->
            return ['||asdf']
        }
        service.metaClass.updateRelationshipErrors = { ElasticSearchConfig esConfig, FileInfo fileInfo, List<String> eecToMarkErrors, List<String> eecToRemoveErrors ->
            return true //succeeds
        }
        service.metaClass.updateRelationshipWarnings = { ElasticSearchConfig esConfig, FileInfo fileInfo, List<String> eecToMarkErrors, List<String> eecToRemoveWarnings ->
            return true
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.PROCESSING.value, 21, [])

        when:
        def result = service.verifyOrgUpload(fileInfo, esConfig)

        then:
        result
    }

    void "updateRelationshipErrors should return true if no users for invalid relationships are found"() {
        setup:
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, int start, int size, String[] includedFields = null, QueryBuilder query = null ->
            return []
        }
        List<String> externalEmployeeCodes = []
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.PROCESSING.value, 21, [])

        when:
        def result = service.updateRelationshipErrors(esConfig, fileInfo, externalEmployeeCodes, [])

        then:
        result
    }

    void "updateRelationshipErrors should return false if bulk update fails"() {
        setup:
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, int start, int size, String[] includedFields = null, QueryBuilder query = null ->
            return [
                    [_source: [
                            raw        : [
                                    externalEmployeeCode: 'asdf',
                            ],
                            validation : [errors: [], warnings: [], duplicates: []],
                            differences: []
                    ]]
            ]
        }
        service.elasticSearchService.metaClass.bulkUpdateRows = { ElasticSearchConfig esConfig, List<ProcessedUserRowDTO> bulkData, OrgUploadStatus step, boolean waitForCompletion ->
            return false //simulate failed bulk update
        }
        List<String> externalEmployeeCodes = ['asdf']
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.PROCESSING.value, 21, [])

        when:
        def result = service.updateRelationshipErrors(esConfig, fileInfo, externalEmployeeCodes, [])

        then:
        !result
    }

    void "updateRelationshipErrors should return true if bulk update succeeds"() {
        setup:
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, int start, int size, String[] includedFields = null, QueryBuilder query = null ->
            return [
                    [_source: [
                            raw        : [
                                    externalEmployeeCode: 'asdf',
                            ],
                            validation : [errors: [], warnings: [], duplicates: []],
                            differences: []
                    ]]
            ]
        }
        service.elasticSearchService.metaClass.bulkUpdateRows = { ElasticSearchConfig esConfig, List<ProcessedUserRowDTO> bulkData, OrgUploadStatus step, boolean waitForCompletion ->
            return true //simulate successful bulk update
        }
        List<String> externalEmployeeCodes = ['asdf']
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.PROCESSING.value, 21, [])

        when:
        def result = service.updateRelationshipErrors(esConfig, fileInfo, externalEmployeeCodes, [])

        then:
        result
    }

    void "updateRelationshipWarnings should return true if no users for invalid relationships are found"() {
        setup:
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, int start, int size, String[] includedFields = null, QueryBuilder query = null ->
            return []
        }
        List<String> externalEmployeeCodes = []
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.PROCESSING.value, 21, [])

        when:
        def result = service.updateRelationshipWarnings(esConfig, fileInfo, externalEmployeeCodes, [])

        then:
        result
    }

    void "updateRelationshipWarnings should return false if bulk update fails"() {
        setup:
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, int start, int size, String[] includedFields = null, QueryBuilder query = null ->
            return [
                    [_source: [
                            raw        : [
                                    externalEmployeeCode: 'asdf',
                            ],
                            validation : [errors: [], warnings: [], duplicates: []],
                            differences: []
                    ]]
            ]
        }
        service.elasticSearchService.metaClass.bulkUpdateRows = { ElasticSearchConfig esConfig, List<ProcessedUserRowDTO> bulkData, OrgUploadStatus step, boolean waitForCompletion ->
            return false //simulate failed bulk update
        }
        List<String> externalEmployeeCodes = ['asdf']
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.PROCESSING.value, 21, [])

        when:
        def result = service.updateRelationshipWarnings(esConfig, fileInfo, externalEmployeeCodes, [])

        then:
        !result
    }

    void "updateRelationshipWarnings should return true if bulk update succeeds"() {
        setup:
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, int start, int size, String[] includedFields = null, QueryBuilder query = null ->
            return [
                    [_source: [
                            raw        : [
                                    externalEmployeeCode: 'asdf',
                            ],
                            validation : [errors: [], warnings: [], duplicates: []],
                            differences: []
                    ]]
            ]
        }
        service.elasticSearchService.metaClass.bulkUpdateRows = { ElasticSearchConfig esConfig, List<ProcessedUserRowDTO> bulkData, OrgUploadStatus step, boolean waitForCompletion ->
            return true //simulate successful bulk update
        }
        List<String> externalEmployeeCodes = ['asdf']
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload', new Date().getTime(), "55", '', 'filename.csv', OrgUploadStatus.PROCESSING.value, 21, [])

        when:
        def result = service.updateRelationshipWarnings(esConfig, fileInfo, externalEmployeeCodes, [])

        then:
        result
    }

    void "findInvalidUserRelationships should return [] if no circular references are found"() {
        setup:
        //valid single branch relationships with user4 at top of tree branch and user1 at bottom
        Map<String, String> userRelationships = [
                user1: 'user2',
                user2: 'user3',
                user3: 'user4'
        ]

        when:
        def invalidUsers = service.findInvalidUserRelationships(userRelationships)

        then:
        invalidUsers.size() == 0
    }

    void "findInvalidUserRelationships should return users that are assigned to themselves as managers"() {
        setup:
        //user 1 and user 3 have user4 as manager (valid) but user2 is assigned to themself (invalid)
        Map<String, String> userRelationships = [
                user1: 'user4',
                user2: 'user2', //self
                user3: 'user4',
                user4: 'user2'
        ]

        when:
        def invalidUsers = service.findInvalidUserRelationships(userRelationships)

        then:
        invalidUsers.size() == 1
        !invalidUsers.contains("user1")
        invalidUsers.contains("user2")
        !invalidUsers.contains("user3")
        !invalidUsers.contains("user4")
    }

    void "findInvalidUserRelationships should return all users in the circular reference if found"() {
        setup:
        //invalid circular relationship. 1,2,3,1,2,3.... forever. user 4 and 5 are fine though
        Map<String, String> userRelationships = [
                user1: 'user2',
                user2: 'user3',
                user3: 'user1',
                user4: 'user5'
        ]

        when:
        def invalidUsers = service.findInvalidUserRelationships(userRelationships)

        then:
        invalidUsers.size() == 3
        invalidUsers.containsAll("user1", "user2", "user3")
        !invalidUsers.contains("user4")
        !invalidUsers.contains("user5")
    }

    void "findInvalidUserRelationships should not report any users who are in a branch just shorter than the max branch length allowed if valid otherwise"() {
        setup: //create a single branch that is almost too long
        Map<String, String> userRelationships = [:]
        def user = "user${0}".toString()
        (0..49).each { i ->
            def manager = "user${i + 1}".toString()
            userRelationships[user] = manager
            user = manager
        }

        when:
        def invalidUsers = service.findInvalidUserRelationships(userRelationships)

        then:
        !invalidUsers
    }

    void "findInvalidUserRelationships should report all users who are in a branch longer than the max branch length allowed"() {
        setup: //create a single branch that is too long
        Map<String, String> userRelationships = [:]
        def user = "user${0}".toString()
        (0..50).each { i ->
            def manager = "user${i + 1}".toString()
            userRelationships[user] = manager
            user = manager
        }

        when:
        def invalidUsers = service.findInvalidUserRelationships(userRelationships)

        then:
        invalidUsers
        invalidUsers?.size() == 51
    }

}
