// This file is part of KeY - Integrated Deductive Software Design
//
// Copyright (C) 2001-2011 Universitaet Karlsruhe (TH), Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
// Copyright (C) 2011-2014 Karlsruhe Institute of Technology, Germany
//                         Technical University Darmstadt, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General
// Public License. See LICENSE.TXT for details.
//

package de.uka.ilkd.key.core;


/** The KeYSelectionListener is notified if the proof or the node the
 * user works with has changed.
 */
public interface KeYSelectionListener {

    /** focused node has changed */
    void selectedNodeChanged(KeYSelectionEvent e);

    /** the selected proof has changed (e.g. a new proof has been
     * loaded) */ 
    void selectedProofChanged(KeYSelectionEvent e);

}