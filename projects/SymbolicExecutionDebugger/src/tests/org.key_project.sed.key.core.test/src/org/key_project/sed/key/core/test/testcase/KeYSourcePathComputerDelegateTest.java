package org.key_project.sed.key.core.test.testcase;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ArchiveSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.DirectorySourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ExternalArchiveSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.FolderSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ProjectSourceContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.junit.Test;
import org.key_project.key4eclipse.starter.core.property.KeYClassPathEntry;
import org.key_project.key4eclipse.starter.core.property.KeYClassPathEntry.KeYClassPathEntryKind;
import org.key_project.key4eclipse.starter.core.property.KeYResourceProperties;
import org.key_project.key4eclipse.starter.core.property.KeYResourceProperties.UseBootClassPathKind;
import org.key_project.sed.key.core.launch.KeYSourcePathComputerDelegate;
import org.key_project.sed.key.core.test.Activator;
import org.key_project.sed.key.core.util.KeySEDUtil;
import org.key_project.util.eclipse.BundleUtil;
import org.key_project.util.test.util.TestUtilsUtil;

/**
 * Tests for {@link KeYSourcePathComputerDelegate}.
 * @author Martin Hentschel
 */
public class KeYSourcePathComputerDelegateTest extends TestCase {
   /**
    * Tests the computed {@link ISourceContainer} by a {@link KeYSourcePathComputerDelegate}
    * of an {@link IMethod} contained in an {@link IProject} without KeY specific
    * class path entries property.
    * @throws CoreException Occurred Exception.
    * @throws InterruptedException Occurred Exception.
    * @throws IOException 
    */
   @Test
   public void testConfigurationWithKeYSpecificClassPathEntries() throws CoreException, InterruptedException, IOException {
      File tempFile = null;
      try {
         // Create test project
         IJavaProject project = TestUtilsUtil.createJavaProject("KeYSourcePathComputerDelegateTest_testConfigurationWithKeYSpecificClassPathEntries");
         BundleUtil.extractFromBundleToWorkspace(Activator.PLUGIN_ID, "data/statements", project.getProject().getFolder("src"));
         // Get method
         IMethod method = TestUtilsUtil.getJdtMethod(project, "FlatSteps", "doSomething", "I", "QString;", "Z");
         // Create configuration
         ILaunchConfiguration configuration = KeySEDUtil.createConfiguration(method);
         // Compute and test container
         KeYSourcePathComputerDelegate computer = new KeYSourcePathComputerDelegate();
         ISourceContainer[] result = computer.computeSourceContainers(configuration, new NullProgressMonitor());
         assertSourceContainer(result, new ProjectSourceContainer(project.getProject(), true));
         // Set class path entries
         tempFile = File.createTempFile("KeYSourcePathComputerDelegateTest", ".jar");
         IFile workspaceFile = (IFile)method.getResource();
         List<KeYClassPathEntry> entries = new LinkedList<KeYClassPathEntry>();
         entries.add(new KeYClassPathEntry(KeYClassPathEntryKind.EXTERNAL_IN_FILE_SYSTEM, tempFile.getAbsolutePath())); // External file
         entries.add(new KeYClassPathEntry(KeYClassPathEntryKind.EXTERNAL_IN_FILE_SYSTEM, tempFile.getParent())); // External folder
         entries.add(new KeYClassPathEntry(KeYClassPathEntryKind.WORKSPACE, workspaceFile.getFullPath().toString())); // Workspace file
         entries.add(new KeYClassPathEntry(KeYClassPathEntryKind.WORKSPACE, workspaceFile.getParent().getFullPath().toString())); // Workspace folder
         entries.add(new KeYClassPathEntry(KeYClassPathEntryKind.WORKSPACE, project.getProject().getFullPath().toString())); // Workspace project
         KeYResourceProperties.setClassPathEntries(project.getProject(), entries);
         // Compute and test container
         result = computer.computeSourceContainers(configuration, new NullProgressMonitor());
         assertSourceContainer(result, 
                               new ExternalArchiveSourceContainer(tempFile.getAbsolutePath(), true),
                               new DirectorySourceContainer(tempFile.getParentFile(), true), 
                               new ArchiveSourceContainer(workspaceFile, true),
                               new FolderSourceContainer(workspaceFile.getParent(), true),
                               new ProjectSourceContainer(project.getProject(), true),
                               new ProjectSourceContainer(project.getProject(), true));
      }
      finally {
         if (tempFile != null) {
            tempFile.delete();
         }
      }
   }
   
