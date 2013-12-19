package com.comic.lazyupload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.StrictMode;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.comic.lazyupload.camera.CameraActivity;
import com.comic.lazyupload.camera.PreviewActivity;
import com.comic.lazyupload.sinaauth.AccessTokenKeeper;
import com.comic.lazyupload.sinaauth.ConstantsSina;
import com.comic.lazyupload.sinaauth.UserInfo;
import com.comic.lazyupload.upload.UploadService;
import com.comic.lazyupload.upload.UploadService.UploadBinder;
import com.comic.lazyupload.utils.NetUtils;
import com.comic.lazyupload.utils.Utils;
import com.comic.lazyupload.view.RightFlipImage;
import com.weibo.sdk.android.Oauth2AccessToken;
import com.weibo.sdk.android.Weibo;
import com.weibo.sdk.android.WeiboAuthListener;
import com.weibo.sdk.android.WeiboDialogError;
import com.weibo.sdk.android.WeiboException;
import com.weibo.sdk.android.sso.SsoHandler;

public class MainActivity extends Activity {

	private static final int FLIP_STEP_SIZE = 2;
	private static final int FLIP_IMAGE_VIEW_SIZE = 8;
	private static final int FLIP_IMAGE_TIME_SETP = 150;

	private static final int MSG_FLIP = 31;
	private static final int MSG_GET_IMAGE_SUCESS = 32;
	private static final int MSG_FLIP_FINISHED = 33;

	private static final int RESULT_CAMERA = 66;
	private static final int RESULT_PREVIEW = 67;

	private static final String EMPTY_IMAGE_TAG = "http://empty_image";

	private Context mCtx;

	private ActionBar mActionBar;

	private Button mTopBtn, mNextBtn;
	private ImageButton mCameraBtn;

	private String mNextString;
	private int mNextBtnWidth;
	private int mNextBtnTextSize;
	private Paint mNextBtnPaint = new Paint();

	private UploadBinder mBinder = null;
	private UploadService mUploadService = null;
	private String mImageReadyPath = "";

	private int[] flipImageId = { R.id.grid_image0, R.id.grid_image1,
			R.id.grid_image2, R.id.grid_image3, R.id.grid_image5,
			R.id.grid_image6, R.id.grid_image7, R.id.grid_image8 };
	private ArrayList<RightFlipImage> mFilpImageViewList = new ArrayList<RightFlipImage>(
			8);
	private int[] mFlipSer = { 0, 1, 2, 3, 4, 5, 6, 7 };

	private ArrayList<String> mImageList = new ArrayList<String>();

	private int mFilpId = 0;
	private int mFlipStartImageId = -FLIP_IMAGE_VIEW_SIZE;
	private int mPageCount = 1;

	private boolean mShowBack = false;
	private boolean mFlipping = false;

	private Weibo mWeibo;
	public static Oauth2AccessToken accessToken;
	private SsoHandler mSsoHandler;
	private ProfileDialog mProfileDialog = null;

