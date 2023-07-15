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
class ResultCompareSpec extends Specification implements DataTest, BuildDataTest, BuildDomainTest<ResultCompare> {
    @Shared
    @AutoCleanup
    HibernateDatastore hibernateDatastore

    @Shared
    PlatformTransactionManager transactionManager
    
    def setupSpec() {
        mockDomain Group
        mockDomain UserGroup
        mockDomain GroupCompare
        mockDomain User
        mockDomain ResultCompare
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
    void "result compare should build, and have unique assessment from primary and list given to constructor "() {
        when:
        def principalUser = User.build()
        AssessmentOrder.build()
        def ass1 = AssessmentOrder.build()
        
        def userGroup = UserGroup.build(name: "ug", type: UserGroupType.RESULTGROUP, interviewModelId: 1)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: userGroup.id)
        
        def resultCompare = new ResultCompare(userGroup, principalUser.id as Long, 'test result compare')
        resultCompare.save(flush: true, failOnError: true)

        def totalGroupAssessmentOrders =  UserGroupAssessmentOrder.findAllByUserGroupId(ResultCompare.findById(resultCompare.id).groupCompareJoinUserGroup.userGroup.id)

        then:
        Group.count() == 2
        ResultCompare.get(resultCompare.id).type == UserGroupType.RESULT_COMPARE
        totalGroupAssessmentOrders.size() == 1
    }
}
