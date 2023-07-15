package com.talentbank.core.userTeams

import com.talentbank.core.Role
import com.talentbank.core.RoleGroup
import com.talentbank.core.RoleGroupRole
import com.talentbank.core.User
import com.talentbank.core.UserRoleGroup
import com.talentbank.core.UserService
import com.talentbank.core.dto.userTeam.command.TeamCompareCommand
import com.talentbank.core.enums.TeamCompareType
import com.talentbank.core.team.TeamCompare
import com.talentbank.core.team.TeamCompareJoinUser
import grails.buildtestdata.BuildDataTest
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Shared
import spock.lang.Specification
import com.talentbank.core.team.SharedTeam

class TeamCompareServiceSpec extends Specification implements DataTest, BuildDataTest ,ServiceUnitTest<TeamCompareService>{

    def setup() {
        mockDomain User
        mockDomain Role
        mockDomain RoleGroup
        mockDomain RoleGroupRole
        mockDomain UserRoleGroup
        mockDomain TeamCompare
        mockDomain TeamCompareJoinUser
        mockDomain SharedTeam
        RoleGroupRole.create(superRoleGroup, superAdmin)
        user.save(flush: true)
        user1.save(flush: true)
        user2.save(flush: true)
        UserRoleGroup.create(user, superRoleGroup)
        UserRoleGroup.create(user1, superRoleGroup)
        UserRoleGroup.create(user2, superRoleGroup)
        service.userTeamService = Mock(UserTeamService)
        service.userService = Mock(UserService)
        service.userService.getCurrentPrincipalUser() >> user
    }

    @Shared def superAdmin = new Role(authority: 'ROLE_RESULT_SHOW')
    @Shared def superRoleGroup = new RoleGroup(displayName: 'Interviewer', master: true, name: 'tbcore-rg-interviewer', administrator: true, talentPlusOnly: true, rank: 1)
    @Shared def user = new User(id: 1, firstName: 'Amit',lastName: 'Admin',email: 'a1@talentplus.com', username: 'a1', password: 'changeme', clientSetupId: 1, active: true)
    @Shared def user1 = new User(id: 2, firstName: 'Amit',lastName: 'Admin',email: 'a2@talentplus.com', username: 'a2', password: 'changeme', clientSetupId: 1, active: true)
    @Shared def user2 = new User(id: 3, firstName: 'Amit',lastName: 'Aadmin',email: 'a3@talentplus.com', username: 'a3', password: 'changeme', clientSetupId: 1, active: true)

    void "Test for creatNewTeamCompare  "() {
        setup:
        TeamCompareCommand tc = new TeamCompareCommand(name: "Team 1 v Team 2", teamIds: [2,3], type: TeamCompareType.TEAM_COMPARE.key)

        when:
        service.userTeamService.isAuthorizedByTeamIds(_ as List<Long>) >> true
        SpringSecurityUtils.metaClass.static.ifAnyGrantedAny = { String roles -> return true}
        def res = service.creatNewTeamCompare(tc)
        then:
        User.findAll()?.size() == 3
        res?.name == "Team 1 v Team 2"
        res?.type == TeamCompareType?.TEAM_COMPARE?.key
        res?.teamIds?.size() == 2

        when:
        service.userTeamService.isAuthorizedByTeamIds(_ as List<Long>) >> false
        def result = service.creatNewTeamCompare(tc)
        then:
        result?.error
    }
    void "Test for updateTeamCompareInfo" () {
        setup:
        TeamCompareCommand tcToCreate = new TeamCompareCommand( name: "Team 1 v Team 2", teamIds: [2,3], type: TeamCompareType.TEAM_COMPARE.key)
        TeamCompareCommand tcToUpdate = new TeamCompareCommand( id: 1, name: "Team 1 v Team 2 (updated)", teamIds: [2,3], type: TeamCompareType.TEAM_COMPARE.key)


        when:
        service.userTeamService.isAuthorizedByTeamIds(_ as List<Long>) >> true
        SpringSecurityUtils.metaClass.static.ifAnyGrantedAny = { String roles -> return true}
        def initial = service?.creatNewTeamCompare(tcToCreate)
        def updated = service.updateTeamCompareInfo(tcToUpdate)
        then:
        initial?.name == "Team 1 v Team 2"
        updated?.name == "Team 1 v Team 2 (updated)"
    }

    void "Test for edge cases for createNewTeamCompare" () {
        setup:
        TeamCompareCommand tcToCreate = new TeamCompareCommand( name: "Team 2 vs Team 3", teamIds: [2,3], type: TeamCompareType.TEAM_COMPARE.key)
        service.userTeamService.isAuthorizedByTeamIds(_ as List<Long>) >> true
        SpringSecurityUtils.metaClass.static.ifAnyGrantedAny = { String roles -> return true}

        when: "Team Ids are empty"
        tcToCreate?.teamIds = []
        then: "Creating a new TeamCompare will fail"
        service?.creatNewTeamCompare(tcToCreate)?.error

        when: "Same name provided to create a new TeamCompare by a same user"
        tcToCreate?.teamIds = [2,3]
        service?.creatNewTeamCompare(tcToCreate)
        def faultyCommand = new TeamCompareCommand( name: "Team 2 vs Team 3", teamIds: [2,3], type: TeamCompareType.TEAM_COMPARE.key)
        then: "Creating a new TeamCompare will fail"
        service?.creatNewTeamCompare(faultyCommand)?.error
    }
}
