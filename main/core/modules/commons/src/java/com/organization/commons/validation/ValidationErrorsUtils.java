package com.organization.commons.validation;

import com.organization.commons.base.BaseEnumUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Util functions for all Error.Group enum classes of validator classes.
 */
public class ValidationErrorsUtils {

    /*
    Developer note: Do not use Generic types for the methods in this class as casting enums to the generic will result
    in a ClassCastException.
     */

    /**
     * Checks if any error enum groups in <code>errors</code> are contained in <code>groupsToMatch</code>
     * @param errors Error enums to check
     * @param groupsToMatch Error enum groups to validate/check error enum groups against
     * @return First containing error group enum
     */
    public static ValidationError.ValidationErrorType.ValidationErrorGroup getFirstMatchingGroup(
                ValidationError.ValidationErrorType[] errors,
                ValidationError.ValidationErrorType.ValidationErrorGroup[] groupsToMatch) {
        for (ValidationError.ValidationErrorType e : errors) {
            ValidationError.ValidationErrorType.ValidationErrorGroup g = e.getGroup();
            if (BaseEnumUtil.contains(g, groupsToMatch)) {
                return g;
            }
        }
        return null;
    }

    /**
     * Extracts the groups from <code>errors</code>
     */
    public static ValidationError.ValidationErrorType.ValidationErrorGroup[] extractErrorsGroups(
                ValidationError.ValidationErrorType[] errors) {
        ArrayList<ValidationError.ValidationErrorType.ValidationErrorGroup> errorEnumsGroups = new ArrayList<>(
                10);

        for (ValidationError.ValidationErrorType e : errors) {
            ValidationError.ValidationErrorType.ValidationErrorGroup g = e.getGroup();
            if (!errorEnumsGroups.contains(g)) {
                errorEnumsGroups.add(g);
            }
        }
        return errorEnumsGroups.toArray(new ValidationError.ValidationErrorType.ValidationErrorGroup[0]);
    }

    /**
     * Extracts all errors within <code>errors</code> matching <code>group</code>
     */
    public static ValidationError.ValidationErrorType[] extractErrorsForGroup(
                ValidationError.ValidationErrorType[] errors,
                ValidationError.ValidationErrorType.ValidationErrorGroup group) {
        ArrayList<ValidationError.ValidationErrorType> returnErrs = new ArrayList<>(10);

        for (ValidationError.ValidationErrorType e : errors) {
            if (e.getGroup() == group) {
                returnErrs.add(e);
            }
        }
        return returnErrs.toArray(new ValidationError.ValidationErrorType[0]);
    }

    /**
     * Extracts all errors from <code>errors</code> of the first matching group in <code>groupsToMatch</code>
     */
    public static ValidationError.ValidationErrorType[] extractErrorsForFirstMatchingGroup(
                ValidationError.ValidationErrorType[] errors,
                ValidationError.ValidationErrorType.ValidationErrorGroup[] groupsToMatch) {
        ValidationError.ValidationErrorType.ValidationErrorGroup g = getFirstMatchingGroup(errors, groupsToMatch);
        return extractErrorsForGroup(errors, g);
    }

    /**
     * Get the strings of all error objects in the list.
     */
    public static String[] getErrorStrings(List<? extends ValidationError> errors) {
        String[] strs = new String[errors.size()];

        for (int i = 0; i < errors.size(); i++) {
            strs[i] = errors.get(i).toString();
        }
        return strs;
    }

    /**
     * Extract all error types from a list of Error objects.
     */
    public static ArrayList<ValidationError.ValidationErrorType> extractErrorTypes(List<? extends ValidationError> errors) {
        ArrayList<ValidationError.ValidationErrorType> types = new ArrayList<>(errors.size());

        for (ValidationError error : errors) {
            ValidationError.ValidationErrorType errType = error.getType();
            if (!types.contains(errType)) {
                types.add(errType);
            }
        }
        return types;
    }

}
