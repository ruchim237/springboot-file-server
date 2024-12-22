package com.ruchi.javafileserver.controller;

import com.ruchi.javafileserver.view.ErrorModel;
import com.ruchi.javafileserver.view.FileListingModel;
import com.ruchi.javafileserver.view.ViewConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class FileServerController {

    @Value("${file.server.directory}")
    private String serverDirectory;

    @GetMapping("/")
    public String listFiles(@RequestParam(value = "path", required = false, defaultValue = "/") String path, Model model) {
        validatePath(path);
        path = simplifyPath(path);
        File folder = new File(serverDirectory + File.separator + path);

        if (!folder.exists() || !folder.isDirectory()) {
            model.addAttribute(ErrorModel.ERROR, "Directory not found: " + folder.getAbsolutePath());
            model.addAttribute(ErrorModel.STATUS, HttpStatus.NOT_FOUND);
            return ViewConstants.ERROR;
        }

        File[] files = folder.listFiles();
        if (files != null) {
            List<String> fileNames = new ArrayList<>();
            for (File file : files) {
                if (file.isDirectory()) {
                    fileNames.add(file.getName() + "/");
                    continue;
                }
                fileNames.add(file.getName());
            }
            model.addAttribute(FileListingModel.FILES, fileNames);
            model.addAttribute(FileListingModel.CURRENT_PATH, path);
        }
        return ViewConstants.FILE_LISTING_VIEW;
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam("path") String path) {
        try {
            // Resolve the file path
            if (path.startsWith("/")) path = path.substring(1);
            Path filePath = Paths.get(serverDirectory).resolve(path).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            // Get the original file name
            String fileName = filePath.getFileName().toString();

            // Set the content type dynamically
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // Set headers to ensure proper download behavior
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String simplifyPath(String path) {
        // path ex, "/resume/test/../../resume/" => "/resume/"
        String[] dirs = path.split("/");
        // dirs = ["", "resume", "test", "..", "..", "resume"]
        Deque<String> dq = new LinkedList<>();
        for (int i = 1; i < dirs.length; i++) {
            if (dirs[i].equals("..")) {
                if (dq.isEmpty()) {
                    throw new RuntimeException("Invalid path! Path cannot exceed scope of '/' directory.");
                }
                dq.removeLast();
            } else {
                dq.addLast(dirs[i]);
            }
        }
        StringBuilder simplifiedPath = new StringBuilder("/");
        while (!dq.isEmpty()) {
            simplifiedPath.append(dq.peekFirst())
                    .append("/");
            dq.removeFirst();
        }
        return simplifiedPath.toString();
    }

    private void validatePath(String path) {
        if (!path.startsWith("/")) {
            throw new RuntimeException("Invalid path, path should start with '/'");
        }
        String[] dirs = path.split("/");
        int backDir = 0;
        for (String s : dirs) {
            backDir = s.equals("..") ? backDir + 1 : backDir;
        }
        if (dirs.length > 1 && backDir > ((dirs.length - 1) >> 1)) {
            throw new RuntimeException("Invalid path, path cannot have more '../' to go beyond scope of '/'");
        }
    }
}
