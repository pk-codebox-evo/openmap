// **********************************************************************
// 
// <copyright>
// 
//  BBN Technologies, a Verizon Company
//  10 Moulton Street
//  Cambridge, MA 02138
//  (617) 873-8000
// 
//  Copyright (C) BBNT Solutions LLC. All rights reserved.
// 
// </copyright>
// **********************************************************************
// 
// $Source: /cvs/distapps/openmap/src/openmap/com/bbn/openmap/layer/GraticuleLayer.java,v $
// $RCSfile: GraticuleLayer.java,v $
// $Revision: 1.5 $
// $Date: 2004/01/26 18:18:08 $
// $Author: dietrick $
// 
// **********************************************************************

// Modified 28 September 2002 by David N. Allsopp to allow font size
// to be changed.  See sections commented with 'DNA'.

package com.bbn.openmap.layer;

import java.awt.*;
import java.awt.event.*;
import java.util.Properties;
import javax.swing.*;

import com.bbn.openmap.*;
import com.bbn.openmap.event.*;
import com.bbn.openmap.omGraphics.*;
import com.bbn.openmap.proj.*;
import com.bbn.openmap.util.*;

/**
 * Layer that draws graticule lines.  If the showRuler property is
 * set to true, then longitude values are displayed on the bottom of
 * the map, and latitude values are displayed on the left side.  If
 * the show1And5Lines property is true, then 5 degree lines are drawn
 * when there are &lt;= threshold ten degree latitude or longitude lines,
 * and 1 degree lines are drawn when there are &lt;= threshold five
 * degree latitude or longitude degree lines.
 *
 * <P> The openmap.properties file can control the layer with the
 * following settings:
 * <code><pre>
 * # Show lat / lon spacing labels
 * graticule.showRuler=true
 * graticule.show1And5Lines=true
 * # Controls when the five degree lines and one degree lines kick in
 * #- when there is less than the threshold of ten degree lat or lon
 * #lines, five degree lines are drawn.  The same relationship is there
 * #for one to five degree lines.
 * graticule.threshold=2
 * # the color of 10 degree spacing lines (Hex ARGB)
 * graticule.10DegreeColor=FF000000
 * # the color of 5 degree spacing lines (Hex ARGB)
 * graticule.5DegreeColor=C7009900
 * # the color of 1 degree spacing lines (Hex ARGB)
 * graticule.1DegreeColor=C7003300
 * # the color of the equator (Hex ARGB)
 * graticule.equatorColor=FFFF0000
 * # the color of the international dateline (Hex ARGB)
 * graticule.datelineColor=7F000099
 * # the color of the special lines (Hex ARGB)
 * graticule.specialLineColor=FF000000
 * # the color of the labels (Hex ARGB)
 * graticule.textColor=FF000000
 * </pre></code>
 * In addition, you can get this layer to work with the OpenMap viewer
 * by editing your openmap.properties file:
 * <code><pre>
 * # layers
 * openmap.layers=graticule ...
 * # class
 * graticule.class=com.bbn.openmap.layer.GraticuleLayer
 * # name
 * graticule.prettyName=Graticule
 * </pre></code>
 *
 */
