package com.example.agritrace.controller;

import com.example.agritrace.dto.ApiResponse;
import com.example.agritrace.service.QrService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/qr")
public class QrController {
    private final QrService service;
    public QrController(QrService service) { this.service = service; }

    @GetMapping(value = "/product/{id}/image", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] image(@PathVariable("id") Long id) throws Exception { return service.generateQrPngByProduct(id); }

    @GetMapping(value = "/text", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] text(@RequestParam("value") String value) throws Exception { return service.generate(value); }

    @PostMapping("/product/{id}")
    public ApiResponse<?> create(@PathVariable("id") Long id) { return ApiResponse.ok(service.createQrRecord(id)); }
}
