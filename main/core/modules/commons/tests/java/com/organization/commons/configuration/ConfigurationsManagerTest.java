package com.organization.commons.configuration;

import com.organization.commons.TestUtils;
import com.organization.commons.base.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ConfigurationsManagerTest {
    private static final Logger Log = LoggerFactory.getLogger(ConfigurationsManagerTest.class);
    private static String mConfigurationsJsonArrayString;

    /**
     * Make GET request on the configurations before testing ConfigurationManager class
     */
    @BeforeClass
    public static void setUp() {
        try {
            mConfigurationsJsonArrayString = TestUtils.initializeConfigurationsIfNotInitialized();
        } catch (IllegalStateException e) {
            Log.warn("Skipping test for {}: {}", ConfigurationsManagerTest.class.getSimpleName(), e.toString());
            Assume.assumeTrue(false);
        }
    }

    @Test
    public void shouldSetUpAllConfigurations() {
        int result = ConfigurationsManager.initializeConfigurations(mConfigurationsJsonArrayString);
        Assert.assertEquals(0, result);
    }

    /**
     * Removes a random second level object from the first configuration entity causing a failure parsing it.
     */
    @Test
    public void shouldSetUpAllButOneConfigurationEntities() {
        if (mConfigurationsJsonArrayString != null) {
            List<String> configJsonStringsList = JSONUtils.parseJsonArrayRootObjectsToList(new JSONArray(
                    mConfigurationsJsonArrayString));
            String testConfigEntityJsonStr = configJsonStringsList.get(0);
            JSONObject rootTestObj = new JSONObject(testConfigEntityJsonStr);
            JSONObject secondLvlTestObj = rootTestObj.getJSONObject(rootTestObj.keys().next());
            secondLvlTestObj.remove(secondLvlTestObj.keys().next());
            configJsonStringsList.set(0, rootTestObj.toString());
            int result = ConfigurationsManager.initializeConfigurations(configJsonStringsList.toString());
            Assert.assertEquals(1, result);
        }
    }

    /**
     * Simply forms an substring of a random length from the Json of the first configuration entity.
     */
    @Test
    public void invalidJsonShouldReturnNegativeOne() {
        if (mConfigurationsJsonArrayString != null) {
            List<String> configJsonStringsList = JSONUtils.parseJsonArrayRootObjectsToList(new JSONArray(
                    mConfigurationsJsonArrayString));
            String testConfigEntityJsonStr = configJsonStringsList.get(0);
            String invalidJsonString = testConfigEntityJsonStr.substring(10);
            configJsonStringsList.set(0, invalidJsonString);
            int result = ConfigurationsManager.initializeConfigurations(configJsonStringsList.toString());
            Assert.assertEquals(-1, result);
        }
    }

    @Test
    public void nullListShouldReturnNegativeTwo() {
        if (mConfigurationsJsonArrayString != null) {
            int result = ConfigurationsManager.initializeConfigurations(new JSONArray());
            Assert.assertEquals(-2, result);
        }
    }

}
