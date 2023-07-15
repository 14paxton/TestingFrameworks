package com.talentbank.core.userGroup

import com.talentbank.core.*
import com.talentbank.core.assessmentOrder.AssessmentOrder
import com.talentbank.core.assessmentResult.AssessmentResult
import com.talentbank.core.catalog.CatalogDetail
import com.talentbank.core.dto.userGroup.command.Create
import com.talentbank.core.dto.userGroup.command.Share
import com.talentbank.core.dto.userGroup.command.Update
import com.talentbank.core.enums.UserGroupType
import com.talentbank.core.group.*
import com.talentbank.core.interviewBuilder.InterviewModel
import com.talentbank.core.recCode.RecommendationCodeService
import com.talentbank.core.scoringModel.ScoringModelService
import com.talentbank.core.security.*
import grails.buildtestdata.BuildDataTest
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import grails.testing.spring.AutowiredTest
import grails.testing.web.GrailsWebUnitTest
import spock.lang.Specification

class UserGroupServiceSpec extends Specification implements AutowiredTest, DataTest, BuildDataTest , ServiceUnitTest<UserGroupService>, GrailsWebUnitTest {

    def setup() {
        mockDomain ClientSetup
        mockDomain CatalogDetail
        mockDomain AssessmentOrder
        mockDomain UserAssessmentOrderAccess
        mockDomain User
        mockDomain UserGroup
        mockDomain SecurityGroup
        mockDomain DataAccessModel
        mockDomain DataAccessModelSecurityGroup
        mockDomain RoleGroup
        mockDomain UserRoleGroup
        mockDomain InterviewModel
        mockDomain UserGroupShare
        mockDomain Group
        mockDomain GroupCompare

    }

    def cleanup() {
    }

