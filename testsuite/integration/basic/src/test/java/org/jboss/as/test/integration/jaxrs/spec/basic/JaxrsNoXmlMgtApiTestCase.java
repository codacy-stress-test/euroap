/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.spec.basic;

import java.io.IOException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.jaxrs.spec.basic.resource.JaxrsApp;
import org.jboss.as.test.integration.jaxrs.spec.basic.resource.JaxrsAppResource;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Assert;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

/**
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JaxrsNoXmlMgtApiTestCase extends ContainerResourceMgmtTestBase {

    @Deployment
    public static Archive<?> deploySimpleResource() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, JaxrsNoXmlMgtApiTestCase.class.getSimpleName() + ".war");
        war.addClasses(JaxrsAppResource.class, JaxrsApp.class);
        return war;
    }


    /**
     * When no web.xml file present in archive auto-scan should fine and register
     * the resource class in the management model.
     * Confirm resource class is registered in the (CLI) management model
     * Corresponding CLI cmd:
     * ./jboss-cli.sh -c --command="/deployment=JaxrsNoXmlMgtApiTestCase.war/subsystem=jaxrs:read-resource(include-runtime=true,recursive=true)"
     *
     */
    @Test
    public void testNoXml() throws IOException, MgmtOperationException {
        ModelNode op =  Util.createOperation(READ_RESOURCE_OPERATION,
            PathAddress.pathAddress(DEPLOYMENT, JaxrsNoXmlMgtApiTestCase.class.getSimpleName() + ".war")
            .append(SUBSYSTEM, "jaxrs")
            .append("rest-resource", JaxrsAppResource.class.getCanonicalName()));
        op.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(true);

        ModelNode result = executeOperation(op);
        Assert.assertFalse("Subsystem is empty.", result.keys().size() == 0);
        ModelNode resClass = result.get("resource-class");
        Assert.assertNotNull("No resource-class present.", resClass);
        Assert.assertTrue("Expected resource-class not found.",
            resClass.toString().contains(JaxrsAppResource.class.getSimpleName()));
    }
}
