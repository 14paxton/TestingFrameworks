package com.talentbank.core.userTeams

import com.talentbank.core.ClientSetup
import com.talentbank.core.Role
import com.talentbank.core.RoleGroup
import com.talentbank.core.RoleGroupRole
import com.talentbank.core.User
import com.talentbank.core.UserRoleGroup
import com.talentbank.core.UserService
import com.talentbank.core.assessmentOrder.AssessmentOrder
import com.talentbank.core.catalog.CatalogDetail
import com.talentbank.core.dto.userTeam.command.TeamCompareCommand
import com.talentbank.core.enums.TeamCompareType
import com.talentbank.core.team.TeamCompareJoinUser
import com.talentbank.core.team.TeamResultCompare
import grails.buildtestdata.BuildDataTest
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Shared
import spock.lang.Specification

class TeamResultCompareServiceSpec extends Specification implements DataTest, BuildDataTest ,ServiceUnitTest<TeamResultCompareService>{
    def setup() {
        mockDomain User
        mockDomain Role
        mockDomain RoleGroup
        mockDomain RoleGroupRole
        mockDomain UserRoleGroup
        mockDomain TeamResultCompare
        mockDomain TeamCompareJoinUser
        mockDomain CatalogDetail
        mockDomain ClientSetup
        mockDomain AssessmentOrder
        RoleGroupRole.create(superRoleGroup, superAdmin)
        clientSetup.save(flush: true)
        user.save(flush: true)
        user1.save(flush: true)
        user2.save(flush: true)
        assessmentOrder.save(flush: true)
        assessmentOrder2.save(flush: true)
        UserRoleGroup.create(user, superRoleGroup)
        UserRoleGroup.create(user1, superRoleGroup)
        UserRoleGroup.create(user2, superRoleGroup)
        service.userTeamService = Mock(UserTeamService)
        service.userService = Mock(UserService)
        service.userService.getCurrentPrincipalUser() >> user
    }

    @Shared def catalogDetail = new CatalogDetail(clientId: 1, assessmentCode: 'TECHTEST-5004', companyInterviewId: 2, type: 'P2P')

    @Shared def clientSetup = new ClientSetup(clientId: 2030, clientName: 'Top talent', companyCode: 'TALENTPLUS',
            tbexPassword: 'talented', externalClientId: 2030, positionCategory: 'position')

    @Shared def assessmentOrder = new AssessmentOrder(assessmentCode: 'test-assessment-code-1', assessmentRequesterEmail: 'test@mailinator.com',
            email: 'blah@mailinator.com', clientSetup: clientSetup, firstName: 'firstName', lastName: 'lastName', catalogDetail: catalogDetail, receiptId: '111')
    @Shared def assessmentOrder2 = new AssessmentOrder(assessmentCode: 'test-assessment-code-2', assessmentRequesterEmail: 'test2@mailinator.com',
            email: 'blah2@mailinator.com', clientSetup: clientSetup, firstName: 'firstName2', lastName: 'lastName2', catalogDetail: catalogDetail, receiptId: '112')

    @Shared def superAdmin = new Role(authority: 'ROLE_RESULT_SHOW')
    @Shared def superRoleGroup = new RoleGroup(displayName: 'Interviewer', master: true, name: 'tbcore-rg-interviewer', administrator: true, talentPlusOnly: true, rank: 1)
    @Shared def user = new User(id: 1, firstName: 'Amit',lastName: 'Admin',email: 'a1@talentplus.com', username: 'a1', password: 'changeme', clientSetupId: 1, active: true)
    @Shared def user1 = new User(id: 2, firstName: 'Amit',lastName: 'Admin',email: 'a2@talentplus.com', username: 'a2', password: 'changeme', clientSetupId: 1, active: true)
    @Shared def user2 = new User(id: 3, firstName: 'Amit',lastName: 'Aadmin',email: 'a3@talentplus.com', username: 'a3', password: 'changeme', clientSetupId: 1, active: true)


    void "Test for createNewResultCompare" () {
        setup:
        TeamCompareCommand tc = new TeamCompareCommand(name: "Team 1 v Team 2", teamIds: [2], assessmentOrderIds: [1,2], type: TeamCompareType.RESULT_COMPARE.key)

        when:
        service.userTeamService.isAuthorizedByTeamIds(_ as List<Long>) >> true
        SpringSecurityUtils.metaClass.static.ifAnyGrantedAny = { String roles -> return true}
        def res = service.createNewResultCompare(tc)
        then:
        User.findAll()?.size() == 3
        res?.name == "Team 1 v Team 2"
        res?.type == TeamCompareType?.RESULT_COMPARE?.key
        res?.assessmentOrderIds?.size() == 2

        when:
        service.userTeamService.isAuthorizedByTeamIds(_ as List<Long>) >> false
        def result = service.createNewResultCompare(tc)
        then:
        result?.error
    }
    void "Test for updateTeamResultCompareInfo" () {
        setup:
        TeamCompareCommand tcToCreate = new TeamCompareCommand(name: "Team 1 v Team 2", teamIds: [2], assessmentOrderIds: [1,2], type: TeamCompareType.RESULT_COMPARE.key)
        TeamCompareCommand tcToUpdate = new TeamCompareCommand(id: 1,name: "Team 1 v Team 2 (updated)", teamIds: [2], assessmentOrderIds: [1,2], type: TeamCompareType.RESULT_COMPARE.key)

        when:
        service.userTeamService.isAuthorizedByTeamIds(_ as List<Long>) >> true
        SpringSecurityUtils.metaClass.static.ifAnyGrantedAny = { String roles -> return true}
        def initial = service?.createNewResultCompare(tcToCreate)
        def updated = service.updateTeamResultCompareInfo(tcToUpdate)
        then:
        initial?.name == "Team 1 v Team 2"
        updated?.name == "Team 1 v Team 2 (updated)"
    }

    void "Test for edge cases for createNewResultCompare" () {
        setup:
        TeamCompareCommand tcToCreate = new TeamCompareCommand(name: "Team 1 v Team 2", teamIds: [2], assessmentOrderIds: [1,2], type: TeamCompareType.RESULT_COMPARE.key)
        service.userTeamService.isAuthorizedByTeamIds(_ as List<Long>) >> true
        SpringSecurityUtils.metaClass.static.ifAnyGrantedAny = { String roles -> return true}

        when: "teamIds are empty or null"
        tcToCreate?.teamIds = []
        then:"Trying to create a TeamResultCompare with empty teamIds will fail"
        service?.createNewResultCompare(tcToCreate)?.error

        when: "assessmentOrderIds are empty or null"
        tcToCreate?.assessmentOrderIds = []
        then: "Trying to create a TeamResultCompare with empty assessmentOrderIds will fail"
        service?.createNewResultCompare(tcToCreate)?.error

        when: "Same name field is provided"
        tcToCreate?.teamIds = [2]
        tcToCreate?.assessmentOrderIds = [1,2]
        def saved = service?.createNewResultCompare(tcToCreate)
        def newObj = new TeamCompareCommand(name: "Team 1 v Team 2", teamIds: [2], assessmentOrderIds: [1,2], type: TeamCompareType.RESULT_COMPARE.key)
        then: "Trying to create a TeamResultCompare with same name will fail"
        saved?.name == "Team 1 v Team 2"
        service?.createNewResultCompare(newObj)?.error
    }
}
