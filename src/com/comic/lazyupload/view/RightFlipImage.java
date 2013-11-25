package com.comic.lazyupload.view;

import java.net.MalformedURLException;
import java.net.URL;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.comic.lazyupload.R;
import com.comic.lazyupload.image.PhotoView;

public class RightFlipImage extends RelativeLayout {

	private PhotoView mFrountImg, mBackImg;

	private boolean mShowBack = false;

	private Animator mRightIn, mRightOut;

	public RightFlipImage(Context context) {
		super(context);
		addImage(context);
		initAnim(context);
	}

	public RightFlipImage(Context context, AttributeSet attrs) {
		super(context, attrs);
		addImage(context);
		initAnim(context);
	}

	private void addImage(Context ctx) {
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.MATCH_PARENT);
		params.addRule(RelativeLayout.CENTER_IN_PARENT);

		mBackImg = new PhotoView(ctx);
		mBackImg.setCustomDownloadingImage(R.drawable.gray_image_downloading);
		mBackImg.setAsPreview(true);
		addView(mBackImg, params);
		mBackImg.setVisibility(View.GONE);

		mFrountImg = new PhotoView(ctx);
		mFrountImg.setCustomDownloadingImage(R.drawable.gray_image_downloading);
		mFrountImg.setAsPreview(true);
		addView(mFrountImg, params);
	}

	private void initAnim(Context ctx) {
		mRightIn = AnimatorInflater.loadAnimator(ctx,
				R.animator.card_flip_right_in);
		mRightOut = AnimatorInflater.loadAnimator(ctx,
				R.animator.card_flip_right_out);

		mRightIn.addListener(new Animator.AnimatorListener() {

			@Override
			public void onAnimationStart(Animator animation) {
			}

			@Override
			public void onAnimationEnd(Animator animation) {
				if (mShowBack) {
					mFrountImg.setVisibility(View.GONE);

				} else {
					mBackImg.setVisibility(View.GONE);
				}
			}

			@Override
			public void onAnimationCancel(Animator animation) {
			}

			@Override
			public void onAnimationRepeat(Animator animation) {
			}

		});
	}

	public void setBackImageSrc(int res) {
		mBackImg.setImageResource(res);
	}

	public void setFrountImageSrc(int res) {
		mFrountImg.setImageResource(res);
	}

	public void invalidateBack() {
		mBackImg.invalidate();
	}

	public void invalidateFrount() {
		mFrountImg.invalidate();
	}

	public void setBackImageUrl(String url) {
		try {
			URL imgURL = new URL(url);
			mBackImg.setImageURL(imgURL, false, true, null);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	public void setFrountImageUrl(String url) {
		try {
			URL imgURL = new URL(url);
			mFrountImg.setImageURL(imgURL, false, true, null);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	public void filpImage() {
		if (mShowBack) {
			mShowBack = false;
			mFrountImg.setVisibility(View.VISIBLE);
			mRightIn.setTarget(mFrountImg);
			mRightIn.start();
			mRightOut.setTarget(mBackImg);
			mRightOut.start();
		} else {
			mShowBack = true;
			mBackImg.setVisibility(View.VISIBLE);
			mRightIn.setTarget(mBackImg);
			mRightIn.start();
			mRightOut.setTarget(mFrountImg);
			mRightOut.start();

		}
	}

}
