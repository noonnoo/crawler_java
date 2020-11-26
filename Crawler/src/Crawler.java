import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.List;

import javax.imageio.ImageIO;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;

public class Crawler {
	static private char[] notUse = {'<', '>' ,':' ,'"' ,'/' ,'|' , '?', '*', ' ' };
	
	String title;
	String defaultDir;
	String dir;
	String defaultUrl;
	
	WebDriver driver;
	int imageNum = 0, detailNum=0;
	static int titleNum = 0;
	boolean scrollYes;
	
	List<List<List<String>>> states;		//states 구성: [[0: action_type, 1: action_val, 2: xpath], ] --> 이게 한 스테이트
	List<String> state_type;
	List<String> urls;
	
	Crawler(CrawlerConfig cc) throws InterruptedException{
		
		this.driver = cc.driver;	
		this.dir = cc.defaultDir;
		defaultDir = cc.defaultDir;
		
		this.defaultUrl = cc.pageUrl;
		this.scrollYes = cc.scrollYes;
		
		this.states = cc.states;
		this.state_type = cc.state_type;
		this.urls = cc.urls;

		try {
			divideState();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//javascript 가동 설정
		new WebDriverWait(driver, 100).until(
		          webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
		
		driver.quit();
	}
	
	private void divideState() throws InterruptedException, IOException {
		
		for(int i = 0; i < states.size(); i++) {
			String type, val, xpath;
			
			if(state_type.get(i).equals("login")) {		
				for(int j = 0; j < states.get(i).size(); j++) {
					type = states.get(i).get(j).get(0);
					val = states.get(i).get(j).get(1);
					xpath = states.get(i).get(j).get(2);
					
					commitAction(type, val, xpath);
				}
			} else if(state_type.get(i).equals("download")) {
				
				for(int k= 0; k < urls.size(); k++) {
					driver.get(urls.get(k));
					System.out.println(urls.get(k));

					getTitle();
					makeDirectory("");
					imageNum = 0;
					detailNum=0;
					
					Thread.sleep(2000);
					Screenshot screenshot= new AShot().shootingStrategy(ShootingStrategies.viewportPasting(300)).takeScreenshot(driver);
					ImageIO.write(screenshot.getImage(),"PNG",new File(dir+"//screenshot.png"));
					
					if(scrollYes) {
						Thread.sleep(5000);
						scrollDown();
					}	
					
					for(int j = 0; j < states.get(i).size(); j++) {
						type = states.get(i).get(j).get(0);
						val = states.get(i).get(j).get(1);
						xpath = states.get(i).get(j).get(2);
						
						commitAction(type, val, xpath);
					}
					
					dir = defaultDir;
				}
				
			}
		}
	}
	
	private void commitAction(String type, String val, String xpath) {
		if(type.equals("click")) {
			actionClick(xpath);
		} else if(type.equals("input")) {
			actionInput(xpath, val);
		} else if(type.equals("txt") || type.equals("img")) {
			try {
				actionDownload(xpath, type, val, 0);
			} catch (IndexOutOfBoundsException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void actionDownload(String xpath, String type, String val, int depth) throws IndexOutOfBoundsException{
		if(depth > 3) return;
	
		
		if(existsElement(xpath)) {
			if(type.equals("img")) {
				actionDownImg(xpath);
			} else if(type.equals("txt")) {
				actionDownTxt(xpath, val);
			}
		}
		
		if(!scrollYes) {
			//System.out.println(driver.findElements(By.xpath(xpath)).size());
			if(existsElement(xpath)) {										//해당 xpath가 많이 있다면 
				if(existsElement(xpath+"//iframe")) {						//그리고 그아래 iframe이 있다면 다 들어가본다.
					for(int i = 0; i < driver.findElements(By.xpath(xpath+"//iframe")).size(); i++) {	
						driver.switchTo().frame(driver.findElements(By.xpath(xpath+"//iframe")).get(i));
						System.out.println("here2: " + driver.getCurrentUrl());
						actionDownload(xpath+"//ifrmae", type,val, depth+1);
						driver.switchTo().defaultContent();
					}
				}
				
			} else {
				if(existsElement("//iframe")) {
					System.out.println("here1: " +"//iframe  " + depth);
					for(int i = 0; i < driver.findElements(By.xpath("//iframe")).size(); i++) {		//있는 iframe 다 들어가보기
						driver.switchTo().frame(driver.findElements(By.xpath("//iframe")).get(i));
						actionDownload("//ifrmae", type,val, depth+1);
						driver.switchTo().defaultContent();
					}
				}
			}			
		}
				
	}
	
	private boolean existsElement(String xpath) {
	    try {
	        driver.findElement(By.xpath(xpath));
	    } catch (Exception e) {
	        return false;
	    }
	    return true;
	}
	
	private void getTitle() {		
		this.title = driver.getTitle();
		for(int i = 0; i < title.length(); i++) {
			for(int j = 0 ; j < notUse.length; j++) {
				if(title.charAt(i) == notUse[j]) {
					title = title.substring(0, i) + '_' + title.substring(i+1);
				}
			}
		}
		dir += "//" + this.title + Integer.toString(titleNum);
		titleNum++;
	}
	
	private void actionInput(String xpath, String val) {
		driver.findElement(By.xpath(xpath)).sendKeys(val);
	}
	
	private void actionClick(String xpath) {
		driver.findElement(By.xpath(xpath)).click();
	}
	
	private void actionDownTxt(String xpath, String val) {
		String content = "";
		if(xpath == "")
			xpath += "//*";
		
		List<WebElement> details = driver.findElements(By.xpath(xpath));
		
		for(int i = 0 ;i < details.size(); i++) {
			String tmp = details.get(i).getText().toString() == "" ? "" : details.get(i).getText().toString() + "\r\n";
			content += tmp;
		}
		
		System.out.println(content);
		if(content != "") {
			try (PrintWriter out = new PrintWriter(dir + "//" + val + detailNum + ".txt")) {
			    out.println(content);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			detailNum++;
		}
	}
	
	private void actionDownImg(String xpath) {
		xpath += "//descendant::img";		
		List<WebElement> description = driver.findElements(By.xpath(xpath));
		
		String last = "";
		for(int i = 0; i < description.size(); i++) {
			String tmp = description.get(i).getAttribute("src");
			
			System.out.println(tmp);			
			saveImage(tmp, dir);
			last = tmp;
		}
	}
	
	//만들 디렉터리 인자로주면 됨
	private void makeDirectory(String dir) {
		
		File newDir1 = new File(this.dir + dir);
		
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
	}
	
	//parameter: 이미지 url과 저장할 디렉터리
	private void saveImage(String imageUrl, String destinationFile) {
		InputStream is = null;
		destinationFile += "//image" + imageNum + ".gif";
		
		try {
			URL url = new URL(imageUrl);
			is = url.openStream();
			OutputStream os = new FileOutputStream(destinationFile);

			byte[] b = new byte[2048];
			int length;

			while ((length = is.read(b)) != -1) {
				os.write(b, 0, length);
			}

			is.close();
			os.close();
		} 
		catch (FileNotFoundException fe){
		}
		catch (IOException ie) {
		}	
		imageNum++;
	}
	
	private void scrollDown() throws InterruptedException {
		JavascriptExecutor jsx = (JavascriptExecutor)driver;
		
		while(true) {
			Object current_pos =  jsx.executeScript("return window.pageYOffset;");
			Object end_pos = jsx.executeScript("return document.body.scrollHeight");
			
			((JavascriptExecutor) driver).executeScript("window.scrollBy(0,800)", "");
	        Thread.sleep(1000);
	        
	        double tmp1, tmp2;
	        
	        if(current_pos instanceof Long)
	        	tmp1 = ((Long) current_pos).doubleValue();
	        else
	        	tmp1 = (Double) current_pos;
	        
	        if(end_pos instanceof Long)
	        	tmp2 = ((Long) end_pos).doubleValue();
	        else
	        	tmp2 = (Double) end_pos;
	        	
	        //System.out.println(tmp2 + " " + tmp1);
	        if(tmp2 -2000 <= tmp1)
	        	break;
	    }
	}
}
