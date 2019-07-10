package io.ffem.lite.barcode;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.Log;
import android.util.SparseArray;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.os.Environment.getExternalStorageDirectory;
import static java.io.File.separator;

public class Utilities {

    // API url where images will be posted for further processing.
    // The server can be hosted using - https://github.com/teamsoo/flask-api-upload-image
    //
    private static final String API_URL = "http://ec2-52-66-17-109.ap-south-1.compute.amazonaws.com:5000";
    private static final String TAG = "Util";

    /**
     * return the timestamp on yyMMdd_hhmmss format
     *
     * @return
     */
    public static String getTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd_hhmmss");
        return sdf.format(new Date());
    }

    /**
     * Saves a specified picture on external disk.
     *
     * @param barcodeValue
     * @param bytes
     * @return
     */
    public static String savePicture(final String barcodeValue, final byte[] bytes) {
        try {
            String mainPath = getExternalStorageDirectory() + separator + "MaskIt" + separator + "images" + separator;
            File basePath = new File(mainPath);
            if (!basePath.exists())
                Log.d(TAG, basePath.mkdirs() ? "Success" : "Failed");

            String filePath = mainPath + "photo_" + Utilities.getTimestamp() + "_" + barcodeValue + ".jpg";
            File captureFile = new File(filePath);
            if (!captureFile.exists())
                Log.d(TAG, captureFile.createNewFile() ? "Success" : "Failed");
            FileOutputStream stream = new FileOutputStream(captureFile);
            stream.write(bytes);
            stream.flush();
            stream.close();
            return filePath;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }


    /**
     * Uploads a file to server using multipart post.
     *
     * @param filePath
     */
    public static void uploadToServer(final String filePath) throws Exception {
        File file = new File(filePath);
        String contentType = file.toURL().openConnection().getContentType();

        Log.d(TAG, "file: " + file.getPath());
        Log.d(TAG, "contentType: " + contentType);

        RequestBody fileBody = RequestBody.create(MediaType.parse(contentType), file);
        final String filename = file.getName();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("user_id", "1")
                .addFormDataPart("group_id", "1")
                .addFormDataPart("image", filename, fileBody)
                .build();

        Request request = new Request.Builder()
                .url(API_URL)
                .post(requestBody)
                .build();

        final OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull final IOException e) {
                Log.d(TAG, "Upload Failed!" + e.getMessage() + e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull final Response response) {
                Log.d(TAG, "Upload completed!");
                response.body().close();
            }
        });
    }


    /**
     * Rotate an image by the specified degree.
     *
     * @param bitmap: input image bitmap
     * @param degree: degree to rotate
     * @return
     */
    public static Bitmap rotateImage(final Bitmap bitmap, final int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return rotatedImg;
    }

    /**
     * Converts bitmap to
     *
     * @param bitmap
     * @return
     */
    public static byte[] bitmapToBytes(final Bitmap bitmap) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        return bos.toByteArray();
    }


    /**
     * Tries to crop an image for area within to barcodes.
     * This was needed because, the barcode detector library as at times detection only one barcode (right or left).
     * We needed to crop the image to extract the area within both barcodes.
     * NOTE: This method works on best effort basis and does not always work!
     *
     * @param bitmap - input image bitmap
     * @param rect   - rect of the identifies bitmap
     * @return
     */
    public static Bitmap checkAndCrop(final Bitmap bitmap, final Rect rect) {
        int x1 = rect.left;
        int y1 = rect.top;

        int x2 = rect.right;
        int y2 = rect.bottom;


        String detectedBarcodeType;
        Bitmap outBitmap;
        if (x1 < 1920 / 2 && x2 < 1920 / 2) { // Left barcode is detected
            outBitmap = cropImage(bitmap, new Rect(x1, y1, 1980, y1));
            detectedBarcodeType = "LEFT";
        } else if (x1 > 1920 / 2 && x2 > 1920 / 2) { // Right barcode is detected
            outBitmap = bitmap;
            detectedBarcodeType = "RIGHT";
        } else if (x1 < 1920 / 2 && x2 > 1920 / 2) { // both are detected
            outBitmap = cropImage(bitmap, rect);
            detectedBarcodeType = "BOTH";
        } else { // could not detect!
            outBitmap = bitmap;
            detectedBarcodeType = "NONE";
        }

        Log.d(TAG, "Detected Barcode type: " + detectedBarcodeType);
        return outBitmap;
    }

    /**
     * Crops an image
     *
     * @param bitmap
     * @param rect
     * @return
     */
    private static Bitmap cropImage(Bitmap bitmap, Rect rect) {
        Log.d(TAG, "Input - width: " + bitmap.getWidth() + ", height:" + bitmap.getHeight());

        int top = rect.top < 50 ? 0 : rect.top - 50;
        int bottom = rect.bottom > 1000 ? 1080 : rect.bottom + 80;

        try {
            Bitmap outBitmap = Bitmap.createBitmap(bitmap, rect.left, top, rect.right - rect.left, bottom - rect.top);
            Log.d(TAG, "Output - width: " + outBitmap.getWidth() + ", height:" + outBitmap.getHeight());
            return outBitmap;
        } catch (Exception e) {
            return bitmap;
        }
    }

    /**
     * Checks is it's a valid aspect ratio - as per:
     * width/height OR height/width == 5
     * NOTE: This is a hacky way to check!
     *
     * @param barcode
     * @return
     */
    public static boolean isValidAspectRatio(final Barcode barcode) {
        int w = barcode.getBoundingBox().width();
        int h = barcode.getBoundingBox().height();
        if (w == 0 || h == 0) return false;

        if ((w > h && w / h < 5) || (h > w && h / w < 5)) {
            Log.d(TAG, "width:" + w + ", height:" + h);
            return true;
        } else {
            Log.d(TAG, "width:" + w + ", height:" + h);
        }
        return false;
    }

    /**
     * Detects barcode(s) from a supplied image.
     *
     * @param bitmap - input image bitmap.
     */
    public static Bitmap detectBarcode(final Bitmap bitmap, Context applicationContext) {
        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(applicationContext)
                .build();
        SparseArray<Barcode> sparseArray = barcodeDetector.detect(frame);


        if (sparseArray != null && sparseArray.size() > 0) {
            for (int i = 0; i < sparseArray.size(); i++) {
                Log.d(TAG, "Value: " + sparseArray.valueAt(i).rawValue + "----" + sparseArray.valueAt(i).displayValue);
            }
            return Utilities.checkAndCrop(bitmap, sparseArray.valueAt(0).getBoundingBox());

        } else {
            Log.e("TAG", "SparseArray null or empty");
        }

        return bitmap;
    }

}
