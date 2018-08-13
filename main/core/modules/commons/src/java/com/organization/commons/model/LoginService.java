package com.organization.commons.model;

/**
 * Login services
 */
public enum LoginService {
    ORGANIZATION,
    TWITTER,
    FACEBOOK,
    GOOGLE;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
