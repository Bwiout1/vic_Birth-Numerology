package com.aab.resguard.reflect;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.os.Build;
import android.os.IBinder;

import com.aab.resguard.AppExtResManager;
import com.aab.resguard.ResourcesHookerUtil;
import com.aab.resguard.util.AabResGuardDbgSwitch;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@SuppressLint("PrivateApi")
public class ResourcesManager {
    private static final Class<?> clzResourcesManager;
    static {
        Class<?> clz;
        try{
            clz = Class.forName("android.app.ResourcesManager");
        } catch (ClassNotFoundException e) {
            clz = null;
            if (AabResGuardDbgSwitch.LOG_ENABLE)
                e.printStackTrace();
        }
        clzResourcesManager = clz;
    }

    private static volatile ResourcesManager inst ;
    public static ResourcesManager getInstance(){
        if (inst == null){
            synchronized (ResourcesManager.class){
                if (inst == null){
                    if (clzResourcesManager!=null){
                        try {
                            Method method = clzResourcesManager.getDeclaredMethod("getInstance");
                            method.setAccessible(true);
                            Object obj = method.invoke(clzResourcesManager);
                            inst = new ResourcesManager(obj);
                        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                            if (AabResGuardDbgSwitch.LOG_ENABLE)
                                e.printStackTrace();
                        }
                    }
                }
            }
        }

        return inst;
    }


    private final Object core;
    private final Reflect reflect;
    private ResourcesManager(Object core){
        this.core = core;
        reflect = core == null ? null : Reflect.on(core);
    }

    public <T> T getField(String name){
        T ret = null;
        try {
            Field field = clzResourcesManager.getDeclaredField(name) ;
            field.setAccessible(true);
            ret = (T) field.get(core);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            if (AabResGuardDbgSwitch.LOG_ENABLE)
                e.printStackTrace();
        }

        return ret;
    }

    public <T> void setField(String name, T val) {
        if (reflect != null) {
            reflect.set(name, val);
        }
    }

    public boolean isApkRes(Object resImpl){
        boolean isApkRes = false;

        String currentAppSourceDir = AppExtResManager.getInstance().getAppSourceDir();

        //ArrayMap<ResourcesKey, WeakReference<ResourcesImpl>>
        Map<Object, WeakReference<Object>> mResourceImpls = getField("mResourceImpls") ;
        for (Map.Entry<Object, WeakReference<Object>> entry: mResourceImpls.entrySet()){
            WeakReference<Object> resImplRef = entry.getValue();

            Object entryResImpl = resImplRef==null ? null : resImplRef.get();
            if(entryResImpl!=null && entryResImpl.equals(resImpl)){
                Object resKey = entry.getKey();
                if (currentAppSourceDir.equals(Reflect.on(resKey).get("mResDir"))){
                    isApkRes = true;
                }

                break;
            }
        }

        return isApkRes;
    }

