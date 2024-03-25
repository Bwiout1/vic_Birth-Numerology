package com.vt.sdkres.dex.core;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;

import com.LogUtil;
import com.Switch;
import com.sun.dex.core.CryptoUtil;
import com.sun.dex.core.exception.InvalidArgumentsException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * @author yb
 * @date 2023/3/24
 * @describe
 */
public class DexLoader {

    private static final String TAG = "DexLoad";
    private static final String FIRST_INSTALL_KEY = "firstInstallTime";

    private String key;

    private static class SINGLETON {
        private static final DexLoader instance = new DexLoader();
    }

    public static DexLoader getInstance() {
        return SINGLETON.instance;
    }

    /**
     * 加载dex
     *
     * @param app
     * @param key 原始解密Key
     * @throws Exception
     */
    public void decryptAndLoad(Application app, String key) throws Exception {
        long startTime = SystemClock.currentThreadTimeMillis();
        if (Switch.LOG_ON) {
            LogUtil.d(TAG, "start load");
        }

        List<File> dexFiles = unZipDex(app, key);
        if (!dexFiles.isEmpty()) {
            ByteBuffer[] byteBuffers = decryptDex(dexFiles);
            loadDexInMemory(app, byteBuffers);
        }
        if (Switch.LOG_ON) {
            LogUtil.d(TAG, "loadDex done:" + (SystemClock.currentThreadTimeMillis() - startTime) + "ms");
        }
    }

    /**
     * 检查特定目录是否存在，如果存在，表明之前已经解压，直接加载。
     *
     * @param app
     * @return
     */
    public boolean load(Application app) throws Exception {
        boolean ret = false;

        boolean isPaidUser = checkUpdate(app);
        if (Switch.LOG_ON) {
            LogUtil.d(TAG, "isPaidUser:" + isPaidUser);
        }

        if (isPaidUser) {//有效用户
            List<File> dexFiles = unZipDex(app, key);
            if (!dexFiles.isEmpty()) {
                ByteBuffer[] byteBuffers = decryptDex(dexFiles);
                loadDexInMemory(app, byteBuffers);
                ret = true;
            }
        }

        return ret;
    }

    //解压dex
    private List<File> unZipDex(Application app, String key) throws Exception {
        if (Switch.LOG_ON) {
            LogUtil.d(TAG, "unZip dex");
        }
        File appDir = getAppDir(app);
        if (Switch.LOG_ON) {
            LogUtil.d(TAG, "appDir:" + appDir);
        }

        PackageInfo pi = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
        if (Switch.LOG_ON) {
            String encrypt = generateSecondDir(key, String.valueOf(pi.firstInstallTime));
            LogUtil.d(TAG, "encrypt:" + encrypt);
            String decrypt = generateKey(encrypt, String.valueOf(pi.firstInstallTime));
            LogUtil.d(TAG, "decrypt:" + decrypt);
        }

        //加密文件解压目录
        File targetDir = null;
        if (!TextUtils.isEmpty(key)) {
            //解压二级目录
            String secondDir = generateSecondDir(key, String.valueOf(pi.firstInstallTime));
            targetDir = getTargetFile(appDir, secondDir);
            this.key = key;
        } else if (appDir.listFiles().length == 1) {
            targetDir = appDir.listFiles()[0];
            //从二级目录中计算出key
            this.key = generateKey(targetDir.getName(), String.valueOf(pi.firstInstallTime));
        }

        if (targetDir == null) {
            throw new RuntimeException("not found file");
        }
        if (!targetDir.exists() || targetDir.listFiles() == null || targetDir.listFiles().length == 0) {
            File apkFile = new File(app.getApplicationInfo().sourceDir);
            if (Switch.LOG_ON) {
                LogUtil.d(TAG, "apkFile:" + apkFile);
            }
            //加密dex存放目录
            String encryptedDexDir = getEncryptedDexDir();
            //解压apk到appDir下
            ZipUtil.getInstance().unZipTargetFile(apkFile, targetDir, encryptedDexDir);
        }
        if (Switch.LOG_ON) {
            LogUtil.d(TAG, "target:" + targetDir + " [size:" + targetDir.listFiles().length + "]");
        }
        //dex文件list
        return Arrays.asList(targetDir.listFiles());
    }

