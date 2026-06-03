package com.example.agritrace.service;

import com.example.agritrace.dto.ProductRequest;
import com.example.agritrace.model.Product;
import com.example.agritrace.repository.ProductRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class ProductService {
    private final ProductRepository repo;
    public ProductService(ProductRepository repo) { this.repo = repo; }

    public List<Product> findAll() { return repo.findAll(); }
    public Product findById(Long id) { return repo.findById(id); }
    public Long create(ProductRequest request) { return repo.createByProcedure(request); }
    public List<Map<String, Object>> findByFarm(Long farmId) { return repo.findByFarm(farmId); }
    public void markReadyForTransport(Long productId, Long userId, String note) { repo.markReadyForTransport(productId, userId, note); }
    public void update(Long id, ProductRequest request) { repo.update(id, request); }
    public void delete(Long id) { repo.delete(id); }
}
