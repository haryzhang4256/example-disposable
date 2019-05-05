package com.hary.disposable;

public interface Disposable {
    /**
     * do not call explicitly, called by gc
     */
    void dispose();
}
