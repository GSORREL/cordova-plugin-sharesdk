package behring.cordovasharesdk;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
//###ADDED jinjr 2018.8.30
import android.content.Intent;
//###END

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.sql.*;

import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;
import cn.sharesdk.framework.PlatformDb;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.sina.weibo.SinaWeibo;
import cn.sharesdk.tencent.qq.QQ;
import cn.sharesdk.wechat.friends.Wechat;
import cn.sharesdk.wechat.moments.WechatMoments;

/**
 * This class ShareSDKPlugin a string called from JavaScript.
 */
public class ShareSDKPlugin extends CordovaPlugin {
	private static final int SINA_WEIBO_CLIENT = 1;
	private static final int WECHAT_CLIENT = 2;
	private static final int QQ_CLIENT = 3;

	/** 平台和分享类型的值参考ShareSDK ios源码中的值 */
	/**
	 * 新浪微博
	 */
	private final int SSDKPlatformTypeWeibo = 1;
	/**
	 * QQ空间
	 */
	private final int SSDKPlatformTypeQZone = 6;
	/**
	 * QQ 好友
	 */
	private final int SSDKPlatformTypeQQFriend = 24;
	/**
	 * 拷贝
	 */
	private final int SSDKPlatformTypeCopy = 21;
	/**
	 * 微信好友
	 */
	private final int SSDKPlatformTypeWechatSession = 22;
	/**
	 * 微信朋友圈
	 */
	private final int SSDKPlatformTypeWechatTimeline = 23;

	private static final int SHARE_TEXT = 1;
	private static final int SHARE_IMAGE = 2;
	private static final int SHARE_WEBPAGE = 3;

	/**
	 * 参考sharesdk中ios枚举型:SSDKResponseState
	 */
	private static final int RESPONSE_STATE_BEGIN = 0;
	private static final int RESPONSE_STATE_SUCCESS = 1;
	private static final int RESPONSE_STATE_FAIL = 2;
	private static final int RESPONSE_STATE_CANCEL = 3;

	// ###ADDED by jinjr 2018.8.30
	// 解决未定义MSG_AUTH_CANCEL等常量,
	// 参考https://blog.csdn.net/z_zT_T/article/details/52185738?locationNum=2
	// https://blog.csdn.net/qq_34310081/article/details/53523112
	private static final int MSG_AUTH_CANCEL = 2;
	private static final int MSG_AUTH_ERROR = 3;
	private static final int MSG_AUTH_COMPLETE = 5;
	private static final int MSG_AUTH_COMPLETE_WEXIN = 6;
	private static final int MSG_AUTH_COMPLETE_WEIBO = 7;
	// ###END


