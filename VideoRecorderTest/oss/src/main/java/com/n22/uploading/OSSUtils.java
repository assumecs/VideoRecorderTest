package com.n22.uploading;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSStsTokenCredentialProvider;
import com.alibaba.sdk.android.oss.internal.OSSAsyncTask;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;
import com.n22.uploading.bean.OssStsResponse;
import com.n22.uploading.idle.BeanUtil;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * OSS 改造上传影像件
 * 
 * @author WWD by time 2016.11.23 modify time 2017.03.22
 */
public class OSSUtils {
	/** 数据传递规则 */
	public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	/** 调用永久地址 */
	public final String ENDPOINT = "http://oss-cn-szfinance.aliyuncs.com";
	/** OSS盘符地址（可变更） */
	private String bucketName = "tpad-sl";
	/** 初始化時数据传输Token工具 */
	private OSSCredentialProvider provider;
	/** 上传使用的OSSclent */
	private OSS oss;
	/** OSS异步上传工具 */
	private OSSAsyncTask task;
	/** 数据载体 */
	private PutObjectRequest objectRequest;
	/** OSS结果回调接口 */
	private OSSCompletedCallback<PutObjectRequest, PutObjectResult> ossCompletedCallback;
	/** OSS异步上传时会有回调进度，进度回调接口 */
	private OSSProgressCallback<PutObjectRequest> progressCallback;
	/** OSS上传使用工具 */
	private BeanUtil beanUtil;
	/** 临时存储数据 */
	private Object TEMPORARY_DATA;
	/** 临时存储数据 */
	private Object TEMPORARY_REQDATA;
	/** oss上传文件数量 */
	public int size = 0;

	public OSSCompletedCallback<PutObjectRequest, PutObjectResult> getOssCompletedCallback() {
		if (ossCompletedCallback == null) {
			Log.e("OSSIllegalArgumentException", "No callback set!");
			return new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {

				@Override
				public void onSuccess(PutObjectRequest paramT1, PutObjectResult paramT2) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onFailure(PutObjectRequest paramT1, ClientException paramClientException, ServiceException paramServiceException) {
					// TODO Auto-generated method stub

				}
			};
		} else
			return ossCompletedCallback;
	}

	public void setOssCompletedCallback(OSSCompletedCallback<PutObjectRequest, PutObjectResult> ossCompletedCallback) {
		this.ossCompletedCallback = ossCompletedCallback;
	}

	public OSSProgressCallback<PutObjectRequest> getProgressCallback() {
		return progressCallback;
	}

	public void setProgressCallback(OSSProgressCallback<PutObjectRequest> progressCallback) {
		this.progressCallback = progressCallback;
	}

	public BeanUtil getBeanUtil() {
		return beanUtil;
	}

	public void setBeanUtil(BeanUtil beanUtil) {
		this.beanUtil = beanUtil;
	}

	private Context context;

	public OSSUtils(Context context, BeanUtil bUtil) {
		this.context = context;
		this.beanUtil = bUtil;
	}

	/** 初始化OSS */
	public boolean initOSS() {

		if (beanUtil == null) {
			getOssCompletedCallback().onFailure(null, new ClientException("初始化数据接口为定义！"), null);
		}

		if (provider == null) {
			provider = getToken(beanUtil.ObtainToken(), beanUtil.BaseUrl());
		}

		if (provider == null) {
			getOssCompletedCallback().onFailure(null, new ClientException("获取Token失败！"), null);
		}

		try {
			oss = new OSSClient(context, ENDPOINT, provider);
		} catch (Exception e) {
			getOssCompletedCallback().onFailure(null, new ClientException("获取Token失败！"), null);
		}

		if (oss != null) {
			return true;
		}

		return false;
	}

