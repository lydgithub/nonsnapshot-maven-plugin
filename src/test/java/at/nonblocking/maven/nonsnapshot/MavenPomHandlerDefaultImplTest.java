package at.nonblocking.maven.nonsnapshot;

import static junit.framework.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.IOUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.impl.StaticLoggerBinder;

import at.nonblocking.maven.nonsnapshot.impl.MavenPomHandlerDefaultImpl;
import at.nonblocking.maven.nonsnapshot.model.WorkspaceArtifact;

public class MavenPomHandlerDefaultImplTest {

    @BeforeClass
    public static void setupLog() {
        StaticLoggerBinder.getSingleton().setLog(new DebugSystemStreamLog());
    }
    
    @Test
    public void testReadArtifact() throws Exception {
        File pomFile = new File("target/test-pom.xml");
        IOUtil.copy(new FileReader("src/test/resources/test-pom.xml"), new FileOutputStream(pomFile));

        MavenPomHandler pomHandler = new MavenPomHandlerDefaultImpl();
        
        WorkspaceArtifact wsArtifact = pomHandler.readArtifact(pomFile);
        
        assertEquals("at.nonblocking", wsArtifact.getGroupId());
        assertEquals("test1", wsArtifact.getArtifactId());
        assertEquals("1.0.0-SNAPSHOT", wsArtifact.getVersion());
        assertEquals(8, wsArtifact.getVersionLocation());
        assertFalse(wsArtifact.isInsertVersionTag());
        assertNull(wsArtifact.getParent());
        
        assertEquals(6, wsArtifact.getDependencies().size());
        
        assertEquals(14, wsArtifact.getDependencies().get(0).getVersionLocation());
        assertEquals(20, wsArtifact.getDependencies().get(1).getVersionLocation());
        assertEquals(26, wsArtifact.getDependencies().get(2).getVersionLocation());
        assertEquals(35, wsArtifact.getDependencies().get(3).getVersionLocation());
        assertEquals(40, wsArtifact.getDependencies().get(4).getVersionLocation());
        assertEquals(46, wsArtifact.getDependencies().get(5).getVersionLocation());
    }
    @Test
    public void testReadArtifactWithParent() throws Exception {
        File pomFile = new File("target/test-pom-parent.xml");
        IOUtil.copy(new FileReader("src/test/resources/test-pom-parent.xml"), new FileOutputStream(pomFile));

        MavenPomHandler pomHandler = new MavenPomHandlerDefaultImpl();
        
        WorkspaceArtifact wsArtifact = pomHandler.readArtifact(pomFile);
        
        assertEquals("at.nonblocking", wsArtifact.getGroupId());
        assertEquals("test1", wsArtifact.getArtifactId());
        assertEquals("1.0.0-SNAPSHOT", wsArtifact.getVersion());
        assertEquals(12, wsArtifact.getVersionLocation());
        
        assertNotNull(wsArtifact.getParent());
        
        assertEquals(9, wsArtifact.getParentVersionLocation());
    }
    
    @Test
    public void testReadAndUpdateArtifact() throws Exception {
        File pomFile = new File("target/test-pom.xml");
        IOUtil.copy(new FileReader("src/test/resources/test-pom.xml"), new FileOutputStream(pomFile));

        MavenPomHandler pomHandler = new MavenPomHandlerDefaultImpl();
        
        WorkspaceArtifact wsArtifact = pomHandler.readArtifact(pomFile);
        
        wsArtifact.setDirty(true);
        wsArtifact.setBaseVersion("1.1.1");
        wsArtifact.setNextRevisionId("12345");
        
        WorkspaceArtifact dependentArtifact = new WorkspaceArtifact(null, "at.nonblocking.at", "test2", "2.0.5-123");
        dependentArtifact.setDirty(true);
        dependentArtifact.setBaseVersion("5.0.1");
        dependentArtifact.setNextRevisionId("555");
        
        wsArtifact.getDependencies().get(1).setArtifact(dependentArtifact);
        
        pomHandler.updateArtifact(wsArtifact, DEPENDENCY_UPDATE_STRATEGY.ALWAYS);
                
        Model pom = new MavenXpp3Reader().read(new FileInputStream(pomFile));
        
        assertEquals("1.1.1-12345", pom.getVersion());
        assertEquals("5.0.1-555", pom.getDependencies().get(1).getVersion());
    }
    