	private CallbackContext callbackContext;
	private PlatformActionListener platformActionStateListener = new PlatformActionListener() {
        //###ADDED by jjr 
		private void handleActionUserInfo(Platform platform, int action, HashMap<String, Object> result) {
			 String token,userId,name,gender,headImageUrl,unionid,openId,sex;
			JSONObject userInfo = new JSONObject();
			// debugLog("JAVAdebug", "onComplete:handleActionUserInfo");
			// 输出所有授权信息
			PlatformDb platDB = platform.getDb();// 获取数平台数据DB
			token = platDB.getToken();
			userId = platDB.getUserId();
			name = platDB.getUserName();
			gender = platDB.getUserGender();
			headImageUrl = platDB.getUserIcon();
			unionid = platDB.get("unionid");
			openId = platDB.get("openid");
			gender = "m".equals(gender) ? "1" : "0";
			// debugLog("JAVAdebug", "getUserinfo");
			try {
				userInfo.putOpt("token", token);
				userInfo.putOpt("userId", userId);
				userInfo.putOpt("name", name);
				userInfo.putOpt("gender", gender);
				userInfo.putOpt("headImageUrl", headImageUrl);
				userInfo.putOpt("unionid", unionid);
				userInfo.putOpt("openId", openId);
				userInfo.putOpt("sex", gender);

				callbackContext.success(userInfo);
				// debugLog("JAVAdebug", "callbackContext");
				// debugLog("JAVAdebug", token);

			} catch (JSONException e) {
				// e.printStackTrace();
				JSONObject jsonObject = new JSONObject();
				try {
					jsonObject.putOpt("state", RESPONSE_STATE_FAIL);
					jsonObject.putOpt("error", "JSONException: UserInfo error");
				} catch (JSONException e2) {
					e2.printStackTrace();
				} finally {
					callbackContext.error(jsonObject);
				}
			}
		}
        //###END
		
		@Override
		public void onComplete(Platform platform, int action, HashMap<String, Object> result) {
			if (callbackContext == null) {
				callbackContext.error("Null callbackContext!");
			}
			//###ADDED jinjr 2018.8.30
			// 参考https://blog.csdn.net/fcwxin/article/details/76020167
			else if (action == Platform.ACTION_USER_INFOR) {
				this.handleActionUserInfo(platform, action, result);
			}
			//###END
		}

		@Override
		public void onError(Platform platform, int action, Throwable throwable) {
			if (callbackContext != null) {
				JSONObject jsonObject = new JSONObject();
				try {
					jsonObject.putOpt("state", RESPONSE_STATE_FAIL);
					jsonObject.putOpt("error", throwable.toString());
				} catch (JSONException e) {
					e.printStackTrace();
				} finally {
					callbackContext.error(jsonObject);
				}
			}
			//###ADDED jinjr 2018.8.30
			// 参考https://blog.csdn.net/fcwxin/article/details/76020167
			else if (action == Platform.ACTION_USER_INFOR) {
//                    ToastUtil.showMessage("授权失败");
			}
			// ###END
		}

		@Override
		public void onCancel(Platform platform, int action) {
			if (callbackContext != null) {
				JSONObject jsonObject = new JSONObject();
				try {
					jsonObject.putOpt("state", RESPONSE_STATE_CANCEL);
				} catch (JSONException e) {
					e.printStackTrace();
				} finally {
					callbackContext.error(jsonObject);
				}
			}
			// ###ADDED jinjr 2018.8.30
			// 参考https://blog.csdn.net/fcwxin/article/details/76020167
			else if (action == Platform.ACTION_USER_INFOR) {
//                    ToastUtil.showMessage("取消授权");
			}
			// ###END
		}
	};


	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);
		ShareSDK.initSDK(cordova.getActivity());
		cordova.setActivityResultCallback(this);
	}

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		this.callbackContext = callbackContext;
		if (action.equals("share")) {
			int platformType = args.optInt(0);
			int shareType = args.optInt(1);
			JSONObject shareInfo = args.optJSONObject(2);
			this.share(platformType, shareType, shareInfo, callbackContext);
			return true;
		} else if (action.equals("isInstallClient")) {
			int clientType = args.optInt(0);
			isInstallClient(clientType, callbackContext);
		}
//###ADDED jinjr 2018.8.30
		else if (action.equals("Login")) {
			int ClientType = args.optInt(0);
			Login(ClientType, callbackContext);
			return true;
		}
