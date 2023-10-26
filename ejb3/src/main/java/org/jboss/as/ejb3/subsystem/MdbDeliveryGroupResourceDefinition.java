/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for mdb delivery group.
 *
 * @author Flavia Rainone
 */
public class MdbDeliveryGroupResourceDefinition extends SimpleResourceDefinition {

    public static final String MDB_DELIVERY_GROUP_CAPABILITY_NAME = "org.wildfly.ejb3.mdb-delivery-group";
    public static final RuntimeCapability<Void> MDB_DELIVERY_GROUP_CAPABILITY =
            RuntimeCapability.Builder.of(MDB_DELIVERY_GROUP_CAPABILITY_NAME, true, Service.NULL.getClass()).build();

    public static final SimpleAttributeDefinition ACTIVE = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.MDB_DELVIERY_GROUP_ACTIVE, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(ModelNode.TRUE)
            .build();

    MdbDeliveryGroupResourceDefinition() {
        super(new SimpleResourceDefinition.Parameters(EJB3SubsystemModel.MDB_DELIVERY_GROUP_PATH, EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.MDB_DELIVERY_GROUP))
                .setAddHandler(MdbDeliveryGroupAdd.INSTANCE)
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setCapabilities(MDB_DELIVERY_GROUP_CAPABILITY));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(ACTIVE, null,
                new AbstractWriteAttributeHandler<Void>(ACTIVE) {
                    @Override
                    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation,
                                                           String attributeName, ModelNode resolvedValue, ModelNode currentValue,
                                                           HandbackHolder<Void> handbackHolder) throws OperationFailedException {
                        updateDeliveryGroup(context, currentValue, resolvedValue);
                        return false;
                    }

                    @Override
                    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation,
                                                         String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Void handback)
                            throws OperationFailedException {
                        updateDeliveryGroup(context, valueToRevert, valueToRestore);
                    }

                    protected void updateDeliveryGroup(OperationContext context, ModelNode currentValue, ModelNode resolvedValue) throws OperationFailedException {
                        if (currentValue.equals(resolvedValue)) {
                            return;
                        }

                        String groupName = context.getCurrentAddressValue();
                        ServiceName deliveryGroupServiceName = context.getCapabilityServiceName(MdbDeliveryGroupResourceDefinition.MDB_DELIVERY_GROUP_CAPABILITY_NAME, Service.NULL.getClass(), groupName);

                        context.getServiceRegistry(true).getRequiredService(deliveryGroupServiceName)
                                .setMode(resolvedValue.asBoolean() ? ServiceController.Mode.ACTIVE : ServiceController.Mode.NEVER);
                    }
                });
    }
}