    @Test
    public void testReadAndUpdateArtifactDependecyStrategySameMajor() throws Exception {
        File pomFile = new File("target/test-pom.xml");
        IOUtil.copy(new FileReader("src/test/resources/test-pom.xml"), new FileOutputStream(pomFile));

        MavenPomHandler pomHandler = new MavenPomHandlerDefaultImpl();
        
        WorkspaceArtifact wsArtifact = pomHandler.readArtifact(pomFile);
        
        wsArtifact.setDirty(true);
        wsArtifact.setBaseVersion("1.1.1");
        wsArtifact.setNextRevisionId("12345");
        
        WorkspaceArtifact dependentArtifact1 = new WorkspaceArtifact(null, "at.nonblocking.at", "test2", "1.1.1-123");
        dependentArtifact1.setDirty(true);
        dependentArtifact1.setBaseVersion("5.0.1");
        dependentArtifact1.setNextRevisionId("555");
        
        WorkspaceArtifact dependentArtifact2 = new WorkspaceArtifact(null, "at.nonblocking.at", "test2", "2.0.5-123");
        dependentArtifact2.setDirty(true);
        dependentArtifact2.setBaseVersion("2.4.5");
        dependentArtifact2.setNextRevisionId("555");
        
        wsArtifact.getDependencies().get(0).setArtifact(dependentArtifact1);
        wsArtifact.getDependencies().get(1).setArtifact(dependentArtifact2);
        
        pomHandler.updateArtifact(wsArtifact, DEPENDENCY_UPDATE_STRATEGY.SAME_MAJOR);
                
        Model pom = new MavenXpp3Reader().read(new FileInputStream(pomFile));
        
        assertEquals("1.1.1-12345", pom.getVersion());
        assertEquals("1.1.1-123", pom.getDependencies().get(0).getVersion());
        assertEquals("2.4.5-555", pom.getDependencies().get(1).getVersion());
    }
    
    @Test
    public void testReadAndUpdateArtifactDependecyStrategySameMajorMinor() throws Exception {
        File pomFile = new File("target/test-pom.xml");
        IOUtil.copy(new FileReader("src/test/resources/test-pom.xml"), new FileOutputStream(pomFile));

        MavenPomHandler pomHandler = new MavenPomHandlerDefaultImpl();
        
        WorkspaceArtifact wsArtifact = pomHandler.readArtifact(pomFile);
        
        wsArtifact.setDirty(true);
        wsArtifact.setBaseVersion("1.1.1");
        wsArtifact.setNextRevisionId("12345");
        
        WorkspaceArtifact dependentArtifact1 = new WorkspaceArtifact(null, "at.nonblocking.at", "test2", "1.1.1-123");
        dependentArtifact1.setDirty(true);
        dependentArtifact1.setBaseVersion("1.2.2");
        dependentArtifact1.setNextRevisionId("555");
        
        WorkspaceArtifact dependentArtifact2 = new WorkspaceArtifact(null, "at.nonblocking.at", "test2", "2.0.5-123");
        dependentArtifact2.setDirty(true);
        dependentArtifact2.setBaseVersion("2.0.11");
        dependentArtifact2.setNextRevisionId("555");
        
        wsArtifact.getDependencies().get(0).setArtifact(dependentArtifact1);
        wsArtifact.getDependencies().get(1).setArtifact(dependentArtifact2);
        
        pomHandler.updateArtifact(wsArtifact, DEPENDENCY_UPDATE_STRATEGY.SAME_MAJOR_MINOR);
                
        Model pom = new MavenXpp3Reader().read(new FileInputStream(pomFile));
        
        assertEquals("1.1.1-12345", pom.getVersion());
        assertEquals("1.1.1-123", pom.getDependencies().get(0).getVersion());
        assertEquals("2.0.11-555", pom.getDependencies().get(1).getVersion());
    }
    
    @Test
    public void testReadAndUpdateArtifactDependecyStrategySameBaseVersion() throws Exception {
        File pomFile = new File("target/test-pom.xml");
        IOUtil.copy(new FileReader("src/test/resources/test-pom.xml"), new FileOutputStream(pomFile));

        MavenPomHandler pomHandler = new MavenPomHandlerDefaultImpl();
        
        WorkspaceArtifact wsArtifact = pomHandler.readArtifact(pomFile);
        
        wsArtifact.setDirty(true);
        wsArtifact.setBaseVersion("1.1.1");
        wsArtifact.setNextRevisionId("12345");
        
        WorkspaceArtifact dependentArtifact1 = new WorkspaceArtifact(null, "at.nonblocking.at", "test2", "1.1.1-123");
        dependentArtifact1.setDirty(true);
        dependentArtifact1.setBaseVersion("1.1.2");
        dependentArtifact1.setNextRevisionId("555");
        
        WorkspaceArtifact dependentArtifact2 = new WorkspaceArtifact(null, "at.nonblocking.at", "test2", "2.0.5-123");
        dependentArtifact2.setDirty(true);
        dependentArtifact2.setBaseVersion("2.0.5");
        dependentArtifact2.setNextRevisionId("555");
        
        wsArtifact.getDependencies().get(0).setArtifact(dependentArtifact1);
        wsArtifact.getDependencies().get(1).setArtifact(dependentArtifact2);
        
        pomHandler.updateArtifact(wsArtifact, DEPENDENCY_UPDATE_STRATEGY.SAME_BASE_VERSION);
                
        Model pom = new MavenXpp3Reader().read(new FileInputStream(pomFile));
        
        assertEquals("1.1.1-12345", pom.getVersion());
        assertEquals("1.1.1-123", pom.getDependencies().get(0).getVersion());
        assertEquals("2.0.5-555", pom.getDependencies().get(1).getVersion());
    }
    
