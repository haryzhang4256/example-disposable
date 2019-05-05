package com.hary.disposable;

public class FinalizeResource implements Resource {
    @Override
    public void dispose() {
        System.out.println("dispose");
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        System.out.println("finalize");
        dispose();
    }
}
