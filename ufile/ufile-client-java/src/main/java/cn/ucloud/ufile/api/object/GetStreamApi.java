package cn.ucloud.ufile.api.object;

import cn.ucloud.ufile.auth.ObjectAuthorizer;
import cn.ucloud.ufile.bean.DownloadStreamBean;
import cn.ucloud.ufile.bean.UfileErrorBean;
import cn.ucloud.ufile.exception.UfileException;
import cn.ucloud.ufile.exception.UfileRequiredParamNotFoundException;
import cn.ucloud.ufile.http.BaseHttpCallback;
import cn.ucloud.ufile.http.HttpClient;
import cn.ucloud.ufile.http.OnProgressListener;
import cn.ucloud.ufile.http.ProgressConfig;
import cn.ucloud.ufile.http.request.GetRequestBuilder;
import cn.ucloud.ufile.util.FileUtil;
import cn.ucloud.ufile.UfileConstants;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;


/**
 * API-Get下载流
 *
 * @author: joshua
 * @E-mail: joshua.yin@ucloud.cn
 * @date: 2018/11/12 19:07
 */
public class GetStreamApi extends UfileObjectApi<DownloadStreamBean> {
    private OnProgressListener onProgressListener;
    private OutputStream outputStream;

    private ProgressConfig progressConfig;
    private AtomicLong bytesWritten;
    private AtomicLong bytesWrittenCache;

    /**
     * 构造方法
     *
     * @param authorizer Object授权器
     * @param host       API域名
     * @param httpClient Http客户端
     */
    protected GetStreamApi(ObjectAuthorizer authorizer, String host, HttpClient httpClient) {
        super(authorizer, host, httpClient);
        progressConfig = ProgressConfig.callbackDefault();
    }

    /**
     * 配置进度回调设置
     *
     * @param config 进度回调设置
     * @return {@link GetStreamApi}
     */
    public GetStreamApi withProgressConfig(ProgressConfig config) {
        progressConfig = config == null ? this.progressConfig : config;
        return this;
    }

    /**
     * 重定向流
     * <p>
     * 默认不重定向流，下载的流会以InputStream的形式在Response中回调，并且不会回调下载进度 onProgress;
     * <p>
     * 如果配置了重定向的输出流，则Response {@link DownloadStreamBean}的 InputStream = null,
     * 因为流已被重定向导流到OutputStream，并且会回调进度 onProgress。
     *
     * @param outputStream 重定向输出流
     * @return {@link GetStreamApi}
     */
    public GetStreamApi redirectStream(OutputStream outputStream) {
        this.outputStream = outputStream;
        return this;
    }

    @Override
    protected void prepareData() throws UfileException {
        if (host == null || host.length() == 0)
            throw new UfileRequiredParamNotFoundException("Param 'host' is null!");

        bytesWritten = new AtomicLong(0);
        bytesWrittenCache = new AtomicLong(0);
        call = new GetRequestBuilder()
                .baseUrl(host)
                .build(httpClient.getOkHttpClient());
    }

    @Override
    public void executeAsync(BaseHttpCallback<DownloadStreamBean, UfileErrorBean> callback) {
        onProgressListener = callback;
        super.executeAsync(callback);
    }

    private Timer progressTimer;

    private class ProgressTask extends TimerTask {
        private long totalSize = 0l;

        private ProgressTask(long totalSize) {
            this.totalSize = totalSize;
        }

        @Override
        public void run() {
            if (onProgressListener != null)
                onProgressListener.onProgress(bytesWritten.get(), totalSize);
        }
    }

    @Override
    public DownloadStreamBean parseHttpResponse(Response response) throws IOException {
        DownloadStreamBean result = new DownloadStreamBean();
        long contentLength = response.body().contentLength();
        result.setContentLength(contentLength);
        result.seteTag(response.header("ETag").replace("\"", ""));

        InputStream is = response.body().byteStream();
        if (outputStream == null) {
            result.setInputStream(is);
        } else {
            if (onProgressListener != null) {
                switch (progressConfig.type) {
                    case PROGRESS_INTERVAL_TIME: {
                        // progressIntervalType是按时间周期回调，则自动做 0 ~ progressInterval 的合法化赋值，progressInterval置0，即实时回调读写进度
                        progressConfig.interval = Math.max(0, progressConfig.interval);
                        progressTimer = new Timer();
                        progressTimer.scheduleAtFixedRate(new ProgressTask(contentLength), progressConfig.interval, progressConfig.interval);
                        break;
                    }
                    case PROGRESS_INTERVAL_PERCENT: {
                        // progressIntervalType是按百分比回调，则若progressInterval<0 | >100，progressInterval置0，即实时回调读写进度
                        if (progressConfig.interval < 0 || progressConfig.interval > 100)
                            progressConfig.interval = 0l;
                        else
                            progressConfig.interval = (long) (progressConfig.interval / 100.f * contentLength);
                        break;
                    }
                    case PROGRESS_INTERVAL_BUFFER: {
                        // progressIntervalType是按读写的buffer size回调，则自动做 0 ~ totalSize-1 的合法化赋值，progressInterval置0，即实时回调读写进度
                        progressConfig.interval = Math.max(0, Math.min(contentLength - 1, progressConfig.interval));
                        break;
                    }
                }
            }

            try {
                byte[] buffer = new byte[UfileConstants.DEFAULT_BUFFER_SIZE];
                int len = 0;
                while ((len = is.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, len);

                    if (onProgressListener != null) {
                        long written = bytesWritten.addAndGet(len);
                        long cache = bytesWrittenCache.addAndGet(len);
                        synchronized (bytesWritten) {
                            if (written < contentLength && cache < progressConfig.interval)
                                continue;

                            if (progressConfig.type != ProgressConfig.ProgressIntervalType.PROGRESS_INTERVAL_TIME) {
                                bytesWrittenCache.set(0);
                                onProgressListener.onProgress(written, contentLength);
                            } else {
                                if (written >= contentLength) {
                                    progressTimer.cancel();
                                    onProgressListener.onProgress(written, contentLength);
                                }
                            }
                        }
                    }
                }
            } finally {
                FileUtil.close(outputStream, is);
            }
        }

        return result;
    }
}
