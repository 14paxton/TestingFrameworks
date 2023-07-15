package com.talentbank.core.userTeams

import com.auth0.json.auth.TokenHolder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.talentbank.core.*
import com.talentbank.core.enums.TeamCompareType
import com.talentbank.core.security.SpringSecurityService
import com.talentbank.core.team.SharedTeam
import com.talentbank.core.team.Team
import com.talentbank.core.team.TeamCompare
import grails.buildtestdata.BuildDataTest
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import grails.testing.web.GrailsWebUnitTest
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Shared
import spock.lang.Specification

class UserTeamServiceSpec extends Specification implements  DataTest, BuildDataTest , ServiceUnitTest<UserTeamService>, GrailsWebUnitTest{
    Closure doWithSpring() {
        { ->
            springSecurityService SpringSecurityService
        }
    }
    SpringSecurityService springSecurityService

    @Shared
    def tokenAuthentication, doofus, dingus

    @Shared
    boolean setupOnce = false

    private decodedJwt(def user) {
        def token = JWT.create()
        token.withIssuer('https://talentplus.auth0.com')
        token.withExpiresAt(new Date(30 * 10001))
        token.withJWTId('jwt-id')
        token.withClaim('email', user.email)
        token.withClaim("https://www.talentbankonline.com/user_metadata_FULL_MAP", '{}')
        token.withClaim("https://www.talentbankonline.com/app_metadata_FULL_MAP", '{}')

        def idToken = token.sign(Algorithm.HMAC256('secret'))
        def tokenHolder = new TokenHolder(accessToken: 'test-access-token', idToken: idToken, refreshToken: 'test-refresh-token', tokenType: 'test-token-type', expiresIn: 10000)


        def decodedJwt = JWT.decode(tokenHolder.getIdToken())
        return decodedJwt
    }

    void setupSpec() {
        mockDomain User
        mockDomain ApplicationAccess
        mockDomain TeamCompare
        mockDomain Team
        mockDomain RoleGroup
        mockDomain RoleGroupRole
        mockDomain UserRoleGroup
        mockDomain Role
        mockDomain ClientSetup
        mockDomain SharedTeam
    }

    void setup() {
        if (!setupOnce) {

            dingus = User.build(firstName: "dingus", lastName: "hamarabe")
            doofus = User.build(firstName: "doofus", lastName: "prittles")
            def newRoleGroup = RoleGroup.build()
            Role.build(authority: 'ROLE_PERMISSION_DUMMY')
            Role.build(authority: 'ROLE_PERMISSION_TEST')
            Role.build(authority: 'ROLE_PERMISSION_TEST2')
            Role.build(authority: 'ROLE_DUMMY')
            def roleList = Role.findAll()
            roleList.each {RoleGroupRole.build(role: it, roleGroup: newRoleGroup)}
            UserRoleGroup.build(user: doofus, roleGroup: newRoleGroup)
            def criteria = new Expando()
            criteria.get = { Closure cls ->
                [:]
            }
            criteria.list = { Closure cls -> [] }
            ApplicationAccess.metaClass.static.createCriteria = { criteria }
            tokenAuthentication = new TokenAuthentication(decodedJwt(doofus), doofus)
            tokenAuthentication.details = doofus
            SecurityContextHolder.metaClass.static.getContext = {-> [getAuthentication : {-> tokenAuthentication}]}

            setupOnce = true
        }
    }

    void "new-team-share-will-add-current-principal-user-as-created-by"(){
        setup:
        def team1 = TeamCompare.build(user: doofus)
        def team2 = TeamCompare.build(user: dingus)
        service.userService = Mock(UserService)
        service.userService.getCurrentPrincipalUser() >> doofus

        when:
       service.shareUserTeam(team1.id,  [dingus.id], TeamCompareType.TEAM_COMPARE.key)
       service.shareUserTeam(team2.id,  [doofus.id], TeamCompareType.TEAM_COMPARE.key)

        then:"created by should be the passed users"
       SharedTeam.findAllByTeamComparisonInList([team1, team2]).createdBy*.id == [doofus.id, doofus.id]

    }

    void "new-team-share-will-add-current-principal-user-as-created-by-set-by-domain"(){
        setup:
        def team1 = TeamCompare.build()
        service.userService = Mock(UserService)
        service.userService.getCurrentPrincipalUser() >> null

        when:
        service.shareUserTeam(team1.id, [dingus.id], TeamCompareType.TEAM_COMPARE.key)

        then:"created by should be the passed users"
        SharedTeam.findAllByTeamComparisonInList([team1]).createdBy*.id == [doofus.id]
    }

    void "test Share Team api"() {
        setup:
        def cs = new ClientSetup(clientId: 2030, clientName: 'Top talent', companyCode: 'TALENTPLUS',
                tbexPassword: 'talented', externalClientId: 2030, positionCategory: 'position').save()
        def user = new User(clientSetupId: cs.id, username: 'test-username', password: 'test-password', firstName: 'test-fname', lastName: 'test-lname',
                email: 'test-email@mailinator.com', active: true).save()

        service.userService = Mock(UserService)
        service.metaClass.static.isAuthorizedToViewTeams = {Long id -> return true}
        service.userService.getCurrentPrincipalUser() >> user
        SpringSecurityUtils.metaClass.static.ifAnyGranted = {String roles -> return true}

        when: 'Sharing a Team'
        def team1 = TeamCompare.build()
        def user2 = new User(clientSetupId: cs.id, username: 'test2-username', password: 'test-password',
                firstName: 'test-fname-2', lastName: 'test-lname-2',
                email: 'test-email-2@mailinator.com', active: true).save()
        def teamId = team1.id
        def userIds = [user2?.id];
        def type = null
        def size = service.shareUserTeam(teamId, userIds, type)

        then: 'returns the all shared teams'
        notThrown Exception
        size != null
        size.size() == 1
    }

}