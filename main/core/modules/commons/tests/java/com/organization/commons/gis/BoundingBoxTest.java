package com.organization.commons.gis;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class BoundingBoxTest {

    @BeforeClass
    public static void setUpCoordinates() { }

    @Test
    public void shouldCreateBoundingBoxFromPoint() {
        BoundingBox bb = BoundingBox.fromPoint(47.56910237, -108.92742448, 2000, 2000);
        Assert.assertEquals(47.587099, bb.northLat(), 1e-4);
        Assert.assertEquals(-108.900724, bb.eastLng(), 1e-4);
        Assert.assertEquals(47.551106, bb.southLat(), 1e-4);
        Assert.assertEquals(-108.954124, bb.westLng(), 1e-4);

        BoundingBox bbNorthPoleWrap = BoundingBox.fromPoint(89.99999999, -108, 2000, 2000);
        Assert.assertEquals(90, bbNorthPoleWrap.northLat(), 1e-4);
        Assert.assertEquals(180, bbNorthPoleWrap.eastLng(), 1e-4);
        Assert.assertEquals(89.982003, bbNorthPoleWrap.southLat(), 1e-4);
        Assert.assertEquals(-180, bbNorthPoleWrap.westLng(), 1e-4);

        BoundingBox bbSouthPoleWrap = BoundingBox.fromPoint(-89.99999999, -108, 2000, 2000);
        Assert.assertEquals(-89.982003, bbSouthPoleWrap.northLat(), 1e-4);
        Assert.assertEquals(180, bbSouthPoleWrap.eastLng(), 1e-4);
        Assert.assertEquals(-90, bbSouthPoleWrap.southLat(), 1e-4);
        Assert.assertEquals(-180, bbSouthPoleWrap.westLng(), 1e-4);

        BoundingBox bbCrossingAntiMeridian = BoundingBox.fromPoint(47.56910237, 10.27999999, 2000, 2000);
        Assert.assertEquals(47.587099, bbCrossingAntiMeridian.northLat(), 1e-4);
        Assert.assertEquals(-169.72000011, bbCrossingAntiMeridian.eastLng(), 1e-4);
        Assert.assertEquals(-90, bbCrossingAntiMeridian.southLat(), 1e-4);
        Assert.assertEquals(-169.71999991, bbCrossingAntiMeridian.westLng(), 1e-4);

    }

}
