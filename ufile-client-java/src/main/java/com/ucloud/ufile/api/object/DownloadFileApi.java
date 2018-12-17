package com.ucloud.ufile.api.object;

import com.ucloud.ufile.UfileConstants;
import com.ucloud.ufile.api.ApiError;
import com.ucloud.ufile.auth.ObjectAuthorizer;
import com.ucloud.ufile.bean.DownloadFileBean;
import com.ucloud.ufile.bean.ObjectProfile;
import com.ucloud.ufile.exception.UfileException;
import com.ucloud.ufile.exception.UfileIOException;
import com.ucloud.ufile.exception.UfileParamException;
import com.ucloud.ufile.exception.UfileRequiredParamNotFoundException;
import com.ucloud.ufile.http.UfileHttpException;
import com.ucloud.ufile.http.BaseHttpCallback;
import com.ucloud.ufile.http.HttpClient;
import com.ucloud.ufile.http.OnProgressListener;
import com.ucloud.ufile.http.ProgressConfig;
import com.ucloud.ufile.http.request.GetRequestBuilder;
import com.ucloud.ufile.util.*;
import okhttp3.Call;
import okhttp3.Response;
import sun.security.validator.ValidatorException;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * API-下载文件
 *
 * @author: joshua
 * @E-mail: joshua.yin@ucloud.cn
 * @date: 2018/11/12 19:07
 */
public class DownloadFileApi extends UfileObjectApi<DownloadFileBean> {
    /**
     * Required
     * 下载文件保存的本地目录路径
     */
    @NotEmpty(message = "Param 'localPath' is required")
    private String localPath;
    /**
     * Required
     * 下载文件保存的本地文件名
     */
    @NotEmpty(message = "Param 'saveName' is required")
    private String saveName;

    /**
     * Required
     * 要下载的云端文件描述，通过ObjectProfile API获得
     */
    @NotNull(message = "Profile is required")
    private ObjectProfile profile;

    /**
     * 有效时限
     */
    private long expiresDuration = 24 * 60 * 60;
    /**
     * 并行下载线程数
     */
    private int threadCount = 10;

    /**
     * 进度回调设置
     */
    private ProgressConfig progressConfig;

    /**
     * Optional
     * 要下载的对象的范围起始偏移量，Default = null，若null则从0开始下载
     */
    private Long offset;
    /**
     * Optional
     * 要下载的对象的范围长度，Default = null，若null则下载整个对象
     */
    private Long size;

    /**
     * Optional
     * 是否覆盖本地已有文件，Default = true
     */
    private boolean isCover = true;

    /**
     * 下载文件的总大小
     */
    private long totalSize = 0;
    /**
     * 分片数量
     */
    private int partCount = 0;
    /**
     * 分片任务集
     */
    private List<DownloadCallable> callList;
    /**
     * 下载到本地的文件
     */
    private File finalFile;
    /**
     * 已写的Byte计数
     */
    private AtomicLong bytesWritten;
    /**
     * 已写的Byte计数缓存，用于按照Buffer大小回调进度
     */
    private AtomicLong bytesWrittenCache;
    /**
     * 下载线程池
     */
    private ExecutorService mFixedThreadPool;

    /**
     * 构造方法
     *
     * @param authorizer Object授权器
     * @param host       API域名
     * @param httpClient Http客户端
     */
    protected DownloadFileApi(ObjectAuthorizer authorizer, String host, HttpClient httpClient) {
        super(authorizer, host, httpClient);
        RESP_CODE_SUCCESS = 206;
        progressConfig = ProgressConfig.callbackDefault();
    }

    /**
     * 配置下载后的本地保存目录和文件名
     *
     * @param localPath 本地目录
     * @param saveName  保存文件名
     * @return {@link DownloadFileApi}
     */
    public DownloadFileApi saveAt(String localPath, String saveName) {
        this.localPath = localPath;
        this.saveName = saveName;
        return this;
    }

    /**
     * 配置需要下载的文件描述信息
     *
     * @param profile 文件描述信息
     * @return {@link DownloadFileApi}
     */
    public DownloadFileApi which(ObjectProfile profile) {
        this.profile = profile;
        return this;
    }

    /**
     * 选择要下载的对象的范围，Default = (0, whole size)
     *
     * @param offset 起始偏移量
     * @param size   范围长度
     * @return {@link DownloadFileApi}
     */
    public DownloadFileApi withinRange(long offset, long size) {
        this.offset = new Long(offset);
        this.size = new Long(size);
        return this;
    }