    public void refreshInMemoryResources(){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            refreshInMemoryResUnderN();
        } else {
            String currentAppSourceDir = AppExtResManager.getInstance().getAppSourceDir();

            String decodedResPath = AppExtResManager.getInstance().getDecodedResPath();

            //1. ArrayMap<ResourcesKey, WeakReference<ResourcesImpl>>
            Map<Object, Object> mResourceImpls = getField("mResourceImpls");
            for (Map.Entry<Object, Object> entry : mResourceImpls.entrySet()) {

                Reflect resKey = Reflect.on(entry.getKey());
                if (currentAppSourceDir.equals(resKey.get("mResDir"))) {
                    String[] mSplitResDirs = resKey.get("mSplitResDirs");
                    boolean hasAppended = false;
                    if (mSplitResDirs != null) {
                        for (String splitResDirs : mSplitResDirs) {
                            if (decodedResPath.equals(splitResDirs)) {
                                hasAppended = true;
                                break;
                            }
                        }
                    }
                    if (!hasAppended) {
                        if (mSplitResDirs == null) {
                            mSplitResDirs = new String[]{decodedResPath};
                        } else {
                            String[] tmp = new String[mSplitResDirs.length + 1];
                            System.arraycopy(mSplitResDirs, 0, tmp, 0, mSplitResDirs.length);
                            tmp[tmp.length - 1] = decodedResPath;
                            mSplitResDirs = tmp;
                        }

                        resKey.set("mSplitResDirs", mSplitResDirs);
                    }

                    WeakReference<Object> resImplRef = (WeakReference<Object>) entry.getValue();
                    ResourcesHookerUtil.addAssetPath(resImplRef.get());
                }

            }

            //2. WeakHashMap<IBinder, ActivityResources> mActivityResourceReferences
            Map<Object, Object> mActivityResourceReferences = getField("mActivityResourceReferences");
            for (Object actRes : mActivityResourceReferences.values()) {
                try {
                    ArrayList<WeakReference<Resources>> activityResources = new ActivityResources(actRes).getField("activityResources");
                    for (WeakReference<Resources> resRef : activityResources) {
                        Resources activityRes = resRef.get();
                        if (activityRes == null)
                            continue;

                        ResourcesHookerUtil.addAssetPath(activityRes, true);
                    }
                } catch (Exception exp) {
                    if (AabResGuardDbgSwitch.LOG_ENABLE)
                        exp.printStackTrace();
                }
            }

            List<WeakReference<Resources>> mResourceReferences = getField("mResourceReferences");
            for (WeakReference<Resources> resRef : mResourceReferences) {
                Resources res = resRef.get();
                if (res == null)
                    continue;

                ResourcesHookerUtil.addAssetPath(res, true);
            }

        }
    }

    //<7.0
    private void refreshInMemoryResUnderN(){
        String currentAppSourceDir = AppExtResManager.getInstance().getAppSourceDir();

        //ArrayMap<ResourcesKey, WeakReference<Resources> > mActiveResources
        Map<Object, WeakReference<Resources>> mActiveResources = getField("mActiveResources");
        if(mActiveResources!=null){
            for (Map.Entry<Object, WeakReference<Resources>> entry : mActiveResources.entrySet()) {
                Reflect resKey = Reflect.on(entry.getKey());
                if(currentAppSourceDir.equals(resKey.get("mResDir"))){
                    Resources res = entry.getValue().get();
                    if (res == null)
                        continue;

                    ResourcesHookerUtil.addAssetPath(res, true);
                }
            }
        }
    }

    public void hook(){
//1.mResourceReferences
        ArrayList<WeakReference<Resources>> resListObj = getField("mResourceReferences");
        if (!(resListObj instanceof mResourceReferencesWrapper)) {
            mResourceReferencesWrapper resListWrapper = new mResourceReferencesWrapper(resListObj);
            setField("mResourceReferences", resListWrapper);
        }

//2.WeakHashMap<IBinder, ActivityResources> mActivityResourceReferences
        WeakHashMap<IBinder, Object> actResMapObj = getField("mActivityResourceReferences");
        if(! (actResMapObj instanceof mActivityResourceReferencesWrapper)) {
            mActivityResourceReferencesWrapper actResMapWrapper = new mActivityResourceReferencesWrapper(actResMapObj);
            setField("mActivityResourceReferences", actResMapWrapper);
        }
    }




    public static class ActivityResources{
        private final static Class<?> clzResourcesManager$ActivityResources;
        static {
            Class<?> clz;
            try{
                clz = Class.forName("android.app.ResourcesManager$ActivityResources");
            } catch (ClassNotFoundException e) {
                clz = null;
                if (AabResGuardDbgSwitch.LOG_ENABLE)
                    e.printStackTrace();
            }
            clzResourcesManager$ActivityResources = clz;
        }


        private final Object core;
        public ActivityResources(Object core){
            this.core = core;
        }

        public <T> T getField(String name){
            T ret = null;
            try {
                Method classGetDFMethod = Class.forName("java.lang.Class").getDeclaredMethod("getDeclaredField", String.class);
                Field field = (Field) classGetDFMethod.invoke(clzResourcesManager$ActivityResources, name);

                field.setAccessible(true);
                ret = (T) field.get(core);
            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | ClassNotFoundException e) {
                if (AabResGuardDbgSwitch.LOG_ENABLE)
                    e.printStackTrace();
            }

            return ret;
        }

        public <T> void setFiled(String name, T val){
            try {
                Method classGetDFMethod = Class.forName("java.lang.Class").getDeclaredMethod("getDeclaredField", String.class);
                Field field = (Field) classGetDFMethod.invoke(clzResourcesManager$ActivityResources, name);

                field.setAccessible(true);
                if (Modifier.isFinal(field.getModifiers())){
                    Field modifiersField;
                    try {
                        modifiersField = Field.class.getDeclaredField("modifiers");
                    } catch (Exception ignored){
                        modifiersField = Field.class.getDeclaredField("accessFlags");
                    }
                    modifiersField.setAccessible(true);
                    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                }

                field.set(core, val);
            } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException | NoSuchFieldException e) {
                if (AabResGuardDbgSwitch.LOG_ENABLE)
                    e.printStackTrace();
            }
        }


        static class activityResourcesWrapper extends  ArrayList<WeakReference<Resources>> {
            public activityResourcesWrapper(ArrayList<WeakReference<Resources>> base){
                super(base);
            }

            @Override
            public boolean add(WeakReference<Resources> e) {
                Resources res = e.get();
                if (res != null) {
                    ResourcesHookerUtil.addAssetPath(res, false);
                }
                return super.add(e);
            }
        }
    }
}

class mActivityResourceReferencesWrapper extends WeakHashMap<IBinder, Object> {
    public mActivityResourceReferencesWrapper(WeakHashMap<IBinder, Object> core){
        super(core);
    }

    @Nullable
    @Override
    public Object put(@NonNull IBinder key, @NonNull Object value) {
        Object result = super.put(key, value);

        //第一次 放入一个新的 activityResources value 对象
        //加 ArrayList的 warp
        try {
            ResourcesManager.ActivityResources actRes = new ResourcesManager.ActivityResources(value);
            ArrayList<WeakReference<Resources>> activityResources = actRes.getField("activityResources");
            if (! (activityResources instanceof ResourcesManager.ActivityResources.activityResourcesWrapper)){
                ResourcesManager.ActivityResources.activityResourcesWrapper wrapper
                        = new ResourcesManager.ActivityResources.activityResourcesWrapper(activityResources) ;
                actRes.setFiled("activityResources", wrapper);
            }

        } catch (Exception e) {
            if (AabResGuardDbgSwitch.LOG_ENABLE)
                e.printStackTrace();
        }

        return result;
    }
}

class mResourceReferencesWrapper  extends ArrayList<WeakReference<Resources>> {
    public mResourceReferencesWrapper(ArrayList<WeakReference<Resources>> core){
        super(core);
    }

    @Override
    public boolean add(WeakReference<Resources> resourcesWeakReference) {
        //如果是我们的包，则插入资源包路径
        Resources res = resourcesWeakReference.get();
        ResourcesHookerUtil.addAssetPath(res, false);
        return super.add(resourcesWeakReference);
    }
}
