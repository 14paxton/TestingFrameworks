package com.talentbank.core.event

import com.talentbank.core.ClientSetup
import com.talentbank.core.Event
import com.talentbank.core.EventAudit
import com.talentbank.core.User
import com.talentbank.core.assessmentOrder.AssessmentOrder
import com.talentbank.core.assessmentOrder.AssessmentOrderRequest
import com.talentbank.core.catalog.CatalogDetail
import com.talentbank.core.content.TextString
import com.talentbank.core.dto.office.CalendarEventDTO
import com.talentbank.core.enums.AssessmentType
import com.talentbank.core.enums.EmailTemplateType
import com.talentbank.core.enums.Source
import com.talentbank.core.enums.StringType
import com.talentbank.core.interviewBuilder.InterviewModel
import com.talentbank.core.notify.EmailTemplate
import com.talentbank.core.security.SecurityGroup
import grails.buildtestdata.BuildDataTest
import grails.buildtestdata.TestDataConfigurationHolder
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import grails.testing.web.GrailsWebUnitTest
import groovy.time.TimeCategory
import spock.lang.Specification

class CalenderNotificationBuilderServiceSpec extends Specification implements DataTest, BuildDataTest, ServiceUnitTest<CalenderNotificationBuilderService>, GrailsWebUnitTest {

    def setup() {
        mockDomain Event
        mockDomain AssessmentOrder
        mockDomain User
        mockDomain EventAudit
        mockDomain InterviewModel
        mockDomain CatalogDetail
        mockDomain SecurityGroup
        mockDomain ClientSetup
        mockDomain EmailTemplate
        mockDomain AssessmentOrderRequest
        mockDomain TextString

        EmailTemplate.build(name: "outlookSchedulingInterviewEmail", emailTemplateType: EmailTemplateType.SCHEDULED_INTERVIEW_OUTLOOK_APPOINTMENT, clientSetup: null,
                subject: TextString.build(type: StringType.GLOBALOUTLOOKSCHEDULINGINTERVIEWSUBJECT, en: 'Interview: @@intervieweeFullName@@ (@@company@@)'),
                body: TextString.build(type: StringType.GLOBALOUTLOOKSCHEDULINGINTERVIEWBODY,
                        en: """<p><strong>Scheduled Interview Summary</strong></p>
                                       |<p><strong>Company</strong>: @@company@@<br>
                                       |<strong>Interview Order</strong>: @@assessmentOrderUrl@@</a><br>
                                       |<strong>Scheduler</strong>: @@scheduledByFullName@@ &lt;<a href="mailto:@@scheduledByEmail@@">@@scheduledByEmail@@</a>&gt;</p>
                                       |<p><strong>== Interviewee Info ==</strong><br>
                                       |<strong>Name</strong>: @@intervieweeFullName@@<br>
                                       |<strong>Time Zone</strong>: @@timeZone@@<br>
                                       |<strong>Start Time</strong>: @@startTime@@<br>
                                       |<strong>End Time</strong>: @@endTime@@<br>
                                       |<strong>Phone</strong>: @@intervieweePhone@@<br>
                                       |<strong>Language</strong>: @@language@@<br>
                                       |<strong>Interview</strong>: @@interview@@<br>
                                       |<strong>Job Title</strong>: @@jobTitle@@<br>
                                       |<strong>Purpose</strong>: @@purpose@@<br>
                                       |<strong>@@securityGroupLabel@@</strong>: @@securityGroupValue@@</p>""".stripMargin()),
                sentFrom: "Talent Plus Solutions", isDefault: true, updatedByEmail: 'bpaxton@talentplus.com')

    }

    def cleanup() {
    }

    void "create CalendarEventDTO"(){
        setup:"create new event with assessment order and an associated interviewModel"
        TestDataConfigurationHolder.reset()
        def secGroup = SecurityGroup.build(name: "KC Strip Mall")
        def intModel = InterviewModel.build(estimatedTime: 30, source: Source.TBFIVE, sourceId: "999")
        def catDetail = CatalogDetail.build(interviewModelId: "999", companyName: "Leisure Suits By Larry", type: AssessmentType.P2P.key)
        def assOrderReq = AssessmentOrderRequest.build(lang : 'en', phone: '8675309' )
        def assOrder = AssessmentOrder.build(catalogDetail: catDetail, securityGroup: secGroup ,
                assessmentOrderUrl: "www.url.com", assessmentOrderRequests: [assOrderReq] , assessmentPurpose: 'EXTRN',
                companyInterviewName: "MyLittleInterview", jobTitle: "Nigerian Prince", firstName: "Ken" , lastName: "Hillington" )

        def interviewer = User.build(firstName: "Doug", lastName: "Flooty")
        def scheduledBy = User.build()
        def theEvent = Event.build(assessmentOrder: assOrder, scheduledDate: new Date(), eventOwner: interviewer,
                scheduledBy: scheduledBy, intervieweeTimeZone:"US/Arizona" )

        ClientSetup.metaClass.static.fetchSecurityGroupLabelsByClientSetupId = {Long id, String en -> [secGroupNameLabel : 'secGroupNameLabel', secGroupCodeLabel : 'secGroupCodeLabel']}

        when: "we pass the new event to the method to get the DTO"
        def calenderDTO = service.createCalendarEvent(theEvent)

        then:"we get CalendarEventDTO"
        calenderDTO instanceof CalendarEventDTO
        theEvent.scheduledDate.format("yyyy-MM-dd'T'HH:mm:ss") == calenderDTO.start.dateTime
        calenderDTO.subject == "Interview: Ken Hillington (Leisure Suits By Larry)"
        calenderDTO.body.content.contains("secGroupNameLabel")

        when: "we add the duration"
        def endDate = theEvent.scheduledDate
        use( TimeCategory ) {
            endDate = endDate + 30.minutes
        }

        then: "the end date is equal"
        endDate.format("yyyy-MM-dd'T'HH:mm:ss") == calenderDTO.end.dateTime

    }
}
