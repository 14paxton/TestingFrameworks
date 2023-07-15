package tbservice

import grails.buildtestdata.BuildDataTest
import grails.gorm.transactions.Rollback
import grails.gorm.transactions.Transactional
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.grails.orm.hibernate.HibernateDatastore
import org.grails.testing.GrailsUnitTest
import org.hibernate.SessionFactory
import org.springframework.transaction.PlatformTransactionManager
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

@Transactional
class UserRoleGroupServiceSpec extends Specification implements DataTest, BuildDataTest, GrailsUnitTest {
    @Shared
    @AutoCleanup
    HibernateDatastore hibernateDatastore

    @Shared
    PlatformTransactionManager transactionManager

    @Shared
    SessionFactory sessionFactory

    @Shared
    UserRoleGroupService userRoleGroupService

    @Shared
    def SUPER_ADMIN, TALENT_PLUS_ADMIN, TALENTBANK_CLIENT_ADMIN, HR_MANAGER, REQUESTOR, newUser, user2, user3, user4, setup

    def setupSpec() {
        mockDomains(User, UserRoleGroup, RoleGroup)

        Map configuration = [
                'hibernate.hbm2ddl.auto': 'create-drop',
                'dataSource.url': 'jdbc:h2:mem:myDB',
        ]
        hibernateDatastore = new HibernateDatastore(configuration, User, UserRoleGroup, RoleGroup)
        transactionManager = hibernateDatastore.getTransactionManager()
        sessionFactory = hibernateDatastore.getSessionFactory()
        this.userRoleGroupService = this.hibernateDatastore.getService(UserRoleGroupService)
    }


    def setup() {
        if (!setup) {
            SUPER_ADMIN = RoleGroup.build(name: 'tbcore-rg-admin-super', displayName: 'SUPER ADMIN')
            TALENT_PLUS_ADMIN = RoleGroup.build(name: 'tbcore-rg-admin-tp', displayName: 'TALENT PLUS ADMIN')
            TALENTBANK_CLIENT_ADMIN = RoleGroup.build(name: 'tbcore-rg-admin-client', displayName: 'TALENTBANK CLIENT ADMIN')
            HR_MANAGER = RoleGroup.build(name: 'tbcore-rg-manager', displayName: 'HR MANAGER')
            REQUESTOR = RoleGroup.build(name: 'tbcore-rg-requestor', displayName: 'REQUESTOR')
            newUser = User.build(dateCreated: new Date(), lastUpdated: new Date())
            user2 = User.build(dateCreated: new Date(), lastUpdated: new Date())
            user3 = User.build(dateCreated: new Date(), lastUpdated: new Date())
            user4 = User.build(dateCreated: new Date(), lastUpdated: new Date())
            newUser.save(flush: true, failOnError: true)
            user2.save(flush: true, failOnError: true)
            user3.save(flush: true, failOnError: true)
            user4.save(flush: true, failOnError: true)
            setup = true
        }
    }

    @Rollback
    void "correct_user_role_groups_should_be_fetched"() {
        def originalRoles = [TALENT_PLUS_ADMIN, TALENTBANK_CLIENT_ADMIN, REQUESTOR]
        def user2Roles = [SUPER_ADMIN, TALENT_PLUS_ADMIN, TALENTBANK_CLIENT_ADMIN, HR_MANAGER, REQUESTOR]
        def user3Roles = [TALENTBANK_CLIENT_ADMIN, HR_MANAGER, REQUESTOR]

        when:"we add role groups to our users"
        originalRoles.each {
            def urg = UserRoleGroup.create(newUser, it as RoleGroup)
            urg.save(flush: true, failOnError: true)
        }

        user2Roles.each {
            def urg = UserRoleGroup.create(user2, it as RoleGroup)
            urg.save(flush: true, failOnError: true)
        }

        user3Roles.each {
            def urg = UserRoleGroup.create(user3, it as RoleGroup)
            urg.save(flush: true, failOnError: true)
        }
        

        then:"no adding should be done"
        Map<User, String> userMapToRequestRGString = [(newUser) : 'TALENT_PLUS_ADMIN/ TALENTBANK_CLIENT_ADMIN/ REQUESTOR'] as Map<User, String>
        def changes = userRoleGroupService.roleGroupsHaveChanges(userMapToRequestRGString)

        then:"no changes and no new groups will just return true"
        !changes

        then:"remove changes"
        Map<User, String> removeMap = [(newUser) : 'TALENT_PLUS_ADMIN/ REQUESTOR'] as Map<User, String>
        def changesRemove = userRoleGroupService.roleGroupsHaveChanges(removeMap)

        then:"no changes and no new groups will just return true"
        changesRemove
        changesRemove.get(newUser.id)*.roleGroup.sort() ==  (originalRoles - TALENT_PLUS_ADMIN).sort()
        
        when:"we send multiple users, should receive multiple results"
        Map<User, String> multiUser = [(newUser) : 'TALENT_PLUS_ADMIN/ TALENTBANK_CLIENT_ADMIN/ REQUESTOR'] as Map<User, String>
        multiUser.put((user2), 'SUPER_ADMIN/ TALENT_PLUS_ADMIN/REQUESTOR')
        multiUser.put((user3), 'TALENTBANK_CLIENT_ADMIN/ HR_MANAGER/ REQUESTOR')
        multiUser.put((user4), 'TALENTBANK_CLIENT_ADMIN/ HR_MANAGER/ REQUESTOR')
        def multiUserChanges = userRoleGroupService.roleGroupsHaveChanges(multiUser)
        
        then:
        multiUserChanges
        !multiUserChanges.containsKey(newUser.id)
        multiUserChanges.get(user2.id)*.roleGroup.sort() ==  (user2Roles - TALENT_PLUS_ADMIN - SUPER_ADMIN).sort()
        multiUserChanges.containsKey(user4.id)
        !multiUserChanges.get(user4.id)
        
        when:"we include the admin"
        Map<User, String> adminIncludeMap = [(user2) : 'TALENT_PLUS_ADMIN/ REQUESTOR'] as Map<User, String>
        def adminIncluded = userRoleGroupService.roleGroupsHaveChanges(adminIncludeMap, true)
        
        then:
        adminIncluded
        !adminIncluded.containsKey(newUser.id)
        adminIncluded.get(user2.id)*.roleGroup.sort() ==  user2Roles.sort()
    }
}