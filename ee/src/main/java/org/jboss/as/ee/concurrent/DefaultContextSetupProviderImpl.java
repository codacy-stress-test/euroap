/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent;

import org.glassfish.enterprise.concurrent.spi.ContextSetupProvider;
import org.jboss.as.ee.concurrent.handle.ResetContextHandle;
import org.jboss.as.ee.concurrent.handle.SetupContextHandle;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.concurrent.handle.NullContextHandle;

import jakarta.enterprise.concurrent.ContextService;
import java.util.Map;

/**
 * The default context setup provider.  delegates context saving/setting/resetting to the context handle factory provided by the current concurrent context.
 *
 * @author Eduardo Martins
 */
public class DefaultContextSetupProviderImpl implements ContextSetupProvider {

    @Override
    public org.glassfish.enterprise.concurrent.spi.ContextHandle saveContext(ContextService contextService) {
        return saveContext(contextService, null);
    }

    @Override
    public org.glassfish.enterprise.concurrent.spi.ContextHandle saveContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        final ConcurrentContext concurrentContext = ConcurrentContext.current();
        if (concurrentContext != null) {
            return concurrentContext.saveContext(contextService, contextObjectProperties);
        } else {
            EeLogger.ROOT_LOGGER.debug("ee concurrency context not found in invocation context");
            return new NullContextHandle();
        }
    }

    @Override
    public org.glassfish.enterprise.concurrent.spi.ContextHandle setup(org.glassfish.enterprise.concurrent.spi.ContextHandle contextHandle) throws IllegalStateException {
        return ((SetupContextHandle) contextHandle).setup();
    }

    @Override
    public void reset(org.glassfish.enterprise.concurrent.spi.ContextHandle contextHandle) {
        ((ResetContextHandle) contextHandle).reset();
    }
}
