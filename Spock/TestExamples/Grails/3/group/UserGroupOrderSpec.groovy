package com.talentbank.core.group

import com.talentbank.core.ClientSetup
import com.talentbank.core.User
import com.talentbank.core.assessmentOrder.AssessmentOrder
import com.talentbank.core.catalog.CatalogDetail
import com.talentbank.core.enums.UserGroupType
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class UserGroupAssessmentOrderSpec extends Specification implements DomainUnitTest<UserGroupAssessmentOrder> {

    def setup() {
        mockDomain ClientSetup
        mockDomain User
        mockDomain UserGroup
        mockDomain GroupCompare
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

        when: 'given Assessment Order and ClientSetup objects'
        def catalogDetail = new CatalogDetail(clientId: 1, assessmentCode: 'TECHTEST-5004', companyInterviewId: 2,type: 'P2P')
        def clientSetup = new ClientSetup(clientId: 2030, clientName: 'Top talent', companyCode: 'TALENTPLUS',
                tbexPassword: 'talented', externalClientId: 2030, positionCategory: 'position')
        def assessmentOrder1 = new AssessmentOrder(assessmentCode: 'test-assessment-code-1', assessmentRequesterEmail: 'test@mailinator.com',
                email: 'blah@mailinator.com', clientSetup: clientSetup, firstName: 'firstName', lastName: 'lastName', catalogDetail: catalogDetail, receiptId: '111')
        def assessmentOrder2 = new AssessmentOrder(assessmentCode: 'test-assessment-code-2', assessmentRequesterEmail: 'test@mailinator.com',
                email: 'blah@mailinator.com', clientSetup: clientSetup, firstName: 'firstName', lastName: 'lastName', catalogDetail: catalogDetail, receiptId: '222')

        group.addToGroupAssessmentOrders(new UserGroupAssessmentOrder(userGroupId: group.id, assessmentOrderId: assessmentOrder1.id))
        group.addToGroupAssessmentOrders(new UserGroupAssessmentOrder(userGroupId: group.id, assessmentOrderId: assessmentOrder2.id))

        and: 'we can save it, and we get back a not null GORM entity for group 1'
        group.save()

        then: 'Group is valid'
        UserGroup grp = UserGroup.get(group.id)
        grp.groupAssessmentOrders.size() == 2
    }
}
