package com.organization.commons.model.util;

import com.organization.commons.TestUtils;
import com.organization.commons.model.Coordinate;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PointsUtilTest {
    private static final Logger log = LoggerFactory.getLogger(PointsUtilTest.class);
    private static String sGeoPointsString;
    private static Coordinate[] sGeoPointsArray;

    @BeforeClass
    public static void setUp() {
        try {
            TestUtils.initializeConfigurationsIfNotInitialized();
        } catch (IllegalStateException e) {
            log.warn("Skipping test for {}: {}", PointsUtilTest.class.getSimpleName(), e.toString());
            Assume.assumeTrue(false);
        }

        sGeoPointsString = "-96.109415 41.215375,-96.107698 41.215375,-96.107259 41.215230,-96.107194 41.213285," +
                "-96.107859 41.212970,-96.108911 41.213003,-96.109211 41.213229," +
                "-96.109533 41.213769,-96.109415 41.215375";

        sGeoPointsArray = new Coordinate[] {
                new Coordinate(-96.109415 ,41.215375), new Coordinate(-96.107698 ,41.215375),
                new Coordinate(-96.107259 ,41.21523), new Coordinate(-96.107194 ,41.213285),
                new Coordinate(-96.107859 ,41.21297), new Coordinate(-96.108911 ,41.213003),
                new Coordinate(-96.109211, 41.213229), new Coordinate(-96.109533, 41.213769),
                new Coordinate(-96.109415, 41.215375)};
    }

    @Test
    public void shouldParseCoordaintesString() {
        Coordinate[] parsedArr = Coordinate.parse(sGeoPointsString);
        for (int i = 0; i < parsedArr.length; i++) {
            Assert.assertEquals(sGeoPointsArray[i], parsedArr[i]);
        }
    }

    @Test
    public void shouldFormatCoordaintesArray() {
        String formattedStr = Coordinate.format(sGeoPointsArray);
        Assert.assertEquals(sGeoPointsString, formattedStr);
    }

    @Test
    public void shouldFormatCoordaintesString() {
        StringBuilder unformattedStrSb = new StringBuilder(sGeoPointsString);
        // delete last to decimals of the 2nd lat/lng pair
        unformattedStrSb.deleteCharAt(29);
        unformattedStrSb.deleteCharAt(29);
        unformattedStrSb.deleteCharAt(37);
        unformattedStrSb.deleteCharAt(37);
        StringBuilder formattedStrSb = new StringBuilder(sGeoPointsString);
        formattedStrSb.setCharAt(29, '0');
        formattedStrSb.setCharAt(30, '0');
        formattedStrSb.setCharAt(39, '0');
        formattedStrSb.setCharAt(40, '0');

        String formattedStr = Coordinate.format(unformattedStrSb.toString());
        Assert.assertEquals(formattedStrSb.toString(), formattedStr);
    }
}
