package com.ucloud.ufile.api.object.multi;

import com.ucloud.ufile.api.object.UfileObjectApi;
import com.ucloud.ufile.auth.ObjectAuthorizer;
import com.ucloud.ufile.exception.UfileException;
import com.ucloud.ufile.exception.UfileRequiredParamNotFoundException;
import com.ucloud.ufile.http.HttpClient;
import com.ucloud.ufile.http.request.PostJsonRequestBuilder;
import com.ucloud.ufile.util.HttpMethod;
import com.ucloud.ufile.util.ParameterValidator;
import okhttp3.MediaType;
import okhttp3.Response;
import sun.security.validator.ValidatorException;

import javax.validation.constraints.NotEmpty;
import java.util.Date;

/**
 * API-初始化分片上传
 *
 * @author: joshua
 * @E-mail: joshua.yin@ucloud.cn
 * @date: 2018/11/12 19:08
 */
public class InitMultiUploadApi extends UfileObjectApi<MultiUploadInfo> {
    /**
     * Required
     * 上传云端后的文件名
     */
    @NotEmpty(message = "KeyName is required to set through method 'nameAs'")
    protected String keyName;

    /**
     * Required
     * 上传对象的mimeType
     */
    @NotEmpty(message = "MimeType is required to set through method 'withMimeType'")
    protected String mimeType;

    /**
     * Required
     * 要上传的目标Bucket
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
    public InitMultiUploadApi(ObjectAuthorizer authorizer, String host, HttpClient httpClient) {
        super(authorizer, host, httpClient);
    }

    /**
     * 配置分片上传到云端的对象名称
     *
     * @param keyName 对象名称
     * @return {@link InitMultiUploadApi}
     */
    public InitMultiUploadApi nameAs(String keyName) {
        this.keyName = keyName;
        return this;
    }

    /**
     * 配置分片上传到云端的对象的MIME类型
     *
     * @param mimeType MIME类型
     * @return {@link InitMultiUploadApi}
     */
    public InitMultiUploadApi withMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    /**
     * 配置分片上传到的Bucket
     *
     * @param bucketName bucket名称
     * @return {@link InitMultiUploadApi}
     */
    public InitMultiUploadApi toBucket(String bucketName) {
        this.bucketName = bucketName;
        return this;
    }

    @Override
    protected void prepareData() throws UfileException {
        try {
            ParameterValidator.validator(this);

            String contentType = MediaType.parse(mimeType).toString();
            String date = dateFormat.format(new Date(System.currentTimeMillis()));
            String authorization = authorizer.authorization(HttpMethod.POST, bucketName, keyName,
                    contentType, "", date);

            PostJsonRequestBuilder builder = new PostJsonRequestBuilder();
            call = builder.baseUrl(generateFinalHost(bucketName, keyName) + "?uploads")
                    .addHeader("Content-Type", contentType)
                    .addHeader("Accpet", "*/*")
                    .addHeader("Date", date)
                    .addHeader("authorization", authorization)
                    .build(httpClient.getOkHttpClient());
        } catch (ValidatorException e) {
            throw new UfileRequiredParamNotFoundException(e.getMessage());
        }
    }

    @Override
    public MultiUploadInfo parseHttpResponse(Response response) throws Exception {
        MultiUploadInfo state = super.parseHttpResponse(response);
        state.setMimeType(mimeType);
        return state;
    }
}
