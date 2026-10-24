package com.hundunlab.site.common.util;

import java.security.SecureRandom;

public class RandomCodeUtil {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ENGLISH_CODE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static String makeComplexCode(int length) {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            //情况一 塞数字
            if (RANDOM.nextInt(10) <= 4) {
                code.append(getRandomSingleInt());
            }
            //情况二 塞字母
            else {
                code.append(getRandomSingleChar());
            }
        }

        return code.toString();
    }

    public static int getRandomSingleInt() {
        return RANDOM.nextInt(10);
    }

    public static char getRandomSingleChar() {
        //随机拿到一个字母
        int which = RANDOM.nextInt(ENGLISH_CODE.length());
        char code = ENGLISH_CODE.charAt(which);
        //随机大小写
        if (RANDOM.nextBoolean()) {
            code = (char) (code + 32);
        }

        return code;
    }
}
