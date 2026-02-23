package com.cuttypaws.cache;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class CacheToggleService {

    private final AtomicBoolean enabled = new AtomicBoolean(true);

    public boolean isEnabled() {
        return enabled.get();
    }

    public void enable() {
        enabled.set(true);
    }

    public void disable() {
        enabled.set(false);
    }
}