    //解密dex
    private ByteBuffer[] decryptDex(List<File> dexFiles) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, InvalidArgumentsException {
        if (Switch.LOG_ON) {
            LogUtil.d(TAG, "decrypt Dex");
            LogUtil.d(TAG, "dexFiles size:" + dexFiles.size());
        }
        //加密的dex数据，一个ByteBuffer是一个dex文件
        ByteBuffer[] byteBuffers = new ByteBuffer[dexFiles.size()];
        int i = 0;
        for (File file : dexFiles) {
            byte[] contents = ProxyUtil.getInstance().getBytes(file);
            byte[] decryptContents = CryptoUtil.decAndRemovePadding(key, contents);
            ByteBuffer buffer = ByteBuffer.wrap(decryptContents);
            byteBuffers[i] = buffer;
            i++;
        }
        return byteBuffers;
    }

    //加载dex,从内存中
    private void loadDexInMemory(Application app, ByteBuffer[] byteBuffers) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        if (Switch.LOG_ON) {
            LogUtil.d(TAG, "Build version:" + Build.VERSION.SDK_INT);
            LogUtil.d(TAG, "load dex in memory");
        }
        //InMemoryDexClassLoader仅支持android8.0以上
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            /*android8.0(26)以下没有makeInMemoryDexElements方法*/
            /*java.lang.NoSuchMethodException: Method makeInMemoryDexElements with parameters [class [Ljava.nio.ByteBuffer;, interface java.util.List] not found in class dalvik.system.DexPathList*/
            return;
        }

        //1.先从 ClassLoader 中获取 pathList 的变量
        Field pathListField = ProxyUtil.getInstance().findField(app.getClassLoader(), "pathList");
        //1.1 得到 DexPathList 类
        Object pathList = pathListField.get(app.getClassLoader());
        //1.2 从 DexPathList 类中拿到 dexElements 变量
        Field dexElementsField = ProxyUtil.getInstance().findField(pathList, "dexElements");
        //1.3 拿到已加载的 dex 数组
        Object[] dexElements = (Object[]) dexElementsField.get(pathList);

        //2. 反射到初始化 dexElements 的方法，也就是得到加载 dex 到系统的方法
        Method makeInMemoryDexElements = ProxyUtil.getInstance().findMethod(pathList, "makeInMemoryDexElements", ByteBuffer[].class, List.class);
        //2.1 实例化一个 集合  makePathElements 需要用到
        ArrayList<IOException> suppressedExceptions = new ArrayList<>();
        //2.2 反射执行 makeInMemoryDexElements 函数，把已解码的 dex 加载到系统，不然是打不开 dex 的，会导致 crash
        Object[] addElements = (Object[]) makeInMemoryDexElements.invoke(pathList, byteBuffers, suppressedExceptions);

        //3. 实例化一个新数组，用于将当前加载和已加载的 dex 合并成一个新的数组
        Object[] newElements = (Object[]) Array.newInstance(dexElements.getClass().getComponentType(), dexElements.length + addElements.length);
        //3.1 将系统中的已经加载的 dex 放入 newElements 中
        System.arraycopy(dexElements, 0, newElements, 0, dexElements.length);
        //3.2 将解密后已加载的 dex 放入新数组中
        System.arraycopy(addElements, 0, newElements, dexElements.length, addElements.length);

        //4. 将合并的新数组重新设置给 DexPathList的 dexElements
        dexElementsField.set(pathList, newElements);
        if (Switch.LOG_ON) {
            LogUtil.d(TAG, "load dex end");
        }
    }

    //检查是否更新apk
    private boolean checkUpdate(Application app) {
        try {
            SharedPreferences sp = getSp(app);
            PackageManager pm = app.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(app.getPackageName(), 0);
            long fit = sp.getLong(FIRST_INSTALL_KEY, 0);
            long lut = pi.lastUpdateTime;
            if (fit == 0) {
                //首次打开
                sp.edit().putLong(FIRST_INSTALL_KEY, lut).apply();
                return false;
            } else if (fit != lut) {
                //更新后首次打开
                if (Switch.LOG_ON) {
                    LogUtil.d(TAG, "firstInstallTime:" + fit + ", lastUpdateTime:" + lut);
                }
                File appDir = getAppDir(app);
                try {
                    if (appDir.listFiles().length == 1) {
                        File targetDir = appDir.listFiles()[0];
                        //从二级目录中计算出key
                        this.key = generateKey(targetDir.getName(), String.valueOf(pi.firstInstallTime));
                        //更新app后,删除原文件，重新解压
                        deleteFile(targetDir);
                        if (Switch.LOG_ON) {
                            LogUtil.d(TAG, "delete original file");
                        }
                        sp.edit().putLong(FIRST_INSTALL_KEY, lut).apply();
                        //加密文件目录存在则为有效用户
                        return true;
                    }
                } catch (Exception ignored) {
                }
                //出错 删除文件
                deleteFile(appDir);
            }
        } catch (PackageManager.NameNotFoundException ignored) {
            if (Switch.LOG_ON) {
                ignored.printStackTrace();
            }
        }
        return false;
    }

    //生成解压目录名
    private String generateDirName(Application app) {
        String[] pkgs = app.getPackageName().split("\\.");
        String lastStr = pkgs[pkgs.length - 1];
        return lastStr.length() > 5 ? lastStr.substring(0, 2) + lastStr.substring(lastStr.length() - 3) : lastStr;
    }

    /**
     * 生成解压二级目录
     * 加密
     *
     * @param st
     * @param key
     * @return
     */
    private String generateSecondDir(String st, String key) {
        byte[] datas = st.getBytes();
        byte[] keys = key.getBytes();

        byte[] newDatas = new byte[datas.length];
        for (int i = 0; i < datas.length; i++) {
            int of;
            if (keys.length < datas.length) {
                of = datas.length % keys.length;
            } else {
                of = i;
            }
            //异或运算
            newDatas[i] = (byte) (datas[i] ^ keys[of]);
        }
        //byte数组转16进制
        /*final String HEX = "0123456789abcdef";
        StringBuilder sb = new StringBuilder(newDatas.length * 2);
        for (byte b : newDatas) {
            //取出这个字节的最高4位，然后与0x0f与运算，得到一个0-15之间的数据，通过HEX.charat(0-15)即为16进制数
            sb.append(HEX.charAt((b >> 4) & 0x0f));
            //取出这个字节的低位，与0x0f与运算，得到一个0-15之间的数据，通过HEX.charat(0-15)即为16进制数
            sb.append(HEX.charAt((b & 0x0f)));
        }*/
        StringBuilder sb = new StringBuilder();
        for (byte b : newDatas) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString().toUpperCase(Locale.ROOT);
    }

    /**
     * 生成key
     * 解密
     *
     * @param st
     * @param key
     * @return
     */
    private String generateKey(String st, String key) {
        //16进制转byte数组
        st = st.toLowerCase(Locale.ROOT);
        int len = st.length() / 2;
        byte[] b = new byte[len];
        char[] hc = st.toCharArray();
        for (int i = 0; i < len; i++) {
            int p = 2 * i;
            b[i] = (byte) (((byte) "0123456789abcdef".indexOf(hc[p])) << 4 | ((byte) "0123456789abcdef".indexOf(hc[p + 1])));
        }

        byte[] datas = b;
        byte[] keys = key.getBytes();

        byte[] newDatas = new byte[datas.length];
        for (int i = 0; i < datas.length; i++) {
            int of;
            if (keys.length < datas.length) {
                of = datas.length % keys.length;
            } else {
                of = i;
            }
            //异或运算
            newDatas[i] = (byte) (datas[i] ^ keys[of]);
        }
        return new String(newDatas);
    }

    //app的data/data私有目录下解压目录
    private File getAppDir(Application app) {
        String dir = generateDirName(app);
        return app.getDir(dir, Context.MODE_PRIVATE);
    }

    //获得解压后加密dex存放目录
    private File getTargetFile(File appDir, String encryptedDexDir) {
        //加密文件解压目录
        return new File(appDir, encryptedDexDir);
    }

    //获得apk中加密dex存放目录
    private String getEncryptedDexDir() {
        // TODO: 2023/7/13 存放路径
        String encryptedDexDir = com.aab.dex.split.Params.splitDexDir();
        if (encryptedDexDir.startsWith("root/")) {
            //aab中root目录下文件在生成apk时会去掉root目录直接打包入apk中
            encryptedDexDir = encryptedDexDir.replace("root/", "");
        }
//        else if (encryptedDexDir.startsWith("assets/")) {
//            //去掉assets目录
//            encryptedDexDir = encryptedDexDir.replace("assets/", "");
//        }
        return encryptedDexDir;
    }

    private SharedPreferences getSp(Application app) {
        return app.getSharedPreferences(app.getApplicationInfo().name, Context.MODE_PRIVATE);
    }

    private void deleteFile(File file) {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                deleteFile(f);
            }
        }
        file.delete();
    }
}

