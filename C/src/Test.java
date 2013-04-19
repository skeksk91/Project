import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;


public class Test {
	
	public static void main(String[] args) throws UnsupportedEncodingException{
		String hangul = "군대";
		String encoded = URLEncoder.encode(hangul, "EUC-KR");
		System.out.println(encoded);
	}
}
