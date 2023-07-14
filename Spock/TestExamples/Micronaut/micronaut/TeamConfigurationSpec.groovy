package com.example.micronaut

import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Specification

@MicronautTest
class MicronautConfigurationSpec extends Specification {


    void "test team configuration"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
                "team.name": 'evolution',
                "team.color": 'green',
                "team.player-names": ['Nirav Assar', 'Lionel Messi']
        ])

        when:
        TeamConfiguration teamConfiguration = ctx.getBean(TeamConfiguration)

        then:
        teamConfiguration.name == "evolution"
        teamConfiguration.color == "green"
        teamConfiguration.playerNames[0] == "Nirav Assar"
        teamConfiguration.playerNames[1] == "Lionel Messi"

        cleanup:
        ctx.close()
    }

    void "test team configuration admin configuration builder "() {
        given:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
                "team.name": 'evolution',
                "team.color": 'green',
                "team.player-names": ['Nirav Assar', 'Lionel Messi'],
                "team.team-admin.manager": "Jerry Jones",
                "team.team-admin.coach": "Tommy O'Neill",
                "team.team-admin.president": "Mark Scanell"
        ])

        when:
        TeamConfiguration teamConfiguration = ctx.getBean(TeamConfiguration)
        TeamAdmin teamAdmin = teamConfiguration.builder.build()

        then:
        teamConfiguration.name == "evolution"
        teamConfiguration.color == "green"
        teamConfiguration.playerNames[0] == "Nirav Assar"
        teamConfiguration.playerNames[1] == "Lionel Messi"

        // check the builder has values set
        teamConfiguration.builder.manager == "Jerry Jones"
        teamConfiguration.builder.coach == "Tommy O'Neill"
        teamConfiguration.builder.president == "Mark Scanell"

        // check the object can be built
        teamAdmin.manager == "Jerry Jones"
        teamAdmin.coach == "Tommy O'Neill"
        teamAdmin.president == "Mark Scanell"

        cleanup:
        ctx.close()
    }



}