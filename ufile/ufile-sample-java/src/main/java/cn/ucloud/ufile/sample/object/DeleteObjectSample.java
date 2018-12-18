package cn.ucloud.ufile.sample.object;

import cn.ucloud.ufile.UfileClient;
import cn.ucloud.ufile.api.ApiError;
import cn.ucloud.ufile.api.object.ObjectConfig;
import cn.ucloud.ufile.bean.base.BaseResponseBean;
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
public class DeleteObjectSample {
    private static final String TAG = "DeleteObjectSample";
    private static ObjectConfig config = new ObjectConfig("your bucket region", "ufileos.com");

    public static void main(String[] args) {
        String keyName = "which";
        String bucketName = "bucketName";

        UfileClient.object(Constants.OBJECT_AUTHORIZER, config)
                .deleteObject(keyName, bucketName)
                .executeAsync(new UfileCallback<BaseResponseBean>() {

                    @Override
                    public void onResponse(BaseResponseBean response) {
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