//###END
		return false;
	}

	private void share(int platformType, int shareType, JSONObject shareInfo, CallbackContext callbackContext) {

		switch (shareType) {
		case SHARE_TEXT:
			if (platformType == SSDKPlatformTypeCopy) {
				this.copyLink(shareInfo, callbackContext);
			} else {
				this.shareText(platformType, shareInfo, callbackContext);
			}

			break;
		case SHARE_IMAGE:
			this.shareImage(platformType, shareInfo, callbackContext);
			break;
		case SHARE_WEBPAGE:
			this.shareWebPage(platformType, shareInfo, callbackContext);
			break;
		default:
			break;
		}
	}

	private void isInstallClient(int clientType, CallbackContext callbackContext) {
		boolean isInstallClient;
		Platform platform = null;
		switch (clientType) {
		case SINA_WEIBO_CLIENT:
			platform = ShareSDK.getPlatform(SinaWeibo.NAME);
			break;
		case WECHAT_CLIENT:
			platform = ShareSDK.getPlatform(Wechat.NAME);
			break;
		case QQ_CLIENT:
			platform = ShareSDK.getPlatform(QQ.NAME);
			break;
		default:
			break;
		}

		if (platform != null) {
			isInstallClient = platform.isClientValid();
			PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, isInstallClient);
			callbackContext.sendPluginResult(pluginResult);
//            debugLog("InstallClient","true");
		} else {
			PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR);
			callbackContext.sendPluginResult(pluginResult);
		}

	}

	private void copyLink(JSONObject shareInfo, final CallbackContext callbackContext) {
		ClipboardManager myClipboard = (ClipboardManager) cordova.getActivity()
				.getSystemService(Context.CLIPBOARD_SERVICE);
		String text = shareInfo.optString("text");
		ClipData clipData = ClipData.newPlainText("text", text);
		myClipboard.setPrimaryClip(clipData);
		callbackContext.success();
	}

	private void shareText(int platformType, JSONObject shareInfo, final CallbackContext callbackContext) {
		Platform.ShareParams sp = null;
		Platform platform = null;
		switch (platformType) {
		case SSDKPlatformTypeWechatSession:
			sp = new Wechat.ShareParams();
			sp.setShareType(Platform.SHARE_TEXT);
			platform = ShareSDK.getPlatform(Wechat.NAME);
			break;
		case SSDKPlatformTypeWechatTimeline:
			sp = new WechatMoments.ShareParams();
			sp.setShareType(Platform.SHARE_TEXT);
			platform = ShareSDK.getPlatform(WechatMoments.NAME);
			break;
		case SSDKPlatformTypeWeibo:
			sp = new SinaWeibo.ShareParams();
			platform = ShareSDK.getPlatform(SinaWeibo.NAME);
			break;
		case SSDKPlatformTypeQQFriend:
			sp = new QQ.ShareParams();
			platform = ShareSDK.getPlatform(QQ.NAME);
			break;
		default:
			break;
		}

		sp.setText(shareInfo.optString("text"));
		platform.setPlatformActionListener(platformActionStateListener);
		platform.share(sp);
	}

	private void shareImage(int platformType, JSONObject shareInfo, final CallbackContext callbackContext) {
		Platform.ShareParams sp = null;
		Platform platform = null;
		switch (platformType) {
		case SSDKPlatformTypeWechatSession:
			sp = new Wechat.ShareParams();
			sp.setShareType(Platform.SHARE_IMAGE);
			platform = ShareSDK.getPlatform(Wechat.NAME);
			break;
		case SSDKPlatformTypeWechatTimeline:
			sp = new WechatMoments.ShareParams();
			sp.setShareType(Platform.SHARE_IMAGE);
			platform = ShareSDK.getPlatform(WechatMoments.NAME);
			break;
		case SSDKPlatformTypeWeibo:
			sp = new SinaWeibo.ShareParams();
			platform = ShareSDK.getPlatform(SinaWeibo.NAME);
			break;
		case SSDKPlatformTypeQQFriend:
			sp = new QQ.ShareParams();
			platform = ShareSDK.getPlatform(QQ.NAME);
			break;
		default:
			break;
		}

		sp.setImageUrl(shareInfo.optString("image"));
		platform.setPlatformActionListener(platformActionStateListener);
		platform.share(sp);
	}

	private void shareWebPage(int platformType, JSONObject shareInfo, final CallbackContext callbackContext) {
		Platform.ShareParams sp = null;
		Platform platform = null;
		switch (platformType) {
		case SSDKPlatformTypeWechatSession:
			sp = new Wechat.ShareParams();
			sp.setShareType(Platform.SHARE_WEBPAGE);
			platform = ShareSDK.getPlatform(Wechat.NAME);
			break;
		case SSDKPlatformTypeWechatTimeline:
			sp = new WechatMoments.ShareParams();
			sp.setShareType(Platform.SHARE_WEBPAGE);
			platform = ShareSDK.getPlatform(WechatMoments.NAME);
			break;
		case SSDKPlatformTypeWeibo:
			sp = new SinaWeibo.ShareParams();
			platform = ShareSDK.getPlatform(SinaWeibo.NAME);
			break;
		case SSDKPlatformTypeQQFriend:
			sp = new QQ.ShareParams();
			platform = ShareSDK.getPlatform(QQ.NAME);
			break;
		default:
			break;
		}

		sp.setImageUrl(shareInfo.optString("icon"));
		sp.setTitle(shareInfo.optString("title"));
		sp.setUrl(shareInfo.optString("url"));
		sp.setText(shareInfo.optString("text"));
		platform.setPlatformActionListener(platformActionStateListener);
		platform.share(sp);
	}

	// ###ADDED jinjr 2018.8.30
	// 参考https://blog.csdn.net/fcwxin/article/details/76020167
	public void Login(int ClientType, CallbackContext callbackContext) {
		// debugLog("JAVAdebug", "login");
		Platform plat = null;
		switch (ClientType) {
		case WECHAT_CLIENT:
			plat = ShareSDK.getPlatform(Wechat.NAME);
			if (!plat.isClientValid()) {
//                    ToastUtil.showMessage("您尚未安装微信客户端");
				return;
			}
			break;
		case QQ_CLIENT:
			plat = ShareSDK.getPlatform(QQ.NAME);
			if (!plat.isClientValid()) {
//                    ToastUtil.showMessage("您尚未安装QQ客户端");
				return;
			}
			break;
		case SINA_WEIBO_CLIENT:
			plat = ShareSDK.getPlatform(SinaWeibo.NAME);
			if (!plat.isClientValid()) {
//                    ToastUtil.showMessage("您尚未安装微博客户端");
				return;
			}
			
			break;
		default:
			break;
		}

		plat.removeAccount(true); // 移除授权状态和本地缓存，下次授权会重新授权
		plat.SSOSetting(false); // SSO授权，传false默认是客户端授权，没有客户端授权或者不支持客户端授权会跳web授权
		plat.setPlatformActionListener(platformActionStateListener);// 授权回调监听，监听oncomplete，onerror，oncancel三种状态
		plat.showUser(null);
	}

