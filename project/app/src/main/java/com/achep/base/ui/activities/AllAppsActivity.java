package com.achep.base.ui.activities;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.base.ui.adapters.ApplicationAdapter;

public class AllAppsActivity extends ListActivity {
    private PackageManager packageManager = null;
    private List < ApplicationInfo > applist = null;
    private ApplicationAdapter listadaptor = null;
    private String corner = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preference_apps);
        corner = getIntent().getStringExtra("corner");
        packageManager = getPackageManager();

        new LoadApplications().execute();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        ApplicationInfo app = applist.get(position);
        try {
            Intent intent = packageManager.getLaunchIntentForPackage(app.packageName);
            if (null != intent) {
                Config config = Config.getInstance();
                switch (corner) {
                    case "corner_action_left_top":
                        Config.getInstance().setCornerActionLeftTopCustomApp(this, intent.getPackage(), null);

                        break;
                    case "corner_action_right_top":
                        Config.getInstance().setCornerActionRightTopCustomApp(this, intent.getPackage(), null);

                        break;
                    case "corner_action_left_bottom":
                        Config.getInstance().setCornerActionLeftBottomCustomApp(this, intent.getPackage(), null);

                        break;
                    case "corner_action_right_bottom":
                        Config.getInstance().setCornerActionRightBottomCustomApp(this, intent.getPackage(), null);
                        break;
                    default:
                        throw new IllegalArgumentException("AllsAppsActivity Corner:" + corner);
                }
                this.finish();
            }
        } catch (ActivityNotFoundException e) {
            Toast.makeText(AllAppsActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(AllAppsActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private List < ApplicationInfo > checkForLaunchIntent(List < ApplicationInfo > list) {
        ArrayList < ApplicationInfo > applist = new ArrayList < ApplicationInfo > ();
        for (ApplicationInfo info: list) {
            try {
                if (null != packageManager.getLaunchIntentForPackage(info.packageName)) {
                    applist.add(info);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return applist;
    }

    private class LoadApplications extends AsyncTask < Void, Void, Void > {
        private ProgressDialog progress = null;

        @Override
        protected Void doInBackground(Void...params) {
            applist = checkForLaunchIntent(packageManager.getInstalledApplications(PackageManager.GET_META_DATA));
            listadaptor = new ApplicationAdapter(AllAppsActivity.this, R.layout.preference_apps_list_row, applist);
            listadaptor.sort(new Comparator<ApplicationInfo>() {
                @Override
                public int compare(ApplicationInfo first, ApplicationInfo second) {
                    return first.loadLabel(packageManager).toString().compareToIgnoreCase(second.loadLabel(packageManager).toString());
                }
            });
            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected void onPostExecute(Void result) {
            setListAdapter(listadaptor);
            progress.dismiss();
            super.onPostExecute(result);
        }

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(AllAppsActivity.this, null, "Loading application info...");
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Void...values) {
            super.onProgressUpdate(values);
        }
    }
}