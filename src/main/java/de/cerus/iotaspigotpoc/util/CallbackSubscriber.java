package de.cerus.iotaspigotpoc.util;

import java.util.function.Consumer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Simplifies the usage of the MongoDB reactive streams driver by providing
 * simple callback functions
 *
 * @param <T> The response type to use for this subscriber
 *
 * @author Lukas Schulte Pelkum
 * @version 1.0.0
 * @since 1.0.0
 */
public class CallbackSubscriber<T> implements Subscriber<T> {

    // Define the consumers to execute after an event
    private Consumer<Subscription> subscribeConsumer;
    private Consumer<T> nextConsumer;
    private Consumer<Throwable> errorConsumer;
    private Runnable completeRunnable;

    /**
     * @param consumer Gets called after the subscriber got subscribed
     */
    public void doOnSubscribe(final Consumer<Subscription> consumer) {
        this.subscribeConsumer = consumer;
    }

    @Override
    public void onSubscribe(final Subscription s) {
        s.request(Long.MAX_VALUE);
        if (this.subscribeConsumer == null) {
            return;
        }
        this.subscribeConsumer.accept(s);
    }

    /**
     * @param consumer Gets called after the subscriber received a new value
     */
    public void doOnNext(final Consumer<T> consumer) {
        this.nextConsumer = consumer;
    }

    @Override
    public void onNext(final T t) {
        if (this.nextConsumer == null) {
            return;
        }
        this.nextConsumer.accept(t);
    }

    /**
     * @param consumer Gets called after the subscriber received an error
     */
    public void doOnError(final Consumer<Throwable> consumer) {
        this.errorConsumer = consumer;
    }

    @Override
    public void onError(final Throwable t) {
        if (this.errorConsumer == null) {
            return;
        }
        this.errorConsumer.accept(t);
    }

    /**
     * @param runnable Gets called after the subscriber completed
     */
    public void doOnComplete(final Runnable runnable) {
        this.completeRunnable = runnable;
    }

    @Override
    public void onComplete() {
        if (this.completeRunnable == null) {
            return;
        }
        this.completeRunnable.run();
    }

}
