package com.vt.sdkres.dex.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtil {

    private static final String TAG = ZipUtil.class.getSimpleName();

    private static class SINGLETON {
        private static final ZipUtil INSTANCE = new ZipUtil();
    }

    public static ZipUtil getInstance() {
        return SINGLETON.INSTANCE;
    }

    /**
     * 解压zip文件至dir目录
     *
     * @param zip 压缩包文件
     * @param dir 目录
     */
    public void unZip(File zip, File dir) {
        try {
            deleteFile(dir);
            ZipFile zipFile = new ZipFile(zip);
            //zip文件中每一个条目
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                //zip中 文件/目录名
                String name = zipEntry.getName();
                //原来的签名文件 不需要了
                if (name.equals("META-INF/CERT.RSA") || name.equals("META-INF/CERT.SF") ||
                        name.equals("META-INF/MANIFEST.MF")) {
                    continue;
                }
                //空目录不管
                if (!zipEntry.isDirectory()) {
                    File file = new File(dir, name);
                    //创建目录
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    //写文件
                    FileOutputStream fos = new FileOutputStream(file);
                    InputStream is = zipFile.getInputStream(zipEntry);
                    byte[] buffer = new byte[2048];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                    is.close();
                    fos.close();
                }
            }
            zipFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 解压指定目录下的文件
     *
     * @param zip       压缩文件
     * @param dir       解压目录
     * @param targetDir 指定目录
     */
    public void unZipTargetFile(File zip, File dir,String targetDir) {
        if (!dir.exists()) {
            dir.mkdirs();
        } else {
            if(dir.listFiles().length > 0){
                //目录已解压
                return;
            }
        }
        try {
//            LogUtil.d("ttst","tg:"+targetDir);
            ZipFile zipFile = new ZipFile(zip);
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zip));
            byte[] buffer = new byte[1024 * 1024];
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                String fileName = zipEntry.getName();
                //指定目录下文件
                if (fileName != null && fileName.contains(targetDir)) {
//                    LogUtil.d("ttst","fileName:"+fileName);
//                    LogUtil.d("ttst","replace:"+targetDir+File.separator);
                    fileName = fileName.replace(targetDir+File.separator,"");
//                    LogUtil.d("ttst","fileName:"+fileName);
                    //非目录
                    if (!zipEntry.isDirectory()) {
                        File file = new File(dir, fileName);
//                        LogUtil.d("ttst","file:"+file);
                        //创建目录
                        if (file.getParentFile() != null && !file.getParentFile().exists()) {
                            file.getParentFile().mkdirs();
                        }
                        //写文件
                        FileOutputStream fos = new FileOutputStream(file);
                        InputStream is = zipFile.getInputStream(zipEntry);
                        int len;
                        while ((len = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, len);
                        }
                        fos.close();
                        is.close();
                    }
                }
            }
            zipInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int unZipFileAndGetDexNum(File zip, File dir, String targetDir) {
        if (!dir.exists()) {
            dir.mkdirs();
        } else {
            for (File f : dir.listFiles()) {
                if (f.getName().contains(targetDir)) {
                    //目标目录已解压
                    return 0;
                }
            }
        }
        try {
            ZipFile zipFile = new ZipFile(zip);
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zip));
            byte[] buffer = new byte[1024 * 1024];
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            int size = 0;
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                String fileName = zipEntry.getName();
                if (fileName.contains("classes") && fileName.endsWith(".dex")) {
                    size++;
                }
                //指定目录下文件
                if (fileName != null && fileName.contains(targetDir)) {
                    //非目录
                    if (!zipEntry.isDirectory()) {
                        File file = new File(dir, fileName);
                        //创建目录
                        if (file.getParentFile() != null && !file.getParentFile().exists()) {
                            file.getParentFile().mkdirs();
                        }
                        //写文件
                        FileOutputStream fos = new FileOutputStream(file);
                        InputStream is = zipFile.getInputStream(zipEntry);
                        int len;
                        while ((len = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, len);
                        }
                        fos.close();
                        is.close();
                    }
                }
            }
            zipInputStream.close();
            return size;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void deleteFile(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteFile(f);
                }
            } else {
                file.delete();
            }
        } else {
            file.delete();
        }
    }

    /**
     * 压缩目录为zip
     *
     * @param dir 待压缩目录
     * @param zip 输出zip文件
     */
    public void zip(File dir, File zip) {
        try {
            zip.delete();
            // 对输出文件做CRC32校验
            CheckedOutputStream cos = new CheckedOutputStream(new FileOutputStream(zip), new CRC32());
            ZipOutputStream zos = new ZipOutputStream(cos);
            //压缩
            compress(dir, zos, "");
            zos.flush();
            zos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 添加目录/文件 至zip中
     *
     * @param srcFile  需要添加的目录/文件
     * @param zos      zip输出流
     * @param basePath 递归子目录时的完整目录 如lib/x86
     */
    private void compress(File srcFile, ZipOutputStream zos, String basePath) {
        if (srcFile.isDirectory()) {
            File[] files = srcFile.listFiles();
            for (File f : files) {
                // zip 递归添加目录中的文件
                compress(f, zos, basePath + srcFile.getName() + "/");
            }
        } else {
            compressFile(srcFile, zos, basePath);
        }
    }

    private void compressFile(File file, ZipOutputStream zos, String dir) {
        try {
            // temp/lib/x86/libdn_ssl.so
            String fullName = dir + file.getName();
            // 需要去掉temp
            String[] fileNames = fullName.split("/");
            // 正确的文件目录名 (去掉了temp)
            StringBuffer sb = new StringBuffer();
            if (fileNames.length > 1) {
                for (int i = 1; i < fileNames.length; i++) {
                    sb.append("/");
                    sb.append(fileNames[i]);
                }
            } else {
                sb.append("/");
            }
            // 添加一个zip条目
            ZipEntry entry = new ZipEntry(sb.substring(1));
            zos.putNextEntry(entry);
            FileInputStream fis = new FileInputStream(file);
            int len;
            byte[] data = new byte[2048];
            while ((len = fis.read(data, 0, 2048)) != -1) {
                zos.write(data, 0, len);
            }
            fis.close();
            zos.closeEntry();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
