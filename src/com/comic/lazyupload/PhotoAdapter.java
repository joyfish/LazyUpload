package com.comic.lazyupload;

import java.util.ArrayList;
import java.util.Random;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.comic.lazyupload.view.RightFlipImage;

public class PhotoAdapter extends BaseAdapter {

	private static final int MSG_FLIP = 31;

	private ArrayList<String> mListData = new ArrayList<String>();
	private int[] mFlipSer = { 0, 1, 2, 3, 5, 6, 7, 8 };

	static class ViewHolder {
		RightFlipImage image;
	}

	private Context mContext;

	public PhotoAdapter(Context context) {
		mContext = context;
	}

	@Override
	public int getCount() {
		return 9;
	}

	@Override
	public Object getItem(int position) {
		if (position < mListData.size()) {
			return mListData.get(position);
		}
		return null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = LayoutInflater.from(mContext.getApplicationContext())
					.inflate(R.layout.photo_grid_item, null);
			holder = new ViewHolder();
			holder.image = (RightFlipImage) convertView
					.findViewById(R.id.around_image);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		if (position == 4 && getCount() > 4) {
			holder.image.setFrountImageSrc(R.drawable.grid_camera_btn_normal);
		} else {
			holder.image.setFrountImageSrc(R.drawable.ic_launcher);
			holder.image.setBackImageSrc(R.drawable.gray_image_downloading);
		}

		return convertView;
	}

	void setListData(ArrayList<String> listData) {
		genRandomSer();
	}

	private void genRandomSer() {
		int index, t;
		for (int i = 0; i < mFlipSer.length; i++) {
			index = new Random().nextInt(3);
			t = mFlipSer[i];
			mFlipSer[i] = mFlipSer[index];
			mFlipSer[index] = t;
		}
		for (int i : mFlipSer) {
			Loge.i("mFlipSer no." + i);
		}
	}

	private Handler mHandler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_FLIP:
				
				break;
			default:
				break;
			}
			return false;
		}
	});

}