   /**
    * Tests the computed {@link ISourceContainer} by a {@link KeYSourcePathComputerDelegate}
    * of an {@link IMethod} contained in an {@link IProject} without KeY specific boot
    * class path property.
    * @throws CoreException Occurred Exception.
    * @throws InterruptedException Occurred Exception.
    * @throws IOException 
    */
   @Test
   public void testConfigurationWithKeYSpecificBootClassPath() throws CoreException, InterruptedException, IOException {
      File tempFile = null;
      try {
         // Create test project
         IJavaProject project = TestUtilsUtil.createJavaProject("KeYSourcePathComputerDelegateTest_testConfigurationWithKeYSpecificProperties");
         BundleUtil.extractFromBundleToWorkspace(Activator.PLUGIN_ID, "data/statements", project.getProject().getFolder("src"));
         // Get method
         IMethod method = TestUtilsUtil.getJdtMethod(project, "FlatSteps", "doSomething", "I", "QString;", "Z");
         // Create configuration
         ILaunchConfiguration configuration = KeySEDUtil.createConfiguration(method);
         // Compute and test container
         KeYSourcePathComputerDelegate computer = new KeYSourcePathComputerDelegate();
         ISourceContainer[] result = computer.computeSourceContainers(configuration, new NullProgressMonitor());
         assertSourceContainer(result, new ProjectSourceContainer(project.getProject(), true));
         // Set boot class path to external file
         tempFile = File.createTempFile("KeYSourcePathComputerDelegateTest", ".jar");
         KeYResourceProperties.setBootClassPath(project.getProject(), tempFile.getAbsolutePath());
         KeYResourceProperties.setUseBootClassPathKind(project.getProject(), UseBootClassPathKind.EXTERNAL_IN_FILE_SYSTEM);
         // Compute and test container
         result = computer.computeSourceContainers(configuration, new NullProgressMonitor());
         assertSourceContainer(result, 
                               new ExternalArchiveSourceContainer(tempFile.getAbsolutePath(), true), 
                               new ProjectSourceContainer(project.getProject(), true));
         // Set boot class path to external folder
         KeYResourceProperties.setBootClassPath(project.getProject(), tempFile.getParent());
         KeYResourceProperties.setUseBootClassPathKind(project.getProject(), UseBootClassPathKind.EXTERNAL_IN_FILE_SYSTEM);
         // Compute and test container
         result = computer.computeSourceContainers(configuration, new NullProgressMonitor());
         assertSourceContainer(result, 
                               new DirectorySourceContainer(tempFile.getParentFile(), true), 
                               new ProjectSourceContainer(project.getProject(), true));
         // Set boot class path to workspace file
         IFile workspaceFile = (IFile)method.getResource();
         KeYResourceProperties.setBootClassPath(project.getProject(), workspaceFile.getFullPath().toString());
         KeYResourceProperties.setUseBootClassPathKind(project.getProject(), UseBootClassPathKind.WORKSPACE);
         // Compute and test container
         result = computer.computeSourceContainers(configuration, new NullProgressMonitor());
         assertSourceContainer(result, 
                               new ArchiveSourceContainer(workspaceFile, true), 
                               new ProjectSourceContainer(project.getProject(), true));
         // Set boot class path to workspace folder
         IFolder workspaceFolder = (IFolder)workspaceFile.getParent();
         KeYResourceProperties.setBootClassPath(project.getProject(), workspaceFolder.getFullPath().toString());
         KeYResourceProperties.setUseBootClassPathKind(project.getProject(), UseBootClassPathKind.WORKSPACE);
         // Compute and test container
         result = computer.computeSourceContainers(configuration, new NullProgressMonitor());
         assertSourceContainer(result, 
                               new FolderSourceContainer(workspaceFolder, true), 
                               new ProjectSourceContainer(project.getProject(), true));
         // Set boot class path to workspace project
         KeYResourceProperties.setBootClassPath(project.getProject(), project.getProject().getFullPath().toString());
         KeYResourceProperties.setUseBootClassPathKind(project.getProject(), UseBootClassPathKind.WORKSPACE);
         // Compute and test container
         result = computer.computeSourceContainers(configuration, new NullProgressMonitor());
         assertSourceContainer(result, 
                               new ProjectSourceContainer(project.getProject(), true), 
                               new ProjectSourceContainer(project.getProject(), true));
      }
      finally {
         if (tempFile != null) {
            tempFile.delete();
         }
      }
   }
   
