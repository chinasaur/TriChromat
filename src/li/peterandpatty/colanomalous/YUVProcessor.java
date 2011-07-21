package li.peterandpatty.colanomalous;

public abstract class YUVProcessor {	
	public abstract void processYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height);
	
	@Override
	public String toString() { return getName(); }	
	public abstract String getName();
}