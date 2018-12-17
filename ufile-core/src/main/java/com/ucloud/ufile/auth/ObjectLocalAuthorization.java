package com.ucloud.ufile.auth;


import com.ucloud.ufile.auth.sign.Signer;
import com.ucloud.ufile.auth.sign.UfileSigner;

/**
 * 本地授权者
 *
 * @author: joshua
 * @E-mail: joshua.yin@ucloud.cn
 * @date: 2018/11/6 18:50
 */
public abstract class ObjectLocalAuthorization extends ObjectAuthorization {
    /**
     * 用户私钥
     */
    protected String privateKey;
    /**
     * 签名器 {@link Signer}
     */
    protected Signer signer;

    /**
     * 构造方法 (使用UFile默认签名器 {@link UfileSigner})
     *
     * @param publicKey  用户公钥
     * @param privateKey 用户私钥
     */
    protected ObjectLocalAuthorization(String publicKey, String privateKey) {
        this(publicKey, privateKey, new UfileSigner());
    }

    /**
     * 构造方法
     *
     * @param publicKey  用户公钥
     * @param privateKey 用户私钥
     * @param signer     签名器
     */
    protected ObjectLocalAuthorization(String publicKey, String privateKey, Signer signer) {
        super(publicKey);
        this.privateKey = privateKey;
        this.signer = signer;
    }
}
