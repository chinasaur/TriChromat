package li.peterandpatty.trichromat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.view.View;

class ProcessedView extends View implements Camera.PreviewCallback {
	private final Paint mPaint;
	
	private boolean processing;
	private byte[] data;
	private int[] rgb;

	public ProcessedView(Context context) {
		super(context);
		mPaint = new Paint();
	}


	public void setYUVProcessor(String processorName) {
		setYUVProcessor(YUVProcessor.find(processorName));
	}


	// Sent over from PreviewSurface whenever the size is changed; could be pulled from camera in onPreviewFrame, but 
	// that would be more frequent checking than necessary.
	private Size previewImageSize;
	public void reset(Size previewImageSize) {
		processing = false;
		rgb = new int[0];
		data = new byte[0];
		this.previewImageSize = previewImageSize;
	}

	
	private YUVProcessor mYUVProcessor;
	void setYUVProcessor(YUVProcessor yuvProcessor) { mYUVProcessor = yuvProcessor; }
	YUVProcessor getYUVProcessor() { return mYUVProcessor; }
	
	int currentYUVProcessor() {
		for (int i = 0; i < YUVProcessor.YUV_PROCESSORS.length; i++) {
			YUVProcessor yp = YUVProcessor.YUV_PROCESSORS[i];
			if (yp == mYUVProcessor) return i;
		}
		throw new Error("This should never happen");
	}
	
	
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) { 
		loadNV21(data); 
	}
	
	
	private void loadNV21(byte[] bytearray) {
		if (processing) return;
		processing = true;

		if (data.length != bytearray.length) data = new byte[bytearray.length];
		System.arraycopy(bytearray, 0, data, 0, bytearray.length);

		invalidate();
	}


	static final boolean showDebugging = true;
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

		if (showDebugging) {
			String debugString = "";
			debugString += Double.toString(1000 / (System.currentTimeMillis() - lastFrame)) + " FPS";
			canvas.drawText(debugString, 50, 50, mPaint);
			lastFrame = System.currentTimeMillis();
		}

		processing = false;
	}

}