package com.example.servicehook;

import android.content.Context;
import android.view.WindowManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;

/**
 * Description: 用来监控 WindowManager 的所有行为
 *
 * @author Shawn_Dut
 * @since 2019-02-13
 */
public class WindowManagerHook {

    private static final String TAG = "WindowManagerHook";

    public static void hookService(Context context) {
        try {
            Class clazz = Class.forName("android.app.SystemServiceRegistry");
            Field SYSTEM_SERVICE_FETCHERS = clazz.getDeclaredField("SYSTEM_SERVICE_FETCHERS");
            SYSTEM_SERVICE_FETCHERS.setAccessible(true);
            HashMap<String, Object> fetchers = (HashMap<String, Object>) SYSTEM_SERVICE_FETCHERS.get(null);
            Object windowFetcher = fetchers.get(Context.WINDOW_SERVICE);

            Class ServiceFetcher = Class.forName("android.app.SystemServiceRegistry$CachedServiceFetcher");

            Object newWindowFetcher =
                     Proxy.newProxyInstance(ServiceFetcher.getClassLoader(),
                             ServiceFetcher.getInterfaces(),
                            new ServiceFetcherHandler(windowFetcher, context));

            fetchers.remove(Context.WINDOW_SERVICE);
            fetchers.put(Context.WINDOW_SERVICE, newWindowFetcher);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static class ServiceFetcherHandler implements InvocationHandler {
        private Object windowFetcher;
        private Context context;

        public ServiceFetcherHandler(Object windowFetcher, Context context) {
            this.windowFetcher = windowFetcher;
            this.context = context;
        }

        @Override
        public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
            if (method.getName().equals("getService")) {
                Class ServiceFetcher = Class.forName("android.app.SystemServiceRegistry$CachedServiceFetcher");
                Class ContextImpl = Class.forName("android.app.ContextImpl");
                Method getService = ServiceFetcher.getDeclaredMethod("getService", ContextImpl);
                WindowManager windowManager = (WindowManager) getService.invoke(windowFetcher, objects); // the origin window manager

                Object newWindowManager = Proxy.newProxyInstance(windowManager.getClass().getClassLoader(),
                        windowManager.getClass().getInterfaces(), new WindowManagerHandler(context));
                return newWindowManager;
            }
            return method.invoke(windowFetcher, objects);
        }
    }

    private static class WindowManagerHandler implements InvocationHandler {
        private WindowManager windowManager;

        public WindowManagerHandler(Context context) {
            this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        }

        @Override
        public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
            try {
                Object object = method.invoke(windowManager, objects);
                return object;
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
