package com.example.servicehook;

import android.content.ClipData;
import android.content.Context;
import android.os.IBinder;
import android.os.Parcel;
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
public class ClipboardHook {

    private static final String TAG = ClipboardHook.class.getSimpleName();

    public static void hookService(Context context) {
        IBinder clipboardService = ServiceManager.getService(Context.CLIPBOARD_SERVICE);
        String IClipboard = "android.content.IClipboard";

        if (clipboardService != null) {
            IBinder hookClipboardService =
                    (IBinder) Proxy.newProxyInstance(clipboardService.getClass().getClassLoader(),
                            clipboardService.getClass().getInterfaces(),
                            new ServiceHook(clipboardService, IClipboard, true, new ClipboardHookHandler()));
            ServiceManager.setService(Context.CLIPBOARD_SERVICE, hookClipboardService);
        } else {
            Log.e(TAG, "ClipboardService hook failed!");
        }
    }

    public static class ClipboardHookHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Log.e(TAG, "clipboardhookhandler invoke");
            String methodName = method.getName();
            int argsLength = args.length;
            //每次从本应用复制的文本，后面都加上分享的出处
            if ("setPrimaryClip".equals(methodName)) {
                if (argsLength >= 2 && args[0] instanceof ClipData) {
                    ClipData data = (ClipData) args[0];
                    String text = data.getItemAt(0).getText().toString();
                    text += "this is shared from ServiceHook-----by Shawn_Dut";
                    args[0] = ClipData.newPlainText(data.getDescription().getLabel(), text);
                }
            }
            return method.invoke(proxy, args);
        }
    }

    //用来监控 TransactionTooLargeException 错误
    public static class TransactionWatcherHook implements InvocationHandler {

        IBinder binder;

        public TransactionWatcherHook(IBinder binderProxy) {
            binder = binderProxy;
        }

        @Override
        public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
            if (objects.length >= 2 && objects[1] instanceof Parcel) {
                //第二个参数对应为 Parcel 对象
                Log.e(TAG, "clipboard service invoked, transact's parameter size is " + ((Parcel)objects[1]).dataSize() + " B");
            }
            return method.invoke(binder, objects);
        }
    }
}
