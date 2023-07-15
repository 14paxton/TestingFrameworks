package com.talentbank.core.group

import com.talentbank.core.ClientSetup
import com.talentbank.core.User
import com.talentbank.core.enums.UserGroupType
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class UserGroupSpec extends Specification implements DomainUnitTest<UserGroup> {

    def setup() {
        mockDomain ClientSetup
        mockDomain User
        mockDomain UserGroup
    }

    def cleanup() {
    }

    void "saving User Group"(){
        when: "given Objects"
        def cs = new ClientSetup(clientId: 1, externalClientId: 1, clientName: 'test-client-name', companyCode: 'test-code', positionCategory: 'test-position').save()
        def user = new User(clientSetupId: cs.id, username: 'test-username', password: 'test-password', firstName: 'test-fname', lastName: 'test-lname',
                email: 'test-email@mailinator.com', active: true).save()

        def group = new UserGroup(name: "MySavedGroup",
                type: UserGroupType.MYSAVEDGROUP,
                clientSetupId: cs.id,
                userId: user.id)

        then: 'Group is valid'
        group.validate()

        and: 'we can save it, and we get back a not null GORM entity for group 1'
        group.save()

        and: 'there is one row'
        UserGroup.count() == old(UserGroup.count()) + 1

    }
}
