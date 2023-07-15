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
class GroupCompareServiceSpec extends Specification implements DataTest, BuildDataTest, ServiceUnitTest<GroupCompareService>{
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
    def principalUser

    @Shared
    def ass1, ass2, ass3, ass4, ass5, ass6, ass7, ass8
    
    @Shared
    def runServiceSetup

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
                'hibernate.cache.queries': false,
                'hibernate.cache.use_second_level_cache': false,
                'hibernate.cache.use_query_cache': false,
                'hibernate.cache.region.factory_class': 'org.hibernate.cache.ehcache.EhCacheRegionFactory',
                'grails.gorm.multiTenancy.mode': 'DISCRIMINATOR',
                'grails.gorm.multiTenancy.tenantResolverClass': SystemPropertyTenantResolver
        ]

        hibernateDatastore = new HibernateDatastore(configuration, AssessmentOrder, Group, ResultCompare, GroupCompare, UserGroup, UserGroupAssessmentOrder, GroupCompareJoinUserGroup)
        transactionManager = hibernateDatastore.getTransactionManager()
        groupCompareJoinUserGroupService = this.hibernateDatastore.getService(GroupCompareJoinUserGroupService)
        userGroupAssessmentOrderService = this.hibernateDatastore.getService(UserGroupAssessmentOrderService)

        principalUser = User.build()
        runServiceSetup = true
        
        AssessmentOrder.withTransaction {
            ass1 = AssessmentOrder.build()
            ass2 = AssessmentOrder.build()
            ass3 = AssessmentOrder.build( ass1.properties as Map)
            ass4 = AssessmentOrder.build(ass2.properties as Map)
            ass5 = AssessmentOrder.build(ass2.properties as Map)
            ass6 = AssessmentOrder.build(ass1.properties as Map)
            ass7 = AssessmentOrder.build(ass3.properties as Map)
            ass8 = AssessmentOrder.build(ass4.properties as Map)
        }
    }
    
    def createMockServices(){
        service.userService = Mock(UserService)
        service.userService.getCurrentPrincipalUser() >> principalUser
        service.groupCompareJoinUserGroupService = Mock(GroupCompareJoinUserGroupService)
        service.groupCompareJoinUserGroupService.fetchAssociatedAssessments(_ as GroupCompare, true) >> {groupCompare, removeRelationships -> groupCompareJoinUserGroupService.fetchAssociatedAssessments(groupCompare , true)}
        service.groupCompareJoinUserGroupService.countByGroupCompareAndUserGroup(_ as Group, _ as Group) >> {compare, group -> groupCompareJoinUserGroupService.countByGroupCompareAndUserGroup(compare, group)}
        service.groupCompareJoinUserGroupService.fetchAssociatedAssessments(_ as GroupCompare, _) >> { groupCompare, remove -> groupCompareJoinUserGroupService.fetchAssociatedAssessments(groupCompare, true) }
        service.groupCompareJoinUserGroupService.countByGroupCompareAndUserGroup(_ as Group, _ as Group) >> { Group compare, Group userGroup -> groupCompareJoinUserGroupService.countByGroupCompareAndUserGroup(compare, userGroup) }
        service.groupCompareJoinUserGroupService.findAllByUserGroupIdList(_, _ ) >> {Group groupCompare, List<Long> userGroupIdList -> groupCompareJoinUserGroupService.findAllByUserGroupIdList(groupCompare, userGroupIdList) }
        service.userGroupAssessmentOrderService.fetchOrderIdsForRemoval(_, _) >> {Group groupCompare, List<Long> userGroupIdList -> userGroupAssessmentOrderService.fetchOrderIdsForRemoval(groupCompare, userGroupIdList) }

        service.userGroupAssessmentOrderService = Mock(UserGroupAssessmentOrderService)
        service.userGroupAssessmentOrderService.save(_ as Group, _ as Long, _ as Long) >> { compare, assId, ugId -> userGroupAssessmentOrderService.save(compare, assId, ugId) }
    }

    def setup() {
        Group.withTransaction {
            Group.all.each { it.delete(flush: true) }
            UserGroupAssessmentOrder.all.each { it.delete(flush: true) }
        }

        if(runServiceSetup) createMockServices()
    }

    @Rollback
    void "create group compare"() {
        when:
        def userGroup = UserGroup.build(userId: principalUser.id as Long)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: userGroup.id)
        service.createGroupCompare([userGroup.id] as List<Long>, "GetGud", principalUser.id as Long)

        then:
        def gc = GroupCompare.findAll()
        gc.size() == 1
        gc[0].name == "GetGud"
    }

    @Rollback
    void "create GroupCompare with assessment orders"() {
        when:
        def userGroup = UserGroup.build(name: "166", userId: principalUser.id as Long)
        def userGroup2 = UserGroup.build(name: "266", userId: principalUser.id as Long)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: userGroup.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: userGroup.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass2.id, userGroupId: userGroup2.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass1.id, userGroupId: userGroup2.id)
        def command = new CompareCommand(groupIds: [userGroup.id, userGroup2.id] as List<Long>, name : "My Little Group", type: UserGroupType.GROUP_COMPARE )
        def createGroupDTO = service.creatNewGroupCompare( command)

        then:
        createGroupDTO?.assessmentOrderIds?.containsAll([ass1.id, ass2.id])
    }

    @Rollback
    void "ids sent to update, ids matching existing groups will be removed, and new ids will add those groups"() {
        when:
        def userGroup = UserGroup.build(name: "1662", userId: principalUser.id as Long)
        def userGroup2 = UserGroup.build(name: "2662", userId: principalUser.id as Long)
        def userGroup3 = UserGroup.build(name: "26626", userId: principalUser.id as Long)
        def userGroup4 = UserGroup.build(name: "26627", userId: principalUser.id as Long)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass6.id, userGroupId: userGroup.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass5.id, userGroupId: userGroup.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass7.id, userGroupId: userGroup.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass6.id, userGroupId: userGroup2.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass5.id, userGroupId: userGroup2.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass6.id, userGroupId: userGroup3.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass5.id, userGroupId: userGroup3.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass6.id, userGroupId: userGroup4.id)
        UserGroupAssessmentOrder.build(assessmentOrderId: ass5.id, userGroupId: userGroup4.id)
        def command = new CompareCommand(groupIds: [userGroup.id] as List<Long>, name : "My Little Group", type: UserGroupType.GROUP_COMPARE ) 
        def createGroupDTO = service.creatNewGroupCompare(command, true)

        then:
        createGroupDTO?.groupIds?.size() == 1
        createGroupDTO?.groupIds[0] == userGroup.id
        String nameUpdate = "Changed"
        
        when:"add same group"
        def updateCommand = new CompareCommand(id: createGroupDTO?.id, groupIds:  [ userGroup.id], name: nameUpdate, type: UserGroupType.GROUP_COMPARE )
        def updateGroupDTO = service.updateGroupCompareInfo(updateCommand, true )

        then:
        updateGroupDTO.error == "can not have 0 groups"
        
        when:"add same group and new groups"
        String nameUpdate2 = "updated"
        def updateCommand2 = new CompareCommand(id: createGroupDTO?.id, groupIds:  [userGroup2.id, userGroup3.id, userGroup.id], name: nameUpdate2, type: UserGroupType.GROUP_COMPARE )
        def updateGroupDTO2 = service.updateGroupCompareInfo(updateCommand2, true )
        
        then:
        updateGroupDTO2?.groupIds?.size() == 2
        updateGroupDTO2?.groupIds?.containsAll([userGroup2.id, userGroup3.id])
        updateGroupDTO2?.name == nameUpdate2
        updateGroupDTO2.deleted[0] == userGroup.id
        updateGroupDTO2.added.containsAll([userGroup2.id, userGroup3.id])
    }

    @Rollback
    void "delete group compare"() {
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
        groupCompare.addToGroupAssessmentOrders(userGroupAssessmentOrderService.save(groupCompare, ass1.id, groupCompare.id))
        groupCompare.addToGroupAssessmentOrders(userGroupAssessmentOrderService.save(groupCompare, ass2.id, groupCompare.id))

        def groupCompare2 = new GroupCompare(ug1, principleUser.id, "test delete2")
        groupCompare2.addUserGroupToCompare(ug2)
        groupCompare2.addUserGroupToCompare(ug3)
        groupCompare2.addUserGroupToCompare(ug4)
        groupCompare2.save(flush: true, failOnError: true)
        groupCompare2.addToGroupAssessmentOrders(userGroupAssessmentOrderService.save(groupCompare2, ass1.id, groupCompare2.id))
        groupCompare2.addToGroupAssessmentOrders(userGroupAssessmentOrderService.save(groupCompare2, ass3.id, groupCompare2.id))

        def groupCompare3 = new GroupCompare(ug1, principleUser.id, "test delete3")
        groupCompare3.addUserGroupToCompare(ug2)
        groupCompare3.addUserGroupToCompare(ug3)
        groupCompare3.addUserGroupToCompare(ug4)
        groupCompare3.save(flush: true, failOnError: true)
        groupCompare3.addToGroupAssessmentOrders(userGroupAssessmentOrderService.save(groupCompare3, ass1.id, groupCompare3.id))
        groupCompare3.addToGroupAssessmentOrders(userGroupAssessmentOrderService.save(groupCompare3, ass2.id, groupCompare3.id))

        then:
        GroupCompare.count == 3

        when:
        service.deleteGroupCompare([groupCompare.id, groupCompare3.id])
        hibernateDatastore.currentSession.flush()

        then:
        GroupCompare.count == 1
    }

    @Rollback
    void "remove group"() {
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
        groupCompare.addToGroupAssessmentOrders(userGroupAssessmentOrderService.save(groupCompare, ass1.id, groupCompare.id))
        groupCompare.addToGroupAssessmentOrders(userGroupAssessmentOrderService.save(groupCompare, ass2.id, groupCompare.id))
        groupCompare.addToGroupAssessmentOrders(userGroupAssessmentOrderService.save(groupCompare, ass3.id, groupCompare.id))

        then:
        groupCompare.userGroupJoinGroupCompare.size() == 4
        groupCompare.groupAssessmentOrders.size() == 3
        UserGroupAssessmentOrder.count == 9
        GroupCompareJoinUserGroup.count == 4

        when:
        service.removeGroupFromGroupCompare(groupCompare.id, [ug2.id])

        then:
        groupCompare.userGroupJoinGroupCompare.size()  == 3
        
        when:
        service.removeGroupFromGroupCompare(groupCompare.id, [ug4.id])
        hibernateDatastore.sessionFactory.currentSession.flush()
        then:
        groupCompare.userGroupJoinGroupCompare.size()  == 2
        GroupCompareJoinUserGroup.count == 2
    }
}
