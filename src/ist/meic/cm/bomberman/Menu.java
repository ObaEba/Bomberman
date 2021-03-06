package ist.meic.cm.bomberman;

import ist.meic.cm.bomberman.p2p.WiFiServiceDiscoveryActivity;
import ist.meic.cm.bomberman.settings.SettingsActivity;
import ist.meic.cm.bomberman.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class Menu extends Activity {
	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean TOGGLE_ON_CLICK = true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;

	private MediaPlayer player;

	private BackgroundSound mBackgroundSound;

	private Button exit;

	private final static int SINGLE = 0;
	private final static int CENTRALIZED = 1;
	private final static int DECENTRALIZED = 2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		setContentView(R.layout.activity_menu);

		exit = (Button) findViewById(R.id.Exit);
		exit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_MAIN);
				intent.addCategory(Intent.CATEGORY_HOME);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);

			}
		});
		Button settings = (Button) findViewById(R.id.settings);
		settings.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Menu.this, SettingsActivity.class);
				startActivity(intent);
			}
		});

		Button newGame = (Button) findViewById(R.id.NewGame);
		newGame.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				new AlertDialog.Builder(Menu.this)
						.setTitle("Game Mode")
						.setMessage("Please Select the Game Mode.")
						.setNeutralButton("Multi Player",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
										if (!isWifiOn()) {
											Toast.makeText(Menu.this,
													"Wifi must be ON!",
													Toast.LENGTH_SHORT).show();
										} else
											whichMultiplayer();

									}
								})
						.setPositiveButton("Single Player",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
										askForName(SINGLE, InGame.class);
									}

								})
						.setNegativeButton(android.R.string.no,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
										// do nothing
									}
								}).setIcon(R.drawable.ic_launcher).show();
			}

		});

		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		final View contentView = findViewById(R.id.imageView1);

		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, contentView,
				HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider
				.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
					// Cached values.
					int mControlsHeight;
					int mShortAnimTime;

					@Override
					@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
					public void onVisibilityChange(boolean visible) {
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
							// If the ViewPropertyAnimator API is available
							// (Honeycomb MR2 and later), use it to animate the
							// in-layout UI controls at the bottom of the
							// screen.
							if (mControlsHeight == 0) {
								mControlsHeight = controlsView.getHeight();
							}
							if (mShortAnimTime == 0) {
								mShortAnimTime = getResources().getInteger(
										android.R.integer.config_shortAnimTime);
							}
							controlsView
									.animate()
									.translationY(visible ? 0 : mControlsHeight)
									.setDuration(mShortAnimTime);
						} else {
							// If the ViewPropertyAnimator APIs aren't
							// available, simply show or hide the in-layout UI
							// controls.
							controlsView.setVisibility(visible ? View.VISIBLE
									: View.GONE);
						}

						if (visible && AUTO_HIDE) {
							// Schedule a hide().
							delayedHide(AUTO_HIDE_DELAY_MILLIS);
						}
					}
				});

		// Set up the user interaction to manually show or hide the system UI.
		contentView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (TOGGLE_ON_CLICK) {
					mSystemUiHider.toggle();
				} else {
					mSystemUiHider.show();
				}
			}
		});

		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		findViewById(R.id.NewGame).setOnTouchListener(mDelayHideTouchListener);

		findViewById(R.id.Exit).setOnTouchListener(mDelayHideTouchListener);
	}

	private void whichMultiplayer() {
		new AlertDialog.Builder(Menu.this)
				.setTitle("Multiplayer Mode")
				.setMessage("Please Select the Multiplayer Mode.")
				.setNeutralButton("Centralized",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								if (connected())
									askForName(CENTRALIZED, InGame.class);
								else
									Toast.makeText(Menu.this,
											"You must connected to a Network!",
											Toast.LENGTH_SHORT).show();
							}
						})
				.setPositiveButton("Decentralized",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								askForName(DECENTRALIZED,
										WiFiServiceDiscoveryActivity.class);
							}

						})
				.setNegativeButton(android.R.string.no,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								// do nothing
							}
						}).setIcon(R.drawable.ic_launcher).show();
	}

	private boolean connected() {
		try {
			ConnectivityManager connectivityManager = (ConnectivityManager) Menu.this
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo wifiInfo = connectivityManager
					.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

			if (wifiInfo.isConnected()) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;

	}

	public boolean isWifiOn() {
		WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		return wifi.isWifiEnabled();
	}

	@Override
	public void onResume() {
		super.onResume();
		mBackgroundSound = new BackgroundSound();
		mBackgroundSound.execute();
	}

	@Override
	public void onPause() {
		super.onPause();
		mBackgroundSound.cancel(true);
		if (player != null) {
			player.stop();
			if (isFinishing()) {
				player.stop();
				player.release();
			}
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide(1000);
	}

	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			if (AUTO_HIDE) {
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
			}
			return false;
		}
	};

	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			mSystemUiHider.hide();
		}
	};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}

	private void askForName(final int mode,
			final Class<? extends Activity> activity) {

		final AlertDialog.Builder alert = new AlertDialog.Builder(Menu.this)
				.setTitle("Insert Player Name:");
		final EditText input = new EditText(this);
		input.setHint("Player");
		alert.setView(input);

		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = "Player";
				String tmp = input.getText().toString().trim();
				if (!tmp.equals(""))
					value = tmp;
				Intent intent = new Intent(Menu.this, activity);
				intent.putExtra("player_name", value);
				if (mode == SINGLE) {
					// singlePlayer - mode 0
					intent.putExtra("game_mode", "singleplayer");
				} else if (mode == CENTRALIZED) {
					// multiplayer centralized - mode 1
					intent.putExtra("game_mode", "multiplayer");
				} else if (mode == DECENTRALIZED) {
					intent.putExtra("game_mode", "multiplayerD");
				}

				startActivity(intent);
			}
		});

		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.cancel();
					}
				});
		alert.show();
	}

	private class BackgroundSound extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {

			player = MediaPlayer.create(Menu.this, R.raw.theme);
			player.setLooping(true); // Set looping
			player.setVolume(100, 100);
			player.start();
			return null;
		}
	}

	@Override
	public void onBackPressed() {
		exit.performClick();
	}
}
