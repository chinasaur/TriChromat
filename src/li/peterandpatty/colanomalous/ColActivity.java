package li.peterandpatty.colanomalous;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import java.io.IOException;

public class ColActivity extends Activity {	
	
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		ProcessedView  mProcessedView  = new ProcessedView(this); 
		mProcessedView.setYUVProcessor(new RGB2MG());
		PreviewSurface mPreviewSurface = new PreviewSurface(this, mProcessedView);

		setContentView(R.layout.main);
		FrameLayout mFrame = (FrameLayout) findViewById(R.id.frame);		
		mFrame.addView(mPreviewSurface);
		mFrame.addView(mProcessedView);
	}
}

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

	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// The Surface has been created, acquire the camera and tell it where to draw.
		mCamera = Camera.open();
		try {
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

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		mCamera.setPreviewCallback(null);
		mCamera.stopPreview();
		
		// Now that the size is known, set up the camera parameters and begin the preview.
		Camera.Parameters mParameters = mCamera.getParameters();

		// This needs to be fixed to handle landscape orientation correctly.
		// Also would be nice to give user control.
//		List<Size> sizes = mParameters.getSupportedPreviewSizes();
//		if (sizes != null && !sizes.isEmpty()) {
//			Size optimalSize = getOptimalPreviewSize(sizes, w, h);
//			mParameters.setPreviewSize(optimalSize.width, optimalSize.height);
//		}
		Size previewImageSize = mParameters.getPreviewSize();

		mProcessedView.setPreviewImageSize(previewImageSize);
		
//		mCamera.setParameters(mParameters);
		mCamera.setPreviewCallback(mProcessedView);
		mCamera.startPreview();
	}
	
//	private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
//		final double ASPECT_TOLERANCE = 0.05;
//		double targetRatio = (double) w / h;
//		if (sizes == null)
//			return null;
//
//		Size optimalSize = null;
//		double minDiff = Double.MAX_VALUE;
//
//		int targetHeight = h / 10;
//
//		// Try to find an size match aspect ratio and size
//		for (Size size : sizes) {
//			double ratio = (double) size.width / size.height;
//			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
//				continue;
//			if (Math.abs(size.height - targetHeight) < minDiff) {
//				optimalSize = size;
//				minDiff = Math.abs(size.height - targetHeight);
//			}
//		}
//
//		// Cannot find the one match the aspect ratio, ignore the requirement
//		if (optimalSize == null) {
//			minDiff = Double.MAX_VALUE;
//			for (Size size : sizes) {
//				if (Math.abs(size.height - targetHeight) < minDiff) {
//					optimalSize = size;
//					minDiff = Math.abs(size.height - targetHeight);
//				}
//			}
//		}
//		return optimalSize;
//	}

}


class ProcessedView extends View implements Camera.PreviewCallback {
	private boolean processing;
	private byte[] data;
	private int[] rgb;
	private final Paint mPaint;

	public ProcessedView(Context context) {
		super(context);
		processing = false;
		rgb = new int[0];
		data = new byte[0];
		mPaint = new Paint();
	}

	private YUVProcessor mYUVProcessor;
	public void setYUVProcessor(YUVProcessor yuvProcessor) {
		mYUVProcessor = yuvProcessor;
	}

	// Sent over from PreviewSurface whenever the size is changed; could be pulled from camera in onPreviewFrame, but 
	// that would be more frequent checking than necessary.
	private Size previewImageSize;
	public void setPreviewImageSize(Size previewImageSize) {
		this.previewImageSize = previewImageSize;
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {		
		loadNV21(data);
	}

	public void loadNV21(byte[] bytearray) {
		if (processing) return;
		processing = true;

		if (data.length != bytearray.length) data = new byte[bytearray.length];
		System.arraycopy(bytearray, 0, data, 0, bytearray.length);

		invalidate();
	}


	static final boolean showFPS = true;
	private Bitmap bmp;
	private long lastFrame = System.currentTimeMillis();

	@Override
	protected void onDraw(Canvas canvas) {
		if (data.length == 0) return;

		int w = previewImageSize.width;
		int h = previewImageSize.height;

		if (rgb.length != w*h) rgb = new int[w*h];
		mYUVProcessor.processYUV420SP(rgb, data, w, h);
		bmp = Bitmap.createBitmap(rgb, w, h, Bitmap.Config.RGB_565);
		canvas.drawBitmap(bmp, 0, 0, null);

		if (showFPS) {
			canvas.drawText(Double.toString(1000 / (System.currentTimeMillis() - lastFrame)) + " FPS", 50, 50, mPaint);
			lastFrame = System.currentTimeMillis();
		}

		processing = false;
	}
}