/*
 * Copyright (C) 2014 AChep@xda <artemchep@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package com.achep.acdisplay.settings;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.acdisplay.blacklist.SharedList;
import com.achep.acdisplay.hotword.Hotword;
import com.achep.acdisplay.hotword.HotwordStorage;

import java.util.ArrayList;

/**
 * Created by Artem on 09.02.14.
 */
public class HotwordSettings extends ListFragment {

    private Adapter mAdapter;

    private Enabler mHotwordEnabler;
    private HotwordStorage mHotwordStorage;
    private SharedList.OnSharedListChangedListener<Hotword> mHotwordListener =
            new SharedList.OnSharedListChangedListener<Hotword>() {
        @Override
        public void onPut(Hotword hotwordNew,
                          Hotword hotwordOld, int diff) {
            mAdapter.notifyDataSetInvalidated();
            mAdapter.mArrayList.add(hotwordNew);
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onRemoved(Hotword hotwordRemoved) {
            mAdapter.notifyDataSetInvalidated();
            mAdapter.mArrayList.remove(hotwordRemoved);
            mAdapter.notifyDataSetChanged();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        Activity activity = getActivity();
        ActionBar actionBar = activity.getActionBar();
        assert actionBar != null;

        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.layout_ab_switch);
        Switch switch_ = (Switch) actionBar.getCustomView().findViewById(R.id.switch_);
        mHotwordEnabler = new Enabler(activity, switch_, Config.KEY_HOTWORD);
        mHotwordStorage = HotwordStorage.getInstance(activity);

        mAdapter = new Adapter(activity, R.layout.preference_blacklist_app_config);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setEmptyText(getString(R.string.settings_hotword_empty));

        setListAdapter(mAdapter);
        getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        getListView().setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                mode.setTitle(Integer.toString(getListView().getCheckedItemCount()));
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.hotword_settings_choice, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_remove:
                        final ListView lv = getListView();

                        SparseBooleanArray array = lv.getCheckedItemPositions();
                        ArrayList<Hotword> hotwords = new ArrayList<>();

                        // Get hotwords from items
                        int length = array.size();
                        for (int i = 0; i < length; i++) {
                            hotwords.add((Hotword) lv.getItemAtPosition(array.keyAt(i)));
                        }

                        // Remove hotwords
                        Context context = getActivity();
                        for (Hotword hotword : hotwords) {
                            mHotwordStorage.remove(context, hotword);
                        }

                        mode.finish();
                        break;
                    default:
                        return false;
                }
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) { /* unused */ }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mHotwordEnabler.resume();
        mHotwordStorage.registerListener(mHotwordListener);
        initHotwordsList();
    }

    /**
     * Re-adds all hotwords to adapter.
     */
    private void initHotwordsList() {
        mAdapter.notifyDataSetInvalidated();
        mAdapter.mArrayList.clear();
        mAdapter.mArrayList.addAll(mHotwordStorage.valuesSet());
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        mHotwordEnabler.pause();
        mHotwordStorage.unregisterListener(mHotwordListener);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.hotword_settings, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                mHotwordStorage.put(getActivity(), new Hotword("Google", "" + Math.random()));
                break;
            default:
                return false;
        }
        return true;
    }

    private static class Adapter extends ArrayAdapter<Hotword> {

        private final ArrayList<Hotword> mArrayList = new ArrayList<>();
        private final LayoutInflater mInflater;
        private final int mResource;

        public Adapter(Context context, int resource) {
            super(context, 0);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mResource = resource;
        }

        private static class Holder {
            ImageView icon;
            TextView title;
            TextView summary;
        }

        @Override
        public int getCount() {
            return mArrayList.size();
        }

        @Override
        public Hotword getItem(int position) {
            return mArrayList.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Hotword hotword = getItem(position);
            final Holder holder;
            final View view;

            if (convertView == null) {
                holder = new Holder();

                view = mInflater.inflate(mResource, parent, false);
                holder.icon = (ImageView) view.findViewById(R.id.icon);
                holder.title = (TextView) view.findViewById(android.R.id.title);
                holder.summary = (TextView) view.findViewById(android.R.id.summary);

                view.setTag(holder);
            } else {
                view = convertView;
                holder = (Holder) view.getTag();
            }

            // All view fields must be updated every time, because the view may
            // be recycled
            holder.title.setText(hotword.speech);
            holder.summary.setText(hotword.action);

            return view;
        }

    }
}