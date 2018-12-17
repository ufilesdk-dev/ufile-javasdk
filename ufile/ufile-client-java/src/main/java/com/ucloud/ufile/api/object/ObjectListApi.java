package com.ucloud.ufile.api.object;

import com.ucloud.ufile.auth.ObjectAuthorizer;
import com.ucloud.ufile.bean.ObjectListBean;
import com.ucloud.ufile.exception.UfileException;
import com.ucloud.ufile.exception.UfileRequiredParamNotFoundException;
import com.ucloud.ufile.http.HttpClient;
import com.ucloud.ufile.http.request.GetRequestBuilder;
import com.ucloud.ufile.util.HttpMethod;
import com.ucloud.ufile.util.Parameter;
import com.ucloud.ufile.util.ParameterValidator;
import sun.security.validator.ValidatorException;

import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * API-获取云端对象列表
 *
 * @author: joshua
 * @E-mail: joshua.yin@ucloud.cn
 * @date: 2018/11/12 19:09
 */
public class ObjectListApi extends UfileObjectApi<ObjectListBean> {
    /**
     * Optional
     * 过滤前缀
     */
    private String prefix;
    /**
     * Optional
     * 分页标记
     */
    private String marker;
    /**
     * Optional
     * 分页数据上限，Default = 20
     */
    private Integer limit;

    /**
     * Required
     * Bucket空间名称
     */
    @NotEmpty(message = "BucketName is required to set through method 'atBucket'")
    private String bucketName;

    /**
     * 构造方法
     *
     * @param authorizer Object授权器
     * @param host       API域名
     * @param httpClient Http客户端
     */
    protected ObjectListApi(ObjectAuthorizer authorizer, String host, HttpClient httpClient) {
        super(authorizer, host, httpClient);
    }

    /**
     * 配置指定Bucket
     *
     * @param bucketName bucket名称
     * @return {@link ObjectListApi}
     */
    public ObjectListApi atBucket(String bucketName) {
        this.bucketName = bucketName;
        return this;
    }

    /**
     * 配置过滤前缀
     *
     * @param prefix 前缀
     * @return {@link ObjectListApi}
     */
    public ObjectListApi withPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    /**
     * 配置分页标记
     *
     * @param marker 分页标记
     * @return {@link ObjectListApi}
     */
    public ObjectListApi withMarker(String marker) {
        this.marker = marker;
        return this;
    }

    /**
     * 配置分页数据长度
     *
     * @param limit 分页数据长度
     * @return {@link ObjectListApi}
     */
    public ObjectListApi dataLimit(int limit) {
        this.limit = limit;
        return this;
    }

    @Override
    protected void prepareData() throws UfileException {
        try {
            ParameterValidator.validator(this);
            List<Parameter<String>> query = new ArrayList<>();
            if (prefix != null)
                query.add(new Parameter<>("prefix", prefix));
            if (marker != null)
                query.add(new Parameter<>("marker", marker));
            if (limit != null)
                query.add(new Parameter<>("limit", String.valueOf(limit.intValue())));

            String contentType = "application/json; charset=utf-8";
            String date = dateFormat.format(new Date(System.currentTimeMillis()));

            String authorization = authorizer.authorization(HttpMethod.GET, bucketName, "",
                    contentType, "", date);

            GetRequestBuilder builder = new GetRequestBuilder();
            call = builder.baseUrl(generateFinalHost(bucketName, "") + "?list&" + builder.generateUrlQuery(query))
                    .addHeader("Content-Type", contentType)
                    .addHeader("Accpet", "*/*")
                    .addHeader("Date", date)
                    .addHeader("authorization", authorization)
                    .build(httpClient.getOkHttpClient());
        } catch (ValidatorException e) {
            throw new UfileRequiredParamNotFoundException(e.getMessage(), e);
        }
    }
}
