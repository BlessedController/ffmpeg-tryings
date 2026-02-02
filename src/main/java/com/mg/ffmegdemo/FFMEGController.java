package com.mg.ffmegdemo;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class FFMEGController {

    private final VideoProcessingServiceImpl videoProcessingService;


    public FFMEGController(VideoProcessingServiceImpl videoProcessingService) {
        this.videoProcessingService = videoProcessingService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void convert(@RequestParam MultipartFile file) {
        videoProcessingService.processVideo(file);
    }

}
