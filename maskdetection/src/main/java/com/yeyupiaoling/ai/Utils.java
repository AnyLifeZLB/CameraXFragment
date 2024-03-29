package com.yeyupiaoling.ai;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Utils {
    private static final String TAG = Utils.class.getName();


    // 获取最优的预览图片大小
    public static Size chooseOptimalSize(final Size[] choices, final int width, final int height) {
        final Size desiredSize = new Size(width, height);

        // Collect the supported resolutions that are at least as big as the preview Surface
        boolean exactSizeFound = false;
        float desiredAspectRatio = width * 1.0f / height; //in landscape perspective
        float bestAspectRatio = 0;
        final List<Size> bigEnough = new ArrayList<Size>();
        for (final Size option : choices) {
            if (option.equals(desiredSize)) {
                // Set the size but don't return yet so that remaining sizes will still be logged.
                exactSizeFound = true;
                break;
            }

            float aspectRatio = option.getWidth() * 1.0f / option.getHeight();
            if (aspectRatio > desiredAspectRatio) continue; //smaller than screen
            //try to find the best aspect ratio which fits in screen
            if (aspectRatio > bestAspectRatio) {
                if (option.getHeight() >= height && option.getWidth() >= width) {
                    bigEnough.clear();
                    bigEnough.add(option);
                    bestAspectRatio = aspectRatio;
                }
            } else if (aspectRatio == bestAspectRatio) {
                if (option.getHeight() >= height && option.getWidth() >= width) {
                    bigEnough.add(option);
                }
            }
        }
        if (exactSizeFound) {
            return desiredSize;
        }

        if (bigEnough.size() > 0) {
            final Size chosenSize = Collections.min(bigEnough, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(
                            (long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
                }
            });
            return chosenSize;
        } else {
            return choices[0];
        }
    }

    /**
     * copy model file to local
     *
     * @param context     activity context
     * @param assets_path model in assets path
     * @param new_path    copy to new path
     */
    public static void copyFileFromAsset(Context context, String assets_path, String new_path) {
        File father_path = new File(new File(new_path).getParent());
        if (!father_path.exists()) {
            father_path.mkdirs();
        }
        try {
            File new_file = new File(new_path);
            InputStream is_temp = context.getAssets().open(assets_path);
            if (new_file.exists() && new_file.isFile()) {
                if (contrastFileMD5(new_file, is_temp)) {
                    Log.d(TAG, new_path + " is exists!");
                    return;
                } else {
                    Log.d(TAG, "delete old model file!");
                    new_file.delete();
                }
            }
            InputStream is = context.getAssets().open(assets_path);
            FileOutputStream fos = new FileOutputStream(new_file);
            byte[] buffer = new byte[1024];
            int byteCount;
            while ((byteCount = is.read(buffer)) != -1) {
                fos.write(buffer, 0, byteCount);
            }
            fos.flush();
            is.close();
            fos.close();
            Log.d(TAG, "the model file is copied");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //get bin file's md5 string
    private static boolean contrastFileMD5(File new_file, InputStream assets_file) {
        MessageDigest new_file_digest, assets_file_digest;
        int len;
        try {
            byte[] buffer = new byte[1024];
            new_file_digest = MessageDigest.getInstance("MD5");
            FileInputStream in = new FileInputStream(new_file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                new_file_digest.update(buffer, 0, len);
            }

            assets_file_digest = MessageDigest.getInstance("MD5");
            while ((len = assets_file.read(buffer, 0, 1024)) != -1) {
                assets_file_digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        String new_file_md5 = new BigInteger(1, new_file_digest.digest()).toString(16);
        String assets_file_md5 = new BigInteger(1, assets_file_digest.digest()).toString(16);
        Log.d("new_file_md5", new_file_md5);
        Log.d("assets_file_md5", assets_file_md5);
        return new_file_md5.equals(assets_file_md5);
    }

    // get max probability label
    public static int getMaxResult(float[] result) {
        float probability = 0;
        int r = 0;
        for (int i = 0; i < result.length; i++) {
            if (probability < result[i]) {
                probability = result[i];
                r = i;
            }
        }
        return r;
    }

    // get photo from Uri
    public static String getPathFromURI(Context context, Uri uri) {
        String result;
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) {
            result = uri.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    // 压缩大小
    public static Bitmap getScaleBitmap(Bitmap bitmap, int size) {
        int bmpWidth = bitmap.getWidth();
        int bmpHeight = bitmap.getHeight();
        if (bmpHeight < size && bmpWidth < size){
            return bitmap;
        }
        float scale;
        if (bmpHeight > bmpWidth){
            scale = (float) size / bmpHeight;
        }else {
            scale = (float) size / bmpWidth;
        }
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        return Bitmap.createBitmap(bitmap, 0, 0, bmpWidth, bmpHeight, matrix, true);
    }


    public static Bitmap drawBitmap(Bitmap bitmap, Face[] faces){
        int left, top, right, bottom;
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        Paint paint1 = new Paint();
        paint1.setColor(Color.RED);
        paint1.setStyle(Paint.Style.STROKE);
        paint1.setStrokeWidth(2);
        paint1.setTextSize(33);

        Paint paint2 = new Paint();
        paint2.setColor(Color.WHITE);
        paint2.setStyle(Paint.Style.FILL_AND_STROKE);
        paint2.setTextSize(44);
        paint2.setStrokeWidth(2);

        for (Face face : faces) {
            left = (int) (face.roi[0]);
            top = (int) (face.roi[1]);
            right = (int) (face.roi[2] + face.roi[0]);
            bottom = (int) (face.roi[3] + face.roi[1]);

            canvas.drawRect(left, top, right, bottom, paint1);

            for (int j = 0; j < face.keypoints.length; j = j + 2) {
                canvas.drawText(String.valueOf(j / 2), face.keypoints[j], face.keypoints[j + 1], paint);
            }

            if (face.classid == 1){
                canvas.drawText("戴口罩", left, top-11, paint2);
                Log.d("AI","戴了口罩");
            }else {
                canvas.drawText("未戴口罩", left, top-11, paint2);
                Log.e("AI","没戴口罩");
            }
        }
        return bitmap;
    }
}
