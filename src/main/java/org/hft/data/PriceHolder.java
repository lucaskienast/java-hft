package org.hft.data;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PriceHolder {
    // private static final int STRIPE_SIZE = 12;

    private final ThreadLocal<Map<String, BigDecimal>> lastSeenPrices;
    private final Map<String, BigDecimal> namesToPrices;

    private final Map<String, ReentrantReadWriteLock> entityLocks;
    // downside of striped lock is that all entities that map to the same lock will share the same condition variable
    // private final ReentrantReadWriteLock[] stripedLocks = new ReentrantReadWriteLock[STRIPE_SIZE];

    private final Map<String, Condition> entityConditions;

    public PriceHolder() {
        namesToPrices = new ConcurrentHashMap<>();
        entityLocks = new ConcurrentHashMap<>();
        entityConditions = new ConcurrentHashMap<>();
        lastSeenPrices = ThreadLocal.withInitial(ConcurrentHashMap::new);
        // for (int i = 0; i < STRIPE_SIZE; i++) stripedLocks[i] = new ReentrantReadWriteLock();
    }

    /** Called when a price ‘p’ is received for an entity ‘e’ */
    public void putPrice(String e, BigDecimal p) {
        ReentrantReadWriteLock lock = getEntityLock(e);
        // todo: use tryLock with timeout
        lock.writeLock().lock();

        try {
            namesToPrices.put(e, p);
            Condition writeLockCondition = getLockCondition(e, LockType.WRITE_LOCK);
            writeLockCondition.signalAll();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Called to get the latest price for entity ‘e’ */
    public BigDecimal getPrice(String e) {
        ReentrantReadWriteLock lock = getEntityLock(e);
        lock.readLock().lock();

        try {
            BigDecimal price = namesToPrices.get(e);
            lastSeenPrices.get().put(e, price);
            return price;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Called to determine if the price for entity ‘e’ has
     * changed since the last call to getPrice(e).
     */
    public boolean hasPriceChanged(String e) {
        ReentrantReadWriteLock lock = getEntityLock(e);
        lock.readLock().lock();

        try {
            BigDecimal lastSeenPrice = lastSeenPrices.get().get(e);
            BigDecimal currentPrice = namesToPrices.get(e);

            if (lastSeenPrice == null) return currentPrice != null;

            return !lastSeenPrice.equals(currentPrice);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the next price for entity ‘e’. If the price has changed since the last
     * call to getPrice() or waitForNextPrice(), it returns immediately that price.
     * Otherwise, it blocks until the next price change for entity ‘e’.
     */
    /*
    public BigDecimal waitForNextPrice(String e) throws InterruptedException {
        ReentrantReadWriteLock lock = getEntityLock(e);
        lock.readLock().lock();

        System.out.println(Thread.currentThread().getName() + " - is holding lock to wait for next price");

        try {
            BigDecimal lastSeenPrice = lastSeenPrices.get().get(e);
            Condition condition = entityConditions.computeIfAbsent(e, k -> lock.writeLock().newCondition());

            while (true) {
                BigDecimal currentPrice = namesToPrices.get(e);

                System.out.println(Thread.currentThread().getName() + " - Last seen price=" + lastSeenPrice + ", current price=" + currentPrice);

                if (currentPrice != null && !currentPrice.equals(lastSeenPrice)) {
                    lastSeenPrices.get().put(e, currentPrice);
                    return currentPrice;
                }

                System.out.println(Thread.currentThread().getName() + " - waiting for condition to be signalled, giving up lock...");
                boolean receivedSignal = condition.await(3, TimeUnit.SECONDS);
                System.out.println(Thread.currentThread().getName() + " - reacquired lock, did receive signal for new price? -> " + receivedSignal);
                if (!receivedSignal) return currentPrice;
            }
        } finally {
            lock.readLock().unlock();
        }
    }
     */

    public BigDecimal waitForNextPrice(String e) throws InterruptedException {
        ReentrantReadWriteLock lock = getEntityLock(e);
        lock.writeLock().lock();

        try {
            BigDecimal lastSeenPrice = lastSeenPrices.get().get(e);
            BigDecimal currentPrice = namesToPrices.get(e);

            while (currentPrice == null || currentPrice.equals(lastSeenPrice)) {
                // Wait for a signal that a new price is available
                Condition condition = lock.writeLock().newCondition();
                boolean receivedNewPriceSignal = condition.await(3, TimeUnit.SECONDS);

                if (!receivedNewPriceSignal) {
                    // Timeout occurred, return the current price
                    return currentPrice;
                }

                // Update currentPrice after the wait
                currentPrice = namesToPrices.get(e);
            }

            // Update last seen price and return the new price
            lastSeenPrices.get().put(e, currentPrice);
            return currentPrice;

        } finally {
            lock.writeLock().unlock();
        }
    }

    private ReentrantReadWriteLock getEntityLock(String e) {
        // return stripedLocks[getStripeIndex(e)];
        return entityLocks.computeIfAbsent(e, k -> new ReentrantReadWriteLock(true));
    }

    /*
    private int getStripeIndex(String e) {
        return Math.abs(e.hashCode() % STRIPE_SIZE);
    }
     */

    private Condition getLockCondition(String e, LockType lockType) {
        /*
        ReentrantReadWriteLock lock = stripedLocks[getStripeIndex(e)];
        if (lockType == LockType.WRITE_LOCK) return lock.writeLock().newCondition();
        else if (lockType == LockType.READ_LOCK) return lock.readLock().newCondition();
        else throw new RuntimeException("Invalid LockType");
         */

        return entityConditions.computeIfAbsent(e, k -> {
            if (lockType == LockType.WRITE_LOCK)
                return entityLocks.get(e).writeLock().newCondition();
            else if (lockType == LockType.READ_LOCK)
                return entityLocks.get(e).readLock().newCondition();
            else
                throw new RuntimeException("Invalid LockType");
        });
    }

    private enum LockType {
        WRITE_LOCK, READ_LOCK;
    }

}
