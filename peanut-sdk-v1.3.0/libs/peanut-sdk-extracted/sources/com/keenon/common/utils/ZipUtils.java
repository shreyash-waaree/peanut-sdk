package com.keenon.common.utils;

import com.keenon.common.constant.PeanutConstants;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/ZipUtils.class */
public class ZipUtils {
    private static final String TAG = "ZipUtils";

    public static boolean ZipFolder(String srcFileString, String zipFileName, String zipFileString) throws Exception {
        createFile(zipFileString);
        File file1 = new File(zipFileString + zipFileName);
        FileOutputStream fileOutputStream = new FileOutputStream(file1);
        ZipOutputStream outZip = new ZipOutputStream(fileOutputStream);
        File file = new File(srcFileString);
        boolean isSuccess = ZipFiles(file.getParent() + File.separator, file.getName(), outZip);
        outZip.finish();
        outZip.close();
        return isSuccess;
    }

    public static void createFile(String zipFileString) {
        File file = new File(zipFileString);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                LogUtils.e(PeanutConstants.TAG_UTIL, "[ZipUtils][createFile][error : " + e.getMessage() + "]");
                e.printStackTrace();
            }
        }
    }

    private static boolean ZipFiles(String folderString, String fileString, ZipOutputStream zipOutputSteam) {
        if (zipOutputSteam == null) {
            return false;
        }
        File file = new File(folderString + fileString);
        if (file.isFile()) {
            ZipEntry zipEntry = new ZipEntry(fileString);
            try {
                FileInputStream inputStream = new FileInputStream(file);
                zipOutputSteam.putNextEntry(zipEntry);
                byte[] buffer = new byte[4096];
                while (true) {
                    int len = inputStream.read(buffer);
                    if (len != -1) {
                        zipOutputSteam.write(buffer, 0, len);
                    } else {
                        zipOutputSteam.closeEntry();
                        return true;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                LogUtils.e(PeanutConstants.TAG_UTIL, "[ZipUtils][ZipFiles][error : " + e.getMessage() + "]");
                return false;
            }
        } else {
            String[] fileList = file.list();
            if (fileList == null) {
                LogUtils.e(PeanutConstants.TAG_UTIL, "[ZipUtils][ZipFiles][fileList is null]");
                return false;
            }
            if (fileList.length <= 0) {
                ZipEntry zipEntry2 = new ZipEntry(fileString + File.separator);
                try {
                    zipOutputSteam.putNextEntry(zipEntry2);
                    zipOutputSteam.closeEntry();
                } catch (IOException e2) {
                    e2.printStackTrace();
                    LogUtils.e(PeanutConstants.TAG_UTIL, "[ZipUtils][ZipFiles][error : " + e2.getMessage() + "]");
                    return false;
                }
            }
            for (String str : fileList) {
                ZipFiles(folderString, fileString + File.separator + str, zipOutputSteam);
            }
            return true;
        }
    }

    public static boolean unzip(String zipFilePath, String desDirectory) {
        File desDir = new File(desDirectory);
        if (!desDir.exists()) {
            boolean mkdirSuccess = desDir.mkdir();
            if (!mkdirSuccess) {
                return false;
            }
        }
        try {
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath));
            for (ZipEntry zipEntry = zipInputStream.getNextEntry(); zipEntry != null; zipEntry = zipInputStream.getNextEntry()) {
                if (zipEntry.isDirectory()) {
                    mkdir(new File(desDirectory + File.separator + zipEntry.getName()));
                } else {
                    String unzipFilePath = desDirectory + File.separator + zipEntry.getName();
                    File file = new File(unzipFilePath);
                    mkdir(file.getParentFile());
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(unzipFilePath));
                    byte[] bytes = new byte[1024];
                    while (true) {
                        int readLen = zipInputStream.read(bytes);
                        if (readLen == -1) {
                            break;
                        }
                        bufferedOutputStream.write(bytes, 0, readLen);
                    }
                    bufferedOutputStream.close();
                }
                zipInputStream.closeEntry();
            }
            zipInputStream.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void mkdir(File file) {
        if (null == file || file.exists()) {
            return;
        }
        mkdir(file.getParentFile());
        file.mkdir();
    }
}
