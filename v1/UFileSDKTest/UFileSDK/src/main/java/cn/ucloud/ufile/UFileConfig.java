package cn.ucloud.ufile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class UFileConfig {
    private String ucloudPublicKey;
    private String ucloudPrivateKey;
    private String proxySuffix;
    private String downloadProxySuffix;

    public UFileConfig() {
        this.ucloudPublicKey = "";
        this.ucloudPrivateKey = "";
        this.proxySuffix = "";
        this.downloadProxySuffix = "";
    }

    public UFileConfig(String ucloudPublicKey, String ucloudPrivateKey, String proxySuffix, String downloadProxySuffix) {
        this.ucloudPublicKey = ucloudPublicKey;
        this.ucloudPrivateKey = ucloudPrivateKey;
        this.proxySuffix = proxySuffix;
        this.downloadProxySuffix = downloadProxySuffix;
    }

    public UFileConfig(String configPath) {
        loadConfig(configPath);
    }

    private void loadConfig(String configPath) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(configPath);
            Properties configProperties = new Properties();
            configProperties.load(inputStream);
            this.ucloudPublicKey = configProperties.getProperty("UCloudPublicKey");
            this.ucloudPrivateKey = configProperties.getProperty("UCloudPrivateKey");
            this.proxySuffix = configProperties.getProperty("ProxySuffix");
            this.downloadProxySuffix = configProperties.getProperty("DownloadProxySuffix");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getUcloudPublicKey() {
        return ucloudPublicKey;
    }

    public void setUcloudPublicKey(String ucloudPublicKey) {
        this.ucloudPublicKey = ucloudPublicKey;
    }

    public String getUcloudPrivateKey() {
        return ucloudPrivateKey;
    }

    public void setUcloudPrivateKey(String ucloudPrivateKey) {
        this.ucloudPrivateKey = ucloudPrivateKey;
    }

    public String getProxySuffix() {
        return proxySuffix;
    }

    public void setProxySuffix(String proxySuffix) {
        this.proxySuffix = proxySuffix;
    }

    public String getDownloadProxySuffix() {
        return downloadProxySuffix;
    }

    public void setDownloadProxySuffix(String downloadProxySuffix) {
        this.downloadProxySuffix = downloadProxySuffix;
    }
}
