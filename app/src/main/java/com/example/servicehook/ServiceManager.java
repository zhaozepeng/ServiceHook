package com.example.servicehook;

import android.os.IBinder;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Description:
 *
 * @author Shawn_Dut
 * @since 2017-02-21
 */
public class ServiceManager {

    private static Method sGetServiceMethod;
    private static Map<String, IBinder> sCacheService;

    private static Class c_ServiceManager;

    static {
        try {
            c_ServiceManager = Class.forName("android.os.ServiceManager");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static IBinder getService(String serviceName) {
        if (c_ServiceManager == null) {
            return null;
        }

        if (sGetServiceMethod == null) {
            try {
                sGetServiceMethod = c_ServiceManager.getDeclaredMethod("getService", String.class);
                sGetServiceMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        if (sGetServiceMethod != null) {
            try {
                return (IBinder) sGetServiceMethod.invoke(null, serviceName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public static void setService(String serviceName, IBinder service) {
        if (c_ServiceManager == null) {
            return;
        }

        if (sCacheService == null) {
            try {
                Field sCache = c_ServiceManager.getDeclaredField("sCache");
                sCache.setAccessible(true);
                sCacheService = (Map<String, IBinder>) sCache.get(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        sCacheService.remove(serviceName);
        sCacheService.put(serviceName, service);
    }
}