    void "test listMySavedResult api"() {
        setup:
        def cs = new ClientSetup(clientId: 2030, clientName: 'Top talent', companyCode: 'TALENTPLUS',
                tbexPassword: 'talented', externalClientId: 2030, positionCategory: 'position').save()
        def user = new User(clientSetupId: cs.id, username: 'test-username', password: 'test-password', firstName: 'test-fname', lastName: 'test-lname',
                email: 'test-email@mailinator.com', active: true).save()

        def group = new UserGroup(name: "MySavedGroup",
                type: UserGroupType.MYSAVEDGROUP,
                clientSetupId: cs.id,
                userId: user.id).save()

        SecurityGroup securityGroup = new SecurityGroup(clientSetup: cs, name: 'East Park Movie Theater',
                code: 'Marcus-1234', type: 'BRANCH', address: [city: 'Lincoln', provinceCode: 'NE']).save()
        DataAccessModel dataAccessModel = new DataAccessModel(name: 'Front Line Requestors', clientSetup: cs,
                creatorName: "Test Admin", hrMgrCatSecEnabled: true, reqCatSecEnabled: true, secGroupSecEnabled: true).save()
        DataAccessModelSecurityGroup.create(dataAccessModel, securityGroup)

        def orders = []
        def catalogs = []

        8.times {
            def date = new Date().parse("yyy-MM-dd HH:mm:ss", "2018-03-0$it 19:${it}9:52")

            def assessmentCode = "TECHTEST-500$it"
            def cd = new CatalogDetail(clientId: cs.id, assessmentCode: assessmentCode, showOverallScore: true, showRecommendation:true,
                    companyInterviewId: 2, interviewModelId:  2, type: 'AO6', scoringModelId: it, scoringModelName:"SM-${it}",
                    companyInterviewName: "Assessment${it} TOA").save()
            catalogs.add(cd)

            def order = new AssessmentOrder(assessmentCode: 'test-assessment-code-1', assessmentRequesterEmail: 'test@mailinator.com',
                    email: 'blah@mailinator.com', clientSetup: cs, firstName: "firstName${it}", lastName: "lastName${it}", catalogDetail: cd,
                    externalPersonId:"externalPersonId-${it}",receiptId: "$it-101").save()

            def result = new AssessmentResult( assessmentOrder: order,
                    assessmentCode:assessmentCode, assessmentPurpose:"EXTRN",assessmentStatus:"Completed", clientId:cs.id,
                    reportUrlDisabled:false,cutoffScore:6,email:order.email,statusCode:null,completedDate: date,
                    scoringModelId:it,companyCode:cs.companyCode, companyInterviewId:cd.companyInterviewId,companyInterviewName:cd.companyInterviewName,
                    finalScore:17,firstName:order.firstName,interviewModelId:cd.interviewModelId,lastName:order.lastName,isp2p:false,isPush:false,
                    maxScore:22,passed:true,percentCutScore:0,percentScore:77.3,receiptId:order.receiptId,
                    recommendationStatement:"Recommended to move forward in the selection process", type:"AO6",scoringModelAlias:"SMM").save()
            order.assessmentResult = result
            order.save()

            orders.add(order)
            group.addToGroupAssessmentOrders(new UserGroupAssessmentOrder(userGroupId: group.id, assessmentOrderId: order.id))
        }

        group.save()

        service.sessionService = Mock(SessionService)
        service.userService = Mock(UserService)
        service.userAssessmentOrderAccessService = Mock(UserAssessmentOrderAccessService)
        service.talentBankSecurityService = Mock(TalentBankSecurityService)
        service.scoringModelService = Mock(ScoringModelService)
        service.recommendationCodeService = Mock(RecommendationCodeService)

        service.sessionService.currentUser() >> user
        service.userService.getCurrentPrincipalUser() >> user
        service.sessionService.selectedClientSetupId() >> user.clientSetupId

        service.userService.getCurrentPrincipalUser() >> user
        SpringSecurityUtils.metaClass.static.ifAnyGranted = { String roles -> return true }

        service.scoringModelService.fetchScoringModelDisplayName() >> 'SMM'

        def resultData = []
        orders.each { o ->
            resultData.add([id: o.id, firstName:o.firstName, lastName: o.lastName, email: o.email,
                            externalPersonId: o.externalPersonId, lastUpdated: o.lastUpdated, assessmentPurpose : o.assessmentPurpose,
                            clientResultAccess: o.clientResultAccess, orderCompletedDate: o.completedDate,
                            securityGroupId: null, companyInterviewName: o.catalogDetail?.companyInterviewName,
                            interviewModelId: o.assessmentResult?.interviewModelId,
                            recommendationStatement: o.assessmentResult?.recommendationStatement,
                            passed: o.assessmentResult?.passed, resultCompletedDate: o.assessmentResult?.completedDate, scoringModelId: o.catalogDetail?.scoringModelId,
                            showRecommendation: o.catalogDetail?.showRecommendation, showOverallScore: o.catalogDetail?.showOverallScore ])
        }

        def assessmentOrderCriteria = new Expando()
        assessmentOrderCriteria.list = {Closure cls -> resultData }
        AssessmentOrder.metaClass.static.createCriteria = {assessmentOrderCriteria}

        service.userAssessmentOrderAccessService.fetchAssessmentOrderIdsByUser(user) >> []
        service.recommendationCodeService.fetchRecommendationCodes() >> []
//        service.talentBankSecurityService.fetchDAM(user.id, user.clientSetupId) >> null
//        service.talentBankSecurityService.fetchAllowedAssessmentCodesByDam(null, user.id) >> [reqAssessmentCodes : null,mgrAssessmentCodes : null,reqAllowAllAssessments : false,mgrAllowAllAssessments : false]

        when: 'fetch my saved results'
        def results = service.listMySavedResult(null, null)
        then: 'will return results with recommended statements '
        results != null
        results.isEmpty() == false
        results.size() == 8

        when: "Non Talent Plus Users not DAM "
        SpringSecurityUtils.metaClass.static.ifAnyGranted = { String roles -> return false }
        results = service.listMySavedResult(null, null)

        then: 'will return results with recommended statements for non Talent Plus Users '
        results != null
        results.isEmpty() == false
        results.size() == 8
        results*.result?.findAll {it != '' }?.size() == 8

        when: "Non Talent Plus and Security Group Enabled"
        SpringSecurityUtils.metaClass.static.ifAnyGranted = { String roles -> return false }
        service.talentBankSecurityService.fetchDAM(user.id, user.clientSetupId) >> dataAccessModel
        service.talentBankSecurityService.fetchAllowedAssessmentCodesByDam(dataAccessModel, user.id) >> [reqAssessmentCodes : null,mgrAssessmentCodes : null,reqAllowAllAssessments : false,mgrAllowAllAssessments : false]


        results = service.listMySavedResult(null, null)

        then: 'will not return results for orders without permission '
        results != null
        results.isEmpty() == true
        results.size() == 0
        results*.result?.findAll {it != '' }?.size() == 0


        when: "Non Talent Plus and Security Group Enabled, allowed one"
        SpringSecurityUtils.metaClass.static.ifAnyGranted = { String roles -> return false }
        service.talentBankSecurityService.fetchDAM(user.id, user.clientSetupId) >> dataAccessModel
        service.talentBankSecurityService.fetchAllowedAssessmentCodesByDam(dataAccessModel, user.id) >> [reqAssessmentCodes : null,mgrAssessmentCodes : null,reqAllowAllAssessments : false,mgrAllowAllAssessments : false]
        service.talentBankSecurityService.allowedSecurityGroups() >> [securityGroup]

        results = service.listMySavedResult(null, null)

        then: 'will not return results based on security groups '
        results != null
        results.isEmpty() == true
        results.size() == 0
        results*.result?.findAll {it != '' }?.size() == 0
        results*.completedDate?.findAll {it != null }?.size() == 0
        results*.passed?.findAll {it != null }?.size() == 0
        results*.blockedOrHidden?.orderBlocked?.findAll {it == true }?.size() == 0
        results*.blockedOrHidden?.resultHidden?.findAll {it == false }?.size() == 0

    }

