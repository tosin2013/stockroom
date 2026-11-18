package dev.stockroom.product;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe collector of {@link StockReserved} events, used by concurrency tests.
 */
@Component
public class StockReservedCollector {

    private final List<StockReserved> events = new CopyOnWriteArrayList<>();

    @EventListener
    public void onStockReserved(StockReserved event) {
        events.add(event);
    }

    public List<StockReserved> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public void clear() {
        events.clear();
    }
}
