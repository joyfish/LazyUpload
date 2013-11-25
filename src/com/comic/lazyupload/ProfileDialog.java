package com.comic.lazyupload;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.comic.lazyupload.image.PhotoView;
import com.comic.lazyupload.sinaauth.AccessTokenKeeper;
import com.comic.lazyupload.sinaauth.UserInfo;
import com.comic.lazyupload.utils.NetUtils;

public class ProfileDialog extends Dialog {

	public static final int MESSAGE_GAT_PROFILE_SUCCESS = 833;
	public static final int MESSAGE_SHOW_TOKEN_WARN = 853;
	public static final int MESSAGE_LOGOUT_SUCESS = 863;

	private Context mCtx;

	private PhotoView imageView;

	private TextView nameTextView;

	private Button logoutButton, backButton;

	UserInfo mUserInfo = null;

	private Handler mDataHandler = new Handler(new Handler.Callback() {

		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_GAT_PROFILE_SUCCESS: {
				if (imageView != null & nameTextView != null) {
					nameTextView.setText(mUserInfo.mScreenName);
					try {
						URL localURL = new URL(mUserInfo.mAvatar);
						imageView.setImageURL(localURL, true, null);
						imageView.invalidate();
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
				}
			}
				break;
			case MESSAGE_SHOW_TOKEN_WARN: {
				Toast.makeText(mCtx, R.string.plz_login_sina, Toast.LENGTH_LONG)
						.show();
			}
				break;
			case MESSAGE_LOGOUT_SUCESS: {
				dismiss();
			}
				break;
			default:
				break;
			}
			return false;
		}
	});

	public ProfileDialog(Context context, int theme) {
		super(context, theme);
		mCtx = context;
	}

	public ProfileDialog(Context context) {
		super(context);
		mCtx = context;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle(R.string.profile);
		setContentView(R.layout.profile_dialog);

		imageView = (PhotoView) findViewById(R.id.profile_image);
		nameTextView = (TextView) findViewById(R.id.profile_name);
		logoutButton = (Button) findViewById(R.id.dialog_btn_logout);
		backButton = (Button) findViewById(R.id.dialog_btn_back);

		logoutButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				logOut();
			}
		});

		backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		mUserInfo = AccessTokenKeeper.readUserInfo(mCtx);
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (mUserInfo.mAvatar.isEmpty() || mUserInfo.mScreenName.isEmpty()) {
			getUserInfo();
		} else {
			nameTextView.setText(mUserInfo.mScreenName);
			try {
				URL localURL = new URL(mUserInfo.mAvatar);
				imageView.setImageURL(localURL, true, null);
				imageView.invalidate();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
	}

	public void getUserInfo() {

		new Thread(new Runnable() {
			@Override
			public void run() {

				String acctoken = NetUtils.getAccessToken(mCtx);

				if (acctoken == null || acctoken.isEmpty()) {
					if (mDataHandler != null) {
						mDataHandler.sendEmptyMessage(MESSAGE_SHOW_TOKEN_WARN);
					}
					return;
				}

				String sUeserId = null;

				Object oUserId = NetUtils.getResult(
						"https://api.weibo.com/2/account/get_uid.json", null,
						acctoken);
				if (oUserId == null) {
					return;
				}

				try {
					JSONObject jUserId = new JSONObject(oUserId.toString());
					sUeserId = jUserId.getString("uid");
				} catch (JSONException e) {
					e.printStackTrace();
				}

				Loge.d("sUeserId = " + sUeserId);

				HashMap<String, Object> params = new HashMap<String, Object>();
				params.put("uid", sUeserId);
				Object oUserInfo = NetUtils.getResult(
						"https://api.weibo.com/2/users/show.json", params,
						acctoken);

				if (oUserInfo == null) {
					return;
				}
				mUserInfo = new UserInfo();
				try {
					JSONObject jUserInfo = new JSONObject(oUserInfo.toString());
					mUserInfo.mScreenName = jUserInfo.getString("screen_name");
					mUserInfo.mAvatar = jUserInfo.getString("avatar_large");
				} catch (JSONException e) {
					e.printStackTrace();
				}

				Loge.d("mScreenName = " + mUserInfo.mScreenName);
				Loge.d("mAvatar = " + mUserInfo.mAvatar);

				if ((mUserInfo.mScreenName != null && !mUserInfo.mScreenName
						.isEmpty())
						&& (mUserInfo.mAvatar != null && !mUserInfo.mAvatar
								.isEmpty())) {
					AccessTokenKeeper.keepUserInfo(mCtx, mUserInfo);
				}

				if (mDataHandler != null) {
					mDataHandler.sendEmptyMessage(MESSAGE_GAT_PROFILE_SUCCESS);
				}
			}
		}).start();
	}

	public void logOut() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				String acctoken = NetUtils.getAccessToken(mCtx);

				if (acctoken == null || acctoken.isEmpty()) {
					AccessTokenKeeper.clear(mCtx);
				} else {
					NetUtils.getResult(
							"https://api.weibo.com/2/account/end_session.json",
							null, acctoken);
					AccessTokenKeeper.clear(mCtx);
				}

				ContentResolver contentResolver = mCtx.getContentResolver();

				if (mDataHandler != null) {
					mDataHandler.sendEmptyMessage(MESSAGE_LOGOUT_SUCESS);
				}
			}

		}).start();

	}
}
