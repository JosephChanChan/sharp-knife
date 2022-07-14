package com.joseph.sharpknife.blade.queue;


import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Joseph
 */
public abstract class MonitoredTaskQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {

    protected int countLimit ;
    protected BlockingQueue<E> queue ;

    /**
     * 计算队列饱和度
     *
     * @return 饱和度
     */
    abstract double calcSaturation();


    @Override
    public void put(E e) throws InterruptedException {
        queue.put(e);
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        return queue.offer(e, timeout, unit);
    }

    @Override
    public E take() throws InterruptedException {
        return queue.take();
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }

    @Override
    public int remainingCapacity() {
        return queue.remainingCapacity();
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return queue.drainTo(c);
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        return queue.drainTo(c, maxElements);
    }

    @Override
    public boolean offer(E e) {
        return queue.offer(e);
    }

    @Override
    public E poll() {
        return queue.poll();
    }

    @Override
    public E peek() {
        return queue.peek();
    }

    @Override
    public Iterator<E> iterator() {
        return queue.iterator();
    }

    @Override
    public int size() {
        return queue.size();
    }
}
