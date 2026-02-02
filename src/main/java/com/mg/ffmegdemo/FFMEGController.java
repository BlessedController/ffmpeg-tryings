package com.mg.ffmegdemo;

import org.springframework.web.bind.annotation.*;

@RestController
public class FFMEGController {

    private final VideoProcessingServiceImpl videoProcessingService;


    public FFMEGController(VideoProcessingServiceImpl videoProcessingService) {
        this.videoProcessingService = videoProcessingService;
    }

    @PostMapping
    public void convert(@RequestBody Paths paths) {
        videoProcessingService.processVideo(paths.folderDir(), paths.fileDir());
    }

}
