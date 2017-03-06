package com.example.servicehook;

import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Description:
 *
 * @author Shawn_Dut
 * @since 2017-02-21
 */
public class ServiceHook implements InvocationHandler {
    private static final String TAG = "ServiceHook";

    private IBinder mBase;
    private Class<?> mStub;
    private Class<?> mInterface;
    private InvocationHandler mInvocationHandler;

    public ServiceHook(IBinder mBase, String iInterfaceName, boolean isStub, InvocationHandler InvocationHandler) {
        this.mBase = mBase;
        this.mInvocationHandler = InvocationHandler;

        try {
            this.mInterface = Class.forName(iInterfaceName);
            this.mStub = Class.forName(String.format("%s%s", iInterfaceName, isStub ? "$Stub" : ""));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("queryLocalInterface".equals(method.getName())) {
            return Proxy.newProxyInstance(proxy.getClass().getClassLoader(), new Class[]{mInterface},
                    new HookHandler(mBase, mStub, mInvocationHandler));
        }

        Log.e(TAG, "ERROR!!!!! method:name = " + method.getName());
        return method.invoke(mBase, args);
    }

    private static class HookHandler implements InvocationHandler {
        private Object mBase;
        private InvocationHandler mInvocationHandler;

        public HookHandler(IBinder base, Class<?> stubClass,
                           InvocationHandler InvocationHandler) {
            mInvocationHandler = InvocationHandler;

            try {
                Method asInterface = stubClass.getDeclaredMethod("asInterface", IBinder.class);
                this.mBase = asInterface.invoke(null, base);

                Class clazz = mBase.getClass();
                Field mRemote = clazz.getDeclaredField("mRemote");
                mRemote.setAccessible(true);
                //新建一个 BinderProxy 的代理对象
                Object binderProxy = Proxy.newProxyInstance(mBase.getClass().getClassLoader(),
                        new Class[] {IBinder.class}, new ClipboardHook.TransactionWatcherHook((IBinder) mRemote.get(mBase)));
                mRemote.set(mBase, binderProxy);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (mInvocationHandler != null) {
                return mInvocationHandler.invoke(mBase, method, args);
            }
            return method.invoke(mBase, args);
        }
    }
}