	/** 获取临时Token联网接口 */
	private OSSCredentialProvider getToken(String json, String url) {
		try {
			OkHttpClient client = new OkHttpClient();
			client.newBuilder().connectTimeout(15, TimeUnit.SECONDS).writeTimeout(300, TimeUnit.SECONDS).readTimeout(300, TimeUnit.SECONDS);
			RequestBody body = RequestBody.create(JSON, json);
			Request request = new Request.Builder().url(url).post(body).build();
			Response response = client.newCall(request).execute();
			if (response.isSuccessful()) {
				String decode = beanUtil.AnalyticMessage(response.body().string());
				if (TextUtils.isEmpty(decode)) {
					getOssCompletedCallback().onFailure(null, new ClientException("获取Token数据解析错误！"), null);
				} else {
					OssStsResponse res = beanUtil.JsonUtilJTO(decode, OssStsResponse.class);
					if (res == null) {
						return null;
					}
					if (!TextUtils.isEmpty(res.ossStsResponse.bucketName)) {
						this.bucketName = res.ossStsResponse.bucketName;
					}
					return new OSSStsTokenCredentialProvider(res.ossStsResponse.accessKeyId, res.ossStsResponse.accessKeySecret, res.ossStsResponse.securityToken);
				}
			}
		} catch (IOException e) {
			getOssCompletedCallback().onFailure(null, new ClientException(e.getLocalizedMessage()), null);
		}
		return null;
	}

	/** 上传OSS 这里采用异步上传 */
	public void putObjectOSS(String filename, String filepath) {
		objectRequest = new PutObjectRequest(this.bucketName, filename, filepath);
		if (progressCallback != null) {
			objectRequest.setProgressCallback(progressCallback);
		}
		if (oss != null) {
			task = oss.asyncPutObject(objectRequest, ossCompletedCallback);
		} else {
			getOssCompletedCallback().onFailure(null, new ClientException("未初始化 OSSClient ，请检查！"), null);
		}
	}

	/**
	 * 上传OSS 这里采用异步上传 双份
	 */
	public void putObjectOSSDouble(String filename, String filepath, Object asfi, Object messageReq) {
		this.TEMPORARY_DATA = asfi;
		this.TEMPORARY_REQDATA = messageReq;
		objectRequest = new PutObjectRequest(this.bucketName, filename, filepath);
		if (progressCallback != null) {
			objectRequest.setProgressCallback(progressCallback);
		}
		if (oss != null) {
			task = oss.asyncPutObject(objectRequest, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {

				@Override
				public void onSuccess(PutObjectRequest paramT1, PutObjectResult paramT2) {

					List<Object> list = beanUtil.AssemblyData(TEMPORARY_DATA);
					if (list != null) {
						if (list.size() == size) {
							Map<String, String> encapsulationData = (Map<String, String>) beanUtil.EncapsulationData(TEMPORARY_REQDATA);
							putObjectMssageServer(encapsulationData.get("D_Data"), encapsulationData.get("D_Url"));
						} else {
							getOssCompletedCallback().onSuccess(paramT1, paramT2);
						}
					} else {
						getOssCompletedCallback().onFailure(null, new ClientException("执行上传失败，请重试！"), null);
					}
					// putObjectServer(map.get("file_path"),
					// map.get("file_url"), map.get("json_name"));

					/**
					 * update 2017.04.25
					 * 
					 * oss剔除服务端上传直接 解开注释代码
					 * 
					 * r然后注释掉 putObjectServer(map.get("file_path"),
					 * map.get("file_url"), map.get("json_name")); 只注释掉一行就可以了
					 */
					// Map<String, String> map1 =
					// beanUtil.EncapsulationData(map.get("json_name"));
					// if (map1 != null) {
					// messageList.add(map1);
					// if (messageList.size() == size) {
					// Map<String, String> encapsulationData =
					// beanUtil.EncapsulationData(messageList);
					// putObjectMssageServer(encapsulationData.get("D_Data"),
					// encapsulationData.get("D_Url"));
					// } else {
					// getOssCompletedCallback().onSuccess(paramT1, paramT2);
					// }
					// } else {
					// getOssCompletedCallback().onFailure(null, new
					// ClientException("执行上传失败，请重试！"), null);
					// }

				}

				@Override
				public void onFailure(PutObjectRequest paramT1, ClientException paramClientException, ServiceException paramServiceException) {
					getOssCompletedCallback().onFailure(paramT1, paramClientException, paramServiceException);
				}
			});
		} else {
			getOssCompletedCallback().onFailure(null, new ClientException("未初始化 OSSClient ，请检查！"), null);
		}
	}

