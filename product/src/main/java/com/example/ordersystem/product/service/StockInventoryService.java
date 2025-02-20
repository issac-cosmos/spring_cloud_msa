//package com.example.ordersystem.product.service;
//
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//
//@Service
////rdb와 연결할 일이 없기 때문에 여기서는 굳이 Transactional필요하지 않다.
//public class StockInventoryService {
//    @Qualifier("stockinventory")
//    private final RedisTemplate<String, String> redisTemplate; //레디스는 자료 타입이 다 String이다 그런데 관례적으로 <String, Object>많이 쓰인다.
//
//
//    public StockInventoryService(@Qualifier("stockinventory") RedisTemplate<String, String> redisTemplate) {
//        this.redisTemplate = redisTemplate;
//    }
//
//    //    상품 등록시,주문취소시 increasStock
//    public int increaseStock(Long productId, int quantity){
//        String remainsObject = redisTemplate.opsForValue().get(String.valueOf(productId));// 이 때는 리턴값이 long타입으로 안받아짐.그래서 Object나 String으로 받았음
//        if(remainsObject !=null) {
//            int remains = Integer.parseInt(remainsObject);
//            if (remains < quantity) {
//                redisTemplate.opsForValue().set(String.valueOf(productId), "0");
//            }
//        }
//        Long newRemains =  redisTemplate.opsForValue().increment(String.valueOf(productId),quantity); //increase된 최종값을 리턴해준다,//원래 서드파티랑 연결할때는 받는 타입이 중구난방...
//        return newRemains.intValue();
//    }
//    //    주문시 decreaseStock
//    public int decreaseStock(Long productId, int quantity){
////        먼저 조회 후에 재고 감소가 가능할 때decrease시킨다. 마이너스 값이 되면 안되니까
//        String remainsObject = redisTemplate.opsForValue().get(String.valueOf(productId));// 이 때는 리턴값이 long타입으로 안받아짐.그래서 Object나 String으로 받았음
//        int remains = Integer.parseInt(remainsObject);
//        if(remains < quantity){
//            return -1;
//        }else{
//            Long finalremains = redisTemplate.opsForValue().decrement(String.valueOf(productId),quantity);
//            return finalremains.intValue();
//        }
//    }
//}