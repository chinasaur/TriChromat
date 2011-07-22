package li.peterandpatty.colanomalous;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

class PreviewSurface extends SurfaceView implements SurfaceHolder.Callback {	
	private final SurfaceHolder mHolder;
	private Camera mCamera;
	private final ProcessedView mProcessedView;
	
	PreviewSurface(Context context, ProcessedView processedView) {
		super(context);

		// Install a SurfaceHolder.Callback so we get notified when the underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		mProcessedView = processedView;
	}


	private List<Size> supportedPreviewSizes;
	List<Size> getSupportedPreviewSizes() { return supportedPreviewSizes; }	
	
	private List<String> supportedFocusModes;
	List<String> getSupportedFocusModes() { return supportedFocusModes; }
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// The Surface has been created, acquire the camera and tell it where to draw.
		mCamera = Camera.open();
		try {
			// This needs to be available to the preview size menu
			Camera.Parameters parameters = mCamera.getParameters();
			supportedPreviewSizes = parameters.getSupportedPreviewSizes();
			supportedFocusModes = parameters.getSupportedFocusModes();

			mCamera.setPreviewDisplay(holder);
		} catch (IOException exception) {
			mCamera.release();
			mCamera = null;
		}
	}

	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// Surface will be destroyed when we return, so stop the preview.
		// Because the CameraDevice object is not a shared resource, it's very
		// important to release it when the activity is paused.
		mCamera.setPreviewCallback(null);
		mCamera.stopPreview();
		mCamera.release();
		mCamera = null;
	}


	// Apparently I can't create a new Size() without a camera instance to say mCamera.new Size().
	// I don't get this.  But the upshot is that it's more convenient to store width and height than a Size object.
	// This way width and height can be set before the camera is created in surfaceCreated.
	private int previewWidth = 0, previewHeight = 0;
	void setPreviewSize(Size previewSize) { setPreviewSize(previewSize.width, previewSize.height); }
	void setPreviewSize(int width, int height) {
		previewWidth  = width;
		previewHeight = height;
	}
	int getPreviewWidth()  { return previewWidth; }
	int getPreviewHeight() { return previewHeight; }

	private String focusMode;
	void setFocusMode(String mode) { focusMode = mode; }
	String getFocusMode() { return focusMode; }
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		mCamera.setPreviewCallback(null);
		mCamera.stopPreview();

		Camera.Parameters mParameters = mCamera.getParameters();		
		
		// Check previewWidth and previewHeight, if invalid just use what the camera wanted
		if (previewWidth == 0 || previewHeight == 0)        setPreviewSize(mParameters.getPreviewSize());
		if (!validPreviewSize(previewWidth, previewHeight)) setPreviewSize(mParameters.getPreviewSize());

		// Check focusMode, if invalid just use what the camera wanted
		if (focusMode == null)          setFocusMode(mParameters.getFocusMode());
		if (!validFocusMode(focusMode)) setFocusMode(mParameters.getFocusMode());		
		
		// Set preview size, focus mode, notify camera
		mParameters.setPreviewSize(previewWidth, previewHeight);
		mParameters.setFocusMode(focusMode);
		mCamera.setParameters(mParameters);
		
		// Reset ProcessedView, notify of preview size, set callback and start camera!
		mProcessedView.reset(mParameters.getPreviewSize());
		mCamera.setPreviewCallback(mProcessedView);
		mCamera.startPreview();
	}
	
	boolean canAutoFocus() {
		return focusMode.equals(Camera.Parameters.FOCUS_MODE_AUTO) || focusMode.equals(Camera.Parameters.FOCUS_MODE_MACRO);
	}
	
	private boolean validPreviewSize(int width, int height) {
		for (Size sps : supportedPreviewSizes)
			if (sps.width == width && sps.height == height) return true;
		return false;
	}
	
	private boolean validFocusMode(String mode) {
		for (String m : supportedFocusModes)
			if (m.equals(mode)) return true;
		return false;
	}
	
	int currentPreviewSizeIndex() {
		for (int i = 0; i < supportedPreviewSizes.size(); i++) {
			Size sps = supportedPreviewSizes.get(i);
			if (sps.width == previewWidth && sps.height == previewHeight) return i;
		}
		throw new Error("This should never happen");
	}
	
	int currentFocusModeIndex() {
		for (int i = 0; i < supportedFocusModes.size(); i++)
			if (supportedFocusModes.get(i).equals(focusMode)) return i;
		throw new Error("This should never happen");
	}
	
	void autoFocus(AutoFocusCallback cb) {
		mCamera.autoFocus(cb);
	}
}
