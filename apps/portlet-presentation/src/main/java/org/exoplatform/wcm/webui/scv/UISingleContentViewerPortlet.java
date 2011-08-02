/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wcm.webui.scv;

import java.util.Collection;
import java.util.Date;

import javax.jcr.Node;
import javax.portlet.MimeResponse;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceURL;

import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserNodeFilterConfig;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.WCMService;
import org.exoplatform.services.wcm.navigation.NavigationUtils;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Element;

/**
 * Created by The eXo Platform SAS
 * Author : DANG TAN DUNG
 * dzungdev@gmail.com
 * Jun 9, 2008
 */
@ComponentConfig(
  lifecycle = UIApplicationLifecycle.class,
  template = "app:/groovy/SingleContentViewer/UISingleContentView.gtmpl"
)

public class UISingleContentViewerPortlet extends UIPortletApplication {

  /** The REPOSITORY. */
  public static String REPOSITORY     = "repository" ;

  /** The WORKSPACE. */
  public static String WORKSPACE      = "workspace" ;

  /** The IDENTIFIER. */
  public static String IDENTIFIER     = "nodeIdentifier" ;

  /** The DRIVE. */
  public static String DRIVE          = "nodeDrive";

  /** The Parameterized String **/
  public static String PARAMETER      = "ParameterName";

  /** The ShowDate **/
  public static String SHOW_DATE      = "ShowDate";

  /** The ShowTitle **/
  public static String SHOW_TITLE     = "ShowTitle";

  /** The ShowOptionBar **/
  public static String SHOW_OPTIONBAR = "ShowOptionBar";

  /** The is ContextualMode **/
  public static String CONTEXTUAL_MODE= "ContextEnable";

  /** The Parameterized String for printing**/
  public static String PRINT_PARAMETER= "PrintParameterName";

  /** The Page that show the print viewer **/
  public static String PRINT_PAGE     = "PrintPage";
  /** The mode_. */

  /** The Constant PREFERENCE_TARGET_PAGE. */
  public final static String  PREFERENCE_TARGET_PAGE                = "basePath";

  /** The Constant PREFERENCE_SHOW_SCL_WITH. */
  public final static String PREFERENCE_SHOW_SCV_WITH               = "showScvWith";

  public static final String DEFAULT_SHOW_SCV_WITH                  = "content-id";
  
  /** The Cache */
  public static final String ENABLE_CACHE = "sharedCache";

  private PortletMode mode = null;//PortletMode.VIEW ;

  public static final String UIPreferencesPopupID = "UIPreferencesPopupWindows";

  private UISCVPreferences popPreferences;
  private UIPresentationContainer uiPresentation;
  PortletPreferences preferences;
  private static final Log log = ExoLogger.getLogger(UISingleContentViewerPortlet.class);

  /**
   * Instantiates a new uI single content viewer portlet.
   *
   * @throws Exception the exception
   */
  public UISingleContentViewerPortlet() throws Exception {
    addChild(UIPopupContainer.class, null, "UIPopupContainer-" + new Date().getTime());
    PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
    preferences = portletRequestContext.getRequest().getPreferences();
    //popPreferences = addChild(UISCVPreferences.class, null, null).setRendered(false);
    //uiPresentation = addChild(UIPresentationContainer.class, null, null);
  }

  /**
   * Activate mode.
   *
   * @param newMode the mode
   *
   * @throws Exception the exception
   */
  public void activateMode(PortletMode newMode) throws Exception{
    if (getChild(UIPresentationContainer.class) !=null) {
      removeChild(UIPresentationContainer.class);
    }
    if (getChild(UISCVPreferences.class) != null) {
      removeChild(UISCVPreferences.class);
    }
    if(PortletMode.VIEW.equals(newMode)) {
      uiPresentation = addChild(UIPresentationContainer.class, null, null);
    } else if (PortletMode.EDIT.equals(newMode)) {
      popPreferences = addChild(UISCVPreferences.class, null, null);
      popPreferences.setInternalPreferencesMode(true);
    }
  }
  public boolean isViewMode() {
//    System.out.println(Utils.getCurrentMode());
    return Utils.getCurrentMode().equals(PortletMode.VIEW);

  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.webui.core.UIPortletApplication#processRender(org.exoplatform
   * .webui.application.WebuiApplication,
   * org.exoplatform.webui.application.WebuiRequestContext)
   */
  public void processRender(WebuiApplication app, WebuiRequestContext context) throws Exception {
    PortletRequestContext pContext = (PortletRequestContext) context ;
    PortletMode newMode = pContext.getApplicationMode() ;
    PortletPreferences preferences = pContext.getRequest().getPreferences();
    Boolean sharedCache = "true".equals(preferences.getValue(ENABLE_CACHE, "true"));

    if (context.getRemoteUser()==null ||
          (!"Edit".equals(Utils.getCurrentMode()) && sharedCache)) {
      WCMService wcmService = getApplicationComponent(WCMService.class);
      pContext.getResponse().setProperty(MimeResponse.EXPIRATION_CACHE, ""+wcmService.getPortletExpirationCache());
      if (log.isTraceEnabled())
        log.trace("SCV rendering : cache set to "+wcmService.getPortletExpirationCache());
    }

    if(!newMode.equals(mode)) {
      activateMode(newMode) ;
      mode = newMode ;
    }

    Node nodeView = null;
    if (uiPresentation!=null) {
      nodeView = uiPresentation.getNodeView();
      if (nodeView != null) {
        TemplateService templateService = getApplicationComponent(TemplateService.class);
        uiPresentation.getChild(UIPresentation.class).setTemplatePath(templateService.getTemplatePath(nodeView, false));
      }
    }
    
    if (uiPresentation!=null && uiPresentation.isContextual() && nodeView!=null) {
      RenderResponse response = context.getResponse();
      Element title = response.createElement("title");
      title.setTextContent(uiPresentation.getTitle(nodeView));
      response.addProperty(MimeResponse.MARKUP_HEAD_ELEMENT, title);
    }

    super.processRender(app, context) ;
  }

