package tbservice

import com.google.common.util.concurrent.RateLimiter
import grails.buildtestdata.BuildDataTest
import grails.gorm.transactions.Rollback
import grails.gorm.transactions.Transactional
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.elasticsearch.index.query.QueryBuilder
import org.grails.orm.hibernate.HibernateDatastore
import org.hibernate.SessionFactory
import org.springframework.transaction.PlatformTransactionManager
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import tbservice.dto.org.FileInfo
import tbservice.dto.org.ProcessedUserRowDTO
import tbservice.enums.OrgUploadStatus
import tbservice.enums.UserStatus

class OrgUploadPublishServiceSpec extends Specification implements ServiceUnitTest<OrgUploadPublishService>, DataTest {

    def esConfig = new ElasticSearchConfig("endpoint", "index")
    RateLimiter auth0RateLimiter = RateLimiter.create(10)

    def setup() {
    }

    def cleanup() {
    }

    def setupSpec() {
        mockDomain User
        mockDomain UserRelationship
        mockDomain ClientSetup
        mockDomain RoleGroup
    }

    void "publishOrgUpload should return false if publishing users fail"() {
        setup:
        service.metaClass.publishUsers = { FileInfo fileInfo, ElasticSearchConfig esConfig ->
            return false //fail
        }
        service.metaClass.updateUserManagerCoreIds = { FileInfo fileInfo, ElasticSearchConfig esConfig ->
            return true
        }
        service.metaClass.publishUserRelationships = { FileInfo fileInfo, ElasticSearchConfig esConfig ->
            return true
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload-publish', new Date().getTime(), "55", 'gfgfdg', 'filename.csv', OrgUploadStatus.PUBLISHING.value, 21, [])

        when:
        def result = service.publishOrgUpload(fileInfo, esConfig)

        then:
        !result
    }

    void "publishOrgUpload should return false if updating user manager core ids fail"() {
        setup:
        service.metaClass.publishUsers = { FileInfo fileInfo, ElasticSearchConfig esConfig ->
            return true
        }
        service.metaClass.updateUserManagerCoreIds = { FileInfo fileInfo, ElasticSearchConfig esConfig ->
            return false //fail
        }
        service.metaClass.publishUserRelationships = { FileInfo fileInfo, ElasticSearchConfig esConfig ->
            return true
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload-publish', new Date().getTime(), "55", 'gfgfdg', 'filename.csv', OrgUploadStatus.PUBLISHING.value, 21, [])

        when:
        def result = service.publishOrgUpload(fileInfo, esConfig)

        then:
        !result
    }

    void "publishOrgUpload should return false if updating user relationships fail"() {
        setup:
        service.metaClass.publishUsers = { FileInfo fileInfo, ElasticSearchConfig esConfig ->
            return true
        }
        service.metaClass.updateUserManagerCoreIds = { FileInfo fileInfo, ElasticSearchConfig esConfig ->
            return true
        }
        service.metaClass.publishUserRelationships = { FileInfo fileInfo, ElasticSearchConfig esConfig ->
            return false //fail
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload-publish', new Date().getTime(), "55", 'gfgfdg', 'filename.csv', OrgUploadStatus.PUBLISHING.value, 21, [])

        when:
        def result = service.publishOrgUpload(fileInfo, esConfig)

        then:
        !result
    }

    void "publishOrgUpload should return true if all steps succeed"() {
        setup:
        service.metaClass.publishUsers = { FileInfo fileInfo, ElasticSearchConfig esConfig ->
            return true
        }
        service.metaClass.updateUserManagerCoreIds = { FileInfo fileInfo, ElasticSearchConfig esConfig ->
            return true
        }
        service.metaClass.publishUserRelationships = { FileInfo fileInfo, ElasticSearchConfig esConfig ->
            return true
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload-publish', new Date().getTime(), "55", 'gfgfdg', 'filename.csv', OrgUploadStatus.PUBLISHING.value, 21, [])

        when:
        def result = service.publishOrgUpload(fileInfo, esConfig)

        then:
        result
    }

    void "publishUsers should fail ClientSetup cannot be found"() {
        setup:
        service.fileInfoService = new FileInfoService()
        service.fileInfoService.metaClass.saveFileInfo = { FileInfo fileInfo ->
            return true
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, int start, int size, String[] includedFields = null, QueryBuilder query = null ->
            return [
                    [_id    : "1",
                     _source: [
                             raw     : [
                                     email               : 'asdf@gmail.com',
                                     externalEmployeeCode: 'asdf',
                                     externalPersonId    : 'asdf1',
                                     firstName           : 'as',
                                     jobTitle            : 'dev',
                                     lastName            : 'df'
                             ],
                             action  : 'delete',
                             computed: [
                                     userCoreId: "",
                                     mgrCoreId : "",
                                     mgrName   : ""
                             ]
                     ]],
                    [_id    : "2",
                     _source: [
                             raw     : [
                                     email               : 'fdsa@gmail.com',
                                     externalEmployeeCode: 'fdsa',
                                     externalPersonId    : 'fdsa1',
                                     firstName           : 'fd',
                                     jobTitle            : 'dev',
                                     lastName            : 'sa'
                             ],
                             action  : 'update',
                             computed: [
                                     userCoreId: "",
                                     mgrCoreId : "",
                                     mgrName   : ""
                             ]
                     ]],
                    [_id    : "3",
                     _source: [
                             raw     : [
                                     email               : 'qwerty@gmail.com',
                                     externalEmployeeCode: 'qwerty',
                                     externalPersonId    : 'qwerty1',
                                     firstName           : 'qw',
                                     jobTitle            : 'dev',
                                     lastName            : 'erty'
                             ],
                             action  : 'add',
                             computed: [
                                     userCoreId: "",
                                     mgrCoreId : "",
                                     mgrName   : ""
                             ]
                     ]],
            ]
        }
        service.elasticSearchService.metaClass.bulkUpdateRows = { ElasticSearchConfig esConfig, List<ProcessedUserRowDTO> bulkData, OrgUploadStatus step, boolean waitForCompletion ->
            return true
        }
        ClientSetup.metaClass.static.fetchById = { Long clientSetupId ->
            return null //fail
        }
        User.metaClass.static.findAllByClientSetupIdAndExternalEmployeeCodeInList = { String externalEmployeeCode ->
            return [
                    new User(
                            id: 1,
                            active: true,
                            clientSetupId: 55,
                            email: "asdf@gmail.com",
                            externalEmployeeCode: "asdf",
                            externalPersonId: "asdf1",
                            firstName: "as",
                            inAdminResetProcess: false,
                            jobTitle: "dev",
                            lastName: "df",
                            password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                            userStatus: UserStatus.ACTIVE_REGISTERED,
                            username: "asdf@gmail.com"
                    )
            ]
        }
        service.metaClass.disableUser = { User tbcoreUser, def clientSetup, RateLimiter auth0RateLimiter ->
            return tbcoreUser
        }
        service.metaClass.updateUser = { User tbcoreUser, Object esUser, def clientSetup, RateLimiter auth0RateLimiter ->
            return tbcoreUser
        }
        service.metaClass.addUser = { Object esUser, def clientSetup, RateLimiter auth0RateLimiter ->
            return new User(
                    id: 2,
                    active: true,
                    clientSetupId: 55,
                    email: "fdsa@gmail.com",
                    externalEmployeeCode: "fdsa",
                    externalPersonId: "fdsa1",
                    firstName: "fd",
                    inAdminResetProcess: false,
                    jobTitle: "dev",
                    lastName: "sa",
                    password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                    userStatus: UserStatus.ACTIVE_REGISTERED,
                    username: "fdsa@gmail.com"
            )
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload-publish', new Date().getTime(), "55", 'gfgfdg', 'filename.csv', OrgUploadStatus.PUBLISHING.value, 21, [])

        when:
        def result = service.publishUsers(fileInfo, esConfig)

        then:
        !result
    }

    void "publishUsers return immediately if no users are found to publish"() {
        setup:
        service.fileInfoService = new FileInfoService()
        service.fileInfoService.metaClass.saveFileInfo = { FileInfo fileInfo ->
            return true
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, int start, int size, String[] includedFields = null, QueryBuilder query = null ->
            return [] //early return
        }
        service.elasticSearchService.metaClass.bulkUpdateRows = { ElasticSearchConfig esConfig, List<ProcessedUserRowDTO> bulkData, OrgUploadStatus step, boolean waitForCompletion ->
            return true
        }
        ClientSetup.metaClass.static.fetchById = { Long clientSetupId ->
            return [id: 55, companyCode: 'tp', customIdP: false]
        }
        User.metaClass.static.findAllByClientSetupIdAndExternalEmployeeCodeInList = { String externalEmployeeCode ->
            return [
                    new User(
                            id: 1,
                            active: true,
                            clientSetupId: 55,
                            email: "asdf@gmail.com",
                            externalEmployeeCode: "asdf",
                            externalPersonId: "asdf1",
                            firstName: "as",
                            inAdminResetProcess: false,
                            jobTitle: "dev",
                            lastName: "df",
                            password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                            userStatus: UserStatus.ACTIVE_REGISTERED,
                            username: "asdf@gmail.com"
                    )
            ]
        }
        service.metaClass.disableUser = { User tbcoreUser, def clientSetup, RateLimiter auth0RateLimiter ->
            return tbcoreUser
        }
        service.metaClass.updateUser = { User tbcoreUser, Object esUser, def clientSetup, RateLimiter auth0RateLimiter ->
            return tbcoreUser
        }
        service.metaClass.addUser = { Object esUser, def clientSetup, RateLimiter auth0RateLimiter ->
            return new User(
                    id: 2,
                    active: true,
                    clientSetupId: 55,
                    email: "fdsa@gmail.com",
                    externalEmployeeCode: "fdsa",
                    externalPersonId: "fdsa1",
                    firstName: "fd",
                    inAdminResetProcess: false,
                    jobTitle: "dev",
                    lastName: "sa",
                    password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                    userStatus: UserStatus.ACTIVE_REGISTERED,
                    username: "fdsa@gmail.com"
            )
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload-publish', new Date().getTime(), "55", 'gfgfdg', 'filename.csv', OrgUploadStatus.PUBLISHING.value, 21, [])

        when:
        def result = service.publishUsers(fileInfo, esConfig)

        then:
        result
    }

    void "publishUsers should fail if bulk update fails"() {
        setup:
        service.fileInfoService = new FileInfoService()
        service.fileInfoService.metaClass.saveFileInfo = { FileInfo fileInfo ->
            return true
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, int start, int size, String[] includedFields = null, QueryBuilder query = null ->
            return [
                    [_id    : "1",
                     _source: [
                             raw     : [
                                     email               : 'asdf@gmail.com',
                                     externalEmployeeCode: 'asdf',
                                     externalPersonId    : 'asdf1',
                                     firstName           : 'as',
                                     jobTitle            : 'dev',
                                     lastName            : 'df'
                             ],
                             action  : 'delete',
                             computed: [
                                     userCoreId: "",
                                     mgrCoreId : "",
                                     mgrName   : ""
                             ]
                     ]],
                    [_id    : "2",
                     _source: [
                             raw     : [
                                     email               : 'fdsa@gmail.com',
                                     externalEmployeeCode: 'fdsa',
                                     externalPersonId    : 'fdsa1',
                                     firstName           : 'fd',
                                     jobTitle            : 'dev',
                                     lastName            : 'sa'
                             ],
                             action  : 'update',
                             computed: [
                                     userCoreId: "",
                                     mgrCoreId : "",
                                     mgrName   : ""
                             ]
                     ]],
                    [_id    : "3",
                     _source: [
                             raw     : [
                                     email               : 'qwerty@gmail.com',
                                     externalEmployeeCode: 'qwerty',
                                     externalPersonId    : 'qwerty1',
                                     firstName           : 'qw',
                                     jobTitle            : 'dev',
                                     lastName            : 'erty'
                             ],
                             action  : 'add',
                             computed: [
                                     userCoreId: "",
                                     mgrCoreId : "",
                                     mgrName   : ""
                             ]
                     ]],
            ]
        }
        service.elasticSearchService.metaClass.bulkUpdateRows = { ElasticSearchConfig esConfig, List<ProcessedUserRowDTO> bulkData, OrgUploadStatus step, boolean waitForCompletion ->
            return false //fail
        }
        ClientSetup.metaClass.static.fetchById = { Long clientSetupId ->
            return [id: 55, companyCode: 'tp', customIdP: false]
        }
        User.metaClass.static.findAllByClientSetupIdAndExternalEmployeeCodeInList = { String externalEmployeeCode ->
            return [
                    new User(
                            id: 1,
                            active: true,
                            clientSetupId: 55,
                            email: "asdf@gmail.com",
                            externalEmployeeCode: "asdf",
                            externalPersonId: "asdf1",
                            firstName: "as",
                            inAdminResetProcess: false,
                            jobTitle: "dev",
                            lastName: "df",
                            password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                            userStatus: UserStatus.ACTIVE_REGISTERED,
                            username: "asdf@gmail.com"
                    )
            ]
        }
        service.metaClass.disableUser = { User tbcoreUser, def clientSetup, RateLimiter auth0RateLimiter ->
            return tbcoreUser
        }
        service.metaClass.updateUser = { User tbcoreUser, Object esUser, def clientSetup, RateLimiter auth0RateLimiter ->
            return tbcoreUser
        }
        service.metaClass.addUser = { Object esUser, def clientSetup, RateLimiter auth0RateLimiter ->
            return new User(
                    id: 2,
                    active: true,
                    clientSetupId: 55,
                    email: "fdsa@gmail.com",
                    externalEmployeeCode: "fdsa",
                    externalPersonId: "fdsa1",
                    firstName: "fd",
                    inAdminResetProcess: false,
                    jobTitle: "dev",
                    lastName: "sa",
                    password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                    userStatus: UserStatus.ACTIVE_REGISTERED,
                    username: "fdsa@gmail.com"
            )
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload-publish', new Date().getTime(), "55", 'gfgfdg', 'filename.csv', OrgUploadStatus.PUBLISHING.value, 21, [])

        when:
        def result = service.publishUsers(fileInfo, esConfig)

        then:
        !result
    }

    void "publishUsers should succeed if everything works"() {
        setup:
        service.fileInfoService = new FileInfoService()
        service.fileInfoService.metaClass.saveFileInfo = { FileInfo fileInfo ->
            return true
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, int start, int size, String[] includedFields = null, QueryBuilder query = null ->
            if (start == 0) {
                return [
                        [_id    : "1",
                         _source: [
                                 raw          : [
                                         email               : 'asdf@gmail.com',
                                         externalEmployeeCode: 'asdf',
                                         externalPersonId    : 'asdf1',
                                         firstName           : 'as',
                                         jobTitle            : 'dev',
                                         lastName            : 'df'
                                 ],
                                 action       : 'delete',
                                 computed     : [
                                         userCoreId: "",
                                         mgrCoreId : "",
                                         mgrName   : ""
                                 ],
                                 "uniqueDocId": 1
                         ]],
                        [_id    : "2",
                         _source: [
                                 raw          : [
                                         email               : 'fdsa@gmail.com',
                                         externalEmployeeCode: 'fdsa',
                                         externalPersonId    : 'fdsa1',
                                         firstName           : 'fd',
                                         jobTitle            : 'dev',
                                         lastName            : 'sa'
                                 ],
                                 action       : 'update',
                                 computed     : [
                                         userCoreId: "",
                                         mgrCoreId : "",
                                         mgrName   : ""
                                 ],
                                 "uniqueDocId": 2
                         ]],
                        [_id    : "3",
                         _source: [
                                 raw          : [
                                         email               : 'qwerty@gmail.com',
                                         externalEmployeeCode: 'qwerty',
                                         externalPersonId    : 'qwerty1',
                                         firstName           : 'qw',
                                         jobTitle            : 'dev',
                                         lastName            : 'erty'
                                 ],
                                 action       : 'add',
                                 computed     : [
                                         userCoreId: "",
                                         mgrCoreId : "",
                                         mgrName   : ""
                                 ],
                                 "uniqueDocId": 3
                         ]],
                ]
            }
            else {
                return []
            }
        }
        service.elasticSearchService.metaClass.bulkUpdateRows = { ElasticSearchConfig esConfig, List<ProcessedUserRowDTO> bulkData, OrgUploadStatus step, boolean waitForCompletion ->
            return true
        }
        ClientSetup.metaClass.static.fetchById = { Long clientSetupId ->
            return [id: 55, companyCode: 'tp', customIdP: false]
        }
        User.metaClass.static.findAllByClientSetupIdAndExternalEmployeeCodeInList = { String externalEmployeeCode ->
            return [
                    new User(
                            id: 1,
                            active: true,
                            clientSetupId: 55,
                            email: "asdf@gmail.com",
                            externalEmployeeCode: "asdf",
                            externalPersonId: "asdf1",
                            firstName: "as",
                            inAdminResetProcess: false,
                            jobTitle: "dev",
                            lastName: "df",
                            password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                            userStatus: UserStatus.ACTIVE_REGISTERED,
                            username: "asdf@gmail.com"
                    )
            ]
        }
        service.metaClass.disableUser = { User tbcoreUser, def clientSetup, RateLimiter auth0RateLimiter ->
            return tbcoreUser
        }
        service.metaClass.updateUser = { User tbcoreUser, Object esUser, def clientSetup, RateLimiter auth0RateLimiter ->
            return tbcoreUser
        }
        service.metaClass.addUser = { Object esUser, def clientSetup, RateLimiter auth0RateLimiter ->
            return new User(
                    id: 2,
                    active: true,
                    clientSetupId: 55,
                    email: "fdsa@gmail.com",
                    externalEmployeeCode: "fdsa",
                    externalPersonId: "fdsa1",
                    firstName: "fd",
                    inAdminResetProcess: false,
                    jobTitle: "dev",
                    lastName: "sa",
                    password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                    userStatus: UserStatus.ACTIVE_REGISTERED,
                    username: "fdsa@gmail.com"
            )
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload-publish', new Date().getTime(), "55", 'gfgfdg', 'filename.csv', OrgUploadStatus.PUBLISHING.value, 21, [])

        when:
        def result = service.publishUsers(fileInfo, esConfig)

        then:
        result
    }

    void "disableUser should not disable a tbcore user if user has auth0 id auth0 update was not successful"() {
        setup:
        service.auth0Service = new Auth0Service()
        service.auth0Service.metaClass.updateUser = { User user, def clientSetup, def updateEmail = false ->
            return null
        }
        User tbcoreUser = new User(
                active: true,
                clientSetupId: 55,
                email: "asdf@gmail.com",
                externalEmployeeCode: "asdf",
                externalPersonId: "asdf1",
                firstName: "as",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "df",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "asdf@gmail.com",
                auth0userId: 'asdf' //has auth0 id
        )
        tbcoreUser.save(flush: true)
        def clientSetup = [id: 55, companyCode: 'tp', customIdP: false]

        when:
        def result = service.disableUser(tbcoreUser, clientSetup, auth0RateLimiter)

        then:
        !result
    }

    void "disableUser should successfully disable a tbcore user who does not has auth0userId"() {
        setup:
        User tbcoreUser = new User(
                active: true,
                clientSetupId: 55,
                email: "asdf@gmail.com",
                externalEmployeeCode: "asdf",
                externalPersonId: "asdf1",
                firstName: "as",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "df",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "asdf@gmail.com",
                auth0userId: null
        )
        tbcoreUser.save(flush: true)
        def clientSetup = [id: 55, companyCode: 'tp', customIdP: true]

        when:
        def result = service.disableUser(tbcoreUser, clientSetup, auth0RateLimiter)

        then:
        !tbcoreUser.active
        !tbcoreUser.inAdminResetProcess
        tbcoreUser.userStatus == UserStatus.INACTIVE_DISABLED
        result
    }

    void "disableUser should successfully disable a tbcore user has auth0userId and if auth0 update was successful"() {
        setup:
        service.auth0Service = new Auth0Service()
        service.auth0Service.metaClass.updateUser = { User user, def clientSetup, def updateEmail = false ->
            return user
        }
        User tbcoreUser = new User(
                active: true,
                clientSetupId: 55,
                email: "asdf@gmail.com",
                externalEmployeeCode: "asdf",
                externalPersonId: "asdf1",
                firstName: "as",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "df",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "asdf@gmail.com",
                auth0userId: 'asdf'
        )
        tbcoreUser.save(flush: true)
        def clientSetup = [id: 55, companyCode: 'tp', customIdP: false]

        when:
        def result = service.disableUser(tbcoreUser, clientSetup, auth0RateLimiter)

        then:
        !tbcoreUser.active
        !tbcoreUser.inAdminResetProcess
        tbcoreUser.userStatus == UserStatus.INACTIVE_DISABLED
        result
    }

    void "updateUser should not update the user and return false if user has auth0userId and auth0 update was not successful"() {
        setup:
        service.auth0Service = new Auth0Service()
        service.auth0Service.metaClass.updateUser = { User user, def clientSetup, def updateEmail = false ->
            return null //fail
        }
        User tbcoreUser = new User(
                active: true,
                clientSetupId: 55,
                email: "asdf@gmail.com",
                externalEmployeeCode: "asdf",
                externalPersonId: "asdf1",
                firstName: "as",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "df",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "asdf@gmail.com",
                auth0userId: 'asdf' //has id
        )
        tbcoreUser.save(flush: true)
        Object esUser = [
                _id    : '1',
                _source: [
                        raw   : [
                                email               : 'new_asdf@gmail.com',
                                externalEmployeeCode: 'asdf',
                                externalPersonId    : 'new_asdf1',
                                firstName           : 'new_as',
                                jobTitle            : 'new_dev',
                                lastName            : 'new_df'
                        ],
                        action: 'update'
                ]
        ]
        def clientSetup = [:]

        when:
        def result = service.updateUser(tbcoreUser, esUser, clientSetup, auth0RateLimiter)

        then:
        !result
    }

    void "updateUser should update the user and return true if user does not have auth0userId"() {
        setup:
        User tbcoreUser = new User(
                active: true,
                clientSetupId: 55,
                email: "asdf@gmail.com",
                externalEmployeeCode: "asdf",
                externalPersonId: "asdf1",
                firstName: "as",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "df",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "asdf@gmail.com",
                auth0userId: null //has no auth0 id
        )
        tbcoreUser.save(flush: true)
        Object esUser = [
                _id    : '1',
                _source: [
                        raw   : [
                                email               : 'new_asdf@gmail.com',
                                externalEmployeeCode: 'asdf',
                                externalPersonId    : 'new_asdf1',
                                firstName           : 'new_as',
                                jobTitle            : 'new_dev',
                                lastName            : 'new_df'
                        ],
                        action: 'update'
                ]
        ]
        def clientSetup = [id: 55, companyCode: 'tp', customIdP: true]

        service.userAssessmentOrderAccessService = new UserAssessmentOrderAccessService()
        service.userAssessmentOrderAccessService.metaClass.handleAssessmentLink = {User user -> return user}

        when:
        def result = service.updateUser(tbcoreUser, esUser, clientSetup, auth0RateLimiter)

        then:
        tbcoreUser.email == 'new_asdf@gmail.com'
        tbcoreUser.externalPersonId == 'new_asdf1'
        tbcoreUser.firstName == 'new_as'
        tbcoreUser.jobTitle == 'new_dev'
        tbcoreUser.lastName == 'new_df'
        tbcoreUser.username == 'new_asdf@gmail.com'
        result
    }

    void "updateUser should update the user and return true if user has auth0userId and auth0 update was also successful"() {
        setup:
        service.auth0Service = new Auth0Service()
        service.auth0Service.metaClass.updateUser = { User user, def clientSetup, def updateEmail = false ->
            return user
        }
        service.userAssessmentOrderAccessService = new UserAssessmentOrderAccessService()
        service.userAssessmentOrderAccessService.metaClass.handleAssessmentLink = {User user -> return user}
        User tbcoreUser = new User(
                active: true,
                clientSetupId: 55,
                email: "asdf@gmail.com",
                externalEmployeeCode: "asdf",
                externalPersonId: "asdf1",
                firstName: "as",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "df",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "asdf@gmail.com",
                auth0userId: 'asdf' //has id
        )
        tbcoreUser.save(flush: true)
        Object esUser = [
                _id    : '1',
                _source: [
                        raw   : [
                                email               : 'new_asdf@gmail.com',
                                externalEmployeeCode: 'asdf',
                                externalPersonId    : 'new_asdf1',
                                firstName           : 'new_as',
                                jobTitle            : 'new_dev',
                                lastName            : 'new_df'
                        ],
                        action: 'update'
                ]
        ]
        def clientSetup = [:]

        when:
        def result = service.updateUser(tbcoreUser, esUser, clientSetup, auth0RateLimiter)

        then:
        tbcoreUser.email == 'new_asdf@gmail.com'
        tbcoreUser.externalPersonId == 'new_asdf1'
        tbcoreUser.firstName == 'new_as'
        tbcoreUser.jobTitle == 'new_dev'
        tbcoreUser.lastName == 'new_df'
        tbcoreUser.username == 'new_asdf@gmail.com'
        result
    }

    void "addUser should not add the user and return null if client does not have a custom IdP and auth0 add was not successful"() {
        setup:
        service.auth0Service = new Auth0Service()
        service.auth0Service.metaClass.addUser = { User user, def clientSetup, def authGroups = [] ->
            return null //fail
        }
        Object esUser = [
                _id    : '1',
                _source: [
                        raw   : [
                                email               : 'new_asdf@gmail.com',
                                externalEmployeeCode: 'asdf',
                                externalPersonId    : 'new_asdf1',
                                firstName           : 'new_as',
                                jobTitle            : 'new_dev',
                                lastName            : 'new_df'
                        ],
                        action: 'update'
                ]
        ]
        def clientSetup = [id: 55, companyCode: 'tp', customIdP: false] //no custom IdP

        when:
        def result = service.addUser(esUser, clientSetup, auth0RateLimiter)
        def newTBCoreUser = User.get(1)

        then:
        !newTBCoreUser
        !result
    }

    void "addUser should add the user and return user if client does not have a custom IdP and auth0 add was successful"() {
        setup:
        service.auth0Service = new Auth0Service()
        service.auth0Service.metaClass.addUser = { User user, def clientSetup, def authGroups = [] ->
            return user
        }
        service.userAssessmentOrderAccessService = new UserAssessmentOrderAccessService()
        service.userAssessmentOrderAccessService.metaClass.handleAssessmentLink = {User user -> return user}
        Object esUser = [
                _id    : '1',
                _source: [
                        raw   : [
                                email               : 'new_asdf@gmail.com',
                                externalEmployeeCode: 'asdf',
                                externalPersonId    : 'new_asdf1',
                                firstName           : 'new_as',
                                jobTitle            : 'new_dev',
                                lastName            : 'new_df'
                        ],
                        action: 'update'
                ]
        ]
        def clientSetup = [id: 55, companyCode: 'tp', customIdP: false] //no custom IdP

        when:
        def result = service.addUser(esUser, clientSetup, auth0RateLimiter)
        def newTBCoreUser = User.get(1)

        then:
        newTBCoreUser
        result
    }

    void "addUser should add the user and return user if client does have a custom IdP"() {
        setup:
        service.userAssessmentOrderAccessService = new UserAssessmentOrderAccessService()
        service.userAssessmentOrderAccessService.metaClass.handleAssessmentLink = {User user -> return user}
        Object esUser = [
                _id    : '1',
                _source: [
                        raw   : [
                                email               : 'fdsa@gmail.com',
                                externalEmployeeCode: 'fdsa',
                                externalPersonId    : 'fdsa1',
                                firstName           : 'fd',
                                jobTitle            : 'dev',
                                lastName            : 'sa'
                        ],
                        action: 'update'
                ]
        ]
        def clientSetup = [id: 55, companyCode: 'tp', customIdP: true]
        when:
        def result = service.addUser(esUser, clientSetup, auth0RateLimiter)
        def newTBCoreUser = User.get(1)

        then:
        newTBCoreUser.email == 'fdsa@gmail.com'
        newTBCoreUser.externalPersonId == 'fdsa1'
        newTBCoreUser.firstName == 'fd'
        newTBCoreUser.jobTitle == 'dev'
        newTBCoreUser.lastName == 'sa'
        newTBCoreUser.username == 'fdsa@gmail.com'
        result
    }

    void "updateUserManagerCoreIds should return false if bulk update fails"() {
        setup:
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, int start, int size, String[] includedFields = null, QueryBuilder query = null ->
            return [
                    [
                            _id    : "4",
                            _source: [
                                    raw     : [
                                            mgrExternalEmployeeCode: 'asdf'
                                    ],
                                    computed: [
                                            mgrCoreId: ""
                                    ]
                            ]
                    ]
            ]
        }
        User.metaClass.static.findAllIdsByClientSetupIdAndExternalEmployeeCodeInList = { Long clientSetupId, List<String> externalEmployeeCodes, int max = 1_000_000 ->
            return [
                    new User(id: 22, externalEmployeeCode: 'asdf')
            ]
        }
        service.elasticSearchService.metaClass.bulkUpdateRows = { ElasticSearchConfig esConfig, List<ProcessedUserRowDTO> bulkData, OrgUploadStatus step, boolean waitForCompletion ->
            return false //fails
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload-publish', new Date().getTime(), "55", 'gfgfdg', 'filename.csv', OrgUploadStatus.PUBLISHING.value, 21, [])

        when:
        def result = service.updateUserManagerCoreIds(fileInfo, esConfig)

        then:
        !result
    }

    void "updateUserManagerCoreIds should return true if there are no users to update"() {
        setup:
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, int start, int size, String[] includedFields = null, QueryBuilder query = null ->
            return []
        }

        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload-publish', new Date().getTime(), "55", 'gfgfdg', 'filename.csv', OrgUploadStatus.PUBLISHING.value, 21, [])

        when:
        def result = service.updateUserManagerCoreIds(fileInfo, esConfig)

        then:
        result
    }

    void "updateUserManagerCoreIds should return true if all steps are successful"() {
        setup:
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, int start, int size, String[] includedFields = null, QueryBuilder query = null ->
            return [
                    [
                            _id    : "4",
                            _source: [
                                    raw     : [
                                            mgrExternalEmployeeCode: 'asdf'
                                    ],
                                    computed: [
                                            mgrCoreId: ""
                                    ]
                            ]
                    ]
            ]
        }
        User.metaClass.static.findAllIdsByClientSetupIdAndExternalEmployeeCodeInList = { Long clientSetupId, List<String> externalEmployeeCodes, int max = 1_000_000 ->
            return [
                    new User(id: 22, externalEmployeeCode: 'asdf')
            ]
        }
        service.elasticSearchService.metaClass.bulkUpdateRows = { ElasticSearchConfig esConfig, List<ProcessedUserRowDTO> bulkData, OrgUploadStatus step, boolean waitForCompletion ->
            return true
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload-publish', new Date().getTime(), "55", 'gfgfdg', 'filename.csv', OrgUploadStatus.PUBLISHING.value, 21, [])

        when:
        def result = service.updateUserManagerCoreIds(fileInfo, esConfig)

        then:
        result
    }

    void "publishUserRelationships should return true if there are no user relationships to publish"() {
        setup:
        UserRelationship.metaClass.static.findAllByClientSetupIdAsIdMap = { Long clientSetupId ->
            return [:]
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchAllInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, String[] includedFields = null, QueryBuilder query = null ->
            return []
        }
        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload-publish', new Date().getTime(), "55", 'gfgfdg', 'filename.csv', OrgUploadStatus.PUBLISHING.value, 21, [])


        when:
        def result = service.publishUserRelationships(fileInfo, esConfig)

        then:
        result
    }

    void "publishUserRelationships should return false if updating updateUserRelationshipsFromMap fails"() {
        setup:
        UserRelationship.metaClass.static.findAllByClientSetupIdAsIdMap = { Long clientSetupId ->
            return [
                    3: 2,
                    2: 1
            ]
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchAllInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, String[] includedFields = null, QueryBuilder query = null ->
            return [
                    [
                            _source: [
                                    action  : 'add',
                                    computed: [
                                            userCoreId: 4,
                                            mgrCoreId : 3
                                    ]
                            ]
                    ]
            ]
        }
        service.metaClass.updateUserRelationshipsFromMap = { Map unionUserRelationshipIdMap ->
            return false
        }
        service.metaClass.publishUserRelationshipReassignments = { FileInfo fileInfo, ElasticSearchConfig esConfig, Map<Long, Long> unionMap ->
            return true
        }

        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload-publish', new Date().getTime(), "55", 'gfgfdg', 'filename.csv', OrgUploadStatus.PUBLISHING.value, 21, [])


        when:
        def result = service.publishUserRelationships(fileInfo, esConfig)

        then:
        !result
    }

    void "publishUserRelationships should return false if updating publishUserRelationshipReassignments fails"() {
        setup:
        UserRelationship.metaClass.static.findAllByClientSetupIdAsIdMap = { Long clientSetupId ->
            return [
                    3: 2,
                    2: 1
            ]
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchAllInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, String[] includedFields = null, QueryBuilder query = null ->
            return [
                    [
                            _source: [
                                    action  : 'add',
                                    computed: [
                                            userCoreId: 4,
                                            mgrCoreId : 3
                                    ]
                            ]
                    ]
            ]
        }
        service.metaClass.updateUserRelationshipsFromMap = { Map unionUserRelationshipIdMap ->
            return true
        }
        service.metaClass.publishUserRelationshipReassignments = { FileInfo fileInfo, ElasticSearchConfig esConfig, Map<Long, Long> unionMap ->
            return false
        }

        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload-publish', new Date().getTime(), "55", 'gfgfdg', 'filename.csv', OrgUploadStatus.PUBLISHING.value, 21, [])


        when:
        def result = service.publishUserRelationships(fileInfo, esConfig)

        then:
        !result
    }

    void "publishUserRelationships should return true if all steps succeed"() {
        setup:
        UserRelationship.metaClass.static.findAllByClientSetupIdAsIdMap = { Long clientSetupId ->
            return [
                    3: 2,
                    2: 1
            ]
        }
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchAllInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, String[] includedFields = null, QueryBuilder query = null ->
            return [
                    [
                            _source: [
                                    action  : 'add',
                                    computed: [
                                            userCoreId: 4,
                                            mgrCoreId : 3
                                    ]
                            ]
                    ]
            ]
        }
        service.metaClass.updateUserRelationshipsFromMap = { Map unionUserRelationshipIdMap ->
            return true
        }
        service.metaClass.publishUserRelationshipReassignments = { FileInfo fileInfo, ElasticSearchConfig esConfig, Map<Long, Long> unionMap ->
            return true
        }

        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload-publish', new Date().getTime(), "55", 'gfgfdg', 'filename.csv', OrgUploadStatus.PUBLISHING.value, 21, [])

        when:
        def result = service.publishUserRelationships(fileInfo, esConfig)

        then:
        result
    }

    void "updateUserRelationshipsFromMap should not overwrite old existing user relationship for a user if new manager is disabled"() {
        setup:
        User user1 = new User(
                active: true,
                clientSetupId: 55,
                email: "asdf@gmail.com",
                externalEmployeeCode: "asdf",
                externalPersonId: "asdf1",
                firstName: "as",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "df",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "asdf@gmail.com",
                auth0userId: 'asdf'
        )
        User user2 = new User(
                active: true,
                clientSetupId: 55,
                email: "fdsa@gmail.com",
                externalEmployeeCode: "fdsa",
                externalPersonId: "fdsa1",
                firstName: "fd",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "sa",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "fdsa@gmail.com",
                auth0userId: 'fdsa'
        )
        User user3 = new User(
                active: false,
                clientSetupId: 55,
                email: "qwerty@gmail.com",
                externalEmployeeCode: "qwerty",
                externalPersonId: "qwerty1",
                firstName: "qwe",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "rty",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.INACTIVE_DISABLED,
                username: "qwerty@gmail.com",
                auth0userId: 'qwerty'
        )

        user1.save(flush: true)
        user2.save(flush: true)
        user3.save(flush: true)

        //old relationship
        UserRelationship.create(1, 2)

        //new user relationship for user1 specified in union map
        Map<Long, Long> unionMap = [1: 3]

        when:
        service.updateUserRelationshipsFromMap(unionMap)

        then:
        UserRelationship.findByUser(User.get(1))?.managerId == 2
    }

    void "updateUserRelationshipsFromMap should overwrite old existing user relationship for a user"() {
        setup:
        User user1 = new User(
                active: true,
                clientSetupId: 55,
                email: "asdf@gmail.com",
                externalEmployeeCode: "asdf",
                externalPersonId: "asdf1",
                firstName: "as",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "df",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "asdf@gmail.com",
                auth0userId: 'asdf'
        )
        User user2 = new User(
                active: true,
                clientSetupId: 55,
                email: "fdsa@gmail.com",
                externalEmployeeCode: "fdsa",
                externalPersonId: "fdsa1",
                firstName: "fd",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "sa",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "fdsa@gmail.com",
                auth0userId: 'fdsa'
        )
        User user3 = new User(
                active: true,
                clientSetupId: 55,
                email: "qwerty@gmail.com",
                externalEmployeeCode: "qwerty",
                externalPersonId: "qwerty1",
                firstName: "qwe",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "rty",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "qwerty@gmail.com",
                auth0userId: 'qwerty'
        )

        user1.save(flush: true)
        user2.save(flush: true)
        user3.save(flush: true)

        //old relationship
        UserRelationship.create(1, 2)

        //new user relationship for user1 specified in union map
        Map<Long, Long> unionMap = [1: 3]

        when:
        service.updateUserRelationshipsFromMap(unionMap)

        then:
        UserRelationship.findByUser(User.get(1))?.managerId == 3
    }

    void "publishUserRelationshipReassignments should reassign inactive manager relationships with active upline manager relationships"() {
        setup:
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchAllInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, String[] includedFields = null, QueryBuilder query = null ->
            return [
                    [
                            _source: [
                                    computed: [
                                            userCoreId: 2

                                    ]
                            ]
                    ]
            ]
        }

        User user1 = new User(
                active: true,
                clientSetupId: 55,
                email: "asdf@gmail.com",
                externalEmployeeCode: "asdf",
                externalPersonId: "asdf1",
                firstName: "as",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "df",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "asdf@gmail.com",
                auth0userId: 'asdf'
        )
        //inactive
        User user2 = new User(
                active: false,
                clientSetupId: 55,
                email: "fdsa@gmail.com",
                externalEmployeeCode: "fdsa",
                externalPersonId: "fdsa1",
                firstName: "fd",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "sa",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.INACTIVE_DISABLED,
                username: "fdsa@gmail.com",
                auth0userId: 'fdsa'
        )
        User user3 = new User(
                active: true,
                clientSetupId: 55,
                email: "qwerty@gmail.com",
                externalEmployeeCode: "qwerty",
                externalPersonId: "qwerty1",
                firstName: "qwe",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "rty",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "qwerty@gmail.com",
                auth0userId: 'qwerty'
        )
        user1.save(flush: true)
        user2.save(flush: true)
        user3.save(flush: true)

        //old relationship
        UserRelationship.create(1, 2)

        //user one reports to user 2 who is disabled, user 2 reports to user 3 who is upline and active
        Map<Long, Long> unionMap = [1l: 2l, 2l: 3l]

        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload-publish', new Date().getTime(), "55", 'gfgfdg', 'filename.csv', OrgUploadStatus.PUBLISHING.value, 21, [])

        when:
        def result = service.publishUserRelationshipReassignments(fileInfo, esConfig, unionMap)
        def newUserRelationship = UserRelationship.findByUser(user1)

        then:
        result
        newUserRelationship.managerId == 3
        newUserRelationship.manager.userStatus == UserStatus.ACTIVE_REGISTERED
    }

    void "publishUserRelationshipReassignments look all the way up the tree to replace inactive manager relationships with active upline manager relationship"() {
        setup:
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchAllInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, String[] includedFields = null, QueryBuilder query = null ->
            return [
                    [
                            _source: [
                                    computed: [
                                            userCoreId: 2

                                    ]
                            ]
                    ]
            ]
        }

        User user1 = new User(
                active: true,
                clientSetupId: 55,
                email: "asdf@gmail.com",
                externalEmployeeCode: "asdf",
                externalPersonId: "asdf1",
                firstName: "as",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "df",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "asdf@gmail.com",
                auth0userId: 'asdf'
        )
        //inactive
        User user2 = new User(
                active: false,
                clientSetupId: 55,
                email: "fdsa@gmail.com",
                externalEmployeeCode: "fdsa",
                externalPersonId: "fdsa1",
                firstName: "fd",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "sa",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.INACTIVE_DISABLED,
                username: "fdsa@gmail.com",
                auth0userId: 'fdsa'
        )
        User user3 = new User(
                active: false,
                clientSetupId: 55,
                email: "qwerty@gmail.com",
                externalEmployeeCode: "qwerty",
                externalPersonId: "qwerty1",
                firstName: "qwe",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "rty",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.INACTIVE_DISABLED,
                username: "qwerty@gmail.com",
                auth0userId: 'qwerty'
        )
        User user4 = new User(
                active: true,
                clientSetupId: 55,
                email: "ytrewq@gmail.com",
                externalEmployeeCode: "ytrewq",
                externalPersonId: "ytrewq1",
                firstName: "ytr",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "ewq",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "ytrewq@gmail.com",
                auth0userId: 'ytrewq'
        )
        user1.save(flush: true)
        user2.save(flush: true)
        user3.save(flush: true)
        user4.save(flush: true)

        //old relationship
        UserRelationship.create(1, 2)

        //user one reports to user 2 who is disabled, user 2 reports to user 3 who is also disabled, user 3 reports to user 4 who is upline and active
        Map<Long, Long> unionMap = [1l: 2l, 2l: 3l, 3l: 4l]

        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload-publish', new Date().getTime(), "55", 'gfgfdg', 'filename.csv', OrgUploadStatus.PUBLISHING.value, 21, [])

        when:
        def result = service.publishUserRelationshipReassignments(fileInfo, esConfig, unionMap)
        def newUserRelationship = UserRelationship.findByUser(user1)

        then:
        result
        newUserRelationship.managerId == 4
        newUserRelationship.manager.userStatus == UserStatus.ACTIVE_REGISTERED
    }

    void "publishUserRelationshipReassignments should set upline replacement to null if all upline managers are inactive"() {
        setup:
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchAllInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, String[] includedFields = null, QueryBuilder query = null ->
            return [
                    [
                            _source: [
                                    computed: [
                                            userCoreId: 2

                                    ]
                            ]
                    ]
            ]
        }

        User user1 = new User(
                active: true,
                clientSetupId: 55,
                email: "asdf@gmail.com",
                externalEmployeeCode: "asdf",
                externalPersonId: "asdf1",
                firstName: "as",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "df",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "asdf@gmail.com",
                auth0userId: 'asdf'
        )
        //inactive
        User user2 = new User(
                active: false,
                clientSetupId: 55,
                email: "fdsa@gmail.com",
                externalEmployeeCode: "fdsa",
                externalPersonId: "fdsa1",
                firstName: "fd",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "sa",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.INACTIVE_DISABLED,
                username: "fdsa@gmail.com",
                auth0userId: 'fdsa'
        )
        User user3 = new User(
                active: false,
                clientSetupId: 55,
                email: "qwerty@gmail.com",
                externalEmployeeCode: "qwerty",
                externalPersonId: "qwerty1",
                firstName: "qwe",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "rty",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.INACTIVE_DISABLED,
                username: "qwerty@gmail.com",
                auth0userId: 'qwerty'
        )
        user1.save(flush: true)
        user2.save(flush: true)
        user3.save(flush: true)

        //old relationship
        UserRelationship.create(1, 2)

        //users 2 and 3 are inactive
        Map<Long, Long> unionMap = [1l: 2l, 2l: 3l]

        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload-publish', new Date().getTime(), "55", 'gfgfdg', 'filename.csv', OrgUploadStatus.PUBLISHING.value, 21, [])

        when:
        def result = service.publishUserRelationshipReassignments(fileInfo, esConfig, unionMap)
        def newUserRelationship = UserRelationship.findByUser(user1)

        then:
        result
        !newUserRelationship
    }

    void "publishUserRelationshipReassignments should set upline replacement to null if supplied union map with circular relationship"() {
        setup:
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.metaClass.searchAllInIndex = { ElasticSearchConfig esConfig, FileInfo fileInfo, String[] includedFields = null, QueryBuilder query = null ->
            return [
                    [
                            _source: [
                                    computed: [
                                            userCoreId: 2

                                    ]
                            ]
                    ]
            ]
        }

        User user1 = new User(
                active: true,
                clientSetupId: 55,
                email: "asdf@gmail.com",
                externalEmployeeCode: "asdf",
                externalPersonId: "asdf1",
                firstName: "as",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "df",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "asdf@gmail.com",
                auth0userId: 'asdf'
        )
        //inactive
        User user2 = new User(
                active: false,
                clientSetupId: 55,
                email: "fdsa@gmail.com",
                externalEmployeeCode: "fdsa",
                externalPersonId: "fdsa1",
                firstName: "fd",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "sa",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.INACTIVE_DISABLED,
                username: "fdsa@gmail.com",
                auth0userId: 'fdsa'
        )
        User user3 = new User(
                active: false,
                clientSetupId: 55,
                email: "qwerty@gmail.com",
                externalEmployeeCode: "qwerty",
                externalPersonId: "qwerty1",
                firstName: "qwe",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "rty",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.INACTIVE_DISABLED,
                username: "qwerty@gmail.com",
                auth0userId: 'qwerty'
        )
        user1.save(flush: true)
        user2.save(flush: true)
        user3.save(flush: true)

        //old relationship
        UserRelationship.create(1, 2)

        //circular relationship between inactive users 2 and 3
        Map<Long, Long> unionMap = [1l: 2l, 2l: 3l, 3l: 2l]

        FileInfo fileInfo = new FileInfo("fileId", 1, 'org-upload-publish', new Date().getTime(), "55", 'gfgfdg', 'filename.csv', OrgUploadStatus.PUBLISHING.value, 21, [])

        when:
        def result = service.publishUserRelationshipReassignments(fileInfo, esConfig, unionMap)
        def newUserRelationship = UserRelationship.findByUser(user1)

        then:
        result
        !newUserRelationship
    }

    void "findUplineReplacementManagers should find next upline active manager replacements"() {
        setup:
        User user1 = new User(
                active: true,
                clientSetupId: 55,
                email: "asdf@gmail.com",
                externalEmployeeCode: "asdf",
                externalPersonId: "asdf1",
                firstName: "as",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "df",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "asdf@gmail.com",
                auth0userId: 'asdf'
        )
        //inactive
        User user2 = new User(
                active: false,
                clientSetupId: 55,
                email: "fdsa@gmail.com",
                externalEmployeeCode: "fdsa",
                externalPersonId: "fdsa1",
                firstName: "fd",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "sa",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.INACTIVE_DISABLED,
                username: "fdsa@gmail.com",
                auth0userId: 'fdsa'
        )
        User user3 = new User(
                active: true,
                clientSetupId: 55,
                email: "qwerty@gmail.com",
                externalEmployeeCode: "qwerty",
                externalPersonId: "qwerty1",
                firstName: "qwe",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "rty",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "qwerty@gmail.com",
                auth0userId: 'qwerty'
        )
        user1.save(flush: true)
        user2.save(flush: true)
        user3.save(flush: true)

        Map<Long, Long> unionMap = [1l: 2l, 2l: 3l]
        Set<Long> managersBeingRemoved = [2]

        when:
        Map<Long, Long> replacementMap = service.findUplineReplacementManagers(unionMap, managersBeingRemoved)

        then:
        replacementMap == [2l: 3l]
    }

    void "findUplineReplacementManagers should go all the way upline to find next upline active manager replacements"() {
        setup:
        User user1 = new User(
                active: true,
                clientSetupId: 55,
                email: "asdf@gmail.com",
                externalEmployeeCode: "asdf",
                externalPersonId: "asdf1",
                firstName: "as",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "df",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "asdf@gmail.com",
                auth0userId: 'asdf'
        )
        //inactive
        User user2 = new User(
                active: false,
                clientSetupId: 55,
                email: "fdsa@gmail.com",
                externalEmployeeCode: "fdsa",
                externalPersonId: "fdsa1",
                firstName: "fd",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "sa",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.INACTIVE_DISABLED,
                username: "fdsa@gmail.com",
                auth0userId: 'fdsa'
        )
        //inactive
        User user3 = new User(
                active: false,
                clientSetupId: 55,
                email: "qwerty@gmail.com",
                externalEmployeeCode: "qwerty",
                externalPersonId: "qwerty1",
                firstName: "qwe",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "rty",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.INACTIVE_DISABLED,
                username: "qwerty@gmail.com",
                auth0userId: 'qwerty'
        )
        User user4 = new User(
                active: true,
                clientSetupId: 55,
                email: "ytrewq@gmail.com",
                externalEmployeeCode: "ytrewq",
                externalPersonId: "ytrewq1",
                firstName: "ytr",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "ewq",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "ytrewq@gmail.com",
                auth0userId: 'ytrewq'
        )
        user1.save(flush: true)
        user2.save(flush: true)
        user3.save(flush: true)
        user4.save(flush: true)

        Map<Long, Long> unionMap = [1l: 2l, 2l: 3l, 3l: 4l]
        Set<Long> managersBeingRemoved = [2, 3]

        when:
        Map<Long, Long> replacementMap = service.findUplineReplacementManagers(unionMap, managersBeingRemoved)

        then:
        replacementMap == [2l: 4l, 3l: 4l]
    }

    void "findUplineReplacementManagers should return null manager replacements if no active replacements are found"() {
        setup:
        User user1 = new User(
                active: true,
                clientSetupId: 55,
                email: "asdf@gmail.com",
                externalEmployeeCode: "asdf",
                externalPersonId: "asdf1",
                firstName: "as",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "df",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "asdf@gmail.com",
                auth0userId: 'asdf'
        )
        //inactive
        User user2 = new User(
                active: false,
                clientSetupId: 55,
                email: "fdsa@gmail.com",
                externalEmployeeCode: "fdsa",
                externalPersonId: "fdsa1",
                firstName: "fd",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "sa",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.INACTIVE_DISABLED,
                username: "fdsa@gmail.com",
                auth0userId: 'fdsa'
        )
        //inactive
        User user3 = new User(
                active: false,
                clientSetupId: 55,
                email: "qwerty@gmail.com",
                externalEmployeeCode: "qwerty",
                externalPersonId: "qwerty1",
                firstName: "qwe",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "rty",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.INACTIVE_DISABLED,
                username: "qwerty@gmail.com",
                auth0userId: 'qwerty'
        )
        user1.save(flush: true)
        user2.save(flush: true)
        user3.save(flush: true)

        Map<Long, Long> unionMap = [1l: 2l, 2l: 3l]
        Set<Long> managersBeingRemoved = [2, 3]

        when:
        Map<Long, Long> replacementMap = service.findUplineReplacementManagers(unionMap, managersBeingRemoved)

        then:
        replacementMap == [2l: null, 3l: null]
    }

    void "findUplineReplacementManagers should return null manager replacements if circular relationships are passed and not run forever"() {
        setup:
        User user1 = new User(
                active: true,
                clientSetupId: 55,
                email: "asdf@gmail.com",
                externalEmployeeCode: "asdf",
                externalPersonId: "asdf1",
                firstName: "as",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "df",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "asdf@gmail.com",
                auth0userId: 'asdf'
        )
        //inactive
        User user2 = new User(
                active: false,
                clientSetupId: 55,
                email: "fdsa@gmail.com",
                externalEmployeeCode: "fdsa",
                externalPersonId: "fdsa1",
                firstName: "fd",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "sa",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.INACTIVE_DISABLED,
                username: "fdsa@gmail.com",
                auth0userId: 'fdsa'
        )
        //inactive
        User user3 = new User(
                active: false,
                clientSetupId: 55,
                email: "qwerty@gmail.com",
                externalEmployeeCode: "qwerty",
                externalPersonId: "qwerty1",
                firstName: "qwe",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "rty",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.INACTIVE_DISABLED,
                username: "qwerty@gmail.com",
                auth0userId: 'qwerty'
        )
        user1.save(flush: true)
        user2.save(flush: true)
        user3.save(flush: true)

        //circular relationship
        Map<Long, Long> unionMap = [1l: 2l, 2l: 3l, 3l: 2l]
        Set<Long> managersBeingRemoved = [2, 3]

        when:
        Map<Long, Long> replacementMap = service.findUplineReplacementManagers(unionMap, managersBeingRemoved)

        then:
        replacementMap == [2l: null, 3l: null]
    }

    void "removeInactiveManagerUserRelationships should remove inactive manager's relationships"() {
        setup:
        User user1 = new User(
                active: true,
                clientSetupId: 55,
                email: "asdf@gmail.com",
                externalEmployeeCode: "asdf",
                externalPersonId: "asdf1",
                firstName: "as",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "df",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "asdf@gmail.com",
                auth0userId: 'asdf'
        )
        User user2 = new User(
                active: true,
                clientSetupId: 55,
                email: "fdsa@gmail.com",
                externalEmployeeCode: "fdsa",
                externalPersonId: "fdsa1",
                firstName: "fd",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "sa",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "fdsa@gmail.com",
                auth0userId: 'fdsa'
        )
        //inactive
        User user3 = new User(
                active: false,
                clientSetupId: 55,
                email: "qwerty@gmail.com",
                externalEmployeeCode: "qwerty",
                externalPersonId: "qwerty1",
                firstName: "qwe",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "rty",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.INACTIVE_DISABLED,
                username: "qwerty@gmail.com",
                auth0userId: 'qwerty'
        )
        user1.save(flush: true)
        user2.save(flush: true)
        user3.save(flush: true)

        UserRelationship.create(1, 2)
        UserRelationship.create(2, 3)

        Set<Long> managerIdsBeingRemoved = [3]

        when:
        def result = service.removeInactiveManagerUserRelationships(managerIdsBeingRemoved)

        then:
        result
        UserRelationship.findAllByManager(User.get(2)) != null
        !UserRelationship.findAllByManager(User.get(3))
    }

    void "reassignOrphanedUserRelationships should publish the new replacement managers"() {
        setup:
        User user1 = new User(
                active: true,
                clientSetupId: 55,
                email: "asdf@gmail.com",
                externalEmployeeCode: "asdf",
                externalPersonId: "asdf1",
                firstName: "as",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "df",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "asdf@gmail.com",
                auth0userId: 'asdf'
        )
        User user2 = new User(
                active: false,
                clientSetupId: 55,
                email: "fdsa@gmail.com",
                externalEmployeeCode: "fdsa",
                externalPersonId: "fdsa1",
                firstName: "fd",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "sa",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.INACTIVE_DISABLED,
                username: "fdsa@gmail.com",
                auth0userId: 'fdsa'
        )
        User user3 = new User(
                active: true,
                clientSetupId: 55,
                email: "qwerty@gmail.com",
                externalEmployeeCode: "qwerty",
                externalPersonId: "qwerty1",
                firstName: "qwe",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "rty",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "qwerty@gmail.com",
                auth0userId: 'qwerty'
        )
        user1.save(flush: true)
        user2.save(flush: true)
        user3.save(flush: true)

        //old relationships
        UserRelationship.create(1, 2) //inactive manager2
        UserRelationship.create(2, 3)

        Map<Long, Long> userRelationshipsBeingOrphaned = [1l: 2l]
        Map<Long, Long> uplineReplacementManagerIdMap = [2l: 3l]

        when:
        service.reassignOrphanedUserRelationships(userRelationshipsBeingOrphaned, uplineReplacementManagerIdMap)

        then:
        //user 1 gets reassigned to user 3
        UserRelationship.findByUser(User.get(1))?.managerId == 3
    }

    void "reassignOrphanedUserRelationships should not publish new relationship if null upline replacement manager"() {
        setup:
        User user1 = new User(
                active: true,
                clientSetupId: 55,
                email: "asdf@gmail.com",
                externalEmployeeCode: "asdf",
                externalPersonId: "asdf1",
                firstName: "as",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "df",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "asdf@gmail.com",
                auth0userId: 'asdf'
        )
        User user2 = new User(
                active: false,
                clientSetupId: 55,
                email: "fdsa@gmail.com",
                externalEmployeeCode: "fdsa",
                externalPersonId: "fdsa1",
                firstName: "fd",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "sa",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.INACTIVE_DISABLED,
                username: "fdsa@gmail.com",
                auth0userId: 'fdsa'
        )
        User user3 = new User(
                active: true,
                clientSetupId: 55,
                email: "qwerty@gmail.com",
                externalEmployeeCode: "qwerty",
                externalPersonId: "qwerty1",
                firstName: "qwe",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "rty",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "qwerty@gmail.com",
                auth0userId: 'qwerty'
        )
        user1.save(flush: true)
        user2.save(flush: true)
        user3.save(flush: true)

        //old relationships
        UserRelationship.create(1, 2)
        UserRelationship.create(2, 3)

        Map<Long, Long> userRelationshipsBeingOrphaned = [1l: 2l]
        Map<Long, Long> uplineReplacementManagerIdMap = [2l: null] as Map<Long, Long>

        when:
        service.reassignOrphanedUserRelationships(userRelationshipsBeingOrphaned, uplineReplacementManagerIdMap)

        then:
        //user 1 gets reassigned to user 3
        !UserRelationship.findByUser(User.get(1))
    }

    void "reassignOrphanedUserRelationships should not publish new relationship if replacement manager is disabled"() {
        setup:
        User user1 = new User(
                active: true,
                clientSetupId: 55,
                email: "asdf@gmail.com",
                externalEmployeeCode: "asdf",
                externalPersonId: "asdf1",
                firstName: "as",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "df",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.ACTIVE_REGISTERED,
                username: "asdf@gmail.com",
                auth0userId: 'asdf'
        )
        User user2 = new User(
                active: false,
                clientSetupId: 55,
                email: "fdsa@gmail.com",
                externalEmployeeCode: "fdsa",
                externalPersonId: "fdsa1",
                firstName: "fd",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "sa",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.INACTIVE_DISABLED,
                username: "fdsa@gmail.com",
                auth0userId: 'fdsa'
        )
        User user3 = new User(
                active: false,
                clientSetupId: 55,
                email: "qwerty@gmail.com",
                externalEmployeeCode: "qwerty",
                externalPersonId: "qwerty1",
                firstName: "qwe",
                inAdminResetProcess: false,
                jobTitle: "dev",
                lastName: "rty",
                password: "${UUID.randomUUID().toString().take(8)}" + "ABCabc0123",
                userStatus: UserStatus.INACTIVE_DISABLED,
                username: "qwerty@gmail.com",
                auth0userId: 'qwerty'
        )
        user1.save(flush: true)
        user2.save(flush: true)
        user3.save(flush: true)

        //old relationships
        UserRelationship.create(1, 2)
        UserRelationship.create(2, 3)

        Map<Long, Long> userRelationshipsBeingOrphaned = [1l: 2l]
        Map<Long, Long> uplineReplacementManagerIdMap = [2l: 3l]

        when:
        service.reassignOrphanedUserRelationships(userRelationshipsBeingOrphaned, uplineReplacementManagerIdMap)

        then:
        //user 1 gets reassigned to user 3
        !UserRelationship.findByUser(User.get(1))
    }

    def "should_return_role_groups_from_passed_string"() {
        setup:
        def SUPER_ADMIN= new RoleGroup(name: 'tbcore-rg-admin-super', displayName: 'SUPER ADMIN', master: true, talentPlusOnly: true, administrator: true, rank: 1).save(flush: true)
        def TALENT_PLUS_ADMIN= new RoleGroup(name: 'tbcore-rg-admin-tp', displayName: 'TALENT PLUS ADMIN', master: true, talentPlusOnly: true, administrator: true, rank: 2).save(flush: true)
        def TALENTBANK_CLIENT_ADMIN= new RoleGroup(name: 'tbcore-rg-admin-client', displayName: 'TALENTBANK CLIENT ADMIN', master: true, talentPlusOnly: false, administrator: true, rank: 3).save(flush: true)
        def HR_MANAGER= new RoleGroup(name: 'tbcore-rg-manager', displayName: 'HR MANAGER', master: true, talentPlusOnly: false, administrator: false).save(flush: true)
        def REQUESTOR= new RoleGroup(name: 'tbcore-rg-requestor', displayName: 'REQUESTOR', master: true, talentPlusOnly: false, administrator: false).save(flush: true)
        def INTERVIEWER= new RoleGroup(name: 'tbcore-rg-interviewer', displayName: 'INTERVIEWER', master: true, talentPlusOnly: false, administrator: false).save(flush: true)
        def TB6_CLIENT_ADMIN= new RoleGroup(name: 'tb6-clientadmin', displayName: 'TB6 CLIENT ADMIN', master: false, administrator: true, talentPlusOnly: true).save(flush: true)
        
        def idealPassedString = 'TALENTBANK_CLIENT_ADMIN/INTERVIEWER/TB6_CLIENT_ADMIN'
        def tryingToAddAdminsFail = 'TALENTBANK_CLIENT_ADMIN/INTERVIEWER/TB6_CLIENT_ADMIN/TALENT_PLUS_ADMIN/SUPER_ADMIN'
        def tryingToAddAdminsPass = 'TALENTBANK_CLIENT_ADMIN/INTERVIEWER/TB6_CLIENT_ADMIN/TALENT_PLUS_ADMIN/SUPER_ADMIN'
        def duplicateValues = 'TALENTBANK_CLIENT_ADMIN/INTERVIEWER/TB6_CLIENT_ADMIN/TALENT_PLUS_ADMIN/SUPER_ADMIN/INTERVIEWER/TB6_CLIENT_ADMIN/TALENT_PLUS_ADMIN/SUPER_ADMIN'

        def extraSpaces = 'TALEN TBANK _CLIENT_ADMIN /INTERV IEWER/TB6_CLIENT_ADMIN/TALENT_ PLUS_ A DMIN /SUPER_ADMIN'
        def extraSpacesAndDuplicates = 'TALEN TBANK _CLIENT_ADMIN /INTERV IEWER/TB6_CLIENT_ADMIN/ TALENT_ PLUS_A DMIN/SUPER_ADMIN/TALENTBANK_CLIENT_ADMIN/INTERVIEWER/TB6_CLIENT_ADMIN /'
        def commas = 'TALENTBANK_CLIENT_ADMIN,INTERVIEWER,TB6_CLIENT_ADMIN'
        def period = 'TALENTBANK_CLIENT_ADMIN.INTERVIEWER.TB6_CLIENT_ADMIN'
        def dash = 'TALENTBANK_CLIENT_ADMIN-INTERVIEWER-TB6_CLIENT_ADMIN'
        def allDelimeters = 'TALENTBANK_CLIENT_ADMIN-INTERVIEWER.TB6_CLIENT_ADMIN,HR_MANAGER/REQUESTOR'
        def misSpellings = 'TALENTBuNK_CLIENT_ADMIN-INTERVIEWER/TB6_CLaENT_ADMIN/HR_MANAGER/REQUESTOoR'
        
        when:
        def  idealPassedStringReturnVal = service.getRoleGroupsFromDisplayNameString(idealPassedString)?.roleGroupList
        def  tryingToAddAdminsFailReturnVal = service.getRoleGroupsFromDisplayNameString(tryingToAddAdminsFail)?.roleGroupList
        def  tryingToAddAdminsPassReturnVal = service.getRoleGroupsFromDisplayNameString(duplicateValues, true)?.roleGroupList
        def  duplicateValuesReturnVal = service.getRoleGroupsFromDisplayNameString(tryingToAddAdminsPass, true)?.roleGroupList
        def  extraSpacesReturnValue = service.getRoleGroupsFromDisplayNameString(extraSpaces)?.roleGroupList
        def  extraSpacesAndDuplicatesReturnValue = service.getRoleGroupsFromDisplayNameString(extraSpacesAndDuplicates)?.roleGroupList
        def  extraSpacesAddAdminReturnValue = service.getRoleGroupsFromDisplayNameString(extraSpaces, true)?.roleGroupList
        def  commasReturnValue = service.getRoleGroupsFromDisplayNameString(commas)?.roleGroupList
        def  periodReturnValue = service.getRoleGroupsFromDisplayNameString(period)?.roleGroupList
        def  dashReturnValue = service.getRoleGroupsFromDisplayNameString(dash)?.roleGroupList
        def  allDelimetersReturnValue = service.getRoleGroupsFromDisplayNameString(allDelimeters, true)?.roleGroupList
        def  misSpellingsReturnValue = service.getRoleGroupsFromDisplayNameString(misSpellings).unmatchedGroups
        
        then:
        idealPassedStringReturnVal.sort() == [TALENTBANK_CLIENT_ADMIN,INTERVIEWER,TB6_CLIENT_ADMIN].sort()
        tryingToAddAdminsFailReturnVal.sort() == [TALENTBANK_CLIENT_ADMIN,INTERVIEWER,TB6_CLIENT_ADMIN].sort()
        tryingToAddAdminsPassReturnVal.sort() == [TALENTBANK_CLIENT_ADMIN,INTERVIEWER,TB6_CLIENT_ADMIN,TALENT_PLUS_ADMIN,SUPER_ADMIN].sort()
        duplicateValuesReturnVal.sort() == [TALENTBANK_CLIENT_ADMIN,INTERVIEWER,TB6_CLIENT_ADMIN,TALENT_PLUS_ADMIN,SUPER_ADMIN].sort()
        extraSpacesReturnValue.sort() == [TALENTBANK_CLIENT_ADMIN,INTERVIEWER, TB6_CLIENT_ADMIN].sort()
        extraSpacesAndDuplicatesReturnValue.sort() == [TALENTBANK_CLIENT_ADMIN,INTERVIEWER, TB6_CLIENT_ADMIN].sort()
        extraSpacesAddAdminReturnValue.sort() == [TALENTBANK_CLIENT_ADMIN,INTERVIEWER,TB6_CLIENT_ADMIN,TALENT_PLUS_ADMIN,SUPER_ADMIN].sort()
        commasReturnValue.sort() == [TALENTBANK_CLIENT_ADMIN,INTERVIEWER,TB6_CLIENT_ADMIN].sort()
        periodReturnValue.sort() == [TALENTBANK_CLIENT_ADMIN,INTERVIEWER,TB6_CLIENT_ADMIN].sort()
        dashReturnValue.sort() == [TALENTBANK_CLIENT_ADMIN,INTERVIEWER,TB6_CLIENT_ADMIN].sort()
        allDelimetersReturnValue.sort() == [TALENTBANK_CLIENT_ADMIN, INTERVIEWER, TB6_CLIENT_ADMIN, HR_MANAGER,REQUESTOR ].sort()
        misSpellingsReturnValue == ['TALENTBuNK_CLIENT_ADMIN', 'TB6_CLaENT_ADMIN', 'REQUESTOoR']
    }

}

@Transactional
class OrgUploadPublishServiceWithHibernateSpec extends Specification implements DataTest, BuildDataTest, ServiceUnitTest<OrgUploadPublishService> {
    @Shared
    @AutoCleanup
    HibernateDatastore hibernateDatastore

    @Shared
    PlatformTransactionManager transactionManager

    @Shared
    SessionFactory sessionFactory
    
    @Shared
    UserRoleGroupService userRoleGroupService
    
    @Shared
    def SUPER_ADMIN, TALENT_PLUS_ADMIN, TALENTBANK_CLIENT_ADMIN, HR_MANAGER, REQUESTOR, newUser, setup

    def setupSpec() {
        mockDomains(User, UserRoleGroup, RoleGroup)

        Map configuration = [
                'hibernate.hbm2ddl.auto': 'create-drop',
                'dataSource.url': 'jdbc:h2:mem:myDB',
        ]
        hibernateDatastore = new HibernateDatastore(configuration, User, UserRoleGroup, RoleGroup)
        transactionManager = hibernateDatastore.getTransactionManager()
        sessionFactory = hibernateDatastore.getSessionFactory()
        this.userRoleGroupService = this.hibernateDatastore.getService(UserRoleGroupService)
    }

    def setup() {
        if (!setup) {
            SUPER_ADMIN = RoleGroup.build(name: 'tbcore-rg-admin-super', displayName: 'SUPER ADMIN')
            TALENT_PLUS_ADMIN = RoleGroup.build(name: 'tbcore-rg-admin-tp', displayName: 'TALENT PLUS ADMIN')
            TALENTBANK_CLIENT_ADMIN = RoleGroup.build(name: 'tbcore-rg-admin-client', displayName: 'TALENTBANK CLIENT ADMIN')
            HR_MANAGER = RoleGroup.build(name: 'tbcore-rg-manager', displayName: 'HR MANAGER')
            REQUESTOR = RoleGroup.build(name: 'tbcore-rg-requestor', displayName: 'REQUESTOR')
            newUser = User.build(dateCreated: new Date(), lastUpdated: new Date())
            newUser.save(flush: true, failOnError: true)
            setup = true
        }
        
        UserRoleGroup.withSession {
            UserRoleGroup.withTransaction {
                UserRoleGroup.where {user == newUser}.deleteAll()
            }
        }

    }
    
    @Rollback
    void "should_return_info_comparing_new_to_old_role_groups"(){
        setup:
        service.sessionFactory = Mock(SessionFactory)
        service.sessionFactory.getCurrentSession() >> sessionFactory.getCurrentSession()
        def originalRoles = [TALENT_PLUS_ADMIN, TALENTBANK_CLIENT_ADMIN, REQUESTOR]

        when:
        originalRoles.each {
            def urg = UserRoleGroup.create(newUser, it)
            urg.save(flush: true, failOnError: true)
        }

        then:
        UserRoleGroup.findAllByUser(newUser)*.roleGroup.sort() == originalRoles.sort()

        when:
        def roleGroupString = 'TALENTBANK_CLIENT_ADMIN/SUPER_ADMIN/HR_MANAGER/SUPER_ADMIN/GOLDENgAWD/RAQUESTOR'
        def updateMap = tbservice.util.TBServiceUtils.compareRequestedRoleGroups(roleGroupString, newUser)
        hibernateDatastore.sessionFactory.currentSession.flush()
        def urgList = UserRoleGroup.where{roleGroup == REQUESTOR}.list()
        
        then:
        updateMap.requestedAdminGroups == ["SUPER_ADMIN"]
        updateMap.allRequestedChanges.added?.sort() == ["HR_MANAGER", "GOLDENGAWD",  "RAQUESTOR"].sort()
        updateMap.removing == urgList
    }

    @Rollback
    void "should_add_user_to_usergroups"() {
        setup:
        service.sessionFactory = Mock(SessionFactory)
        service.sessionFactory.getCurrentSession() >> sessionFactory.getCurrentSession()
        service.userRoleGroupService = Mock(UserRoleGroupService)
        service.userRoleGroupService.roleGroupsHaveChanges(_,_) >> {userMapToRequestRGString, noAdmin ->
            userRoleGroupService.roleGroupsHaveChanges(userMapToRequestRGString, noAdmin)}
        def originalRoles = [TALENT_PLUS_ADMIN, TALENTBANK_CLIENT_ADMIN, REQUESTOR]
        
        when:
        originalRoles.each {
            def urg = UserRoleGroup.create(newUser, it)
            urg.save(flush: true, failOnError: true)
        }

        then:
        UserRoleGroup.findAllByUser(newUser)*.roleGroup.sort() == originalRoles.sort()

        when:"pass role group options via string to method"
        def roleGroupString = 'TALENTBANK_CLIENT_ADMIN/SUPER_ADMIN/HR_MANAGER/SUPER_ADMIN/GOLDENgAWD/RAQUESTOR'
        def updateMap = service.updateUserRoleGroups(newUser, roleGroupString)
        def updatedRoles = [TALENT_PLUS_ADMIN, TALENTBANK_CLIENT_ADMIN, HR_MANAGER]
        hibernateDatastore.sessionFactory.currentSession.flush()

        then:"user is updated and role groups not in list are deleted"
        updateMap.removed.size() == 1
        updateMap.removed.containsAll(['REQUESTOR'])
        updateMap.addedRoleGroupOption.size() == 1
        updateMap.addedRoleGroupOption.containsAll(['HR_MANAGER'])
        updateMap.addedRoleGroups.size() == 1
        updateMap.addedRoleGroups.roleGroup.containsAll([HR_MANAGER])
        updateMap.requestsNotAdded.size() == 5
        updateMap.requestsNotAdded.sort() == ['TALENTBANK_CLIENT_ADMIN', 'SUPER_ADMIN', 'SUPER_ADMIN', 'GOLDENgAWD', 'RAQUESTOR'].sort()
        UserRoleGroup.where { user == newUser &&  roleGroup in updatedRoles }.count() == 3
        
        when:"we pass same roles that are assigned"
        def currentRoleGroups = 'TALENT_PLUS_ADMIN/TALENTBANK_CLIENT_ADMIN/HR_MANAGER'
        def noResults = service.updateUserRoleGroups(newUser, currentRoleGroups)
        
        then:"no adding should be done"
        noResults.requestsNotAdded.sort() == ['TALENT_PLUS_ADMIN','TALENTBANK_CLIENT_ADMIN','HR_MANAGER'].sort()


        when: "we pass an empty"
        def deleteResult = service.updateUserRoleGroups(newUser, null)
        hibernateDatastore.sessionFactory.currentSession.flush()

        then: "no changes and no new groups will just return true"
        deleteResult.removed == 2
        !UserRoleGroup.countByUserAndRoleGroupInList(newUser, (updatedRoles - TALENT_PLUS_ADMIN).flatten())
        UserRoleGroup.findByUserAndRoleGroup(newUser, TALENT_PLUS_ADMIN)
    }

}
