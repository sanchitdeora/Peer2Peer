/**
 *
 *  Operations involving byte arrays
 *
 */

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class ByteArray {


    static byte[] joinByteArrays(byte[] one, byte[] two) {

        byte[] res = new byte[one.length + two.length];
        System.arraycopy(one, 0, res, 0, one.length);
        System.arraycopy(two, 0, res, one.length, two.length);

        return res;

    }

    static byte[] joinByteArrayWithByte(byte[] one, byte two) {

        byte[] res = new byte[one.length + 1];
        System.arraycopy(one, 0, res, 0, one.length);
        res[one.length] = two;

        return res;

    }

    static byte[] intToByteArray(int value) {

        byte[] ret = new byte[4];

        ret[0] = (byte) ((value >> 24) & 0xFF);
        ret[1] = (byte) ((value >> 16) & 0xFF);
        ret[2] = (byte) ((value >> 8) & 0xFF);
        ret[3] = (byte) (value & 0xFF);

        return ret;

    }

    static int byteArrayToInt(byte[] byteArray) {

        int ret0 = ((byteArray[0] & 0xFF) << 24);
        int ret1 = ((byteArray[1] & 0xFF) << 16);
        int ret2 = ((byteArray[2] & 0xFF) << 8);
        int ret3 = (byteArray[3] & 0xFF);
        return ret0 | ret1 | ret2 | ret3;

    }

    static byte[] booleanArraytoByteArray(AtomicBoolean[] booleanArray) {

        byte[] byteArray = new byte[booleanArray.length];
        Arrays.fill(byteArray, (byte)0);

        for (int ind = 0; ind < booleanArray.length; ind++) {
            if (booleanArray[ind].get()) {

                byteArray[ind / 8] |= 1 << (7 - (ind % 8));

            }

        }

        return byteArray;
    }
}