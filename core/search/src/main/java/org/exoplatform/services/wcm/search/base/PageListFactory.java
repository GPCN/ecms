/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.services.wcm.search.base;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Jun 17, 2011  
 */
public class PageListFactory {

  public static <E> AbstractPageList<E> createPageList(String queryStatement,
                                                String workspace,
                                                String language,
                                                boolean isSystemSession, 
                                                NodeSearchFilter filter,
                                                SearchDataCreator<E> dataCreator,
                                                int pageSize,
                                                int bufferSize) {
    if (pageSize == 0) {
      pageSize = AbstractPageList.DEFAULT_PAGE_SIZE;
    }
    if (bufferSize < pageSize) {
      bufferSize = Math.max(pageSize, AbstractPageList.DEAFAULT_BUFFER_SIZE);
    }
    try {
      SessionProvider sessionProvider = isSystemSession ? WCMCoreUtils.getSystemSessionProvider() :
                                                          WCMCoreUtils.getUserSessionProvider();
      Session session = sessionProvider.getSession(workspace, WCMCoreUtils.getRepository());
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      Query query = queryManager.createQuery(queryStatement, language);
      QueryResult result = query.execute();
      int totalNodes = (int)result.getNodes().getSize();
      if (totalNodes <= AbstractPageList.RESULT_SIZE_SEPARATOR) {
        return new ArrayNodePageList<E>(result, pageSize, filter, dataCreator);
      } else {
        QueryData queryData = new QueryData(queryStatement, workspace, language, isSystemSession);
        QueryResultPageList<E> ret = new QueryResultPageList<E>(pageSize, queryData, totalNodes, bufferSize, filter, dataCreator);
        return ret;        
      }
    } catch (Exception e) {
      return null;
    }
  }
  
  public static <E> AbstractPageList<E> createPageList(String queryStatement,
                                                String workspace,
                                                String language,
                                                boolean isSystemSession, 
                                                NodeSearchFilter filter,
                                                SearchDataCreator<E> dataCreator) {
    return createPageList(queryStatement, workspace, language,
                          isSystemSession, filter, dataCreator,
                          AbstractPageList.DEFAULT_PAGE_SIZE, AbstractPageList.DEAFAULT_BUFFER_SIZE);
  }
  
  public static <E> AbstractPageList<E> createPageList(List<Node> nodes, int pageSize, 
                           NodeSearchFilter filter, SearchDataCreator<E> dataCreator) {

    return new ArrayNodePageList<E>(nodes, pageSize, filter, dataCreator);
  }
}