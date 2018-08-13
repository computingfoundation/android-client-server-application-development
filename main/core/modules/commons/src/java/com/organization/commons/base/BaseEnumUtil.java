package com.organization.commons.base;

/**
 * Util functions for Enum classes
 */
public class BaseEnumUtil {

    public static boolean contains(BaseEnum anEnum, BaseEnum[] validEnums) {
        for (BaseEnum e : validEnums) {
            if (e == anEnum) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if any enums in <code>enumsToCheck</code> are contained in <code>validEnums</code>
     * @param enumsToCheck Enums to check
     * @param validEnums Enums to validate/check against
     * @return First containing enum
     */
    public static BaseEnum containAny(BaseEnum[] enumsToCheck, BaseEnum[] validEnums) {
        for (BaseEnum e : enumsToCheck) {
            if (contains(e, validEnums)) {
                return e;
            }
        }
        return null;
    }

}
