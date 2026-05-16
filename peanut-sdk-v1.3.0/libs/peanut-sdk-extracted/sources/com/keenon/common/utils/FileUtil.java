package com.keenon.common.utils;

import android.content.Context;
import android.os.Environment;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.sdk.http.ws.WsStatus;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/FileUtil.class */
public class FileUtil {
    private static final String TAG = "[FileUtil]";
    public static String BASE_FILE_DIR;
    private static StringBuffer sb = new StringBuffer();
    private static volatile FileUtil mInstance;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/FileUtil$FileUnit.class */
    public enum FileUnit {
        KB,
        MB,
        GB
    }

    public static FileUtil getInstance() {
        if (null == mInstance) {
            synchronized (FileUtil.class) {
                if (null == mInstance) {
                    mInstance = new FileUtil();
                }
            }
        }
        return mInstance;
    }

    public static String getFromAssert(Context context, String name) {
        try {
            InputStreamReader inputReader = new InputStreamReader(context.getAssets().open(name));
            BufferedReader bufReader = new BufferedReader(inputReader);
            String Result = "";
            while (true) {
                String line = bufReader.readLine();
                if (line != null) {
                    Result = Result + line;
                } else {
                    return Result;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static <T> T getAssetsObject(Context context, String str, Class<T> cls) throws IOException {
        InputStream inputStreamOpen = null;
        BufferedReader bufferedReader = null;
        try {
            inputStreamOpen = context.getAssets().open(str);
            bufferedReader = new BufferedReader(new InputStreamReader(inputStreamOpen, "UTF-8"));
            StringBuilder sb2 = new StringBuilder();
            while (true) {
                String line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }
                sb2.append(line);
            }
            T t = (T) GsonUtil.gson2Bean(sb2.toString(), (Class) cls);
            if (inputStreamOpen != null) {
                inputStreamOpen.close();
            }
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            return t;
        } catch (Throwable th) {
            if (inputStreamOpen != null) {
                inputStreamOpen.close();
            }
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            throw th;
        }
    }

    public static File[] getFiles(String path) {
        File file = new File(path);
        File[] files = file.listFiles();
        return files;
    }

    public static synchronized File save2sdcard(String crashReport, String dirPath, String fileName) {
        sb.append("/r/n");
        sb.append(crashReport);
        if (Environment.getExternalStorageState().equals("mounted")) {
            try {
                File dir = new File(dirPath);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File file = new File(dirPath, fileName);
                if (!file.exists()) {
                    file.createNewFile();
                }
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(crashReport.toString().getBytes());
                fos.close();
                return file;
            } catch (FileNotFoundException e) {
                LogUtils.e(PeanutConstants.TAG_UTIL, TAG, (Exception) e);
                return null;
            } catch (IOException e2) {
                LogUtils.e(PeanutConstants.TAG_UTIL, TAG, (Exception) e2);
                return null;
            }
        }
        return null;
    }

    public static boolean existSdCard() {
        return Environment.getExternalStorageState().equals("mounted");
    }

    public static String getAvailableStoragePath(Context context, boolean save2Sdcard) {
        if (save2Sdcard) {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        return context.getFilesDir().getAbsolutePath();
    }

    public static void deleteDir(File file) {
        if (file.exists()) {
            if (file.isFile()) {
                file.delete();
            } else if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files == null) {
                    return;
                }
                for (File file2 : files) {
                    deleteDir(file2);
                }
            }
            file.delete();
        }
    }

    public static void deleteFile(File file) {
        if (file.exists()) {
            if (file.isFile()) {
                file.delete();
            } else if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files == null) {
                    return;
                }
                for (File file2 : files) {
                    deleteFile(file2);
                }
            }
            file.delete();
        }
    }

