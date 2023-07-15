package com.talentbank.core.group

import com.talentbank.core.ClientSetup
import com.talentbank.core.User
import com.talentbank.core.assessmentOrder.AssessmentOrder
import grails.buildtestdata.BuildDataTest
import grails.gorm.transactions.Rollback
import grails.gorm.transactions.Transactional
import grails.testing.gorm.DataTest
import grails.validation.ValidationException
import org.grails.datastore.mapping.multitenancy.resolvers.SystemPropertyTenantResolver
import org.grails.orm.hibernate.HibernateDatastore
import org.grails.testing.GrailsUnitTest
import org.springframework.transaction.PlatformTransactionManager
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

@Transactional
class GroupCompareJoinUserGroupServiceSpec extends Specification implements DataTest, BuildDataTest, GrailsUnitTest{
    @Shared
    @AutoCleanup
    HibernateDatastore hibernateDatastore

    @Shared
    PlatformTransactionManager transactionManager
    
    @Shared
   GroupCompareJoinUserGroupService groupCompareJoinUserGroupService
    
    @Shared
    UserGroupAssessmentOrderService userGroupAssessmentOrderService
    
    @Shared
    def ass1, ass2, ass3, ass4
    
    def setupSpec() {
        mockDomain UserGroup
        mockDomain ResultCompare
        mockDomain Group
        mockDomain User
        mockDomain GroupCompare
        mockDomain UserGroupAssessmentOrder
        mockDomain GroupCompareJoinUserGroup
        mockDomain AssessmentOrder
        mockDomain ClientSetup

        Map configuration = [
                'hibernate.hbm2ddl.auto': 'create-drop',
                'dataSource.url': 'jdbc:h2:mem:myDB',
                'hibernate.cache.queries': false,
                'hibernate.cache.use_second_level_cache': false,
                'hibernate.cache.use_query_cache': false,
                'hibernate.cache.region.factory_class': 'org.hibernate.cache.ehcache.EhCacheRegionFactory',
                'grails.gorm.multiTenancy.mode': 'DISCRIMINATOR',
                'grails.gorm.multiTenancy.tenantResolverClass': SystemPropertyTenantResolver
        ]
        hibernateDatastore = new HibernateDatastore(configuration, AssessmentOrder, ClientSetup, GroupCompare, UserGroup, UserGroupAssessmentOrder, GroupCompareJoinUserGroup, Group, ResultCompare)
        transactionManager = hibernateDatastore.getTransactionManager()
        userGroupAssessmentOrderService = this.hibernateDatastore.getService(UserGroupAssessmentOrderService)
        groupCompareJoinUserGroupService = this.hibernateDatastore.getService(GroupCompareJoinUserGroupService)

        AssessmentOrder.withTransaction {
            ass1 = AssessmentOrder.build()
            ass2 = AssessmentOrder.build()
            ass3 = AssessmentOrder.build(ass1.properties as Map)
            ass4 = AssessmentOrder.build(ass2.properties as Map)
        }
    }
    
    def setup(){
        
        Group.withTransaction {
            Group.all.each {it.delete(flush: true)}
            UserGroupAssessmentOrder.all.each {it.delete(flush: true)}
        }

    }

    @Rollback
    void "interface methods work"() {
        when:
        def principleUser = User.build()
        def ug1 = UserGroup.build(name: "Testing Result Compare", interviewModelId: 1)
        def ug2 = UserGroup.build(name: "Testing Result Compare2", interviewModelId: 1)
        def ug3 = UserGroup.build(name: "Testing Result Compare3", interviewModelId: 1)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: ug1.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: ug2.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: ug2.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: ug3.id)
        def groupCompare = new GroupCompare(ug1, principleUser.id, "test interface")
        groupCompare.addUserGroupToCompare(ug2)
        groupCompare.addUserGroupToCompare(ug3)
        groupCompare.save(flush: true, failOnError: true)

