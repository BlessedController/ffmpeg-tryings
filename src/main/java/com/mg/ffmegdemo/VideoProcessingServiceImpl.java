package com.mg.ffmegdemo;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class VideoProcessingServiceImpl {
    private static final Logger log = LoggerFactory.getLogger(VideoProcessingServiceImpl.class);


    public void processVideo(MultipartFile file) {

        try {
            Path folderDir = Paths.get("D:\\MAHABBAT_GOZALOV\\FFMPEG\\folderDir");

            Files.createDirectories(folderDir);

            Path tempVideoFile = createTempVideoFile(file);

            ProcessBuilder ffmpegCommands = this.createFFMPEGCommands(folderDir, tempVideoFile);

            Process process = ffmpegCommands.start();

            Thread writeProcessLogs = consumeProcessLogs(process);

            writeProcessLogs.start();

            int exitCode = process.waitFor();

            writeProcessLogs.join();

            if (exitCode != 0) {
                log.error("FFMPEG command failed with exit code {}", exitCode);
                throw new RuntimeException("FFMPEG command failed with exit code " + exitCode);
            }

        } catch (IOException e) {
            log.error("An IO exception has occured durng ffmpeg processing. Exception Message: {}", e.getMessage());
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            log.error("An InterruptedException has occured durng ffmpeg processing. Exception Message: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private Path createTempVideoFile(MultipartFile file) {
        try {
            Path folderFile = Paths.get("D:\\MAHABBAT_GOZALOV\\FFMPEG\\folderFile");

            Files.createDirectories(folderFile);

            Path tempRawFile = folderFile.resolve(System.currentTimeMillis() + ".tmp");

            file.transferTo(tempRawFile);

            return tempRawFile;
        } catch (IOException e) {
            log.error("IO exception occured during creating temp file or transferring raw video file to temp file : {}", e.getMessage());
            throw new RuntimeException("An IO exception occured while creating raw video file");
        }
    }


    private ProcessBuilder createFFMPEGCommands(Path hlsTempDir, Path tempRawFile) {

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
        return pb;
    }

    private Thread consumeProcessLogs(Process process) {
        return new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info(line);
                }
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        });
    }


}


