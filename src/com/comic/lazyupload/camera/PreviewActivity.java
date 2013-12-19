package com.comic.lazyupload.camera;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.comic.lazyupload.Loge;
import com.comic.lazyupload.R;

public class PreviewActivity extends Activity {

	public static final int MEDIA_TYPE_IMAGE = 1;

	boolean enableFrame = true;

	private JniBitmapHolder bitmapHolder = null;

	private ImageView mPreviewImage;
//	private ImageView mEmojiImage;

	private ImageButton mSaveBtn, mRetakeBtn;

	private String mImagePath = null;

	private Bitmap mImage;

	private int mScreenWidth;

	private int mOrientation = 0;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.preview_activity);

		final DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		mScreenWidth = displayMetrics.widthPixels;

		mPreviewImage = (ImageView) findViewById(R.id.preview_image);
		FrameLayout.LayoutParams previewParams = new FrameLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT, mScreenWidth);
		mPreviewImage.setLayoutParams(previewParams);

//		mEmojiImage = (ImageView) findViewById(R.id.emoji_image);
//		FrameLayout.LayoutParams emojiParams = new FrameLayout.LayoutParams(
//				mScreenWidth / 10, mScreenWidth / 10);
//		emojiParams.setMargins(mScreenWidth / 50, mScreenWidth / 50, 0, 0);
//		mEmojiImage.setLayoutParams(emojiParams);

		mSaveBtn = (ImageButton) findViewById(R.id.preview_save);
		mRetakeBtn = (ImageButton) findViewById(R.id.preview_retake);

		mSaveBtn.setOnClickListener(mSaveClicked);
		mRetakeBtn.setOnClickListener(mRetakeClicked);

		getDataFromExtra();
		new SavePicTask().execute();
	}

	private View.OnClickListener mSaveClicked = new View.OnClickListener() {

		@Override
		public void onClick(View v) {

			new Thread(new Runnable() {
				@Override
				public void run() {
					// saveImageWithEmoji();
					mHandler.sendEmptyMessage(0);
				}
			}).start();
		}
	};

	private View.OnClickListener mRetakeClicked = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			deleteImageFile();
			setResult(RESULT_CANCELED);
			finish();
		}
	};

	private Handler mHandler = new Handler(new Handler.Callback() {

		@Override
		public boolean handleMessage(Message msg) {
			Intent intent = new Intent();
			intent.putExtra("path", mImagePath);
			setResult(RESULT_OK, intent);
			finish();
			return true;
		}
	});

	void getDataFromExtra() {
		Intent intent = getIntent();
		mImagePath = intent.getStringExtra("path");
		mOrientation = intent.getIntExtra("orientation", 0);
	}

	void getImage() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				if (mImagePath != null) {
					BitmapFactory.Options opts = new BitmapFactory.Options();
					opts.inJustDecodeBounds = true;

					BitmapFactory.decodeFile(mImagePath, opts);

					int sampleSize = (int) (opts.outWidth / mScreenWidth);

					Loge.i("sampleSize = " + sampleSize);

					if (sampleSize < 1) {
						sampleSize = 1;
					}

					opts.inSampleSize = sampleSize;
					opts.inJustDecodeBounds = false;
					opts.inPurgeable = true;

					mImage = BitmapFactory.decodeFile(mImagePath, opts);

					if (mHandler != null) {
						mHandler.post(new Runnable() {
							@Override
							public void run() {
								if (mImage != null) {
									mPreviewImage.setImageBitmap(mImage);
								}
							}
						});
					}
				}
			}
		}).start();

	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mImage != null)
			mImage.recycle();
	}

	private Bitmap addEmojiOnPhoto(int emojiId) throws OutOfMemoryError {
		if (mImage != null) {
			int imageWidth = mImage.getWidth();
			int emojiMargin = imageWidth / 50;
			int emojiSize = imageWidth / 10;

			Bitmap emojiBitmap = getBitmapFromResources(emojiId, emojiSize);

			Bitmap overlay = Bitmap.createBitmap(imageWidth, imageWidth,
					mImage.getConfig());
			Canvas canvas = new Canvas(overlay);

			Paint bitmapPaint = new Paint();
			bitmapPaint.setFilterBitmap(true);
			canvas.drawBitmap(mImage, 0, 0, bitmapPaint);
			canvas.drawBitmap(emojiBitmap, emojiMargin, emojiMargin,
					bitmapPaint);

			emojiBitmap.recycle();

			return overlay;
		}
		return null;
	}

	private Bitmap getBitmapFromResources(int resId, int size) {
		Resources res = this.getResources();
		Bitmap origin = BitmapFactory.decodeResource(res, resId);
		if (origin.getWidth() > size) {
			Bitmap finalRes = scaleBitmap(origin, size);
			origin.recycle();
			return finalRes;
		}
		return origin;
	}

	private Bitmap scaleBitmap(Bitmap originBitmap, int size) {
		float sampleSize = size / (float) originBitmap.getWidth();
		Loge.i("sampleSize = " + sampleSize);
		if (sampleSize > 1) {
			sampleSize = 1;
		}
		Matrix matrix = new Matrix();
		matrix.postScale(sampleSize, sampleSize);
		Bitmap resizeBmp = Bitmap
				.createBitmap(originBitmap, 0, 0, originBitmap.getWidth(),
						originBitmap.getHeight(), matrix, true);
		return resizeBmp;
	}

	private void deleteImageFile() {
		File file = new File(mImagePath);
		file.delete();
	}

	// private void saveImageWithEmoji() {
	//
	// ByteArrayOutputStream ostream = new ByteArrayOutputStream();
	//
	// Bitmap withEmoji = null;
	// try {
	// withEmoji = addEmojiOnPhoto(mSelectEmoji);
	// } catch (OutOfMemoryError e) {
	// e.printStackTrace();
	// }
	//
	// if (withEmoji != null) {
	// if (mImage != null && !mImage.isRecycled()) {
	// mImage.recycle();
	// mImage = null;
	// }
	// withEmoji.compress(Bitmap.CompressFormat.JPEG, 100, ostream);
	// deleteImageFile();
	// } else {
	// mImage.compress(Bitmap.CompressFormat.JPEG, 100, ostream);
	// deleteImageFile();
	// }
	//
	// byte[] byteArray = ostream.toByteArray();
	//
	// if (withEmoji != null && !withEmoji.isRecycled()) {
	// withEmoji.recycle();
	// withEmoji = null;
	// }
	// if (mImage != null && !mImage.isRecycled()) {
	// mImage.recycle();
	// mImage = null;
	// }
	//
	// File pictureFile = new File(mImagePath);
	// try {
	// FileOutputStream fos = new FileOutputStream(pictureFile);
	// fos.write(byteArray);
	// fos.close();
	// } catch (FileNotFoundException e) {
	// Loge.d("File not found: " + e.getMessage());
	// } catch (IOException e) {
	// Loge.d("Error accessing file: " + e.getMessage());
	// }
	// }

	class SavePicTask extends AsyncTask<Void, Void, String> {

		@Override
		protected String doInBackground(Void... params) {
			Loge.d("SavePicTask doInBackground");

			if (bitmapHolder == null) {
				bitmapHolder = new JniBitmapHolder();
			}

			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inJustDecodeBounds = true;

			BitmapFactory.decodeFile(mImagePath, opts);

			int sampleSize = (int) (opts.outWidth / 1080);

			Loge.i("sampleSize = " + sampleSize);

			if (sampleSize < 1) {
				sampleSize = 1;
			}

			opts.inSampleSize = sampleSize;
			opts.inJustDecodeBounds = false;
			opts.inPurgeable = true;

			Bitmap originBitmap = BitmapFactory.decodeFile(mImagePath, opts);
			int originBitmapHeight = originBitmap.getHeight();
			int originBitmapWidth = originBitmap.getWidth();

			bitmapHolder.storeBitmap(originBitmap);

			if (originBitmap != null && !originBitmap.isRecycled()) {
				originBitmap.recycle();
				originBitmap = null;
			}

			// crop bitmap
			Loge.d("originBitmapHeight: " + originBitmapHeight
					+ " originBitmapWidth: " + originBitmapWidth);
			if (originBitmapHeight != originBitmapWidth) {
				if (originBitmapWidth > originBitmapHeight) {
					int startX = (originBitmapWidth - originBitmapHeight) / 2;
					bitmapHolder.cropBitmap(startX, 0, originBitmapHeight
							+ startX, originBitmapHeight);
				} else {
					int startY = (originBitmapHeight - originBitmapWidth) / 2;
					bitmapHolder.cropBitmap(0, startY, originBitmapWidth,
							originBitmapWidth + startY);
				}
			}

			// rotate bitmap
			switch (mOrientation) {
			case 0:
				bitmapHolder.rotateBitmapCw90();
				break;
			case 90:
				bitmapHolder.rotateBitmapCw90();
				bitmapHolder.rotateBitmapCw90();
				break;
			case 180:
				bitmapHolder.rotateBitmapCcw90();
				break;
			case 270:
				break;
			default:
				break;
			}

			Bitmap rotateBitmap = bitmapHolder.getBitmapAndFree();

			if (rotateBitmap == null) {
				return null;
			}
			Bitmap frameBitmap = null;
			try {
				// frameBitmap = useFrame(rotateBitmap);
				if (frameBitmap != null) {
					if (rotateBitmap != null && !rotateBitmap.isRecycled()) {
						rotateBitmap.recycle();
						rotateBitmap = null;
					}
				}
			} catch (OutOfMemoryError e) {
				e.printStackTrace();
			}

			ByteArrayOutputStream ostream = new ByteArrayOutputStream();

			if (frameBitmap != null) {
				frameBitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream);
			} else {
				rotateBitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream);
			}

			byte[] byteArray = ostream.toByteArray();

			if (rotateBitmap != null && !rotateBitmap.isRecycled()) {
				rotateBitmap.recycle();
				rotateBitmap = null;
			}
			if (frameBitmap != null && !frameBitmap.isRecycled()) {
				frameBitmap.recycle();
				frameBitmap = null;
			}

			File pictureFile = new File(mImagePath);

			try {
				FileOutputStream fos = new FileOutputStream(pictureFile);
				fos.write(byteArray);
				fos.close();
				byteArray = null;
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
				getImage();
			}
			super.onPostExecute(result);
		}
	}

	Bitmap useFrame(Bitmap origin) throws OutOfMemoryError {
		if (origin == null || !enableFrame) {
			return null;
		}
		int w = origin.getWidth();
		int h = origin.getHeight();
		int frameW = w / 20;

		Bitmap overlay = Bitmap.createBitmap(w + frameW * 2, h + frameW * 2,
				origin.getConfig());
		Canvas canvas = new Canvas(overlay);
		canvas.setBitmap(overlay);
		canvas.drawColor(Color.DKGRAY);

		Paint whitekPaint = new Paint();
		whitekPaint.setColor(Color.WHITE);
		whitekPaint.setStrokeWidth(10);
		canvas.drawRect((float) (frameW * 0.02), (float) (frameW * 0.02), w
				+ frameW * 2 - (float) (frameW * 0.05), h + frameW * 2
				- (float) (frameW * 0.05), whitekPaint);

		Paint underLinePaint = new Paint();
		underLinePaint.setColor(Color.GRAY);
		underLinePaint.setStrokeWidth(10);
		underLinePaint.setTextSize((float) (frameW * 0.9));
		underLinePaint.setTypeface(Typeface.SANS_SERIF);

		String timeStamp = new SimpleDateFormat("yy.MM.dd/HH:mm")
				.format(new Date());

		float textWidth = underLinePaint.measureText(timeStamp);
		canvas.drawText(timeStamp, w + frameW * 2 - textWidth - frameW, h
				+ frameW + (float) (frameW * 0.9), underLinePaint);

		canvas.drawBitmap(origin, frameW, frameW, null);

		return overlay;
	}

}
