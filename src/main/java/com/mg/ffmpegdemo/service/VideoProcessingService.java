package com.mg.ffmpegdemo.service;

import org.springframework.web.multipart.MultipartFile;

public interface VideoProcessingService {
    void processVideo(MultipartFile file);
}
