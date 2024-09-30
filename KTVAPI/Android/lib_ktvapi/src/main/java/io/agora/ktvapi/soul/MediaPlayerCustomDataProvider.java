package io.agora.ktvapi.soul;

import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import cn.soulapp.android.lib.media.zego.interfaces.IMediaPlayerDecryptBlock;
import io.agora.mediaplayer.IMediaPlayerCustomDataProvider;

public class MediaPlayerCustomDataProvider implements IMediaPlayerCustomDataProvider {
    private static String TAG = "hhhhhh";

    public static final int SEEK_SET = 0;  // The file offset is set to offset bytes.
    public static final int SEEK_CUR = 1;  // The file offset is set to its current location plus offset bytes.
    public static final int SEEK_END = 2;  // file offset is set to the size of the file plus offset bytes.

    public static final int SEEK_SIZE = 65536;  //

    private RandomAccessFile fileInput = null;
    private String url;
    private long offset = 0;
    private long fileLength = 0;
    private IMediaPlayerDecryptBlock mediaPlayerFileReader;
    private byte[] key;

    public void setUrl(String url) {
        try {
            // 打开文件
            fileInput = new RandomAccessFile(url, "r");
            fileLength = fileInput.length();
            this.url = url;
//            key = "jerryjerryjerry1".getBytes(StandardCharsets.UTF_8);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setMediaPlayerFileReader(IMediaPlayerDecryptBlock mediaPlayerFileReader) {
        this.mediaPlayerFileReader = mediaPlayerFileReader;
    }

    /**
     * 读取文件
     *
     * @param byteBuffer 需要填充的buffer
     * @param size       读取的数据量
     * @return > 0: 读取的数据量
     * <= 0: 读到文件结尾或者读取文件出错
     */
    @Override
    public int onReadData(ByteBuffer byteBuffer, int size) {
        Log.d(TAG, "onReadData:  size: " + size);
        if (fileInput == null) {
            return -1;
        }
        int readSize = 0;
//        byte[] buffer = new byte[size];
        try {
            long startIdx = (offset / 16) * 16;
            long endIdx = ((offset + size) / 16) * 16 + 16;
            if ((offset + size) % 16 == 0) {
                endIdx = endIdx - 16;
            }
            Log.d(TAG, "onReadData startIdx: " + startIdx + " endIdx: " + endIdx);
            byte[] tempBuffer = new byte[(int) (endIdx - startIdx)];
            // 读取size大小的数据量
            fileInput.seek(startIdx);
            readSize = fileInput.read(tempBuffer);
            if (readSize > 0) {
                byte[] tempDecBuffer = new byte[(int) (endIdx - startIdx)];
                for (int i = 0; i * 16 < tempBuffer.length; i++) {
                    byte[] rb = Arrays.copyOfRange(tempBuffer, i * 16, i * 16 + 16);
//                    byte[] b = SoulCrypt.KtvAesDecrypt(key, rb);
                    byte[] b = mediaPlayerFileReader.decryptAudio(rb);
                    System.arraycopy(b, 0, tempDecBuffer, i * 16, 16);
                }
                int from = (int) (offset - startIdx);
                int to = (int) (tempDecBuffer.length - endIdx + offset + size);
                Log.d(TAG, "onReadData from: " + from + " to: " + to);
                byte[] resultByte = Arrays.copyOfRange(tempDecBuffer, from, to);
                byteBuffer.put(resultByte, 0, resultByte.length);
                Log.d(TAG, "onReadData resultByte.length: " + resultByte.length);
                byteBuffer.flip();
                fileInput.seek(offset + size);
                offset += size;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        return size;
    }

    /**
     * seek事件回调
     *
     * @param offset 偏移量
     * @param whence 当whence==65536时要返回文件长度
     * @return = 0: success
     * < 0: failed
     */
    @Override
    public long onSeek(long offset, int whence) {
        if (fileInput == null) {
            return -1;
        }
        try {
            if (whence == SEEK_SET) {
                // 设置偏移量
                this.offset = offset;
            } else if (whence == SEEK_CUR) {
                // 从当前偏移量再增加一个偏移量
                this.offset += offset;
            } else if (whence == SEEK_END) {
                // 偏移到文件末尾
                this.offset = fileLength;
            } else if (whence == SEEK_SIZE) {
                // 返回文件长度
                return fileLength;
            }
            // 执行seek操作
            Log.d(TAG, "whence: " + whence + ", offset: " + offset);
            fileInput.seek(this.offset);
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    public String getUrl() {
        return url;
    }

    public void close() {
        if (fileInput == null) {
            return;
        }
        try {
            fileInput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}