    void "test Create User Group api"() {
        setup:
        def cs = new ClientSetup(clientId: 2030, clientName: 'Top talent', companyCode: 'TALENTPLUS',
                tbexPassword: 'talented', externalClientId: 2030, positionCategory: 'position').save()
        def user = new User(clientSetupId: cs.id, username: 'test-username', password: 'test-password', firstName: 'test-fname', lastName: 'test-lname',
                email: 'test-email@mailinator.com', active: true).save()

        service.sessionService = Mock(SessionService)
        service.userService = Mock(UserService)
        service.userAssessmentOrderAccessService = Mock(UserAssessmentOrderAccessService)
        service.talentBankSecurityService = Mock(TalentBankSecurityService)

        service.sessionService.currentUser() >> user
        service.userService.getCurrentPrincipalUser() >> user
        service.sessionService.selectedClientSetupId() >> user.clientSetupId

        service.userService.getCurrentPrincipalUser() >> user
        SpringSecurityUtils.metaClass.static.ifAnyGranted = { String roles -> return true }

        when: 'Saving a User Group'
        Create group1 = new Create()
        group1.name = "TestGroup"
        def id = service.createGroup(group1)

        then: 'User Group Saved'
        id != null
        id == 1

        when: 'Saving a Duplicate User Group'
        Create duplicate = new Create()
        duplicate.name = "TestGroup"
        def duplicateId = service.createGroup(duplicate)

        then: 'save should throw an exception, Duplicate Group Name'
        def ex = thrown(Exception)
        ex.message == 'Duplicate Group Name'
        duplicateId == null

        when: 'Saving a 2nd User Group'
        Create group2 = new Create()
        group2.name = "TestGroup 2"
        def id2 = service.createGroup(group2)

        then: 'User Group Saved'
        id2 != null
        id2 == 2

        when: 'Updating a 2nd User Group as Duplicate'
        Update group2Duplicate = new Update()
        group2Duplicate.id = id2
        group2Duplicate.name = "TestGroup"
        service.updateGroup(group2Duplicate)

        then: 'update should throw an exception, Duplicate Group Name'
        def ex2 = thrown(Exception)
        ex2.message == 'Duplicate Group Name'

        when: 'Show or Display 2nd User Group'
        def userGroup2 = service.show(id2)

        then: 'User Group 2 fetched'
        notThrown Exception
        userGroup2 != null
        userGroup2.id == id2
        userGroup2.groupName == group2.name

        //using session to remove, .remove will not work with unit test
        when: 'Deleting 2nd User Group'
        Group.get(id2).delete([flush: true, failOnError: true])

        then: 'User Group Deleted'
        notThrown Exception

        when: 'Deleting 2nd User Group again'
        service.remove(id2)

        then: 'Delete should throw an exception, Duplicate Group Name'
        def ex3 = thrown(Exception)
        ex3.message == "Invalid User Group"

        when: 'Loading 10 User Group'
        def userGroups = []
        10.times {
            Create ug = new Create()
            ug.name = "TestGroup-${it}"
            def ugId = service.createGroup(ug)
            userGroups.add([id: ugId, name: ug.name, interviewModelId: 0, visibility: '',lastUpdated:'01-01-2010'])
        }

        then: 'Has 10 user groups'
        notThrown Exception
        userGroups.size() == 10

        when: 'Listing all User Group'
        Group.metaClass.static.getGroupsByUser = {List<UserGroupType> requestedTypes, Long userId, Long clientId -> userGroups}
        def list = service.list()

        then: 'returns the all user groups'
        notThrown Exception
        list != null
        list.size() != 0
        list.size() == 10
    }

