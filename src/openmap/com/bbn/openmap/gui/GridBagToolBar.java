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
// $Source: /cvs/distapps/openmap/src/openmap/com/bbn/openmap/gui/GridBagToolBar.java,v $
// $RCSfile: GridBagToolBar.java,v $
// $Revision: 1.2 $
// $Date: 2004/01/26 18:18:07 $
// $Author: dietrick $
// 
// **********************************************************************

package com.bbn.openmap.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;

import com.bbn.openmap.util.Debug;

public class GridBagToolBar extends javax.swing.JToolBar {
    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();

    public GridBagToolBar() {
        setLayout(gridbag);
        c.gridy = 0;
        c.weightx = 0;
        c.anchor = GridBagConstraints.WEST;
        setFloatable(false);
        if (Debug.debugging("layout")) {
            setBorder(BorderFactory.createLineBorder(Color.blue));
        }
    }

    public Component add(Component comp) {
        gridbag.setConstraints(comp, c);
        return super.add(comp);
    }
}
