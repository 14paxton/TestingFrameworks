package com.talentbank.core.group

import com.talentbank.core.User
import com.talentbank.core.UserService
import com.talentbank.core.assessmentOrder.AssessmentOrder
import grails.buildtestdata.BuildDataTest
import grails.gorm.transactions.Rollback
import grails.gorm.transactions.Transactional
import grails.testing.gorm.DataTest
import org.grails.datastore.mapping.multitenancy.resolvers.SystemPropertyTenantResolver
import org.grails.orm.hibernate.HibernateDatastore
import org.grails.testing.GrailsUnitTest
import org.springframework.transaction.PlatformTransactionManager
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

@Transactional
class UserGroupAssessmentOrderServiceSpec extends Specification implements DataTest, BuildDataTest, GrailsUnitTest{
    @Shared
    @AutoCleanup
    HibernateDatastore hibernateDatastore

    @Shared
    PlatformTransactionManager transactionManager

    @Shared
    UserGroupAssessmentOrderService userGroupAssessmentOrderService

    @Shared
    def principalUser

    def setupSpec() {
        mockDomain UserGroup
        mockDomain ResultCompare
        mockDomain Group
        mockDomain User
        mockDomain GroupCompare
        mockDomain UserGroupAssessmentOrder
        mockDomain AssessmentOrder
        mockDomain GroupCompareJoinUserGroup

        Map configuration = [
                'hibernate.hbm2ddl.auto': 'create-drop',
                'dataSource.url': 'jdbc:h2:mem:myDB',
                'hibernate.cache.region.factory_class': 'org.hibernate.cache.ehcache.EhCacheRegionFactory',
                'grails.gorm.multiTenancy.mode': 'DISCRIMINATOR',
                'grails.gorm.multiTenancy.tenantResolverClass': SystemPropertyTenantResolver
        ]
        hibernateDatastore = new HibernateDatastore(configuration,GroupCompare, UserGroup, UserGroupAssessmentOrder, GroupCompareJoinUserGroup, Group, ResultCompare)
        transactionManager = hibernateDatastore.getTransactionManager()
        userGroupAssessmentOrderService = this.hibernateDatastore.getService(UserGroupAssessmentOrderService)

        principalUser = User.build()
    }

    def cleanup() {
    }

    def setup() {
        Group.withTransaction {
            Group.all.each {it.delete(flush: true)}
            UserGroupAssessmentOrder.all.each {it.delete(flush: true)}
        }
    }

    @Rollback
    void "interface methods work"() {
        when:
        def ass1 = AssessmentOrder.build()
        def ass2 = AssessmentOrder.build()
        def ass3 = AssessmentOrder.build(ass2.properties as Map)
        def ug1 = UserGroup.build(name: "Testing Result Compare", interviewModelId: 1)
        def ug2 = UserGroup.build(name: "Testing Result Compare2", interviewModelId: 1)
        def ug3 = UserGroup.build(name: "Testing Result Compare3", interviewModelId: 1)
        def ug4 = UserGroup.build(name: "Testing Result Compare4", interviewModelId: 1)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: ug1.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: ug2.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: ug2.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: ug3.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: ug4.id)
        
        def groupCompare = new GroupCompare(ug1, principalUser.id, "test interface")
        groupCompare.addUserGroupToCompare(ug2)
        groupCompare.addUserGroupToCompare(ug3)
        groupCompare.save(flush: true, failOnError: true)
        
        groupCompare.addToGroupAssessmentOrders(userGroupAssessmentOrderService.save(groupCompare, ass1.id, groupCompare.id))
        groupCompare.addToGroupAssessmentOrders(userGroupAssessmentOrderService.save(groupCompare, ass2.id, groupCompare.id))

        then:
        userGroupAssessmentOrderService.count() == 7
        userGroupAssessmentOrderService.countByUserGroupCompare(groupCompare) == 2
        userGroupAssessmentOrderService.countByUserGroupId(ug1.id) == 1
        userGroupAssessmentOrderService.findUserGroupAssessmentOrderAssessmentOrderId(groupCompare).containsAll( [ass1.id, ass2.id])
        !userGroupAssessmentOrderService.findUserGroupAssessmentOrderAssessmentOrderId(ug1.id as Long).containsAll([ass1.id, ass2.id])
        userGroupAssessmentOrderService.findUserGroupAssessmentOrderAssessmentOrderId(ug2.id as Long).containsAll([ass1.id, ass2.id])
        
        when:
        userGroupAssessmentOrderService.delete(UserGroupAssessmentOrder.first().id)
        hibernateDatastore.currentSession.flush()
        
        then:
        userGroupAssessmentOrderService.count() == 6
        
        when:
        userGroupAssessmentOrderService.delete(groupCompare)
        hibernateDatastore.currentSession.flush()
        
