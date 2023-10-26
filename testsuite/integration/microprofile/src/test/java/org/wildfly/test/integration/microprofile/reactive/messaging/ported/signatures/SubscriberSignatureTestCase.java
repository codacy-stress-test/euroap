/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.ported.signatures;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.wildfly.test.integration.microprofile.reactive.messaging.ported.utils.ReactiveMessagingTestUtils.await;
import static org.wildfly.test.integration.microprofile.reactive.messaging.ported.utils.ReactiveMessagingTestUtils.checkList;

import java.util.List;
import java.util.PropertyPermission;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;
import org.wildfly.test.integration.microprofile.reactive.messaging.ported.utils.ReactiveMessagingTestUtils;

/**
 * Ported from Quarkus and adjusted
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@ServerSetup(EnableReactiveExtensionsSetupTask.class)
@RunWith(Arquillian.class)
public class SubscriberSignatureTestCase {

    @Deployment
    public static WebArchive enableExtensions() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "rx-messaging-subscriber-signature.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addClasses(SubscriberSignatureTestCase.class,
                        BeanUsingSubscriberOfPayload.class,
                        BeanUsingSubscriberOfMessage.class,
                        BeanUsingConsumerMethod.class,
                        BeanConsumingMessages.class,
                        BeanConsumingPayloads.class,
                        Spy.class)
                .addClasses(ReactiveMessagingTestUtils.class, TimeoutUtil.class, EnableReactiveExtensionsSetupTask.class, CLIServerSetupTask.class)
                .addAsManifestResource(createPermissionsXmlAsset(
                        new PropertyPermission(TimeoutUtil.FACTOR_SYS_PROP, "read")
                ), "permissions.xml");
        return webArchive;
    }

    @Inject
    BeanUsingSubscriberOfPayload beanUsingSubscriberOfPayload;
    @Inject
    BeanUsingSubscriberOfMessage beanUsingSubscriberOfMessage;
    @Inject
    BeanUsingConsumerMethod beanUsingConsumerMethod;
    @Inject
    BeanConsumingMessages beanConsumingMessages;
    @Inject
    BeanConsumingPayloads beanConsumingPayloads;

    @Test
    public void testMethodReturningASubscriberOfPayload() {
        Emitter<Integer> emitter = beanUsingSubscriberOfPayload.emitter();
        List<Integer> items = beanUsingSubscriberOfPayload.getItems();

        emit(emitter);

        await(() -> items.size() == 10);
        checkList(items, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        Assert.assertTrue(beanUsingSubscriberOfPayload.hasCompleted());
    }

    private void emit(Emitter<Integer> emitter) {
        CountDownLatch completedLatch = new CountDownLatch(1);
        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                emitter.send(i);
            }
            emitter.complete();
            completedLatch.countDown();
        }).start();

        try {
            completedLatch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // Rethrow as an unchecked exception to keep this test as similar as possible to the original Quarkus
            // code (I don't want to declare every method to throw Exception)
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testMethodReturningASubscriberOfMessage() {
        Emitter<Integer> emitter = beanUsingSubscriberOfMessage.emitter();
        List<Integer> items = beanUsingSubscriberOfMessage.getItems();

        emit(emitter);

        await(() -> items.size() == 10);
        checkList(items, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        Assert.assertEquals(10, beanUsingSubscriberOfMessage.getMessages().size());
        Assert.assertTrue(beanUsingSubscriberOfPayload.hasCompleted());
    }

    @Test
    public void testMethodConsumingPayloadSynchronously() {
        Emitter<Integer> emitter = beanUsingConsumerMethod.emitter();
        List<Integer> items = beanUsingConsumerMethod.getItems();

        emit(emitter);

        await(() -> items.size() == 10);
        checkList(items, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void testMethodConsumingPayloadAsynchronously() {
        Emitter<Integer> emitter = beanConsumingPayloads.emitter();
        List<Integer> items = beanConsumingPayloads.getItems();

        emit(emitter);

        await(() -> items.size() == 10);
        checkList(items, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void testMethodConsumingMessages() {
        Emitter<Integer> emitter = beanConsumingMessages.emitter();
        List<Integer> items = beanConsumingMessages.getItems();

        emit(emitter);

        await(() -> items.size() == 10);
        checkList(items, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        Assert.assertEquals(10, beanConsumingMessages.getMessages().size());
    }

    @ApplicationScoped
    public static class BeanUsingSubscriberOfPayload extends Spy {

        @Inject
        @Channel("A")
        Emitter<Integer> emitter;

        public Emitter<Integer> emitter() {
            return emitter;
        }

        @SuppressWarnings("SubscriberImplementation")
        @Incoming("A")
        public Subscriber<Integer> consume() {
            return new Subscriber<Integer>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscription.request(10);
                }

                @Override
                public void onNext(Integer o) {
                    items.add(o);
                }

                @Override
                public void onError(Throwable throwable) {
                    failure.set(throwable);
                }

                @Override
                public void onComplete() {
                    completed.set(true);
                }
            };
        }

    }

    @ApplicationScoped
    public static class BeanUsingSubscriberOfMessage extends Spy {

        @Inject
        @Channel("B")
        Emitter<Integer> emitter;

        public Emitter<Integer> emitter() {
            return emitter;
        }

        @SuppressWarnings("SubscriberImplementation")
        @Incoming("B")
        public Subscriber<Message<Integer>> consume() {
            return new Subscriber<Message<Integer>>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscription.request(10);
                }

                @Override
                public void onNext(Message<Integer> o) {
                    messages.add(o);
                    items.add(o.getPayload());
                }

                @Override
                public void onError(Throwable throwable) {
                    failure.set(throwable);
                }

                @Override
                public void onComplete() {
                    completed.set(true);
                }
            };
        }

    }

    @ApplicationScoped
    public static class BeanUsingConsumerMethod extends Spy {

        @Inject
        @Channel("C")
        Emitter<Integer> emitter;

        public Emitter<Integer> emitter() {
            return emitter;
        }

        @Incoming("C")
        public void consume(Integer i) {
            items.add(i);
        }

    }

    @ApplicationScoped
    public static class BeanConsumingMessages extends Spy {

        @Inject
        @Channel("D")
        Emitter<Integer> emitter;

        public Emitter<Integer> emitter() {
            return emitter;
        }

        @Incoming("D")
        public CompletionStage<Void> consume(Message<Integer> message) {
            getItems().add(message.getPayload());
            getMessages().add(message);
            return message.ack();
        }

    }

    @ApplicationScoped
    public static class BeanConsumingPayloads extends Spy {
        @Inject
        @Channel("E")
        Emitter<Integer> emitter;

        public Emitter<Integer> emitter() {
            return emitter;
        }

        @Incoming("E")
        public CompletionStage<Void> consume(Integer item) {
            getItems().add(item);
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class Spy {
        AtomicBoolean completed = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        List<Integer> items = new CopyOnWriteArrayList<>();
        List<Message<Integer>> messages = new CopyOnWriteArrayList<>();

        public boolean hasCompleted() {
            return completed.get();
        }

        public List<Integer> getItems() {
            return items;
        }

        public List<Message<Integer>> getMessages() {
            return messages;
        }
    }

}
