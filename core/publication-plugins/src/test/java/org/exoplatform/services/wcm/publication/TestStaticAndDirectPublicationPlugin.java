/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.wcm.publication;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.jcr.Node;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.ecms.test.BaseECMSTestCase;
import org.exoplatform.services.ecm.publication.IncorrectStateUpdateLifecycleException;
import org.exoplatform.services.ecm.publication.PublicationPlugin;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.ecm.publication.plugins.staticdirect.StaticAndDirectPublicationPlugin;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jul 24, 2012  
 */

@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/ecms-test-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/wcm/test-publication-configuration.xml")
  })
public class TestStaticAndDirectPublicationPlugin extends BaseECMSTestCase {
  
  private static final String CURRENT_STATE = "publication:currentState";
  private static final String TEST = "test";
  private static final String ENROLLED = "enrolled";
  private static final String PUBLISHED = "published";
  private static final String NON_PUBLISHED = "non published";  
  
  private PublicationService publicationService_;
  private PublicationPlugin plugin_;
  private Node node_;
  
  @Override
  protected void afterContainerStart() {
    super.afterContainerStart();
    publicationService_ = WCMCoreUtils.getService(PublicationService.class);
  }

  @BeforeMethod
  public void setUp() throws Exception {
    applySystemSession();
    node_ = session.getRootNode().addNode(TEST);
    session.save();
    ConversationState c = new ConversationState(new Identity(session.getUserID()));
    ConversationState.setCurrent(c);
    plugin_ = new StaticAndDirectPublicationPlugin();
    plugin_.setName("Static And Direct");
    plugin_.setDescription("Static And Direct");
    publicationService_.addPublicationPlugin(plugin_);
  }
  
  @AfterMethod
  public void tearDown() throws Exception {
    publicationService_.getPublicationPlugins().clear();
    node_.remove();
    session.save();
  }

  /**
   * tests changing state for a node 
   */
  @Test
  public void testChangeState() throws Exception {
    HashMap<String, String> context = new HashMap<String, String>();
    node_.addMixin(NodetypeConstant.MIX_VERSIONABLE);
    session.save();
    context.put("nodeVersionUUID", node_.checkin().getUUID());
    node_.checkout();
    context.put("visibility", "true");
    
    publicationService_.enrollNodeInLifecycle(node_, plugin_.getLifecycleName());
    plugin_.changeState(node_, PUBLISHED, context);
    assertEquals(PUBLISHED, node_.getProperty(CURRENT_STATE).getString());
    plugin_.changeState(node_, NON_PUBLISHED, context);
    assertEquals(NON_PUBLISHED, node_.getProperty(CURRENT_STATE).getString());
    plugin_.changeState(node_, NON_PUBLISHED, context);
    assertEquals(NON_PUBLISHED, node_.getProperty(CURRENT_STATE).getString());
    plugin_.changeState(node_, PUBLISHED, context);
    assertEquals(PUBLISHED, node_.getProperty(CURRENT_STATE).getString());
    
    context.remove("nodeVersionUUID");
    Exception e = null;
    try {
      plugin_.changeState(node_, PUBLISHED, context);
    } catch (IncorrectStateUpdateLifecycleException ex) {
      e = ex;
    }
    assertNotNull(e);
  }

  /**
  * tests getting user info
  */
  @Test
  public void testGetUserInfo() throws Exception {
    publicationService_.enrollNodeInLifecycle(node_, plugin_.getLifecycleName());
    assertNotNull(plugin_.getUserInfo(node_, new Locale("en")));
  }
  
  /**
  * tests getting state image 
  */
  @Test
  public void testGetStateImage() throws Exception {
    HashMap<String, String> context = new HashMap<String, String>();
    node_.addMixin(NodetypeConstant.MIX_VERSIONABLE);
    session.save();
    context.put("nodeVersionUUID", node_.checkin().getUUID());
    node_.checkout();
    context.put("visibility", "true");

    publicationService_.enrollNodeInLifecycle(node_, plugin_.getLifecycleName());
    plugin_.changeState(node_, PUBLISHED, context);
    assertNotNull(plugin_.getStateImage(node_, new Locale("en")));
    plugin_.changeState(node_, NON_PUBLISHED, context);
    assertNotNull(plugin_.getStateImage(node_, new Locale("en")));
  }

  /**
  * tests getting possible states 
  */
  @Test
  public void testGetPossibleStates() throws Exception {
    List<String> states = Arrays.asList(plugin_.getPossibleStates());
    assertTrue(states.contains(ENROLLED));
    assertTrue(states.contains(PUBLISHED));
    assertTrue(states.contains(NON_PUBLISHED));
  }
  
  /**
   * tests getLocalizedAndSubstituteLog
   */
  @Test
  public void testGetLocalizedAndSubstituteLog() throws Exception {
    assertEquals("Test EN", plugin_.getLocalizedAndSubstituteMessage(
                           new Locale("en"), "PublicationService.test.test", new String[]{}));
  }
  
}
