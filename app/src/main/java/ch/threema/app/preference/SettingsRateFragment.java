/*  _____ _
 * |_   _| |_  _ _ ___ ___ _ __  __ _
 *   | | | ' \| '_/ -_) -_) '  \/ _` |_
 *   |_| |_||_|_| \___\___|_|_|_\__,_(_)
 *
 * Threema for Android
 * Copyright (c) 2019-2024 Threema GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.threema.app.preference;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import org.slf4j.Logger;

import androidx.annotation.NonNull;
import ch.threema.app.BuildConfig;
import ch.threema.app.BuildFlavor;
import ch.threema.app.R;
import ch.threema.app.dialogs.GenericAlertDialog;
import ch.threema.app.dialogs.RateDialog;
import ch.threema.base.utils.LoggingUtil;

import static ch.threema.app.ThreemaApplication.getAppContext;

public class SettingsRateFragment extends ThreemaPreferenceFragment implements RateDialog.RateDialogClickListener, GenericAlertDialog.DialogClickListener {

	private static final Logger logger = LoggingUtil.getThreemaLogger("SettingsRateFragment");

	private static final String DIALOG_TAG_RATE = "rate";
	private static final String DIALOG_TAG_RATE_ON_GOOGLE_PLAY = "ratep";

	@Override
	protected void initializePreferences() {
		RateDialog rateDialog = RateDialog.newInstance(getString(R.string.rate_title));
		rateDialog.setTargetFragment(SettingsRateFragment.this, 0);
		rateDialog.show(getParentFragmentManager(), DIALOG_TAG_RATE);
	}

	private boolean startRating(Uri uri) throws ActivityNotFoundException {
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
		try {
			startActivity(intent);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	@Override
	public void onYes(String tag, final int rating, final String text) {
		if (rating >= 4 && shouldRedirectToGooglePlay(BuildFlavor.getLicenseType())) {
			GenericAlertDialog dialog = GenericAlertDialog.newInstance(R.string.rate_title,
					getString(R.string.rate_thank_you) + " " +
							getString(R.string.rate_forward_to_play_store) ,
					R.string.yes,
					R.string.no);
			dialog.setTargetFragment(this);
			dialog.show(getParentFragmentManager(), DIALOG_TAG_RATE_ON_GOOGLE_PLAY);
		} else {
			Toast.makeText(getAppContext(), getString(R.string.rate_thank_you), Toast.LENGTH_LONG).show();
			onBackPressed();
		}
	}

	@Override
	public void onYes(String tag, Object data) {
		if (DIALOG_TAG_RATE_ON_GOOGLE_PLAY.equals(tag)) {
			if (!startRating(Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID))) {
				startRating(Uri.parse("https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID));
			}
			onBackPressed();
		}
	}

	@Override
	public void onNo(String tag, Object data) {
		onBackPressed();
	}

	@Override
	public void onCancel(String tag) {
		onBackPressed();
	}

	@Override
	public int getPreferenceTitleResource() {
		return R.string.rate_title;
	}

	@Override
	public int getPreferenceResource() {
		return R.xml.preference_rate;
	}

	private void onBackPressed() {
		if (getActivity() != null) {
			getActivity().onBackPressed();
		}
	}

	private boolean shouldRedirectToGooglePlay(@NonNull BuildFlavor.LicenseType licenseType) {
		switch (licenseType) {
			case GOOGLE:
			case NONE:
				return true;
			case GOOGLE_WORK:
				return isInstalledFromPlayStore();
			default:
				return false;
		}
	}

	private boolean isInstalledFromPlayStore() {
		Context context = getAppContext();
		if (context == null) {
			logger.warn("Could not get app context.");
			return false;
		}

		try {
			String installerPackageName;
			PackageManager packageManager = context.getPackageManager();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				installerPackageName = packageManager.getInstallSourceInfo(context.getPackageName()).getInstallingPackageName();
			} else {
				installerPackageName = packageManager.getInstallerPackageName(context.getPackageName());
			}

			return "com.android.vending".equals(installerPackageName);
		} catch (Exception e) {
			logger.error("Could not determine package source", e);
			return false;
		}
	}
}
