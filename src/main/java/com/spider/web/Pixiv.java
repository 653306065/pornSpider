package com.spider.web;

import java.net.Proxy;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.spider.utils.OKHttpUtils;
import com.spider.utils.download.ImageDownload;

@Service
public class Pixiv {

	private Logger logger = LoggerFactory.getLogger(Pixiv.class);

	@Value("${pixiv.todayRank.R-18}")
	private String R18Url;

	@Value("${pixiv.cookie}")
	private String cookie;

	@Value("${pixiv.home}")
	private String home;

	@Autowired
	Proxy proxy;

	@Value("${pixiv.savePath}")
	private String savePath;

	@Autowired
	ImageDownload imageDownload;

	@Value("${pixiv.thread}")
	private int thread;

	SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");

	public List<String> getHistoryRankListUrl(Date date) {
		List<String> urlList = new ArrayList<String>();
		String dateStr = simpleDateFormat.format(date);
		String url = R18Url.replace("@{date}", dateStr).replace("@{page}", String.valueOf(1));
		Map<String, String> header = new HashMap<>();
		header.put("cookie", cookie);
		String json = OKHttpUtils.get(url, header, proxy);
		JSONObject jsonObject = JSON.parseObject(json);
		JSONArray jsonArray = jsonObject.getJSONArray("contents");
		for (int i = 0; i < jsonArray.size(); i++) {
			logger.info(jsonArray.getJSONObject(i).getString("url"));
			String imageurl = jsonArray.getJSONObject(i).getString("url").replace("c/240x480/img-master", "img-original").replace("_master1200", "");
			logger.info(imageurl);
			urlList.add(imageurl);
		}
		
		String page2 = R18Url.replace("@{date}", dateStr).replace("@{page}", String.valueOf(2));
		String json2 = OKHttpUtils.get(url, header, proxy);
		JSONObject jsonObject2 = JSON.parseObject(json2);
		JSONArray jsonArray2 = jsonObject.getJSONArray("contents");
		for (int i = 0; i < jsonArray2.size(); i++) {
			logger.info(jsonArray.getJSONObject(i).getString("url"));
			String imageurl = jsonArray.getJSONObject(i).getString("url").replace("c/240x480/img-master", "img-original").replace("_master1200", "");
			logger.info(imageurl);
			urlList.add(imageurl);
		}
		return urlList;
	}

	public void downloadHistoryR18() {
		Date date = new Date(110, 0, 0);
		while (true) {
			try {
				ExecutorService executorService = Executors.newFixedThreadPool(thread);;
				String dateStr = simpleDateFormat.format(date);
				List<String> urls = getHistoryRankListUrl(date);
				for (String url : urls) {
					executorService.execute(new Runnable() {
						@Override
						public void run() {
							Map<String, String> header = new HashMap<>();
							header.put("Referer", "https://www.pixiv.net/ranking.php?mode=daily&content=illust");
							imageDownload.downloadFile(url, header,
									savePath + "\\" + dateStr + "\\" + UUID.randomUUID() + ".jpg", proxy);
						}
					});
				}
				executorService.shutdown();
				while(true) {
					if(executorService.isTerminated()) {
						logger.info("{},下载完成",dateStr);
						break;
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			date.setTime(date.getTime() + 1000 * 60 * 60 * 24);
		}
	}
}
