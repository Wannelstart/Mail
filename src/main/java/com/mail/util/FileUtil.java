package com.mail.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;
@Component
public class FileUtil {

    @Value("${app.upload-dir}")
    private String uploadDir;

    public String saveFile(MultipartFile file) throws IOException {
        String dir = buildDir();
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path path = Paths.get(dir, filename).toAbsolutePath();
        Files.createDirectories(path.getParent());
        file.transferTo(path.toFile());
        return path.toString();
    }

    public String saveBase64File(String base64Data, String fileName) throws IOException {
        byte[] data = Base64.getDecoder().decode(base64Data);
      String dir = buildDir();
        String filename = UUID.randomUUID() + "_" + fileName;
        Path path = Paths.get(dir, filename).toAbsolutePath();
        Files.createDirectories(path.getParent());
        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            fos.write(data);
        }
        return path.toString();
    }

    public String saveBytes(byte[] data, String fileName) throws IOException {
        String dir = buildDir();
        String filename = UUID.randomUUID() + "_" + fileName;
        Path path = Paths.get(dir, filename).toAbsolutePath();
        Files.createDirectories(path.getParent());
        Files.write(path, data);
        return path.toString();
    }

    public byte[] readFile(String filePath) throws IOException {
        return Files.readAllBytes(Paths.get(filePath));
    }

    private String buildDir() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    return uploadDir + File.separator + date;
    }
}
