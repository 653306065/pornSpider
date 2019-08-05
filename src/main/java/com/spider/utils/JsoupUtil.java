package com.spider.utils;

import java.net.Proxy;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import com.spider.utils.SpringContentUtil;

public class JsoupUtil {

	private static Proxy proxy = SpringContentUtil.getBean(Proxy.class);

	public static Document getDocument(String url) {
		String html=OKHttpUtils.get(url, null);
		Document document=Jsoup.parse(html);
		return document;
	}
	
	public static Document getDocumentByProxy(String url) {
		String html=OKHttpUtils.get(url, proxy);
		Document document=Jsoup.parse(html);
		return document;
	}
	
	public static Document getDocumentByProxy(String url,Map<String,String> header) {
		String html=OKHttpUtils.get(url, header, proxy);
		Document document=Jsoup.parse(html);
		return document;
	}
}