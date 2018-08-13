package com.organization.commons.gis;

import com.organization.commons.model.Coordinate;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class GISUtilTest {
    private static Coordinate[] mGeoPointCoordinates;
    private static com.vividsolutions.jts.geom.Coordinate[] mJtsCoordinates;

    @BeforeClass
    public static void setUp() {
        mGeoPointCoordinates = new Coordinate[] {
                new Coordinate(47.872144, 4.042969),
                new Coordinate(48.458352, 30.585938)};

        mJtsCoordinates = new com.vividsolutions.jts.geom.Coordinate[mGeoPointCoordinates.length];
        for (int i = 0; i < mJtsCoordinates.length; i++) {
            Coordinate geoPoint = mGeoPointCoordinates[i];
            mJtsCoordinates[i] = new com.vividsolutions.jts.geom.Coordinate(geoPoint.getLng(), geoPoint.getLat());
        }
    }

    @Test
    public void shouldProcessCorrectDistance() {
        double degDist = GISUtil.calcDistance(-10, 1, -10, -1);
        Assert.assertEquals(339.9012612136378, degDist, 1e-4);
    }

    @Test
    public void shouldProcessCorrectMidpoint() {
        Coordinate gpCoordinate = GISUtil.calcMidpoint(mGeoPointCoordinates[0], mGeoPointCoordinates[1]);
        Assert.assertEquals(48.934746, gpCoordinate.getLat(), 1e-4);
        Assert.assertEquals(17.237226, gpCoordinate.getLng(), 1e-4);

        com.vividsolutions.jts.geom.Coordinate jtsCoordinate = GISUtil.calcMidpoint(mJtsCoordinates[0], mJtsCoordinates[1]);
        Assert.assertEquals(48.934746, jtsCoordinate.y, 1e-4);
        Assert.assertEquals(17.237226, jtsCoordinate.x, 1e-4);
    }

}
