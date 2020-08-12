package com.spider.utils.download;

import com.alibaba.fastjson.JSON;
import com.spider.utils.ConsoleProgressBar;
import com.spider.utils.OKHttpUtils;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Scope("prototype")
public class MultithreadingDownload {

    public AtomicLong downloadByte = new AtomicLong(0);

    private final Logger logger = LoggerFactory.getLogger(MultithreadingDownload.class);

    public void fileDownload(String HttpUrl, String path, Map<String, String> header, Proxy proxy, int threadNum, long segmentSize) {
        try {
            long startTime = System.currentTimeMillis();
            File file = createFile(path);
            if(Objects.isNull(file)){
                return;
            }
            DownloadFileInfo info = getDownloadFileInfo(HttpUrl, header, proxy);
            if (Objects.isNull(info) || !String.valueOf(info.getResponseCode()).startsWith("20")) {
                logger.info("----获取下载信息错误：responseCode=" + info.getResponseCode() + "----");
                return;
            } else {
                logger.info(path + ",开始下载,url:" + HttpUrl);
                String fileSizeStr=info.getContentLength() / 1024.0 / 1024.0+"m";
                logger.info(JSON.toJSONString(info) + ",大小" + fileSizeStr);
                RandomAccessFile raf = new RandomAccessFile(file, "rw");
                raf.setLength(info.getContentLength());
                ExecutorService executorService = Executors.newFixedThreadPool(threadNum);
                int i=0;
                while (true){
                    long startByte = i * segmentSize;
                    long endByte = (i + 1) * segmentSize - 1;
                    if(endByte >= info.getContentLength()){
                        DownloadThread thread = new DownloadThread(HttpUrl, header, proxy, startByte, info.getContentLength(), file, downloadByte);
                        executorService.execute(thread);
                        break;
                    }
                    DownloadThread thread = new DownloadThread(HttpUrl, header, proxy, startByte, endByte, file, downloadByte);
                    executorService.execute(thread);
                    i++;
                }
                executorService.shutdown();
                ConsoleProgressBar cpb = new ConsoleProgressBar(100, '#');
                while (true) {
                    double percentage = (downloadByte.longValue() * 1.0) / (info.getContentLength() * 1.0) * 100.0;
                    cpb.show((int) Math.floor(percentage));
                    if (String.valueOf(percentage).length() > 5) {
                        System.out.print("(" + String.valueOf(percentage).substring(0, 5) + "%),"+fileSizeStr);
                    } else {
                        System.out.print("(" + String.valueOf(percentage) + "%),"+fileSizeStr);
                    }
                    Thread.sleep(10);
                    if (executorService.isTerminated()) {
                        break;
                    }
                }
                downloadByte.set(0);
                logger.info("//r----" + path + ",下载完成----");
                long endTime = System.currentTimeMillis();
                logger.info("耗时:" + (endTime - startTime) / 1000 / 60.0 + "分钟");
                raf.close();
            }
        } catch (Exception e) {
            downloadByte.set(0);
            logger.info("----下载异常----");
            new File(path).delete();
            e.printStackTrace();
        }
    }


    private File createFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            logger.info(path + ",已存在");
            return null;
        }
        file.getParentFile().mkdirs();
        return file;
    }


    private DownloadFileInfo getDownloadFileInfo(String HttpUrl, Map<String, String> header, Proxy proxy) {
        try {
            Response response = OKHttpUtils.getResponse(HttpUrl, header, proxy);
            DownloadFileInfo fileInfo = new DownloadFileInfo();
            if (Objects.nonNull(response.header("Content-Length"))) {
                fileInfo.setContentLength(Long.valueOf(response.header("Content-Length")));
            }
            fileInfo.setContentType(response.header("Content-Type"));
            fileInfo.setResponseCode(response.code());
            fileInfo.setEtag(response.header("etag"));
            response.body().close();
            response.close();
            return fileInfo;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
