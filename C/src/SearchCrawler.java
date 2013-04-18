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
	// Max URLs drop down values.
	private static final String[] MAX_URLS = { "50", "100", "500", "1000" }; 
    
	// Cache of robot disallow lists.
	private HashMap disallowListCache = new HashMap(); // disallow : ~을 허가하지 않다.
	// robot의 침투를 허용하지 않은 site의 리스트

	// Search GUI controls. // run했을 때 나오는 UI와 비교해서 아래 변수들을 비교하면 볼 것
	// File이라고 써있는 곳의 아랬부분
	private JTextField startTextField; // startURL의 텍스트 필드
	private JComboBox maxComboBox; // MaxURLs의 콤보박스 => 50 100 500 1000
	private JCheckBox limitCheckBox; // Limit crawling to start URL site의 체크박스
	private JTextField logTextField; // Matches Log File의 텍스트 필드
	private JTextField searchTextField;// Search String의 텍스트 필드
	private JCheckBox caseCheckBox; // Case sensitive의 체크 박스
	private JButton searchButton; // Search 버튼

	// Search stats GUI controls.
	// Search 버튼의 밑줄 아랫 부분
	private JLabel crawlingLabel2; // Crawling :
	private JLabel crawledLabel2; // Crawled URLS :
	private JLabel toCrawlLabel2; // URL to Crawl :
	private JProgressBar progressBar; // Crawling Progress 
	private JLabel matchesLabel2; // URL
	// Table listing search matches.
	private JTable table; // URL이라고 써있는 곳의 큰 텍스트 박스 같은 곳 -> 나중에 결과 출력

	// Flag for whether or not crawling is underway.
	private boolean crawling; // 로봇의 활동이 진행 중인지 아닌지 나타냄 (나타내는 곳을 Flag라고 하는 것 같음)

	// Matches log file print writer.
	private PrintWriter logFileWriter; // 로그 파일 출력을 위한 것

	// Constructor for Search Web Crawler. => 생성자에서 UI에 관한 모든 것이 이루어 진다. (향후
	// Flex로 대체)
	public SearchCrawler() {
		// Set application title.
		setTitle("Search Crawler"); // UI의 제목

		// Set window size.
		setSize(600, 600); // width 600 height 700으로 설정

		// Handle window closing events.
		addWindowListener(new WindowAdapter() { // UI를 닫기 위한 WindowListener 설정
			// (X를 눌렀을 때 사라짐)
			public void windowClosing(WindowEvent e) {
				actionExit();
			}
		});

		// Set up search panel.
		JPanel searchPanel = new JPanel();
		// 패널(판넬)은 투명한 판이다. 여기에 우리가 만든 UI를 가져다 붙인다.

		GridBagConstraints constraints;
		// GridBagConstraints 클래스는,GridBagLayout 클래스를 사용해 배치되는 컴퍼넌트의 제약을 지정합니다.
		GridBagLayout layout = new GridBagLayout();
		// GridBagLayout 클래스는, 다른 크기의 컴퍼넌트에서도 종횡에,
		// 또는 baseline에 따라 배치할 수 있는 유연한 레이아웃 매니저입니다.

		//
		searchPanel.setLayout(layout);// 패널에 우리가 만든 레이아웃을 붙이 겠다고 말해줌

		// File바로 아래에 밑줄 밑에

		// Start URL : [] 관한 설정 시작
		// ////////////////////////////////////////////////////////////
		// Start URL : [] 관한 Label 설정 시작
		JLabel startLabel = new JLabel("Start URL:");
		constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.EAST; // 아래 위치(inset)에서 오른쪽 정렬
		constraints.insets = new Insets(5, 5, 0, 0); // Start URL : 등의 모든 위치가
		// 이것으로 통일 된다.
		layout.setConstraints(startLabel, constraints);
		// 라벨을 패널에 붙이되 이름은 Start URL로 Inset서 정한 위치에 붙이라고...
		searchPanel.add(startLabel);
		// Start URL : [] Label 관한 설정 끝
		// Start URL : [] 관한 TextField 설정 시작
		startTextField = new JTextField();
		constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.insets = new Insets(5, 5, 0, 5);
		layout.setConstraints(startTextField, constraints);
		searchPanel.add(startTextField);
		// Start URL : [] 관한 TextField 설정 끝
		// Start URL : [] 관한 설정 끝
		// /////////////////////////////////////////////////////////////

		// 아래부터는 설명 생략, 위 Start URL과 동일하게 생각하기 바람
		// Max URLs to Crawl:
		// 시작///////////////////////////////////////////////////////////////
		// 라벨
		JLabel maxLabel = new JLabel("Max URLs to Crawl:");
		constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.EAST;
		constraints.insets = new Insets(5, 5, 0, 0);
		layout.setConstraints(maxLabel, constraints);
		searchPanel.add(maxLabel);
		// 콤보박스
		maxComboBox = new JComboBox(MAX_URLS);
		maxComboBox.setEditable(true);
		constraints = new GridBagConstraints();
		constraints.insets = new Insets(5, 5, 0, 0);
		layout.setConstraints(maxComboBox, constraints);
		searchPanel.add(maxComboBox);
		// 체크박스
		limitCheckBox = new JCheckBox("Limit crawling to Start URL site");
		constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.WEST;
		constraints.insets = new Insets(0, 10, 0, 0);
		layout.setConstraints(limitCheckBox, constraints);
		searchPanel.add(limitCheckBox);
		// 체크박스에 감춰진 라벨이 존재한다. 아직까지 무엇에서 쓸 것인가 알수 없음
		JLabel blankLabel = new JLabel();
		constraints = new GridBagConstraints();
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		layout.setConstraints(blankLabel, constraints);
		searchPanel.add(blankLabel);
		// Max URLs to Crawl:
		// 끝/////////////////////////////////////////////////////////////////

		// Matches Log File: 시작
		// //////////////////////////////////////////////////////////////
		// 라벨
		JLabel logLabel = new JLabel("Matches Log File:");
		constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.EAST;
		constraints.insets = new Insets(5, 5, 0, 0);
		layout.setConstraints(logLabel, constraints);
		searchPanel.add(logLabel);
		// 텍스트 파일에 자동으로 경로가 써질 것인데 이것을 준비
		String file = System.getProperty("user.dir")
		+ System.getProperty("file.separator") + "crawler.log";
		logTextField = new JTextField(file);
		constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.insets = new Insets(5, 5, 0, 5);
		layout.setConstraints(logTextField, constraints);
		searchPanel.add(logTextField);
		// Matches Log File: 끝
		// ///////////////////////////////////////////////////////////////

		// Search String: 시작
		// /////////////////////////////////////////////////////////////////
		// 라벨
		JLabel searchLabel = new JLabel("Search String:");
		constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.EAST;
		constraints.insets = new Insets(5, 5, 0, 0);
		layout.setConstraints(searchLabel, constraints);
		searchPanel.add(searchLabel);
		// 텍스트 박스
		searchTextField = new JTextField();
		constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.insets = new Insets(5, 5, 0, 0);
		constraints.gridwidth = 2;
		constraints.weightx = 1.0d;
		layout.setConstraints(searchTextField, constraints);
		searchPanel.add(searchTextField);
		// 체크박스
		caseCheckBox = new JCheckBox("Case Sensitive");
		constraints = new GridBagConstraints();
		constraints.insets = new Insets(5, 5, 0, 5);
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		layout.setConstraints(caseCheckBox, constraints);
		searchPanel.add(caseCheckBox);
		// Search String: 끝
		// //////////////////////////////////////////////////////////////////////

		// Search 버튼 시작
		// ///////////////////////////////////////////////////////////////////////
		searchButton = new JButton("Search");
		searchButton.addActionListener(new ActionListener() {
			public
			void actionPerformed(ActionEvent e) {
				actionSearch();
			}
		});
		constraints = new GridBagConstraints();
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.insets = new Insets(5, 5, 5, 5);
		layout.setConstraints(searchButton, constraints);
		searchPanel.add(searchButton);
		// Search 버튼 끝
		// ////////////////////////////////////////////////////////////////////////////////

		// 밑줄
		// 시작-----------------------------------------------------------------------------------------
		JSeparator separator = new JSeparator();
		constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.insets = new Insets(5, 5, 5, 5);
		layout.setConstraints(separator, constraints);
		searchPanel.add(separator);
		// 밑줄
		// 끝-----------------------------------------------------------------------------------------

		// Crawling: 시작
		// ///////////////////////////////////////////////////////////////////////
		JLabel crawlingLabel1 = new JLabel("Crawling:");
		constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.EAST;
		constraints.insets = new Insets(5, 5, 0, 0);
		layout.setConstraints(crawlingLabel1, constraints);
		searchPanel.add(crawlingLabel1);
		// 화면에 보여지는 Label이외 하나를 더 설정한다. (이유는 아직 모름)
		crawlingLabel2 = new JLabel();
		crawlingLabel2.setFont(crawlingLabel2.getFont().deriveFont(Font.PLAIN));
		constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.insets = new Insets(5, 5, 0, 5);
		layout.setConstraints(crawlingLabel2, constraints);
		searchPanel.add(crawlingLabel2);
		// Crawling: 끝
		// ////////////////////////////////////////////////////////////////////////

		// Crawled URLs: 시작
		// /////////////////////////////////////////////////////////////////
		JLabel crawledLabel1 = new JLabel("Crawled URLs:");
		constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.EAST;
		constraints.insets = new Insets(5, 5, 0, 0);
		layout.setConstraints(crawledLabel1, constraints);
		searchPanel.add(crawledLabel1);
		// 여기두 Label이 하다 더 있다. (이유 아직 몰러~~~~~~~)
		crawledLabel2 = new JLabel();
		crawledLabel2.setFont(crawledLabel2.getFont().deriveFont(Font.PLAIN));
		constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.insets = new Insets(5, 5, 0, 5);
		layout.setConstraints(crawledLabel2, constraints);
		searchPanel.add(crawledLabel2);
		// Crawled URLs: 끝
		// ////////////////////////////////////////////////////////////////////

		// URLs to Crawl: 시작
		// ////////////////////////////////////////////////////////////////
		JLabel toCrawlLabel1 = new JLabel("URLs to Crawl:");
		constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.EAST;
		constraints.insets = new Insets(5, 5, 0, 0);
		layout.setConstraints(toCrawlLabel1, constraints);
		searchPanel.add(toCrawlLabel1);
		// 여기도 Label이 또 있는 것으로 보아 로봇이 두개 있던지 아니면 두번 돌아 갈 것 같다.
		toCrawlLabel2 = new JLabel();
		toCrawlLabel2.setFont(toCrawlLabel2.getFont().deriveFont(Font.PLAIN));
		constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.insets = new Insets(5, 5, 0, 5);
		layout.setConstraints(toCrawlLabel2, constraints);
		searchPanel.add(toCrawlLabel2);
		// URLs to Crawl: 끝
		// ////////////////////////////////////////////////////////////////////////

		// Crawling Progress: 시작
		// //////////////////////////////////////////////////////////////
		JLabel progressLabel = new JLabel("Crawling Progress:");
		constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.EAST;
		constraints.insets = new Insets(5, 5, 0, 0);
		layout.setConstraints(progressLabel, constraints);
		searchPanel.add(progressLabel);
		// 프로그레스 바
		progressBar = new JProgressBar();
		progressBar.setMinimum(0);
		progressBar.setStringPainted(true);
		constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.insets = new Insets(5, 5, 0, 5);
		layout.setConstraints(progressBar, constraints);
		searchPanel.add(progressBar);
		// Crawling Progress: 끝
		// /////////////////////////////////////////////////////////////////

		// Search Matches: 시작
		// ////////////////////////////////////////////////////////////////
		JLabel matchesLabel1 = new JLabel("Search Matches:");
		constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.EAST;
		constraints.insets = new Insets(5, 5, 10, 0);
		layout.setConstraints(matchesLabel1, constraints);
		searchPanel.add(matchesLabel1);

		matchesLabel2 = new JLabel();
		matchesLabel2.setFont(matchesLabel2.getFont().deriveFont(Font.PLAIN));
		constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.insets = new Insets(5, 5, 10, 5);
		layout.setConstraints(matchesLabel2, constraints);
		searchPanel.add(matchesLabel2);
		// Search Matches: 끝
		// ///////////////////////////////////////////////////////////////////

		// Set up matches table.
		// URL이라고 써져서 결과를 받아 주는 곳 : 테이블로 만들었음 1행 URL 2행 결과 되시겠다~
		table = new JTable(new DefaultTableModel(new Object[][] {},
				new String[] { "URL" }) {
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		});

		// Set up matches panel.
		// Matches라고 써져 있는 곳 주변을 보면 투명 밑줄이 보인다. 또다른 패널을 이곳에 붙이신 성스러운 자국되시겠다.
		// 휴대폰 액정 보호하는 필름 두개 붙었을때 그것도 조금 틀어져서... 이런거 보이는데 똑같다고 판단된다.
		JPanel matchesPanel = new JPanel();
		matchesPanel.setBorder(BorderFactory.createTitledBorder("Matches"));
		matchesPanel.setLayout(new BorderLayout());
		matchesPanel.add(new JScrollPane(table), BorderLayout.CENTER);

		// Add panels to display.
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(searchPanel, BorderLayout.NORTH);
		getContentPane().add(matchesPanel, BorderLayout.CENTER);
	} // ///////////////////////////////////////////////////////////////////////////UI
	// 끝....

	// Exit this program. //프로그램 실행 끝내고 싶을 때 호출
	private void actionExit() {
		System.exit(0);
	}

	// Handle search/stop button being clicked. Search 또는 Stop 버튼이 클릭되면 여기가 호출
	// 된다.
	private void actionSearch() {
		// If stop button clicked, turn crawling flag off.
		if (crawling) {
			crawling = false;
			return;
		}

		ArrayList<String> errorList = new ArrayList<String>(); // 에러를 담는 리스트

		// Validate that start URL has been entered. => startURL에 사용자가 적은 내용을
		// 담는다.
		String startUrl = startTextField.getText().trim();
		// startURL의 유효성 검사
		if (startUrl.length() < 1) {
			errorList.add("Missing Start URL.");
		}
		// Verify start URL.
		else if (verifyUrl(startUrl) == null) {
			errorList.add("Invalid Start URL.");
		}

		// Validate that max URLs is either empty or is a number.
		// => 사용자가 Max URLs to Crawl에 적은 내용을 담는다.
		int maxUrls = 0;
		String max = ((String) maxComboBox.getSelectedItem()).trim();
		if (max.length() > 0) {
			try {
				maxUrls = Integer.parseInt(max);
			} catch (NumberFormatException e) {
			}
			if (maxUrls < 1) {
				errorList.add("Invalid Max URLs value.");
			}
		}

		// Validate that matches log file has been entered.
		// Matches Log File에 적힌 내용을 담는다.
		String logFile = logTextField.getText().trim();
		if (logFile.length() < 1) {
			errorList.add("Missing Matches Log File.");
		}

		// Validate that search string has been entered.
		// 사용자가 Search String에 적은 내용을 담는다.
		String searchString = searchTextField.getText().trim();
		if (searchString.length() < 1) {
			errorList.add("Missing Search String.");
		}

		// Show errors, if any, and return.//에러메세지 출력 
		if (errorList.size() > 0) {
			StringBuffer message = new StringBuffer();//에러메세지를 담을 버퍼 설정

			// Concatenate errors into single message. //에러 리스트를 하나의 메세지로 만들어
			// 버퍼에 담음
			for (int i = 0; i < errorList.size(); i++) {
				message.append(errorList.get(i));
				if (i + 1 < errorList.size()) {
					message.append("\n");
				}
			}

			showError(message.toString());
			return;
		}

		// Remove "www" from start URL if present. 
		startUrl = removeWwwFromUrl(startUrl); //removeWwwFromUrl() 호출 

		// Start the search crawler.
		search(logFile, startUrl, maxUrls, searchString); //search() 호출
	}

	private void search(final String logFile, final String startUrl,
			final int maxUrls, final String searchString) {
		// Start the search in a new thread.
		Thread thread = new Thread(new Runnable() { //thread 생성
			public void run() { //thread 시작 
				// Show hour glass cursor while crawling is under way.
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));//모래시계 커서로 바뀜

				// Disable search controls. //로봇활동중에 입력을 금지 시킴 
				startTextField.setEnabled(false);
				maxComboBox.setEnabled(false);
				limitCheckBox.setEnabled(false);
				logTextField.setEnabled(false);
				searchTextField.setEnabled(false);
				caseCheckBox.setEnabled(false);

				// Switch search button to "Stop." //로봇 활동 중에 search 버튼을 stop버튼으로 바꿔줌
				searchButton.setText("Stop");

				// Reset stats.
				table.setModel(new DefaultTableModel(new Object[][] {},//URL아래 Table에 관한 세팅
						new String[] { "URL" }) {
					public boolean isCellEditable(int row, int column) {
						//넘오는 값에 상관없이 테이블에 값이 나타나는 것을 방지 -> 값이 이상하면 false
						return false;
					}
				});
				updateStats(startUrl, 0, 0, maxUrls); //updateStates호출 

				// Open matches log file. //로그파일을 적기 위한 준비 
				try {
					logFileWriter = new PrintWriter(new FileWriter(logFile));
				} catch (Exception e) {
					showError("Unable to open matches log file.");
					return;
				}

				// Turn crawling flag on. //flag로 로봇의 활동을 알림 표시 
				crawling = true;

				// Perform the actual crawling.//crawl() 호출 
				try {
					crawl(startUrl, maxUrls, limitCheckBox.isSelected(), //is selected는 체크 상태를 확인하는 것
							searchString, caseCheckBox.isSelected());
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				// Turn crawling flag off. //flag로 로봇의 활동이 중지되었음을 알린다. 
				crawling = false;

				// Close matches log file. //로그파일을 쓰기위한 writer를 닫는다. 
				try {
					logFileWriter.close();
				} catch (Exception e) {
					showError("Unable to close matches log file.");
				}

				// Mark search as done. // 숨겨진 라벨에 활동이 끝났음을 'done'으로 알림 ㄴ
				crawlingLabel2.setText("Done");

				// Enable search controls. //사용자가 텍스트 필드를 사용할 수 있도록 활성화 함 
				startTextField.setEnabled(true);
				maxComboBox.setEnabled(true);
				limitCheckBox.setEnabled(true);
				logTextField.setEnabled(true);
				searchTextField.setEnabled(true);
				caseCheckBox.setEnabled(true);

				// Switch search button back to "Search." //버튼을 stop -> search로 바꿈 
				searchButton.setText("Search");

				// Return to default cursor.
				setCursor(Cursor.getDefaultCursor());//커서를 모래시계에서 일반 커서로 바꿈 

				// Show message if search string not found.
				if (table.getRowCount() == 0) { //검색한 것이 없을 경우의 출력 
					JOptionPane
					.showMessageDialog(
							SearchCrawler.this,
							"Your Search String was not found. Please try another.",
							"Search String Not Found",
							JOptionPane.WARNING_MESSAGE);
				}
			}
		});
		thread.start();
	}

	// Show dialog box with error message.
	private void showError(String message) {
		JOptionPane.showMessageDialog(this, message, "Error",
				JOptionPane.ERROR_MESSAGE);
	}

	// Update crawling stats.
	private void updateStats(String crawling, int crawled, int toCrawl,
			int maxUrls) { //생성자 UI 세팅할때 라벨이 왜 2개씩인가에 대한 비밀이 여기서 풀린다. 
						   // 스레드 상태에서 표시되는 라벨이 하나 더 있다. 
		//=> 텍스트필드로 만들면 사용자가 입력하기때문에 이것을 방지하기 위해 라벨로 만듬 
		crawlingLabel2.setText(crawling);
		crawledLabel2.setText("" + crawled);
		toCrawlLabel2.setText("" + toCrawl);

		// Update progress bar. //프로그레스 바의 최대값 설정 
		if (maxUrls == -1) { //최대 값보다 작다면 수집된 수 + 수집될 수 
			// 미리 수집될 정보가 몇개인지 파악이 가능한 듯 하다. 
			progressBar.setMaximum(crawled + toCrawl);
		} else { //위의 경우가 아닐경우 사용자 지정 최대수로 함
			progressBar.setMaximum(maxUrls); 
		}
		progressBar.setValue(crawled); //수집된 갯수를 프로그래스바를 세팅 

		matchesLabel2.setText("" + table.getRowCount());// 행을 증가 시킬 때 줄을 긋는다. 
	}

	// Add match to matches table and log file.
	private void addMatch(String url) { //테이블에 log를 출력한다. 
		// Add URL to matches table.
		DefaultTableModel model = (DefaultTableModel) table.getModel();
		model.addRow(new Object[] { url });

		// Add URL to matches log file.
		try {
			logFileWriter.println(url);
		} catch (Exception e) {
			showError("Unable to log match.");
		}
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
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					pageUrl.openStream(),"EUC-KR"));  //EUC-KR
			
			// Read page into buffer.
			String line = null;  //Each Line in HTML 
			StringBuffer pageBuffer = new StringBuffer();
			
			while ((line = reader.readLine()) != null) {
				pageBuffer.append(line);
			}

			return pageBuffer.toString();
		} catch (Exception e) {
		}

		return null;
	}

	// Remove leading "www" from a URL's host if present.
	// removeWwwFromUrl()을 호출하여 입력한　url에서 www까지 삭제
	private String removeWwwFromUrl(String url) {
		int index = url.indexOf("://www.");
		if (index != -1) {
			return url.substring(0, index + 3) + url.substring(index + 7);
		} //(예) http://www.daum.net=> http://daum.net
		return (url);
	}

	// Parse through page contents and retrieve links.
	private ArrayList<String> retrieveLinks(URL pageUrl, String pageContents,
			HashSet crawledList, boolean limitHost) {
		// Compile link matching pattern.
		Pattern p = Pattern.compile("<a\\s+href\\s*=\\s*\"?(.*?)[\"|>]", //<a href=""> 이런 것을 걸러냄
				Pattern.CASE_INSENSITIVE); //String이 컴파일될때 패턴을 지정 -> JAVA의 정규표현식 
		Matcher m = p.matcher(pageContents); //정규표현식에 맞는지 매칭시켜본다. 

		// Create list of link matches.
		ArrayList<String> linkList = new ArrayList<String>();
		while (m.find()) { //매치가 되면 계속 돌아간다. 
			String link = m.group(1).trim(); // s.substring(m.start(1), m.end(1))와 같은 표현 
											 // 매칭된 것  중 첫번째에서 link만  잘라내어 저장 
			// Skip empty links. //링크가 비어있다면 
			if (link.length() < 1) {
				continue;
			}

			// Skip links that are just page anchors.
			if (link.charAt(0) == '#') { //#을 쓴 링크라면 
				continue;
			}

			// Skip mailto links.
			if (link.indexOf("mailto:") != -1) {//메일이 link되어 있다면 
				continue;
			}

			// Skip JavaScript links. //javascript가 link 되어 있다면 
			if (link.toLowerCase().indexOf("javascript") != -1) {
				continue;
			}

			// Prefix absolute and relative URLs if necessary.
			if (link.indexOf("://") == -1) { // 이런 형식(://)이 없다면 
				// Handle absolute URLs.
				if (link.charAt(0) == '/') { // 슬래쉬(/)로 시작하는 절대 url일 경우 (예) /blog/hagi
					link = "http://" + pageUrl.getHost() + link; //앞에 http://를 붙인다. 
					// Handle relative URLs.
				} else { //상대 경로 
					String file = pageUrl.getFile();
					if (file.indexOf('/') == -1) { /* index.html과 같은 경우 (슬래쉬가 완전 없는 경우)*/ 
						link = "http://" + pageUrl.getHost() + "/" + link;
					} else { /* blog/hagi 같이 슬래쉬(/)를 쓰지 않은 경우  */
						String path = file.substring(0,
								file.lastIndexOf('/') + 1);
						link = "http://" + pageUrl.getHost() + path + link;
					}
				}
			}

			// Remove anchors from link. //link에 #이 있다면 그전까지 자른다. 
			int index = link.indexOf('#');
			if (index != -1) {
				link = link.substring(0, index);
			}

			// Remove leading "www" from URL's host if present.
			link = removeWwwFromUrl(link); //www를 잘라냄 

			// Verify link and skip if invalid.
			URL verifiedLink = verifyUrl(link);
			if (verifiedLink == null) {
				continue;
			}

			/*
			 * If specified, limit links to those having the same host as the
			 * start URL.
			 *    나의 로봇 시작 페이지가 http://daum.net이고  
			 */ //abc.html의 상위주소가 http://daum.net이라면 이것은 걸러낸다. 
			if (limitHost
					&& !pageUrl.getHost().toLowerCase().equals(
							verifiedLink.getHost().toLowerCase())) {
				continue;
			}

			// Skip link if it has already been crawled.
			if (crawledList.contains(link)) { //이미 crawledList에서 수집된 정보라면 넘어간다. 
				continue;
			}

			// Add link to list.
			linkList.add(link); //모두 아니라면 linkList에 저장 
		}

		return (linkList);
	}

	/*
	 * Determine whether or not search string is matched in the given page
	 * contents.
	 */
	private boolean searchStringMatches(String pageContents,
			String searchString, boolean caseSensitive) {
		String searchContents = pageContents;

		/*
		 * If case sensitive search, lowercase page contents for comparison.
		 */
		if (!caseSensitive) { //대소문자를 구분 안할때 
			searchContents = pageContents.toLowerCase();
		}

		// Split search string into individual terms.
		Pattern p = Pattern.compile("[\\s]+"); //공백일때를 기준으로 패턴을 준다. 
		String[] terms = p.split(searchString); //공백을 기준으로 자른다. 
		
		// Check to see if each term matches.
		for (int i = 0; i < terms.length; i++) { //페이지 내용에 찾을 단어가 있는지 판단하여 리턴 
			if (caseSensitive) {
				if (searchContents.indexOf(terms[i]) == -1) {
					return false;
				}
			} else {
				if (searchContents.indexOf(terms[i].toLowerCase()) == -1) {
					return false;
				}
			}
		}

		return true;
	}

	// Perform the actual crawling, searching for the search string.
	public void crawl(String startUrl, int maxUrls, boolean limitHost, //로봇이 본격적으로 활동 
			String searchString, boolean caseSensitive) throws IOException {
		// Setup crawl lists. -> 수집 정보를 기록하기 위한 HashSet 설정 
		HashSet crawledList = new HashSet(); //수집이 끝난 것을 기록하기 위한 것 
		LinkedHashSet toCrawlList = new LinkedHashSet(); //앞으로 수집할 것을 기록하기 위한 것 

		// Add start URL to the to crawl list.
		toCrawlList.add(startUrl); //수집하기 위한 리스트에 사용자가 출발점으로 지정한 사이트를 기록

		File save_txt2 = new File("C:/Users/Administrator/Desktop/link.txt"); // 저장 될 파일명
        PrintWriter pw2 = new PrintWriter(new FileWriter(save_txt2,true));
		/*
		 * Perform actual crawling by looping through the to crawl list.
		 */
		while (crawling && toCrawlList.size() > 0) { 
			// 수집이 끝났다는 신호가 있거나 앞으로 조사할 리스트가 0이상일 때까지 반복~
			/*
			 * Check to see if the max URL count has been reached, if it was
			 * specified.
			 */
			if (maxUrls != -1) { //최대 url갯수가 설정 되어 있다면 
				if (crawledList.size() == maxUrls) { //수집된 리스트의 갯구와 최대 url수가 같을때 중지!!
					break;
				}
			}

			// Get URL at bottom of the list. //앞으로 조사할 리스트의 다음 값을 url에 저장
			String url = (String) toCrawlList.iterator().next();

			// Remove URL from the to crawl list.//앞으로 조사할 리스트의 값을 지운다.
			toCrawlList.remove(url);             //사용자 지정 값이 1개이면 여기서 지우면 0이 될 것임

			// Convert string url to URL object. //위에서 입력받은 url에 대한 검사를 하는 것 같음
			URL verifiedUrl = verifyUrl(url);  //URL이라는 클래스는 java에 기본 내장 되어 있음 
											//RFC 2396: Uniform Resource Identifiers (URI): Generic Syntax, 
											//상기 규정에 맞는지 검사 
			// Skip URL if robots are not allowed to access it.
			if (!isRobotAllowed(verifiedUrl)) {//규정에 맞지 않으면 수집을 하지 않고 다음으로 넘어 간다. 
				continue;
			}

			// Update crawling stats. //규정에 맞다면 로봇 상태를 갱신한다. 
			updateStats(url, crawledList.size(), toCrawlList.size(), maxUrls);

			// Add page to the crawled list. //수집된 리스트에 url을 넣는다. 
			crawledList.add(url);

			// Download the page at the given url. //downloadpage를 호출,수행하여 리턴값을 pageContent에 넣음
			String pageContents = downloadPage(verifiedUrl); //페이지를 전부 읽어 들여 저장
			File save_txt = new File("C:/Users/Administrator/Desktop/test.txt"); // 저장 될 파일명
            PrintWriter pw = new PrintWriter(new FileWriter(save_txt,true));
            pw.println(pageContents); 
			/*
			 * If the page was downloaded successfully, retrieve all of its
			 * links and then see if it contains the search string.
			 */
			if (pageContents != null && pageContents.length() > 0) { //페이지의 내용이 있을 경우 
				// Retrieve list of valid links from page.
				ArrayList<String> links = retrieveLinks(verifiedUrl, pageContents,
						crawledList, limitHost); 

				// Add links to the to crawl list.
				toCrawlList.addAll(links); //수집한 모든 link를 toCrawlList에 담는다. 

				pw2.println("from " + verifiedUrl);
				for(String i: links){
					pw2.println("   to " + i);
				}
				/*
				 * Check if search string is present in page and if so record a
				 * match.
				 */
				if (searchStringMatches(pageContents, searchString,
						caseSensitive)) { 
					addMatch(url); //페이지 내용에 찾을 단어 가 있다면 url을 테이블에서 출력한다. 
				}
			}

			// Update crawling stats. //숨겨진 라벨과 프로그레스바를 업데이트한다. 
			updateStats(url, crawledList.size(), toCrawlList.size(), maxUrls);
		}
	}

	// Run the Search Crawler.
	public static void main(String[] args) {
		SearchCrawler crawler = new SearchCrawler();
		crawler.show();
	}
	
}
