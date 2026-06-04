package com.example.agritrace.controller;

import com.example.agritrace.dto.ApiResponse;
import com.example.agritrace.model.Farm;
import com.example.agritrace.repository.FarmRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/farms")
public class FarmController {
    private final FarmRepository repo;
    public FarmController(FarmRepository repo) { this.repo = repo; }

    @GetMapping public ApiResponse<?> all() { return ApiResponse.ok(repo.findAll()); }
    @GetMapping("/{id}") public ApiResponse<?> one(@PathVariable("id") Long id) { return ApiResponse.ok(repo.findById(id)); }
    @PostMapping public ApiResponse<?> create(@RequestBody Farm farm) { repo.create(farm); return ApiResponse.ok("Created"); }
    @PutMapping("/{id}") public ApiResponse<?> update(@PathVariable("id") Long id, @RequestBody Farm farm) { repo.update(id, farm); return ApiResponse.ok("Updated"); }
    @DeleteMapping("/{id}") public ApiResponse<?> delete(@PathVariable("id") Long id) { repo.delete(id); return ApiResponse.ok("Deleted"); }
}
