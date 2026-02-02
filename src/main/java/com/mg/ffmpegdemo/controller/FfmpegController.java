package com.mg.ffmpegdemo.controller;

import com.mg.ffmpegdemo.service.VideoProcessingService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class FfmpegController {

    private final VideoProcessingService videoProcessingService;

    public FfmpegController(VideoProcessingService videoProcessingService) {
        this.videoProcessingService = videoProcessingService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void convert(@RequestParam MultipartFile file) {
        videoProcessingService.processVideo(file);
    }

}
