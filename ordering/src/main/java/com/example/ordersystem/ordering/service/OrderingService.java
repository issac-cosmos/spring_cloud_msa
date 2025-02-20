package com.example.ordersystem.ordering.service;

import com.example.ordersystem.ordering.controller.SseController;
import com.example.ordersystem.ordering.domain.OrderDetail;
import com.example.ordersystem.ordering.domain.Ordering;
import com.example.ordersystem.ordering.dto.OrderCreateDto;
import com.example.ordersystem.ordering.dto.OrderListResDto;
import com.example.ordersystem.ordering.dto.ProductDto;
import com.example.ordersystem.ordering.dto.ProductUpdateStockDto;
import com.example.ordersystem.ordering.repository.OrderingDetailRepository;
import com.example.ordersystem.ordering.repository.OrderingRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class OrderingService {
    private final OrderingRepository orderingRepository;
    private final RestTemplate restTemplate;
    private final ProductFeign productFeign;
    private final KafkaTemplate<String , Objects> kafkaTemplate;

    public OrderingService(KafkaTemplate<String , Objects> kafkaTemplate
            ,ProductFeign productFeign,RestTemplate restTemplate
            , OrderingRepository orderingRepository
            ) {
        this.orderingRepository = orderingRepository;
        this.restTemplate = restTemplate;
        this.productFeign= productFeign;
        this.kafkaTemplate = kafkaTemplate;
    }

    public Ordering orderCreate(List<OrderCreateDto> dtos){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Ordering ordering = Ordering.builder()
                .memberEmail(email)
                .build();
        for(OrderCreateDto o : dtos){
//            product서버에 api요청을 통해 product객체를 받아와야함. > 동기처리 필수!
            String productGetUrl = "http://product-service/product/"+o.getProductId();
            String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization",token);
            HttpEntity<String> httpEntity = new HttpEntity<>(headers);

            ResponseEntity<ProductDto> response = restTemplate.exchange(productGetUrl, HttpMethod.GET, httpEntity, ProductDto.class );
            ProductDto productDto = response.getBody();

            System.out.println(productDto);

            int quantity = o.getProductCount();

            if(productDto.getStockQuantity() < quantity){
                throw new IllegalArgumentException("재고부족");
            }else {
                // 재고 감소 api요청을 product서버에 보내줘야함 -> 비동기 처리 가능.
                String productUpdateStockUrl = "http://product-service/product/updatestock";
                headers.setContentType(MediaType.APPLICATION_JSON);// 수동으로 body부를 json으로 셋팅
                HttpEntity<ProductUpdateStockDto> updateEntity = new HttpEntity<>(
                        ProductUpdateStockDto.builder()
                                .productId(o.getProductId()).productQuantity(o.getProductCount()).build()
                        ,headers
                );
                restTemplate.exchange(productUpdateStockUrl,HttpMethod.PUT,updateEntity,Void.class);
            }

            OrderDetail orderDetail = OrderDetail.builder()
                    .ordering(ordering)
                    .productId(o.getProductId())
                    .quantity(o.getProductCount())
                    .build();
            ordering.getOrderDetails().add(orderDetail);
        }
        orderingRepository.save(ordering);
        return ordering;
    }
    public Ordering orderKafkaCreate(List<OrderCreateDto> dtos){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Ordering ordering = Ordering.builder()
                .memberEmail(email)
                .build();
        for(OrderCreateDto o : dtos){
//            product서버에 feign클라이언트를 통한 api요청 조회
            ProductDto productDto = productFeign.getProductById(o.getProductId());

            int quantity = o.getProductCount();

            if(productDto.getStockQuantity() < quantity){
                throw new IllegalArgumentException("재고부족");
            }else {
                // 재고 감소 api요청을 product서버에 보내줘야함 -> kafka에 메시지 발행
//                productFeign.updateProductStock(ProductUpdateStockDto.builder()
//                        .productId(o.getProductId()).productQuantity(o.getProductCount()).build());
                ProductUpdateStockDto dto =ProductUpdateStockDto.builder()
                        .productId(o.getProductId()).productQuantity(o.getProductCount()).build();
                kafkaTemplate.send("update-stock-topic", dto);
            }

            OrderDetail orderDetail = OrderDetail.builder()
                    .ordering(ordering)
                    .productId(o.getProductId())
                    .quantity(o.getProductCount())
                    .build();
            ordering.getOrderDetails().add(orderDetail);
        }
        orderingRepository.save(ordering);
        return ordering;
    }
    public List<OrderListResDto> findAll(){
        List<Ordering> orderings = orderingRepository.findAll();
        List<OrderListResDto> orderListResDtos = new ArrayList<>();
        for(Ordering o : orderings){
            orderListResDtos.add(o.fromEntity());
        }
        return orderListResDtos;
    }
    public List<OrderListResDto> myOrders(){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        List<OrderListResDto> orderListDtos = new ArrayList<>();
        for(Ordering o : orderingRepository.findByMemberEmail(email)){
            orderListDtos.add(o.fromEntity());
        }
        return orderListDtos;
    }
    public Ordering orderCancel(Long id){
        Ordering ordering = orderingRepository.findById(id).orElseThrow(()->new EntityNotFoundException("order is not found"));
        ordering.cancelStatus();

        return ordering;
    }

}
