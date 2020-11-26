import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CrawlerConfig {

	String pageUrl;
	String siteName;	
	boolean scrollYes;
	WebDriver driver;
	
	//states 구성: [[0: action_type, 1: action_val, 2: xpath], ]
	List<List<List<String>>> states;
	List<String> state_type;
	List<String> urls;
	
	String defaultDir = "";
	String filePath = "";
	String configPath = "";
	
	CrawlerConfig() throws ParserConfigurationException, SAXException, IOException{
		
		getFileLocation();
		getConfig();
		getURLs();
		makeDefaultDirectory();
		
		//driver설정
		System.setProperty("webdriver.chrome.driver", "D:\\김다예\\숙명여대\\2학년 겨울방학\\java\\SeleniumDemo\\exefiles\\chromedriver.exe");
		ChromeOptions options = new ChromeOptions();
        options.addArguments("start-maximized");
        options.addArguments("disable-infobars");
        options.addArguments("--disable-extensions"); 
		driver = new ChromeDriver(options);
		
		driver.get(pageUrl);
	}
	
	@SuppressWarnings("null")
	private void getConfig() throws ParserConfigurationException, SAXException, IOException  {
		states = new ArrayList();
		state_type = new ArrayList();
		
		File xmlFile = new File(configPath);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(xmlFile);
		
		Element root = doc.getDocumentElement();
		NodeList stateList = root.getElementsByTagName("state");
		
		siteName = root.getAttribute("filename");
		scrollYes = getAttr("scroll", root).equals("true") ? true : false; 
		pageUrl = root.getAttribute("originUrl");
		
		if(stateList.getLength() == 0) return;
		
		for(int i = 0; i < stateList.getLength(); i++) {
			Node state = stateList.item(i);
			Element s = (Element) state;
			
			List<List<String>> stateL = new ArrayList();
			state_type.add(getAttr("type", s));	
			
			NodeList actionList = s.getElementsByTagName("action");
			for(int j = 0; j < actionList.getLength(); j++) {
				Node action = actionList.item(j);
				Element a = (Element) action;
				
				List<String> actionL = new ArrayList();
				actionL.add(getAttr("type", a));
				actionL.add(getAttr("val", a));
				actionL.add(getTagValue(a));
				
				stateL.add(actionL);
			}
			states.add(stateL);
		}
	}
	
	private String getTagValue(Element element) {
		String result = element.getTextContent();
        return result;
	}
	
	private String getAttr(String attrName, Element element) {
		if(element.getAttributes().getNamedItem(attrName) != null) {
			return element.getAttributes().getNamedItem(attrName).getNodeValue();
		} else
			return null;
	}
	
	private void getURLs() {
		urls = new ArrayList();
		try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
			String line;
			while ((line = br.readLine()) != null) {
				System.out.println(line);
				urls.add(line);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void makeDefaultDirectory() {
		
		File newDir1 = new File(defaultDir);
		defaultDir = defaultDir + "//" + siteName;
		File newDir2 = new File(defaultDir);

		if (!newDir1.exists()) {
		    System.out.println("creating directory: " + newDir1.getName());
		    boolean result = false;

		    try{
		    	newDir1.mkdir();
		        result = true;
		    } 
		    catch(SecurityException se){    }       
		    if(result) {    
		        System.out.println("DIR created");  
		    }
		} 
		
		if(!newDir2.exists()) {
		    System.out.println("creating directory: " + newDir2.getName());
		    boolean result = false;

		    try{
		    	newDir2.mkdir();
		        result = true;
		    } 
		    catch(SecurityException se){    }        
		    if(result) {    
		        System.out.println("DIR created");  
		    }
		}
	}
	
	private void getFileLocation() {
		
		JFileChooser chooser = new JFileChooser();
		
		while(true) {
			JOptionPane.showMessageDialog(null, "기본 디렉터리를 선택해주세요.");
			chooser.setDialogTitle("choose default directory");
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.showSaveDialog(null);		
			if(chooser.getSelectedFile() == null) continue;
			
			defaultDir = chooser.getSelectedFile().toPath().toString();
			convertPath(0);
			System.out.println(defaultDir);
			break;
		}
		
		FileNameExtensionFilter xmlfilter = new FileNameExtensionFilter("xml files (*.xml)", "xml");
		chooser.setFileFilter(xmlfilter);
		
		while(true) {
			JOptionPane.showMessageDialog(null, "config 파일을 선택해주세요.");
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			chooser.setDialogTitle("choose config file");
			chooser.showSaveDialog(null);		
			if(chooser.getSelectedFile() == null) continue;
			
			configPath = chooser.getSelectedFile().toPath().toString();
			convertPath(1);
			System.out.println(configPath);
			break;
		}
		
		FileNameExtensionFilter filter = new FileNameExtensionFilter("TEXT FILES", "txt", "text");
		chooser.setFileFilter(filter);
		
		while(true) {
			JOptionPane.showMessageDialog(null, "url 파일을 선택해주세요.");
			chooser.setDialogTitle("choose url file");
			chooser.showSaveDialog(null);		
			if(chooser.getSelectedFile() == null) continue;
			
			filePath = chooser.getSelectedFile().toPath().toString();
			convertPath(2);
			System.out.println(filePath);
			break;
		}
		
	}
	
	private void convertPath(int flag) {
		String path = "";
		
		switch(flag) {
		case 0:
			path = defaultDir;
			break;
		case 1:
			path = configPath;
			break;
		case 2:
			path = filePath;
			break;
		}
	
		for(int i = 0; i < path.length(); i++) {
			if(path.charAt(i) == '\\') {
				path = path.substring(0, i)+ "//" + path.substring(i+1);
			}
		}

		switch(flag) {
		case 0:
			defaultDir = path;
			break;
		case 1:
			configPath = path;
			break;
		case 2:
			filePath = path;
			break;
		}
	}
}
