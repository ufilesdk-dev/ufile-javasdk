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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * API-文件秒传
 *
 * @author: joshua
 * @E-mail: joshua.yin@ucloud.cn
 * @date: 2018/11/12 19:08
 */
public class UploadFileHitApi extends UfileObjectApi<BaseResponseBean> {
    /**
     * Required
     * 云端对象名称
     */
    @NotEmpty(message = "KeyName is required to set through method 'nameAs'")
    protected String keyName;
    /**
     * Required
     * 要上传的文件
     */
    @NotNull(message = "File is required")
    private File file;
    /**
     * Required
     * Bucket空间名称
     */
    @NotEmpty(message = "BucketName is required to set through method 'toBucket'")
    protected String bucketName;

    /**
     * 构造方法
     *
     * @param authorizer Object授权器
     * @param host       API域名
     * @param httpClient Http客户端
     */
    protected UploadFileHitApi(ObjectAuthorizer authorizer, String host, HttpClient httpClient) {
        super(authorizer, host, httpClient);
    }

    /**
     * 设置上传到云端的对象名称
     *
     * @param keyName 对象名称
     * @return {@link UploadFileHitApi}
     */
    public UploadFileHitApi nameAs(String keyName) {
        this.keyName = keyName;
        return this;
    }

    /**
     * 设置要上传的文件
     *
     * @param file 需上传的文件
     * @return {@link UploadFileHitApi}
     */
    public UploadFileHitApi from(File file) {
        this.file = file;
        return this;
    }

    /**
     * 设置要上传到的Bucket名称
     *
     * @param bucketName bucket名称
     * @return {@link UploadFileHitApi}
     */
    public UploadFileHitApi toBucket(String bucketName) {
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
            try {
                query.add(new Parameter<>("Hash", Etag.etag(file, UfileConstants.MULTIPART_SIZE).geteTag()));
            } catch (IOException e) {
                throw new UfileIOException("Calculate ETag failed!", e);
            }
            query.add(new Parameter<>("FileName", keyName));
            query.add(new Parameter<>("FileSize", String.valueOf(file.length())));

            call = builder.baseUrl(builder.generateGetUrl(url, query))
                    .addHeader("Accpet", "*/*")
                    .addHeader("Date", date)
                    .addHeader("authorization", authorization)
                    .build(httpClient.getOkHttpClient());
        } catch (ValidatorException e) {
            throw new UfileRequiredParamNotFoundException(e.getMessage());
        }
    }
}