        then:
        userGroupAssessmentOrderService.count() == 4
        userGroupAssessmentOrderService.countByUserGroupCompare(groupCompare) == 0
        !userGroupAssessmentOrderService.findUserGroupAssessmentOrderAssessmentOrderId(ug1.id as Long).containsAll([ass1.id, ass2.id])
        userGroupAssessmentOrderService.findUserGroupAssessmentOrderAssessmentOrderId(ug2.id as Long).containsAll([ass1.id, ass2.id])
    }
    
    @Rollback
    void "interface can find assessments by assigned groups"(){
        when:
        def ass1 = AssessmentOrder.build()
        def ass2 = AssessmentOrder.build()
        def ass3 = AssessmentOrder.build(ass2.properties as Map)
        def ug1 = UserGroup.build(name: "Testing Result Compare", interviewModelId: 1)
        def ug2 = UserGroup.build(name: "Testing Result Compare2", interviewModelId: 1)
        def ug3 = UserGroup.build(name: "Testing Result Compare3", interviewModelId: 1)
        def ug4 = UserGroup.build(name: "Testing Result Compare4", interviewModelId: 1)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: ug1.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: ug2.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: ug2.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: ug3.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass3.id, userGroupId: ug4.id)

        def groupCompare = new GroupCompare(ug1, principalUser.id, "test interface")
        groupCompare.addUserGroupToCompare(ug2)
        groupCompare.addUserGroupToCompare(ug3)
        groupCompare.save(flush: true, failOnError: true)
        
        then:
        userGroupAssessmentOrderService.findAllAssessmentIdsByGroups(groupCompare).size() == 2
        
        when:
        groupCompare.addUserGroupToCompare(ug4)
        groupCompare.save(flush: true, failOnError: true)
        
        then:
        userGroupAssessmentOrderService.findAllAssessmentIdsByGroups(groupCompare).size() == 3
    }

    @Rollback
    void "delete assigned assessments by assessment id list"() {
        when:
        def ass1 = AssessmentOrder.build()
        def ass2 = AssessmentOrder.build()
        def ass3 = AssessmentOrder.build(ass2.properties as Map)
        def ug1 = UserGroup.build(name: "Testing Result Compare", interviewModelId: 1)
        def ug2 = UserGroup.build(name: "Testing Result Compare2", interviewModelId: 1)
        def ug3 = UserGroup.build(name: "Testing Result Compare3", interviewModelId: 1)
        def ug4 = UserGroup.build(name: "Testing Result Compare4", interviewModelId: 1)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: ug1.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: ug2.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: ug2.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: ug3.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass3.id, userGroupId: ug4.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: ug4.id)

        def groupCompare = new GroupCompare(ug1, principalUser.id, "test interface")
        groupCompare.addUserGroupToCompare(ug2)
        groupCompare.addUserGroupToCompare(ug3)
        groupCompare.addUserGroupToCompare(ug4)
        groupCompare.save(flush: true, failOnError: true)

        groupCompare.addToGroupAssessmentOrders(userGroupAssessmentOrderService.save(groupCompare, ass1.id, groupCompare.id))
        groupCompare.addToGroupAssessmentOrders(userGroupAssessmentOrderService.save(groupCompare, ass3.id, groupCompare.id))
        groupCompare.addToGroupAssessmentOrders(userGroupAssessmentOrderService.save(groupCompare, ass2.id, groupCompare.id))

        then:
        userGroupAssessmentOrderService.countByUserGroupCompare(groupCompare) == 3

        when: "delete by list"
        userGroupAssessmentOrderService.findAllByUserGroupCompareAndAssessmentOrderIds(groupCompare, [ass2.id, ass3.id]).each{
            groupCompare.removeFromGroupAssessmentOrders(it)
        }
        hibernateDatastore.currentSession.flush()
        
        then:
        userGroupAssessmentOrderService.countByUserGroupCompare(groupCompare) == 1
        groupCompare.groupAssessmentOrders.size() == 1
    }


    @Rollback
    void "query should filter out groups for deletion"() {
        given:
        def ass1 = AssessmentOrder.build()
        def ass2 = AssessmentOrder.build()
        def ass3 = AssessmentOrder.build(ass2.properties)

        when:
        def principleUser = User.build()
        def ug1 = UserGroup.build(name: "Testing Result Compare", interviewModelId:  1, userId: principalUser.id as Long)
        def ug2 = UserGroup.build(name: "Testing Result Compare2", interviewModelId: 1, userId: principalUser.id as Long)
        def ug3 = UserGroup.build(name: "Testing Result Compare3", interviewModelId: 1, userId: principalUser.id as Long)
        def ug4 = UserGroup.build(name: "Testing Result Compare4", interviewModelId: 1, userId: principalUser.id as Long)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: ug1.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: ug2.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: ug2.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: ug3.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass3.id, userGroupId: ug4.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: ug4.id)

        def groupCompare = new GroupCompare(ug1, principleUser.id, "test delete")
        groupCompare.addUserGroupToCompare(ug2)
        groupCompare.addUserGroupToCompare(ug3)
        groupCompare.addUserGroupToCompare(ug4)
        groupCompare.save(flush: true, failOnError: true)
        def ugao1 = userGroupAssessmentOrderService.save(groupCompare, ass1.id, groupCompare.id)
        def ugao2 = userGroupAssessmentOrderService.save(groupCompare, ass2.id, groupCompare.id)
        def ugao3 = userGroupAssessmentOrderService.save(groupCompare, ass3.id, groupCompare.id)
        groupCompare.addToGroupAssessmentOrders(ugao1)
        groupCompare.addToGroupAssessmentOrders(ugao2)
        groupCompare.addToGroupAssessmentOrders(ugao3)
        groupCompare.save(flush: true, failOnError: true)

        def groupCompare2 = new GroupCompare(ug1, principleUser.id, "test filter for delete")
        groupCompare2.addUserGroupToCompare(ug2)
        groupCompare2.addUserGroupToCompare(ug3)
        groupCompare2.addUserGroupToCompare(ug4)
        groupCompare2.save(flush: true, failOnError: true)
        groupCompare2.addToGroupAssessmentOrders(userGroupAssessmentOrderService.save(groupCompare2, ass1.id, groupCompare2.id))
        groupCompare2.addToGroupAssessmentOrders(userGroupAssessmentOrderService.save(groupCompare2, ass2.id, groupCompare2.id))
        groupCompare2.addToGroupAssessmentOrders(userGroupAssessmentOrderService.save(groupCompare2, ass3.id, groupCompare2.id))
        groupCompare2.save(flush: true, failOnError: true)

        then:
        userGroupAssessmentOrderService.fetchDuplicateAssessmentOrderIds(groupCompare).size() == 2
        userGroupAssessmentOrderService.fetchDuplicateAssessmentOrderIds(groupCompare).containsAll([ass1.id, ass2.id])
        userGroupAssessmentOrderService.fetchToRemove(ug4.id, groupCompare).size() == 1
        userGroupAssessmentOrderService.fetchToRemove(ug4.id, groupCompare).containsAll([ ugao3])
    }

    @Rollback
    void "query should filter from user group order id list"() {
        given:
        def ass1 = AssessmentOrder.build()
        def ass2 = AssessmentOrder.build()
        def ass3 = AssessmentOrder.build(ass2.properties)
        def ass4 = AssessmentOrder.build(ass2.properties)
        def ass5 = AssessmentOrder.build(ass2.properties)
        def ass6 = AssessmentOrder.build(ass2.properties)

        when:
        def principleUser = User.build()
        def ug1 = UserGroup.build(name: "Testing Result Compare", interviewModelId:  1, userId: principalUser.id as Long)
        def ug2 = UserGroup.build(name: "Testing Result Compare2", interviewModelId: 1, userId: principalUser.id as Long)
        def ug3 = UserGroup.build(name: "Testing Result Compare3", interviewModelId: 1, userId: principalUser.id as Long)
        def ug4 = UserGroup.build(name: "Testing Result Compare4", interviewModelId: 1, userId: principalUser.id as Long)
        
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: ug2.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: ug2.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass3.id, userGroupId: ug2.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: ug3.id)

        //duplicates
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: ug1.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: ug4.id)
        //duplicates but should be removed
        UserGroupAssessmentOrder.build(assessmentOrderId: ass6.id, userGroupId: ug1.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass6.id, userGroupId: ug4.id)
        //standalone should be removed
        UserGroupAssessmentOrder.build(assessmentOrderId: ass4.id, userGroupId: ug4.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass5.id, userGroupId: ug4.id)

        def groupCompare = new GroupCompare(ug1, principleUser.id, "test batch delete")
        groupCompare.addUserGroupToCompare(ug2)
        groupCompare.addUserGroupToCompare(ug3)
        groupCompare.addUserGroupToCompare(ug4)
        groupCompare.save(flush: true, failOnError: true)
        def ugao1 = userGroupAssessmentOrderService.save(groupCompare, ass1.id, groupCompare.id)
        def ugao2 = userGroupAssessmentOrderService.save(groupCompare, ass2.id, groupCompare.id)
        def ugao3 = userGroupAssessmentOrderService.save(groupCompare, ass3.id, groupCompare.id)
        def ugao4 = userGroupAssessmentOrderService.save(groupCompare, ass4.id, groupCompare.id)
        def ugao5 = userGroupAssessmentOrderService.save(groupCompare, ass5.id, groupCompare.id)
        def ugao6 = userGroupAssessmentOrderService.save(groupCompare, ass6.id, groupCompare.id)
        groupCompare.addToGroupAssessmentOrders(ugao1)
        groupCompare.addToGroupAssessmentOrders(ugao2)
        groupCompare.addToGroupAssessmentOrders(ugao3)
        groupCompare.save(flush: true, failOnError: true)

        then:userGroupAssessmentOrderService.fetchDuplicateAssessmentOrderIds(groupCompare).size() == 3
        userGroupAssessmentOrderService.fetchDuplicateAssessmentOrderIds(groupCompare).containsAll([ass1.id, ass2.id, ass6.id])
        userGroupAssessmentOrderService.fetchFromUserGroupIDListToRemove(groupCompare, [ug1.id, ug4.id]).size() == 3
        userGroupAssessmentOrderService.fetchFromUserGroupIDListToRemove(groupCompare, [ug1.id, ug4.id] ).containsAll([ ugao4, ugao5, ugao6])
    }
}
