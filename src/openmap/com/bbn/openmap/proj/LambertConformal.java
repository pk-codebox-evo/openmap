// **********************************************************************
//
// **********************************************************************
//
// $Source: /cvs/distapps/openmap/src/openmap/com/bbn/openmap/proj/LambertConformal.java,v $
// $RCSfile: LambertConformal.java,v $
// $Revision: 1.1 $
// $Date: 2004/09/17 18:26:37 $
// $Author: dietrick $
//
// **********************************************************************


package com.bbn.openmap.proj;


import java.awt.Point;
import java.util.ArrayList;

import com.bbn.openmap.LatLonPoint;
import com.bbn.openmap.MoreMath;
import com.bbn.openmap.proj.ProjMath;
import com.bbn.openmap.util.Debug;

/** 
 * Implements the LambertConformalConic projection. <br>
 * <br>
 * <b>NOTE:</b> This implementation only works for the
 * northern hemisphere. <br>
 * <br>
 * Needs to be modified for use in the southern hemesphere.
 *
 * @author David J. Ward
 */
public class LambertConformal extends Proj {
    
    /**
     * The LambertCC name.
     */
    public final static transient String LambertConformalName = "Lambert Conformal";
    
    /**
     * The LambertCC type of projection.
     */
    public final static transient int LambertConformalType = 4200;
    
    private static double lambert_sp_one;
    private static double lambert_sp_two;
    private static double centralMeridian;
    
    double angle_sp_one;
    double angle_sp_two;
    double distance_sp_one;
    double distance_sp_two;
    double lambert_lamn;
    double lambert_lamf;
    
    int locationCenterXPixel = 0;
    int locationCenterYPixel = 0;
    double locationCenterXLambert = 0.0;
    double locationCenterYLambert = 0.0;
    double locationPixelsPerLambert = 0.0;

    double locationOriginX = 0.0;
    double locationOriginY = 0.0;

    double referenceLatitude = 0.0; 
    double falseEasting = 0.0;
    double falseNorthing = 0.0;
    
    /**
     * Construct a Lambert projection.
     * <p>
     * @param center LatLonPoint center of projection
     * @param scale float scale of projection
     * @param width width of screen
     * @param height height of screen
     */
    private LambertConformal(LatLonPoint center, float scale, int width, int height) {
        super(center, scale, width, height, LambertConformalType);
    }
    
    /** 
     * Constructor for the lambert conformal projection.
     * @param center center location for projections
     * @param scale scale of projection
     * @param width width of projection
     * @param height height of projection
     * @param centralMeridian the Central Meridian in degrees.
     * @param sp_one Standard Parallel One in degrees.
     * @param sp_two Standard Parallel Two in degrees.
     */
    private LambertConformal(LatLonPoint center, float scale, 
                             int width, int height, 
                             float centralMeridian, float sp_one, float sp_two) {
        this(center, scale, width, height, centralMeridian, sp_one, sp_two, 0f, 0, 0);
    }

    /** 
     * Constructor for the lambert conformal projection.
     * @param center center location for projections
     * @param scale scale of projection
     * @param width width of projection
     * @param height height of projection
     * @param centralMeridian the Central Meridian in degrees.
     * @param sp_one Standard Parallel One in degrees.
     * @param sp_two Standard Parallel Two in degrees.
     * @param reference_latitude the latitude for the origin of the projection
     * @param falseEasting number of meters added as buffer to origin E/W.
     * @param falseNorthing number of meters added as buffer to origin for N/S.
     */
    public LambertConformal(LatLonPoint center, float scale, 
                            int width, int height, 
                            double centralMeridian, double sp_one, double sp_two,
                            double reference_latitude, double falseEasting, double falseNorthing) {

        super(center, scale, width, height, LambertConformalType);
        
        this.centralMeridian = centralMeridian;
        this.lambert_sp_one = sp_one;
        this.lambert_sp_two = sp_two;
        this.referenceLatitude = reference_latitude;
        this.falseEasting = falseEasting;
        this.falseNorthing = falseNorthing;

        computeParameters();
    }
    