//###END

//###ADDED by jinjr 2018.9.18, 发送 debug 日志到服务器
//参考:https://blog.csdn.net/Procedure_bear/article/details/80976816
	public static String debugLog(String action, Object content) {
		String url = "https://ax.morshoo.com/api/tools/log/savelog";
		String result = "";
		try {
			URL realUrl = new URL(url);
			// 打开和URL之间的连接
			HttpURLConnection connection = (HttpURLConnection) realUrl.openConnection();
			// 发送POST请求必须设置如下两行
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setRequestMethod("POST");
			connection.setUseCaches(false);
			connection.setInstanceFollowRedirects(true);
			connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
			connection.connect();
			// 获取URLConnection对象对应的输出流
			DataOutputStream out = new DataOutputStream(connection.getOutputStream());
			// 发送请求参数
			// String data = "action=shareSDK&content="+content;
			// out.write(data.getBytes("UTF-8"));
			JSONObject object = new JSONObject();
			object.put("action", action);
			object.put("value", content);
			out.write(object.toString().getBytes("UTF-8"));
			// flush输出流的缓冲
			out.flush();
			out.close();
			// 定义BufferedReader输入流来读取URL的响应
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String lines;
			StringBuffer sb = new StringBuffer("");
			while ((lines = reader.readLine()) != null) {
				lines = new String(lines.getBytes(), "utf-8");
				sb.append(lines);
			}
			// System.out.println(sb);
			reader.close();
			// 断开连接
			connection.disconnect();
		} catch (Exception e) {
			// System.out.println("发送 POST 请求出现异常！" + e);
			String errorStr = e.getMessage();
		}
		return result;
	}
//###END
}
