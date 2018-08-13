package com.organization.commons.gis;

import com.organization.commons.model.Coordinate;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GISShapeInfoProcessorTest {
    private static final Logger LOG = LoggerFactory.getLogger(GISShapeInfoProcessorTest.class);
    private static Coordinate[] mGeoPointCoordinates;
    private static com.vividsolutions.jts.geom.Coordinate[] mJtsCoordinates;

    @BeforeClass
    public static void setUp() {
        mGeoPointCoordinates = new Coordinate[] {
                new Coordinate(-87.620580, 41.878205),  //NW
                new Coordinate(-87.619014, 41.878285),  //NE
                new Coordinate(-87.617576, 41.878269),  //NE-SE MIDDLE
                new Coordinate(-87.617512, 41.873348),  //SE
                new Coordinate(-87.620387, 41.873316),  //SW
                new Coordinate(-87.620430, 41.873332),  //SW-NW MIDDLE
                new Coordinate(-87.620580, 41.878205)}; //NW

        mJtsCoordinates = new com.vividsolutions.jts.geom.Coordinate[mGeoPointCoordinates.length];
        for (int i = 0; i < mJtsCoordinates.length; i++) {
            Coordinate geoPoint = mGeoPointCoordinates[i];
            mJtsCoordinates[i] = new com.vividsolutions.jts.geom.Coordinate(geoPoint.getLng(), geoPoint.getLat());
        }
    }

    @Test
    public void shouldProcessCorrectPropertiesForGeoPointType() {
        GISInfoProcessor.GISInfo GISShapeInfo = GISInfoProcessor.process(mGeoPointCoordinates);
        Assert.assertEquals(true, GISShapeInfo.formsValidPolygon());
        Assert.assertEquals(false, GISShapeInfo.formsMoreThanOnePolygon());
        Assert.assertEquals(7565.7, GISShapeInfo.getArea(), 1e-1);
        Assert.assertEquals(713.5, GISShapeInfo.getPerimeter(), 1e-1);
        Assert.assertEquals(320.3, GISShapeInfo.getLongestSegmentLength(), 1e-1);
        Assert.assertEquals(-87.6175, GISShapeInfo.getLongestSegmentFrom().getLat(), 1e-4);
        Assert.assertEquals(-87.6203, GISShapeInfo.getLongestSegmentTo().getLat(), 1e-4);
        Assert.assertEquals(-87.6190, GISShapeInfo.getCentroid().getLat(), 1e-4);
    }

    @Test
    public void shouldProcessCorrectPropertiesForJtsType() {
        GISInfoProcessor.GISInfo GISShapeInfo = GISInfoProcessor.process(mJtsCoordinates, -1);
        Assert.assertEquals(true, GISShapeInfo.formsValidPolygon());
        Assert.assertEquals(false, GISShapeInfo.formsMoreThanOnePolygon());
        Assert.assertEquals(7565.7, GISShapeInfo.getArea(), 1e-1);
        Assert.assertEquals(713.5, GISShapeInfo.getPerimeter(), 1e-1);
        Assert.assertEquals(320.3, GISShapeInfo.getLongestSegmentLength(), 1e-1);
        Assert.assertEquals(-87.6175, GISShapeInfo.getLongestSegmentFrom().getLat(), 1e-4);
        Assert.assertEquals(-87.6203, GISShapeInfo.getLongestSegmentTo().getLat(), 1e-4);
        Assert.assertEquals(-87.6190, GISShapeInfo.getCentroid().getLat(), 1e-4);
    }

}
