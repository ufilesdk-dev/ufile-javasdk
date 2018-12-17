package com.ucloud.ufile.sample.object;

import com.ucloud.ufile.UfileClient;
import com.ucloud.ufile.api.ApiError;
import com.ucloud.ufile.api.object.ObjectConfig;
import com.ucloud.ufile.auth.UfileAuthorizationException;
import com.ucloud.ufile.auth.sign.UfileSignatureException;
import com.ucloud.ufile.bean.DownloadFileBean;
import com.ucloud.ufile.bean.DownloadStreamBean;
import com.ucloud.ufile.bean.UfileErrorBean;
import com.ucloud.ufile.exception.UfileRequiredParamNotFoundException;
import com.ucloud.ufile.http.UfileCallback;
import com.ucloud.ufile.sample.Constants;
import com.ucloud.ufile.util.JLog;
import okhttp3.Request;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;


/**
 *
 * @author: joshua
 * @E-mail: joshua.yin@ucloud.cn
 * @date: 2018-12-11 14:32
 */
public class GetObjectSample {
    private static final String TAG = "GetObjectSample";
    private static ObjectConfig config = new ObjectConfig("your bucket region", "ufileos.com");

    public static void main(String[] args) {
        String keyName = "which";
        String bucketName = "bucketName";
        long expiresDuration = 5 * 60;

        String localDir = "local save dir";
        String saveName = "local save name";
        try {
            String url = UfileClient.object(Constants.OBJECT_AUTHORIZER, config)
                    .getDownloadUrlFromPrivateBucket(keyName, bucketName, expiresDuration);
            getStream(url, localDir, saveName);
        } catch (UfileSignatureException e) {
            e.printStackTrace();
        } catch (UfileRequiredParamNotFoundException e) {
            e.printStackTrace();
        } catch (UfileAuthorizationException e) {
            e.printStackTrace();
        }
    }

    public static void getFile(String url, String localDir, String saveName) {
        UfileClient.object(Constants.OBJECT_AUTHORIZER, config)
                .getFile(url)
                .saveAt(localDir, saveName)
                /**
                 * 是否覆盖本地已有文件, Default = true;
                 */
//                .withCoverage(false)
                /**
                 * 指定progress callback的间隔
                 */
//                .withProgressConfig(ProgressConfig.callbackWithPercent(10))
                .executeAsync(new UfileCallback<DownloadFileBean>() {
                    @Override
                    public void onProgress(long bytesWritten, long contentLength) {
                        JLog.D(TAG, String.format("[progress] = %d%% - [%d/%d]", (int) (bytesWritten * 1.f / contentLength * 100), bytesWritten, contentLength));
                    }

                    @Override
                    public void onResponse(DownloadFileBean response) {
                        JLog.D(TAG, String.format("[res] = %s", (response == null ? "null" : response.toString())));
                    }

                    @Override
                    public void onError(Request request, ApiError error, UfileErrorBean response) {
                        JLog.D(TAG, String.format("[error] = %s\n[info] = %s",
                                (error == null ? "null" : error.toString()),
                                (response == null ? "null" : response.toString())));
                    }
                });
    }

    public static void getStream(String url, String localDir, String saveName) {
        OutputStream os = null;
        try {
            os = new FileOutputStream(new File(localDir, saveName));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        UfileClient.object(Constants.OBJECT_AUTHORIZER, config)
                .getStream(url)
                /**
                 * 重定向流
                 *
                 * 默认不重定向流，下载的流会以InputStream的形式在Response中回调，并且不会回调下载进度 onProgress;
                 *
                 * 如果配置了重定向的输出流，则Response {@link DownloadStreamBean}的 InputStream = null,
                 * 因为流已被重定向导流到OutputStream，并且会回调进度 onProgress。
                 */
                .redirectStream(os)
                /**
                 * 指定progress callback的间隔
                 */
//                .withProgressConfig(ProgressConfig.callbackWithPercent(10))
                .executeAsync(new UfileCallback<DownloadStreamBean>() {
                    @Override
                    public void onProgress(long bytesWritten, long contentLength) {
                        JLog.D(TAG, String.format("[progress] = %d%% - [%d/%d]", (int) (bytesWritten * 1.f / contentLength * 100), bytesWritten, contentLength));
                    }

                    @Override
                    public void onResponse(DownloadStreamBean response) {
                        JLog.D(TAG, String.format("[res] = %s", (response == null ? "null" : response.toString())));
                    }

                    @Override
                    public void onError(Request request, ApiError error, UfileErrorBean response) {
                        JLog.D(TAG, String.format("[error] = %s\n[info] = %s",
                                (error == null ? "null" : error.toString()),
                                (response == null ? "null" : response.toString())));
                    }
                });
    }
}