public class GraticuleLayer extends OMGraphicHandlerLayer 
    implements ActionListener {

    // default to not showing the ruler (mimicing older GraticuleLayer)
    protected boolean defaultShowRuler = false;
    protected boolean defaultShowOneAndFiveLines = false;
    protected int defaultThreshold = 2;

    /**
     * Flag for lineType - true is LINETYPE_STRAIGHT, false is
     * LINETYPE_GREATCIRCLE. 
     */
    private boolean boxy = true;
    /**
     * Threshold is the total number of ten lines on the screen before the
     * five lines appear, and the total number of five lines on the screen
     * before the one lines appear.
     */
    protected int threshold = defaultThreshold;
    /** The ten degree latitude and longitude lines, premade. */
    protected OMGraphicList tenDegreeLines = null;
    /** The equator, dateline and meridian lines, premade. */
    protected OMGraphicList markerLines = null;

    private final static int SHOW_TENS = 0;
    private final static int SHOW_FIVES = 1;
    private final static int SHOW_ONES = 2;

    protected boolean showOneAndFiveLines = defaultShowOneAndFiveLines;
    protected boolean showRuler = defaultShowRuler;

//     protected Font font = new Font("Helvetica", java.awt.Font.PLAIN, 10);
    protected Font font = null;
    protected int fontSize = 10;

    // Color variables for different line types
    protected Color tenDegreeColor = null;
    protected Color fiveDegreeColor = null;
    protected Color oneDegreeColor = null;
    protected Color equatorColor = null;
    protected Color dateLineColor = null;
    protected Color specialLineColor = null; // Tropic of Cancer, Capricorn
    protected Color textColor = null;

    // Default colors to use, if not specified in the properties.
    protected String defaultTenDegreeColorString = "000000";
    protected String defaultFiveDegreeColorString = "33009900";
    protected String defaultOneDegreeColorString = "33003300";
    protected String defaultEquatorColorString = "990000";
    protected String defaultDateLineColorString = "000099";
    protected String defaultSpecialLineColorString = "000000"; 
    protected String defaultTextColorString = "000000";

    // property text values
    public static final String TenDegreeColorProperty = "10DegreeColor";
    public static final String FiveDegreeColorProperty = "5DegreeColor";
    public static final String OneDegreeColorProperty = "1DegreeColor";
    public static final String EquatorColorProperty = "equatorColor";
    public static final String DateLineColorProperty = "datelineColor";
    public static final String SpecialLineColorProperty = "specialLineColor";
    public static final String TextColorProperty = "textColor";
    public static final String ThresholdProperty = "threshold";
    public static final String ShowRulerProperty = "showRuler";
    public static final String ShowOneAndFiveProperty = "show1And5Lines";
    public static final String FontSizeProperty = "fontSize"; //DNA

    /**
     * Construct the GraticuleLayer.
     */
    public GraticuleLayer() {
        // precalculate for boxy
        boxy = true;
    }

    /** 
     * The properties and prefix are managed and decoded here, for
     * the standard uses of the GraticuleLayer.
     *
     * @param prefix string prefix used in the properties file for this layer.
     * @param properties the properties set in the properties file.  
     */
    public void setProperties(String prefix, java.util.Properties properties) {
        super.setProperties(prefix, properties);
        prefix = PropUtils.getScopedPropertyPrefix(prefix);

        tenDegreeColor = PropUtils.parseColorFromProperties(
            properties, prefix + TenDegreeColorProperty,
            defaultTenDegreeColorString);       

        fiveDegreeColor = PropUtils.parseColorFromProperties(
            properties, prefix + FiveDegreeColorProperty,
            defaultFiveDegreeColorString);
        
        oneDegreeColor = PropUtils.parseColorFromProperties(
            properties, prefix + OneDegreeColorProperty,
            defaultOneDegreeColorString);       
        
        equatorColor = PropUtils.parseColorFromProperties(
            properties, prefix + EquatorColorProperty,
            defaultEquatorColorString); 
        
        dateLineColor = PropUtils.parseColorFromProperties(
            properties, prefix + DateLineColorProperty,
            defaultDateLineColorString);        
        
        specialLineColor = PropUtils.parseColorFromProperties(
            properties, prefix + SpecialLineColorProperty,
            defaultSpecialLineColorString);     
        
        textColor = PropUtils.parseColorFromProperties(
            properties, prefix + TextColorProperty,
            defaultTextColorString);    

        threshold = PropUtils.intFromProperties(properties, 
                                                 prefix + ThresholdProperty, 
                                                 defaultThreshold);

        fontSize = PropUtils.intFromProperties(properties, 
                                                prefix + FontSizeProperty, 
                                                fontSize);
        font = new Font("Helvetica", java.awt.Font.PLAIN, fontSize);

        setShowOneAndFiveLines(PropUtils.booleanFromProperties(
            properties, prefix + ShowOneAndFiveProperty, 
            defaultShowOneAndFiveLines));

        setShowRuler(PropUtils.booleanFromProperties(
            properties, prefix + ShowRulerProperty, 
            defaultShowRuler));

        // So they will get re-created.
        tenDegreeLines = null;
        markerLines = null;
    }

    protected JCheckBox showRulerButton = null;    
    protected JCheckBox show15Button = null;
                                           
    public void setShowOneAndFiveLines(boolean set) {
        showOneAndFiveLines = set;
        if (show15Button != null) {
            show15Button.setSelected(set);
        }
    }

    public boolean getShowOneAndFiveLines() {
        return showOneAndFiveLines;
    }

    public void setShowRuler(boolean set) {
        showRuler = set;
        if (showRulerButton != null) {
            showRulerButton.setSelected(set);
        }
    }

    public boolean getShowRuler() {
        return showRuler;
    }

    /** 
     * The properties and prefix are managed and decoded here, for
     * the standard uses of the GraticuleLayer.
     *
     * @param properties the properties set in the properties file.  
     */
    public Properties getProperties(Properties properties) {
        properties = super.getProperties(properties);

        String prefix = PropUtils.getScopedPropertyPrefix(this);
        String colorString;

        if (tenDegreeColor == null) {
            colorString = defaultTenDegreeColorString;
        } else {
            colorString = Integer.toHexString(tenDegreeColor.getRGB());
        }
        properties.put(prefix + TenDegreeColorProperty, colorString);

        if (fiveDegreeColor == null) {
            colorString = defaultFiveDegreeColorString;
        } else {
            colorString = Integer.toHexString(fiveDegreeColor.getRGB());
        }
        properties.put(prefix + FiveDegreeColorProperty, colorString);

        if (oneDegreeColor == null) {
            colorString = defaultOneDegreeColorString;
        } else {
            colorString = Integer.toHexString(oneDegreeColor.getRGB());
        }
        properties.put(prefix + OneDegreeColorProperty, colorString);
        
        if (equatorColor == null) {
            colorString = defaultEquatorColorString;
        } else {
            colorString = Integer.toHexString(equatorColor.getRGB());
        }
        properties.put(prefix + EquatorColorProperty, colorString);
        
        if (dateLineColor == null) {
            colorString = defaultDateLineColorString;
        } else {
            colorString = Integer.toHexString(dateLineColor.getRGB());
        }
        properties.put(prefix + DateLineColorProperty, colorString);
        
        if (specialLineColor == null) {
            colorString = defaultSpecialLineColorString;
        } else {
            colorString = Integer.toHexString(specialLineColor.getRGB());
        }
        properties.put(prefix + SpecialLineColorProperty, colorString);
        
        if (textColor == null) {
            colorString = defaultTextColorString;
        } else {
            colorString = Integer.toHexString(textColor.getRGB());
        }
        properties.put(prefix + TextColorProperty, colorString);
        
        properties.put(prefix + ThresholdProperty, Integer.toString(threshold));
        properties.put(prefix + FontSizeProperty, Integer.toString(fontSize)); //DNA

        properties.put(prefix + ShowOneAndFiveProperty, new Boolean(showOneAndFiveLines).toString());
        
        properties.put(prefix + ShowRulerProperty, new Boolean(showRuler).toString());

        return properties;
    }

    /** 
     * The properties and prefix are managed and decoded here, for
     * the standard uses of the GraticuleLayer.
     *
     * @param properties the properties set in the properties file.  
     */
    public Properties getPropertyInfo(Properties properties) {
        properties = super.getPropertyInfo(properties);

        properties.put(initPropertiesProperty, TenDegreeColorProperty + " " + FiveDegreeColorProperty + " " + OneDegreeColorProperty + " " + EquatorColorProperty + " " + DateLineColorProperty + " " + SpecialLineColorProperty + " " + ShowOneAndFiveProperty + " " + ShowRulerProperty + " " + ThresholdProperty + " " + FontSizeProperty);

        properties.put(TenDegreeColorProperty, "Color of the ten degree graticule lines.");
        properties.put(TenDegreeColorProperty + ScopedEditorProperty, 
                       "com.bbn.openmap.util.propertyEditor.ColorPropertyEditor");

        properties.put(FiveDegreeColorProperty, "Color of the five degree graticule lines.");
        properties.put(FiveDegreeColorProperty + ScopedEditorProperty, 
                       "com.bbn.openmap.util.propertyEditor.ColorPropertyEditor");

        properties.put(OneDegreeColorProperty, "Color of the one degree graticule lines.");
        properties.put(OneDegreeColorProperty + ScopedEditorProperty, 
                       "com.bbn.openmap.util.propertyEditor.ColorPropertyEditor");

        properties.put(EquatorColorProperty, "Color of the Equator.");
        properties.put(EquatorColorProperty + ScopedEditorProperty, 
                       "com.bbn.openmap.util.propertyEditor.ColorPropertyEditor");

        properties.put(DateLineColorProperty, "Color of the Date line.");
        properties.put(DateLineColorProperty + ScopedEditorProperty, 
                       "com.bbn.openmap.util.propertyEditor.ColorPropertyEditor");

        properties.put(SpecialLineColorProperty, "Color of Tropic of Cancer, Capricorn lines.");
        properties.put(SpecialLineColorProperty + ScopedEditorProperty, 
                       "com.bbn.openmap.util.propertyEditor.ColorPropertyEditor");

        properties.put(TextColorProperty, "Color of the line label text.");
        properties.put(TextColorProperty + ScopedEditorProperty, 
                       "com.bbn.openmap.util.propertyEditor.ColorPropertyEditor");

        properties.put(ThresholdProperty, "The number of lines showing before finer grain lines appear.");

        properties.put(ShowOneAndFiveProperty, "Show the one and five degree lines.");
        properties.put(ShowOneAndFiveProperty + ScopedEditorProperty, 
                       "com.bbn.openmap.util.propertyEditor.TrueFalsePropertyEditor");

        properties.put(ShowRulerProperty, "Show the line label text.");
        properties.put(ShowRulerProperty + ScopedEditorProperty, 
                       "com.bbn.openmap.util.propertyEditor.TrueFalsePropertyEditor");

        //DNA
        properties.put(FontSizeProperty, "The size of the font, in points, of the line labels");
        //DNA
        return properties;
    }

    /** 
     * Implementing the ProjectionPainter interface.
     */
    public synchronized void renderDataForProjection(Projection proj, java.awt.Graphics g) {
        if (proj == null) {
            Debug.error("GraticuleLayer.renderDataForProjection: null projection!");
            return;
        } else if (!proj.equals(getProjection())) {
            setProjection(proj.makeClone());
            // Figure out which line type to use
            if (proj instanceof Cylindrical) boxy = true;
            else boxy = false;

            setList(constructGraticuleLines());
        }
        paint(g);
    }

    /**
     * Invoked when the projection has changed or this Layer has been added to
     * the MapBean.
     * <p>
     * Perform some extra checks to see if reprojection of the graphics is
     * really necessary.
     * @param e ProjectionEvent
     *
     */    
    public void projectionChanged(ProjectionEvent e) {

        // extract the projection and check to see if it's really different.
        // if it isn't then we don't need to do all the work again, just
        // repaint.
        Projection proj = setProjection(e);
        if (proj == null) {
            repaint();
            return;
        }
        
        // Figure out which line type to use
        if (proj instanceof Cylindrical) boxy = true;
        else boxy = false;

        setList(null);
        doPrepare();
    }

    /**
     * Creates the OMGraphic list with graticule lines.
     */
    public OMGraphicList prepare() {
        return constructGraticuleLines();
    }

    /**
     * Create the graticule lines.
     * <p>
     * NOTES:
     * <ul>
     * <li>Currently graticule lines are hardcoded to 10 degree intervals.
     * <li>No thought has been given to clipping based on the view rectangle.
     * For non-boxy projections performance may be degraded at very large
     * scales.  (But we make up for this by running the task in its own
     * thread to support liveness).
     * </ul>
     * @return OMGraphicList new graphic list
     */
    protected OMGraphicList constructGraticuleLines() {
        float[] llp;
        OMGraphicList newgraphics = new OMGraphicList(20);
        // Lets figure out which lines should be painted...
        Projection projection = getProjection();

        if (showOneAndFiveLines || showRuler) {

            float left = projection.getUpperLeft().getLongitude();
            float right = projection.getLowerRight().getLongitude();
            float up = projection.getUpperLeft().getLatitude();
            float down = projection.getLowerRight().getLatitude();

            if (up > 80.0f) up = 80.0f;
            if (down > 80.0f) down = 80f; // unlikely
            if (up < -80.0f) up = -80.0f; // unlikely
            if (down < -80) down = -80.0f;

            int showWhichLines = evaluateSpacing(up, down, left, right);

            // Find out whether we need to do one or two queries,
            // depending on if we're straddling the dateline.
            if ((left > 0 && right < 0) || 
                (left > right) || 
                (Math.abs(left - right) < 1)) {
                // Test to draw the ones and fives, which will also do
                // the labels.

                if (showWhichLines != SHOW_TENS) {
                    newgraphics.add(constructGraticuleLines(up, down, left, 180.0f, showWhichLines));
                    newgraphics.add(constructGraticuleLines(up, down, -180.0f, right, showWhichLines));
                } else if (showRuler) {  // Just do the labels for the tens lines
                    newgraphics.add(constructTensLabels(up, down, left, 180.0f, true));
                    newgraphics.add(constructTensLabels(up, down, -180.0f, right, false));
                }
            } else {
                // Test to draw the ones and fives, which will also do
                // the labels.
                if (showWhichLines != SHOW_TENS) {
                    newgraphics = constructGraticuleLines(up, down, left, right, showWhichLines);
                } else if (showRuler) {  // Just do the labels for the tens lines
                    newgraphics.add(constructTensLabels(up, down, left, right, true));
                }
            }
        }

        OMGraphicList list;
        if (tenDegreeLines == null) {
            list = constructTenDegreeLines();
            tenDegreeLines = list;
        } else {
            synchronized(tenDegreeLines) {
                setLineTypeAndProject(tenDegreeLines, boxy ? OMGraphic.LINETYPE_STRAIGHT : OMGraphic.LINETYPE_RHUMB);
            }
        }
        if (markerLines == null) {
            list = constructMarkerLines();
            markerLines = list;
        } else {
            synchronized(markerLines) {
                setLineTypeAndProject(markerLines, boxy ? OMGraphic.LINETYPE_STRAIGHT : OMGraphic.LINETYPE_RHUMB);
            }
        }

        newgraphics.add(markerLines);
        newgraphics.add(tenDegreeLines);

        if (Debug.debugging("graticule")) {
            Debug.output("GraticuleLayer.constructGraticuleLines(): " +
                    "constructed " + newgraphics.size() + " graticule lines");
        }

        return newgraphics;
    }

    /** 
     * Figure out which graticule lines should be drawn based on the
     * treshold set in the layer, and the coordinates of the screen.
     * Method checks for crossing of the dateline, but still assumes
     * that the up and down latitude coordinates are less than
     * abs(+/-80).  This is because the projection shouldn't give
     * anything above 90 degrees, and we limit the lines to less than
     * 80..
     *
     * @param up northern latitude corrdinate, in decimal degrees,
     * @param down southern latitude coordinate, in decimal degrees.
     * @param left western longitude coordinate, in decimal degrees,
     * @param right eastern longitude coordinate, in decimal degrees.
     * @return which lines should be shown, either SHOW_TENS,
     * SHOW_FIVES and SHOW_ONES.  
     */
    protected int evaluateSpacing(float up, float down, 
                                  float left, float right) {
        int ret = SHOW_TENS;

        // Set the flag for when labels are wanted, but not the 1 and
        // 5 lines;
        if (!showOneAndFiveLines) {
            return ret;
        }

        // Find the north - south difference
        float nsdiff = up - down;
        // And the east - west difference
        float ewdiff;
        // Check for straddling the dateline -west is positive while
        // right is negative, or, in a big picture view, the west is
        // positive, east is positive, and western hemisphere is
        // between them.
        if ((left > 0 && right < 0) || 
            (left > right) || 
            (Math.abs(left - right) < 1)) {
            ewdiff = (180.0f - left) + (right + 180.0f);
        } else {
            ewdiff = right - left;
        }

        // And use the lesser of the two.
        float diff = (nsdiff < ewdiff)?nsdiff:ewdiff;
        // number of 10 degree lines
        if ((diff/10) <= (float)threshold) ret = SHOW_FIVES;
        // number of five degree lines
        if ((diff/5) <= (float)threshold) ret = SHOW_ONES;

        return ret;
    }

    /** 
     * Construct the five degree and one degree graticule lines,
     * depending on the showWhichLines setting. Assumes that the
     * coordinates passed in do not cross the dateline, and that the
     * up is not greater than 80 and that the south is not less than
     * -80.
     *
     * @param up northern latitude corrdinate, in decimal degrees,
     * @param down southern latitude coordinate, in decimal degrees.
     * @param left western longitude coordinate, in decimal degrees,
     * @param right eastern longitude coordinate, in decimal degrees.
     * @param showWhichLines indicator for which level of lines should
     * be included, either SHOW_FIVES or SHOW_ONES.  SHOW_TENS could
     * be there, too, but then we wouldn't do anything.  
     */
    protected OMGraphicList constructGraticuleLines(float up, float down, 
                                                    float left, float right, 
                                                    int showWhichLines) {
        OMGraphicList lines = new OMGraphicList();

        // Set the line limits for the lat/lon lines...
        int north = (int)Math.ceil(up);
        if (north > 80) north = 80;

        int south = (int)Math.floor(down);
        south -= (south%10); // Push down to the lowest 10 degree line.
        // for neg numbers, Mod raised it, lower it again.  Also
        // handle straddling the equator.
        if ((south < 0 && south > -80) || south == 0) south -= 10; 

        int west = (int)Math.floor(left);
        west -= (west%10);
        // for neg numbers, Mod raised it, lower it again.  Also
        // handle straddling the prime meridian.
        if ((west < 0 && west > -180) || west == 0) west -= 10;

        int east = (int)Math.ceil(right);
        if (east > 180) east = 180;

        int stepSize;
        // Choose how far apart the lines will be.
        stepSize = ((showWhichLines == SHOW_ONES)? 1:5);
        float[] llp;
        OMPoly currentLine;
        OMText currentText;

        // For calculating text locations
        java.awt.Point point;
        LatLonPoint llpoint;

        Projection projection = getProjection();

        // generate other parallels of latitude be creating series
        // of polylines
        for (int i = south; i < north; i += stepSize) {
            float lat = (float)i;
            // generate parallel of latitude North/South of the equator
            if (west < 0 && east > 0) {
                llp = new float[6];
                llp[2] = lat; llp[3] = 0f;
                llp[4] = lat; llp[5] = east;
            } else {
                llp = new float[4];
                llp[2] = lat; llp[3] = east;
            }
            llp[0] = lat; llp[1] = west;

            // Do not duplicate the 10 degree line.
            if ((lat%10) != 0) {
                currentLine = new OMPoly(llp, OMGraphic.DECIMAL_DEGREES,
                                         boxy ? OMGraphic.LINETYPE_STRAIGHT
                                         : OMGraphic.LINETYPE_RHUMB);
                if ((lat%5) == 0) {
                    currentLine.setLinePaint(fiveDegreeColor);
                } else {
                    currentLine.setLinePaint(oneDegreeColor);
                }
                lines.addOMGraphic(currentLine);
            }

            if (showRuler && (lat%2) == 0) {
                if (boxy) {
                    point = projection.forward(lat, west);
                    point.x = 0;
                    llpoint = projection.inverse(point);
                } else {
                    llpoint = new LatLonPoint(lat, west);
                    while (projection.forward(llpoint).x < 0) {
                        llpoint.setLongitude(llpoint.getLongitude() + stepSize);
                    }
                }
                
                currentText = new OMText(llpoint.getLatitude(), 
                                         llpoint.getLongitude(),
                                         // Move them up a little
                                         (int)2, (int) -2, 
                                         Integer.toString((int)lat),
                                         font, OMText.JUSTIFY_LEFT);
                currentText.setLinePaint(textColor);
                lines.addOMGraphic(currentText);
            }
        }
        
        // generate lines of longitude
        for (int i = west; i < east; i += stepSize) {
            float lon = (float)i;
            
            if (north < 0 && south > 0) {
                llp = new float[6];
                llp[2] = 0f; llp[3] = lon;
                llp[4] = south; llp[5] = lon;
            } else {
                llp = new float[4];
                llp[2] = south; llp[3] = lon;
            }
            llp[0] = north; llp[1] = lon;
            
            if ((lon%10) != 0) {
                currentLine = new OMPoly(llp, OMGraphic.DECIMAL_DEGREES,
                                         boxy ? OMGraphic.LINETYPE_STRAIGHT
                                         : OMGraphic.LINETYPE_GREATCIRCLE);
                if ((lon%5) == 0) {
                    currentLine.setLinePaint(fiveDegreeColor);
                } else {
                    currentLine.setLinePaint(oneDegreeColor);
                }
                lines.addOMGraphic(currentLine);
            }

            if (showRuler && (lon%2) == 0) {
                if (boxy) {
                    point = projection.forward(south, lon);
                    point.y = projection.getHeight();
                    llpoint = projection.inverse(point);
                } else {
                    llpoint = new LatLonPoint(south, lon);
                    while (projection.forward(llpoint).y > projection.getHeight()) {
                        llpoint.setLatitude(llpoint.getLatitude() + stepSize);
                    }
                }
                
                currentText = new OMText(llpoint.getLatitude(), 
                                         llpoint.getLongitude(),
                                         // Move them up a little
                                         (int)2, (int) -5, 
                                         Integer.toString((int)lon),
                                         font, OMText.JUSTIFY_CENTER);
                currentText.setLinePaint(textColor);
                lines.addOMGraphic(currentText);

            }
        }

        if (Debug.debugging("graticule")) {
            Debug.output("GraticuleLayer.constructTenDegreeLines(): " +
                    "constructed " + lines.size() + " graticule lines");
        }
        lines.generate(projection);
        return lines;
    }

    /** Create the ten degree lines. */
    protected OMGraphicList constructTenDegreeLines() {

        OMGraphicList lines = new OMGraphicList(3);
        OMPoly currentLine;

        // generate other parallels of latitude by creating series
        // of polylines
        for (int i = 1; i <= 8; i++) {
            for (int j = -1; j < 2; j += 2) {
                float lat = (float)(10*i*j);
                // generate parallel of latitude North/South of the equator
                float[] llp = {lat, -180f, lat, -90f,
                               lat, 0f, lat, 90f, lat, 180f};
                currentLine =   new OMPoly(llp, OMGraphic.DECIMAL_DEGREES,
                                           boxy ? OMGraphic.LINETYPE_STRAIGHT
                                           : OMGraphic.LINETYPE_RHUMB);
                currentLine.setLinePaint(tenDegreeColor);
                lines.addOMGraphic(currentLine);
            }
        }

        // generate lines of longitude
        for (int i = 1; i < 18; i++) {
            for (int j = -1; j < 2; j += 2) {
                float lon = (float)(10*i*j);
                //not quite 90.0 for beautification reasons.
                float[] llp = {80f, lon,
                               0f,  lon,
                               -80f, lon};
                if (MoreMath.approximately_equal(Math.abs(lon), 90f, 0.001f)) {
                    llp[0] = 90f;
                    llp[4] = -90f;
                }
                currentLine = new OMPoly(llp, OMGraphic.DECIMAL_DEGREES,
                                         boxy ? OMGraphic.LINETYPE_STRAIGHT
                                         : OMGraphic.LINETYPE_GREATCIRCLE);
                currentLine.setLinePaint(tenDegreeColor);
                lines.addOMGraphic(currentLine);
            }
        }

        if (Debug.debugging("graticule")) {
            Debug.output("GraticuleLayer.constructTenDegreeLines(): " +
                    "constructed " + lines.size() + " graticule lines");
        }
        lines.generate(getProjection());
        return lines;
    }

    /** 
     * Constructs the labels for the tens lines.  Called from within
     * the constructGraticuleLines if the showRuler variable is true.
     * Usually called only if the ones and fives lines are not being
     * drawn.
     *
     * @param up northern latitude corrdinate, in decimal degrees,
     * @param down southern latitude coordinate, in decimal degrees.
     * @param left western longitude coordinate, in decimal degrees,
     * @param right eastern longitude coordinate, in decimal degrees.  
     * @param doLats do the latitude labels if true.
     * @return OMGraphicList of labels.
     */
    protected OMGraphicList constructTensLabels(float up, float down, 
                                                float left, float right, 
                                                boolean doLats) {

        OMGraphicList labels = new OMGraphicList();

        // Set the line limits for the lat/lon lines...
        int north = (int)Math.ceil(up);
        if (north > 80) north = 80;

        int south = (int)Math.floor(down);
        south -= (south%10); // Push down to the lowest 10 degree line.
        // for neg numbers, Mod raised it, lower it again
        if ((south < 0 && south > -70) || south == 0) {
            south -= 10; 
        }

        int west = (int)Math.floor(left);
        west -= (west%10);
        // for neg numbers, Mod raised it, lower it again
        if ((west < 0 && west > -170) || west == 0) {
            west -= 10;
        }

        int east = (int)Math.ceil(right);
        if (east > 180) east = 180;

        int stepSize = 10;
        OMText currentText;

        // For calculating text locations
        java.awt.Point point;
        LatLonPoint llpoint;
        Projection projection = getProjection();

        if (doLats) {

            // generate other parallels of latitude be creating series
            // of labels
            for (int i = south; i < north; i += stepSize) {
                float lat = (float)i;
                
                if ((lat%2) == 0) {
                    if (boxy) {
                        point = projection.forward(lat, west);
                        point.x = 0;
                        llpoint = projection.inverse(point);
                    } else {
                        llpoint = new LatLonPoint(lat, west);
                        while (projection.forward(llpoint).x < 0) {
                            llpoint.setLongitude(llpoint.getLongitude() + stepSize);
                        }
                    }
                    
                    currentText = new OMText(llpoint.getLatitude(), llpoint.getLongitude(),
                                             (int)2, (int) -2, // Move them up a little
                                             Integer.toString((int)lat),
                                             font, OMText.JUSTIFY_LEFT);
                    currentText.setLinePaint(textColor);
                    labels.addOMGraphic(currentText);
                }
            }
        }
        
        // generate labels of longitude
        for (int i = west; i < east; i += stepSize) {
            float lon = (float)i;
            
            if ((lon%2) == 0) {
                if (boxy) {
                    point = projection.forward(south, lon);
                    point.y = projection.getHeight();
                    llpoint = projection.inverse(point);
                } else {
                    llpoint = new LatLonPoint(south, lon);
                    while (projection.forward(llpoint).y > projection.getHeight()) {
                        llpoint.setLatitude(llpoint.getLatitude() + stepSize);
                    }
                }
                
                currentText = new OMText(llpoint.getLatitude(), 
                                         llpoint.getLongitude(),
                                         // Move them up a little
                                         (int)2, (int) -5,
                                         Integer.toString((int)lon),
                                         font, OMText.JUSTIFY_CENTER);
                currentText.setLinePaint(textColor);
                labels.addOMGraphic(currentText);

            }
        }

        if (Debug.debugging("graticule")) {
            Debug.output("GraticuleLayer.constructTensLabels(): " +
                    "constructed " + labels.size() + " graticule labels");
        }
        labels.generate(projection);
        return labels;
    }

    /** Constructs the Dateline and Prime Meridian lines. */
    protected OMGraphicList constructMarkerLines() {

        OMGraphicList lines = new OMGraphicList(3);
        OMPoly currentLine;

        // generate Prime Meridian and Dateline
        for (int j = 0; j < 360; j += 180) {
            float lon = (float)j;
            float[] llp = {90f, lon,
                           0f,  lon,
                           -90f, lon};
            currentLine = new OMPoly(llp, OMGraphic.DECIMAL_DEGREES,
                                     boxy ? OMGraphic.LINETYPE_STRAIGHT
                                     : OMGraphic.LINETYPE_GREATCIRCLE);
            currentLine.setLinePaint(dateLineColor);
            lines.addOMGraphic(currentLine);
        }

        // equator
        float[] llp = {0f, -180f,
                       0f, -90f,
                       0f, 0f,
                       0f, 90f,
                       0f, 180f};
        // polyline
        currentLine = new OMPoly(llp, OMGraphic.DECIMAL_DEGREES,
                                 boxy ? OMGraphic.LINETYPE_STRAIGHT
                                 : OMGraphic.LINETYPE_GREATCIRCLE);
        currentLine.setLinePaint(equatorColor);
        lines.addOMGraphic(currentLine);

        if (Debug.debugging("graticule")) {
            Debug.output("GraticuleLayer.constructMarkerLines(): " +
                    "constructed " + lines.size() + " graticule lines");
        }
        lines.generate(getProjection());
        return lines;
    }

    /** 
     * Take a graphic list, and set all the items on the list to the
     * line type specified, and project them into the current
     * projection.
     * 
     * @param list the list containing the lines to change.
     * @param lineType the line type to cahnge the lines to.  */
    protected void setLineTypeAndProject(OMGraphicList list, int lineType) {
        int size = list.size();
        OMGraphic graphic;
        for (int i = 0; i < size; i++) {
            graphic = list.getOMGraphicAt(i);
            graphic.setLineType(lineType);
            graphic.generate(getProjection());
        }
    }


    //----------------------------------------------------------------------
    // GUI
    //----------------------------------------------------------------------

    /** The user interface palette for the DTED layer. */
    protected Box palette = null;

    /** Creates the interface palette. */
    public java.awt.Component getGUI() {

        if (palette == null) {
            if (Debug.debugging("graticule"))
                Debug.output("GraticuleLayer: creating Graticule Palette.");

            palette = Box.createVerticalBox();

            JPanel layerPanel = PaletteHelper.createPaletteJPanel("Graticule Layer Options");
            
            ActionListener al = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String ac = e.getActionCommand();
                    
                    if (ac.equalsIgnoreCase(ShowRulerProperty)) {
                        JCheckBox jcb = (JCheckBox)e.getSource();
                        showRuler = jcb.isSelected();
                    } else if (ac.equalsIgnoreCase(ShowOneAndFiveProperty)) {
                        JCheckBox jcb = (JCheckBox)e.getSource();
                        showOneAndFiveLines = jcb.isSelected();
                    } else {
                        Debug.error("Unknown action command \"" + ac +
                                           "\" in GraticuleLayer.actionPerformed().");
                    }
                }
            };

            showRulerButton = new JCheckBox("Show Lat/Lon Labels", 
                                            showRuler);
            showRulerButton.addActionListener(al);
            showRulerButton.setActionCommand(ShowRulerProperty);

            show15Button = new JCheckBox("Show 1, 5 Degree Lines", 
                                         showOneAndFiveLines);
            show15Button.addActionListener(al);
            show15Button.setActionCommand(ShowOneAndFiveProperty);


            layerPanel.add(showRulerButton);
            layerPanel.add(show15Button);
            palette.add(layerPanel);
            
            JPanel subbox3 = new JPanel(new GridLayout(0, 1));
            
            JButton setProperties = new JButton("Preferences");
            setProperties.setActionCommand(DisplayPropertiesCmd);
            setProperties.addActionListener(this);
            subbox3.add(setProperties);

            JButton redraw = new JButton("Redraw Graticule Layer");
            redraw.setActionCommand(RedrawCmd);
            redraw.addActionListener(this);
            subbox3.add(redraw);
            palette.add(subbox3);
        }
        return palette;
    }

    //----------------------------------------------------------------------
    // ActionListener interface implementation
    //----------------------------------------------------------------------
    
    /**
     * Used just for the redraw button.
     */
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        String command = e.getActionCommand();

        if (command == RedrawCmd) {
            //redrawbutton
            doPrepare();
        }
    }

}
