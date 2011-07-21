package li.peterandpatty.colanomalous;

import java.util.Arrays;

public abstract class YUVProcessor {	
	final static YUVProcessor[] YUV_PROCESSORS = new YUVProcessor[]{new RGB2MG(), new RGB2RC()};
	final static YUVProcessor DEFAULT = YUV_PROCESSORS[0];

	public static YUVProcessor find(String processorName) {
		if (processorName == null) return DEFAULT;

		for (YUVProcessor p : Arrays.asList(YUV_PROCESSORS))
			if (p.getName() == processorName) return p;
		
		return DEFAULT;
	}

	public abstract void processYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height);
	
	@Override
	public String toString() { return getName(); }	
	public abstract String getName();

}