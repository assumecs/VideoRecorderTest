package com.n22.uploading.idle;

/**
 * 初始化接口
 * @author WWD
 * time 2017.03.22
 */
public interface BeanUtil {

	/**工具类接口，传递json工具object TO json*/
	<T> T JsonUtilOTJ(T t);

	/**工具类接口，传递json工具Json TO object*/
	<T> T JsonUtilJTO(String json, Class<T> classOfT);

	/**解析DES工具*/
	<T> T DecodeUtil(String des);

	/**URL传递*/
	String BaseUrl();

	/**解析返回包工具*/
	<T> T AnalyticMessage(String body);

	/**获取Token时组装的数据*/
	String ObtainToken();

	/**数据转换*/
	<T> T AssemblyData(Object obj);

	/**二次封装数据*/
	<T> T EncapsulationData(String json);
	
	/**二次封装数据*/
	<T> T EncapsulationData(Object obj);
}
