package com.mg.ffmegdemo;


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
public class VideoProcessingServiceImpl {
    private static final Logger log = LoggerFactory.getLogger(VideoProcessingServiceImpl.class);
    private static final String FOLDER_DIR = "D:\\MAHABBAT_GOZALOV\\FFMPEG\\folderDir";


    public void processVideo(MultipartFile file) {
        Path folderDir = createFolderDir();

        Path tempVideoFile = createTempVideoFile(file);

        Process process = this.doFFMPEGProcess(folderDir, tempVideoFile);

        handleLogs(process);
    }

    private void handleLogs(Process process) {
        try {
            consumeProcessLogs(process);

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.error("FFMPEG command failed with exit code {}", exitCode);
                throw new RuntimeException();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private Path createFolderDir() {
        Path folderDir;
        try {
            folderDir = Files.createDirectory(Path.of(FOLDER_DIR + System.currentTimeMillis()));
        } catch (IOException e) {
            log.error(" An IO Exception oocurred creating folder {}", e.getMessage());
            throw new RuntimeException();
        }
        return folderDir;
    }

    private Path createTempVideoFile(MultipartFile file) {
        Path folderFile;
        try {

            folderFile = Files.createTempFile("temp_raw_video_file", ".tmp");

            file.transferTo(folderFile);

        } catch (IOException e) {
            log.error("IO exception occured during creating temp file or transferring raw video file to temp file : {}", e.getMessage());
            throw new RuntimeException("An IO exception occured while creating raw video file");
        }
        return folderFile;
    }


    private Process doFFMPEGProcess(Path hlsTempDir, Path tempRawFile) {

        List<String> cmd = new ArrayList<>();

        cmd.add("ffmpeg");

        cmd.add("-i");
        cmd.add(tempRawFile.toString());

        cmd.add("-g");
        cmd.add("48");
        cmd.add("-keyint_min");
        cmd.add("48");
        cmd.add("-force_key_frames");
        cmd.add("expr:gte(t,n_forced*2)");

        // region Quality 0 (1920p)
        cmd.add("-filter:v:0");
        cmd.add("scale=w=1920:h=-2");
        cmd.add("-c:v:0");
        cmd.add("libx264");
        cmd.add("-b:v:0");
        cmd.add("3000k");
        cmd.add("-c:a:0");
        cmd.add("aac");
        cmd.add("-b:a:0");
        cmd.add("128k");
        //endregion

        // region Quality 1 (1280p)
        cmd.add("-filter:v:1");
        cmd.add("scale=w=1280:h=-2");
        cmd.add("-c:v:1");
        cmd.add("libx264");
        cmd.add("-b:v:1");
        cmd.add("1800k");
        cmd.add("-c:a:1");
        cmd.add("aac");
        cmd.add("-b:a:1");
        cmd.add("128k");
        //endregion

        // region Quality 2 (854p)
        cmd.add("-filter:v:2");
        cmd.add("scale=w=854:h=-2");
        cmd.add("-c:v:2");
        cmd.add("libx264");
        cmd.add("-b:v:2");
        cmd.add("900k");
        cmd.add("-c:a:2");
        cmd.add("aac");
        cmd.add("-b:a:2");
        cmd.add("96k");
        //endregion

        // region Quality 3 (640p)
        cmd.add("-filter:v:3");
        cmd.add("scale=w=640:h=-2");
        cmd.add("-c:v:3");
        cmd.add("libx264");
        cmd.add("-b:v:3");
        cmd.add("600k");
        cmd.add("-c:a:3");
        cmd.add("aac");
        cmd.add("-b:a:3");
        cmd.add("96k");
        //endregion

        // region Stream Mapping
        for (int i = 0; i < 4; i++) {
            cmd.add("-map");
            cmd.add("0:v:0");
            cmd.add("-map");
            cmd.add("0:a:0");
        }
        //endregion

        // region HLS Settings
        cmd.add("-f");
        cmd.add("hls");

        cmd.add("-hls_time");
        cmd.add("2");

        cmd.add("-hls_playlist_type");
        cmd.add("vod");

        cmd.add("-hls_segment_filename");
        cmd.add(hlsTempDir.toString() + "/v%v/segment%d.ts");

        cmd.add("-master_pl_name");
        cmd.add("master.m3u8");

        cmd.add("-var_stream_map");
        cmd.add("v:0,a:0 v:1,a:1 v:2,a:2 v:3,a:3");

        cmd.add(hlsTempDir + "/v%v/playlist.m3u8");
        // endregion

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        try {
            return pb.start();
        } catch (IOException e) {
            log.error("An IO Exception occured during FFMPEG start : {}", e.getMessage());
            throw new RuntimeException();
        }
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


