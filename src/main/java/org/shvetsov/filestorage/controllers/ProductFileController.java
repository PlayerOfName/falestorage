package org.shvetsov.filestorage.controllers;

import lombok.RequiredArgsConstructor;
import org.shvetsov.filestorage.services.FileService;
import org.shvetsov.requestApi.ProductPhotoRQ;
import org.shvetsov.requestApi.UploadFileRQ;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("files")
@RequiredArgsConstructor
public class ProductFileController {
    private final FileService fileService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductPhotoRQ uploadFile(@RequestParam UUID productId, @RequestParam UUID userId, @RequestParam MultipartFile file) {
        return fileService.uploadProductPhoto(productId, userId, file);
    }

    @DeleteMapping("/delete")
    public void deleteFile(@RequestParam UUID productId,@RequestParam String filename) {
        fileService.deleteProductPhoto(productId, filename);
    }

    @GetMapping("/getfiles")
    public List<ProductPhotoRQ> getFiles(@RequestParam UUID productId) throws Exception {
        return fileService.listFiles("products/" + productId + "/");
    }
}
