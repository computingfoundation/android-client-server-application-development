package com.organization.commons.validation;

import com.organization.commons.configuration.RegulationConfigurations;
import com.organization.commons.configuration.ValidationConfigurations;
import com.organization.commons.model.User;
import org.apache.commons.validator.routines.EmailValidator;

import java.util.ArrayList;

/**
 * User properties validator class
 */
public class UserValidator {

    public static class Error implements ValidationError {
        
        public enum TYPE implements ValidationErrorType {
            NAME_NULL_OR_EMPTY(GROUP.NAME, "empty."),
            NAME_TOO_SHORT(GROUP.NAME, "too short (minimum %s characters)."),
            NAME_TOO_LONG(GROUP.NAME, "too long (maximum %s characters)."),
            NAME_CONTAINS_UPPERCASE_CHARACTERS(GROUP.NAME, "contains uppercase characters."),
            NAME_CONTAINS_INVALID_CHARACTERS(GROUP.NAME, "contains invalid characters."),
            NAME_DOES_NOT_BEGIN_WITH_AN_ALPHA_CHARACTER(GROUP.NAME, "does not begin with an alpha character."),
            NAME_CONTAINS_UNALLOWED_WORDS(GROUP.NAME, "contains unallowed words."),
            PASSWORD_NULL_OR_EMPTY(GROUP.PASSWORD, "empty."),
            PASSWORD_TOO_SHORT(GROUP.PASSWORD, "too short (minimum %s characters)."),
            PASSWORD_TOO_LONG(GROUP.PASSWORD, "too long (maximum %s characters)."),
            PASSWORD_CONTAINS_INVALID_CHARACTERS(GROUP.PASSWORD, "contains invalid characters."),
            PHONE_NUMBER_NULL_OR_EMPTY(GROUP.PHONE_NUMBER, "empty."),
            PHONE_NUMBER_INVALID(GROUP.PHONE_NUMBER, "invalid."),
            EMAIL_NULL_OR_EMPTY(GROUP.EMAIL, "empty."),
            EMAIL_INVALID(GROUP.EMAIL, "invalid."),
            FIRST_NAME_NULL_OR_EMPTY(GROUP.FIRST_NAME, "empty."),
            FIRST_NAME_TOO_SHORT(GROUP.FIRST_NAME, "too short (minimum 2 characters)."),
            FIRST_NAME_TOO_LONG(GROUP.FIRST_NAME, "too long (maximum 40 characters)."),
            FIRST_NAME_CONTAINS_INVALID_CHARACTERS(GROUP.FIRST_NAME, "contains invalid characters."),
            LAST_NAME_NULL_OR_EMPTY(GROUP.LAST_NAME, "empty."),
            LAST_NAME_TOO_SHORT(GROUP.LAST_NAME, "too short (minimum 2 characters)."),
            LAST_NAME_TOO_LONG(GROUP.LAST_NAME, "too long (maximum 40 characters)."),
            LAST_NAME_CONTAINS_INVALID_CHARACTERS(GROUP.LAST_NAME, "contains invalid characters.");

            public enum GROUP implements ValidationErrorGroup {
                NAME, PASSWORD, PHONE_NUMBER, EMAIL, FIRST_NAME, LAST_NAME;

                @Override
                public String toString() {
                    String str = super.toString().toLowerCase().replaceAll("_", " ");
                    return str.substring(0, 1).toUpperCase() + str.substring(1);
                }
            }

            private GROUP mGroup;
            private String mBaseDescription;

            TYPE(GROUP group, String baseDescription) {
                mGroup = group;
                mBaseDescription = baseDescription;
            }

            public GROUP getGroup() { return mGroup; }
            public String getBaseDescription() { return mBaseDescription; }
            public void setDescription(String description) { mBaseDescription = description; }
        }

        private TYPE mType;
        private String mDescription;

        public Error(TYPE type) {
            mType = type;
            mDescription = mType.getBaseDescription();
        }

        public Error(TYPE type, Object... descriptionFormatArguments) {
            mType = type;
            mDescription = String.format(mType.getBaseDescription(), descriptionFormatArguments);
        }

        @Override
        public TYPE getType() {
            return mType;
        }

        @Override
        public String toString() {
            return (mType.getGroup() == TYPE.GROUP.NAME) ? "Username" : mType.getGroup().toString() + " " +
                    mDescription;
        }
    }

    /**
     * Validate all user properties.
     */
    public static ArrayList<Error> validate(User user, Error.TYPE.GROUP[] validationGroups) {
        ArrayList<Error> errs = new ArrayList<>(10);

        for (Error.TYPE.GROUP group : validationGroups) {
            switch (group) {
                case NAME:
                    errs.addAll(validateName(user.getName()));
                    break;
                case PASSWORD:
                    errs.addAll(validatePassword(user.getPassword()));
                    break;
                case PHONE_NUMBER:
                    errs.addAll(validatePhoneNumber(user.getPhoneNumber()));
                    break;
                case EMAIL:
                    errs.addAll(validateEmail(user.getEmail()));
                    break;
                case FIRST_NAME:
                    errs.addAll(validateFirstName(user.getFirstName()));
                    break;
                case LAST_NAME:
                    errs.addAll(validateLastName(user.getLastName()));
                    break;
            }
        }
        return errs;
    }