        then:
        groupCompareJoinUserGroupService.count() == 3
        groupCompareJoinUserGroupService.countByGroupCompare(groupCompare) == 3
        groupCompareJoinUserGroupService.findAll(groupCompare).size() == 3
        groupCompareJoinUserGroupService.findAll(groupCompare)*.userGroup.containsAll([ug1, ug2, ug3])

    }

    @Rollback
    void "find all will return usergroups"() {
        when:
        def principleUser = User.build()
        def ug1 = UserGroup.build(name: "Testing Result Compare69", interviewModelId: 1)
        def ug2 = UserGroup.build(name: "Testing Result Compare29", interviewModelId: 1)
        def ug3 = UserGroup.build(name: "Testing Result Compare39", interviewModelId: 1)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: ug1.id)
        ug2.addToGroupAssessmentOrders(UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: ug2.id))
        ug2.addToGroupAssessmentOrders(UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: ug2.id))
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: ug3.id)
        
        def groupCompare = new GroupCompare(ug1, principleUser.id, "test interface find usergroups")
        groupCompare.addUserGroupToCompare(ug2)
        groupCompare.addUserGroupToCompare(ug3)
        groupCompare.save(flush: true, failOnError: true)

        def groupCompare2 = new GroupCompare(ug2, principleUser.id, "test interface find usergroups2")
        groupCompare2.addUserGroupToCompare(ug1)
        groupCompare2.addUserGroupToCompare(ug3)
        groupCompare2.save(flush: true, failOnError: true)
        groupCompare2.addToGroupAssessmentOrders(userGroupAssessmentOrderService.save(groupCompare2, ass1.id, groupCompare2.id))
        groupCompare2.addToGroupAssessmentOrders(userGroupAssessmentOrderService.save(groupCompare2, ass2.id, groupCompare2.id))

        then:
        groupCompareJoinUserGroupService.findUserGroup(groupCompare).size() == 3
        groupCompareJoinUserGroupService.findAllByUserGroupIdList(groupCompare, [ug1.id, ug2.id]).size() == 2
    }

    @Rollback
    void "find all unique assessment ids based on groups assinged to group compare"() {
        when:
        def principleUser = User.build()
        def ug1 = UserGroup.build(name: "Testing Result findUserGroupAssessmentOrder", interviewModelId: 1)
        def ug2 = UserGroup.build(name: "Testing Result findUserGroupAssessmentOrder2", interviewModelId: 1)
        def ug3 = UserGroup.build(name: "Testing Result findUserGroupAssessmentOrder3", interviewModelId: 1)
        def ug4 = UserGroup.build(name: "Testing Result findUserGroupAssessmentOrder4", interviewModelId: 1)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: ug1.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: ug2.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: ug3.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: ug4.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: ug4.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass3.id, userGroupId: ug4.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass4.id, userGroupId: ug4.id)

        def groupCompare = new GroupCompare(ug1, principleUser.id, "test interface find assessment ids")
        groupCompare.addUserGroupToCompare(ug2)
        groupCompare.addUserGroupToCompare(ug3)
        groupCompare.save(flush: true, failOnError: true)

        then:"assessment ids for the added user groups should be returned"
        groupCompareJoinUserGroupService.fetchAssociatedAssessments(groupCompare).containsAll([ass1.id, ass2.id])
        groupCompareJoinUserGroupService.fetchAssociatedAssessments(groupCompare, true).containsAll([ass1.id, ass2.id])
        groupCompareJoinUserGroupService.fetchAssociatedAssessments(groupCompare, false, true).collect {it.id}.containsAll([ass1.id, ass2.id])
        groupCompareJoinUserGroupService.fetchAssociatedAssessments(groupCompare, true, true).collect {it.id}.containsAll([ass1.id, ass2.id])

        when:
        groupCompare.addUserGroupToCompare(ug4)
        groupCompare.save(flush: true, failOnError: true)
        
        then:
        groupCompareJoinUserGroupService.fetchAssociatedAssessments(groupCompare).containsAll(AssessmentOrder.all*.id)
        groupCompareJoinUserGroupService.fetchAssociatedAssessments(groupCompare, true).containsAll(AssessmentOrder.all*.id)
        groupCompareJoinUserGroupService.fetchAssociatedAssessments(groupCompare, false, true).collect {it.id}.containsAll(AssessmentOrder.all*.id)
        groupCompareJoinUserGroupService.fetchAssociatedAssessments(groupCompare, true, true).collect {it.id}.containsAll(AssessmentOrder.all*.id)
        
        when:
        groupCompare.addToGroupAssessmentOrders(userGroupAssessmentOrderService.save(groupCompare, ass3.id, groupCompare.id))
        groupCompare.addToGroupAssessmentOrders(userGroupAssessmentOrderService.save(groupCompare, ass4.id, groupCompare.id))
        
        then:
        groupCompareJoinUserGroupService.fetchAssociatedAssessments(groupCompare).containsAll(AssessmentOrder.all*.id)
        groupCompareJoinUserGroupService.fetchAssociatedAssessments(groupCompare, true).containsAll([ass1.id, ass2.id])
        groupCompareJoinUserGroupService.fetchAssociatedAssessments(groupCompare, false, true).collect {it.id}.containsAll(AssessmentOrder.all*.id)
        groupCompareJoinUserGroupService.fetchAssociatedAssessments(groupCompare, true, true).collect {it.id}.containsAll([ass1.id, ass2.id])
    }

    @Rollback
    void "should not be able to save same group to compare"(){
        given:
        def principleUser = User.build()
        def ug1 = UserGroup.build(name: "Testing Result findUserGroupAssessmentOrder", interviewModelId: 1)
        def ug2 = UserGroup.build(name: "Testing Result findUserGroupAssessmentOrder2", interviewModelId: 2)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: ug1.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: ug2.id)

        def groupCompare = new GroupCompare(ug1, principleUser.id, "test group save")
        groupCompare.save(flush: true, failOnError: true)
        
        when:
        groupCompareJoinUserGroupService.save(groupCompare, ug1)
        
        then:
        ValidationException ex = thrown()
        assert ex
    }

    @Rollback
    void "interview model validation, im should be the same for comparing"(){
        when:
        def principleUser = User.build()
        def ug1 = UserGroup.build(name: "Testing Result Compare33", interviewModelId: 1)
        def ug2 = UserGroup.build(name: "Testing Result Compare23", interviewModelId: 1 )
        def ug3 = UserGroup.build(name: "Testing Result 99", interviewModelId: 3 )
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: ug1.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: ug2.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: ug3.id)

        ug1.save(flush: true, insert: true)

        def groupCompare = new GroupCompare(ug1, principleUser.id , 'test group compare1')
        groupCompare.save(flush: true, failOnError: true)
        groupCompareJoinUserGroupService.save(groupCompare, ug2)

        then:
        groupCompareJoinUserGroupService.countByGroupCompare(groupCompare) == 2
        groupCompareJoinUserGroupService.countByGroupCompareAndUserGroup(groupCompare, ug2) == 1
        
        when:"saving a user group with a different interview model"
        groupCompareJoinUserGroupService.save(groupCompare, ug3)
        
        then:"an error is thrown"
        ValidationException ex = thrown()
        ex.errors.getAllErrors()[0].defaultMessage == "interviewModelId is not the same for these groups"
        
    }

    @Rollback
    void "getting all assessment ids including duplicates"(){
        when:
        def principleUser = User.build()
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

        def groupCompare = new GroupCompare(ug1, principleUser.id, "test interface")
        groupCompare.addUserGroupToCompare(ug2)
        groupCompare.addUserGroupToCompare(ug3)
        groupCompare.addUserGroupToCompare(ug4)
        groupCompare.save(flush: true, failOnError: true)

        then:
        groupCompareJoinUserGroupService.fetchAllAssessmentIdsIncludeDuplicates(groupCompare).size() == 6
        when: "delete by list"
        groupCompareJoinUserGroupService.deleteByUserGroup(ug4)

        then:
        groupCompareJoinUserGroupService.fetchAllAssessmentIdsIncludeDuplicates(groupCompare).size() == 4
       
    }
    
}
