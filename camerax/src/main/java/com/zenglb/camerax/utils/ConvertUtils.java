package com.zenglb.camerax.utils;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * 转换Utils
 * <p>
 * Android 的兼容性问题，哎....
 */
public class ConvertUtils {

    /**
     * 将 NV21 格式字节缓冲区转换为Bitmap。
     */
    @Nullable
    public static Bitmap getBitmap(ByteBuffer data, int width, int height, int rotation) {
        data.rewind();
        byte[] imageInBuffer = new byte[data.limit()];
        data.get(imageInBuffer, 0, imageInBuffer.length);
        try {
            YuvImage image = new YuvImage(imageInBuffer, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compressToJpeg(new Rect(0, 0, width, height), 80, stream);

            Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());

            stream.close();
            return rotateBitmap(bmp, rotation, true, false);
        } catch (Exception e) {
            Log.e("VisionProcessorBase", "Error: " + e.getMessage());
        }
        return null;
    }

    /**
     * 将来自 CameraX API 的 YUV_420_888 图像转换为Bitmap。
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @Nullable
    @ExperimentalGetImage
    public static Bitmap getBitmap(ImageProxy imageProxy) {
        if (imageProxy.getImage() == null)
            return null;
        ByteBuffer nv21Buffer = yuv420ThreePlanesToNV21(imageProxy.getImage().getPlanes(), imageProxy.getWidth(), imageProxy.getHeight());
        return getBitmap(nv21Buffer, imageProxy.getWidth(), imageProxy.getHeight(), imageProxy.getImageInfo().getRotationDegrees());
    }

    /**
     * bitmap旋转或者翻转
     *
     */
    private static Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees, boolean flipX, boolean flipY) {
        Matrix matrix = new Matrix();

        // 图像旋转
        matrix.postRotate(rotationDegrees);

        // flipY垂直或者flipX水平镜像翻转
        matrix.postScale(flipX ? -1.0f : 1.0f, flipY ? -1.0f : 1.0f);
        Bitmap rotatedBitmap =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        // 如果旧bitmap已更改，则回收。
        if (rotatedBitmap != bitmap) {
            bitmap.recycle();
        }
        return rotatedBitmap;
    }

    /**
     * YUV_420_888格式转换成NV21.
     * <p>
     * NV21 格式由一个包含 Y、U 和 V 值的单字节数组组成。
     * 对于大小为 S 的图像，数组的前 S 个位置包含所有 Y 值。其余位置包含交错的 V 和 U 值。
     * U 和 V 在两个维度上都进行了 2 倍的二次采样，因此有 S/4 U 值和 S/4 V 值。
     * 总之，NV21 数组将包含 S 个 Y 值，后跟 S/4 + S/4 VU 值: YYYYYYYYYYYYYY(...)YVUVUVUVU(...)VU
     * <p>
     * YUV_420_888 是一种通用格式，可以描述任何 YUV 图像，其中 U 和 V 在两个维度上都以 2 倍的因子进行二次采样。
     * {@link Image#getPlanes} 返回一个包含 Y、U 和 V 平面的数组
     * Y 平面保证不会交错，因此我们可以将其值复制到 NV21 数组的第一部分。U 和 V 平面可能已经具有 NV21 格式的表示。
     * 如果平面共享相同的缓冲区，则会发生这种情况，V 缓冲区位于 U 缓冲区之前的一个位置，并且平面的 pixelStride 为 2。
     * 如果是这种情况，我们可以将它们复制到 NV21 阵列中。
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private static ByteBuffer yuv420ThreePlanesToNV21(
            Image.Plane[] yuv420888planes, int width, int height) {
        int imageSize = width * height;
        byte[] out = new byte[imageSize + 2 * (imageSize / 4)];

        if (areUVPlanesNV21(yuv420888planes, width, height)) {
            // 复制 Y 的值
            yuv420888planes[0].getBuffer().get(out, 0, imageSize);
            // 从 V 缓冲区获取第一个 V 值，因为 U 缓冲区不包含它。
            yuv420888planes[2].getBuffer().get(out, imageSize, 1);
            // 从 U 缓冲区复制第一个 U 值和剩余的 VU 值。
            yuv420888planes[1].getBuffer().get(out, imageSize + 1, 2 * imageSize / 4 - 1);
        } else {
            // 回退到一个一个地复制 UV 值，这更慢但也有效。
            // 取 Y.
            unpackPlane(yuv420888planes[0], width, height, out, 0, 1);
            // 取 U.
            unpackPlane(yuv420888planes[1], width, height, out, imageSize + 1, 2);
            // 取 V.
            unpackPlane(yuv420888planes[2], width, height, out, imageSize, 2);
        }

        return ByteBuffer.wrap(out);
    }

    /**
     * 检查 YUV_420_888 图像的 UV 平面缓冲区是否为 NV21 格式。
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private static boolean areUVPlanesNV21(Image.Plane[] planes, int width, int height) {
        int imageSize = width * height;

        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        // 备份缓冲区属性。
        int vBufferPosition = vBuffer.position();
        int uBufferLimit = uBuffer.limit();

        // 将 V 缓冲区推进 1 个字节，因为 U 缓冲区将不包含第一个 V 值。
        vBuffer.position(vBufferPosition + 1);
        // 切掉 U 缓冲区的最后一个字节，因为 V 缓冲区将不包含最后一个 U 值。
        uBuffer.limit(uBufferLimit - 1);

        // 检查缓冲区是否相等并具有预期的元素数量。
        boolean areNV21 = (vBuffer.remaining() == (2 * imageSize / 4 - 2)) && (vBuffer.compareTo(uBuffer) == 0);

        // 将缓冲区恢复到初始状态。
        vBuffer.position(vBufferPosition);
        uBuffer.limit(uBufferLimit);

        return areNV21;
    }

    /**
     * 将图像平面解压缩为字节数组。
     * <p>
     * 输入平面数据将被复制到“out”中，从“offset”开始，每个像素将被“pixelStride”隔开。 请注意，输出上没有行填充。
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static void unpackPlane(Image.Plane plane, int width, int height, byte[] out, int offset, int pixelStride) {
        ByteBuffer buffer = plane.getBuffer();
        buffer.rewind();

        // 计算当前平面的大小。假设它的纵横比与原始图像相同。
        int numRow = (buffer.limit() + plane.getRowStride() - 1) / plane.getRowStride();
        if (numRow == 0) {
            return;
        }
        int scaleFactor = height / numRow;
        int numCol = width / scaleFactor;

        // 提取输出缓冲区中的数据。
        int outputPos = offset;
        int rowStart = 0;
        for (int row = 0; row < numRow; row++) {
            int inputPos = rowStart;
            for (int col = 0; col < numCol; col++) {
                out[outputPos] = buffer.get(inputPos);
                outputPos += pixelStride;
                inputPos += plane.getPixelStride();
            }
            rowStart += plane.getRowStride();
        }
    }


}