    /**
     * 配置并行下载的线程数, Default = 10
     *
     * @param threadCount 线程数
     * @return {@link DownloadFileApi}
     */
    public DownloadFileApi together(int threadCount) {
        this.threadCount = threadCount;
        return this;
    }

    /**
     * 配置若本地已存在文件是否覆盖
     *
     * @param isCover 是否覆盖
     * @return {@link DownloadFileApi}
     */
    public DownloadFileApi withCoverage(boolean isCover) {
        this.isCover = isCover;
        return this;
    }

    /**
     * 配置进度回调设置
     *
     * @param config 进度回调设置
     * @return {@link DownloadFileApi}
     */
    public DownloadFileApi withProgressConfig(ProgressConfig config) {
        progressConfig = config == null ? this.progressConfig : config;
        return this;
    }

    @Override
    protected void prepareData() throws UfileException {
        if (host == null || host.length() == 0)
            throw new UfileRequiredParamNotFoundException("Param 'host' is null!");

        try {
            ParameterValidator.validator(this);

            File dir = new File(localPath);
            if (!dir.exists() || (dir.exists() && !dir.isDirectory()))
                dir.mkdirs();

            String absPath = localPath + (localPath.endsWith(File.separator) ? "" : File.separator) + saveName;

            File file = new File(absPath);
            if (file.exists() && file.isFile())
                if (isCover) {
                    FileUtil.deleteFileCleanly(file);
                    file = new File(absPath);
                } else {
                    int i = 1;
                    boolean isExist = true;
                    while (isExist) {
                        String tmpPath = absPath + String.format("-%d", i++);
                        file = new File(tmpPath);
                        if (!file.exists() || file.isDirectory()) {
                            isExist = false;
                            absPath = tmpPath;
                        }
                    }
                }

            finalFile = file;

            if (offset == null || size == null) {
                totalSize = profile.getContentLength();
            } else {
                if (offset.longValue() < 0l)
                    throw new UfileParamException("Invalid range param 'offset', offset must be >= 0");
                if (size.longValue() < 0l)
                    throw new UfileParamException("Invalid range param 'size', size must be >= 0");
                if (size.longValue() <= offset.longValue())
                    throw new UfileParamException("Invalid range, size must be > offset");

                totalSize = size.longValue() - offset.longValue();
            }
            host = new GenerateObjectPrivateUrlApi(authorizer, host, profile.getKeyName(), profile.getBucket(), expiresDuration).createUrl();

            partCount = (int) Math.ceil(totalSize * 1.d / UfileConstants.MULTIPART_SIZE);

            switch (progressConfig.type) {
                case PROGRESS_INTERVAL_TIME: {
                    // progressIntervalType是按时间周期回调，则自动做 0 ~ progressInterval 的合法化赋值，progressInterval置0，即实时回调读写进度
                    progressConfig.interval = Math.max(0, progressConfig.interval);
                    break;
                }
                case PROGRESS_INTERVAL_PERCENT: {
                    // progressIntervalType是按百分比回调，则若progressInterval<0 | >100，progressInterval置0，即实时回调读写进度
                    if (progressConfig.interval < 0 || progressConfig.interval > 100)
                        progressConfig.interval = 0l;
                    else
                        progressConfig.interval = (long) (progressConfig.interval / 100.f * totalSize);
                    break;
                }
                case PROGRESS_INTERVAL_BUFFER: {
                    // progressIntervalType是按读写的buffer size回调，则自动做 0 ~ totalSize-1 的合法化赋值，progressInterval置0，即实时回调读写进度
                    progressConfig.interval = Math.max(0, Math.min(totalSize - 1, progressConfig.interval));
                    break;
                }
            }

            bytesWritten = new AtomicLong(0);
            bytesWrittenCache = new AtomicLong(0);

            callList = new ArrayList<>();
            for (int i = 0; i < partCount; i++) {
                int start = i * UfileConstants.MULTIPART_SIZE;
                int end = ((int) Math.min(totalSize, (start + UfileConstants.MULTIPART_SIZE))) - 1;
                GetRequestBuilder builder = (GetRequestBuilder) new GetRequestBuilder()
                        .baseUrl(host)
                        .addHeader("Range", String.format("bytes=%d-%d", start, end));
                callList.add(new DownloadCallable(builder.build(httpClient.getOkHttpClient()), i));
            }

            mFixedThreadPool = Executors.newFixedThreadPool(threadCount);
        } catch (ValidatorException e) {
            throw new UfileRequiredParamNotFoundException(e);
        }
    }

    private OnProgressListener onProgressListener;

