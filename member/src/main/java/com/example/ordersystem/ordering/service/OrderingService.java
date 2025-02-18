package com.example.ordersystem.ordering.service;

import com.example.ordersystem.member.domain.Member;
import com.example.ordersystem.member.repository.MemberRepository;
import com.example.ordersystem.ordering.controller.SseController;
import com.example.ordersystem.ordering.domain.OrderDetail;
import com.example.ordersystem.ordering.domain.Ordering;
import com.example.ordersystem.ordering.dto.OrderCreateDto;
import com.example.ordersystem.ordering.dto.OrderDetailResDto;
import com.example.ordersystem.ordering.dto.OrderListResDto;
import com.example.ordersystem.ordering.repository.OrderingDetailRepository;
import com.example.ordersystem.ordering.repository.OrderingRepository;
import com.example.ordersystem.product.domain.Product;
import com.example.ordersystem.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrderingService {
    private final OrderingRepository orderingRepository;
    private final MemberRepository memberRepository;
    private final OrderingDetailRepository orderingDetailRepository;
    private final ProductRepository productRepository;
    private final SseController sseController;

    public OrderingService(OrderingRepository orderingRepository, MemberRepository memberRepository
            , OrderingDetailRepository orderingDetailRepository,ProductRepository productRepository
            , SseController sseController) {
        this.orderingRepository = orderingRepository;
        this.memberRepository = memberRepository;
        this.orderingDetailRepository = orderingDetailRepository;
        this.productRepository = productRepository;
        this.sseController = sseController;
    }

    public Ordering orderCreate(List<OrderCreateDto> dtos){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email).orElseThrow(()->new EntityNotFoundException("member is not found"));
////        방법1. cacading 없이 db저장
////        Ordering객체 생성 및 save
//        Ordering ordering = Ordering.builder()
//                .member(member)
//                .build();
//        orderingRepository.save(ordering);
////        OrderingDetail 객체 생성 및 save
//        for(OrderCreateDto o : dtos){
//            Product product = productRepository.findById(o.getProductId())
//                    .orElseThrow(()->new EntityNotFoundException("not found"));
//
//            if(product.getStockQuantity()<o.getProductCount()){
//                throw new IllegalArgumentException("재고부족");
//            }else {
//                // 재고 감소 로직
//                product.updateStockQuantity(o.getProductCount());
//            }
//            OrderDetail orderDetail = OrderDetail.builder()
//                    .ordering(ordering)
//                    .product(product)
//                    .quantity(o.getProductCount())
//                    .build();
//            orderingDetailRepository.save(orderDetail);
//        }


//        방법2. cacading 사용하여 db저장
//        Ordering객체 생성하면서 OrderingDetail객체 같이 생성.
        Ordering ordering = Ordering.builder()
                .member(member)
                .build();
        for(OrderCreateDto o : dtos){
            Product product = productRepository.findById(o.getProductId())
                    .orElseThrow(()->new EntityNotFoundException("not found"));
            int quantity = o.getProductCount();

            //동시성 이슈를 고려 안 한 코드
//            if(product.getStockQuantity()<quantity){
//                throw new IllegalArgumentException("재고부족");
//            }else {
//                // 재고 감소 로직
//                product.updateStockQuantity(o.getProductCount());
//            }

            //동시성 이슈를 고려한 코드
            //redis를 통한 재고관리 및 재고잔량 확인

            //rdb동기화(rabbitmq)


            OrderDetail orderDetail = OrderDetail.builder()
                    .ordering(ordering)
                    .product(product)
                    .quantity(o.getProductCount())
                    .build();
            ordering.getOrderDetails().add(orderDetail);
        }
        Ordering ordering1 = orderingRepository.save(ordering);

//        sse를 통한 admin계정에 메시지 발송
        sseController.publichMessage(ordering1.fromEntity(),"admin@naver.com");

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
        Member member = memberRepository.findByEmail(email).orElseThrow(()->new EntityNotFoundException("not found member"));

        List<OrderListResDto> orderListDtos = new ArrayList<>();
        for(Ordering o : member.getOrderingList()){
            orderListDtos.add(o.fromEntity());
        }
        return orderListDtos;
    }
    public Ordering orderCancel(Long id){
        Ordering ordering = orderingRepository.findById(id).orElseThrow(()->new EntityNotFoundException("order is not found"));
        ordering.cancelStatus();

        for(OrderDetail orderDetail : ordering.getOrderDetails()){
            orderDetail.getProduct().cancelOrder(orderDetail.getQuantity());
        }
        return ordering;
    }

}
