package com.spider.service;

import com.alibaba.fastjson.JSON;
import com.spider.entity.FaceInfo;
import com.spider.utils.FFmpegUtil;
import com.spider.utils.FaceUtil;
import com.spider.utils.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import com.spider.entity.Video;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class VideoService extends BaseService<Video> {

    @Autowired
    private MongoTemplate mongoTemplate;

    public Video findByName(String name) {
        Query query = new Query(Criteria.where("name").is(name));
        return mongoTemplate.findOne(query, Video.class);
    }

    public Video findByMd5(String md5) {
        Query query = new Query(Criteria.where("md5").is(md5));
        return mongoTemplate.findOne(query, Video.class);
    }

    public Video findByVideoUrl(String videoUrl) {
        Query query = new Query(Criteria.where("videoUrl").is(videoUrl));
        return mongoTemplate.findOne(query, Video.class);
    }

    public Video findBySourceUrl(String sourceUrl) {
        Query query = new Query(Criteria.where("sourceUrl").is(sourceUrl));
        return mongoTemplate.findOne(query, Video.class);
    }

    public Double videoScore(String videoId, Integer imageCount, double threshold,boolean isDelete) {
        Video findVideo = this.findById(videoId);
        if (Objects.nonNull(findVideo)) {
            long duration = findVideo.getMultimediaInfo().getDuration();
            long size = duration / imageCount;
            File file = new File(findVideo.getSavePath());
            String path = file.getParentFile().getAbsolutePath() + File.separator + "temp";
            new File(path).mkdirs();
            for (int i = 0; i < imageCount; i++) {
                FFmpegUtil.videoSnapshot(findVideo.getSavePath(), path, findVideo.getName() + "_" + i, (duration - (size * (i + 1))) / 1000, 1);
            }
            List<File> fileList = new ArrayList<>();
            FileUtils.getPathFileList(path, fileList);
            List<FaceInfo> faceInfos = fileList.stream().sequential().flatMap(image -> {
                try {
                    List<FaceInfo> faceInfoList = FaceUtil.faceInfo(org.apache.commons.io.FileUtils.readFileToByteArray(image));
                    if (!CollectionUtils.isEmpty(faceInfoList)) {
                        System.out.println(JSON.toJSONString(faceInfoList));
                        return faceInfoList.stream();
                    }
                    Thread.sleep(1300);
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
            System.out.println(JSON.toJSONString(faceInfos));
            if (faceInfos.stream().filter(faceInfo -> faceInfo.getGender().equals("Female")).count() == 0) {
                logger.info("未匹配到面部，删除视频:{}", findVideo.getSavePath());
                file.delete();
                fileList.stream().forEach(image -> image.delete());
                new File(path).delete();
                return null;
            }
            findVideo.setFaceInfoList(faceInfos.stream().filter(faceInfo -> faceInfo.getGender().equals("Female")).collect(Collectors.toList()));
            double value = faceInfos.stream().filter(faceInfo -> faceInfo.getGender().equals("Female")).mapToDouble(faceInfo -> faceInfo.getFemaleScore() + faceInfo.getMaleScore()).average().getAsDouble() / 2;
            logger.info("{},平均评分:{}", findVideo.getSavePath(), value);
            findVideo.setAvgFaceScore(value);
            this.updateById(findVideo);
            if (value < threshold && isDelete) {
                logger.info("颜值过低，删除视频," + findVideo.getSavePath());
                file.delete();
            }
            fileList.stream().forEach(image -> image.delete());
            new File(path).delete();
            return value;
        }
        return null;
    }
}
