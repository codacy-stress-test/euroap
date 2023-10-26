/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.integration.ejb.interceptor.serverside;

import java.util.List;

import org.jboss.as.arquillian.container.ManagementClient;

public interface InterceptorsSetupTask {

    void packModule(InterceptorModule module) throws Exception;

    List<InterceptorModule> getModules();

    void modifyServerInterceptors(List<InterceptorModule> interceptorModules, ManagementClient managementClient) throws Exception;

    void revertServerInterceptors(ManagementClient managementClient) throws Exception;
}
