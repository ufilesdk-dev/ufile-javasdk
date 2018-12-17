package com.ucloud.ufile.api.object;

import com.ucloud.ufile.UfileConstants;
import com.ucloud.ufile.auth.ObjectAuthorizer;
import com.ucloud.ufile.bean.base.BaseResponseBean;
import com.ucloud.ufile.exception.UfileException;
import com.ucloud.ufile.exception.UfileIOException;
import com.ucloud.ufile.exception.UfileRequiredParamNotFoundException;
import com.ucloud.ufile.http.HttpClient;
import com.ucloud.ufile.http.request.PostJsonRequestBuilder;
import com.ucloud.ufile.util.*;
import sun.security.validator.ValidatorException;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * API-流秒传
 *
 * @author: joshua
 * @E-mail: joshua.yin@ucloud.cn
 * @date: 2018/11/12 19:08
 */
public class UploadStreamHitApi extends UfileObjectApi<BaseResponseBean> {
    /**
     * Required
     * 云端对象名称
     */
    @NotEmpty(message = "KeyName is required to set through method 'nameAs'")
    protected String keyName;
    /**
     * Required
     * 要上传的流
     */
    @NotNull(message = "InputStream is required")
    protected InputStream inputStream;
    /**
     * Required
     * Bucket空间名称
     */
    @NotEmpty(message = "BucketName is required to set through method 'toBucket'")
    protected String bucketName;

    private ByteArrayOutputStream cacheOutputStream;

    /**
     * 构造方法
     *
     * @param authorizer Object授权器
     * @param host       API域名
     * @param httpClient Http客户端
     */
    protected UploadStreamHitApi(ObjectAuthorizer authorizer, String host, HttpClient httpClient) {
        super(authorizer, host, httpClient);
    }

    /**
     * 设置上传到云端的对象名称
     *
     * @param keyName 对象名称
     * @return {@link UploadStreamHitApi}
     */
    public UploadStreamHitApi nameAs(String keyName) {
        this.keyName = keyName;
        return this;
    }

    /**
     * 设置要上传的流
     *
     * @param inputStream 输入流
     * @return {@link UploadStreamHitApi}
     */
    public UploadStreamHitApi from(InputStream inputStream) {
        this.inputStream = inputStream;
        return this;
    }

    /**
     * 设置要上传到的Bucket名称
     *
     * @param bucketName bucket名称
     * @return {@link UploadStreamHitApi}
     */
    public UploadStreamHitApi toBucket(String bucketName) {
        this.bucketName = bucketName;
        return this;
    }

    @Override
    protected void prepareData() throws UfileException {
        try {
            ParameterValidator.validator(this);

            String date = dateFormat.format(new Date(System.currentTimeMillis()));
            String authorization = authorizer.authorization(HttpMethod.POST, bucketName, keyName,
                    "", "", date);

            PostJsonRequestBuilder builder = new PostJsonRequestBuilder();

            String url = generateFinalHost(bucketName, "uploadhit");
            List<Parameter<String>> query = new ArrayList<>();

            backupStream();

            try {
                query.add(new Parameter<>("Hash", Etag.etag(new ByteArrayInputStream(cacheOutputStream.toByteArray()), UfileConstants.MULTIPART_SIZE).geteTag()));
            } catch (IOException e) {
                throw new UfileIOException("Calculate ETag failed!", e);
            }

            query.add(new Parameter<>("FileName", keyName));
            query.add(new Parameter<>("FileSize", String.valueOf(new ByteArrayInputStream(cacheOutputStream.toByteArray()).available())));

            call = builder.baseUrl(builder.generateGetUrl(url, query))
                    .addHeader("Accpet", "*/*")
                    .addHeader("Date", date)
                    .addHeader("authorization", authorization)
                    .build(httpClient.getOkHttpClient());
        } catch (ValidatorException e) {
            throw new UfileRequiredParamNotFoundException(e.getMessage());
        }
    }

    /**
     * 备份流
     */
    private void backupStream() {
        cacheOutputStream = new ByteArrayOutputStream();
        byte[] buff = new byte[64 * 1024];
        int len = 0;
        try {
            while ((len = inputStream.read(buff)) > 0) {
                cacheOutputStream.write(buff, 0, len);
            }

            cacheOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            FileUtil.close(inputStream);
        }
    }

}
