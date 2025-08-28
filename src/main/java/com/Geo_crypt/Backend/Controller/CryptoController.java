package com.Geo_crypt.Backend.Controller;

import com.Geo_crypt.Backend.Model.FileLog;
import com.Geo_crypt.Backend.Service.CryptoService;
import com.Geo_crypt.Backend.Service.MinioService;
import com.Geo_crypt.Backend.repository.FileRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class CryptoController {
    private final CryptoService cryptoService;
    private final MinioService minioService;
    private final FileRepository fileLogRepository;


    @Value("${minio.bucket-encrypted}")
    private String encryptedBucket;


    @Value("${minio.bucket-decrypted}")
    private String decryptedBucket;


    @Value("${app.crypto.presigned-expiry-seconds}")
    private int presignedExpiry;
    public CryptoController(CryptoService cryptoService, MinioService minioService, FileRepository fileLogRepository) {
        this.cryptoService = cryptoService;
        this.minioService = minioService;
        this.fileLogRepository = fileLogRepository;
    }

    private String extractUsername(HttpServletRequest req){
        Object principal = req.getUserPrincipal();
        return principal==null?null:principal.toString();
    }

    @PostMapping(value = "/encrypt",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> encrypt(@RequestParam("file") MultipartFile file,
                                     @RequestParam("secretKey") String secretKey,
                                     @RequestParam(value = "latitude", required = false) String latitude,
                                     @RequestParam(value = "longitude", required = false) String longitude,
                                     HttpServletRequest request) throws Exception {


        byte[] plain = file.getBytes();
        String combined = cryptoService.buildCombined(secretKey, latitude, longitude, cryptoService.getCoordDecimals());
        var aesKey = cryptoService.deriveKey(combined);
        byte[] encrypted = cryptoService.encrypt(plain, aesKey);


        String original = StringUtils.getFilename(file.getOriginalFilename());
        String objectName = UUID.randomUUID().toString() + "-" + (original == null ? "file" : original) + ".enc";

        minioService.upload(encryptedBucket, objectName, new ByteArrayInputStream(encrypted), encrypted.length, "application/octet-stream");

        String url = minioService.presignedUrl(encryptedBucket, objectName, presignedExpiry);

        FileLog log = new FileLog();
        log.setUsername(extractUsername(request));
        log.setObjectName(objectName);
        log.setOperation("ENCRYPT");
        log.setSuccess(true);
        log.setLatitude(latitude);
        log.setLongitude(longitude);
        fileLogRepository.save(log);


        return ResponseEntity.ok(Map.of("objectName", objectName, "downloadUrl", url));
    }

    @PostMapping(value = "/decrypt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> decryptUpload(@RequestParam(value = "file", required = false) MultipartFile encryptedFile,
                                           @RequestParam(value = "objectName", required = false) String objectName,
                                           @RequestParam("secretKey") String secretKey,
                                           @RequestParam(value = "latitude", required = false) String latitude,
                                           @RequestParam(value = "longitude", required = false) String longitude,
                                           HttpServletRequest request) throws Exception {


        byte[] encryptedBytes;
        String sourceObject = null;

        if (encryptedFile != null && !encryptedFile.isEmpty()) {
            encryptedBytes = encryptedFile.getBytes();
        } else if (objectName != null && !objectName.isBlank()) {
            sourceObject = objectName;
            try (var is = minioService.getObject(encryptedBucket, objectName)) {
                encryptedBytes = is.readAllBytes();
            }
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Provide file or objectName"));
        }

        try {
            byte[] plain = cryptoService.tryDecryptWithTolerance(encryptedBytes, secretKey, latitude, longitude);


            String outName = UUID.randomUUID().toString() + "-decrypted";
            if (sourceObject != null && sourceObject.contains(".")) {
                String base = sourceObject;
                if (base.endsWith(".enc")) base = base.substring(0, base.length() - 4);
                outName = UUID.randomUUID().toString() + "-" + base;
            }


            minioService.upload(decryptedBucket, outName, new ByteArrayInputStream(plain), plain.length, "application/octet-stream");
            String url = minioService.presignedUrl(decryptedBucket, outName, presignedExpiry);

            FileLog log = new FileLog();
            log.setUsername(extractUsername(request));
            log.setObjectName(sourceObject == null ? (encryptedFile == null ? objectName : encryptedFile.getOriginalFilename()) : sourceObject);
            log.setOperation("DECRYPT");
            log.setSuccess(true);
            log.setLatitude(latitude);
            log.setLongitude(longitude);
            fileLogRepository.save(log);


            return ResponseEntity.ok(Map.of("objectName", outName, "downloadUrl", url));


        } catch (Exception ex) {
            FileLog log = new FileLog();
            log.setUsername(extractUsername(request));
            log.setObjectName(sourceObject == null ? (encryptedFile == null ? objectName : encryptedFile.getOriginalFilename()) : sourceObject);
            log.setOperation("DECRYPT");
            log.setSuccess(false);
            log.setLatitude(latitude);
            log.setLongitude(longitude);
            fileLogRepository.save(log);

            return ResponseEntity.status(400).body(Map.of("message", "Decryption failed: wrong key/location or GPS drift too large"));
        }
    }
}
