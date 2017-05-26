package com.github.gnastnosaj.pandora.util;

import com.facebook.common.util.Hex;

import java.io.FileInputStream;
import java.util.HashMap;

/**
 * Created by jasontsang on 5/26/17.
 */

public class FileTypeUtil {

    public static final HashMap<String, String> mFileTypes = new HashMap<>();

    static {
        // images
        mFileTypes.put("FFD8FFE0", "jpg");
        mFileTypes.put("89504E47", "png");
        mFileTypes.put("47494638", "gif");
        mFileTypes.put("49492A00", "tif");
        mFileTypes.put("424D", "bmp");

        mFileTypes.put("41433130", "dwg"); // CAD
        mFileTypes.put("38425053", "psd");
        mFileTypes.put("7B5C727466", "rtf"); // 日记本
        mFileTypes.put("3C3F786D6C", "xml");
        mFileTypes.put("68746D6C3E", "html");
        mFileTypes.put("44656C69766572792D646174653A", "eml"); // 邮件
        mFileTypes.put("D0CF11E0", "doc");
        mFileTypes.put("D0CF11E0", "xls");//excel2003版本文件
        mFileTypes.put("5374616E64617264204A", "mdb");
        mFileTypes.put("252150532D41646F6265", "ps");
        mFileTypes.put("255044462D312E", "pdf");
        mFileTypes.put("504B0304", "docx");
        mFileTypes.put("504B0304", "xlsx");//excel2007以上版本文件
        mFileTypes.put("52617221", "rar");
        mFileTypes.put("57415645", "wav");
        mFileTypes.put("41564920", "avi");
        mFileTypes.put("2E524D46", "rm");
        mFileTypes.put("000001BA", "mpg");
        mFileTypes.put("000001B3", "mpg");
        mFileTypes.put("6D6F6F76", "mov");
        mFileTypes.put("3026B2758E66CF11", "asf");
        mFileTypes.put("4D546864", "mid");
        mFileTypes.put("1F8B08", "gz");
    }

    public static String getFileType(String filePath) {
        return mFileTypes.get(getFileHeader(filePath));
    }

    public static String getFileTypeByFileInputStream(FileInputStream is) {
        return mFileTypes.get(getFileHeaderByFileInputStream(is));
    }

    public static String getFileHeader(String filePath) {
        FileInputStream is = null;
        String value = null;
        try {
            is = new FileInputStream(filePath);
            byte[] b = new byte[4];
            is.read(b, 0, b.length);
            value = Hex.encodeHex(b, false);
        } catch (Exception e) {
        } finally {
            try {
                is.close();
            } catch (Exception e) {
            }
        }
        return value;
    }

    public static String getFileHeaderByFileInputStream(FileInputStream is) {
        String value = null;
        try {
            byte[] b = new byte[4];
            is.read(b, 0, b.length);
            value = Hex.encodeHex(b, false);
        } catch (Exception e) {
        } finally {
            try {
                is.close();
            } catch (Exception e) {
            }
        }
        return value;
    }
}