    void "test Share User Group api"() {
        setup:
        def cs = new ClientSetup(clientId: 2030, clientName: 'Top talent', companyCode: 'TALENTPLUS',
                tbexPassword: 'talented', externalClientId: 2030, positionCategory: 'position').save()
        def user = new User(clientSetupId: cs.id, username: 'test-username', password: 'test-password', firstName: 'test-fname', lastName: 'test-lname',
                email: 'test-email@mailinator.com', active: true).save()

        service.sessionService = Mock(SessionService)
        service.userService = Mock(UserService)
        service.userAssessmentOrderAccessService = Mock(UserAssessmentOrderAccessService)
        service.talentBankSecurityService = Mock(TalentBankSecurityService)

        service.sessionService.currentUser() >> user
        service.userService.getCurrentPrincipalUser() >> user
        service.sessionService.selectedClientSetupId() >> user.clientSetupId

        service.userService.getCurrentPrincipalUser() >> user
        SpringSecurityUtils.metaClass.static.ifAnyGranted = { String roles -> return true }

        when: 'Saving a User Group'
        Create group1 = new Create()
        group1.name = "TestGroup"
        def id = service.createGroup(group1)

        then: 'User Group Saved'
        id != null
        id == 1

        when: 'When sharing group with other User'
        def user2 = new User(clientSetupId: cs.id, username: 'test2-username', password: 'test-password',
                firstName: 'test-fname-2', lastName: 'test-lname-2',
                email: 'test-email-2@mailinator.com', active: true).save()
        Share share = new Share()
        share.userIds = [user2?.id]
        
        def ugs = UserGroupShare.build(userGroup: UserGroup.get(id), userGroupId: id,  userId:  user2.id)
        service.userGroupShareService = Mock(UserGroupShareService)
        service.userGroupShareService.findOrSaveUserGroupShare(_ as UserGroup, _ as Long, _ as Long) >> ugs
        
        def size = service.shareUserGroup(share, id)

        then: 'returns the all user groups'
        notThrown Exception
        size != null
        size == 1
    }
}