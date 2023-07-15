package tbservice

import grails.testing.gorm.DataTest
import spock.lang.Specification
import tbservice.dto.org.DuplicateStats
import tbservice.dto.org.RawUser
import tbservice.enums.ActionStatus
import tbservice.enums.FieldError
import tbservice.enums.UserField
import tbservice.dto.org.ProcessedUserRowDTO

class ProcessedUserRowDTOSpec extends Specification implements DataTest {

    def setup() {
    }

    def cleanup() {
    }

    def setupSpec() {
        mockDomain User
    }

    void "should compute mgrName as empty string if newManagerInfo does not have first and last name"() {
        setup:
        ProcessedUserRowDTO processedUserRowDTO = new ProcessedUserRowDTO()
        def newUserInfo = new RawUser(externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test', lastName: 'test', jobTitle: 'tester', externalPersonId: 'ttest')
        def managerInfo = new RawUser(externalEmployeeCode: 'man', email: 'man@talentplus.com', firstName: '', lastName: '', jobTitle: 'manager', externalPersonId: 'man', mgrExternalEmployeeCode: '')

        when:
        processedUserRowDTO.computeValues(newUserInfo, managerInfo)

        then:
        processedUserRowDTO.computed?.mgrName == ''
    }

    void "should compute mgrName correctly if newManagerInfo has first and last name"() {
        setup:
        ProcessedUserRowDTO processedUserRowDTO = new ProcessedUserRowDTO()
        def newUserInfo = new RawUser(externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test', lastName: 'test', jobTitle: 'tester', externalPersonId: 'ttest')
        def managerInfo = new RawUser(externalEmployeeCode: 'man', email: 'man@talentplus.com', firstName: 'John', lastName: 'Smith', jobTitle: 'manager', externalPersonId: 'man', mgrExternalEmployeeCode: '')

        when:
        processedUserRowDTO.computeValues(newUserInfo, managerInfo)

        then:
        processedUserRowDTO.computed?.mgrName == 'John Smith'
    }

    void "should compute mgrName correctly if newManagerInfo has just first or last name"() {
        setup:
        def newUserInfo = new RawUser(externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test', lastName: 'test', jobTitle: 'tester', externalPersonId: 'ttest')
        ProcessedUserRowDTO processedUserRowDTONoFirst = new ProcessedUserRowDTO()
        def managerInfoNoFirst = new RawUser(externalEmployeeCode: 'man', email: 'man@talentplus.com', firstName: '', lastName: 'Smith', jobTitle: 'manager', externalPersonId: 'man', mgrExternalEmployeeCode: '')
        ProcessedUserRowDTO processedUserRowDTONoLast = new ProcessedUserRowDTO()
        def managerInfoNoLast = new RawUser(externalEmployeeCode: 'man', email: 'man@talentplus.com', firstName: 'John', lastName: '', jobTitle: 'manager', externalPersonId: 'man', mgrExternalEmployeeCode: '')

        when:
        processedUserRowDTONoFirst.computeValues(newUserInfo, managerInfoNoFirst)
        processedUserRowDTONoLast.computeValues(newUserInfo, managerInfoNoLast)

        then:
        processedUserRowDTONoFirst.computed?.mgrName == 'Smith'
        processedUserRowDTONoLast.computed?.mgrName == 'John'
    }

    void "should determine delete action if delete flag is present on user row"() {
        setup:
        ProcessedUserRowDTO processedUserRowDTO = new ProcessedUserRowDTO()
        def oldUserInfo = new User(externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test', lastName: 'test', externalPersonId: 'ttest')
        def newUserInfo = new RawUser(externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test', lastName: 'test', jobTitle: 'tester', externalPersonId: 'ttest', delete: 'y')

        when:
        processedUserRowDTO.determineAction(newUserInfo, oldUserInfo)

        then:
        processedUserRowDTO.action == ActionStatus.DELETE.value
        processedUserRowDTO.validation?.warnings?.size() == 0
    }

    void "should determine delete action if delete flag is present on user row, but add warning if no existing user to delete"() {
        setup:
        ProcessedUserRowDTO processedUserRowDTO = new ProcessedUserRowDTO()
        def oldUserInfo = null
        def newUserInfo = new RawUser(externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test', lastName: 'test', jobTitle: 'tester', externalPersonId: 'ttest', delete: 'y')

        when:
        processedUserRowDTO.determineAction(newUserInfo, oldUserInfo)

        then:
        processedUserRowDTO.action == ActionStatus.DELETE.value
        processedUserRowDTO.validation?.warnings?.size() == 1
        processedUserRowDTO.validation?.warnings == [[field: UserField.DELETE.value, type: FieldError.NOT_FOUND.value]]
    }

    void "should determine update action if old and new user info exist and delete flag is off"() {
        setup:
        ProcessedUserRowDTO processedUserRowDTO = new ProcessedUserRowDTO()
        def oldUserInfo = new User(externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test', lastName: 'test', externalPersonId: 'ttest')
        def newUserInfo = new RawUser(externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test', lastName: 'test', jobTitle: 'tester', externalPersonId: 'ttest', delete: '')

        when:
        processedUserRowDTO.determineAction(newUserInfo, oldUserInfo)

        then:
        processedUserRowDTO.action == ActionStatus.UPDATE.value
        processedUserRowDTO.validation?.warnings?.size() == 0
    }

    void "should determine add action if only new user info exists, and delete flag is off"() {
        setup:
        ProcessedUserRowDTO processedUserRowDTO = new ProcessedUserRowDTO()
        def oldUserInfo = null
        def newUserInfo = new RawUser(externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test', lastName: 'test', jobTitle: 'tester', externalPersonId: 'ttest', delete: '')

        when:
        processedUserRowDTO.determineAction(newUserInfo, oldUserInfo)

        then:
        processedUserRowDTO.action == ActionStatus.ADD.value
        processedUserRowDTO.validation?.warnings?.size() == 0
    }

    void "findUserDifferences should not process any differences if action is not UPDATE"() {
        setup:
        ProcessedUserRowDTO processedUserRowDTO = new ProcessedUserRowDTO(action: ActionStatus.ADD.value)
        def oldUserInfo = new User(externalEmployeeCode: 'asdf1', email1: 'ttest1@talentplus.com', firstName: 'test', lastName: 'test', externalPersonId: 'ttest')
        oldUserInfo.jobTitle = 'tester' //transient
        def newUserInfo = new RawUser(externalEmployeeCode: 'asdf2', email2: 'ttest2@talentplus.com', firstName: 'test', lastName: 'test', jobTitle: 'tester', externalPersonId: 'ttest')

        when:
        processedUserRowDTO.findDifferences(newUserInfo, oldUserInfo, null, null)

        then:
        processedUserRowDTO.differences == []
    }

    void "findUserDifferences should return empty array if no differences and change action to unchanged"() {
        setup:
        ProcessedUserRowDTO processedUserRowDTO = new ProcessedUserRowDTO(action: ActionStatus.UPDATE.value)
        def oldUserInfo = new User(externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test', lastName: 'test', externalPersonId: 'ttest')
        oldUserInfo.jobTitle = 'tester' //transient
        def newUserInfo = new RawUser(externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test', lastName: 'test', jobTitle: 'tester', externalPersonId: 'ttest')

        when:
        processedUserRowDTO.findDifferences(newUserInfo, oldUserInfo, null, null)

        then:
        processedUserRowDTO.differences == []
        processedUserRowDTO.action == ActionStatus.UNCHANGED.value
    }

    void "findUserDifferences should return firstName difference if first name changed"() {
        setup:
        ProcessedUserRowDTO processedUserRowDTO = new ProcessedUserRowDTO(action: ActionStatus.UPDATE.value)
        def oldUserInfo = new User(externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test', lastName: 'test', externalPersonId: 'ttest')
        oldUserInfo.jobTitle = 'tester' //transient
        def newUserInfo = new RawUser(externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test2', lastName: 'test', jobTitle: 'tester', externalPersonId: 'ttest')

        when:
        processedUserRowDTO.findDifferences(newUserInfo, oldUserInfo, null, null)

        then:
        processedUserRowDTO.differences == [[field: UserField.FIRST_NAME.value, dbValue: oldUserInfo.firstName]]
        processedUserRowDTO.action == ActionStatus.UPDATE.value
    }

    void "findUserDifferences should return lastName difference if last name changed"() {
        setup:
        ProcessedUserRowDTO processedUserRowDTO = new ProcessedUserRowDTO(action: ActionStatus.UPDATE.value)
        def oldUserInfo = new User(externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test', lastName: 'test1', externalPersonId: 'ttest')
        oldUserInfo.jobTitle = 'tester' //transient
        def newUserInfo = new RawUser(externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test', lastName: 'test2', jobTitle: 'tester', externalPersonId: 'ttest')

        when:
        processedUserRowDTO.findDifferences(newUserInfo, oldUserInfo, null, null)

        then:
        processedUserRowDTO.differences == [[field: UserField.LAST_NAME.value, dbValue: oldUserInfo.lastName]]
        processedUserRowDTO.action == ActionStatus.UPDATE.value
    }

    void "findUserDifferences should return email difference if email changed"() {
        setup:
        ProcessedUserRowDTO processedUserRowDTO = new ProcessedUserRowDTO(action: ActionStatus.UPDATE.value)
        def oldUserInfo = new User(externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test', lastName: 'test', externalPersonId: 'ttest')
        oldUserInfo.jobTitle = 'tester' //transient
        def newUserInfo = new RawUser(externalEmployeeCode: 'asdf', email: 'ttest2@talentplus.com', firstName: 'test', lastName: 'test', jobTitle: 'tester', externalPersonId: 'ttest')

        when:
        processedUserRowDTO.findDifferences(newUserInfo, oldUserInfo, null, null)

        then:
        processedUserRowDTO.differences == [[field: UserField.EMAIL.value, dbValue: oldUserInfo.email]]
        processedUserRowDTO.action == ActionStatus.UPDATE.value
    }

    void "findUserDifferences should return external Person Id difference if external person id changed"() {
        setup:
        ProcessedUserRowDTO processedUserRowDTO = new ProcessedUserRowDTO(action: ActionStatus.UPDATE.value)
        def oldUserInfo = new User(externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test', lastName: 'test', externalPersonId: 'ttest')
        oldUserInfo.jobTitle = 'tester' //transient
        def newUserInfo = new RawUser(externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test', lastName: 'test', jobTitle: 'tester', externalPersonId: 'ttest2')

        when:
        processedUserRowDTO.findDifferences(newUserInfo, oldUserInfo, null, null)

        then:
        processedUserRowDTO.differences == [[field: UserField.EXTERNAL_PERSON_ID.value, dbValue: oldUserInfo.externalPersonId]]
        processedUserRowDTO.action == ActionStatus.UPDATE.value
    }

    void "findUserDifferences should return no manager differences if external mgr external employee did not change"() {
        setup:
        ProcessedUserRowDTO processedUserRowDTO = new ProcessedUserRowDTO(action: ActionStatus.UPDATE.value)
        def oldUserInfo = new User(externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test', lastName: 'test', externalPersonId: 'ttest')
        oldUserInfo.jobTitle = 'tester' //transient
        def newUserInfo = new RawUser(externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test', lastName: 'test', jobTitle: 'tester', externalPersonId: 'ttest', mgrExternalEmployeeCode: 'samecode')

        def oldManagerInfo = new User(externalEmployeeCode: 'samecode', email: 'manager@talentplus.com', firstName: 'man', lastName: 'ager', externalPersonId: 'manager')
        def newManagerInfo = new RawUser(externalEmployeeCode: 'samecode', email: 'manager@talentplus.com', firstName: 'man', lastName: 'ager', externalPersonId: 'manager')

        when:
        processedUserRowDTO.findDifferences(newUserInfo, oldUserInfo, newManagerInfo, oldManagerInfo)

        then:
        processedUserRowDTO.differences == []
        processedUserRowDTO.action == ActionStatus.UNCHANGED.value
    }

    void "findUserDifferences should return manager differences if external mgr external employee code changed"() {
        setup:
        ProcessedUserRowDTO processedUserRowDTO = new ProcessedUserRowDTO(action: ActionStatus.UPDATE.value)
        def oldUserInfo = new User(externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test', lastName: 'test', externalPersonId: 'ttest')
        oldUserInfo.jobTitle = 'tester' //transient
        def newUserInfo = new RawUser(externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test', lastName: 'test', jobTitle: 'tester', externalPersonId: 'ttest', mgrExternalEmployeeCode: 'newcode')

        def oldManagerInfo = new User(externalEmployeeCode: 'oldcode', email: 'manager@talentplus.com', firstName: 'man', lastName: 'ager', externalPersonId: 'manager')
        def newManagerInfo = new RawUser(externalEmployeeCode: 'newcode', email: 'manager@talentplus.com', firstName: 'newman', lastName: 'newager', externalPersonId: 'manager')

        when:
        processedUserRowDTO.findDifferences(newUserInfo, oldUserInfo, newManagerInfo, oldManagerInfo)

        then:
        processedUserRowDTO.differences == [
                [field: UserField.MGR_EXTERNAL_EMPLOYEE_CODE.value, dbValue: oldManagerInfo.externalEmployeeCode],
                [field: UserField.MGR_NAME.value, dbValue: oldManagerInfo.getFullName()]
        ]
        processedUserRowDTO.action == ActionStatus.UPDATE.value
    }

    void "findUserDifferences should return list of differences if multiple differences"() {
        setup:
        ProcessedUserRowDTO processedUserRowDTO = new ProcessedUserRowDTO(action: ActionStatus.UPDATE.value)
        def oldUserInfo = new User(externalEmployeeCode: 'asdf', email: 'ttest1@talentplus.com', firstName: 'test', lastName: 'test', externalPersonId: 'ttest1')
        oldUserInfo.jobTitle = 'tester' //transient
        def newUserInfo = new RawUser(externalEmployeeCode: 'asdf', email: 'ttest2@talentplus.com', firstName: 'test', lastName: 'test', jobTitle: 'tester', externalPersonId: 'ttest2')

        when:
        processedUserRowDTO.findDifferences(newUserInfo, oldUserInfo, null, null)

        then:
        processedUserRowDTO.differences == [
                [field: UserField.EMAIL.value, dbValue: oldUserInfo.email],
                [field: UserField.EXTERNAL_PERSON_ID.value, dbValue: oldUserInfo.externalPersonId]
        ]
        processedUserRowDTO.action == ActionStatus.UPDATE.value
    }

    void "assignDuplicates should find no duplicates if no duplicates are passed in"() {
        setup:
        ProcessedUserRowDTO processedUserRowDTO = new ProcessedUserRowDTO()
        processedUserRowDTO.raw = [externalEmployeeCode: 'asdf', email: 'asdf@gmail.com', externalPersonId: 'asdf1']
        Map<String, Map> duplicates = [
                externalEmployeeCodes: [:],
                emails               : [:],
                externalPersonIds    : [:]
        ]

        when:
        processedUserRowDTO.assignDuplicates(duplicates)

        then:
        processedUserRowDTO.validation.duplicates == []
    }

    void "assignDuplicates should find no duplicates if there are not any that match its raw"() {
        setup:
        ProcessedUserRowDTO processedUserRowDTO = new ProcessedUserRowDTO()
        processedUserRowDTO.raw = [externalEmployeeCode: 'asdf', email: 'asdf@gmail.com', externalPersonId: 'asdf1']
        Map<String, Map<String, DuplicateStats>> duplicates = [
                externalEmployeeCodes: [
                        'fdsa': new DuplicateStats('fdsa', [77, 6], [55, 5]) //should be ignored
                ],
                emails               : [
                        'fdsa@gmail.com': new DuplicateStats('fdsa', [77, 6], [55, 5]) //should be ignored
                ],
                externalPersonIds    : [
                        'fdsa1': new DuplicateStats('fdsa', [77, 6], [55, 5]) //should be ignored
                ]
        ]

        when:
        processedUserRowDTO.assignDuplicates(duplicates)

        then:
        processedUserRowDTO.validation.duplicates == []
    }

    void "assignDuplicates should assign duplicates that match its raw values and ignore those that don't"() {
        setup:
        ProcessedUserRowDTO processedUserRowDTO = new ProcessedUserRowDTO()
        processedUserRowDTO.raw = [externalEmployeeCode: 'asdf', email: 'asdf@gmail.com', externalPersonId: 'asdf1']
        Map<String, Map<String, DuplicateStats>> duplicates = [
                externalEmployeeCodes: [
                        'asdf': new DuplicateStats('asdf', [2, 3], [4, 5]), //should be added
                        'fdsa': new DuplicateStats('fdsa', [77, 6], [55, 5]) //should be ignored
                ],
                emails               : [
                        'asdf@gmail.com': new DuplicateStats('asdf', [2, 3], [4, 5]), //should be added
                        'fdsa@gmail.com': new DuplicateStats('fdsa', [77, 6], [55, 5]) //should be ignored
                ],
                externalPersonIds    : [
                        'asdf1': new DuplicateStats('asdf', [2, 3], [4, 5]), //should be added
                        'fdsa1': new DuplicateStats('fdsa', [77, 6], [55, 5]) //should be ignored
                ]
        ]

        when:
        processedUserRowDTO.assignDuplicates(duplicates)

        then:
        processedUserRowDTO.validation.duplicates == [
                [field: UserField.EXTERNAL_EMPLOYEE_CODE.value, dbIds: [2, 3], fileIds: [4, 5]],
                [field: UserField.EMAIL.value, dbIds: [2, 3], fileIds: [4, 5]],
                [field: UserField.EXTERNAL_PERSON_ID.value, dbIds: [2, 3], fileIds: [4, 5]]
        ]
    }

    void "assignRelationshipError should assign the error if it does not already exist"() {
        setup:
        ProcessedUserRowDTO processedUserRowDTO = new ProcessedUserRowDTO()

        when:
        processedUserRowDTO.assignRelationshipError()

        then:
        processedUserRowDTO.validation.errors?.size() == 1
    }

    void "assignRelationshipError should not reassign the error if it already exists from previous verify step"() {
        setup:
        ProcessedUserRowDTO processedUserRowDTO = new ProcessedUserRowDTO()
        processedUserRowDTO.validation.errors = [[field: UserField.MGR_EXTERNAL_EMPLOYEE_CODE.value, type: FieldError.INVALID_RELATIONSHIP.value]]

        when:
        processedUserRowDTO.assignRelationshipError()

        then:
        processedUserRowDTO.validation.errors?.size() == 1
    }

    void "unassignRelationshipError should remove the error if it already exists from previous verify step"() {
        setup:
        ProcessedUserRowDTO processedUserRowDTO = new ProcessedUserRowDTO()
        processedUserRowDTO.validation.errors = [[field: UserField.MGR_EXTERNAL_EMPLOYEE_CODE.value, type: FieldError.INVALID_RELATIONSHIP.value]]

        when:
        processedUserRowDTO.unassignRelationshipError()

        then:
        processedUserRowDTO.validation.errors?.size() == 0
    }

    void "assignRelationshipWarning should assign the error if it does not already exist"() {
        setup:
        ProcessedUserRowDTO processedUserRowDTO = new ProcessedUserRowDTO()

        when:
        processedUserRowDTO.assignOrphanWarning()

        then:
        processedUserRowDTO.validation.warnings?.size() == 1
    }

    void "assignRelationshipWarning should not reassign the warning if it already exists from previous verify step"() {
        setup:
        ProcessedUserRowDTO processedUserRowDTO = new ProcessedUserRowDTO()
        processedUserRowDTO.validation.warnings = [[field: UserField.DELETE.value, type: FieldError.INVALID_RELATIONSHIP.value]]

        when:
        processedUserRowDTO.assignOrphanWarning()

        then:
        processedUserRowDTO.validation.warnings?.size() == 1
    }

    void "unassignRelationshipWarning should remove the warning if it already exists from previous verify step"() {
        setup:
        ProcessedUserRowDTO processedUserRowDTO = new ProcessedUserRowDTO()
        processedUserRowDTO.validation.warnings = [[field: UserField.DELETE.value, type: FieldError.INVALID_RELATIONSHIP.value]]

        when:
        processedUserRowDTO.unassignOrphanWarning()

        then:
        processedUserRowDTO.validation.warnings?.size() == 0
    }

    void "updateCounts should default counts to 0 if no errors, warnings, or differences"() {
        setup:
        ProcessedUserRowDTO processedUserRowDTO = new ProcessedUserRowDTO()

        when:
        processedUserRowDTO.updateCounts()

        then:
        processedUserRowDTO.counts?.countOfErrors == 0
        processedUserRowDTO.counts?.countOfWarnings == 0
        processedUserRowDTO.counts?.countOfDifferences == 0
        processedUserRowDTO.counts?.countOfDuplicates == 0
    }

    void "updateCounts should count accurately"() {
        setup:
        ProcessedUserRowDTO processedUserRowDTO = new ProcessedUserRowDTO()
        processedUserRowDTO.validation.errors.addAll([:], [:], [:])
        processedUserRowDTO.validation.warnings.addAll([:], [:])
        processedUserRowDTO.validation.duplicates.addAll([:])
        processedUserRowDTO.differences.addAll([:], [:], [:], [:])

        when:
        processedUserRowDTO.updateCounts()

        then:
        processedUserRowDTO.counts?.countOfErrors == 3
        processedUserRowDTO.counts?.countOfWarnings == 2
        processedUserRowDTO.counts?.countOfDuplicates == 1
        processedUserRowDTO.counts?.countOfDifferences == 4

    }

}
