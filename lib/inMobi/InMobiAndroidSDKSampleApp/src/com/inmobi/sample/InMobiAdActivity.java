package com.inmobi.sample;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.inmobi.androidsdk.IMAdInterstitial;
import com.inmobi.androidsdk.IMAdInterstitial.State;
import com.inmobi.androidsdk.IMAdInterstitialListener;
import com.inmobi.androidsdk.IMAdListener;
import com.inmobi.androidsdk.IMAdRequest;
import com.inmobi.androidsdk.IMAdRequest.ErrorCode;
import com.inmobi.androidsdk.IMAdView;
import com.inmobi.androidsdk.impl.Constants;

public class InMobiAdActivity extends Activity {

	private IMAdView mIMAdView;
	private Button mBtnGetIntAd;
	private Button mBtnShowIntAd;
	private IMAdInterstitial mIMAdInterstitial;
	private IMAdRequest mAdRequest;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		// Get the IMAdView instance
		mIMAdView = (IMAdView) findViewById(R.id.imAdview);

		// set the test mode to true (Make sure you set the test mode to false
		// when distributing to the users)
		mAdRequest = new IMAdRequest();
		mAdRequest.setTestMode(true);
		mIMAdView.setIMAdRequest(mAdRequest);

		// set the listener if the app has to know ad status notifications
		mIMAdView.setIMAdListener(mIMAdListener);

		mBtnGetIntAd = (Button) findViewById(R.id.btnGetIntAd);
		mBtnShowIntAd = (Button) findViewById(R.id.btnShowIntAd);
		mBtnShowIntAd.setEnabled(false);

		// initialize an interstitial ad
		mIMAdInterstitial = new IMAdInterstitial(this,
				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");

		// set the listener if the app has to know ad status notifications
		mIMAdInterstitial.setImAdInterstitialListener(mIMAdInListener);

	}

	public void onRefreshAd(View view) {

		mIMAdView.loadNewAd();
	}

	public void onGetInAd(View view) {

		mIMAdInterstitial.loadNewAd(mAdRequest);
	}

	public void onShowInAd(View view) {

		if (mIMAdInterstitial.getState() == State.READY) {
			mIMAdInterstitial.show();
			mBtnGetIntAd.setEnabled(true);
			mBtnShowIntAd.setEnabled(false);
		}
	}

	private IMAdListener mIMAdListener = new IMAdListener() {

		@Override
		public void onShowAdScreen(IMAdView adView) {
			Log.i(Constants.LOGGING_TAG,
					"InMobiAdActivity-> onShowAdScreen, adView: " + adView);

		}

		@Override
		public void onDismissAdScreen(IMAdView adView) {
			Log.i(Constants.LOGGING_TAG,
					"InMobiAdActivity-> onDismissAdScreen, adView: " + adView);
		}

		@Override
		public void onAdRequestFailed(IMAdView adView, ErrorCode errorCode) {
			Log.i(Constants.LOGGING_TAG,
					"InMobiAdActivity-> onAdRequestFailed, adView: " + adView
							+ " ,errorCode: " + errorCode);
			Toast.makeText(
					InMobiAdActivity.this,
					"Ad failed to load. Check the logcat for logs. Errorcode: "
							+ errorCode, Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onAdRequestCompleted(IMAdView adView) {
			Log.i(Constants.LOGGING_TAG,
					"InMobiAdActivity-> onAdRequestCompleted, adView: "
							+ adView);
		}
	};

	private IMAdInterstitialListener mIMAdInListener = new IMAdInterstitialListener() {

		@Override
		public void onShowAdScreen(IMAdInterstitial adInterstitial) {
			Log.i(Constants.LOGGING_TAG,
					"InMobiAdActivity-> onShowAdScreen, adInterstitial: "
							+ adInterstitial);
		}

		@Override
		public void onDismissAdScreen(IMAdInterstitial adInterstitial) {
			Log.i(Constants.LOGGING_TAG,
					"InMobiAdActivity-> onDismissAdScreen, adInterstitial: "
							+ adInterstitial);

		}

		@Override
		public void onAdRequestFailed(IMAdInterstitial adInterstitial,
				ErrorCode errorCode) {
			Log.i(Constants.LOGGING_TAG,
					"InMobiAdActivity-> onAdRequestFailed, adInterstitial: "
							+ adInterstitial + " ,errorCode: " + errorCode);
			Toast.makeText(InMobiAdActivity.this,
					"Interstitial Ad failed to load. Errorcode: " + errorCode,
					Toast.LENGTH_SHORT).show();
			mBtnGetIntAd.setEnabled(true);
			mBtnShowIntAd.setEnabled(false);
		}

		@Override
		public void onAdRequestLoaded(IMAdInterstitial adInterstitial) {
			Log.i(Constants.LOGGING_TAG,
					"InMobiAdActivity-> onAdRequestLoaded, adInterstitial: "
							+ adInterstitial);
			Toast.makeText(InMobiAdActivity.this, "Interstitial Ad Loaded.",
					Toast.LENGTH_SHORT).show();
			mBtnGetIntAd.setEnabled(false);
			mBtnShowIntAd.setEnabled(true);
		}
	};

}