    /**
     * Called when some fundamental parameters change.
     * <p>
     * Each projection will decide how to respond to this change.
     * For instance, they may need to recalculate "constant" parameters
     * used in the forward() and inverse() calls.<p>
     *
     */
    public void computeParameters() {
        
        angle_sp_one = ProjMath.degToRad(90.0f - lambert_sp_one);
        angle_sp_two = ProjMath.degToRad(90.0f - lambert_sp_two);
        
        distance_sp_one = Math.log(Math.sin(angle_sp_one)) - Math.log(Math.sin(angle_sp_two));
        distance_sp_two = Math.log(Math.tan(angle_sp_one/2.0f)) - Math.log(Math.tan(angle_sp_two/2.0f));
        
        lambert_lamn = distance_sp_one / distance_sp_two;
        lambert_lamf = Math.sin(angle_sp_one) /
            (lambert_lamn * Math.pow(Math.tan(angle_sp_one/2.0), lambert_lamn));
        
        locationCenterXPixel = (int) ((float) getWidth() / 2.0 + .5);
        locationCenterYPixel = (int) ((float) getHeight() / 2.0 + .5);
        
        //Multiply by the cosmological constant of 100 to adjust pixels per lambert
        //to produce a ratio that is close to that of other projections
        locationPixelsPerLambert = getMaxScale()/getScale()*100;
                
        LambertPoint lp = new LambertPoint();
        LatLonPoint origin = new LatLonPoint(referenceLatitude, centralMeridian);
        LLToWorld(origin.getLatitude(), origin.getLongitude(), lp);
        locationOriginX = lp.x;
        locationOriginY = lp.y;
        
        LatLonPoint center = getCenter();
        LLToWorld(center.getLatitude(), center.getLongitude(), lp);
        locationCenterXLambert = lp.x;
        locationCenterYLambert = lp.y;

        if (Debug.debugging("lcc")) {
            Debug.output("Creating LambertConformal: center x = " + 
                         locationCenterXLambert +
                         ", center y = " + 
                         locationCenterYLambert);
            Debug.output("Creating LambertConformal: origin x = " + 
                         locationOriginX +
                         ", origin y = " + 
                         locationOriginY);
        }
        
    }
    
    /**
     * Sets radian latitude to something sane.  This is an abstract
     * function since some projections don't deal well with extreme
     * latitudes.<p>
     *
     * @param lat float latitude in radians
     * @return float latitude (-PI/2 &lt;= y &lt;= PI/2)
     * @see com.bbn.openmap.LatLonPoint#normalize_latitude(float)
     *
     */
    public float normalize_latitude(float lat) {
        if (lat > NORTH_POLE) {
            return NORTH_POLE;
        } else if (lat < SOUTH_POLE) {
            return SOUTH_POLE;
        }
        return lat;
    }
    
    class LambertPoint {
        double x;
        double y;
    }
    
    /**
     * Pan the map/projection.
     * <ul>
     * <li><code>pan(�180, c)</code> pan south
     * <li><code>pan(-90, c)</code> pan west
     * <li><code>pan(0, c)</code> pan north
     * <li><code>pan(90, c)</code> pan east
     * </ul>
     * @param Az azimuth "east of north" in decimal degrees:
     * <code>-180 &lt;= Az &lt;= 180</code>
     */
    public void pan(float Az) {
        if (MoreMath.approximately_equal(Math.abs(Az), 180f, 0.01f)) {
            setCenter(inverse(width/2,height));//south
        } else if (MoreMath.approximately_equal(Az, -135f, 0.01f)) {
            setCenter(inverse(0,height));//southwest
        } else if (MoreMath.approximately_equal(Az, -90f, 0.01f)) {
            setCenter(inverse(0,height/2));//west
        } else if (MoreMath.approximately_equal(Az, -45f, 0.01f)) {
            setCenter(inverse(0,0));//northwest
        } else if (MoreMath.approximately_equal(Az, 0f, 0.01f)) {
            setCenter(inverse(width/2,0));//north
        } else if (MoreMath.approximately_equal(Az, 45f, 0.01f)) {
            setCenter(inverse(width,0));//northeast
        } else if (MoreMath.approximately_equal(Az, 90f, 0.01f)) {
            setCenter(inverse(width,height/2));//east
        } else if (MoreMath.approximately_equal(Az, 135f, 0.01f)) {
            setCenter(inverse(width,height));//southeast
        } else {
            super.pan(Az);
        }
    }

