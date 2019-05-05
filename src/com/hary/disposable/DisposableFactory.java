package com.hary.disposable;



import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DisposableFactory {

    private static Map<Disposable, DisposeRuntime> GLOBAL_DISPOSABLE = new ConcurrentHashMap<>();
    private static DisposalQueue REF_QUEUE = new DisposalQueue();

    /**
     * create source proxy, receiver type is interface type that common with source .
     * 1. return source proxy
     * 2. dispose method call by thread
     * @param source
     * @param interClazz interface class
     * @return
     */
    public static <T extends Disposable, P extends Disposable> P create(T source, Class<P> interClazz) {
        DisposalInvocationHandler<T> handler = new DisposalInvocationHandler<>(source);
        P proxy = (P)Proxy.newProxyInstance(source.getClass().getClassLoader(), new Class[]{interClazz}, handler);
        PhantomDisposableRef ref = new PhantomDisposableRef(proxy, source, REF_QUEUE);
        DisposalThread refThread = new DisposalThread(REF_QUEUE);
        refThread.start();
        GLOBAL_DISPOSABLE.put(source, new DisposeRuntime(source, ref, refThread, handler));
        return proxy;
    }

    private static <T extends Disposable> void destroySource(T source) {
        DisposeRuntime runtime = GLOBAL_DISPOSABLE.remove(source);
        runtime.refThread.exit();
        runtime.ref.clear();
        runtime.ref = null;
        runtime.refThread = null;
        runtime.handler = null;
        runtime.disposable = null;
    }


    private static class DisposalQueue extends ReferenceQueue<Disposable> {

    }

    private static class DisposalInvocationHandler<T extends Disposable> implements InvocationHandler {

        private T source;
        private DisposalInvocationHandler(T t) {
            this.source = t;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return method.invoke(this.source, args);
        }
    }


    private static class DisposalThread  extends Thread {

        private DisposalQueue queue;
        private boolean mRun;
        public DisposalThread(DisposalQueue queue) {
            this.queue = queue;
            setDaemon(true);
            setName("ReferenceDisposalThread");
            mRun = true;
        }

        @Override
        public void run() {
            while(mRun) {
                try {
                    PhantomDisposableRef ref = (PhantomDisposableRef) queue.remove();
                    ref.dispose();
                    ref.clear();
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }

        public void exit() {
            mRun = false;
            try {
                this.interrupt();
            } catch (Exception e) {

            }
        }
    }


    private static class PhantomDisposableRef extends PhantomReference<Disposable> {


        /**
         * real disposable
         */
        private Disposable disposable;

        /**
         *
         * @param referent  proxy
         * @param q
         */
        public PhantomDisposableRef(Disposable referent, Disposable source, ReferenceQueue<? super Disposable> q) {
            super(referent, q);
            this.disposable = source;
        }

        public void dispose() {
            if (disposable != null) {
                disposable.dispose();
                destroySource(disposable);
            }
        }
    }

    private static class DisposeRuntime {
        public Disposable disposable;
        public PhantomDisposableRef ref;
        public DisposalThread refThread;
        public DisposalInvocationHandler handler;
        public DisposeRuntime(Disposable source, PhantomDisposableRef phantomRef, DisposalThread thread, DisposalInvocationHandler handler) {
            this.disposable = source;
            this.ref = phantomRef;
            this.refThread = thread;
            this.handler = handler;
        }
    }

}

