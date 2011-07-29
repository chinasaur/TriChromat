package li.peterandpatty.trichromat;

public class NativeRGB2MG extends YUVProcessor {
    static { System.loadLibrary("yuv420sp2mg"); }
    public static native void processYUV(int[] rgb, byte[] yuv420sp, int width, int height, int type);	

	@Override
	public void processYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {
		processYUV(rgb, yuv420sp, width, height, 2);
	}
	
	@Override
	public String getName() {
		return "Native RGB2MG (not working)";
	}
}
