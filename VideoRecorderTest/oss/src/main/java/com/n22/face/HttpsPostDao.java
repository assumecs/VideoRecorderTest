package com.n22.face;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.n22.face.inter.PassIn;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpsPostDao {
	private static PassIn in;

	public static void setIn(PassIn in) {
		HttpsPostDao.in = in;
	}

	public static void postFace(String imgStr) throws NoSuchAlgorithmException, KeyManagementException {
		X509TrustManager[] manager = new X509TrustManager[] { new X509TrustManager() {

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

			}

			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

			}
		} };

		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, manager, null);

		HostnameVerifier hostnameVerifier = new HostnameVerifier() {

			@Override
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};

		OkHttpClient client = new OkHttpClient.Builder().hostnameVerifier(hostnameVerifier)
				.sslSocketFactory(sslContext.getSocketFactory(), manager[0])
				.readTimeout(30, TimeUnit.SECONDS)
				.writeTimeout(30, TimeUnit.SECONDS)
				.connectTimeout(30, TimeUnit.SECONDS)
				.build();

		String url = "https://dm-21.data.aliyun.com/rest/160601/face/detection.json";
		String bodys = "{\"inputs\":[{\"type\":{\"dataType\":10,\"dataValue\":1},\"image\":{\"dataType\":50,\"dataValue\":\"" + imgStr + "\"}}]}";

		MediaType type = MediaType.parse("application/json; charset=utf-8");
		RequestBody body = RequestBody.create(type, bodys);

		Request.Builder reBuilder = new Request.Builder().url(url);

		reBuilder.addHeader("Authorization", "APPCODE f51b873fb805494f9e39bab3e1a80c80");

		Request request = reBuilder.post(body).build();

		Call call = client.newCall(request);

		call.enqueue(new Callback() {

			@Override
			public void onResponse(Call arg0, Response response) {
				FacePass(response);
			}

			@Override
			public void onFailure(Call arg0, IOException arg1) {
				System.out.println(arg1);
				in.Fail();
			}
		});
	}

	/**人脸解析数据返回*/
	private static void FacePass(Response response) {
		try {
			JSONObject jsonObject;
			jsonObject = new JSONObject(response.body().string());
			JSONArray array = jsonObject.getJSONArray("outputs");
			for (int i = 0; i < array.length(); i++) {
				jsonObject = array.getJSONObject(i);
				if (jsonObject.get("outputValue") != null) {
					jsonObject = jsonObject.getJSONObject("outputValue");
					String str = jsonObject.getString("dataValue");
					jsonObject = new JSONObject(str);
					String errno = jsonObject.getString("errno");
					String number = jsonObject.getString("number");
					if (in != null) {
						in.Success(errno, number);
					}
				}else{
					if (in != null) {
						in.Fail();
					}
				}
			}
		} catch (JSONException e) {
			if (in != null) {
				in.Fail();
			}
			System.out.println(e);
		} catch (IOException e) {
			if (in != null) {
				in.Fail();
			}
			System.out.println(e);
		}
	}

}
