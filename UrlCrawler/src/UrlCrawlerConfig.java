import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.openqa.selenium.By;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class UrlCrawlerConfig {
	String pageUrl;
	String siteName;	
	boolean ismobile;
	WebDriver driver;
	
	//states 구성: [[0: action_type, 1: action_val], ]
	List<List<List<String>>> states;
	List<String> state_type;
	
	String defaultDir = "";
	String configPath = "";
	
	UrlCrawlerConfig() throws ParserConfigurationException, SAXException, IOException{		
		getFileLocation();
		getConfig(); 
		
		System.setProperty("webdriver.chrome.driver", "D:\\김다예\\숙명여대\\2학년 겨울방학\\java\\SeleniumDemo\\exefiles\\chromedriver.exe");
		if(ismobile) {
			Map<String, String> mobileEmulation = new HashMap<>();
			mobileEmulation.put("deviceName", "Nexus 5");
			ChromeOptions chromeOptions = new ChromeOptions();
			chromeOptions.setExperimentalOption("mobileEmulation", mobileEmulation);
			driver = new ChromeDriver();
		} else {
			driver = new ChromeDriver();
		}
				
		driver.get(pageUrl);	
		
	}
	
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
		pageUrl = root.getAttribute("originUrl");	
		ismobile = root.getAttribute("mobile").equals("true") ? true : false;	
		
		if(stateList.getLength() == 0) return;
		
		for(int i = 0; i < stateList.getLength(); i++) {
			Node state = stateList.item(i);
			Element s = (Element) state;
			getActions(s);
		}		
	}
	
	private void getActions(Element element) {
		state_type.add(getAttr("type", element));	
		NodeList nodeList = element.getChildNodes();
		List<List<String>> stateL = new ArrayList();
		
		for(int i = 0; i < nodeList.getLength(); i++) {
			Node action = nodeList.item(i);			

			if(action.getNodeName().equals("#text")) continue;
			Element a = (Element) action;							
			
			List<String> actionL = new ArrayList();
			actionL.add(getAttr("type", a));
			actionL.add(getAttr("val", a));
			actionL.add(getTagValue(a));
			stateL.add(actionL);
			
			if(getAttr("type", a).equals("loop")) {			
				states.add(stateL);								
				getActions(a);
				
				state_type.add("loop_end");
				states.add(null);
				return;
			} 
		}
		
		states.add(stateL);
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

		FileNameExtensionFilter filter = new FileNameExtensionFilter("xml files (*.xml)", "xml");
		chooser.setFileFilter(filter);
		
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
		}
	}
}
