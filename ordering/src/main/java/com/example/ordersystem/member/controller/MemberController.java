package com.example.ordersystem.member.controller;

import com.example.ordersystem.common.auth.JwtTokenProvider;
import com.example.ordersystem.member.domain.Member;
import com.example.ordersystem.member.dto.LoginDto;
import com.example.ordersystem.member.dto.MemberRefreshDto;
import com.example.ordersystem.member.dto.MemberResDto;
import com.example.ordersystem.member.dto.MemberSaveReqDto;
import com.example.ordersystem.member.service.MemberService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/member")
public class MemberController {
    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;
    @Qualifier("rtdb")
    private final RedisTemplate<String,Object> redisTemplate;

    @Value("${jwt.secretKeyRt}")
    private String secretKeyRt;

    public MemberController(MemberService memberService, JwtTokenProvider jwtTokenProvider,@Qualifier("rtdb") RedisTemplate<String,Object> redisTemplate) {
        this.memberService = memberService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
    }

    @PostMapping("/create")
    public ResponseEntity<?> memberCreate(@RequestBody MemberSaveReqDto memberSaveReqDto){
        Long memberId = memberService.save(memberSaveReqDto);
        return new ResponseEntity<>(memberId, HttpStatus.CREATED);
    }
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")// 가장 편한 방법, ROLE_ 붙일 필요없음. 예외는 filter 레벨에서 발생.
    public ResponseEntity<?> memberList(){
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        if(!authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))){
//            throw new AccessDeniedException("잘못된 경로입니다.");
//        } //admin만 접근 가능하게 하는 메서드
        List<MemberResDto> memberResDto = memberService.findAll();
        return new ResponseEntity<>(memberResDto,HttpStatus.OK);
    }
    @GetMapping("/myinfo")
    public ResponseEntity<?> myinfo(){
        MemberResDto dto = memberService.myInfo();
        return new ResponseEntity<>(dto,HttpStatus.OK);
    }

    @PostMapping("/doLogin")
    public ResponseEntity<?> doLogin(@RequestBody LoginDto dto){
//        email, password 검증
        Member member = memberService.login(dto);
//        토큰생성 및 return
        String token = jwtTokenProvider.createToken(member.getEmail(),member.getRole().toString());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getEmail(),member.getRole().toString());
//        redis에 rt 저장
        redisTemplate.opsForValue().set(member.getEmail(), refreshToken, 200, TimeUnit.DAYS);//200일 ttl
//        사용자에게 at, rt 지급
        Map<String , Object> loginInfo = new HashMap<>();
        loginInfo.put("id",member.getId());
        loginInfo.put("token",token);
        loginInfo.put("refreshToken",refreshToken);
        return new ResponseEntity<>(loginInfo,HttpStatus.OK);
    }
    @PostMapping("/refresh-token")
    public ResponseEntity<?> generateNewAt(@RequestBody MemberRefreshDto dto){
//        rt디코딩 후 email 추출
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKeyRt)
                .build()
                .parseClaimsJws(dto.getRefreshToken())
                .getBody();
//        rt를 redis의 rt비교 검증
        Object rt =  redisTemplate.opsForValue().get(claims.getSubject());
        if(rt == null || !rt.toString().equals(dto.getRefreshToken())){
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
//        at생성하여 지급
        String token = jwtTokenProvider.createToken(claims.getSubject(),claims.get("role").toString());
        Map<String , Object> loginInfo = new HashMap<>();
        loginInfo.put("token",token);
        return new ResponseEntity<>(loginInfo,HttpStatus.OK);
    }


}
