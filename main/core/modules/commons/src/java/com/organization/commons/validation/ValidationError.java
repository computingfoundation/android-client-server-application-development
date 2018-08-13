package com.organization.commons.validation;

import com.organization.commons.base.BaseEnum;

/**
 * Base error of all validation error classes.
 */
public interface ValidationError {

    ValidationErrorType getType();

    /**
     * Base TYPE enum implemented by the TYPE enum in all validation error classes.
     */
    interface ValidationErrorType extends BaseEnum {

        ValidationErrorGroup getGroup();
        String getBaseDescription();
        void setDescription(String description);

        /**
         * Base enum entity implemented by the Group enup in all validation classes.
         */
        interface ValidationErrorGroup extends BaseEnum {

        }
    }

}
