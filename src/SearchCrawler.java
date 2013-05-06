import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.*;
import java.util.regex.*;

import javax.servlet.jsp.PageContext;
import javax.swing.*;
import javax.swing.table.*;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


// The Search Web Crawler
@WebServlet("/SearchCrawler")
public class SearchCrawler extends HttpServlet {
	// Cache of robot disallow lists.
	private HashMap disallowListCache = new HashMap(); 
	// robot��移⑦닾瑜��덉슜�섏� �딆� site��由ъ뒪��
	public String startUrl = "";  // 移댄뀒怨좊━ �⑥뼱 
	public String pageContents = "";
	
	private static final long serialVersionUID = 1L;
    public String str;   
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SearchCrawler() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.setContentType("text/html; charset=UTF-8");
		request.setCharacterEncoding("UTF-8");
		
		str = request.getParameter("string");
		byte[] b = str.getBytes("iso-8859-1");
		str = new String(b, "UTF-8");
		
		go();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(pageContents);
		
		PrintWriter pw = response.getWriter();
		pw.println("<html><title>gugu ANG</title><body>");
		pw.println(pageContents);
		pw.println("</body></html>");
		
		pw.close();
	}

	// Handle search/stop button being clicked. Search �먮뒗 Stop 踰꾪듉���대┃�섎㈃ �ш린媛��몄텧
	// �쒕떎.
	private void go() { 
		try {
			str = URLEncoder.encode(str, "EUC-KR"); // 援곕�瑜�EUC-KR�뺤떇�쇰줈 蹂�솚 
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		
		//Nate News Search Format
		str = "http://search.nate.com/search/all.html?ssn=036&dsn=3&asn=003600540&thr=vnnw&nq=&q=" + str + "&e=";

		// Start the search crawler.
		for(int pageNum = 1; pageNum <= 11; pageNum += 10){  // 10�섏씠吏�퉴吏��섏쭛
			search(str + pageNum); //search() �몄텧
		}
	}

	private void search(final String startUrl) {
		// Start the search in a new thread.
		Thread thread = new Thread(new Runnable() { //thread �앹꽦
			public void run() { //thread �쒖옉 
				try {
					crawl(startUrl , true); // crawl(寃�깋二쇱냼 , ��냼臾몄옄 援щЦ or not)
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		thread.start();
	}

	// Verify URL format. //url�щ㎎��寃�깋 
	private URL verifyUrl(String url) {
		// Only allow HTTP URLs.
		if (!url.toLowerCase().startsWith("http://"))
			return null;

		// Verify format of URL.
		URL verifiedUrl = null;
		try {
			verifiedUrl = new URL(url);
		} catch (Exception e) {
			return null;
		}

		return verifiedUrl;
	}

	// Check if robot is allowed to access the given URL.
	@SuppressWarnings("unchecked") //濡쒕큸 �꾨줈�좎퐳���댄뻾�섍린 �꾪븳 硫붿꽌��
	private boolean isRobotAllowed(URL urlToCheck) {
		String host = urlToCheck.getHost().toLowerCase();

		// Retrieve host's disallow list from cache. //濡쒕큸 �꾨줈�좎퐳��留욎� �딅뒗 寃껋쓣 �댁쓣 由ъ뒪��
		ArrayList<String> disallowList = (ArrayList<String>) disallowListCache.get(host);

		// If list is not in the cache, download and cache it.
		if (disallowList == null) {
			disallowList = new ArrayList();

			try {
				URL robotsFileUrl = new URL("http://" + host + "/robots.txt");

				// Open connection to robot file URL for reading.
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(robotsFileUrl.openStream()));

				// Read robot file, creating list of disallowed paths.
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.indexOf("Disallow:") == 0) {
						String disallowPath = line.substring("Disallow:"
								.length());

						// Check disallow path for comments and remove if
						// present.
						int commentIndex = disallowPath.indexOf("#");
						if (commentIndex != -1) {
							disallowPath = disallowPath.substring(0,
									commentIndex);
						}

						// Remove leading or trailing spaces from disallow path.
						disallowPath = disallowPath.trim();

						// Add disallow path to list.
						disallowList.add(disallowPath);
					}
				}

				// Add new disallow list to cache.
				disallowListCache.put(host, disallowList);
			} catch (Exception e) {
				/*
				 * Assume robot is allowed since an exception is thrown if the
				 * robot file doesn't exist.
				 */
				return true;
			}
		}

		/*
		 * Loop through disallow list to see if the crawling is allowed for the
		 * given URL.
		 */
		String file = urlToCheck.getFile();
		for (int i = 0; i < disallowList.size(); i++) {
			String disallow = (String) disallowList.get(i);
			if (file.startsWith(disallow)) {
				return false;
			}
		}

		return true;
	}

	// Download page at given URL. //寃�궗瑜��듦낵��url���ㅼ슫濡쒕뱶 �쒕떎. 
	private String downloadPage(URL pageUrl) {
		try {
			// Open connection to URL for reading.
			InputStream stream = pageUrl.openStream();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					stream,"EUC-KR"));  //EUC-KR, UTF-8
			// Read page into buffer.
			String line = null;  //Each Line in HTML 
			StringBuffer pageBuffer = new StringBuffer();
			
			while ((line = reader.readLine()) != null) {
				if(line.indexOf("<ul class=\"search-list\">") != -1){
					ArrayList<String> list = searchResults(reader);
					for(String str : list){
						pageBuffer.append(str+"\n");
					}
					break;
				}
			}
			return pageBuffer.toString();
		} catch (Exception e) {
		}
		return null;
	}

	public ArrayList<String> searchResults(BufferedReader reader) throws IOException{
		ArrayList<String> searchList = new ArrayList<String>();
		String line = null;
		while((line = reader.readLine()).indexOf("</ul>") == -1){
			searchList.add(line);
		}
		return searchList;
	}
	
	public String extractStrings(String resultPage){
		StringBuffer extracted = new StringBuffer();
		
		Pattern p = Pattern.compile("<a\\s+href\\s*=\\s*\"http://news.nate.com/view/?(.*?)[\"|>]", //<a href="http://news.nate.com/view/******" 蹂꾪몴瑜�嫄몃윭��
				Pattern.CASE_INSENSITIVE); 
		Matcher m = p.matcher(resultPage);
		
		resultPage = resultPage.replace("003600540&amp;sa=&amp;c=1','','');\"><img src","");
		
		Pattern p2 = Pattern.compile("003600540&amp;sa=&amp;c=1','',''\\);\">?(.*?)</a></dt>");
		Matcher m2 = p2.matcher(resultPage);
		
		String prev = "";
		while (m.find()) { //留ㅼ튂媛��섎㈃ 怨꾩냽 �뚯븘媛꾨떎. 
			String link = m.group(1).trim(); 
			
			if(!link.equals(prev)){
				extracted.append(link + '\n');
			}
			prev = link;
		}
		while(m2.find()){
			String link2 = m2.group(1).trim();
			link2 = link2.replace("<b>", "");
			link2 = link2.replace("</b>", "");
			extracted.append(link2 + '\n');
		}
		
		return extracted.toString();
	}
	
	// Perform the actual crawling, searching for the search string.
	public void crawl(String startUrl, boolean caseSensitive) throws IOException{
		// Setup crawl lists. -> �섏쭛 �뺣낫瑜�湲곕줉�섍린 �꾪븳 HashSet �ㅼ젙 
		HashSet crawledList = new HashSet(); //�섏쭛���앸궃 寃껋쓣 湲곕줉�섍린 �꾪븳 寃�
		LinkedHashSet toCrawlList = new LinkedHashSet(); //�욎쑝濡��섏쭛��寃껋쓣 湲곕줉�섍린 �꾪븳 寃�

		// Add start URL to the to crawl list.
		toCrawlList.add(startUrl); //�섏쭛�섍린 �꾪븳 由ъ뒪�몄뿉 �ъ슜�먭� 異쒕컻�먯쑝濡�吏�젙���ъ씠�몃� 湲곕줉

			URL verifiedUrl = verifyUrl(startUrl);  //URL�대씪���대옒�ㅻ뒗 java��湲곕낯 �댁옣 �섏뼱 �덉쓬 
										     	//RFC 2396: Uniform Resource Identifiers (URI): Generic Syntax, 									//�곴린 洹쒖젙��留욌뒗吏�寃�궗 
			// Skip URL if robots are not allowed to access it.
			if (!isRobotAllowed(verifiedUrl)) {//洹쒖젙��留욎� �딆쑝硫��섏쭛���섏� �딄퀬 �ㅼ쓬�쇰줈 �섏뼱 媛꾨떎. 
				System.out.println("Robot��嫄몃┝");
			}
		    pageContents = downloadPage(verifiedUrl); //�섏씠吏�� �꾨� �쎌뼱 �ㅼ뿬 ��옣
			//System.out.println(extractStrings(pageContents));		
			// Download the page at the given url. //downloadpage瑜��몄텧,�섑뻾�섏뿬 由ы꽩媛믪쓣 pageContent���ｌ쓬
			File save_txt = new File("C:/Users/Administrator/Desktop/test.out"); // ��옣 ���뚯씪紐�
            PrintWriter pw = new PrintWriter(new FileWriter(save_txt,true));
            pw.println(pageContents);
            pw.close();
	}
	
	// Run the Search Crawler.
	public static void main(String[] args) {
	}
}
