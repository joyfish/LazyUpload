/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.comic.lazyupload.camera;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;

import com.comic.lazyupload.Loge;
import com.comic.lazyupload.R;

public class FocusView extends View {

	private static final int FOCUS_AREA_SIZE = 100;
	private static final int HALF_PREVIEW_AREA_SIZE = 1000;

	private Camera mCamera;

	boolean mSupportAutoFocus;

	private Context mCtx;

	private int mTargetHeight;
	private int mTargetWidth;

	private int mPreviewHeight = 2000;
	private int mPreviewWidth = 2000;

	private int mAreaSizeRecount = HALF_PREVIEW_AREA_SIZE * 2;

	private Paint mFocusPaint;
	private Paint mFocusPaintDust;

	private float mCenterX;
	private float mCenterY;

	private Path mFocusPath;
	private Path mFocusDustPath;

	private int mFocusOffset;
	private int mFocusInsidePadding;
	private int mFocusInsideOffset;

	private boolean mUnderFocus = false;

	private int mOrientation = 0;

	public FocusView(Context context) {
		super(context);
		mCtx = context;

		mFocusPaint = new Paint();
		mFocusPaint.setStrokeWidth(5);
		mFocusPaint.setColor(Color.GRAY);

		mFocusPaintDust = new Paint();
		mFocusPaintDust.setColor(Color.BLACK);
		mFocusPaintDust.setXfermode(new PorterDuffXfermode(Mode.DST_OUT));

		mFocusPath = new Path();
		mFocusDustPath = new Path();

		mFocusOffset = context.getResources().getDimensionPixelSize(
				R.dimen.focus_offset);
		mFocusInsidePadding = context.getResources().getDimensionPixelSize(
				R.dimen.focus_insde_padding);
		mFocusInsideOffset = context.getResources().getDimensionPixelSize(
				R.dimen.focus_insde_offset);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mTargetHeight = h;
		mTargetWidth = w;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawPath(mFocusPath, mFocusPaint);
		canvas.drawPath(mFocusDustPath, mFocusPaintDust);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			mUnderFocus = true;
			mFocusPath.reset();
			invalidate();

			float x = event.getY();
			float y = event.getX();

			mCenterY = event.getY();
			mCenterX = event.getX();

			requestFocus(x, y);

			return true;
		}

		return false;
	}

	public void requestFocus(float x, float y) {
		// mAreaSizeRecount only base on the view height so no need to
		// change ty
		if (mCamera != null && mSupportAutoFocus) {
			float tX = x / mTargetHeight * mAreaSizeRecount - mAreaSizeRecount
					/ 2;
			float tY = HALF_PREVIEW_AREA_SIZE - y / mTargetWidth
					* HALF_PREVIEW_AREA_SIZE * 2;
			Loge.d("Focus touched ori X: " + x + ", ori Y: " + y);
			Loge.d("Focus touched X: " + tX + ", Y: " + tY);

			int left, top, right, bottom;

			if (tX < -900) {
				left = -1000;
				right = -900;
			} else if (tX > 900) {
				left = 900;
				right = 1000;
			} else {
				left = (int) (tX - 100);
				right = (int) (tX + 100);
			}

			int bound = (FOCUS_AREA_SIZE - mAreaSizeRecount / 2);
			if (tY < bound) {
				top = bound - FOCUS_AREA_SIZE;
				bottom = bound;
			} else if (tY > -bound) {
				top = -bound;
				bottom = -bound + FOCUS_AREA_SIZE;
			} else {
				top = (int) (tY - 100);
				bottom = (int) (tY + 100);
			}

			Loge.d("left: " + left + "top: " + top + "right: " + right
					+ "bottom: " + bottom);

			Camera.Parameters params = mCamera.getParameters();

			List<Camera.Area> focusAreaList = new ArrayList<Camera.Area>();
			Rect areaRect1 = new Rect(left, top, right, bottom);
			Camera.Area area = new Camera.Area(areaRect1, 1);
			focusAreaList.add(area);

			params.setFocusAreas(focusAreaList);
			params.setMeteringAreas(focusAreaList);

			mCamera.setParameters(params);

			mCamera.autoFocus(mAutoFocusCallback);

			drawFocusOutline(mCenterX, mCenterY);

		}
		invalidate();
	}

	private void drawFocusOutline(float x, float y) {
		mFocusPath.reset();
		invalidate();

		mFocusPath.addRect(new RectF(x - mFocusOffset, y - mFocusOffset, x
				+ mFocusOffset, y + mFocusOffset), Path.Direction.CCW);

		Loge.d("Focus touched orientation: "
				+ mCtx.getResources().getConfiguration().orientation);

		mFocusDustPath.reset();

		switch (mOrientation) {
		case 0:
		case 180: {
			int inside = mFocusOffset - mFocusInsidePadding;
			mFocusDustPath.addRect(new RectF(x - inside, y - inside,
					x + inside, y + inside), Path.Direction.CCW);
			int insideNew = mFocusOffset - mFocusInsideOffset;
			mFocusDustPath.addRect(new RectF(x - insideNew, y - mFocusOffset, x
					+ insideNew, y + mFocusOffset), Path.Direction.CCW);
		}
			break;
		case 90:
		case 270: {
			int inside = mFocusOffset - mFocusInsidePadding;
			mFocusDustPath.addRect(new RectF(x - inside, y - inside,
					x + inside, y + inside), Path.Direction.CCW);
			int insideNew = mFocusOffset - mFocusInsideOffset;
			mFocusDustPath.addRect(new RectF(x - mFocusOffset, y - insideNew, x
					+ mFocusOffset, y + insideNew), Path.Direction.CCW);
		}
			break;
		default:
			break;
		}
	}

	public void setCamera(Camera camera) {
		mCamera = camera;
		Camera.Parameters params = mCamera.getParameters();
		List<String> focusModeList = params.getSupportedFocusModes();
		mSupportAutoFocus = false;
		if (focusModeList.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
			params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
			mSupportAutoFocus = true;
		}
		Loge.d("SupportAutoFocus : " + mSupportAutoFocus);

		mCamera.setParameters(params);
	}

	public void setPreviewSize(int h, int w) {
		mPreviewHeight = h;
		mPreviewWidth = w;

		mAreaSizeRecount = HALF_PREVIEW_AREA_SIZE * 2 * mPreviewHeight
				/ mPreviewWidth;
		Loge.d("area_size_recount: " + mAreaSizeRecount);
	}

	public void setOrientation(int orientation) {
		if (mOrientation != orientation) {
			mOrientation = orientation;
			if (mUnderFocus) {
				drawFocusOutline(mCenterX, mCenterY);
			}
		}
	}

	Camera.AutoFocusCallback mAutoFocusCallback = new Camera.AutoFocusCallback() {
		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			Loge.d("onAutoFocus success: " + success);
			if (success) {
				mFocusPaint.setColor(Color.GREEN);
				invalidate();
				mDrawHandler.sendEmptyMessageDelayed(0, 200);
			} else {
				mDrawHandler.sendEmptyMessage(0);
			}
		}
	};

	private Handler mDrawHandler = new Handler(new Handler.Callback() {

		@Override
		public boolean handleMessage(Message msg) {
			mUnderFocus = false;
			mFocusPaint.setColor(Color.GRAY);
			mFocusPath.reset();
			mFocusDustPath.reset();
			invalidate();
			return false;
		}
	});

}
