package tbservice

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification
import tbservice.enums.AssessmentOrderStatusType

class UserAssessmentOrderAccessServiceSpec extends Specification implements DataTest, ServiceUnitTest<UserAssessmentOrderAccessService>{

    def setupSpec() {
        mockDomain ClientSetup
        mockDomain CatalogDetail
        mockDomain AssessmentOrder
        mockDomain UserAssessmentOrderAccess
        mockDomain User
    }
    def setup() {
    }

    def cleanup() {
    }

    void 'test hasOwner'() {
        given: 'An assessment order id that belong to a user'
        def cs = new ClientSetup(clientId: 1, externalClientId: 1, clientName: 'test-client-name', companyCode: 'test-code', positionCategory: 'test-position').save(flush: true)
        def catalogDetail = new CatalogDetail(clientId: 1, assessmentCode: 'test-assessment-code', companyInterviewId: 2, type: 'P2P').save(flush: true)
        def assessmentOrder = new AssessmentOrder(firstName: "Test", lastName:"Test Last Name",assessmentCode:"ABC",catalogDetail: catalogDetail, clientSetup: cs, email: 'test-email@mailinator.com', status: 'Invited', statusCode: AssessmentOrderStatusType.INVITED).save(flush: true)
        def user1 = new User(clientSetupId: cs?.id, username: 'test-username', password: 'test-password', firstName: 'test-fname', lastName: 'test-lname',
                email: 'test-email@mailinator.com', active: true).save(flush: true, failOnError: true)
        new UserAssessmentOrderAccess(user: user1, assessmentOrder: assessmentOrder, relationship:'self').save(flush: true)
        //Mock criteria query
        def userAssessmentOrderAccessCriteria= new Expando()
        userAssessmentOrderAccessCriteria.list = {Closure cls ->
            [1]
        }
        UserAssessmentOrderAccess.metaClass.static.createCriteria = {userAssessmentOrderAccessCriteria}

        when: 'check if the given assessmentOrder has an owner'
        def hasOwner = service.hasOwner(assessmentOrder.id)

        then: 'should return true'
        hasOwner == true
    }

    void 'should be to add link a user to an assessmentOrder'() {
        given:
        def cs = new ClientSetup(clientId: 1, externalClientId: 1, clientName: 'test-client-name', companyCode: 'test-code', positionCategory: 'test-position').save(flush: true)
        def catalogDetail = new CatalogDetail(clientId: 1, assessmentCode: 'test-assessment-code', companyInterviewId: 2, type: 'P2P').save(flush: true)
        def assessmentOrder = new AssessmentOrder(id: 1, firstName: "Test", lastName:"Test Last Name",assessmentCode:"ABC",catalogDetail: catalogDetail, clientSetup: cs, email: 'test-email@mailinator.com', status: 'Invited', statusCode: AssessmentOrderStatusType.COMPLETED).save(flush: true)
        def assessmentOrder2 = new AssessmentOrder(firstName: "Tesaat", lastName:"Test aaLast Name",assessmentCode:"ABC",catalogDetail: catalogDetail, clientSetup: cs, email: 'test-email22@mailinator.com', status: 'Invited', statusCode: AssessmentOrderStatusType.COMPLETED).save(flush: true)

        def user1 = new User(clientSetupId: cs?.id, username: 'test-username', password: 'test-password', firstName: 'test-fname', lastName: 'test-lname',
                email: 'test-email@mailinator.com', active: true).save(flush: true, failOnError: true)

        def newUserAssessmentOrderAccess = new UserAssessmentOrderAccess(assessmentOrder: assessmentOrder2, user: user1).save(flush:true)

        //Mock criteria query
        def userAssessmentOrderAccessCriteria= new Expando()
        userAssessmentOrderAccessCriteria.list = {Closure cls ->
            []
        }

        UserAssessmentOrderAccess.metaClass.static.createCriteria = {userAssessmentOrderAccessCriteria}


        when: 'passing userId and an assessmentOrder to linkUserToOrder'
        service.linkUserToOrder(user1, 1)

        then: 'it will add an entry to UserAssessmentOrderAccess'
        UserAssessmentOrderAccess.count() == old(UserAssessmentOrderAccess.count()) + 1
    }


    void 'should be to handle linking a user to assessmentOrders'() {
        given:
        def cs = new ClientSetup(clientId: 1, externalClientId: 1, clientName: 'test-client-name', companyCode: 'test-code', positionCategory: 'test-position').save(flush: true)
        def catalogDetail = new CatalogDetail(clientId: 1, assessmentCode: 'test-assessment-code', companyInterviewId: 2, type: 'P2P').save(flush: true)
        def assessmentOrder = new AssessmentOrder(id: 1, firstName: "Test", lastName:"Test Last Name",assessmentCode:"ABC",catalogDetail: catalogDetail, clientSetup: cs, email: 'test-email@mailinator.com', status: 'Invited', statusCode: AssessmentOrderStatusType.COMPLETED).save(flush: true)
        def assessmentOrder2 = new AssessmentOrder(firstName: "Tesaat", lastName:"Test aaLast Name",assessmentCode:"ABC",catalogDetail: catalogDetail, clientSetup: cs, email: 'test-email22@mailinator.com', status: 'Invited', statusCode: AssessmentOrderStatusType.COMPLETED).save(flush: true)

        def user1 = new User(clientSetupId: cs?.id, username: 'test-username23', password: 'test-password', firstName: 'test-fname', lastName: 'test-lname',
                email: 'test-email23@mailinator.com', active: true).save(flush: true, failOnError: true)

        //Mock criteria query
        def userAssessmentOrderAccessCriteria= new Expando()
        userAssessmentOrderAccessCriteria.list = {Closure cls ->
            []
        }
        UserAssessmentOrderAccess.metaClass.static.createCriteria = {userAssessmentOrderAccessCriteria}

        def assessmentCriteria= new Expando()
        assessmentCriteria.list = {Closure cls ->
            [[id: 1], [id:2]]
        }

        AssessmentOrder.metaClass.static.createCriteria = {assessmentCriteria}


        when: 'passing userId and an assessmentOrder to linkUserToOrder'
        service.handleAssessmentLink(user1)

        then: 'it will add an entry to UserAssessmentOrderAccess'
        UserAssessmentOrderAccess.count() == 2
    }

}