    /*----------------------------------------------------------------------------
     * FUNCTION:     LLToWorld
     * DATE CREATE:  28 February 1995
     * CREATED BY:   David J. Ward
     * DESCRIPTION:  This function converts lat, lon coordinate to lambert
     *               coordinate. 
     *--------------------------------------------------------------------------*/
    public LambertPoint LLToWorld(double lat, double lon, LambertPoint lp) {
        double formula_one = Math.abs(lambert_lamf) *
            Math.pow(Math.abs(Math.tan(ProjMath.degToRad(90.0 - (double) lat) / 2.0)),
                      Math.abs(lambert_lamn));
        
        lon -=centralMeridian;
        while (lon > 180f) lon -= 360f;
        while (lon <= -180f) lon += 360f;
        
        double formula_two = ProjMath.degToRad(Math.abs(lambert_lamn) * (lon ));
        
        lp.x = formula_one * Math.sin(formula_two);
        lp.y = formula_one * Math.cos(formula_two);
        
        return lp;
    } /* end of function LLToWorld */
    
    
    /*----------------------------------------------------------------------------
     * FUNCTION:     lat_lon_to_pixel
     * DATE CREATE:  28 February 1995
     * CREATED BY:   David J. Ward
     * DESCRIPTION:  This function converts lat, lon coordinate to pixel
     *               coordinate. 
     *--------------------------------------------------------------------------*/
    
    public Point LLToPixel(double lat, double lon, Point p) {
        LambertPoint lp = new LambertPoint();
        
        LLToWorld(lat, lon, lp);
        
        double xrel = lp.x - locationCenterXLambert;
        double yrel = lp.y - locationCenterYLambert;
        
        xrel = locationCenterXPixel + (xrel * locationPixelsPerLambert) + .5;
        yrel = locationCenterYPixel + (yrel * locationPixelsPerLambert) + .5;
        if (p == null) p = new Point();
        p.x = (int) xrel;
        p.y = (int) yrel;
        
        return p;
    } /* end of function LLToPixel */
    
    
    /*----------------------------------------------------------------------------
     * FUNCTION:     worldToLL
     * DATE CREATE:  28 February 1995
     * CREATED BY:   David J. Ward
     * DESCRIPTION:  This function converts lambert projections to lat, lon
     *               coordinate.  It is given the lambert projections as the
     *               input, and the lat, lon coordinate as the output.
     *--------------------------------------------------------------------------*/
    public LatLonPoint worldToLL(double x, double y, LatLonPoint llp) {

        double formula_two = Math.atan2(x,y);
        double formula_one = Math.sqrt(x * x + y * y);
        
        double lon = ProjMath.radToDeg(formula_two / Math.abs(lambert_lamn)) + centralMeridian;
        double lat  = 90.0 - ProjMath.radToDeg(Math.atan2(Math.pow((formula_one/Math.abs(lambert_lamf)), (1./lambert_lamn)), 1) * 2.0);
        
        if (lambert_lamn < 0.0)
            lat *=  -1.0;
        
        llp.setLatLon((float)lat, (float) lon);
        
        return llp;
    } /* end of function worldToLL */
    
    /*----------------------------------------------------------------------------
     * FUNCTION:     pixelToLL
     * DATE CREATE:  28 February 1995
     * CREATED BY:   David J. Ward
     * DESCRIPTION:  This function converts pixel coordinate into lat, lon
     *               coordinate. 
     *--------------------------------------------------------------------------*/
    public LatLonPoint pixelToLL(int xabs, int yabs, LatLonPoint llp) {

        double x = locationCenterXLambert +
            (((int) xabs - locationCenterXPixel) / locationPixelsPerLambert);
        double y = locationCenterYLambert +
            (((int) yabs - locationCenterYPixel) / locationPixelsPerLambert);
        
        worldToLL(x, y, llp);
        
        return llp;
    } /* end of function pixelToLL */
    
    
    protected Point plotablePoint = new Point();

    /** 
     * Determine if the location is plotable on the screen. The Lambert Conformal projection
     * does not lend its self to a simple determination. This method invokes forward to
     * obtain the screen coordinates. If the screen coordinates are visible returns true
     * otherwise returns false.
     * @param lat latitude in degrees
     * @param lon longitude in degrees
     * @return true is plotable, otherwise false
     */
    public boolean isPlotable(float lat, float lon) {
        //It is almost impossible to determine it the location
        // is plotable without calling forward() for the Point
        // and checking if the point is in bounds.
        // Be lazy and return true.
        
        if (lat < -55f) return false;
        forward(lat, lon, plotablePoint);
        if (plotablePoint.x >= 0 && plotablePoint.x < this.width && plotablePoint.y >= 0 && plotablePoint.y < height) {
            return true;
        }
        return false;
    }
    
