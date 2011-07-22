package li.peterandpatty.colanomalous;

import android.app.Activity;
import android.content.SharedPreferences;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

public class ColActivity extends Activity {	
	private FrameLayout mFrame;
	private ProcessedView mProcessedView;
	private PreviewSurface mPreviewSurface;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mProcessedView = new ProcessedView(this); 
		mPreviewSurface = new PreviewSurface(this, mProcessedView);
		
		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		int width  = preferences.getInt("previewWidth",  0);
		int height = preferences.getInt("previewHeight", 0);
		mPreviewSurface.setPreviewSize(width, height);
		mPreviewSurface.setFocusMode(preferences.getString("focusMode", null));
		mProcessedView.setYUVProcessor(preferences.getString("processingMode", null));		

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
    public boolean onPrepareOptionsMenu(Menu menu) {
    	menu.removeItem(R.id.focus_now);
        if (mPreviewSurface.canAutoFocus()) menu.add(Menu.NONE, R.id.focus_now, Menu.NONE, R.string.focus_now);
    	return true;
    }
    
    
    private boolean inMenu = false;
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.set_preview_size:
        	showPreviewSizeMenu();
            return true;
        case R.id.set_preview_processing_mode:
        	showPreviewProcessingModeMenu();
        	return true;
        case R.id.set_focus_mode:
        	showFocusModeMenu();
        	return true;
        case R.id.focus_now:
        	mPreviewSurface.autoFocus(null);
        	return true;
        case R.id.quit:
        	finish();
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    
    private void showFocusModeMenu() {
    	inMenu = true;
    	setContentView(getFocusModeMenu());
    }
    
    ListView focusModeMenu;
    private ListView getFocusModeMenu() {
    	if (focusModeMenu == null) buildFocusModeMenu();
    	return focusModeMenu;
    }

    private void buildFocusModeMenu() {
    	ListView lv = new ListView(this);

    	TextView header = new TextView(this);
    	header.setText(getString(R.string.set_focus_mode));
    	lv.addHeaderView(header);
    	final int positionOffset = 1;

    	final List<String> focusModes = mPreviewSurface.getSupportedFocusModes();
    	lv.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, focusModes));
    	lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    	lv.setItemChecked(mPreviewSurface.currentFocusModeIndex()+positionOffset, true);
    	
        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            	mPreviewSurface.setFocusMode(focusModes.get(position-positionOffset));
            	exitMenu();
            }
        });

        focusModeMenu = lv;
    }
    
    
    private void showPreviewSizeMenu() {
    	inMenu = true;
        setContentView(getPreviewSizeMenu());
    }

    private ListView previewSizeMenu;
    private ListView getPreviewSizeMenu() {
    	if (previewSizeMenu == null) buildPreviewSizeMenu();
    	return previewSizeMenu;
    }
    
    private void buildPreviewSizeMenu() {
        ListView lv = new ListView(this);
        
    	TextView header = new TextView(this);
    	header.setText(getString(R.string.set_preview_size));
    	lv.addHeaderView(header);
    	final int positionOffset = 1;
    	
    	final List<Size> previewSizes = mPreviewSurface.getSupportedPreviewSizes();
    	String[] previewSizeNames = new String[previewSizes.size()];
    	for (int i = 0; i < previewSizes.size(); i++) {
    		Size size = previewSizes.get(i);
    		previewSizeNames[i] = size.width + "x" + size.height;
    	}
        lv.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, previewSizeNames));
    	lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    	
    	int selectedPosition = mPreviewSurface.currentPreviewSizeIndex() + positionOffset;
    	lv.setItemChecked(selectedPosition, true);
    	lv.setSelection(selectedPosition);
        
        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            	mPreviewSurface.setPreviewSize(previewSizes.get(position-positionOffset));
            	exitMenu();
            }
        });

        previewSizeMenu = lv;
    }

    
    private void showPreviewProcessingModeMenu() {
    	inMenu = true;
        setContentView(getPreviewProcessingModeMenu());	
    }
    
    private ListView previewProcessingModeMenu;
    private ListView getPreviewProcessingModeMenu() {
    	if (previewProcessingModeMenu == null) buildPreviewProcessingModeMenu();
    	return previewProcessingModeMenu;
    }

    private void buildPreviewProcessingModeMenu() {
        ListView lv = new ListView(this);

        TextView header = new TextView(this);
    	header.setText(getString(R.string.set_preview_processing_mode));
    	lv.addHeaderView(header);
    	final int positionOffset = 1;
    	
        lv.setAdapter(new ArrayAdapter<YUVProcessor>(this, android.R.layout.simple_list_item_single_choice, YUVProcessor.YUV_PROCESSORS));
    	lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    	lv.setItemChecked(mProcessedView.currentYUVProcessor()+positionOffset, true);
        
        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            	mProcessedView.setYUVProcessor(YUVProcessor.YUV_PROCESSORS[position-positionOffset]);
            	exitMenu();
            }
        });
        previewProcessingModeMenu = lv;    	
    }
    
    
    @Override
    public void onBackPressed() {
    	if (inMenu) exitMenu();
    	else finish();
    }
    
    
    private void exitMenu() {
    	setContentView(mFrame);
    	inMenu = false;
    }

    
    @Override
    protected void onPause() {
      super.onPause();
      
      SharedPreferences preferences = getPreferences(MODE_PRIVATE);
      SharedPreferences.Editor editor = preferences.edit();

      editor.putString("processingMode", mProcessedView.getYUVProcessor().getName());
      editor.putInt("previewWidth",  mPreviewSurface.getPreviewWidth());
      editor.putInt("previewHeight", mPreviewSurface.getPreviewHeight());
      editor.putString("focusMode", mPreviewSurface.getFocusMode());

      editor.commit();
    }
}