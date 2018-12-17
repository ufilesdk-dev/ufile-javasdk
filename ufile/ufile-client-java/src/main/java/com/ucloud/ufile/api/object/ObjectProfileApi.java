package com.ucloud.ufile.api.object;

import com.ucloud.ufile.auth.ObjectAuthorizer;
import com.ucloud.ufile.bean.ObjectProfile;
import com.ucloud.ufile.bean.UfileErrorBean;
import com.ucloud.ufile.exception.UfileException;
import com.ucloud.ufile.exception.UfileRequiredParamNotFoundException;
import com.ucloud.ufile.http.UfileHttpException;
import com.ucloud.ufile.http.HttpClient;
import com.ucloud.ufile.http.request.HeadRequestBuilder;
import com.ucloud.ufile.util.HttpMethod;
import com.ucloud.ufile.util.ParameterValidator;
import okhttp3.Response;
import sun.security.validator.ValidatorException;

import javax.validation.constraints.NotEmpty;
import java.util.Date;

/**
 * API-获取云端对象描述信息
 *
 * @author: joshua
 * @E-mail: joshua.yin@ucloud.cn
 * @date: 2018/11/12 19:09
 */
public class ObjectProfileApi extends UfileObjectApi<ObjectProfile> {
    /**
     * Required
     * 云端对象名称
     */
    @NotEmpty(message = "KeyName is required")
    private String keyName;
    /**
     * Required
     * Bucket空间名称
     */
    @NotEmpty(message = "BucketName is required")
    private String bucketName;

    /**
     * 构造方法
     *
     * @param authorizer Object授权器
     * @param host       API域名
     * @param httpClient Http客户端
     */
    protected ObjectProfileApi(ObjectAuthorizer authorizer, String host, HttpClient httpClient) {
        super(authorizer, host, httpClient);
    }

    /**
     * 配置需要获取信息的云端对象名称
     *
     * @param keyName 对象名称
     * @return {@link ObjectProfileApi}
     */
    public ObjectProfileApi which(String keyName) {
        this.keyName = keyName;
        return this;
    }

    /**
     * 配置需要获取信息的对象所在的Bucket
     *
     * @param bucketName bucket名称
     * @return {@link ObjectProfileApi}
     */
    public ObjectProfileApi atBucket(String bucketName) {
        this.bucketName = bucketName;
        return this;
    }

    @Override
    protected void prepareData() throws UfileException {
        try {
            ParameterValidator.validator(this);

            String contentType = "application/json; charset=utf-8";
            String date = dateFormat.format(new Date(System.currentTimeMillis()));
            String authorization = authorizer.authorization(HttpMethod.HEAD, bucketName, keyName,
                    contentType, "", date);

            call = new HeadRequestBuilder()
                    .baseUrl(generateFinalHost(bucketName, keyName))
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
    public ObjectProfile parseHttpResponse(Response response) throws Exception {
        ObjectProfile result = new ObjectProfile();
        int code = response.code();

        if (code == 200) {
            result.setContentLength(Long.parseLong(response.header("Content-Length", "0")));
            result.setContentType(response.header("Content-Type", ""));
            result.seteTag(response.header("ETag", "").replace("\"", ""));
            result.setAcceptRanges(response.header("Accept-Ranges", ""));
            result.setLastModified(response.header("Last-Modified", ""));
            result.setBucket(bucketName);
            result.setKeyName(keyName);

            return result;
        } else {
            throw new UfileHttpException(parseErrorResponse(response).toString());
        }
    }

    @Override
    public UfileErrorBean parseErrorResponse(Response response) {
        UfileErrorBean errorBean = new UfileErrorBean();
        errorBean.setxSessionId(response.header("X-SessionId"));
        int code = response.code();
        errorBean.setRetCode(code);
        switch (code) {
            case 404: {
                errorBean.setErrMsg(String.format("Response-Code : %d, the which is inexistent in the bucket", code));
                break;
            }
            default: {
                errorBean.setErrMsg(String.format("Response-Code : %d", code));
                break;
            }
        }

        return errorBean;
    }
}