   /**
    * Tests the computed {@link ISourceContainer} by a {@link KeYSourcePathComputerDelegate}
    * of an {@link IMethod} contained in an {@link IProject} without KeY specific properties.
    * @throws CoreException Occurred Exception.
    * @throws InterruptedException Occurred Exception.
    */
   @Test
   public void testConfigurationWithoutKeYSpecificProperties() throws CoreException, InterruptedException {
      // Create test project
      IJavaProject project = TestUtilsUtil.createJavaProject("KeYSourcePathComputerDelegateTest_testConfigurationWithoutKeYSpecificProperties");
      BundleUtil.extractFromBundleToWorkspace(Activator.PLUGIN_ID, "data/statements", project.getProject().getFolder("src"));
      // Get method
      IMethod method = TestUtilsUtil.getJdtMethod(project, "FlatSteps", "doSomething", "I", "QString;", "Z");
      // Create configuration
      ILaunchConfiguration configuration = KeySEDUtil.createConfiguration(method);
      // Compute and test container
      KeYSourcePathComputerDelegate computer = new KeYSourcePathComputerDelegate();
      ISourceContainer[] result = computer.computeSourceContainers(configuration, new NullProgressMonitor());
      assertSourceContainer(result, new ProjectSourceContainer(project.getProject(), true));
   }
   
   /**
    * Makes sure that the expected and the current {@link ISourceContainer} are equal.
    * @param current The current {@link ISourceContainer}.
    * @param expected The expected {@link ISourceContainer}.
    */
   protected void assertSourceContainer(ISourceContainer[] current, ISourceContainer... expected) {
      TestCase.assertEquals(expected.length, current.length);
      for (int i = 0; i < expected.length; i++) {
         if (expected[i] instanceof ProjectSourceContainer) {
            assertTrue(current[i] instanceof ProjectSourceContainer);
            ProjectSourceContainer expectedContainer = (ProjectSourceContainer)expected[i];
            ProjectSourceContainer currentContainer = (ProjectSourceContainer)current[i];
            assertEquals(expectedContainer.getProject(), currentContainer.getProject());
            assertEquals(expectedContainer.isSearchReferencedProjects(), currentContainer.isSearchReferencedProjects());
         }
         else if (expected[i] instanceof ExternalArchiveSourceContainer) {
            assertTrue(current[i] instanceof ExternalArchiveSourceContainer);
            ExternalArchiveSourceContainer expectedContainer = (ExternalArchiveSourceContainer)expected[i];
            ExternalArchiveSourceContainer currentContainer = (ExternalArchiveSourceContainer)current[i];
            assertEquals(expectedContainer.getName(), currentContainer.getName());
            assertEquals(expectedContainer.isDetectRoot(), currentContainer.isDetectRoot());
         }
         else if (expected[i] instanceof DirectorySourceContainer) {
            assertTrue(current[i] instanceof DirectorySourceContainer);
            DirectorySourceContainer expectedContainer = (DirectorySourceContainer)expected[i];
            DirectorySourceContainer currentContainer = (DirectorySourceContainer)current[i];
            assertEquals(expectedContainer.getName(), currentContainer.getName());
            assertEquals(expectedContainer.isComposite(), currentContainer.isComposite());
         }
         else if (expected[i] instanceof ArchiveSourceContainer) {
            assertTrue(current[i] instanceof ArchiveSourceContainer);
            ArchiveSourceContainer expectedContainer = (ArchiveSourceContainer)expected[i];
            ArchiveSourceContainer currentContainer = (ArchiveSourceContainer)current[i];
            assertEquals(expectedContainer.getFile(), currentContainer.getFile());
            assertEquals(expectedContainer.isDetectRoot(), currentContainer.isDetectRoot());
         }
         else if (expected[i] instanceof FolderSourceContainer) {
            assertTrue(current[i] instanceof FolderSourceContainer);
            FolderSourceContainer expectedContainer = (FolderSourceContainer)expected[i];
            FolderSourceContainer currentContainer = (FolderSourceContainer)current[i];
            assertEquals(expectedContainer.getContainer(), currentContainer.getContainer());
            assertEquals(expectedContainer.isComposite(), currentContainer.isComposite());
         }
         else {
            fail("Unsupported ISourceContainer: " + expected[i]);
         }
      }
   }
}