package org.key_project.sed.ui.visualization.execution_tree.feature;

import org.eclipse.debug.core.DebugException;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.IUpdateFeature;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.key_project.sed.core.model.ISEDDebugNode;
import org.key_project.sed.core.model.ISEDDebugTarget;
import org.key_project.sed.core.model.ISEDThread;

/**
 * Implementation of {@link IUpdateFeature} for {@link ISEDDebugTarget}s.
 * @author Martin Hentschel
 */
public class DebugTargetUpdateFeature extends AbstractDebugNodeUpdateFeature {
   /**
    * Constructor.
    * @param fp The {@link IFeatureProvider} which provides this {@link IUpdateFeature}.
    */      
   public DebugTargetUpdateFeature(IFeatureProvider fp) {
      super(fp);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected boolean canUpdateBusinessObject(Object businessObject) {
      return businessObject instanceof ISEDDebugTarget;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected boolean isNameUpdateNeeded(PictogramElement pictogramElement) throws DebugException {
      return false; // Is never needed
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected boolean haveAllBusinessObjectChildrenHaveGraphicalRepresentation(PictogramElement pictogramElement) throws DebugException {
      Object bo = getBusinessObjectForPictogramElement(pictogramElement);
      boolean childrenHavePictogramElement = true;
      if (bo instanceof ISEDDebugTarget) {
         ISEDThread[] threads = ((ISEDDebugTarget)bo).getSymbolicThreads();
         int i = 0;
         while (childrenHavePictogramElement && i < threads.length) {
            PictogramElement threadPE = getPictogramElementForBusinessObject(threads[i]);
            childrenHavePictogramElement = threadPE != null;
            i++;
         }
      }
      return childrenHavePictogramElement;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected boolean updateName(PictogramElement pictogramElement) throws DebugException {
      return true; // Nothing to do
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected boolean updateChildren(PictogramElement pictogramElement) throws DebugException {
      Object bo = getBusinessObjectForPictogramElement(pictogramElement);
      if (bo instanceof ISEDDebugTarget) {
         ISEDThread[] threads = ((ISEDDebugTarget)bo).getSymbolicThreads();
         for (ISEDDebugNode thread : threads) {
            PictogramElement threadPE = getPictogramElementForBusinessObject(thread);
            if (threadPE == null) {
               createGraphicalRepresentationForSubtree(null, thread);
            }
         }
      }
      return true;
   }
}