	/**
	 * 上传OSS 这里采用异步上传 双份
	 */
	public void putObjectOSSDouble(String filename, String filepath, Object asfi) {
		this.TEMPORARY_DATA = asfi;
		objectRequest = new PutObjectRequest(this.bucketName, filename, filepath);
		if (progressCallback != null) {
			objectRequest.setProgressCallback(progressCallback);
		}
		if (oss != null) {
			task = oss.asyncPutObject(objectRequest, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {

				@Override
				public void onSuccess(PutObjectRequest paramT1, PutObjectResult paramT2) {
					Map<String, String> map = beanUtil.AssemblyData(TEMPORARY_DATA);
					putObjectServer(map.get("file_path"), map.get("file_url"), map.get("json_name"));

					/**
					 * update 2017.04.25
					 * 
					 * oss剔除服务端上传直接 解开注释代码
					 * 
					 * r然后注释掉 putObjectServer(map.get("file_path"),
					 * map.get("file_url"), map.get("json_name")); 只注释掉一行就可以了
					 */
					// Map<String, String> map1 =
					// beanUtil.EncapsulationData(map.get("json_name"));
					// if (map1 != null) {
					// putObjectMssageServer(map1.get("D_Data"),
					// map1.get("D_Url"));
					// } else {
					// getOssCompletedCallback().onFailure(null, new
					// ClientException("执行上传失败，请重试！"), null);
					// }

				}

				@Override
				public void onFailure(PutObjectRequest paramT1, ClientException paramClientException, ServiceException paramServiceException) {
					getOssCompletedCallback().onFailure(paramT1, paramClientException, paramServiceException);
				}
			});
		} else {
			getOssCompletedCallback().onFailure(null, new ClientException("未初始化 OSSClient ，请检查！"), null);
		}
	}

	/**
	 * 普通上传，上传至服务器
	 * 
	 * @throws IOException
	 */
	public void putObjectServer(String filepath, String url, String filename) {
		try {
			okhttp3.OkHttpClient.Builder buil = new okhttp3.OkHttpClient.Builder();
			/* 设置超时时间为三分钟 */
			buil.readTimeout(3, TimeUnit.MINUTES);
			buil.connectTimeout(3, TimeUnit.MINUTES);
			buil.writeTimeout(3, TimeUnit.MINUTES);
			client = buil.build();
			File file = new File(filepath);
			RequestBody filebody = RequestBody.create(MediaType.parse("application/octet-stream"), file);
			Log.e("info", "URL_OSS:" + url);
			RequestBody bodyd = new MultipartBody.Builder().setType(MediaType.parse("multipart/form-data; charset=utf-8")).addFormDataPart("file", filename, filebody).build();
			Request request = new Request.Builder().url(url).post(bodyd).build();
			Response response = client.newCall(request).execute();
			if (response.isSuccessful()) {
				Map<String, String> map = beanUtil.EncapsulationData(filename);
				if (map != null) {
					putObjectMssageServer(map.get("D_Data"), map.get("D_Url"));
				} else {
					getOssCompletedCallback().onFailure(null, new ClientException("执行上传失败，请重试！"), null);
				}
			}
		} catch (IOException e) {
			getOssCompletedCallback().onFailure(null, new ClientException("执行上传失败，请重试！"), null);
		}
	}

	/**
	 * 普通上传，传递数据回传服务
	 * 
	 * @throws IOException
	 */
	public void putObjectMssageServer(String json, String url) {
		try {
			okhttp3.OkHttpClient.Builder buil = new okhttp3.OkHttpClient.Builder();
			/* 设置超时时间为三分钟 */
			buil.readTimeout(3, TimeUnit.MINUTES);
			buil.connectTimeout(3, TimeUnit.MINUTES);
			buil.writeTimeout(3, TimeUnit.MINUTES);
			client = buil.build();
			RequestBody jsonbody = RequestBody.create(JSON, json);
			Request request = new Request.Builder().url(url).post(jsonbody).build();
			Response response = client.newCall(request).execute();
			String responseStr = response.body().string();
			if (response.isSuccessful()) {
				PutObjectResult objectResult = new PutObjectResult();
				Log.e("oss:", responseStr);
				objectResult.setServerCallbackReturnBody(responseStr);
				getOssCompletedCallback().onSuccess(null, objectResult);
			} else {
				getOssCompletedCallback().onFailure(null, new ClientException("执行回传服务失败:" + responseStr), null);
			}
		} catch (IOException e) {
			getOssCompletedCallback().onFailure(null, new ClientException("执行回传服务失败，请重试！"), null);
		}
	}

	private OkHttpClient client;

	/** 取消上传 */
	public void cancel() {
		if (task != null && !task.isCanceled())
			task.cancel();
		if (client != null)
			client.dispatcher().cancelAll();
	}
}
