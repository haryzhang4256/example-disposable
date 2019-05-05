package com.hary.disposable;

public class Main {

    public static void main(String[] args) {
	// write your code here

        createResource();
        System.gc();
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.gc();

    }


    /**
     * if resource disposable need time to destroy, that if you use finalize method to call disposable , it will occur timeoutException in FinalizeReference
     * so, you can you PhantomReference to watch the proxy resource , when it finalize method called, it will invoke real resource to dispose related resource ,
     * when the second gc ,  then the real resource will finalize and memory recycle.
     */
    private static void createResource() {
        Resource autoDisposableResource = DisposableFactory.create(new RealResource(), Resource.class);
        Resource autoDisposableResource1 = new FinalizeResource();
    }
}
