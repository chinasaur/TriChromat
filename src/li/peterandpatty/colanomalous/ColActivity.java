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
import java.util.List;

public class ColActivity extends Activity {	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		FrameLayout mFrame = (FrameLayout) findViewById(R.id.frame);
		
		PreviewSurface mPreview = new PreviewSurface(this);
		mFrame.addView(mPreview);
		mFrame.addView(mPreview.mPaintView);
	}
}

class PreviewSurface extends SurfaceView implements SurfaceHolder.Callback {
	private static final String TAG = "PreviewSurface";
	
	private SurfaceHolder mHolder;
	private Camera mCamera;
	PreviewProcessor mPaintView;
	private Size previewImageSize;
	private int surfaceWidth;
	private int surfaceHeight;

	PreviewSurface(Context context) {
		super(context);

		mPaintView = new PreviewProcessor(context);

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// The Surface has been created, acquire the camera and tell it where
		// to draw.
		mCamera = Camera.open();
		try {
			mCamera.setPreviewDisplay(holder);
		} catch (IOException exception) {
			mCamera.release();
			mCamera = null;
			// TODO: add more exception handling logic here
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
		
		surfaceWidth  = w;
		surfaceHeight = h;

		// Now that the size is known, set up the camera parameters and begin
		// the preview.
		Camera.Parameters mParameters = mCamera.getParameters();

		// This needs to be fixed to handle landscape orientation correctly.
		// Also would be nice to give user control.
//		List<Size> sizes = mParameters.getSupportedPreviewSizes();
//		if (sizes != null && !sizes.isEmpty()) {
//			Size optimalSize = getOptimalPreviewSize(sizes, w, h);
//			mParameters.setPreviewSize(optimalSize.width, optimalSize.height);
//		}
		previewImageSize = mParameters.getPreviewSize();
		
		mCamera.setPreviewCallback(mPaintView);
		
//		mCamera.setParameters(mParameters);
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

	public class PreviewProcessor extends View implements Camera.PreviewCallback {
		private boolean processing;
		private int counter;
		private byte[] data;
		private int[] rgb;
		private Bitmap bmp;
		private Paint mPaint;

		@Override
		public void onPreviewFrame(byte[] data, Camera camera) {
			loadNV21(data);
		}
		
		public void loadNV21(byte[] bytearray) {
			if (processing) return;

			processing = true;
			counter++;

			if (data.length != bytearray.length) data = new byte[bytearray.length];
			System.arraycopy(bytearray, 0, data, 0, bytearray.length);
			
			invalidate();
		}
		
		public PreviewProcessor(Context context) {
			super(context);
			processing = false;
			counter = 0;
			rgb = new int[0];
			data = new byte[0];
			mPaint = new Paint();
		}

		@Override
		protected void onDraw(Canvas canvas) {
			if (data.length == 0) return;
			
			int w = previewImageSize.width;
			int h = previewImageSize.height;
			if (rgb.length != w*h) rgb = new int[w*h];
			decodeYUV420SP_colanomalous(rgb, data, w, h);
			bmp = Bitmap.createBitmap(rgb, w, h, Bitmap.Config.RGB_565);
			canvas.drawBitmap(bmp, 0, 0, null);
//			canvas.drawText(Integer.toString(counter), 50, 50, mPaint);
			processing = false;
		}

		// TODO: Move this to JNI/JNA
		// Copies the red channel into the blue channel!
		public void decodeYUV420SP_colanomalous(int[] rgb, byte[] yuv420sp, int width, int height) {
			final int frameSize = width * height;

			for (int j = 0, yp = 0; j < height; j++) {
				int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
				for (int i = 0; i < width; i++, yp++) {
					int y = (0xff & ((int) yuv420sp[yp])) - 16;
					if (y < 0) y = 0;
					if ((i & 1) == 0) {
						v = (0xff & yuv420sp[uvp++]) - 128;
						u = (0xff & yuv420sp[uvp++]) - 128;
					}

					int y1192 = 1192 * y;
					int r = (y1192 + 1634 * v);
					int g = (y1192 - 833 * v - 400 * u);
//				    int b = (y1192 + 2066 * u);
					int b = (y1192 + 1634 * v);

					if (r < 0) r = 0; else if (r > 262143) r = 262143;
					if (g < 0) g = 0; else if (g > 262143) g = 262143;
					if (b < 0) b = 0; else if (b > 262143) b = 262143;

					rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
				}
			}
		}

	}

}