    /** Determine if the location is plotable
     * @param llpoint location to check
     * @return returns true is plotable, otherwise false
     */
    public boolean isPlotable(LatLonPoint llpoint) {
        return isPlotable(llpoint.getLatitude(), llpoint.getLongitude());
    }
    
    /**
     * Projects a point from Lat/Lon space to X/Y space.
     * <p>
     * @param pt LatLonPoint
     * @param p Point retval
     * @return Point p
     */
    public Point forward(LatLonPoint pt, Point p) {
        return forward(pt.radlat_, pt.radlon_, p, true);
    }
    
    /**
     * Forward projects a lat,lon coordinates.
     * <p>
     * @param lat raw latitude in decimal degrees
     * @param lon raw longitude in decimal degrees
     * @param p Resulting XY Point
     * @return Point p
     */
    public Point forward(float lat, float lon, Point p) {
        return forward(lat, lon, p, false);
    }
    
    /** 
     * Forward projects lat,lon into XY space and returns a Point.
     * <p>
     * @return Point p
     * @param lat latitude
     * @param lon longitude
     * @param p Resulting XY Point
     * @param isRadian indicates that lat,lon
     * arguments are in radians
     */
    public Point forward(float lat, float lon, Point p, boolean isRadian) {
        
        // Figure out the point for screen coordinates.  Need to take
        // into account that the origin point of the projection may be
        // off screen, so we need to take the calculated world
        // coordinates of the center of the screen and subtract the
        // screen offset from that.
        if (isRadian) {
            LLToPixel(ProjMath.radToDeg(lat), ProjMath.radToDeg(lon), p);
        } else {
            LLToPixel(lat, lon, p);
        }
        return p;
    }
    
    
    /**
     * Inverse project x,y coordinates into a LatLonPoint.
     * <p>
     * @param x integer x coordinate
     * @param y integer y coordinate
     * @param llp LatLonPoint
     * @return LatLonPoint llp
     * @see Proj#inverse(Point)
     */
    public LatLonPoint inverse(int x, int y, LatLonPoint llp) {
        if (llp == null) {
            llp = new LatLonPoint();
        }
        // convert from screen to world coordinates
        pixelToLL(x, y, llp);
        return llp;
    }
    
    
    /** 
     * Inverse project a Point.
     * <p>
     * @return LatLonPoint llp
     * @param pt Point
     * @param llp resulting LatLonPoint
     */
    public LatLonPoint inverse(Point pt, LatLonPoint llp) {
        return inverse(pt.x, pt.y, llp);
    }
    
    /**
     * Get the upper left (northwest) point of the projection.
     * <p>
     * Returns the upper left point (or closest equivalent) of the
     * projection based on the center point and height and width of
     * screen.
     * <p>
     * @return LatLonPoint
     */
    public LatLonPoint getUpperLeft() {
        // In a conic projection the upper left is meaningless
        // unless at realitively small scales.
        // Return 90.0 -180 until someone fugures out a better way.
        return new LatLonPoint(90.0, -180.0);
    }
    
    /**
     * Get the lower right (southeast) point of the projection.
     * <p>
     * Returns the lower right point (or closest equivalent) of the
     * projection based on the center point and height and width of
     * screen.
     * <p>
     * @return LatLonPoint
     */
    public LatLonPoint getLowerRight() {
        // In a conic projection the upper left is meaningless
        // unless at realitively small scales.
        // Return 90.0 -180 until someone fugures out a better way.
        return new LatLonPoint(-90.0, 180.0);
    }

    /** 
     * Get the name string of the projection.
     * @return the projection name
     */
    public String getName() {
        return LambertConformalName;
    }
    
