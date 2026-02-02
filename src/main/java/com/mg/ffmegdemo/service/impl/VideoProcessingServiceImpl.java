package com.mg.ffmegdemo.service.impl;


import com.mg.ffmegdemo.service.VideoProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class VideoProcessingServiceImpl implements VideoProcessingService {
    private static final Logger log = LoggerFactory.getLogger(VideoProcessingServiceImpl.class);
    private static final String FOLDER_DIR_NAME = "temp_hls_dir";
    private static final String FILE_DIR_PREFIX = "temp_raw_video_file_";
    private static final String FILE_DIR_SUFFIX = ".tmp";

    @Override
    public void processVideo(MultipartFile file) {
        Path tempFolderDir = createFolderDir();

        Path tempVideoFile = createTempVideoFile(file);

        ProcessBuilder process = getFFMPEGProcessBuilder(tempFolderDir, tempVideoFile);

        handleProcessBuilder(process);
    }

    private void handleProcessBuilder(ProcessBuilder processBuilder) {
        try {
            Process process = processBuilder.start();

            consumeProcessLogs(process);

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.error("FFMPEG command failed with exit code {}", exitCode);
                throw new RuntimeException();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Path createFolderDir() {
        Path folderDir;
        try {
            folderDir = Files.createTempDirectory(FOLDER_DIR_NAME + System.currentTimeMillis());
        } catch (IOException e) {
            log.error(" An IO Exception oocurred creating folder {}", e.getMessage());
            throw new RuntimeException();
        }
        return folderDir;
    }

    private Path createTempVideoFile(MultipartFile file) {
        Path folderFile;
        try {

            folderFile = Files.createTempFile(FILE_DIR_PREFIX, FILE_DIR_SUFFIX);

            file.transferTo(folderFile);

        } catch (IOException e) {
            log.error("IO exception occured during creating temp file or transferring raw video file to temp file : {}", e.getMessage());
            throw new RuntimeException("An IO exception occured while creating raw video file");
        }
        return folderFile;
    }

    private ProcessBuilder getFFMPEGProcessBuilder(Path hlsTempDir, Path tempRawFile) {

        List<String> cmd = new ArrayList<>();

        cmd.add("ffmpeg");
        cmd.add("-threads");
        cmd.add("2");


        cmd.add("-i");
        cmd.add(tempRawFile.toString());

        cmd.add("-g");
        cmd.add("48");
        cmd.add("-keyint_min");
        cmd.add("48");
        cmd.add("-force_key_frames");
        cmd.add("expr:gte(t,n_forced*2)");

        for (int i = 0; i < 4; i++) {
            cmd.add("-map");
            cmd.add("0:v:0");
            cmd.add("-map");
            cmd.add("0:a:0");
        }

        addQuality(0, "600k", "96k", 640, cmd);
        addQuality(1, "900k", "96k", 854, cmd);
        addQuality(2, "1800k", "128k", 1280, cmd);
        addQuality(3, "3000k", "128k", 1920, cmd);


        // region Output Settings
        cmd.add("-f");
        cmd.add("hls");

        cmd.add("-hls_time");
        cmd.add("2");

        cmd.add("-hls_playlist_type");
        cmd.add("vod");

        cmd.add("-var_stream_map");
        cmd.add("v:0,a:0 v:1,a:1 v:2,a:2 v:3,a:3");
        // endregion

        // region Names
        cmd.add("-hls_segment_filename");
        cmd.add(hlsTempDir.resolve("v%v/segment%d.ts").toString());

        cmd.add("-master_pl_name");
        cmd.add("master.m3u8");

        cmd.add(hlsTempDir.resolve("v%v/playlist.m3u8").toString());
        // endregion

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);

        return pb;

    }

    private void addQuality(int index, String videoBitRateInKPS, String audioBitrateInKPS, int width, List<String> cmd) {
        cmd.add("-filter:v:" + index);

        cmd.add("scale=w=" + width + ":h=-2");

        cmd.add("-c:v:" + index);
        cmd.add("libx264");
        cmd.add("-b:v:" + index);
        cmd.add(videoBitRateInKPS);

        cmd.add("-c:a:" + index);
        cmd.add("aac");
        cmd.add("-b:a:" + index);
        cmd.add(audioBitrateInKPS);
    }

    private void consumeProcessLogs(Process process) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info(line);
                }
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }).start();
    }


}


