package org.key_project.util.test.testcase;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.junit.Test;
import org.key_project.util.eclipse.WorkbenchUtil;
import org.key_project.util.test.util.TestUtilsUtil;

/**
 * Contains tests for {@link WorkbenchUtil}.
 * @author Martin Hentschel
 */
public class WorkbenchUtilTest extends TestCase {
    /**
     * Tests {@link WorkbenchUtil#getActiveShell()}
     */
    @Test
    public void testGetActiveShell() {
        assertNotNull(WorkbenchUtil.getActiveShell());
    }

    /**
     * Tests {@link WorkbenchUtil#getActiveEditor()}
     */
    @Test
    public void testGetActiveEditor() throws PartInitException {
        IEditorPart editor = null;
        try {
            // Make sure that no editor is opened
            assertNull(WorkbenchUtil.getActiveEditor());
            // Create project and file
            IProject project = TestUtilsUtil.createProject("WorkbenchUtilTest_testGetActiveEditor");
            IFile file = TestUtilsUtil.createFile(project, "Test.txt", "Hello World");
            // Open editor
            editor = WorkbenchUtil.openEditor(file);
            // Make sure that editor is opened
            IEditorPart active = WorkbenchUtil.getActiveEditor();
            assertNotNull(active);
            assertEquals(editor, active);
            assertSame(active, WorkbenchUtil.getActivePart());
            // Close editor
            WorkbenchUtil.closeEditor(editor, true);
            // Make sure that no editor is opened
            assertNull(WorkbenchUtil.getActiveEditor());
            assertNotSame(active, WorkbenchUtil.getActivePart());
        }
        finally {
            WorkbenchUtil.closeEditor(editor, true);
        }
    }

    /**
     * Tests {@link WorkbenchUtil#getActivePart()}
     */
    @Test
    public void testGetActivePart() throws PartInitException {
       IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
       assertEquals(part, WorkbenchUtil.getActivePart());
    }

    /**
     * Tests {@link WorkbenchUtil#getActivePage()}
     */
    @Test
    public void testGetActivePage() {
        assertNotNull(WorkbenchUtil.getActivePage());
    }

    /**
     * Tests {@link WorkbenchUtil#getActiveWorkbenchWindow()}
     */
    @Test
    public void testGetActiveWorkbenchWindow() {
        assertNotNull(WorkbenchUtil.getActiveWorkbenchWindow());
    }
    
    /**
     * Tests {@link WorkbenchUtil#openEditor(org.eclipse.core.resources.IFile)}
     * and {@link WorkbenchUtil#closeEditor(IEditorPart, boolean)}
     */
    @Test
    public void testOpenAndCloseEditor() throws PartInitException {
        // Test opening and closing an editor.
        IProject project = TestUtilsUtil.createProject("WorkbenchUtilTest_testOpenAndCloseEditor");
        IFile file = TestUtilsUtil.createFile(project, "Test.txt", "Hello World!");
        IEditorPart editor = WorkbenchUtil.openEditor(file);
        assertNotNull(editor);
        assertNotNull(editor.getEditorInput());
        assertEquals(file, editor.getEditorInput().getAdapter(IFile.class));
        assertTrue(editor.getEditorSite().getPage().isPartVisible(editor));
        boolean closed = WorkbenchUtil.closeEditor(editor, true);
        assertTrue(closed);
        assertFalse(editor.getEditorSite().getPage().isPartVisible(editor));
        // Try to close already closed editor again
        closed = WorkbenchUtil.closeEditor(editor, true);
        assertTrue(closed);
        assertFalse(editor.getEditorSite().getPage().isPartVisible(editor));
        // Test opening null parameter
        try {
            WorkbenchUtil.openEditor(null);
            fail("Opening null should not be possible.");
        }
        catch (Exception e) {
            assertEquals("No file to open defined.", e.getMessage());
        }
        // Test closing null parameter
        closed = WorkbenchUtil.closeEditor(null, true);
        assertTrue(closed);
    }
}