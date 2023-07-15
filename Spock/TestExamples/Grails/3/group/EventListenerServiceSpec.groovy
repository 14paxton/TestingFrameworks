package com.talentbank.core.group

import com.talentbank.core.User
import com.talentbank.core.eventlistener.AsyncEmailRoutingService
import com.talentbank.core.eventlistener.EventListenerService
import com.talentbank.core.team.SharedTeam
import grails.async.Promises
import grails.buildtestdata.BuildDataTest
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.grails.datastore.mapping.engine.event.PostInsertEvent
import spock.lang.Specification

class EventListenerServiceSpec extends Specification implements DataTest, BuildDataTest ,  ServiceUnitTest<EventListenerService>{
    Closure doWithSpring() {{ ->
        asyncUserGroupEmailService AsyncEmailRoutingService
    }}
    AsyncEmailRoutingService asyncUserGroupEmailService
    
    def setupSpec() {
        mockDomains UserGroupShare
        mockDomains SharedTeam
        mockDomains User
    }

    def cleanup() {
    }

    void "UserGroupShare.PostInsertEvent triggers EventListenerService.save"(){
        given:
        def promiseTask=0
        service.asyncEmailRoutingService = Mock(AsyncEmailRoutingService)

        UserGroupShare ugs = UserGroupShare.build()
        ugs.save()

        PostInsertEvent event = new PostInsertEvent(dataStore, ugs)

        when:
        service.afterInsert(event)

        then:
        1 * service.asyncEmailRoutingService.sendUserGroupShareEmail(_ as Long) >> Promises.task{promiseTask += 1 }
        promiseTask == 1
    }

    void "SharedTeam.PostInsertEvent triggers EventListenerService.save"(){
        given:
        def promiseTask=0
        service.asyncEmailRoutingService = Mock(AsyncEmailRoutingService)

        SharedTeam sharedTeam = SharedTeam.build(user: User.build())
        sharedTeam.save()

        PostInsertEvent event = new PostInsertEvent(dataStore, sharedTeam)

        when:
        service.afterInsert(event)

        then:
        1 * service.asyncEmailRoutingService.sendSharedTeamEmail(_ as Long) >> Promises.task{promiseTask += 1 }
        promiseTask == 1
    }

}