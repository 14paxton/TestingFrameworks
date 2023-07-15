package com.talentbank.core.group

import com.talentbank.core.User
import com.talentbank.core.UserService
import com.talentbank.core.assessmentOrder.AssessmentOrder
import com.talentbank.core.dto.userGroup.command.CompareCommand
import com.talentbank.core.enums.UserGroupType
import grails.buildtestdata.BuildDataTest
import grails.gorm.transactions.Rollback
import grails.gorm.transactions.Transactional
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.grails.datastore.mapping.multitenancy.resolvers.SystemPropertyTenantResolver
import org.grails.orm.hibernate.HibernateDatastore
import org.springframework.transaction.PlatformTransactionManager
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

@Transactional
class ResultCompareServiceSpec extends Specification implements DataTest, BuildDataTest, ServiceUnitTest<ResultCompareService> {
    @Shared
    @AutoCleanup
    HibernateDatastore hibernateDatastore

    @Shared
    PlatformTransactionManager transactionManager

    @Shared
    UserGroupAssessmentOrderService userGroupAssessmentOrderService

    @Shared
    def principalUser
    
    @Shared
    def createAssessments

    @Shared
    def ass1, ass2, ass3, ass4, ass5, ass6, ass7, ass8

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

        hibernateDatastore = new HibernateDatastore(configuration, Group, ResultCompare, GroupCompare, UserGroup, UserGroupAssessmentOrder, GroupCompareJoinUserGroup)
        transactionManager = hibernateDatastore.getTransactionManager()
        userGroupAssessmentOrderService = this.hibernateDatastore.getService(UserGroupAssessmentOrderService)

        principalUser = User.build()

        createAssessments = true
    }

    def createAssessments(){
        ass5 = AssessmentOrder.build()
        ass6 = AssessmentOrder.build()
        ass7 = AssessmentOrder.build(ass5.properties as Map)
        ass8 = AssessmentOrder.build(ass6.properties as Map)
        ass1 = AssessmentOrder.build(ass7.properties as Map)
        ass2 = AssessmentOrder.build(ass8.properties as Map)
        ass3 = AssessmentOrder.build(ass2.properties as Map)
        ass4 = AssessmentOrder.build(ass3.properties as Map)

        createAssessments = false
    }

    def setup() {
        Group.withTransaction {
            AssessmentOrder.all.each {it.delete(flush: true)}
            ResultCompare.all.each {it.delete(flush: true)}
            GroupCompareJoinUserGroup.all.each {it.delete(flush: true)}
            Group.all.each {it.delete(flush: true)}
        }
        if(createAssessments) createAssessments()

        service.userService = Mock(UserService)
        service.userService.getCurrentPrincipalUser() >> principalUser
        service.userGroupAssessmentOrderService = Mock(UserGroupAssessmentOrderService)
        service.userGroupAssessmentOrderService.save(_ as Group, _ as Long, _ as Long) >> { compare, assId, ugId -> userGroupAssessmentOrderService.save(compare, assId, ugId) }
        service.userGroupAssessmentOrderService.findUserGroupAssessmentOrderAssessmentOrderId(_ as Group) >> { Group ug ->userGroupAssessmentOrderService.findUserGroupAssessmentOrderAssessmentOrderId(ug as Group)  }
        service.userGroupAssessmentOrderService.getOrderIdsByUserGroup(_ as Group) >> { Group ug ->userGroupAssessmentOrderService.getOrderIdsByUserGroup(ug as Group)  }
        service.userGroupAssessmentOrderService.findAllByUserGroupCompareAndAssessmentOrderIds(_ , _) >> { Group userGroupCompare, List<Long> asessmentIdList -> userGroupAssessmentOrderService.findAllByUserGroupCompareAndAssessmentOrderIds(userGroupCompare , asessmentIdList)}
    }

    @Rollback
    void "create result compare"() {
        when:
        def userGroup = UserGroup.build(userId: principalUser.id as Long, interviewModelId: 99)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: userGroup.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: userGroup.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass4.id, userGroupId: userGroup.id)

        def command = new CompareCommand(assessmentOrderIds: [ass1.id, ass2.id, ass3.id, ass1.id, ass3.id], groupIds: [userGroup.id] as List<Long>, name: "My Little Result Group", type: UserGroupType.RESULT_COMPARE)


        service.createNewResultCompare(command)

        then:
        ResultCompare.count == 1
    }

    @Rollback
    void "test  delete all"() {
        when:
        def userGroup = UserGroup.build(userId: principalUser.id as Long, interviewModelId: 99)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: userGroup.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: userGroup.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass4.id, userGroupId: userGroup.id)
        hibernateDatastore.sessionFactory.currentSession.flush()

        def command = new CompareCommand(assessmentOrderIds: [ass1.id, ass2.id, ass3.id, ass1.id, ass3.id], groupIds: [userGroup.id] as List<Long>, name: "My delete", type: UserGroupType.RESULT_COMPARE)
        def command2 = new CompareCommand(assessmentOrderIds: [ass1.id, ass2.id, ass3.id, ass1.id, ass3.id], groupIds: [userGroup.id] as List<Long>, name: "My delete 2", type: UserGroupType.RESULT_COMPARE)
        def command3 = new CompareCommand(assessmentOrderIds: [ass1.id, ass2.id, ass3.id, ass1.id, ass3.id], groupIds: [userGroup.id] as List<Long>, name: "My delete 23", type: UserGroupType.RESULT_COMPARE)

        service.createNewResultCompare(command)
        service.createNewResultCompare(command2)
        service.createNewResultCompare(command3)

        then:
        ResultCompare.count == 3


        when:
        def deleteList = [ResultCompare.first().id, ResultCompare.last().id]
        service.deleteResultCompare(deleteList)

        then:
        ResultCompare.count == 1
    }


    @Rollback 
    void "test  update"() {
        when:
        def userGroup = UserGroup.build(userId: principalUser.id as Long, interviewModelId: 99)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: userGroup.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: userGroup.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass4.id, userGroupId: userGroup.id)
        hibernateDatastore.sessionFactory.currentSession.flush()
        def command = new CompareCommand(assessmentOrderIds: [ass1.id, ass2.id], groupIds: [userGroup.id] as List<Long>, name: "MyUpdate", type: UserGroupType.RESULT_COMPARE)
         def returnObject = service.createNewResultCompare(command)

        then:
        ResultCompare.count == 1
        returnObject.status
        returnObject.name == command.name
        returnObject.assessmentOrderIds.size() == 3
        returnObject.assessmentOrderIds.containsAll([ass1.id, ass2.id, ass4.id])

        when:
        def nameUpdate = "New Name"
        def updateCommand = new CompareCommand(id: returnObject?.id , assessmentOrderIds: [ass1.id, ass2.id, ass3.id, ass4.id], name: nameUpdate, type: UserGroupType.RESULT_COMPARE )
        def updateReturnObject = service.updateResultCompareInfo(updateCommand)

        then:
        ResultCompare.count == 1
        updateReturnObject.deleted.size() == 3
        updateReturnObject.added.size() == 1
        updateReturnObject.name == nameUpdate
        updateReturnObject.assessmentOrderIds.size() == 1
        updateReturnObject.assessmentOrderIds[0] == ass3.id
    }
}
