package com.comic.lazyupload.camera;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.comic.lazyupload.Loge;
import com.comic.lazyupload.R;

public class CameraActivity extends Activity {

	public static final int MEDIA_TYPE_IMAGE = 1;

	public static final int NUM_CAMERA = Camera.getNumberOfCameras();

	public static final int MSG_SWITCH_CAMERA_SUCCESS = 0;
	public static final int MSG_SAVE_PIC_SUCCESS = 1;
	public static final int MSG_NOT_EXT_STORAGE = 2;

	private int mSelectedCamera = 0;// 0 back,1 front
	private int mSelectedFlashMode = 0;// 0 off, 1 auto, 2 on

	private Camera mCamera;
	private CameraPreview mPreview;
	private FocusView mFocusView;

	private FrameLayout mPreviewPanel;
	private LinearLayout mSavePicPanel;

	private ImageButton mBack, mCapture, mSwitchCamera, mFlashLight, mRetake,
			mSave;

	private int mScreenHeight;
	private int mSreenWidth;
	private int mActionBarHeight;

	private byte[] mPicData = null;

	private String mFileFullPath;

	boolean enableFrame = true;

	private MyOrientationEventListener mOrientationListener;
	private int mOrientation = 0;
	private int mPicOrientation;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Loge.i("onCreate");
		setContentView(R.layout.activity_camera);

		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		mScreenHeight = dm.heightPixels;
		mSreenWidth = dm.widthPixels;
		mActionBarHeight = this.getResources().getDimensionPixelSize(
				R.dimen.action_bar_height);

		if (!checkCameraHardware(this)) {
			Toast.makeText(this, "No camera", Toast.LENGTH_LONG).show();
		}

		if (!Environment.getExternalStorageState().equals("mounted")) {
			Toast.makeText(this, "Please insert SDcard", Toast.LENGTH_LONG)
					.show();
		}

		mPreviewPanel = (FrameLayout) findViewById(R.id.camera_preview);
		mSwitchCamera = (ImageButton) findViewById(R.id.action_bar_switch_btn);
		mFlashLight = (ImageButton) findViewById(R.id.action_bar_flash_btn);
		mBack = (ImageButton) findViewById(R.id.action_bar_back_btn);
		mCapture = (ImageButton) findViewById(R.id.capture_btn);
		mSavePicPanel = (LinearLayout) findViewById(R.id.save_pic_panel);
		mRetake = (ImageButton) findViewById(R.id.retake_btn);
		mSave = (ImageButton) findViewById(R.id.save_btn);

		// Create our Preview view and set it as the content of our activity.
		FrameLayout.LayoutParams cameraParams = new FrameLayout.LayoutParams(
				mSreenWidth, mSreenWidth);
		cameraParams.gravity = Gravity.CENTER;

		mPreview = new CameraPreview(this);
		mPreviewPanel.addView(mPreview, cameraParams);

		// Create focus overlay layout
		mFocusView = new FocusView(this);
		mPreviewPanel.addView(mFocusView, cameraParams);

		int maskHeight = mScreenHeight
				- this.getResources().getDimensionPixelSize(
						R.dimen.action_bar_height)
				- this.getResources().getDimensionPixelSize(
						R.dimen.capture_panel_size) - mSreenWidth;
		maskHeight = maskHeight / 2;

		ImageView maskImageTop = new ImageView(this);
		maskImageTop.setBackgroundColor(0xff747474);

		FrameLayout.LayoutParams maskParamsTop = new FrameLayout.LayoutParams(
				mSreenWidth, maskHeight);
		maskParamsTop.gravity = Gravity.TOP;
		mPreviewPanel.addView(maskImageTop, maskParamsTop);

		ImageView maskImageBottom = new ImageView(this);
		maskImageBottom.setBackgroundColor(0xff747474);

		FrameLayout.LayoutParams maskParamsBottom = new FrameLayout.LayoutParams(
				mSreenWidth, maskHeight);
		maskParamsBottom.gravity = Gravity.BOTTOM;
		mPreviewPanel.addView(maskImageBottom, maskParamsBottom);

