package li.peterandpatty.colanomalous;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;

import java.io.IOException;
import java.util.List;

public class ColActivity extends Activity {	
	private FrameLayout mFrame;
	private ProcessedView mProcessedView;
	private PreviewSurface mPreviewSurface;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SharedPreferences preferences = getPreferences(MODE_PRIVATE);

		mProcessedView = new ProcessedView(this); 
		mPreviewSurface = new PreviewSurface(this, mProcessedView);

		String processingMode = preferences.getString("processingMode", null);
		mProcessedView.setYUVProcessor(processingMode);
		
		int width  = preferences.getInt("previewWidth",  0);
		int height = preferences.getInt("previewHeight", 0);
		mPreviewSurface.setPreviewSize(width, height);

		setContentView(R.layout.main);
		mFrame = (FrameLayout) findViewById(R.id.frame);		
		mFrame.addView(mPreviewSurface);
		mFrame.addView(mProcessedView);
	}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.set_preview_size:
        	showPreviewSizeMenu();
            return true;
        case R.id.set_preview_processing_mode:
        	showPreviewProcessingModeMenu();
        	return true;
        case R.id.quit:
        	finish();
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    
    private void showPreviewSizeMenu() {
        setContentView(getPreviewSizeMenu());
    }

    ListView previewSizeMenu;
    private ListView getPreviewSizeMenu() {
    	if (previewSizeMenu == null) previewSizeMenu = buildPreviewSizeMenu();
    	return previewSizeMenu;
    }
    
    private ListView buildPreviewSizeMenu() {
    	final List<Size> previewSizes = mPreviewSurface.getSupportedPreviewSizes();
    	String[] previewSizeNames = new String[previewSizes.size()];
    	for (int i = 0; i < previewSizes.size(); i++) {
    		Size size = previewSizes.get(i);
    		previewSizeNames[i] = size.width + "x" + size.height;
    	}
    	
        ListView lv = new ListView(this);
        lv.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, previewSizeNames));
        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            	mPreviewSurface.setPreviewSize(previewSizes.get(position));
            	setContentView(mFrame);
            }
        });

        return lv;
    }

    
    private void showPreviewProcessingModeMenu() {
        setContentView(getPreviewProcessingModeMenu());	
    }
    
    private ListView previewProcessingModeMenu;
    private ListView getPreviewProcessingModeMenu() {
    	if (previewProcessingModeMenu == null) previewProcessingModeMenu = buildPreviewProcessingModeMenu();
    	return previewProcessingModeMenu;
    }

    private ListView buildPreviewProcessingModeMenu() {
        ListView lv = new ListView(this);
        lv.setAdapter(new ArrayAdapter<YUVProcessor>(this, android.R.layout.simple_list_item_1, YUVProcessor.YUV_PROCESSORS));
        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            	mProcessedView.setYUVProcessor(YUVProcessor.YUV_PROCESSORS[position]);
            	setContentView(mFrame);
            }
        });
        return lv;    	
    }
    
    @Override
    protected void onPause() {
      super.onPause();
      
      SharedPreferences preferences = getPreferences(MODE_PRIVATE);
      SharedPreferences.Editor editor = preferences.edit();

      editor.putString("processingMode", mProcessedView.getYUVProcessor().getName());

      editor.putInt("previewWidth",  mPreviewSurface.getPreviewWidth());
      editor.putInt("previewHeight", mPreviewSurface.getPreviewHeight());

      editor.commit();
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


	private List<Size> supportedPreviewSizes;
	List<Size> getSupportedPreviewSizes() { return supportedPreviewSizes; }	
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// The Surface has been created, acquire the camera and tell it where to draw.
		mCamera = Camera.open();
		try {
			// This needs to be available to the preview size menu
			Camera.Parameters parameters = mCamera.getParameters();
			supportedPreviewSizes = parameters.getSupportedPreviewSizes();

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

	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		mCamera.setPreviewCallback(null);
		mCamera.stopPreview();
		
		// Check previewWidth and previewHeight, if invalid just use what the camera wanted
		Camera.Parameters mParameters = mCamera.getParameters();		
		if (previewWidth == 0 || previewHeight == 0)        setPreviewSize(mParameters.getPreviewSize());
		if (!validPreviewSize(previewWidth, previewHeight)) setPreviewSize(mParameters.getPreviewSize());

		// Set preview size, notify camera
		mParameters.setPreviewSize(previewWidth, previewHeight);
		mCamera.setParameters(mParameters);
		
		// Reset ProcessedView, notify of preview size, set callback and start camera!
		mProcessedView.reset(mParameters.getPreviewSize());
		mCamera.setPreviewCallback(mProcessedView);
		mCamera.startPreview();
	}


	private boolean validPreviewSize(int width, int height) {
		for (Size sps : supportedPreviewSizes)
			if (sps.width == width && sps.height == height) return true;
		return false;
	}
	
}

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
	
	
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) { loadNV21(data); }

	public void loadNV21(byte[] bytearray) {
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
			String debugString = Double.toString(1000 / (System.currentTimeMillis() - lastFrame)) + " FPS, " + w + "x" + h;
			canvas.drawText(debugString, 50, 50, mPaint);
			lastFrame = System.currentTimeMillis();
		}

		processing = false;
	}
}