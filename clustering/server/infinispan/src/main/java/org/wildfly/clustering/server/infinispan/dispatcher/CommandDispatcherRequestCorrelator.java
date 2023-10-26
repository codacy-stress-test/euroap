/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.dispatcher;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Predicate;

import org.jgroups.BytesMessage;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.blocks.Request;
import org.jgroups.blocks.RequestCorrelator;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.conf.ClassConfigurator;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.server.infinispan.ClusteringServerLogger;

/**
 * @author Paul Ferraro
 */
public class CommandDispatcherRequestCorrelator extends RequestCorrelator {

    private final ByteBufferMarshaller marshaller;
    private final Predicate<Message> unknownForkPredicate;

    public CommandDispatcherRequestCorrelator(JChannel channel, RequestHandler handler, ChannelCommandDispatcherFactoryConfiguration config) {
        super(channel.getProtocolStack(), handler, channel.getAddress());
        this.marshaller = config.getMarshaller();
        this.unknownForkPredicate = config.getUnknownForkPredicate();
        this.corr_id = ClassConfigurator.getProtocolId(RequestCorrelator.class);
    }

    @Override
    protected void dispatch(Message message, Header header) {
        boolean exception = false;
        switch (header.type) {
            case Header.REQ:
                this.handleRequest(message, header);
                break;
            case Header.EXC_RSP:
                exception = true;
                // Fall through
            case Header.RSP:
                Request<?> request = this.requests.get(header.req_id);
                if (request != null) {
                    try {
                        Object response = this.readPayload(message);
                        request.receiveResponse(response, message.getSrc(), exception);
                    } catch (IOException e) {
                        ClusteringServerLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
                        request.receiveResponse(e, message.getSrc(), true);
                    }
                }
                break;
            default:
                throw new IllegalArgumentException(header.toString());
        }
    }

    private Object readPayload(Message message) throws IOException {
        if (this.unknownForkPredicate.test(message)) {
            return NoSuchService.INSTANCE;
        }
        if (message.isFlagSet(Message.Flag.SERIALIZED)) {
            return message.getObject();
        }
        ByteBuffer buffer = ByteBuffer.wrap(message.getArray(), message.getOffset(), message.getLength());
        return this.marshaller.read(buffer);
    }

    @Override
    protected void sendReply(Message request, long requestId, Object reply, boolean exception) {
        Message response = new BytesMessage(request.getSrc()).setFlag(request.getFlags(false), false).clearFlag(Message.Flag.RSVP);
        if (request.getDest() != null) {
            response.setSrc(request.getDest());
        }
        try {
            ByteBuffer buffer = this.marshaller.write(reply);
            response.setArray(buffer.array(), buffer.arrayOffset(), buffer.limit() - buffer.arrayOffset());
        } catch (IOException e) {
            ClusteringServerLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
            response.setObject(e);
        }
        this.sendResponse(response, requestId, exception);
    }
}