		mBack.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		mCapture.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// get an image from the camera
				mPicOrientation = mOrientation;
				mCamera.takePicture(null, null, mPicture);
				mFocusView.setVisibility(View.GONE);
				mCamera.stopPreview();
			}
		});

		mSwitchCamera.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (NUM_CAMERA == 1) {
					return;
				}

				new Thread(new Runnable() {

					@Override
					public void run() {
						if (mCamera != null) {
							mCamera.setPreviewCallback(null);
							mCamera.stopPreview();
							mCamera.release();
						}
						mCamera = null;
						for (int i = 0; i < NUM_CAMERA; i++) {
							if (mSelectedCamera != i) {
								mCamera = getCameraInstance(i);
								mSelectedCamera = i;
								break;
							}
						}
						try {
							mCamera.setPreviewDisplay(mPreview.getHolder());
						} catch (IOException e) {
							e.printStackTrace();
						}
						mPreview.setCamera(mCamera);
						mFocusView.setCamera(mCamera);
						mCamera.startPreview();

						mHandler.sendEmptyMessage(MSG_SWITCH_CAMERA_SUCCESS);
					}
				}).run();
			}
		});

		mFlashLight.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mCamera != null) {
					Camera.Parameters params = mCamera.getParameters();
					List<String> flashList = params.getSupportedFlashModes();

					if (flashList.size() == 0) {
						return;
					}

					mSelectedFlashMode++;
					if (mSelectedFlashMode > 2) {
						mSelectedFlashMode = 0;
					}
					switch (mSelectedFlashMode) {
					case 0:
						if (flashList
								.contains(Camera.Parameters.FLASH_MODE_OFF)) {
							params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
							mFlashLight
									.setImageResource(R.drawable.action_bar_flash_off);
						}
						break;
					case 1:
						if (flashList
								.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
							params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
							mFlashLight
									.setImageResource(R.drawable.action_bar_flash_auto);
						}
						break;
					case 2:
						if (flashList.contains(Camera.Parameters.FLASH_MODE_ON)) {
							params.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
							mFlashLight
									.setImageResource(R.drawable.action_bar_flash_on);
						}
						break;
					default:
						break;
					}
					mCamera.setParameters(params);
				}
			}
		});

		mSave.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mPicData != null) {
					new SavePicTask().execute(mPicData);
				}
			}
		});

		mRetake.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mCamera != null) {
					mPicData = null;
					mCamera.startPreview();
					mCapture.setVisibility(View.VISIBLE);
					mFocusView.setVisibility(View.VISIBLE);
					mSavePicPanel.setVisibility(View.GONE);
				}
			}
		});

		mOrientationListener = new MyOrientationEventListener(this);
	}

	private Handler mHandler = new Handler(new Handler.Callback() {

		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_SWITCH_CAMERA_SUCCESS:
				if (mSelectedCamera == 0) {
					mFlashLight.setVisibility(View.VISIBLE);
				} else {
					mFlashLight.setVisibility(View.GONE);
				}
				break;
			case MSG_NOT_EXT_STORAGE:
				Toast.makeText(
						getApplicationContext(),
						getApplicationContext().getResources().getString(
								R.string.save_pic_fail), Toast.LENGTH_LONG)
						.show();
				finish();
				break;
			case MSG_SAVE_PIC_SUCCESS:
				if (mFileFullPath != null && !mFileFullPath.isEmpty()) {
					Intent resultData = new Intent();
					resultData.putExtra("path", mFileFullPath);
					resultData.putExtra("orientation", mPicOrientation);

					setResult(RESULT_OK, resultData);
					finish();
				}
				break;
			default:
				break;
			}
			return false;
		}
	});

	@Override
	protected void onResume() {
		super.onResume();
		try {
			mCamera = getCameraInstance(mSelectedCamera);

			try {
				mCamera.setPreviewDisplay(mPreview.getHolder());
			} catch (IOException e) {
				e.printStackTrace();
			}
			mPreview.setCamera(mCamera);
			mFocusView.setCamera(mCamera);
			mCamera.startPreview();
		} catch (NullPointerException e) {
			Toast.makeText(this,
					this.getResources().getString(R.string.open_camera_fail),
					Toast.LENGTH_LONG).show();
			finish();
		}
		mOrientationListener.enable();
	}

	@Override
	protected void onPause() {
		super.onPause();
		releaseCamera(); // release the camera immediately on pause event
		mOrientationListener.disable();
	}

	private void releaseCamera() {
		if (mCamera != null) {
			mCamera.setPreviewCallback(null);
			mCamera.stopPreview();
			mCamera.release(); // release the camera for other applications
			mCamera = null;
		}
	}

	/** Check if this device has a camera */
	private boolean checkCameraHardware(Context context) {
		if (context.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_CAMERA)) {
			// this device has a camera
			return true;
		} else {
			// no camera on this device
			return false;
		}
	}

	/** A safe way to get an instance of the Camera object. */
	private Camera getCameraInstance(int id) {
		Camera c = null;

		try {
			if (NUM_CAMERA == 1) {
				c = Camera.open();
			} else {
				c = Camera.open(id); // attempt to get a Camera instance
			}
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
		}

		if (c == null) {
			return null;
		}
		c.setDisplayOrientation(90);
		Camera.Parameters params = c.getParameters();

		List<Camera.Size> sizeList = params.getSupportedPreviewSizes();
		Iterator<Camera.Size> itor = sizeList.iterator();

		Camera.Size size = null;
		while (itor.hasNext()) {
			Camera.Size cur = itor.next();
			Loge.d("Preview Height: " + cur.height + " Width: " + cur.width);
			float min = (float) cur.width / (float) cur.height
					- (float) mSreenWidth / (float) mSreenWidth;
			min = Math.abs(min) * 100;
			if ((int) min == 0) {
				size = cur;
				break;
			}
		}

		if (size == null) {
			size = sizeList.get(0);
		}

		Loge.d("Selected Picture Height: " + size.height + " Width: "
				+ size.width);
		if (size.width != size.height) {
			FrameLayout.LayoutParams cameraParams = new FrameLayout.LayoutParams(
					mSreenWidth, mSreenWidth * size.width / size.height);
			cameraParams.gravity = Gravity.CENTER;
			if (mPreview != null && mFocusView != null) {
				mPreview.setLayoutParams(cameraParams);
				mFocusView.setPreviewSize(size.height, size.width);
			}
		}

		List<Camera.Size> picSizeList = params.getSupportedPictureSizes();
		Iterator<Camera.Size> picSizeItor = picSizeList.iterator();

		List<Camera.Size> cubicPicSizeList = new ArrayList<Camera.Size>();

		while (picSizeItor.hasNext()) {
			Camera.Size cur = picSizeItor.next();
			if (cur.height == cur.width) {
				cubicPicSizeList.add(cur);
			}
		}

		Iterator<Camera.Size> cubicPicSizeItor = cubicPicSizeList.iterator();
		Camera.Size picSize = null;
		while (cubicPicSizeItor.hasNext()) {
			Camera.Size cur = cubicPicSizeItor.next();
			Loge.d("Cubic Pic Height: " + cur.height + " Width: " + cur.width);
			if (cur.width > 1080 && cur.width < 1700) {
				picSize = cur;
				break;
			}
		}

		if (picSize == null) {
			picSize = size;
		}

		Loge.d("Selected Pic Height: " + picSize.height + " Width: "
				+ picSize.width);

		params.setPreviewSize(size.width, size.height);
		params.setPictureSize(picSize.width, picSize.height);
		params.setPreviewFrameRate(15);
		params.setPreviewFormat(PixelFormat.YCbCr_420_SP);

		if (id == 0) {
			List<String> flashList = params.getSupportedFlashModes();
			if (flashList.size() == 0) {
				mFlashLight.setVisibility(View.GONE);
			} else {
				mSelectedFlashMode = 1;
				params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
				mFlashLight.setImageResource(R.drawable.action_bar_flash_auto);
			}
		}

		List<String> whiteBalanceList = params.getSupportedWhiteBalance();
		if (whiteBalanceList.contains(Camera.Parameters.WHITE_BALANCE_AUTO)) {
			params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
		}

		c.setParameters(params);

		return c; // returns null if camera is unavailable
	}

	private PictureCallback mPicture = new PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			mPicData = data;

			mCapture.setEnabled(false);

			if (mPicData != null) {
				new SavePicTask().execute(mPicData);
			}
			/*
			 * mCapture.setVisibility(View.GONE);
			 * mSavePicPanel.setVisibility(View.VISIBLE);
			 */
		}
	};

	/** Create a file Uri for saving an image or video */
	private Uri getOutputMediaFileUri(int type) {
		return Uri.fromFile(getOutputMediaFile(type));
	}

	/** Create a File for saving an image or video */
	private File getOutputMediaFile(int type) {
		// To be safe, you should check that the SDCard is mounted
		// using Environment.getExternalStorageState() before doing this.

		Loge.d("ExternalStorageState " + Environment.getExternalStorageState());
		if (Environment.getExternalStorageState().equals("mounted")) {
			File mediaStorageDir = new File(
					Environment
							.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
					"CubicCamera");
			// This location works best if you want the created images to be
			// shared
			// between applications and persist after your app has been
			// uninstalled.

			// Create the storage directory if it does not exist
			if (!mediaStorageDir.exists()) {
				if (!mediaStorageDir.mkdirs()) {
					Loge.d("failed to create directory");
					return null;
				}
			}

			// Create a media file name
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
					.format(new Date());
			File mediaFile;
			if (type == MEDIA_TYPE_IMAGE) {
				mediaFile = new File(mediaStorageDir.getPath() + File.separator
						+ "IMG_" + timeStamp + ".jpg");
			} else {
				return null;
			}

			mFileFullPath = mediaFile.getPath();

			return mediaFile;
		} else {
			return null;
		}

	}

	class SavePicTask extends AsyncTask<byte[], Void, String> {

		@Override
		protected String doInBackground(byte[]... params) {
			Loge.d("SavePicTask doInBackground");

			byte[] data = null;

			if (params.length > 0) {
				data = params[0];
			}

			if (data == null) {
				return null;
			}

			File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
			if (pictureFile == null) {
				Loge.d("Error creating media file, check storage permissions");
				return null;
			}

			try {
				FileOutputStream fos = new FileOutputStream(pictureFile);
				fos.write(data);
				fos.close();
			} catch (FileNotFoundException e) {
				Loge.d("File not found: " + e.getMessage());
			} catch (IOException e) {
				Loge.d("Error accessing file: " + e.getMessage());
			}
			return "success";
		}

		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				mHandler.sendEmptyMessage(MSG_SAVE_PIC_SUCCESS);
			} else {
				mHandler.sendEmptyMessage(MSG_NOT_EXT_STORAGE);
			}
			super.onPostExecute(result);
		}
	}

	class MyOrientationEventListener extends OrientationEventListener {

		public static final int ORIENTATION_HYSTERESIS = 5;

		public MyOrientationEventListener(Context context) {
			super(context);
		}

		@Override
		public void onOrientationChanged(int orientation) {
			mOrientation = roundOrientation(orientation, mOrientation);
			if (mFocusView != null) {
				mFocusView.setOrientation(mOrientation);
			}
		}

		int roundOrientation(int orientation, int orientationHistory) {
			boolean changeOrientation = false;
			if (orientationHistory == OrientationEventListener.ORIENTATION_UNKNOWN) {
				changeOrientation = true;
			} else {
				int dist = Math.abs(orientation - orientationHistory);
				dist = Math.min(dist, 360 - dist);
				changeOrientation = (dist >= 45 + ORIENTATION_HYSTERESIS);
			}
			if (changeOrientation) {
				return ((orientation + 45) / 90 * 90) % 360;
			}
			return orientationHistory;
		}

	}
}
