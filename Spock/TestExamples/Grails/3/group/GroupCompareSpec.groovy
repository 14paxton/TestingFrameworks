package com.talentbank.core.group

import com.talentbank.core.User
import com.talentbank.core.assessmentOrder.AssessmentOrder
import com.talentbank.core.enums.UserGroupType
import grails.buildtestdata.BuildDataTest
import grails.buildtestdata.BuildDomainTest
import grails.gorm.transactions.Rollback
import grails.gorm.transactions.Transactional
import grails.testing.gorm.DataTest
import org.grails.datastore.mapping.multitenancy.resolvers.SystemPropertyTenantResolver
import org.grails.orm.hibernate.HibernateDatastore
import org.springframework.transaction.PlatformTransactionManager
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

@Transactional
class GroupCompareSpec extends Specification implements DataTest, BuildDataTest, BuildDomainTest<GroupCompare> {

    @Shared
    @AutoCleanup
    HibernateDatastore hibernateDatastore

    @Shared
    PlatformTransactionManager transactionManager

    def setupSpec() {
        mockDomain UserGroup
        mockDomain User
        mockDomain ResultCompare
        mockDomain Group
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
        hibernateDatastore = new HibernateDatastore(configuration, GroupCompare, UserGroup, UserGroupAssessmentOrder, GroupCompareJoinUserGroup, Group, ResultCompare)
        transactionManager = hibernateDatastore.getTransactionManager()
    }

    def setup() {
        hibernateDatastore.currentSession.delete(GroupCompareJoinUserGroup.all)
        hibernateDatastore.currentSession.delete(AssessmentOrder.all)
        hibernateDatastore.currentSession.delete(Group.all)
        hibernateDatastore.currentSession.delete(UserGroupAssessmentOrder.all)
    }

    @Rollback
    void "groups should be only be found by class"() {
        when:
        UserGroup.build(name: "ug2", type: UserGroupType.RESULTGROUP, interviewModelId: 1)
        UserGroup.build(name: "ugSaved", type: UserGroupType.MYSAVEDGROUP, interviewModelId: 1)
        GroupCompare.build(name: 'gc1', type: UserGroupType.GROUP_COMPARE)
        ResultCompare.build(name: "rc1", type: UserGroupType.RESULT_COMPARE)

        then:
        Group.count() == 4
        UserGroup.count() == 2
        GroupCompare.count() == 1
        ResultCompare.count() == 1
    }

    @Rollback
    void "that Group Compare will build"() {
        when:
        def principleUser = User.build()
        def ass1 = AssessmentOrder.build()
        def userGroup = UserGroup.build(interviewModelId: 1)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: userGroup.id)
        def groupCompare = new GroupCompare(userGroup, principleUser.id, " build compare")
        groupCompare.save(flush: true)

        then:
        groupCompare.type == UserGroupType.GROUP_COMPARE
    }

    @Rollback
    void "test that there are groups to compare"() {
        when:
        def principleUser = User.build()
        def ass1 = AssessmentOrder.build()
        def ass2 = AssessmentOrder.build()
        def ug1 = UserGroup.build(name: "Testing Result Compare", interviewModelId: 1)
        def ug2 = UserGroup.build(name: "Testing Result Compare2" , interviewModelId: 1)
        def ug3 = UserGroup.build(name: "Testing Result Compare3" , interviewModelId: 1)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: ug1.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: ug2.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: ug2.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: ug3.id)
        def groupCompare = new GroupCompare(ug1, principleUser.id, 'test assessments')
        groupCompare.addUserGroupToCompare(ug2)
        groupCompare.addUserGroupToCompare(ug3)
        groupCompare.save(flush: true)

        def totalGroupAssessmentOrders = GroupCompare.findById(groupCompare.id).userGroupJoinGroupCompare.collect {
            UserGroupAssessmentOrder.findByUserGroupId(it.userGroup.id)
        }

        then:
        totalGroupAssessmentOrders.size() == 3

    }

    @Rollback
    void "test that there are numerous GroupCompares being assigned to UserGroups"() {
        when:
        def principleUser = User.build()
        def ass1 = AssessmentOrder.build()
        def ass2 = AssessmentOrder.build()
        def ug1 = UserGroup.build(name: "Testing Result Compare33", interviewModelId: 1)
        def ug2 = UserGroup.build(name: "Testing Result Compare23",  interviewModelId: 1)
        def ug3 = UserGroup.build(name: "Testing Result Compare3333",  interviewModelId: 1)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: ug1.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: ug2.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: ug2.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: ug3.id)
        
        ug1.save(flush: true, insert: true)
        
        def groupCompare = new GroupCompare(ug1, principleUser.id , 'test group compare1')
        groupCompare.addUserGroupToCompare(ug2)
        groupCompare.addUserGroupToCompare(ug3)
        groupCompare.save(flush: true, failOnError: true)

        def groupCompare2 = new GroupCompare(ug2, principleUser.id, 'test group compares2' )
        groupCompare2.addUserGroupToCompare(ug1)
        groupCompare2.addUserGroupToCompare(ug3)
        groupCompare2.save(flush: true, failOnError: true)

        def groupCompare3 = new GroupCompare(ug3, principleUser.id , 'test group compare3')
        groupCompare3.addUserGroupToCompare(ug2)
        groupCompare3.addUserGroupToCompare(ug1)
        groupCompare3.save(flush: true, failOnError: true)

        then:
        GroupCompare.last().userGroupJoinGroupCompare.size() >=1
    }
}
