/*
 * Copyright (C) 2010 Felix Bechstein
 * 
 * This file is part of AdBlock.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */
package de.ub0r.android.adBlock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class AdBlock extends Activity implements OnClickListener,
		OnItemClickListener {
	public static boolean ENABLE = false;
	/** Tag for output. */
	private static final String TAG = "AdBlock";

	/** Prefs: name for last version run. */
	private static final String PREFS_LAST_RUN = "lastrun";
	/** Preferences: import url. */
	private static final String PREFS_IMPORT_URL = "importurl";

	/** Filename for export of filter. */

	/** ItemDialog: edit. */
	private static final short ITEM_DIALOG_EDIT = 0;
	/** ItemDialog: delete. */
	private static final short ITEM_DIALOG_DELETE = 1;

	/** Dialog: about. */
	private static final int DIALOG_ABOUT = 0;
	/** Dialog: import. */
	private static final int DIALOG_IMPORT = 1;
	/** Dialog: update. */
	private static final int DIALOG_UPDATE = 2;

	/** Prefs. */
	public SharedPreferences preferences;
	/** Prefs. import URL. */
	private String importUrl = null;

	/** The filter. */
	private ArrayList<String> filter = new ArrayList<String>();
	/** The ArrayAdapter. */
	private ArrayAdapter<String> adapter = null;

	/** Robert's filter. */
	private ArrayList<String> filter2 = new ArrayList<String>();
	/** Robert's ArrayAdapter. */
	private ArrayAdapter<String> adapter2 = null;

	/** Editmode? */
	private int itemToEdit = -1;

	/**
	 * Import filter from URL on background.
	 * 
	 * @author Felix Bechstein
	 */
	class Importer extends AsyncTask<String, Boolean, Boolean> {
		/** Error message. */
		private String message = "";

		/**
		 * Do the work.
		 * 
		 * @param dummy
		 *            nothing here
		 * @return successful?
		 */
		@Override
		protected final Boolean doInBackground(final String... dummy) {
			try {
				HttpURLConnection c = (HttpURLConnection) (new URL(
						AdBlock.this.importUrl)).openConnection();
				int resp = c.getResponseCode();
				if (resp != 200) {
					return false;
				}
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(c.getInputStream()));
				AdBlock.this.filter.clear();
				while (true) {
					String s = reader.readLine();
					if (s == null) {
						break;
					}
					s = s.trim();
					if (s.length() > 0) {
						AdBlock.this.filter.add(s);
					}
				}
				reader.close();
				return true;
			} catch (MalformedURLException e) {
				Log.e(AdBlock.TAG, null, e);
				this.message = e.toString();
				return false;
			} catch (IOException e) {
				this.message = e.toString();
				Log.e(AdBlock.TAG, null, e);
				return false;
			}
		}

		/**
		 * Merge imported filter to the real one.
		 * 
		 * @param result
		 *            nothing here
		 */
		@Override
		protected final void onPostExecute(final Boolean result) {
			if (result.booleanValue()) {
				Toast.makeText(AdBlock.this, "imported", Toast.LENGTH_LONG)
						.show();
				AdBlock.this.adapter.notifyDataSetChanged();
			} else {
				Toast.makeText(AdBlock.this, "failed: " + this.message,
						Toast.LENGTH_LONG).show();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.main);

		this.preferences = PreferenceManager.getDefaultSharedPreferences(this);
		// display changelog?
		String v0 = this.preferences.getString(PREFS_LAST_RUN, "");
		String v1 = this.getString(R.string.app_version);
		if (!v0.equals(v1)) {
			SharedPreferences.Editor editor = this.preferences.edit();
			editor.putString(PREFS_LAST_RUN, v1);
			editor.commit();
			this.showDialog(DIALOG_UPDATE);
		}

		((EditText) this.findViewById(R.id.port)).setText(this.preferences
				.getString(Proxy.PREFS_PORT, "8080"));
		String f = this.preferences.getString(Proxy.PREFS_FILTER,
				this.getString(R.string.default_filter));
		for (String s : f.split("\n")) {
			if (s.length() > 0) {
				this.filter.add(s);
			}
		}
		this.importUrl = this.preferences.getString(PREFS_IMPORT_URL, "");

		((Button) this.findViewById(R.id.start_service))
				.setOnClickListener(this);
		((Button) this.findViewById(R.id.stop_service))
				.setOnClickListener(this);
		((Button) this.findViewById(R.id.filter_add_)).setOnClickListener(this);
		((Button) this.findViewById(R.id.export_log)).setOnClickListener(this);
		((Button) this.findViewById(R.id.start_log)).setOnClickListener(this);
		((Button) this.findViewById(R.id.stop_log)).setOnClickListener(this);
		((Button) this.findViewById(R.id.export_log)).setOnClickListener(this);

		ListView lv = (ListView) this.findViewById(R.id.filter);
		this.adapter = new ArrayAdapter<String>(this,
				R.layout.simple_list_item_1, this.filter);
		lv.setAdapter(this.adapter);
		lv.setTextFilterEnabled(true);
		lv.setOnItemClickListener(this);

		ListView lvWebTraffic = (ListView) this
				.findViewById(R.id.lv_web_traffic);

		this.adapter2 = new ArrayAdapter<String>(this,
				R.layout.simple_list_item_2, this.filter2);

		lvWebTraffic.setAdapter(this.adapter2);

		this.enableModifier();

	}

	/** Save Preferences. */
	private void savePreferences() {
		SharedPreferences.Editor editor = this.preferences.edit();
		editor.putString(Proxy.PREFS_PORT,
				((EditText) this.findViewById(R.id.port)).getText().toString());
		StringBuilder sb = new StringBuilder();
		for (String s : this.filter) {
			sb.append(s + "\n");
		}
		editor.putString(Proxy.PREFS_FILTER, sb.toString());
		editor.putString(PREFS_IMPORT_URL, this.importUrl);

		editor.commit();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onPause() {
		super.onPause();
		this.savePreferences();
	}

	@Override
	public final void onStart() {
		super.onStart();
		this.enableModifier();
	}

	/**
	 * {@inheritDoc}
	 */
	//
	@Override
	public final void onResume() {
		super.onResume();
		this.enableModifier();

	}

	public final void enableModifier() {

		if (ENABLE == true) {
			this.filter2.clear();
			if (Proxy.urlArrayList.isEmpty() == true) {
				this.filter2.add("No traffic has been detected");
			}
			this.filter2.addAll(Proxy.urlArrayList);
			this.adapter2.notifyDataSetChanged();
		}

		if (ENABLE == false) {
			this.filter2.clear();
			this.filter2.add("Log is currently disabled");
			this.adapter2.notifyDataSetChanged();
		}
	}

	@Override
	public final void onClick(final View v) {
		switch (v.getId()) {
		case R.id.start_service:
			this.savePreferences();
			this.startService(new Intent(this, Proxy.class));

			break;
		//
		case R.id.start_log:

			if (ENABLE == false) {
				this.filter2.clear();
				if (Proxy.urlArrayList.isEmpty() == true) {
					this.filter2.add("No traffic has been detected");

				}
			}
			this.adapter2.notifyDataSetChanged();
			ENABLE = true;
			Toast.makeText(this, "Log has started tracking", Toast.LENGTH_LONG)
					.show();
			break;

		case R.id.stop_log:

			this.filter2.clear();
			this.filter2.add("Log is currently disabled");
			this.adapter2.notifyDataSetChanged();
			Proxy.urlArrayList.clear();
			ENABLE = false;
			Toast.makeText(this,
					"Log has stopped tracking. All traffic has been reset",
					Toast.LENGTH_LONG).show();
			break;

		case R.id.export_log:
			if (Proxy.urlArrayList.isEmpty() == true) {
				Toast.makeText(this, "Cannot export empty log",
						Toast.LENGTH_LONG).show();
				break;
			}

			final Intent emailIntent = new Intent(
					android.content.Intent.ACTION_SEND);
			emailIntent.setType("plain/text");
			emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
					new String[] { "robert@famigo.com" });
			emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
					"Export Traffic Log");

			String z = Proxy.urlArrayListFormat.toString();
			z = z.replaceAll("zxcv,", "\n");
			z = z.substring(0, z.length() - 5);
			z = z.substring(1, z.length());
			z = z.replaceAll(" ", "");

			emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, z);
			AdBlock.this.startActivity(Intent.createChooser(emailIntent,
					"Send mail..."));

			break;
		case R.id.stop_service:
			this.stopService(new Intent(this, Proxy.class));
		case R.id.filter_add_:
			EditText et = (EditText) this.findViewById(R.id.filter_add);
			String f = et.getText().toString();
			if (f.length() > 0) {
				if (this.itemToEdit >= 0) {
					this.filter.remove(this.itemToEdit);
					this.itemToEdit = -1;
				}
				this.filter.add(f);
				et.setText("");
				this.adapter.notifyDataSetChanged();
			}
			break;
		case R.id.cancel:
			this.dismissDialog(DIALOG_IMPORT);
			break;
		case R.id.ok:
			this.dismissDialog(DIALOG_IMPORT);
			this.importUrl = ((EditText) v.getRootView().findViewById(
					R.id.import_url)).getText().toString();
			new Importer().execute((String[]) null);
			break;
		default:
			break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = this.getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_about: // start about dialog
			this.showDialog(DIALOG_ABOUT);
			return true;
		case R.id.item_import:
			this.showDialog(DIALOG_IMPORT);
			return true;
		case R.id.item_donate:
			try {
				this.startActivity(new Intent(Intent.ACTION_VIEW, Uri
						.parse(this.getString(R.string.donate_url))));
			} catch (ActivityNotFoundException e) {
				Log.e(TAG, "no browser", e);
			}
			return true;

		case R.id.item_more:
			try {
				this.startActivity(new Intent(Intent.ACTION_VIEW, Uri
						.parse("market://search?q=pub:\"Felix Bechstein\"")));
			} catch (ActivityNotFoundException e) {
				Log.e(TAG, "no market", e);
			}
			return true;

		default:
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final Dialog onCreateDialog(final int id) {
		Dialog d;
		switch (id) {
		case DIALOG_ABOUT:
			d = new Dialog(this);
			d.setContentView(R.layout.about);
			d.setTitle(this.getString(R.string.about_) + " v"
					+ this.getString(R.string.app_version));
			return d;
		case DIALOG_IMPORT:
			d = new Dialog(this);
			d.setContentView(R.layout.import_url);
			d.setTitle(this.getString(R.string.import_url_));
			((Button) d.findViewById(R.id.ok)).setOnClickListener(this);
			((Button) d.findViewById(R.id.cancel)).setOnClickListener(this);
			return d;
		case DIALOG_UPDATE:
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.changelog_);
			final String[] changes = this.getResources().getStringArray(
					R.array.updates);
			final StringBuilder buf = new StringBuilder(changes[0]);
			for (int i = 1; i < changes.length; i++) {
				buf.append("\n\n");
				buf.append(changes[i]);
			}
			builder.setIcon(android.R.drawable.ic_menu_info_details);
			builder.setMessage(buf.toString());
			builder.setCancelable(true);
			builder.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int id) {
							dialog.cancel();
						}
					});
			return builder.create();
		default:
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onPrepareDialog(final int id, final Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		switch (id) {
		case DIALOG_IMPORT:
			((EditText) dialog.findViewById(R.id.import_url))
					.setText(this.importUrl);
			break;
		default:
			break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onItemClick(final AdapterView<?> parent, final View v,
			final int position, final long id) {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setItems(
				this.getResources().getStringArray(R.array.itemDialog),
				new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog,
							final int item) {
						switch (item) {
						case ITEM_DIALOG_EDIT:
							AdBlock.this.itemToEdit = position;
							((EditText) AdBlock.this
									.findViewById(R.id.filter_add))
									.setText(AdBlock.this.adapter
											.getItem(position));
							break;
						case ITEM_DIALOG_DELETE:
							AdBlock.this.filter.remove(position);
							AdBlock.this.adapter.notifyDataSetChanged();
							break;
						default:
							break;
						}
					}
				});
		AlertDialog alert = builder.create();
		alert.show();

	}

}
