/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.container.interceptor;

import java.util.Map;

import org.jboss.ejb.client.EJBClientInterceptor;
import org.jboss.ejb.client.EJBClientInvocationContext;

/**
 * An EJB client side interceptor. The entries in a {@link Map} provided to the constructor are copied to the
 * {@link EJBClientInvocationContext#getContextData()} in the {@link #handleInvocation(EJBClientInvocationContext)} before
 * {@link EJBClientInvocationContext#sendRequest()} is called
 *
 * @author Jaikiran Pai
 */
public class SimpleEJBClientInterceptor implements EJBClientInterceptor {

    private final Map<String, Object> data;

    SimpleEJBClientInterceptor(final Map<String, Object> data) {
        this.data = data;
    }

    @Override
    public void handleInvocation(EJBClientInvocationContext context) throws Exception {
        // add all the data to the EJB client invocation context so that it becomes available to the server side
        context.getContextData().putAll(data);
        // proceed "down" the invocation chain
        context.sendRequest();
    }

    @Override
    public Object handleInvocationResult(EJBClientInvocationContext context) throws Exception {
        // we don't have anything special to do with the result so just return back the result
        // "up" the invocation chain
        return context.getResult();
    }
}
