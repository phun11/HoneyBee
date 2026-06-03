package com.example.agritrace.controller;

import com.example.agritrace.dto.ApiResponse;
import com.example.agritrace.dto.ProductRequest;
import com.example.agritrace.service.ProductService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;
    public ProductController(ProductService service) { this.service = service; }

    @GetMapping
    public ApiResponse<?> all() { return ApiResponse.ok(service.findAll()); }

    @GetMapping("/{id}")
    public ApiResponse<?> one(@PathVariable("id") Long id) { return ApiResponse.ok(service.findById(id)); }

    @PostMapping
    public ApiResponse<?> create(@RequestBody ProductRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable("id") Long id, @RequestBody ProductRequest request) {
        service.update(id, request);
        return ApiResponse.ok("Updated");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ApiResponse.ok("Deleted");
    }
}
