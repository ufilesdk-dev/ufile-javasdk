package cn.ucloud.ufile.sdk.test;

import cn.ucloud.ufile.UFileClient;
import cn.ucloud.ufile.UFileConfig;
import cn.ucloud.ufile.UFileRequest;
import cn.ucloud.ufile.UFileResponse;
import cn.ucloud.ufile.sender.DeleteSender;
import cn.ucloud.ufile.sender.GetSender;
import cn.ucloud.ufile.sender.PutSender;
import org.apache.http.Header;

import java.io.*;

/**
 * Put上传测试
 *
 * @author york
 */
public class UFilePutTest {
    private static UFileConfig uFileConfig = null;

    public static void main(String args[]) {
        String bucketName = "lapd";
        String key = "123456.jpg";
        String saveAsPath = "/Users/joshua/Downloads/test2.jpg";
        String filePath = "/Users/joshua/Downloads/test.jpg";
        String configPath = "";

        //加载配置项
        //UFileConfig.getInstance().loadConfig(configPath);

        //或者逐个设置配置项
        uFileConfig = new UFileConfig();

        uFileConfig.setUcloudPublicKey("TOKEN_e6884790-98c4-4fe7-956e-9ef3d7c3b3d9");
        uFileConfig.setUcloudPrivateKey("42197a75-5e3b-4a98-b71c-896210db5c8e");
        uFileConfig.setProxySuffix(".cn-bj.ufileos.com");
        uFileConfig.setDownloadProxySuffix(".cn-bj.ufileos.com");

        UFileRequest request = new UFileRequest();
        request.setBucketName(bucketName);
        request.setKey(key);
        request.setFilePath(filePath);

        UFileClient ufileClient = null;
        /*
         *     一个请求，使用一个UFileClient，亦即使用一个HTTPclient，发送请求，收到响应，关闭连接
         */
        System.out.println("PutFile BEGIN ...");
        try {
            ufileClient = new UFileClient(uFileConfig);
            putFile(ufileClient, request);
        } finally {
            ufileClient.shutdown();
        }
        System.out.println("PutFile END ...\n\n");


        System.out.println("GetFile BEGIN...");
        try {
            ufileClient = new UFileClient(uFileConfig);
            getFile(ufileClient, request, saveAsPath);
        } finally {
            ufileClient.shutdown();
        }
        System.out.println("GetFile END ...\n\n");
    }

    private static void putFile(UFileClient ufileClient, UFileRequest request) {
        PutSender sender = new PutSender();
        sender.makeAuth(ufileClient, request);

        UFileResponse response = sender.send(ufileClient, request);
        if (response != null) {

            System.out.println("status line: " + response.getStatusLine());

            Header[] headers = response.getHeaders();
            for (int i = 0; i < headers.length; i++) {
                System.out.println("header " + headers[i].getName() + " : " + headers[i].getValue());
            }

            System.out.println("body length: " + response.getContentLength());

            InputStream inputStream = response.getContent();
            if (inputStream != null) {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    String s = "";
                    while ((s = reader.readLine()) != null) {
                        System.out.println(s);
                    }

                } catch (IOException e) {
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
        }
    }

    private static void getFile(UFileClient ufileClient, UFileRequest request, String saveAsPath) {
        GetSender sender = new GetSender();
        sender.makeAuth(ufileClient, request);

        UFileResponse response = sender.send(ufileClient, request);
        if (response != null) {

            System.out.println("status line: " + response.getStatusLine());

            Header[] headers = response.getHeaders();
            for (int i = 0; i < headers.length; i++) {
                System.out.println("header " + headers[i].getName() + " : " + headers[i].getValue());
            }

            System.out.println("body length: " + response.getContentLength());

            //handler error response
            if (response.getStatusLine().getStatusCode() != 200 && response.getContent() != null) {
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new InputStreamReader(response.getContent()));
                    String input;
                    while ((input = br.readLine()) != null) {
                        System.out.println(input);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (response.getContent() != null) {
                        try {
                            response.getContent().close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                InputStream inputStream = null;
                OutputStream outputStream = null;
                try {
                    inputStream = response.getContent();
                    outputStream = new BufferedOutputStream(new FileOutputStream(saveAsPath));
                    int bufSize = 1024 * 4;
                    byte[] buffer = new byte[bufSize];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private static void deleteFile(UFileClient ufileClient, UFileRequest request) {
        DeleteSender sender = new DeleteSender();
        sender.makeAuth(ufileClient, request);

        UFileResponse response = sender.send(ufileClient, request);
        if (response != null) {
            System.out.println("status line: " + response.getStatusLine());
            Header[] headers = response.getHeaders();
            for (int i = 0; i < headers.length; i++) {
                System.out.println("header " + headers[i].getName() + " : " + headers[i].getValue());
            }

            System.out.println("body length: " + response.getContentLength());

            if (response.getContent() != null) {
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new InputStreamReader(response.getContent()));
                    String input;
                    while ((input = br.readLine()) != null) {
                        System.out.println(input);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
