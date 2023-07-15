package tbservice

import grails.testing.gorm.DataTest
import org.apache.commons.lang.RandomStringUtils
import spock.lang.Specification
import tbservice.dto.org.RawUser
import tbservice.enums.FieldError
import tbservice.enums.UserField

class RawUserSpec extends Specification implements DataTest {

    def setup() {
    }

    def cleanup() {
    }

    def setupSpec() {
        mockDomain User
        mockDomain UserRelationship
    }

    def managerInfo = new RawUser(externalEmployeeCode: 'man', email: 'man@talentplus.com', firstName: 'man', lastName: 'man', jobTitle: 'manager', externalPersonId: 'man', mgrExternalEmployeeCode: '')

    void "constructor should work for raw map input"() {
        setup:
        def newUserInfo = [raw: [externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test', lastName: 'test', jobTitle: 'tester', externalPersonId: 'ttest', mgrExternalEmployeeCode: 'mancode', delete: '']]

        when:
        def rawUser = new RawUser(newUserInfo.raw)

        then:
        rawUser.externalEmployeeCode == 'asdf'
        rawUser.email == 'ttest@talentplus.com'
        rawUser.firstName == 'test'
        rawUser.lastName == 'test'
        rawUser.jobTitle == 'tester'
        rawUser.externalPersonId == 'ttest'
        rawUser.mgrExternalEmployeeCode == 'mancode'
        !rawUser.delete
    }

    void "full name getter should be calculated correctly when first and last name are present"() {
        setup:
        def newUserInfo = [raw: [externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'Kevin', lastName: 'Vandy', jobTitle: 'tester', externalPersonId: 'ttest', mgrExternalEmployeeCode: 'mancode', delete: '']]

        when:
        def rawUser = new RawUser(newUserInfo.raw)

        then:
        rawUser.fullName == "Kevin Vandy"
    }

    void "delete flag should be parsed correctly from raw object"() {
        setup:
        def newUserInfoLower = [raw: [delete: 'y']]
        def newUserInfoUpper = [raw: [delete: 'Y']]
        def newUserInfoX = [raw: [delete: 'x']]
        def newUserInfoUndefined = [raw: [:]]
        def newUserInfoNull = [raw: [delete: null]]
        def newUserInfoBlank = [raw: [delete: '']]

        when:
        def userRowLower = new RawUser(newUserInfoLower.raw)
        def userRowUpper = new RawUser(newUserInfoUpper.raw)
        def userRowX = new RawUser(newUserInfoX.raw)
        def userRowUndefined = new RawUser(newUserInfoUndefined.raw)
        def userRowNull = new RawUser(newUserInfoNull.raw)
        def userRowBlank = new RawUser(newUserInfoBlank.raw)

        then:
        userRowLower.delete
        userRowUpper.delete
        !userRowX.delete //only y or Y should be excepted
        !userRowUndefined.delete
        !userRowNull.delete
        !userRowBlank.delete
    }

    //field validation unit tests
    void "validateFields should return empty map of error and warning arrays if valid data"() {
        setup:
        def newUserInfo = [raw: [externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test', lastName: 'test', jobTitle: 'tester', externalPersonId: 'ttest', mgrExternalEmployeeCode: 'mancode']]
        def rawUser = new RawUser(newUserInfo.raw)


        when:
        rawUser.validate()
        def validation = rawUser.validateFields([] as Set<String>, managerInfo)

        then:
        !rawUser.errors.hasErrors()
        validation == [duplicates: [], errors: [], warnings: [[field: UserField.ROLE_GROUPS.value, type: FieldError.MISSING.value]]]
    }

    void "validateFields should return externalEmployeeCode error if missing"() {
        setup:
        def newUserInfo = [raw: [externalEmployeeCode: '', email: 'ttest@talentplus.com', firstName: 'test', lastName: 'test', jobTitle: 'tester', externalPersonId: 'ttest', mgrExternalEmployeeCode: 'mancode']]
        def rawUser = new RawUser(newUserInfo.raw)

        when:
        rawUser.validate()
        def validation = rawUser.validateFields([] as Set<String>, managerInfo)

        then:
        rawUser.errors.hasFieldErrors(UserField.EXTERNAL_EMPLOYEE_CODE.value)
        rawUser.errors.errorCount == 1
        validation == [duplicates: [], errors: [[field: UserField.EXTERNAL_EMPLOYEE_CODE.value, type: FieldError.MISSING.value]], warnings: [[field: UserField.ROLE_GROUPS.value, type: FieldError.MISSING.value]]]
    }

    void "validateFields should return externalEmployeeCode error if too long"() {
        setup:
        def newUserInfo = [raw: [externalEmployeeCode: RandomStringUtils.random(256, ('a'..'z').join()),
                                 email               : 'ttest@talentplus.com', firstName: 'test', lastName: 'test', jobTitle: 'tester', externalPersonId: 'ttest', mgrExternalEmployeeCode: 'mancode']]
        def rawUser = new RawUser(newUserInfo.raw)

        when:
        rawUser.validate()
        def validation = rawUser.validateFields([] as Set<String>, managerInfo)

        then:
        rawUser.errors.hasFieldErrors(UserField.EXTERNAL_EMPLOYEE_CODE.value)
        rawUser.errors.errorCount == 1
        validation == [duplicates: [], errors: [[field: UserField.EXTERNAL_EMPLOYEE_CODE.value, type: FieldError.MAX_LENGTH.value]], warnings: [[field: UserField.ROLE_GROUPS.value, type: FieldError.MISSING.value]]]
    }

    void "validateFields should return first name error if missing"() {
        setup:
        def newUserInfo = [raw: [externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: '', lastName: 'test', jobTitle: 'tester', externalPersonId: 'ttest', mgrExternalEmployeeCode: 'mancode']]
        def rawUser = new RawUser(newUserInfo.raw)

        when:
        rawUser.validate()
        def validation = rawUser.validateFields([] as Set<String>, managerInfo)

        then:
        rawUser.errors.hasFieldErrors(UserField.FIRST_NAME.value)
        rawUser.errors.errorCount == 1
        validation == [duplicates: [], errors: [[field: UserField.FIRST_NAME.value, type: FieldError.MISSING.value]], warnings: [[field: UserField.ROLE_GROUPS.value, type: FieldError.MISSING.value]]]
    }

    void "validateFields should return first name error if too long"() {
        setup:
        def newUserInfo = [raw: [externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com',
                                 firstName           : RandomStringUtils.random(256, ('a'..'z').join()),
                                 lastName            : 'test', jobTitle: 'tester', externalPersonId: 'ttest', mgrExternalEmployeeCode: 'mancode']]
        def rawUser = new RawUser(newUserInfo.raw)

        when:
        rawUser.validate()
        def validation = rawUser.validateFields([] as Set<String>, managerInfo)

        then:
        rawUser.errors.hasFieldErrors(UserField.FIRST_NAME.value)
        rawUser.errors.errorCount == 1
        validation == [duplicates: [], errors: [[field: UserField.FIRST_NAME.value, type: FieldError.MAX_LENGTH.value]], warnings: [[field: UserField.ROLE_GROUPS.value, type: FieldError.MISSING.value]]]
    }

    void "validateFields should return last name error if missing"() {
        setup:
        def newUserInfo = [raw: [externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test', lastName: '', jobTitle: 'tester', externalPersonId: 'ttest', mgrExternalEmployeeCode: 'mancode']]
        def rawUser = new RawUser(newUserInfo.raw)

        when:
        rawUser.validate()
        def validation = rawUser.validateFields([] as Set<String>, managerInfo)

        then:
        rawUser.errors.hasFieldErrors(UserField.LAST_NAME.value)
        rawUser.errors.errorCount == 1
        validation == [duplicates: [], errors: [[field: UserField.LAST_NAME.value, type: FieldError.MISSING.value]], warnings: [[field: UserField.ROLE_GROUPS.value, type: FieldError.MISSING.value]]]
    }

    void "validateFields should return last name error if too long"() {
        setup:
        def newUserInfo = [raw: [externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test',
                                 lastName            : RandomStringUtils.random(256, ('a'..'z').join()),
                                 jobTitle            : 'tester', externalPersonId: 'ttest', mgrExternalEmployeeCode: 'mancode']]
        def rawUser = new RawUser(newUserInfo.raw)

        when:
        rawUser.validate()
        def validation = rawUser.validateFields([] as Set<String>, managerInfo)

        then:
        rawUser.errors.hasFieldErrors(UserField.LAST_NAME.value)
        rawUser.errors.errorCount == 1
        validation == [duplicates: [], errors: [[field: UserField.LAST_NAME.value, type: FieldError.MAX_LENGTH.value]], warnings: [[field: UserField.ROLE_GROUPS.value, type: FieldError.MISSING.value]]]
    }

    void "validateFields should return email error if missing"() {
        setup:
        def newUserInfo = [raw: [externalEmployeeCode: 'asdf', email: '', firstName: 'test', lastName: 'test', jobTitle: 'tester', externalPersonId: 'ttest', mgrExternalEmployeeCode: 'mancode']]
        def rawUser = new RawUser(newUserInfo.raw)

        when:
        rawUser.validate()
        def validation = rawUser.validateFields([] as Set<String>, managerInfo)

        then:
        rawUser.errors.hasFieldErrors(UserField.EMAIL.value)
        rawUser.errors.errorCount == 1
        validation == [duplicates: [], errors: [[field: UserField.EMAIL.value, type: FieldError.MISSING.value]], warnings: [[field: UserField.ROLE_GROUPS.value, type: FieldError.MISSING.value]]]
    }

    void "validateFields should return email error if too long"() {
        setup:
        def newUserInfo = [raw: [externalEmployeeCode: 'asdf', email: RandomStringUtils.random(256, ('a'..'z').join()),
                                 firstName           : 'test', lastName: 'test',
                                 jobTitle            : 'tester', externalPersonId: 'ttest', mgrExternalEmployeeCode: 'mancode']]
        def rawUser = new RawUser(newUserInfo.raw)

        when:
        rawUser.validate()
        def validation = rawUser.validateFields([] as Set<String>, managerInfo)

        then:
        rawUser.errors.hasFieldErrors(UserField.EMAIL.value)
        rawUser.errors.errorCount == 2 //length and format
        validation == [duplicates: [], errors: [[field: UserField.EMAIL.value, type: FieldError.MAX_LENGTH.value]], warnings: [[field: UserField.ROLE_GROUPS.value, type: FieldError.MISSING.value]]]
    }

    void "validateFields should return email error if invalid email format"() {
        setup:
        def newUserInfo = [raw: [externalEmployeeCode: 'asdf', email: '@gmail.com', firstName: 'test', lastName: 'test', jobTitle: 'tester', externalPersonId: 'ttest', mgrExternalEmployeeCode: 'mancode']]
        def rawUser = new RawUser(newUserInfo.raw)

        when:
        rawUser.validate()
        def validation = rawUser.validateFields([] as Set<String>, managerInfo)

        then:
        rawUser.errors.hasFieldErrors(UserField.EMAIL.value)
        rawUser.errors.errorCount == 1
        validation == [duplicates: [], errors: [[field: UserField.EMAIL.value, type: FieldError.INVALID_FORMAT.value]], warnings: [[field: UserField.ROLE_GROUPS.value, type: FieldError.MISSING.value]]]
    }

    void "validateFields should not return email error if valid email domain"() {
        setup:
        def newUserInfo = [raw: [externalEmployeeCode: 'asdf', email: 'tester@talentplus.com', firstName: 'test', lastName: 'test', jobTitle: 'tester', externalPersonId: 'ttest', mgrExternalEmployeeCode: 'mancode']]
        def rawUser = new RawUser(newUserInfo.raw)

        when:
        rawUser.validate()
        def validation = rawUser.validateFields(['talentplus.com'] as Set<String>, managerInfo)

        then:
        !rawUser.errors.hasFieldErrors(UserField.EMAIL.value)
        rawUser.errors.errorCount == 0
        validation == [duplicates: [], errors: [], warnings: [[field: UserField.ROLE_GROUPS.value, type: FieldError.MISSING.value]]]
    }

    void "validateFields should return email error if invalid email domain"() {
        setup:
        def newUserInfo = [raw: [externalEmployeeCode: 'asdf', email: 'tester@gmail.com', firstName: 'test', lastName: 'test', jobTitle: 'tester', externalPersonId: 'ttest', mgrExternalEmployeeCode: 'mancode']]
        def rawUser = new RawUser(newUserInfo.raw)

        when:
        rawUser.validate()
        def validation = rawUser.validateFields(['talentplus.com'] as Set<String>, managerInfo)

        then:
        !rawUser.errors.hasFieldErrors(UserField.EMAIL.value)
        rawUser.errors.errorCount == 0
        validation == [duplicates: [], errors: [[field: UserField.EMAIL.value, type: FieldError.INVALID_VALUE.value]], warnings: [[field: UserField.ROLE_GROUPS.value, type: FieldError.MISSING.value]]]
    }

    void "validateFields should not return job title error if missing since it is optional"() {
        setup:
        def newUserInfo = [raw: [externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test', lastName: 'test',
                                 jobTitle            : '', externalPersonId: 'ttest', mgrExternalEmployeeCode: 'mancode']]
        def rawUser = new RawUser(newUserInfo.raw)

        when:
        rawUser.validate()
        def validation = rawUser.validateFields([] as Set<String>, managerInfo)

        then:
        !rawUser.errors.hasFieldErrors(UserField.JOB_TITLE.value)
        rawUser.errors.errorCount == 0
        validation == [duplicates: [], errors: [], warnings: [[field: UserField.ROLE_GROUPS.value, type: FieldError.MISSING.value]]]
    }

    void "validateFields should return job title error if too long"() {
        setup:
        def newUserInfo = [raw: [externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test', lastName: 'test',
                                 jobTitle            : RandomStringUtils.random(256, ('a'..'z').join()),
                                 externalPersonId    : 'ttest', mgrExternalEmployeeCode: 'mancode']]
        def rawUser = new RawUser(newUserInfo.raw)

        when:
        rawUser.validate()
        def validation = rawUser.validateFields([] as Set<String>, managerInfo)

        then:
        rawUser.errors.hasFieldErrors(UserField.JOB_TITLE.value)
        rawUser.errors.errorCount == 1
        validation == [duplicates: [], errors: [[field: UserField.JOB_TITLE.value, type: FieldError.MAX_LENGTH.value]], warnings: [[field: UserField.ROLE_GROUPS.value, type: FieldError.MISSING.value]]]
    }

    void "validateFields should return external person id warning if missing"() {
        setup:
        def newUserInfo = [raw: [externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test', lastName: 'test', jobTitle: 'tester', externalPersonId: '', mgrExternalEmployeeCode: 'mancode']]
        def rawUser = new RawUser(newUserInfo.raw)

        when:
        rawUser.validate()
        def validation = rawUser.validateFields([] as Set<String>, managerInfo)

        then:
        validation == [duplicates: [], warnings: [[field: UserField.EXTERNAL_PERSON_ID.value, type: FieldError.MISSING.value], [field: UserField.ROLE_GROUPS.value, type: FieldError.MISSING.value]], errors: []]
    }

    void "validateFields should return external person id error if too long"() {
        setup:
        def newUserInfo = [raw: [externalEmployeeCode   : 'asdf', email: 'ttest@talentplus.com', firstName: 'test',
                                 lastName               : 'test', jobTitle: 'tester',
                                 externalPersonId       : RandomStringUtils.random(256, ('a'..'z').join()),
                                 mgrExternalEmployeeCode: 'mancode']]
        def rawUser = new RawUser(newUserInfo.raw)

        when:
        rawUser.validate()
        def validation = rawUser.validateFields([] as Set<String>, managerInfo)

        then:
        rawUser.errors.hasFieldErrors(UserField.EXTERNAL_PERSON_ID.value)
        rawUser.errors.errorCount == 1
        validation == [duplicates: [], errors: [[field: UserField.EXTERNAL_PERSON_ID.value, type: FieldError.MAX_LENGTH.value]], warnings: [[field: UserField.ROLE_GROUPS.value, type: FieldError.MISSING.value]]]
    }

    void "validateFields should return mgr external employee code warning if missing"() {
        setup:
        def newUserInfo = [raw: [externalEmployeeCode: 'asdf', email: 'ttest@talentplus.com', firstName: 'test', lastName: 'test', jobTitle: 'tester', externalPersonId: 'ttest', mgrExternalEmployeeCode: '']]
        def rawUser = new RawUser(newUserInfo.raw)

        when:
        rawUser.validate()
        def validation = rawUser.validateFields([] as Set<String>, managerInfo)

        then:
        !rawUser.errors.hasFieldErrors(UserField.MGR_EXTERNAL_EMPLOYEE_CODE.value) //no error, just warning
        rawUser.errors.errorCount == 0 //no error, just warning
        validation == [duplicates: [], errors: [], warnings: [[field: UserField.MGR_EXTERNAL_EMPLOYEE_CODE.value, type: FieldError.MISSING.value], [field: UserField.ROLE_GROUPS.value, type: FieldError.MISSING.value]]]
    }

    void "validateFields should return mgr external employee code error if too long"() {
        setup:
        def newUserInfo = [raw: [externalEmployeeCode   : 'asdf', email: 'ttest@talentplus.com', firstName: 'test',
                                 lastName               : 'test', jobTitle: 'tester',
                                 externalPersonId       : 'ttest',
                                 mgrExternalEmployeeCode: RandomStringUtils.random(256, ('a'..'z').join())]]
        def rawUser = new RawUser(newUserInfo.raw)

        when:
        rawUser.validate()
        def validation = rawUser.validateFields([] as Set<String>, managerInfo)

        then:
        rawUser.errors.hasFieldErrors(UserField.MGR_EXTERNAL_EMPLOYEE_CODE.value)
        rawUser.errors.errorCount == 1
        validation == [duplicates: [], errors: [[field: UserField.MGR_EXTERNAL_EMPLOYEE_CODE.value, type: FieldError.MAX_LENGTH.value]], warnings: [[field: UserField.ROLE_GROUPS.value, type: FieldError.MISSING.value]]]
    }

    void "validateFields should return mgr external employee code error if code does not have corresponding manager info"() {
        setup:
        def newUserInfo = [raw: [externalEmployeeCode   : 'asdf', email: 'ttest@talentplus.com', firstName: 'test',
                                 lastName               : 'test', jobTitle: 'tester',
                                 externalPersonId       : 'ttest',
                                 mgrExternalEmployeeCode: 'mancode']]
        def rawUser = new RawUser(newUserInfo.raw)

        when:
        rawUser.validate()
        def validation = rawUser.validateFields([] as Set<String>, null)

        then: //value should still be valid to grails
        !rawUser.errors.hasFieldErrors(UserField.MGR_EXTERNAL_EMPLOYEE_CODE.value)
        rawUser.errors.errorCount == 0
        validation == [duplicates: [], errors: [[field: UserField.MGR_EXTERNAL_EMPLOYEE_CODE.value, type: FieldError.NOT_FOUND.value]], warnings: [[field: UserField.ROLE_GROUPS.value, type: FieldError.MISSING.value]]]
    }

}
