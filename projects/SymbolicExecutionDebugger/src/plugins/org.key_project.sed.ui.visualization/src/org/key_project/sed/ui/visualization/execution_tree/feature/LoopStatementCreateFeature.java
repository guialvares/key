/*******************************************************************************
 * Copyright (c) 2013 Karlsruhe Institute of Technology, Germany 
 *                    Technical University Darmstadt, Germany
 *                    Chalmers University of Technology, Sweden
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Technical University Darmstadt - initial API and implementation and/or initial documentation
 *******************************************************************************/

package org.key_project.sed.ui.visualization.execution_tree.feature;

import org.eclipse.debug.core.DebugException;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.ICreateFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.key_project.sed.core.model.ISEDDebugNode;
import org.key_project.sed.core.model.ISEDDebugTarget;
import org.key_project.sed.core.model.ISEDLoopStatement;
import org.key_project.sed.core.model.ISEDThread;
import org.key_project.sed.core.model.memory.SEDMemoryLoopStatement;
import org.key_project.sed.ui.visualization.execution_tree.provider.IExecutionTreeImageConstants;

/**
 * Implementation of {@link ICreateFeature} for {@link ISEDLoopStatement}s.
 * @author Martin Hentschel
 */
public class LoopStatementCreateFeature extends AbstractDebugNodeCreateFeature {
   /**
    * Constructor.
    * @param fp The {@link IFeatureProvider} which provides this {@link IAddFeature}.
    */
   public LoopStatementCreateFeature(IFeatureProvider fp) {
       super(fp, "Loop Statement", "Create a new Loop Statement");
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public String getCreateImageId() {
      return IExecutionTreeImageConstants.IMG_LOOP_STATEMENT;
   }
   
   /**
    * {@inheritDoc}
    */   
   @Override
   public String getNodeType() {
      return "Loop Statement";
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ISEDDebugNode createNewDebugNode(ISEDDebugTarget target,
                                              ISEDDebugNode parent,
                                              ISEDThread thread,
                                              String name) throws DebugException {
      SEDMemoryLoopStatement result = new SEDMemoryLoopStatement(target, parent, thread);
      result.setName(name);
      return result;
   }
}