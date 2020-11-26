import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

public class UrlCrawlMain {

	public static void main(String[] args) {
		try {
			new UrlCrawler(new UrlCrawlerConfig());
		} catch (ParserConfigurationException | SAXException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
