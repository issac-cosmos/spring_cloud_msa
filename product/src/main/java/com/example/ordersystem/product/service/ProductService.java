package com.example.ordersystem.product.service;

import com.example.ordersystem.common.service.StockInventoryService;
import com.example.ordersystem.product.domain.Product;
import com.example.ordersystem.product.dto.ProductRegisterDto;
import com.example.ordersystem.product.dto.ProductResDto;
import com.example.ordersystem.product.dto.ProductSearchDto;
import com.example.ordersystem.product.dto.ProductUpdateStockDto;
import com.example.ordersystem.product.repository.ProductRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.kafka.common.protocol.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ProductService {
    private final ProductRepository productRepository;
    private final StockInventoryService stockInventoryService;


    public ProductService(ProductRepository productRepository, StockInventoryService stockInventoryService) {
        this.productRepository = productRepository;
        this.stockInventoryService = stockInventoryService;

    }

    public Product productCreate(ProductRegisterDto dto){
        try {
//        member 조회
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            Product product = productRepository.save(dto.toEntity(authentication.getName()));
//            redis 재고에 추가
            //redis는 transaction의 대상이 아니므로 , 에러시 별도의 다시 백업작업이 필요함.
            stockInventoryService.increaseStock(product.getId(),dto.getStockQuantity());


//        aws에 image저장 후에 url 추출
//        aws에 s3접근가능한 iam계정생성, iam계정을 통해 aws에 접근 가능한 접근객체 생성
            MultipartFile image = dto.getProductImage();
            byte[] bytes = image.getBytes();
            String fileName = product.getId() + "_" + image.getOriginalFilename();
//        먼저 local에 저장
            Path path = Paths.get("C:/Users/COM/Desktop/tmp/", fileName);
            Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

            return product;
        }catch (IOException e){
            throw new RuntimeException("이미지 저장실패");
        }
    }
    public Page<ProductResDto> findAll(Pageable pageable, ProductSearchDto searchDto){
//        검색을 위해 Specification 객체 사용
//        Specification객체는 복잡한 쿼리를 명세를 이용하여 정의하는 방식으로, 쿼리를 쉽게 생성.
        Specification<Product> specification = new Specification<Product>() {
            @Override
            public Predicate toPredicate(Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
//                root : 엔티티의 속성을 접근하기 위한 객체, criteriabuilder : 쿼리를 생성하기 위한 객체
                List<Predicate> predicates = new ArrayList<>();
                if(searchDto.getCategory() != null){
                    predicates.add(criteriaBuilder.equal(root.get("category"),searchDto.getCategory()));
                }
                if(searchDto.getProductName() != null){
                    predicates.add(criteriaBuilder.like(root.get("name"), "%"+searchDto.getProductName()+"%"));
                }
                Predicate[] predicateArr = new Predicate[predicates.size()];
                for(int i = 0; i<predicates.size(); i++){
                    predicateArr[i] = predicates.get(i);
                }
                Predicate predicate = criteriaBuilder.and(predicateArr);
                return predicate;
            }
        };
        Page<Product> productList = productRepository.findAll(specification,pageable);
        return productList.map(p->p.fromEntity());

    }
    public ProductResDto productDetail(Long id){
        Product product = productRepository.findById(id).orElseThrow(()->new EntityNotFoundException("product is not found"));
        return product.fromEntity();
    }
    public Product updateStockquantity(ProductUpdateStockDto dto){
        Product product = productRepository.findById(dto.getProductId()).orElseThrow(()->new EntityNotFoundException("product is not found"));
        product.updateStockQuantity(dto.getProductQuantity());
        return product;
    }
    @KafkaListener(topics = "update-stock-topic", groupId = "product-group",containerFactory = "KafkaListener")
    public void productConsumer(String message){

        System.out.println("consumer message 수신 : " + message);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            ProductUpdateStockDto dto = objectMapper.readValue(message,ProductUpdateStockDto.class);
            this.updateStockquantity(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }
}