	private String mLatestId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
				.detectDiskReads().detectDiskWrites().detectNetwork()
				.penaltyLog().build());

		mCtx = this;

		setContentView(R.layout.activity_main);
		getView();

		mWeibo = Weibo.getInstance(ConstantsSina.APP_KEY,
				ConstantsSina.REDIRECT_URL, ConstantsSina.SCOPE);
		MainActivity.accessToken = AccessTokenKeeper.readAccessToken(this);

		Intent intent = new Intent();
		intent.setClass(MainActivity.this, UploadService.class);
		bindService(intent, mServiceConnection, BIND_AUTO_CREATE);

		new GetImageListTask().execute();
	}

	private void getView() {
		for (int id : flipImageId) {
			RightFlipImage flipImage = (RightFlipImage) findViewById(id);
			mFilpImageViewList.add(flipImage);
			flipImage.setFrountImageSrc(R.drawable.gray_image_downloading);
			flipImage.setBackImageSrc(R.drawable.gray_image_downloading);
		}
		mTopBtn = (Button) findViewById(R.id.top_btn);
		mNextBtn = (Button) findViewById(R.id.next_btn);
		mCameraBtn = (ImageButton) findViewById(R.id.grid_camera);

		mNextString = this.getResources().getString(R.string.next);
		showNextCountString(mPageCount);

		mNextBtnTextSize = this.getResources().getDimensionPixelSize(
				R.dimen.big_btn_text_size);
		mNextBtnWidth = this.getResources().getDisplayMetrics().widthPixels
				- this.getResources().getDimensionPixelSize(
						R.dimen.big_btn_layout_margin);
		mNextBtnPaint.setTextSize(mNextBtnTextSize);

		mTopBtn.setOnClickListener(mTopBtnClicked);
		mNextBtn.setOnClickListener(mNextBtnClicked);
		mCameraBtn.setOnClickListener(mCameraBtnClicked);

	}

	private void showNextCountString(int pageCount) {
		StringBuilder nextStringBulider = new StringBuilder();
		nextStringBulider.append(mNextString);
		nextStringBulider.append('(');
		nextStringBulider.append(pageCount);
		nextStringBulider.append(')');
		String nextString = nextStringBulider.toString();

		float measuredWidth = mNextBtnPaint.measureText(nextString);
		if (measuredWidth > mNextBtnWidth) {
			float newTextSize = (float) (mNextBtnTextSize * 0.9);
			if (mNextBtn.getTextSize() != newTextSize) {
				mNextBtn.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);
			}
		} else {
			if (mNextBtn.getTextSize() != mNextBtnTextSize) {
				mNextBtn.setTextSize(TypedValue.COMPLEX_UNIT_PX,
						mNextBtnTextSize);
			}
		}
		Loge.i("next Btn width: " + mNextBtnWidth);
		Loge.i("next String width: " + measuredWidth);

		mNextBtn.setText(nextString);
	}

	private View.OnClickListener mTopBtnClicked = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			backToTop();
		}
	};

	private View.OnClickListener mNextBtnClicked = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			flipGrid();
		}
	};

	private View.OnClickListener mCameraBtnClicked = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			takePhoto();
		}
	};

	private void initActionBar() {
		mActionBar = getActionBar();
		if (mActionBar != null) {
			mActionBar.setTitle("Account Name");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_activity_menus, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.sina_login: {
			accessToken = AccessTokenKeeper
					.readAccessToken(getApplicationContext());
			String sToken = accessToken.getToken();
			String sCode = AccessTokenKeeper
					.readAccessCode(getApplicationContext());
			if ((sToken == null || sToken.isEmpty())
					&& (sCode == null || sCode.isEmpty())) {
				mSsoHandler = new SsoHandler(this, mWeibo);
				mSsoHandler.authorize(new AuthDialogListener(),
						getPackageName());
			} else {
				if (mProfileDialog == null) {
					mProfileDialog = new ProfileDialog(mCtx);
				}
				mProfileDialog.show();
			}
		}
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(mServiceConnection);
	}

	private void takePhoto() {
		Loge.i("takePhoto start CameraActivity");
		Intent cameraIntent = new Intent();
		cameraIntent.setClass(MainActivity.this, CameraActivity.class);
		startActivityForResult(cameraIntent, RESULT_CAMERA);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Loge.i("onActivityResult requestCode: " + requestCode + " resultCode: "
				+ resultCode);
		switch (requestCode) {
		case RESULT_CAMERA: {
			if (data != null) {
				String path = data.getStringExtra("path");
				int orientation = data.getIntExtra("orientation", 0);

				Intent startPreview = new Intent();
				startPreview.setClass(MainActivity.this, PreviewActivity.class);
				startPreview.putExtra("path", path);
				startPreview.putExtra("orientation", orientation);

				startActivityForResult(startPreview, RESULT_PREVIEW);
			}
		}
			return;
		case RESULT_PREVIEW: {
			if (resultCode == RESULT_OK) {
				if (data != null) {
					String path = data.getStringExtra("path");
					if (path != null && !path.equals(mImageReadyPath)) {
						mImageReadyPath = path;
						Loge.i("photo shot full path: " + mImageReadyPath);
						if (mUploadService != null && mBinder != null) {
							try {
								Parcel pdata = Parcel.obtain();
								pdata.writeString(path);
								mBinder.transact(
										UploadService.CODE_UPLOADE_PHOTO,
										pdata, null, IBinder.FLAG_ONEWAY);
								pdata.recycle();
							} catch (RemoteException e) {
								e.printStackTrace();
							}
						}
					}
				}
			} else {
				takePhoto();
			}
		}
			return;
		default:
			break;
		}
		if (mSsoHandler != null) {
			mSsoHandler.authorizeCallBack(requestCode, resultCode, data);
		}
	}

	private void backToTop() {
		if (mFlipStartImageId == 0) {
			return;
		}
		mFlipStartImageId = 0;
		int imageSize = mImageList.size();
		for (int i = 0; i < FLIP_IMAGE_VIEW_SIZE; i++) {
			RightFlipImage fImage = mFilpImageViewList.get(i);
			String imageUrlFrount = mImageList.get(i);
			if (mShowBack) {
				fImage.setFrountImageUrl(imageUrlFrount);
				fImage.invalidateFrount();
			} else {
				fImage.setBackImageUrl(imageUrlFrount);
				fImage.invalidateBack();
			}
			if (i + FLIP_IMAGE_VIEW_SIZE < imageSize) {
				String imageUrlBack = mImageList.get(i + FLIP_IMAGE_VIEW_SIZE);
				if (mShowBack) {
					fImage.setBackImageUrl(imageUrlBack);
					fImage.invalidateBack();
				} else {
					fImage.setFrountImageUrl(imageUrlBack);
					fImage.invalidateFrount();
				}
			}
		}
		mPageCount = 1;
		showNextCountString(mPageCount);
		mShowBack = !mShowBack;
		for (int i = 0; i < FLIP_IMAGE_VIEW_SIZE; i++) {
			mFilpImageViewList.get(mFlipSer[i]).filpImage();
		}
	}

	private void flipGrid() {
		if (mFlipping) {
			return;
		}
		mFlipping = true;
		if (mFlipStartImageId == (mImageList.size() - FLIP_IMAGE_VIEW_SIZE * 2)) {
			mFlipStartImageId = -FLIP_IMAGE_VIEW_SIZE * 2;
		}
		genRandomSer();
		flipToNext();
	}

	private void flipToNext() {
		if (mFilpId == FLIP_IMAGE_VIEW_SIZE) {
			mFilpId = 0;
			mHandler.sendEmptyMessage(MSG_FLIP_FINISHED);
			return;
		}
		int size = mFilpId + FLIP_STEP_SIZE;
		for (int i = mFilpId; i < size; i++) {
			mFilpImageViewList.get(mFlipSer[i]).filpImage();
		}
		mFilpId = mFilpId + FLIP_STEP_SIZE;
		mHandler.sendEmptyMessageDelayed(MSG_FLIP, FLIP_IMAGE_TIME_SETP);
	}

	private void genRandomSer() {
		int index, t;
		for (int i = 0; i < mFlipSer.length; i++) {
			index = new Random().nextInt(3);
			t = mFlipSer[i];
			mFlipSer[i] = mFlipSer[index];
			mFlipSer[index] = t;
		}
	}

	private Handler mHandler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_FLIP: {
				flipToNext();
			}
				break;
			case MSG_GET_IMAGE_SUCESS: {
				backToTop();
			}
				break;
			case MSG_FLIP_FINISHED: {
				mShowBack = !mShowBack;
				mFlipStartImageId = mFlipStartImageId + FLIP_IMAGE_VIEW_SIZE;
				if (mFlipStartImageId < 0) {
					mPageCount = mImageList.size() / FLIP_IMAGE_VIEW_SIZE;
				} else if (mFlipStartImageId == 0) {
					mPageCount = 1;
				} else {
					mPageCount = mFlipStartImageId / FLIP_IMAGE_VIEW_SIZE + 1;
				}
				Loge.i("mFlipStartImageId: " + mFlipStartImageId
						+ " mPageCount: " + mPageCount);
				showNextCountString(mPageCount);
				for (int i = 0; i < FLIP_IMAGE_VIEW_SIZE; i++) {
					RightFlipImage fImage = mFilpImageViewList.get(i);
					int imagePos = i + mFlipStartImageId + FLIP_IMAGE_VIEW_SIZE;
					String imageUrl = mImageList.get(imagePos);
					if (mShowBack) {
						fImage.setFrountImageUrl(imageUrl);
					} else {
						fImage.setBackImageUrl(imageUrl);
					}
				}
				mFlipping = false;
			}
				break;
			default:
				break;
			}
			return false;
		}
	});

	class GetImageListTask extends AsyncTask<Void, Void, String> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected String doInBackground(Void... params) {

			if (!Utils.isNetworkAvailable(mCtx)) {
				return null;
			}

			String acctoken = NetUtils.getAccessToken(mCtx);

			if (acctoken == null || acctoken.isEmpty()) {
				return null;
			}

			HashMap<String, Object> queryParams = new HashMap<String, Object>();
			queryParams.put("count", 30);
			queryParams.put("feature", 2);
			queryParams.put("trim_user", 1);
			if (mLatestId != null && !mLatestId.isEmpty()) {
				queryParams.put("since_id", mLatestId);
			}
			Object userPostInfo = NetUtils.getResult(
					"https://api.weibo.com/2/statuses/user_timeline.json",
					queryParams, acctoken);

			if (userPostInfo == null) {
				return null;
			}
			ArrayList<UserHistoryData> tempListData = new ArrayList<UserHistoryData>();
			tempListData.addAll(NetUtils.getUserHistoryData(userPostInfo));

			for (UserHistoryData data : tempListData) {
				if (!data.mText.contains("SeeXian")) {
					continue;
				}
				mImageList.add(data.mOriPic);
			}

			int gap = mImageList.size() % FLIP_IMAGE_VIEW_SIZE;
			Loge.i("Fill Image Gap: " + gap);
			if (gap != 0) {
				for (int i = 0; i < FLIP_IMAGE_VIEW_SIZE - gap; i++) {
					mImageList.add(EMPTY_IMAGE_TAG);
				}
			}
			Loge.i("Fill Image List Size: " + mImageList.size());

			return "sucess";
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			if (result != null) {
				mHandler.sendEmptyMessage(MSG_GET_IMAGE_SUCESS);
			}
		}

	}

	class AuthDialogListener implements WeiboAuthListener {

		@Override
		public void onCancel() {
			Toast.makeText(getApplicationContext(), "Auth cancel",
					Toast.LENGTH_LONG).show();
		}

		@Override
		public void onComplete(Bundle values) {
			String code = values.getString("code");
			String token = values.getString("access_token");
			String expires_in = values.getString("expires_in");

			Loge.d("code = " + code);
			Loge.d("token = " + token);
			Loge.d("expires_in = " + expires_in);

			if (token != null && expires_in != null) {
				MainActivity.accessToken = new Oauth2AccessToken(token,
						expires_in);
				AccessTokenKeeper.clear(getApplicationContext());
				AccessTokenKeeper.keepAccessToken(getApplicationContext(),
						MainActivity.accessToken);
			}

			if (token == null && expires_in == null && code != null) {
				AccessTokenKeeper.keepAccessCode(getApplicationContext(), code);
			}

			getUserInfo();

			Toast.makeText(getApplicationContext(),
					mCtx.getResources().getString(R.string.login_success),
					Toast.LENGTH_LONG).show();

		}

		@Override
		public void onError(WeiboDialogError arg0) {
			Toast.makeText(getApplicationContext(),
					"Auth error : " + arg0.getMessage(), Toast.LENGTH_LONG)
					.show();
		}

		@Override
		public void onWeiboException(WeiboException arg0) {
			Toast.makeText(getApplicationContext(),
					"Auth exception : " + arg0.getMessage(), Toast.LENGTH_LONG)
					.show();
		}

	}

	public void getUserInfo() {

		new Thread(new Runnable() {
			@Override
			public void run() {

				String acctoken = NetUtils.getAccessToken(mCtx);

				if (acctoken == null || acctoken.isEmpty()) {
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
				UserInfo userInfo = new UserInfo();
				try {
					JSONObject jUserInfo = new JSONObject(oUserInfo.toString());
					userInfo.mScreenName = jUserInfo.getString("screen_name");
					userInfo.mAvatar = jUserInfo.getString("avatar_large");
				} catch (JSONException e) {
					e.printStackTrace();
				}

				Loge.d("mScreenName = " + userInfo.mScreenName);
				Loge.d("mAvatar = " + userInfo.mAvatar);

				if ((userInfo.mScreenName != null && !userInfo.mScreenName
						.isEmpty())
						&& (userInfo.mAvatar != null && !userInfo.mAvatar
								.isEmpty())) {
					AccessTokenKeeper.keepUserInfo(mCtx, userInfo);
				}

			}
		}).start();
	}

	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Loge.d("onServiceConnected");
			if (name.getShortClassName().endsWith("UploadService")) {
				Loge.d("class match UploadService");
				mBinder = (UploadBinder) service;
				mUploadService = mBinder.getService();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Loge.d("onServiceDisconnected");
		}
	};
}
