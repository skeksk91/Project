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
import javax.swing.*;
import javax.swing.table.*;

// The Search Web Crawler
public class SearchCrawler extends JFrame {
	// Cache of robot disallow lists.
	private HashMap disallowListCache = new HashMap(); 
	// robot의 침투를 허용하지 않은 site의 리스트

	public SearchCrawler() {
	} 

	// Handle search/stop button being clicked. Search 또는 Stop 버튼이 클릭되면 여기가 호출
	// 된다.
	private void go() {

		String startUrl = "군대";   // 카테고리 단어 

		try {
			startUrl = URLEncoder.encode(startUrl, "EUC-KR"); // 군대를 EUC-KR형식으로 변환 
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		
		//Nate News Search Format
		startUrl = "http://search.nate.com/search/all.html?ssn=036&dsn=3&asn=003600540&thr=vnnw&nq=&q=" + startUrl + "&e=1";

		// Start the search crawler.
		search(startUrl); //search() 호출
	}

	private void search(final String startUrl) {
		// Start the search in a new thread.
		Thread thread = new Thread(new Runnable() { //thread 생성
			public void run() { //thread 시작 
				try {
					crawl(startUrl , true); // crawl(검색주소 , 대소문자 구문 or not)
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		thread.start();
	}

	// Verify URL format. //url포맷의 검색 
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
	@SuppressWarnings("unchecked") //로봇 프로토콜을 이행하기 위한 메서드 
	private boolean isRobotAllowed(URL urlToCheck) {
		String host = urlToCheck.getHost().toLowerCase();

		// Retrieve host's disallow list from cache. //로봇 프로토콜에 맞지 않는 것을 담을 리스트 
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

	// Download page at given URL. //검사를 통과한 url을 다운로드 한다. 
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
		Pattern p = Pattern.compile("<a\\s+href\\s*=\\s*\"http://news.nate.com/view/?(.*?)[\"|>]", //<a href="http://news.nate.com/view/******" 별표를 걸러냄
				Pattern.CASE_INSENSITIVE); 
		Matcher m = p.matcher(resultPage);
		
		String prev = "";
		int i = 1;
		while (m.find()) { //매치가 되면 계속 돌아간다. 
			String link = m.group(1).trim(); 
			if(!link.equals(prev))
				extracted.append(link + '\n');
			prev = link;
			i++;
		}
		return extracted.toString();
	}
	
	// Perform the actual crawling, searching for the search string.
	public void crawl(String startUrl, boolean caseSensitive) throws IOException{
		// Setup crawl lists. -> 수집 정보를 기록하기 위한 HashSet 설정 
		HashSet crawledList = new HashSet(); //수집이 끝난 것을 기록하기 위한 것 
		LinkedHashSet toCrawlList = new LinkedHashSet(); //앞으로 수집할 것을 기록하기 위한 것 

		// Add start URL to the to crawl list.
		toCrawlList.add(startUrl); //수집하기 위한 리스트에 사용자가 출발점으로 지정한 사이트를 기록

			URL verifiedUrl = verifyUrl(startUrl);  //URL이라는 클래스는 java에 기본 내장 되어 있음 
										     	//RFC 2396: Uniform Resource Identifiers (URI): Generic Syntax, 									//상기 규정에 맞는지 검사 
			// Skip URL if robots are not allowed to access it.
			if (!isRobotAllowed(verifiedUrl)) {//규정에 맞지 않으면 수집을 하지 않고 다음으로 넘어 간다. 
				System.out.println("Robot에 걸림");
			}
			String pageContents = downloadPage(verifiedUrl); //페이지를 전부 읽어 들여 저장
			System.out.println(extractStrings(pageContents));		
			// Download the page at the given url. //downloadpage를 호출,수행하여 리턴값을 pageContent에 넣음
			File save_txt = new File("C:/Users/Administrator/Desktop/test.out"); // 저장 될 파일명
            PrintWriter pw = new PrintWriter(new FileWriter(save_txt,true));
           // System.out.println(pageContents);
            pw.println(pageContents);
            pw.close();
	}
	
	// Run the Search Crawler.
	public static void main(String[] args) {
		SearchCrawler crawler = new SearchCrawler();
		crawler.go();
	}
}