    @Test
    public void testReadAndUpdateArtifactWithParent() throws Exception {
        File pomFile = new File("target/test-pom-parent.xml");
        IOUtil.copy(new FileReader("src/test/resources/test-pom-parent.xml"), new FileOutputStream(pomFile));

        MavenPomHandler pomHandler = new MavenPomHandlerDefaultImpl();
        
        WorkspaceArtifact wsArtifact = pomHandler.readArtifact(pomFile);
        
        wsArtifact.setDirty(true);
        wsArtifact.setBaseVersion("1.1.1");
        wsArtifact.setNextRevisionId("12345");
        
        WorkspaceArtifact parentArtifact = new WorkspaceArtifact(null, "at.nonblocking.at", "parent-test", "1.4.5-123");
        parentArtifact.setDirty(true);
        parentArtifact.setBaseVersion("3.3.3");
        parentArtifact.setNextRevisionId("456");
        
        wsArtifact.setParent(parentArtifact);
        
        pomHandler.updateArtifact(wsArtifact, DEPENDENCY_UPDATE_STRATEGY.ALWAYS);
                
        Model pom = new MavenXpp3Reader().read(new FileInputStream(pomFile));
        
        assertEquals("1.1.1-12345", pom.getVersion());
        assertEquals("3.3.3-456", pom.getParent().getVersion());
    }
    
    @Test
    public void testReadAndUpdateArtifactWithNoVersion() throws Exception {
        File pomFile = new File("target/test-pom-noversion.xml");
        IOUtil.copy(new FileReader("src/test/resources/test-pom-noversion.xml"), new FileOutputStream(pomFile));

        MavenPomHandler pomHandler = new MavenPomHandlerDefaultImpl();
        
        WorkspaceArtifact wsArtifact = pomHandler.readArtifact(pomFile);
        
        assertNotNull(wsArtifact.getVersion());
        assertTrue(wsArtifact.isInsertVersionTag());
        
        wsArtifact.setDirty(true);
        wsArtifact.setBaseVersion("1.1.1");
        wsArtifact.setNextRevisionId("12345");
              
        pomHandler.updateArtifact(wsArtifact, DEPENDENCY_UPDATE_STRATEGY.ALWAYS);
                
        Model pom = new MavenXpp3Reader().read(new FileInputStream(pomFile));
        
        assertEquals("1.1.1-12345", pom.getVersion());
    }
    
    @Test
    public void testReadAndUpdateArtifactInsertVersionTag() throws Exception {
        File pomFile = new File("target/test-pom.xml");
        IOUtil.copy(new FileReader("src/test/resources/test-pom.xml"), new FileOutputStream(pomFile));

        MavenPomHandler pomHandler = new MavenPomHandlerDefaultImpl();
        
        WorkspaceArtifact wsArtifact = pomHandler.readArtifact(pomFile);
        
        wsArtifact.setDirty(true);
        wsArtifact.setBaseVersion("1.1.1");
        wsArtifact.setNextRevisionId("12345");
        
        WorkspaceArtifact dependentArtifact = new WorkspaceArtifact(null, "at.nonblocking.at", "test2", "2.0.5-123");
        dependentArtifact.setDirty(true);
        dependentArtifact.setBaseVersion("5.0.1");
        dependentArtifact.setNextRevisionId("555");
        dependentArtifact.setInsertVersionTag(true);
        
        wsArtifact.getDependencies().get(1).setArtifact(dependentArtifact);
        
        pomHandler.updateArtifact(wsArtifact, DEPENDENCY_UPDATE_STRATEGY.ALWAYS);
                
        Model pom = new MavenXpp3Reader().read(new FileInputStream(pomFile));
        
        assertEquals("1.1.1-12345", pom.getVersion());
        assertEquals("5.0.1-555", pom.getDependencies().get(1).getVersion());
    }
}