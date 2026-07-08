package doyensec.protospector.grpc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.utilities.Base64Utils;

public class GrpcUtils {
    static public byte[] intToBigEndian(int intVal) {
        ByteBuffer sizeBuffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
        sizeBuffer.putInt(intVal);
        return sizeBuffer.array();
    }

    static public int intFromBigEndian(byte[] bytes) {
        // rlibby, https://stackoverflow.com/a/5616241
        return java.nio.ByteBuffer.wrap(bytes).getInt();
    }

    static public ByteArray decodeConcatedBase64(String concatedBase64, Base64Utils decoder)
            throws IOException, IllegalArgumentException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        String[] parts = concatedBase64.split("=");
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            // The decoder works without padding
            // throws IllegalArgumentException on bad input
            result.write(decoder.decode(part).getBytes());
        }
        return ByteArray.byteArray(result.toByteArray());
    }
}
