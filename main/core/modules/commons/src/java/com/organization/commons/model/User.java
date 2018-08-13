package com.organization.commons.model;

/**
 * User
 */
public class User {

    private long mId;
    private String mName;
    private String mPassword;
    private int mCountryCode;
    private String mPhoneNumber;
    private String mEmail;
    private String mFirstName;
    private String mLastName;

    public User() { }

    public User(String name) {
        mName = name;
    }

    public User(String name, String password) {
        mName = name;
        mPassword = password;
    }

    public User(String name, String password, int countryCode, String phoneNumber, String email) {
        mName = name;
        mPassword = password;
        mCountryCode = countryCode;
        mPhoneNumber = phoneNumber;
        mEmail = email;
    }

    public User(String name, String password, int countryCode, String phoneNumber, String email, String firstName,
                String lastName) {
        mName = name;
        mPassword = password;
        mCountryCode = countryCode;
        mPhoneNumber = phoneNumber;
        mEmail = email;
        mFirstName = firstName;
        mLastName = lastName;
    }

    public long getId() { return mId; }
    public void setId(long id) { mId = id; }
    public String getName() { return mName; }
    public void setName(String name) { mName = name; }
    public String getPassword() { return mPassword; }
    public void setPassword(String password) { mPassword = password; }
    public int getCountryCode() { return mCountryCode; }
    public void setCountryCode(int countryCode) { mCountryCode = countryCode; }
    public String getPhoneNumber() { return mPhoneNumber; }
    public void setPhoneNumber(String phoneNumber) { mPhoneNumber = phoneNumber; }
    public String getEmail() { return mEmail; }
    public void setEmail(String email) { mEmail = email; }
    public String getFirstName() { return mFirstName; }
    public void setFirstName(String firstName) { mFirstName = firstName; }
    public String getLastName() { return mLastName; }
    public void setLastName(String lastName) { mLastName = lastName; }

}