  public void changeToViewMode() throws Exception{
      PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
      portletRequestContext.setApplicationMode(PortletMode.VIEW);
  }
  public Node getNodeByPreference() {
    try {
      //String repository = preferences.getValue(REPOSITORY, null);
      String workspace = preferences.getValue(WORKSPACE, null);
      String nodeIdentifier = preferences.getValue(IDENTIFIER, null) ;
      WCMService wcmService = getApplicationComponent(WCMService.class);
      return wcmService.getReferencedContent(WCMCoreUtils.getUserSessionProvider(), workspace, nodeIdentifier);
    } catch (Exception e) {
      return null;
    }
  }
  
  @Override
  public void serveResource(WebuiRequestContext context) throws Exception {
    super.serveResource(context);

    ResourceRequest req = context.getRequest();
    String nodeURI = req.getResourceID();

    JSONArray jsChilds = getChildrenAsJSON(nodeURI);
    if (jsChilds == null) {
      return;
    }

    MimeResponse res = context.getResponse();
    res.setContentType("text/json");
    res.getWriter().write(jsChilds.toString());
  }   

  public JSONArray getChildrenAsJSON(String nodeURI) throws Exception {
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
    Collection<UserNode> children = null;

    UserPortal userPortal = Util.getUIPortalApplication().getUserPortalConfig().getUserPortal();

    // make filter
    UserNodeFilterConfig.Builder filterConfigBuilder = UserNodeFilterConfig.builder();
    filterConfigBuilder.withReadWriteCheck().withVisibility(Visibility.DISPLAYED, Visibility.TEMPORAL);
    filterConfigBuilder.withTemporalCheck();
    UserNodeFilterConfig filterConfig = filterConfigBuilder.build();

    // get user node & update children
    UserNode userNode;
    if (context.getRemoteUser() != null) {
      userNode = userPortal.resolvePath(Util.getUIPortal().getUserNavigation(), filterConfig, nodeURI);
    } else {
      userNode = userPortal.resolvePath(filterConfig, nodeURI);
    }

    if (userNode != null) {
      userPortal.updateNode(userNode, NavigationUtils.ECMS_NAVIGATION_SCOPE, null);
      children = userNode.getChildren();
    }

    // build JSON result
    JSONArray jsChildren = new JSONArray();
    if (children == null) {
      return null;
    }
    MimeResponse res = context.getResponse();
    for (UserNode child : children) {
      jsChildren.put(toJSON(child, res));
    }
    return jsChildren;
  }
  
  private JSONObject toJSON(UserNode node, MimeResponse res) throws Exception {
    JSONObject json = new JSONObject();
    String nodeId = node.getId();

    json.put("label", node.getEncodedResolvedLabel());
    json.put("hasChild", node.getChildrenCount() > 0);

    UserNode selectedNode = Util.getUIPortal().getNavPath();
    json.put("isSelected", nodeId.equals(selectedNode.getId()));
    json.put("icon", node.getIcon());
    json.put("uri", node.getURI());

    ResourceURL rsURL = res.createResourceURL();
    rsURL.setResourceID(res.encodeURL(node.getURI()));
    json.put("getNodeURL", rsURL.toString());

    JSONArray jsonChildren = new JSONArray();
    for (UserNode child : node.getChildren()) {
      jsonChildren.put(toJSON(child, res));
    }
    json.put("childs", jsonChildren);
    return json;
  }
}