    /**
     * Forward project a raw array of radian points.
     * This assumes nothing about the array of coordinates.  In no way does it
     * assume the points are connected or that the composite figure is to be
     * filled.
     * <p>
     * It does populate a visible array indicating whether the points are
     * visible on the projected view of the world.
     * <p>
     * @param rawllpts array of lat,lon,... in radians
     * @param rawoff offset into rawllpts
     * @param xcoords x coordinates
     * @param ycoords y coordinates
     * @param visible coordinates visible?
     * @param copyoff offset into x,y,visible arrays
     * @param copylen number of coordinates (coordinate arrays should be at
     * least this long, rawllpts should be at least twice as long).
     * @return boolean true if all points visible, false if some points not
     * visible.
     */
    public boolean forwardRaw(float[] rawllpts, int rawoff,
                              int[] xcoords, int[] ycoords, boolean[] visible,
                              int copyoff, int copylen) {
        boolean visibleTotal = false;
        // HACK grabbed from Cylindrical.  Might need fixing.
        Point temp = new Point();
        int end = copylen+copyoff;
        for (int i=copyoff, j=rawoff; i<end; i++, j+=2) {
            forward(rawllpts[j], rawllpts[j+1], temp, true);
            xcoords[i] = temp.x;
            ycoords[i] = temp.y;
            
            visible[i] = (0 <= temp.x && temp.x <= width) &&
                (0 <= temp.y && temp.y <= height);
            
            if (visible[i] == true && visibleTotal == false) {
                visibleTotal = true;
            }
            
        }
        // if everything is visible
        return visibleTotal;
    }
    
    /**
     * Forward project a lat/lon Poly.
     * Remember to specify vertices in radians!
     * @param rawllpts float[] of lat,lon,lat,lon,... in RADIANS!
     * @param ltype line type (straight, rhumbline, greatcircle)
     * @param nsegs number of segment points (only for greatcircle or
     * rhumbline line types, and if &lt; 1, this value is generated internally)
     * @param isFilled filled poly?  this is currently ignored
     * for cylindrical projections.
     * @return Vector of x[], y[], x[], y[], ... projected poly
     */
    protected ArrayList _forwardPoly(float[] rawllpts,
                                     int ltype,
                                     int nsegs, boolean isFilled) {

        int i, j;
        
        // determine length of pairs
        int len = rawllpts.length >> 1;	// len/2, chop off extra
        if (len < 2)
            return new ArrayList(0);
        
        // Not concerned with any polygons that are completely below 60S
        
        float minlat = ProjMath.degToRad(-60f);
        boolean allBelowMinLat = true;
        for (i=0, j=0; i<len; i++, j+=2) {
            float l = rawllpts[j+1];
            while (l < 0f) l += Math.PI*2f;
            if (rawllpts[j] > minlat) {
                allBelowMinLat = false;
            }
        }
        if (allBelowMinLat) {
            return new ArrayList(0);
        }

        // handle complicated line in specific routines
        if (isComplicatedLineType(ltype))
            return doPolyDispatch(rawllpts, ltype, nsegs, isFilled);
        
        Point temp = new Point();
        int[] xs = new int[len];
        int[] ys = new int[len];
        
        // forward project the points
        for (i=0, j=0; i<len; i++, j+=2) {
            
            temp = forward(rawllpts[j], rawllpts[j+1], temp, true);
            xs[i] = temp.x;
            ys[i] = temp.y;
        }
        
        ArrayList ret_val = new ArrayList(2);
        ret_val.add(xs);
        ret_val.add(ys);
        
        return ret_val;
    }
    
    /**
     * Draw the background for the projection.
     * @param g Graphics2D
     * @param paint java.awt.Paint to use for the background
     */
    public void drawBackground(java.awt.Graphics2D g, java.awt.Paint paint) {
        g.setPaint(paint);
        drawBackground(g);
    }

    /**
     * Draw the background for the projection.
     * @param g Graphics
     */
    public void drawBackground(java.awt.Graphics g) {
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    /** 
     * test method
     * @param argv command line parameters
     */
    public static void main(String argv[]) {
        Debug.init();
        Debug.put("Lambert");
        LambertConformal proj=null;
        centralMeridian = 15.0f;
        lambert_sp_one = 21.67f;
        lambert_sp_two = 48.33f;
        Debug.message("Lambert", "proj = new LambertConformal(new LatLonPoint(0.0f, 0.0f), 100000.0f, 620, 480);");
        proj = new LambertConformal(new LatLonPoint(0f, 0f), 100000.0f, 620, 480);
        Debug.message("Lambert",""+proj.inverse(0,0));
    }
    
    
}