package cn.ucloud.ufile.sample.object;

import cn.ucloud.ufile.UfileClient;
import cn.ucloud.ufile.api.ApiError;
import cn.ucloud.ufile.api.object.ObjectConfig;
import cn.ucloud.ufile.bean.ObjectListBean;
import cn.ucloud.ufile.bean.UfileErrorBean;
import cn.ucloud.ufile.http.UfileCallback;
import cn.ucloud.ufile.sample.Constants;
import cn.ucloud.ufile.util.JLog;
import okhttp3.Request;

/**
 *
 * @author: joshua
 * @E-mail: joshua.yin@ucloud.cn
 * @date: 2018-12-11 14:32
 */
public class ObjectListSample {
    private static final String TAG = "ObjectListSample";
    private static ObjectConfig config = new ObjectConfig("cn-sh2", "ufileos.com");

    public static void main(String[] args) {
        String bucketName = "new-bucket";

        UfileClient.object(Constants.OBJECT_AUTHORIZER, config)
                .objectList(bucketName)
                /**
                 * 过滤前缀
                 */
//                .withPrefix("")
                /**
                 * 分页标记
                 */
//                .withMarker("")
                /**
                 * 分页数据上限，Default = 20
                 */
//                .dataLimit(10)
                .executeAsync(new UfileCallback<ObjectListBean>() {

                    @Override
                    public void onResponse(ObjectListBean response) {
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
