package com.ucloud.ufile.bean;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.ucloud.ufile.bean.base.BaseResponseBean;


/**
 *
 * @author: joshua
 * @E-mail: joshua.yin@ucloud.cn
 * @date: 2018/11/15 16:52
 */
public class PutObjectResultBean extends BaseResponseBean {
    @SerializedName("ETag")
    private String eTag;

    public String geteTag() {
        return eTag;
    }

    public void seteTag(String eTag) {
        this.eTag = eTag;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
