package com.aab.resguard;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.util.Log;

import com.aab.resguard.reflect.LoadedApk;
import com.aab.resguard.reflect.ResourcesManager;
import com.aab.resguard.util.AabResGuardDbgSwitch;
import com.aab.resguard.util.TimeUtil;
import com.sun.dex.core.ConcurrencyEncDecHelper;
import com.sun.dex.core.EncDecHelper;
import com.sun.dex.core.crypto.DiscreteMapper;
import com.sun.dex.core.noise.NoiseProcessor;
import com.sun.dex.core.util.HexStrUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public class AppExtResManager extends ContextWrapper {
    private final static String TAG  = "AppExtResM";

    /**
     * 编译时，插件会根据配置信息，判断是否有资源被抽取，修改该函数的返回值。切勿修改该函数的名字和signature
     * @return
     */
    public static  boolean hasSplitRes(){
//        return com.aab.res.MaskParams.hasSplitRes();
        return true;
    }

    private static volatile AppExtResManager inst;

    public static void init(Application app){
        if (inst == null) {
            synchronized (AppExtResManager.class) {
                if (inst == null) {
                    inst = new AppExtResManager(app);
                }
            }
        }
    }

    public static AppExtResManager getInstance() {
        return inst;
    }

    private final Application app;
    private final String[] extensions = new String[]{ "png", "jpeg", "bmp", "gif", "mp3",  "wav", "ogg",  "jar", "rar", "zip", "xml", "webp", "js", "config", "db", "czl" };


    private final long apkLastModifiedTs;
    //todo, rename to baseApkPath
    private final String appSourceDir;//data/app/com.android.resguard.demo-2/base.apk

    private final String decodedResFilePath;//data/user/0/com.android.resguard.demo/file/ext
    private final String recordFilePath;

    private AppExtResManager(Application base) {
        super(base);
        app = base;

        appSourceDir = app.getApplicationInfo().sourceDir;
        apkLastModifiedTs = new File(getApplicationInfo().sourceDir).lastModified();

        String pkgName = getPackageName();
        long firstInstallTime = -1 ;
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            firstInstallTime = pi.firstInstallTime;
        } catch (Throwable th){}

        recordFilePath = new File(getFilesDir(), generateFileName(pkgName, firstInstallTime, "resRecord")).getAbsolutePath();
        decodedResFilePath = new File(getFilesDir(), generateFileName(pkgName, firstInstallTime,"extRes")).getAbsolutePath() ;
    }

    /**
     * 根据bundle_id和first_install_time(即使升级也不会改变)决定文件名字，各个进程根据相同的规则得出的文件名总是一样。
     * 1.根据bundle_id, firstInstallTime, origFileName生成MD5
     * 2.从md5开头偏移len(origFileName), md5^origFileName^0xA5
     * 3.a 每个字节如果在【a-z|A-Z|-\_]内，直接使用
     * 3.b read the code
     * 3.c read the code
     * @param pkgName
     * @param firstInstallTime
     * @param origFileName
     * @return
     */
    private String generateFileName(String pkgName, long firstInstallTime, String origFileName){
        byte[] origFileName_b = origFileName.getBytes(StandardCharsets.UTF_8);
        int origNameSize = origFileName_b.length;

        byte[] digest;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update((pkgName+firstInstallTime+origFileName).getBytes(StandardCharsets.UTF_8));
            digest = md5.digest();
        } catch (Throwable e){
            byte[] pkg_b = pkgName.getBytes(StandardCharsets.UTF_8);
            byte[] ts_b = String.valueOf(firstInstallTime).getBytes(StandardCharsets.UTF_8);

            digest = new byte[16];
            for (int i = 0; i < digest.length; i++) {
                digest[i] = (byte) (pkg_b[i%pkg_b.length]
                        ^ ts_b[ts_b.length-1-i%ts_b.length]
                        ^ origFileName_b[origFileName_b.length-1-i%origFileName_b.length] );
            }
        }

        StringBuilder builder = new StringBuilder();
        long nameSize = (origNameSize+firstInstallTime)%9+4 ;
        for (int i = 0; i < nameSize; i++) {
            int b = digest[(origNameSize+i)%digest.length] ^ origFileName_b[i % origNameSize] ^ 0xA5;

            if ((b>='a' && b<='z') || (b>='A' && b<='Z') || ('-'==b) || ('_'==b)){
                builder.append((char)b);
            } else if(i>0 && (firstInstallTime+i+nameSize)%2==0){
                builder.append((byte)b);
            } else {
                int h_b = (b>>4) & 0xF;
                char h_c= (char)(h_b<10 ? '0'+h_b : h_b-10+'a');
                int l_b = b & 0xF;
                char l_c = (char)(l_b<10 ? '0'+l_b : l_b-10+'a');

                if (((nameSize+i) & 0x1)==1) {
                    builder.append(h_c).append(l_c);
                } else {
                    builder.append(l_c).append(h_c);
                }
            }
        }

        String fileName = builder.toString();
        if (AabResGuardDbgSwitch.LOG_ENABLE) {
            Log.d(TAG, "file name," + origFileName + "->" + fileName);
        }

        return fileName;
    }

    ////////////////////////从apk解密res到本地文件////////////////////////////////////////////////////
    /**
     * 解密资源，同步操作
     */
    public void decryptRes(){
        try(RandomAccessFile recordFile = new RandomAccessFile(recordFilePath, "rw")) {
            FileLock exclLock = recordFile.getChannel().tryLock();
            if (exclLock != null) {
                long recApkLastModifiedTs = -1 ;
                long recResFileSize = -1;
                try {
                    if (recordFile.length() > 1) {
                        recApkLastModifiedTs = Long.parseLong(recordFile.readUTF());
                        recResFileSize = Long.parseLong(recordFile.readUTF());
                    }
                } catch (Throwable ignored) {
                    if (AabResGuardDbgSwitch.LOG_ENABLE) ignored.printStackTrace();
                }

                boolean decode = true;
                File resFile = new File(decodedResFilePath);
                do {
                    if (recApkLastModifiedTs != apkLastModifiedTs)
                        break;

                    if (!resFile.isFile() || recResFileSize != resFile.length())
                        break;

                    decode = false;
                } while (false);

                if (decode) {
                    long decodedResFileSize = decryptResCore(resFile, com.aab.res.MaskParams.getResKey(), com.aab.res.MaskParams.getResFilesApkPath());
                    if (decodedResFileSize>0) {
                        if (AabResGuardDbgSwitch.LOG_ENABLE) {
                            Log.d(TAG, "资源解密完成, apk last modified TS=" + apkLastModifiedTs + ", res file size=" + decodedResFileSize);
                        }

                        recordFile.setLength(0);
                        recordFile.writeUTF(String.valueOf(apkLastModifiedTs));
                        recordFile.writeUTF(String.valueOf(decodedResFileSize));

                        //random data
                        StringBuilder builder = new StringBuilder();
                        Random rnd = new Random();
                        int randomDataSize = 500 + rnd.nextInt(2000);
                        for (int i = 0; i < randomDataSize; i++) {
                            builder.append((char) ('0' + rnd.nextInt(10)));
                        }
                        recordFile.writeBytes(builder.toString());
                    } else {
                        resFile.delete();
                    }
                }
                exclLock.release();

            } else {
                if (AabResGuardDbgSwitch.LOG_ENABLE) {
                    Log.d(TAG, "another process is decoding the res file, wait for lock....");
                }
                try (FileChannel readChan = new FileInputStream(recordFilePath).getChannel()) {
                    FileLock shLock = readChan.lock();
                    shLock.release();
                    if (AabResGuardDbgSwitch.LOG_ENABLE) {
                        Log.d(TAG, "res file decrypt is completed by other process.");
                    }
                } catch (Throwable ignored) {if (AabResGuardDbgSwitch.LOG_ENABLE) ignored.printStackTrace();}
            }
        } catch (Throwable ig){if (AabResGuardDbgSwitch.LOG_ENABLE)ig.printStackTrace();}
    }

    /**
     * 解密资源，异步操作
     */
    private Thread decryptThread = null;
    private final AtomicBoolean decryptResAsyncCompleted = new AtomicBoolean(false);
    public void decryptResAsync(){
        if (decryptThread==null){
            decryptThread = new Thread(){
                public void run(){
                    decryptRes();
                    synchronized (decryptResAsyncCompleted){
                        decryptResAsyncCompleted.set(true);
                        decryptResAsyncCompleted.notifyAll();
                    }
                }
            };

            decryptThread.setPriority(Thread.MAX_PRIORITY);
            decryptThread.start();
        }
    }

    /**
     * synchronized(lock){//获取对象的monitor lock
     *     xxxx
     *     lock.notify()  //只是把正在等待的线程从队列的移除，这些线程不会立马得到执行，他们还需要获取对象的monitor lock
     *     xxxxxxx        //为了尽快释放monitor lock，让等待的线程快速得到机会运行，设计逻辑时会去掉这部分代码。
     * } //此处才释放对象的monitor lock
     *
     *
     * synchronized(lock){/获取对象的monitor lock
     *     xxx
     *     wait() //释放monitor lock，进入等待队列。这样另外一个线程才有机会进入synchronized，然后执行notify。当返回时，也同时重新得到monitor lock
     *     xxxx
     * }
     */

    /**
     * 等待res解密异步完成
     */
    public void waitForResDecryptAsyncCompleted(){
        if (decryptThread!=null){
            synchronized (decryptResAsyncCompleted){
                while (!decryptResAsyncCompleted.get()) {
                    try { decryptResAsyncCompleted.wait(); } catch (InterruptedException ignored) {}
                }
            }
        }
    }



    /**
     * 解密资源文件
     * @param resFile 解密后的文件
     * @param encodedResPath apk文件中，被加密的文件(.arsc, res/xx/xx, assets/xxx)文件路径
     * @return 解密后的resFile大小
     */
    @SuppressLint("RestrictedApi")
    private long decryptResCore(File resFile, String key, String encodedResPath) {
        if ((resFile==null) || (encodedResPath==null) || (key == null))
            return -1;

        int writtenEntries = 0;
        try (
                ZipFile inZipFile = new ZipFile(app.getApplicationInfo().publicSourceDir);
                ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(resFile)))
        ) {
            zos.setMethod(ZipOutputStream.STORED);
            zos.setLevel(Deflater.NO_COMPRESSION);
            NoiseProcessor np = new NoiseProcessor(DiscreteMapper.getInstance().map2Long(key));


            ZipEntry encodedresfiles = inZipFile.getEntry(encodedResPath);
            if(encodedresfiles!=null){
                try(BufferedInputStream bis = new BufferedInputStream(inZipFile.getInputStream(encodedresfiles))){
                    byte[] dataEncoded= new byte[(int) encodedresfiles.getSize()];
                    int offset = 0;
                    while (offset<dataEncoded.length)
                        offset += bis.read(dataEncoded, offset, dataEncoded.length-offset);

                    byte[] dataDecrypted;
                    if(AabResGuardDbgSwitch.IS_ENC_ENABLE){
                        //电话线换光纤，单车变摩托
                        //加密和解密时，按至少 blockSize 大小切块
                        //切成至少 解密时CPU核心数块，16~32块较好，目前常见机型核心数一般4~8核
                        if(AabResGuardDbgSwitch.LOG_ENABLE) {
                            Log.i(TAG, "DR1-0 " + HexStrUtil.toHexString(DiscreteMapper.getInstance().map2Bytes(dataEncoded)));
                            Log.i(TAG, "DR1-1 " + EncDecHelper.CIPHERS[EncDecHelper.getCipherIndexByKey(key)] + ", " + dataEncoded.length);
                            TimeUtil.start();
                        }


                        dataDecrypted = np.removeNoise(dataEncoded);
                        if(AabResGuardDbgSwitch.LOG_ENABLE) {
                            Log.i(TAG, "DR1-2 " + TimeUtil.end() + ", " + dataDecrypted.length + ", " + dataDecrypted.length / (TimeUtil.watch() + 1));
                            TimeUtil.start();
                        }

//                    byte[] dataDecoded = CryptoUtil.decAndRemovePadding(BuildConfig.key, dataEncoded);
                        dataDecrypted = ConcurrencyEncDecHelper.decAndRemovePadding(key, dataDecrypted);
                        if(AabResGuardDbgSwitch.LOG_ENABLE)
                            Log.i(TAG, "DR1-3 " + TimeUtil.end() + ", " + dataDecrypted.length + ", " + dataDecrypted.length/(TimeUtil.watch()+1));

//                    byte[] dataDecoded = ConcurrencyEncDecHelper.encDecAndPadding(BuildConfig.key, dataEncoded, 32,Runtime.getRuntime().availableProcessors()+1, false);
//                    byte[] dataDecrypted = ConcurrencyEncDecHelper.decAndRemovePadding(BuildConfig.key, dataEncoded);
//                    byte[] dataDecoded = np.removeNoise(dataDecrypted

                        dataEncoded = null;
                        System.gc(); //防止低内存设备OOM
                    }else{
                        dataDecrypted = dataEncoded;
                    }

                    try(ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(dataDecrypted))){
                        ZipEntry inResEntry;
                        while ((inResEntry= zis.getNextEntry()) != null){
                            try(ByteArrayOutputStream bos = new ByteArrayOutputStream()){
                                byte[] buf = new byte[1024*64];
                                int byteRead;
                                while ((byteRead=zis.read(buf)) > 0){
                                    bos.write(buf, 0, byteRead);
                                }

                                byte[] resFileData = bos.toByteArray();
                                ZipEntry outResEntry = new ZipEntry(inResEntry.getName());
                                outResEntry.setMethod(ZipEntry.STORED);
                                outResEntry.setSize(resFileData.length);
                                CRC32 crc32 = new CRC32();
                                crc32.update(resFileData, 0, resFileData.length);
                                outResEntry.setCrc(crc32.getValue());

                                zos.putNextEntry(outResEntry);
                                zos.write(resFileData);
                                zos.closeEntry();

                                writtenEntries += 1;
                            }
                        }
                    }
                } catch (Throwable e) { if (AabResGuardDbgSwitch.LOG_ENABLE) e.printStackTrace();}
            }

            //for api<7.0, the AsssetManager::addAssetPath requires there is a manifest in res file.
            if ((writtenEntries > 0) && (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)){
                ZipEntry manifest = inZipFile.getEntry("AndroidManifest.xml");
                if(manifest != null){
                    try(BufferedInputStream bis = new BufferedInputStream(inZipFile.getInputStream(manifest))){
                        byte[] manifestData = new byte[(int) manifest.getSize()];
                        int offset = 0;
                        while (offset<manifestData.length)
                            offset += bis.read(manifestData, offset, manifestData.length-offset);

                        ZipEntry outEntry = new ZipEntry("AndroidManifest.xml");
                        outEntry.setMethod(ZipEntry.STORED);
                        outEntry.setSize(manifestData.length);
                        CRC32 crc32 = new CRC32();
                        crc32.update(manifestData, 0, manifestData.length);
                        outEntry.setCrc(crc32.getValue());

                        zos.putNextEntry(outEntry);
                        zos.write(manifestData);
                        zos.closeEntry();

                        writtenEntries += 1;
                    }
                }
            }


            if (writtenEntries > 0)
                zos.finish();
        } catch (IOException e) { if (AabResGuardDbgSwitch.LOG_ENABLE) e.printStackTrace();}


        if(writtenEntries==0 || !resFile.isFile() || resFile.length()==0)
            return -1;
        return resFile.length();
    }



    //////////////////////////////从本地文件加载资源///////////////////////////////////////////////////
    /**
     * 同步加载资源
     */
    public void loadRes(){
        File resFile = new File(decodedResFilePath);
        if (!resFile.isFile() || resFile.length()==0)
            return;

        loadRes_newEntryPoint();
    }

    private Thread loadResThread;
    private final AtomicBoolean loadResAsyncCompleted = new AtomicBoolean(false);
    /**
     * 异步加载资源
     */
    public void loadResAsync(){
        if (loadResThread==null){
            loadResThread = new Thread(){
                @Override
                public void run() {
                    loadRes();
                    synchronized (loadResAsyncCompleted){
                        loadResAsyncCompleted.set(true);
                        loadResAsyncCompleted.notifyAll();
                    }
                }
            };

            loadResThread.setPriority(Thread.MAX_PRIORITY);
            loadResThread.start();
        }
    }

    public void waitForResLoadAsyncCompleted(){
        if (loadResThread!=null){
            synchronized (loadResAsyncCompleted){
                while (!loadResAsyncCompleted.get()){
                    try { loadResAsyncCompleted.wait(); } catch (InterruptedException ignored) {}
                }
            }
        }
    }



    private void loadRes_newEntryPoint(){
        //1.loadedApk
        LoadedApk loadedApk = new LoadedApk(getPackageName());
        loadedApk.hook();


        //app related
        ResourcesHookerUtil.addAssetPath(getResources(), true);
        hookAppInfo(getApplicationInfo());

        //refresh all allocated res
        ResourcesManager.getInstance().refreshInMemoryResources();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //hook ResourcesManager
            ResourcesManager.getInstance().hook();
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////



    public void hookAppInfo(ApplicationInfo appInfo){
        if (appInfo == null)
            return;

        String[] splitSourceDirs = appInfo.splitSourceDirs;
        boolean hasAppended = false;
        if (splitSourceDirs!=null){
            for (String resDir : splitSourceDirs){
                if (resDir != null && resDir.equals(getDecodedResPath())){
                    hasAppended = true;
                    break;
                }
            }
        }
        if(!hasAppended){
            if(splitSourceDirs==null){
                splitSourceDirs = new String[]{getDecodedResPath()};
            } else {
                String[] tmp = new String[splitSourceDirs.length+1];
                System.arraycopy(splitSourceDirs, 0, tmp, 0, splitSourceDirs.length);
                tmp[tmp.length - 1] = getDecodedResPath();
                splitSourceDirs = tmp;
            }
            appInfo.splitSourceDirs = splitSourceDirs;
        }
    }

    public String getDecodedResPath(){
        return decodedResFilePath;
    }

    public String getAppSourceDir(){
        return appSourceDir;
    }
}
