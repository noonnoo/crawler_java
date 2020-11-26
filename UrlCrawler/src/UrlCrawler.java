import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;


public class UrlCrawler {
	String defaultDir;
	String defaultUrl;
	String siteName;
	
	Set<String> URLs;
	String urls = "";
	boolean ismobile;
	
	WebDriver driver;
	
	List<List<List<String>>> states;		//states 구성: [[0: action_type, 1: action_val, 2: xpath], ] --> 이게 한 스테이트
	List<String> state_type;
	
	UrlCrawler(UrlCrawlerConfig ucc){
		URLs = new HashSet<String>();
		this.driver = ucc.driver;	
		defaultDir = ucc.defaultDir;
		
		this.defaultUrl = ucc.pageUrl;		
		this.states = ucc.states;
		this.state_type = ucc.state_type;
		this.siteName = ucc.siteName;
		ismobile = ucc.ismobile;
		
		try {
			divideState();
			writeURL();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		driver.quit();
	}
	
	private void divideState() throws InterruptedException{		
		for(int i = 0; i < states.size(); i++) {			
			
			if(state_type.get(i).equals("loop")) {
				i = commitLoop(i);
			} else if(state_type.get(i).equals("search")) {
				String type, val, xpath;
				
				for(int j = 0; j < states.get(i).size(); j++) {
					type = states.get(i).get(j).get(0);
					val = states.get(i).get(j).get(1);
					xpath = states.get(i).get(j).get(2);
					
					commitAction(type, val, xpath);
				}
			}
		}
	}
	
	private void writeURL() {
		String[] urlArr = URLs.toArray(new String[0]);
		
		for(int i = 0; i < urlArr.length; i++) {
			if(urlArr[i] != "")
				urls += urlArr[i] + "\r\n";
		}
		
		if(urls != "") {
			try (PrintWriter out = new PrintWriter(defaultDir + "//" + siteName + "Url" + ".txt")) {
			    out.println(urls);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	private int commitLoop(int index) throws InterruptedException {		
		String loop_xpath = states.get(index++).get(0).get(1);
		String currUrl = driver.getCurrentUrl();
		List<WebElement> elements = driver.findElements(By.xpath(loop_xpath));
		
		System.out.println(loop_xpath + " " + elements.size());
		System.out.println(states.size() + " " + state_type.size());
		String type, val;		

		for(int i= 0; i < elements.size(); i++) {			
			for(int j= 0; j < states.get(index).size(); j++) {
				type = states.get(index).get(j).get(0);
				val = states.get(index).get(j).get(1);
				
				if(type.equals("loop")) {
					commitLoop(++index);
				} else {
					if(type.equals("click")) {
						driver.findElements(By.xpath(loop_xpath)).get(i).click();
						JavascriptExecutor js = (JavascriptExecutor) driver;
						js.executeScript("return window.stop");
					} else if(type.equals("url")) {
						String url = driver.getCurrentUrl();
						URLs.add(url);
						System.out.println(url);
					} else if(type.equals("goBack")) {
						driver.get(currUrl);
					} else if(type.equals("gethref")) {
						String url = elements.get(i).getAttribute("href");						
						URLs.add(url);
						System.out.println(url);
					}
				}
			}
		}	
		
		return ++index;
	}
	
	private void commitAction(String type, String val, String xpath) {
		if(type.equals("click")) {
			actionClick(xpath);
		} else if(type.equals("input")) {
			actionInput(xpath, val);
		} 
	}
	
	private void actionInput(String xpath, String val) {
		driver.findElement(By.xpath(xpath)).sendKeys(val);
	}
	
	private void actionClick(String xpath) {
		driver.findElement(By.xpath(xpath)).click();
	}
	
}
