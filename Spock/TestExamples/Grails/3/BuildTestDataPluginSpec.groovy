package com.talentbank.core

import com.talentbank.core.assessmentOrder.AssessmentOrder
import grails.buildtestdata.BuildDataTest
import grails.buildtestdata.TestDataConfigurationHolder
import grails.testing.gorm.DataTest
import spock.lang.Specification


//use BuildDomainTest<[TheDomainBeingTested]> in place of DomainUnitTest<[TheDomainBeingTested]> for domain tests

class BuildTestDataPluginSpec extends Specification implements DataTest, BuildDataTest {

    void setupSpec() {
        mockDomain AssessmentOrder
        mockDomain User
    }

    def setup() {


    }

    def cleanup() {
        TestDataConfigurationHolder.reset()
    }

    //Documentation for plugin
    //https://longwa.github.io/build-test-data/#generating-dynamic-values

    void "create multiple users"() {
        when:
        // when building more than 2 objects, unique properties need to be defined
        (1..55).collect { User.build(username: "$it", email: "email$it@gmail.com") }

        //can also add unique properties to test.resources.TestDataConfig using a variabl
        //and closure use TestDataConfigurationHolder.reset() in setup

        then:
        User.count() == 55
    }

    void "test config holder"() {
        when:
        // can specify and hold property values in holder for when domain is used
        def i = 0
        def userNames = [
                ('com.talentbank.core.User'): [
                        username: { -> i % 2 == 0 ? "Peter" : "Dick" },
                        email   : { -> i++ % 2 == 0 ? "pNorth@gmail.com" : "lilrichard@gmail.com" }]
        ]
        TestDataConfigurationHolder.sampleData = userNames

        def holiday = User.build()
        def hilton = User.build()

        then:
        holiday.username == "Peter"
        hilton.email == "lilrichard@gmail.com"
    }


}