    @Override
    public DownloadFileBean execute() throws UfileException {
        prepareData();

        try {
            List<Future<DownloadFileBean>> futures = mFixedThreadPool.invokeAll(callList);

            return new DownloadFileBean()
                    .seteTag(Etag.etag(finalFile, UfileConstants.MULTIPART_SIZE).geteTag())
                    .setFile(finalFile)
                    .setContentLength(finalFile.length());
        } catch (IOException e) {
            throw new UfileIOException("Calculate ETag error!", e);
        } catch (InterruptedException e) {
            throw new UfileException("Invoke part occur error!", e);
        }
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
    public void executeAsync(BaseHttpCallback callback) {
        onProgressListener = callback;
        httpCallback = callback;

        try {
            prepareData();

            if (onProgressListener != null
                    && progressConfig.type == ProgressConfig.ProgressIntervalType.PROGRESS_INTERVAL_TIME) {
                progressTimer = new Timer();
                progressTimer.scheduleAtFixedRate(new ProgressTask(totalSize), progressConfig.interval, progressConfig.interval);
            }

            List<Future<DownloadFileBean>> futures = mFixedThreadPool.invokeAll(callList);

            if (httpCallback != null) {
                try {
                    httpCallback.onResponse(new DownloadFileBean()
                            .seteTag(Etag.etag(finalFile, UfileConstants.MULTIPART_SIZE).geteTag())
                            .setFile(finalFile)
                            .setContentLength(finalFile.length()));
                } catch (IOException e) {
                    httpCallback.onError(null,
                            new ApiError(ApiError.ErrorType.ERROR_NORMAL_ERROR, new UfileIOException("Calculate ETag error!", e)), null);
                }
            }
        } catch (UfileException e) {
            if (httpCallback != null)
                httpCallback.onError(null, new ApiError(ApiError.ErrorType.ERROR_PARAMS_ILLEGAL, e), null);
        } catch (InterruptedException e) {
            if (httpCallback != null)
                httpCallback.onError(null, new ApiError(ApiError.ErrorType.ERROR_NORMAL_ERROR, "Invoke part occur error!", e), null);
        }
    }

    private class DownloadCallable implements Callable<DownloadFileBean> {
        private Call call;
        private int index;

        public DownloadCallable(Call call, int index) {
            this.call = call;
            this.index = index;
        }

        @Override
        public DownloadFileBean call() throws Exception {
            try {
                Response response = call.execute();
                if (response == null)
                    throw new UfileHttpException("Response is null");

                if (response.code() != RESP_CODE_SUCCESS)
                    throw new UfileHttpException(parseErrorResponse(response).toString());

                return parseHttpResponse(response);
            } catch (Throwable t) {
                throw new UfileException(t);
            }
        }
    }

    @Override
    public DownloadFileBean parseHttpResponse(Response response) throws IOException {
        DownloadFileBean result = new DownloadFileBean();
        long contentLength = response.body().contentLength();
        result.setContentLength(contentLength);
        String range = response.header("Content-Range", "");
        range = range.replace("bytes", "");
        String[] rangeArr = range.split("-");
        long start = Long.parseLong(rangeArr[0].trim());
        rangeArr = rangeArr[1].trim().split("/");
        long total = Long.parseLong(rangeArr[1].trim());
        JLog.T(TAG, "[Content-Range]:" + range + " [start]:" + start + " [total]:" + total);

        RandomAccessFile raf = new RandomAccessFile(finalFile, "rwd");
        raf.seek(start);

        InputStream is = response.body().byteStream();
        try {
            byte[] buffer = new byte[UfileConstants.DEFAULT_BUFFER_SIZE];
            int len = 0;
            while ((len = is.read(buffer)) > 0) {
                raf.write(buffer, 0, len);
                if (onProgressListener != null) {
                    long written = bytesWritten.addAndGet(len);
                    long cache = bytesWrittenCache.addAndGet(len);
                    synchronized (bytesWritten) {
                        if (written < totalSize && cache < progressConfig.interval)
                            continue;

                        if (progressConfig.type != ProgressConfig.ProgressIntervalType.PROGRESS_INTERVAL_TIME) {
                            bytesWrittenCache.set(0);
                            onProgressListener.onProgress(written, totalSize);
                        } else {
                            if (written >= totalSize) {
                                progressTimer.cancel();
                                onProgressListener.onProgress(written, totalSize);
                            }
                        }
                    }
                }
            }
        } finally {
            FileUtil.close(raf, is);
        }

        return result;
    }
}