    /**
     * Validate a user name.
     */
    public static ArrayList<Error> validateName(String name) {
        ArrayList<Error> errs = new ArrayList<>(10);

        if (name == null || name.equals("")) {
            errs.add(new Error(Error.TYPE.NAME_NULL_OR_EMPTY));
        } else {
            if (name.length() < ValidationConfigurations.User.nameMinimumLength()) {
                errs.add(new Error(Error.TYPE.NAME_TOO_SHORT, String.valueOf(ValidationConfigurations.User
                        .nameMinimumLength())));
            } else if (name.length() > ValidationConfigurations.User.nameMaximumLength()) {
                errs.add(new Error(Error.TYPE.NAME_TOO_LONG, String.valueOf(ValidationConfigurations.User
                        .nameMaximumLength())));
            }

            if (name.matches(".*[A-Z].*")) {
                errs.add(new Error(Error.TYPE.NAME_CONTAINS_UPPERCASE_CHARACTERS));
            } else if (!name.matches(ValidationConfigurations.User.nameRegex())) {
                errs.add(new Error(Error.TYPE.NAME_CONTAINS_INVALID_CHARACTERS));
            }

            if (!name.matches("[a-zA-Z]+.*")) {
                errs.add(new Error(Error.TYPE.NAME_DOES_NOT_BEGIN_WITH_AN_ALPHA_CHARACTER));
            }

            if (org.apache.commons.lang3.StringUtils.indexOfAny(name.toLowerCase(), RegulationConfigurations.Global
                    .primaryUnallowedWords().split(",")) != -1) {
                errs.add(new Error(Error.TYPE.NAME_CONTAINS_UNALLOWED_WORDS));
            } else if (org.apache.commons.lang3.StringUtils.indexOfAny(name.toLowerCase(), RegulationConfigurations
                    .Global.secondaryUnallowedWords().split(",")) != -1) {
                errs.add(new Error(Error.TYPE.NAME_CONTAINS_UNALLOWED_WORDS));
            }
        }
        return errs;
    }

    /**
     * Validate a user password.
     */
    public static ArrayList<Error> validatePassword(String password) {
        ArrayList<Error> errs = new ArrayList<>(10);

        if (password == null || password.equals("")) {
            errs.add(new Error(Error.TYPE.PASSWORD_NULL_OR_EMPTY));
        } else {
            if (password.length() < ValidationConfigurations.User.passwordMinimumLength()) {
                errs.add(new Error(Error.TYPE.PASSWORD_TOO_SHORT, String.valueOf(
                        ValidationConfigurations.User.passwordMinimumLength())));
            } else if (password.length() > ValidationConfigurations.User.passwordMaximumLength()) {
                errs.add(new Error(Error.TYPE.PASSWORD_TOO_LONG, String.valueOf(ValidationConfigurations.User
                        .passwordMaximumLength())));
            }

            if (!password.matches(ValidationConfigurations.User.passwordRegex())) {
                errs.add(new Error(Error.TYPE.PASSWORD_CONTAINS_INVALID_CHARACTERS));
            }
        }
        return errs;
    }

    /**
     * Validate a user phone number.
     */
    public static ArrayList<Error> validatePhoneNumber(String countryCodeAndPhoneNumber) {
        ArrayList<Error> errs = new ArrayList<>(10);

        if (countryCodeAndPhoneNumber == null) {
            errs.add(new Error(Error.TYPE.PHONE_NUMBER_NULL_OR_EMPTY));
        } else if (!countryCodeAndPhoneNumber.matches("(9[976]\\d|8[987530]\\d|6[987]\\d|5[90]\\d|42\\d|" +
                "3[875]\\d|2[98654321]\\d|9[8543210]|8[6421]|6[6543210]|5[87654321]|4[987654310]|3[9643210]|2[70]|7|" +
                "1)\\d{4,14}$")) {
            errs.add(new Error(Error.TYPE.PHONE_NUMBER_INVALID));
        }
        return errs;
    }

    /**
     * Validate a user email address.
     */
    public static ArrayList<Error> validateEmail(String email) {
        ArrayList<Error> errs = new ArrayList<>(10);

        if (email == null) {
            errs.add(new Error(Error.TYPE.EMAIL_NULL_OR_EMPTY));
        } else if (!EmailValidator.getInstance().isValid(email)) {
            // Note: Apaches email validator uses a max valid length of 72 characters
            errs.add(new Error(Error.TYPE.EMAIL_INVALID));
        }
        return errs;
    }

    /**
     * Validate a user first name.
     */
    public static ArrayList<Error> validateFirstName(String firstName) {
        ArrayList<Error> errs = new ArrayList<>(10);

        if (firstName == null) {
            errs.add(new Error(Error.TYPE.FIRST_NAME_NULL_OR_EMPTY));
        } else {
            if (firstName.length() < 2) {
                errs.add(new Error(Error.TYPE.FIRST_NAME_TOO_SHORT));
            } else if (firstName.length() > 40) {
                errs.add(new Error(Error.TYPE.FIRST_NAME_TOO_LONG));
            }
            if (!firstName.matches("[a-zA-Z ]*")) {
                errs.add(new Error(Error.TYPE.FIRST_NAME_CONTAINS_INVALID_CHARACTERS));
            }
        }
        return errs;
    }

    /**
     * Validate a user last name.
     */
    public static ArrayList<Error> validateLastName(String lastName) {
        ArrayList<Error> errs = new ArrayList<>(10);

        if (lastName == null) {
            errs.add(new Error(Error.TYPE.LAST_NAME_NULL_OR_EMPTY));
        } else {
            if (lastName.length() < 2) {
                errs.add(new Error(Error.TYPE.LAST_NAME_TOO_SHORT));
            } else if (lastName.length() > 40) {
                errs.add(new Error(Error.TYPE.LAST_NAME_TOO_LONG));
            }
            if (!lastName.matches("[a-zA-Z ]*")) {
                errs.add(new Error(Error.TYPE.LAST_NAME_CONTAINS_INVALID_CHARACTERS));
            }
        }
        return errs;
    }
    
}
