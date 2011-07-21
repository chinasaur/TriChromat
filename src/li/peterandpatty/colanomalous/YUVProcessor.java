package li.peterandpatty.colanomalous;

public interface YUVProcessor {
	public abstract void processYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height);
}