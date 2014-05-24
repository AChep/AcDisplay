package com.achep.acdisplay.settings;

import java.util.List;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class FontDialogBaseAdapter extends BaseAdapter {

	private List<String> mFonts;
	private Context mContext;
	private int layoutResId;

	public FontDialogBaseAdapter(Context c, List<String> fonts, int resId) {
		this.mContext = c;
		this.mFonts = fonts;
		this.layoutResId = resId;
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mFonts.size();
	}

	@Override
	public Object getItem(int arg0) {
		// TODO Auto-generated method stub
		return mFonts.get(arg0);
	}

	@Override
	public long getItemId(int arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int position, View v, ViewGroup parent) {
		// TODO Auto-generated method stub
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		v = inflater.inflate(layoutResId, parent, false);
		TextView text1 = (TextView) v.findViewById(android.R.id.text1);
		Typeface tf = Typeface.createFromAsset(mContext.getAssets(), mFonts.get(position));
		text1.setTypeface(tf);
		text1.setText(mFonts.get(position).replaceAll("fonts/","").replaceAll(".ttf", "").replaceAll(".otf", ""));
		return v;
	}

}