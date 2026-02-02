package com.mg.ffmegdemo.service;

import org.springframework.web.multipart.MultipartFile;

public interface VideoProcessingService {
    void processVideo(MultipartFile file);
}
