package com.keenon.sdk.http;

import android.annotation.SuppressLint;
import com.keenon.common.utils.LogUtils;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import org.eclipse.californium.core.coap.CoAP;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/http/OkHttpFactory.class */
public class OkHttpFactory {
    private OkHttpClient client = null;
    private static OkHttpFactory instance = new OkHttpFactory();
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static final MediaType STREAM = MediaType.parse("application/octet-stream");

    public static OkHttpFactory getInstance() {
        return instance;
    }

    private OkHttpFactory() {
    }

    public OkHttpClient getClient() {
        if (this.client == null) {
            synchronized (OkHttpFactory.class) {
                if (this.client == null) {
                    this.client = new OkHttpClient.Builder().pingInterval(15000L, TimeUnit.SECONDS).readTimeout(15000L, TimeUnit.SECONDS).writeTimeout(15000L, TimeUnit.SECONDS).connectTimeout(15000L, TimeUnit.SECONDS).retryOnConnectionFailure(true).addInterceptor(new HttpLoggingInterceptor().setLevel(LogUtils.LOG_LEVEL <= 3 ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE)).addInterceptor(new Interceptor() { // from class: com.keenon.sdk.http.OkHttpFactory.1
                        public Response intercept(Interceptor.Chain chain) throws IOException {
                            Request original = chain.request();
                            Request.Builder requestBuilder = original.newBuilder().method(original.method(), original.body());
                            requestBuilder.addHeader("Connection", "close");
                            Request request = requestBuilder.build();
                            return chain.proceed(request);
                        }
                    }).build();
                }
            }
        }
        return this.client;
    }

    @SuppressLint({"TrulyRandom"})
    private static SSLSocketFactory createSSLSocketFactory() {
        SSLSocketFactory sSLSocketFactory = null;
        try {
            SSLContext sc = SSLContext.getInstance(CoAP.PROTOCOL_TLS);
            sc.init(null, new TrustManager[]{new TrustAllManager()}, new SecureRandom());
            sSLSocketFactory = sc.getSocketFactory();
        } catch (Exception e) {
        }
        return sSLSocketFactory;
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/http/OkHttpFactory$TrustAllManager.class */
    private static class TrustAllManager implements X509TrustManager {
        private TrustAllManager() {
        }

        @Override // javax.net.ssl.X509TrustManager
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override // javax.net.ssl.X509TrustManager
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override // javax.net.ssl.X509TrustManager
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/http/OkHttpFactory$TrustAllHostnameVerifier.class */
    private static class TrustAllHostnameVerifier implements HostnameVerifier {
        private TrustAllHostnameVerifier() {
        }

        @Override // javax.net.ssl.HostnameVerifier
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }
}
