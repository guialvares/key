package org.key_project.sed.key.core.test.testcase.swtbot;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Test;
import org.key_project.sed.core.model.ISEDDebugTarget;
import org.key_project.sed.key.core.test.util.TestSEDKeyCoreUtil;
import org.key_project.sed.key.core.util.KeYSEDPreferences;
import org.key_project.util.test.util.TestUtilsUtil;

/**
 * Tests the preference "maximal number of set nodes per branch on run".
 * @author Martin Hentschel
 */
public class SWTBotMaximalNumberOfSetNodesPerBranchOnRunTest extends AbstractKeYDebugTargetTestCase {
   /**
    * Tests the resume functionality with different maximal values.
    */
   @Test
   public void testDifferentValues() throws Exception {
      IKeYDebugTargetTestExecutor executor = new IKeYDebugTargetTestExecutor() {
         @Override
         public void test(SWTWorkbenchBot bot, IJavaProject project, IMethod method, String targetName, SWTBotView debugView, SWTBotTree debugTree, ISEDDebugTarget target, ILaunch launch) throws Exception {
            int originalMaximalNumberOfSetNodesPerBranchOnRun = KeYSEDPreferences.getMaximalNumberOfSetNodesPerBranchOnRun();
            try {
               // Get debug target TreeItem
               SWTBotTreeItem item = TestSEDKeyCoreUtil.selectInDebugTree(debugTree, 0, 0, 0); // Select thread
               // Test initial debug target
               String expectedModelPathInBundle = "data/manyStatements/oracle/ManyStatements";
               String expectedModelFileExtension = ".xml";
               int modelIndex = 0;
               assertStep(target, expectedModelPathInBundle, ++modelIndex, expectedModelFileExtension);
               // Set maximal step value to 1
               setMaximalNumberOfSetNodesPerBranchOnRun(bot, 1);
               assertResume(bot, item, target, expectedModelPathInBundle, ++modelIndex, expectedModelFileExtension);
               // Set maximal step value to 2
               setMaximalNumberOfSetNodesPerBranchOnRun(bot, 2);
               assertResume(bot, item, target, expectedModelPathInBundle, ++modelIndex, expectedModelFileExtension);
               // Set maximal step value to 3
               setMaximalNumberOfSetNodesPerBranchOnRun(bot, 3);
               assertResume(bot, item, target, expectedModelPathInBundle, ++modelIndex, expectedModelFileExtension);
               // Set maximal step value to 100
               setMaximalNumberOfSetNodesPerBranchOnRun(bot, 100);
               assertResume(bot, item, target, expectedModelPathInBundle, ++modelIndex, expectedModelFileExtension);
            }
            finally {
               // Restore original value
               KeYSEDPreferences.setMaximalNumberOfSetNodesPerBranchOnRun(originalMaximalNumberOfSetNodesPerBranchOnRun);
               assertEquals(originalMaximalNumberOfSetNodesPerBranchOnRun, KeYSEDPreferences.getMaximalNumberOfSetNodesPerBranchOnRun());
            }
         }
      };
      doKeYDebugTargetTest("SWTBotMaximalNumberOfSetNodesPerBranchOnRunTest_testDifferentValues",
                           "data/manyStatements/test",
                           false,
                           createMethodSelector("ManyStatements", "main"),
                           false,
                           8,
                           executor);
   }

   /**
    * Sets the preference page value.
    * @param bot The {@link SWTWorkbenchBot} to use.
    * @param value The value to set.
    */
   protected static void setMaximalNumberOfSetNodesPerBranchOnRun(SWTWorkbenchBot bot, int value) {
      SWTBotShell preferenceShell = TestUtilsUtil.openPreferencePage(bot, "Run/Debug", "Symbolic Execution Debugger (SED)", "KeY");
      preferenceShell.bot().text(1).setText(value + "");
      preferenceShell.bot().button("OK").click();
      assertEquals(value, KeYSEDPreferences.getMaximalNumberOfSetNodesPerBranchOnRun());
   }
}