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
// $Source: /cvs/distapps/openmap/src/openmap/com/bbn/openmap/layer/rpf/RpfSubframe.java,v $
// $RCSfile: RpfSubframe.java,v $
// $Revision: 1.1.1.1 $
// $Date: 2003/02/14 21:35:48 $
// $Author: dietrick $
// 
// **********************************************************************


/*
 * Some of the ideas for this code is based on source code provided by
 * The MITRE Corporation, through the browse application source code.
 * Many thanks to Nancy Markuson who provided BBN with the software,
 * and to Theron Tock, who wrote the software, and Daniel Scholten,
 * who revised it - (c) 1994 The MITRE Corporation for those parts,
 * and used with permission.  Namely, the subframe caching mechanism
 * is the part that has been modified.
 */

package com.bbn.openmap.layer.rpf;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.awt.Color;

import com.bbn.openmap.io.*;
import com.bbn.openmap.omGraphics.OMGraphic;
import com.bbn.openmap.omGraphics.OMRasterObject;
import com.bbn.openmap.omGraphics.OMRaster;
import com.bbn.openmap.omGraphics.OMRect;
import com.bbn.openmap.omGraphics.OMScalingRaster;
import com.bbn.openmap.omGraphics.OMText;
import com.bbn.openmap.util.Debug;

/**
 * The RpfSubframe is a holder for images and attributes within the
 * cache. 
 */
public class RpfSubframe {
    /** The version of the subframe. */
    public int version;
    /** The subframe image. */
    public OMScalingRaster image;
    /** The attribute text. */
    public OMText information;
    /** The subframe outline. */
    public OMRect rectangle;
    /** The pointers to the surrounding subframes in the cache. */
    public int nextSubframe, prevSubframe;
    /** The original pixel size of RPF Subframes. */
    public final static int PIXEL_EDGE_SIZE = 256;
    /** The color model of the image. */
    protected int colorModel;
    /** The actual attribute information. */
    protected String data = "";
    /** The opaqueness of the image. */
    public int opaqueness;

    public RpfSubframe() {
	this(OMRasterObject.COLORMODEL_DIRECT);
    }

    public RpfSubframe(int colormodel) throws java.lang.OutOfMemoryError {
	init(colormodel);
    }

    protected void init(int colormodel) {
	// if nothing's changing, don't bother.
	if (image != null && colorModel == colormodel) {
	    return;
	}

	colorModel = colormodel;

	if (colorModel == OMRasterObject.COLORMODEL_DIRECT) {
	    // have to set the location and pixels later.
	    image = new OMScalingRaster(0f, 0f, 0f, 0f,
					PIXEL_EDGE_SIZE, PIXEL_EDGE_SIZE, 
					new int[PIXEL_EDGE_SIZE*PIXEL_EDGE_SIZE]);
	}
	// Have to set the location, colortable, pixel indexes later
	else {	
	    image = new OMScalingRaster(0f, 0f, 0f, 0f,
					PIXEL_EDGE_SIZE, PIXEL_EDGE_SIZE, 
					(byte[]) null, (Color[]) null, opaqueness);
	}
	information = new OMText(0f, 0f, 10, 20, "***", new java.awt.Font("Helvetica", java.awt.Font.PLAIN, 10), OMText.JUSTIFY_LEFT);
	information.setLinePaint(Color.red);
	rectangle = new OMRect(0f, 0f, 0f, 0f, OMGraphic.LINETYPE_STRAIGHT);
	rectangle.setLinePaint(Color.red);
	rectangle.setLineType(OMGraphic.LINETYPE_STRAIGHT);
    }

    public int getColorModel() { 
	return colorModel;
    }

    public void setColorModel(int colorModel) {
	init(colorModel);
    }

    public void setScalingTo(int width, int height) {
	if (width == PIXEL_EDGE_SIZE && height == PIXEL_EDGE_SIZE) {
	    // Not needed due to OMScalingRaster.
//  	    image.setImageFilter(null);
	    information.setData(data);
	} else {

	    // HACK the +1 seems to cover up well for some projection
	    // inadequacies....

	    // Not needed due to OMScalingRaster.
//  	    image.scaleTo(width+1, height+1, OMRasterObject.FAST_SCALING);

	    if (width > PIXEL_EDGE_SIZE && height > PIXEL_EDGE_SIZE) {
		information.setData(data);
	    } else {
		information.setData("");
	    }
	}
	rectangle.setLocation(image.getLat(), image.getLon(), 0, 0, width, height);
    }
    
    /**
     * Set the lat/lon of the frame and attribute text. 
     * @param lat latitude of upper left point, in decimal degrees. 
     * @param lon longitude of upper left point, in decimal degrees. 
     * @deprecated use the other setLocation, with upper left and
     * lower right coordinates.  
     */
    public void setLocation(float lat, float lon) {
	image.setLat(lat);
	image.setLon(lon);
	information.setLat(lat);
	information.setLon(lon);
	rectangle.setLocation(lat, lon, 0, 0, 
			      rectangle.getRight(), 
			      rectangle.getBottom());
    }

    /**
     * Set the lat/lon of the frame and attribute text. 
     * @param lat latitude of upper left point, in decimal degrees. 
     * @param lon longitude of upper left point, in decimal degrees. 
     */
    public void setLocation(float ulat, float wlon, float llat, float elon) {
	image.setULLat(ulat);
	image.setULLon(wlon);
	image.setLRLat(llat);
	image.setLRLon(elon);
	information.setLat(ulat);
	information.setLon(wlon);
	rectangle.setLocation(ulat, wlon, llat, elon, OMGraphic.LINETYPE_STRAIGHT);
    }

    /**
     * setScalingTo has to be called after this for the changes to
     * take place, or else you need to call the information.setData()
     * methods directly. 
     */
    public void setAttributeText(String text) {
	data = text;
    }

    /**
     * getAttributeText retrieves the text that would be displayed as
     * attribute information about the subframe.  
     */
    public String getAttributeText() {
	return data;
    }

//      public void finalize() {
//  	Debug.message("gc", "  RpfSubframe: getting GC'd");
//      }

}
