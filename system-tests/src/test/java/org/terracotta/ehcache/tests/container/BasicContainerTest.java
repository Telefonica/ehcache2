/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.container;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Layout;

import org.apache.commons.logging.LogFactory;
import org.terracotta.toolkit.Toolkit;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.tc.test.AppServerInfo;
import com.tc.test.server.appserver.deployment.AbstractStandaloneTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;

import junit.framework.Test;

public class BasicContainerTest extends AbstractStandaloneTwoServerDeploymentTest {

  private static final String CONTEXT = "BasicContainerTest";

  public static Test suite() {
    return new BasicContainerTestSetup();
  }

  public void testBasics() throws Exception {
    System.out.println("Running test");
    WebClient conversation = new WebClient();

    // do insert on server0
    WebResponse response1 = request(server0, "cmd=insert", conversation);
    assertEquals("OK", response1.getContentAsString().trim());

    // do query on server1
    response1 = request(server1, "cmd=query", conversation);
    assertEquals("OK", response1.getContentAsString().trim());
    System.out.println("Test finished");
  }

  private WebResponse request(WebApplicationServer server, String params, WebClient con) throws Exception {
    return server.ping("/" + CONTEXT + "/BasicTestServlet?" + params, con);
  }

  private static class BasicContainerTestSetup extends ContainerTestSetup {

    public BasicContainerTestSetup() {
      super(BasicContainerTest.class, "basic-cache-test.xml", CONTEXT);
    }

    @Override
    protected void configureWar(DeploymentBuilder builder) {
      super.configureWar(builder);

      builder.addDirectoryOrJARContainingClass(Toolkit.class); // toolkit-runtime

      if (appServerInfo().getId() != AppServerInfo.JBOSS) {
        builder.addDirectoryOrJARContainingClass(LoggerContext.class);
        builder.addDirectoryOrJARContainingClass(Layout.class); // logback
        builder.addDirectoryOrJARContainingClass(LogFactory.class); // common-loggings
      }

      builder.addServlet("BasicTestServlet", "/BasicTestServlet/*", BasicTestServlet.class, null, false);
    }

  }

}
