package lu212.sysStats.General;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.nio.file.Path;
import java.util.Map;

public class QRCodeUtil {
    public static void generateQRCode(String text, Path outputPath, int width, int height) throws Exception {
        QRCodeWriter qrWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrWriter.encode(text, BarcodeFormat.QR_CODE, width, height,
                Map.of(EncodeHintType.MARGIN, 1));
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", outputPath);
    }
}
