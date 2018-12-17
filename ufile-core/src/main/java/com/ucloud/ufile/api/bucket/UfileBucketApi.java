package com.ucloud.ufile.api.bucket;

import com.ucloud.ufile.annotation.UcloudParam;
import com.ucloud.ufile.auth.BucketAuthorizer;
import com.ucloud.ufile.api.UfileApi;
import com.ucloud.ufile.exception.UfileException;
import com.ucloud.ufile.exception.UfileRequiredParamNotFoundException;
import com.ucloud.ufile.http.HttpClient;
import com.ucloud.ufile.http.request.GetRequestBuilder;
import com.ucloud.ufile.util.Parameter;
import com.ucloud.ufile.util.ParameterMaker;
import com.ucloud.ufile.util.ParameterValidator;
import okhttp3.MediaType;
import sun.security.validator.ValidatorException;

import javax.validation.constraints.NotEmpty;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Ufile Bucket相关API基类
 *
 * @author: joshua
 * @E-mail: joshua.yin@ucloud.cn
 * @date: 2018/11/13 11:21
 */
public abstract class UfileBucketApi<T> extends UfileApi<T> {
    protected final String TAG = getClass().getSimpleName();

    /**
     * UCloud Ufile Bucket 域名
     */
    protected static final String UFILE_BUCKET_API_HOST = "http://api.ucloud.cn";

    /**
     * Bucket API 请求动作描述
     */
    @NotEmpty(message = "Action is required")
    @UcloudParam("Action")
    protected String action;
    /**
     * Bucket API授权器
     */
    protected BucketAuthorizer authorizer;

    /**
     * 构造方法
     *
     * @param authorizer Bucket授权器
     * @param httpClient Http客户端
     */
    protected UfileBucketApi(BucketAuthorizer authorizer, HttpClient httpClient, String action) {
        super(httpClient, UFILE_BUCKET_API_HOST);
        this.authorizer = authorizer;
        this.action = action;
    }

    @Override
    protected void prepareData() throws UfileException {
        try {
            ParameterValidator.validator(this);

            List<Parameter<String>> query = ParameterMaker.makeParameter(this);
            query.add(new Parameter("PublicKey", authorizer.getPublicKey()));
            String signature = authorizer.authorizeBucketUrl(query);
            query.add(new Parameter<>("Signature", signature));

            call = new GetRequestBuilder()
                    .baseUrl(host)
                    .addHeader("Content-Type", "application/json; charset=utf-8")
                    .addHeader("Accpet", "*/*")
                    .params(query)
                    .mediaType(MediaType.parse("application/json; charset=utf-8"))
                    .build(httpClient.getOkHttpClient());
        } catch (ValidatorException e) {
            throw new UfileRequiredParamNotFoundException(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new UfileException(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            throw new UfileException(e.getMessage(), e);
        }
    }

}
