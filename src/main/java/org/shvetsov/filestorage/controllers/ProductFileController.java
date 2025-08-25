package org.shvetsov.filestorage.controllers;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.shvetsov.filestorage.services.FileService;
import org.shvetsov.requestApi.ProductPhotoRQ;
import org.shvetsov.requestApi.ProductPhotoRS;
import org.shvetsov.requestApi.UploadFileRQ;
import org.shvetsov.responseApi.FileInfoResponse;
import org.shvetsov.responseApi.FileResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("files")
@RequiredArgsConstructor
public class ProductFileController {
    private final FileService fileService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductPhotoRS uploadFile(@RequestParam UUID productId, @RequestParam UUID fileId, @RequestPart MultipartFile file) {
        return fileService.uploadProductPhoto(productId, fileId, file);
    }

    @DeleteMapping("/delete")
    public void deleteFile(@RequestParam String path) {
        fileService.deleteProductPhoto(path);
    }

    // 2. Просмотр файла
    @GetMapping("/view")
    public ResponseEntity<Resource> viewFile(@RequestParam("path") String path) throws FileNotFoundException {
        FileResponse response = fileService.getFile(path);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + response.getFileName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, response.getContentType())
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(response.getSize()))
                .body(response.getResource());
    }

    // 3. Скачивание файла
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam("path") String path) throws FileNotFoundException {
        FileResponse response = fileService.downloadFile(path);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + response.getFileName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, response.getContentType())
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(response.getSize()))
                .body(response.getResource());
    }

    // 4. Получение информации о файле
    @GetMapping("/info")
    public ResponseEntity<FileInfoResponse> getFileInfo(@RequestParam("path") String path) throws FileNotFoundException {
        FileInfoResponse response = fileService.getFileInfo(path);
        return ResponseEntity.ok(response);
    }

/*    // 5. Список файлов в директории
    @GetMapping("/list")
    public ResponseEntity<List<FileInfoResponse>> listFiles(
            @RequestParam(value = "prefix", required = false) String prefix) {
        List<FileInfoResponse> files = fileService.listFiles(prefix);
        return ResponseEntity.ok(files);
    }*/

    // 6. Генерация URL
    @GetMapping("/url")
    public ResponseEntity<Map<String, String>> generateFileUrl(
            @RequestParam("path") String path,
            @RequestParam(value = "expiryDays", defaultValue = "7") int expiryDays) throws FileNotFoundException {
        String url = fileService.generateFileUrl(path, expiryDays);
        return ResponseEntity.ok(Map.of("url", url));
    }

    // 7. Проверка существования файла
    @GetMapping("/exists")
    public ResponseEntity<Map<String, Boolean>> fileExists(@RequestParam("path") String path) {
        boolean exists = fileService.fileExists(path);
        return ResponseEntity.ok(Map.of("exists", exists));
    }
}
