package com.talentbank.core.group

import com.talentbank.core.ClientSetup
import com.talentbank.core.User
import grails.buildtestdata.BuildDataTest
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import grails.testing.spring.AutowiredTest
import grails.testing.web.GrailsWebUnitTest
import spock.lang.Specification

class UserGroupShareServiceSpec extends Specification implements AutowiredTest, DataTest, BuildDataTest ,  ServiceUnitTest<UserGroupShareService>, GrailsWebUnitTest {

    def setup() {
        mockDomain ClientSetup
        mockDomain User
        mockDomain UserGroup
        mockDomain UserGroupShare

    }

    def cleanup() {
    }

    void "test Share User Group save"() {
        setup:
        def cs = ClientSetup.build()
        def user = User.build(clientSetupId: cs.id)

        when: 'Saving a User Group'
        def group1 = UserGroup.build(name: "TestGroup", userId: user.id, id: 1)
        def id = group1.id

        then: 'User Group Saved'
        id != null
        id == 1

        when: 'When sharing group with other User'
        def user2 = User.build(clientSetupId: cs.id)
        def ugs = service.findOrSaveUserGroupShare(group1, id, user2.id)

        then: 'returns the all user groups'
        notThrown Exception
        ugs.userId == user2.id
        ugs.userGroup == group1
    }

    void "test will find created usergroupshare"() {
        setup:
        def cs = ClientSetup.build()
        def user = User.build(clientSetupId: cs.id)

        when: 'Saving a User Group'
        def group1 = UserGroup.build(name: "TestGroup", userId: user.id, id: 1)
        def id = group1.id

        then: 'User Group Saved'
        id != null
        id == 1

        when: 'When sharing group with other User'
        def user2 = User.build(clientSetupId: cs.id)
        def ugs1 = UserGroupShare.build(userGroup: group1, userId: user2.id, userGroupId: id )
        def ugs2 = service.findOrSaveUserGroupShare(group1, id, user2.id)

        then: 'returns the all user groups'
        notThrown Exception
        ugs1.id == ugs2.id
    }
}