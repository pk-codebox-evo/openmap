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
// $Source: /cvs/distapps/openmap/src/openmap/com/bbn/openmap/gui/menu/SaveAsGifMenuItem.java,v $
// $RCSfile: SaveAsGifMenuItem.java,v $
// $Revision: 1.2 $
// $Date: 2004/01/26 18:18:08 $
// $Author: dietrick $
// 
// **********************************************************************


package com.bbn.openmap.gui.menu;

import com.bbn.openmap.image.AcmeGifFormatter;

/**
 * A JMenuItem that uses the MapHandler to find the MapBean and save
 * an image of it in the GIF format. 
 */
public class SaveAsGifMenuItem extends SaveAsImageMenuItem {
    public SaveAsGifMenuItem() {
        super("GIF", new AcmeGifFormatter());
    }
}
