package at.android.simple.ledlight;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import at.simple.ledlight.R;

/**
 * 
 * @author Martin Schliefellner
 * 
 */
public class MainActivity extends Activity implements SurfaceHolder.Callback{

	private Camera _camera;
	private Button _button;
	private boolean _isOn = false;
	private boolean _hasFlashLight = true;
	private boolean _isFallBack = false;
	//private int _sdkVersion = 0;
	private SurfaceHolder _surfaceHolder;
	private SurfaceView _preview;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//this._sdkVersion = Integer.valueOf(android.os.Build.VERSION.SDK_INT);
		this._hasFlashLight = this.hasFlash() && getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
		//this._hasFlashLight = false;
		
		if (this._hasFlashLight == false) {
			this.fallBack(R.string.no_flashlight);
		}
		else {
			this.setFlashLightMode();
			// Turn on Light
			this.turnOnFlashLight();
		}
	}
	
	private void setFlashLightMode() {
		this._isFallBack = false;
		setContentView(R.layout.activity_main);
		this._button = (Button) findViewById(R.id.lightButton);
		this._button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (v instanceof Button) {
					Button button = (Button) v;
					if (button.getText().equals(getString(R.string.off))) {
						turnOffFlashLight();
					}
					else {
						turnOnFlashLight();
					}
				}
			}
		});
		if (this._camera == null) {
			this._camera = Camera.open();		
		}
		
		this._preview = (SurfaceView) findViewById(R.id.preview);
		this._surfaceHolder = this._preview.getHolder();
		this._surfaceHolder.addCallback(this);
		try {
			this._camera.setPreviewDisplay(this._surfaceHolder);
		} catch (IOException e) {
			e.printStackTrace();
			this.fallBack(R.string.problem_torchmode);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (this._hasFlashLight && this._isOn) {
			this.turnOffFlashLight();
		}
		if (this._isFallBack == false && this._surfaceHolder!= null) {
			_surfaceHolder.removeCallback(this);
		}
		this.releaseCamera();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (this._isFallBack == false) {
			if (this._camera == null) {
				this._camera = Camera.open();
				try {
					_surfaceHolder.addCallback(this);
					this._camera.setPreviewDisplay(_surfaceHolder);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}			
			if (this._camera == null) {
				this.fallBack(R.string.no_access_cam);
			}		
		}
	}
	
	/**
	 * Release the camera if application is paused or destroyed
	 */
	private void releaseCamera() {
		if (this._camera != null) {
			this._camera.stopPreview();
			this._camera.release();
			this._camera = null;
		}
	}
	
	/**
	 * This function checks if flash is supported 
	 * E.g. getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH) is not enough for nexus7 2013
	 * @return True if supported, else false
	 */
	private boolean hasFlash() {
		if (this._camera == null) {
			this._camera = Camera.open();
		}
		if (this._camera == null) {
			return false;
		}
		// Check if torch mode is supported
		Parameters parameters = this._camera.getParameters();
		if (parameters.getFlashMode() == null) {
			return false;
		}
		List<String> supportedFlashModes = parameters.getSupportedFlashModes();
		if (supportedFlashModes == null || supportedFlashModes.isEmpty() || 
				supportedFlashModes.size() == 1 && supportedFlashModes.get(0).equals(Camera.Parameters.FLASH_MODE_OFF)) {
			return false;
		}
		return true;
	}
	
	/**
	 * This function turns on the flash light if supported
	 */
	private void turnOnFlashLight() {
		try {
			// This check is for initial start
			if (this._camera == null) {
				this._camera = Camera.open();
				// if Camera.open didn't work
				if (this._camera == null) {
					this.fallBack(R.string.no_access_cam);
				}
				else {
					this._camera.setPreviewDisplay(_surfaceHolder);
				}
			}
			Parameters parameters = this._camera.getParameters();			
			parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
			this._camera.setParameters(parameters);
			this._camera.startPreview();
			this._isOn = true;
			_button.setText(R.string.off);
		}
		catch (Exception e) {
			e.printStackTrace();
			if (this._hasFlashLight && this._isOn) {
				this.turnOffFlashLight();
			}			
			this.releaseCamera();
			this.fallBack(R.string.problem_torchmode);
		}
	}
	
	/**
	 * This function turns off the flash light
	 */
	private void turnOffFlashLight() {
		if (this._camera != null) {
			Parameters parameters = this._camera.getParameters();
			parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
			this._camera.setParameters(parameters);
			this._camera.stopPreview();
			this._isOn = false;
			this._button.setText(R.string.on);
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		this.releaseCamera();
	}
	
	/**
	 * This function is called if no camera exists or a problem occurs e.g. in Torch-Mode
	 */
	private void fallBack(int messageID) {
		this._isFallBack = true;
		Toast toast = Toast.makeText(getApplicationContext(), messageID, Toast.LENGTH_SHORT);
		toast.show();
		if (this._surfaceHolder != null) {
			this._surfaceHolder.removeCallback(this);
		}
		setContentView(R.layout.activity_main_whitescreen);		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		if (this._hasFlashLight) {
			MenuInflater menuInflater = getMenuInflater();
			if (this._hasFlashLight) {
				menuInflater.inflate(R.layout.activity_main_menu, menu);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
			case R.id.menu_toggle_mode:
				if (this._isFallBack == false) {
					if (this._hasFlashLight && this._isOn) {
						this.turnOffFlashLight();
					}
					this.releaseCamera();
					this.fallBack(R.string.toggleScreenMode);
				}
				else {
					this.setFlashLightMode();	
					Toast toast = Toast.makeText(getApplicationContext(), R.string.toggleScreenMode, Toast.LENGTH_SHORT);
					toast.show();
					this._button.setText(R.string.on);
				}
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	}

	public void surfaceCreated(SurfaceHolder holder) {
		this._surfaceHolder = holder;
		try {
			this._camera.setPreviewDisplay(this._surfaceHolder);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		this._camera.stopPreview();
		this._surfaceHolder = null;
	}

}