    public static long getFileSize(File file) {
        long size = 0;
        try {
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                size = fis.available();
            } else {
                file.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return size;
    }

    public static String getFileMD5(File file) {
        if (!file.exists()) {
            return null;
        }
        byte[] buffer = new byte[1024];
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            FileInputStream in = new FileInputStream(file);
            while (true) {
                int len = in.read(buffer, 0, 1024);
                if (len != -1) {
                    md.update(buffer, 0, len);
                } else {
                    in.close();
                    BigInteger bigInt = new BigInteger(1, md.digest());
                    String s = String.format("%032x", bigInt);
                    return s;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isValidMd5(String md5, String filePath) {
        File file = new File(filePath);
        String fileMD5 = getFileMD5(file);
        if (android.text.TextUtils.isEmpty(fileMD5) || android.text.TextUtils.isEmpty(md5)) {
            LogUtils.e(TAG, "[isValidMd5][empty data]");
            return false;
        }
        LogUtils.d(TAG, "[isValidMd5][zip  : " + fileMD5 + "]");
        LogUtils.d(TAG, "[isValidMd5][info : " + md5 + "]");
        return md5.equals(fileMD5);
    }

    public static boolean isValidMd5(String md5, File file) {
        String fileMD5 = getFileMD5(file);
        if (android.text.TextUtils.isEmpty(fileMD5) || android.text.TextUtils.isEmpty(md5)) {
            LogUtils.e(TAG, "[isValidMd5][empty data]");
            return false;
        }
        LogUtils.d(TAG, "[isValidMd5][file : " + fileMD5 + "]");
        LogUtils.d(TAG, "[isValidMd5][info : " + md5 + "]");
        return md5.equals(fileMD5);
    }

    public static void mkDirIfNotExist(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public static void writeFile(String filePath, String json) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(json);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeFile(String filePath, byte[] data) {
        try {
            FileOutputStream fos = new FileOutputStream(filePath);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(data);
            bos.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean createFile(String targetFile, long fileLength, FileUnit unit) {
        switch (unit) {
            case KB:
                fileLength *= 1024;
                break;
            case MB:
                fileLength = fileLength * 1024 * 1024;
                break;
            case GB:
                fileLength = fileLength * 1024 * 1024 * 1024;
                break;
        }
        FileOutputStream fos = null;
        File file = new File(targetFile);
        try {
            try {
                if (!file.exists()) {
                    file.createNewFile();
                }
                long batchSize = fileLength;
                if (fileLength > 1024) {
                    batchSize = 1024;
                }
                if (fileLength > 1048576) {
                    batchSize = 1048576;
                }
                if (fileLength > 10485760) {
                    batchSize = 10485760;
                }
                long count = fileLength / batchSize;
                long last = fileLength % batchSize;
                fos = new FileOutputStream(file);
                FileChannel fileChannel = fos.getChannel();
                for (int i = 0; i < count; i++) {
                    ByteBuffer buffer = ByteBuffer.allocate((int) batchSize);
                    fileChannel.write(buffer);
                }
                if (last != 0) {
                    ByteBuffer buffer2 = ByteBuffer.allocate((int) last);
                    fileChannel.write(buffer2);
                }
                fos.close();
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            } catch (IOException e2) {
                e2.printStackTrace();
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e3) {
                        e3.printStackTrace();
                        return false;
                    }
                }
                return false;
            }
        } catch (Throwable th) {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e4) {
                    e4.printStackTrace();
                    throw th;
                }
            }
            throw th;
        }
    }

    public static byte[] getBytes(File file) {
        FileInputStream fis;
        ByteArrayOutputStream bos;
        byte[] b;
        byte[] buffer = null;
        try {
            fis = new FileInputStream(file);
            bos = new ByteArrayOutputStream(WsStatus.CODE.NORMAL_CLOSE);
            b = new byte[WsStatus.CODE.NORMAL_CLOSE];
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        while (true) {
            int n = fis.read(b);
            if (n == -1) {
                break;
            }
            bos.write(b, 0, n);
            return buffer;
        }
        fis.close();
        bos.close();
        buffer = bos.toByteArray();
        return buffer;
    }

    public static void createDir(File file) {
        if (!file.exists() && !file.isDirectory()) {
            file.mkdirs();
        }
    }

    public static String readString(File file) {
        BufferedReader br;
        StringBuilder sb2 = new StringBuilder();
        try {
            br = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        while (true) {
            String readLine = br.readLine();
            if (readLine == null) {
                break;
            }
            sb2.append(readLine);
            return sb2.toString();
        }
        br.close();
        return sb2.toString();
    }

    public static void copyFile(File sourceFile, File targetFile) throws IOException {
        FileInputStream input = new FileInputStream(sourceFile);
        BufferedInputStream inBuff = new BufferedInputStream(input);
        FileOutputStream output = new FileOutputStream(targetFile);
        BufferedOutputStream outBuff = new BufferedOutputStream(output);
        byte[] b = new byte[51200];
        while (true) {
            int len = inBuff.read(b);
            if (len != -1) {
                outBuff.write(b, 0, len);
            } else {
                outBuff.flush();
                inBuff.close();
                outBuff.close();
                output.close();
                input.close();
                return;
            }
        }
    }

    public static void compressFile(String filePath, String zipFilePath) throws IOException {
        FileOutputStream fos = new FileOutputStream(zipFilePath);
        ZipOutputStream zos = new ZipOutputStream(fos);
        FileInputStream in = new FileInputStream(filePath);
        ZipEntry zipEntry = new ZipEntry(new File(filePath).getName());
        zos.putNextEntry(zipEntry);
        byte[] buffer = new byte[1024];
        while (true) {
            int length = in.read(buffer);
            if (length > 0) {
                zos.write(buffer, 0, length);
            } else {
                zos.flush();
                fos.getFD().sync();
                zos.closeEntry();
                in.close();
                zos.close();
                fos.close();
                return;
            }
        }
    }

    public static void copyFile(String sourcePath, String targetPath) throws IOException {
        FileInputStream input = null;
        FileOutputStream output = null;
        BufferedInputStream inBuff = null;
        BufferedOutputStream outBuff = null;
        try {
            try {
                input = new FileInputStream(sourcePath);
                inBuff = new BufferedInputStream(input);
                output = new FileOutputStream(targetPath);
                outBuff = new BufferedOutputStream(output);
                byte[] b = new byte[51200];
                while (true) {
                    int len = inBuff.read(b);
                    if (len == -1) {
                        break;
                    } else {
                        outBuff.write(b, 0, len);
                    }
                }
                outBuff.flush();
                output.getFD().sync();
                if (inBuff != null) {
                    inBuff.close();
                }
                if (outBuff != null) {
                    outBuff.close();
                }
                if (output != null) {
                    output.close();
                }
                if (input != null) {
                    input.close();
                }
            } catch (Exception e) {
                LogUtils.e(TAG, "[copyFile]" + e.getMessage());
                throw e;
            }
        } catch (Throwable th) {
            if (inBuff != null) {
                inBuff.close();
            }
            if (outBuff != null) {
                outBuff.close();
            }
            if (output != null) {
                output.close();
            }
            if (input != null) {
                input.close();
            }
            throw th;
        }
    }

    public void init(Context context) {
        if (null != context) {
            BASE_FILE_DIR = context.getFilesDir().getAbsolutePath();
        }
    }
}
