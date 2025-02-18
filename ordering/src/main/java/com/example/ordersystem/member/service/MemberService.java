package com.example.ordersystem.member.service;

import com.example.ordersystem.member.domain.Member;
import com.example.ordersystem.member.dto.LoginDto;
import com.example.ordersystem.member.dto.MemberResDto;
import com.example.ordersystem.member.dto.MemberSaveReqDto;
import com.example.ordersystem.member.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Long save(MemberSaveReqDto memberSaveReqDto){
        Optional<Member> optionalMember =  memberRepository.findByEmail(memberSaveReqDto.getEmail());
        if(optionalMember.isPresent()){
            throw new IllegalArgumentException("기존에 존재하는 회원입니다.");
        }
        String password = passwordEncoder.encode(memberSaveReqDto.getPassword());
        Member member = memberRepository.save(memberSaveReqDto.toEntity(password));
        return  member.getId();
    }
    public List<MemberResDto> findAll(){
        List<Member> members  = memberRepository.findAll();
        return members.stream().map(a->a.fromEntity()).collect(Collectors.toUnmodifiableList());
    }
    public MemberResDto myInfo(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        Member member = memberRepository.findByEmail(email).orElseThrow(()->new EntityNotFoundException("member is not found"));
        return member.fromEntity();
    }
    public Member login(LoginDto dto){
//      email 존재여부
//            boolean check = true;
//            Optional<Member> optionalMember = memberRepository.findByEmail(dto.getEmail());
//            if(!optionalMember.isPresent()){
//                check = false;
//            }
//            if(!passwordEncoder.matches(dto.getPassword(), optionalMember.get().getPassword())){
//                check =false;
//            }
//            if(!check){
//                throw new IllegalArgumentException("email 또는 비밀번호가 일치하지 않습니다.");
//            }
//            return optionalMember.get();
//        } 위방법도 좋아보임

        Member member = memberRepository.findByEmail(dto.getEmail()).orElseThrow(()->new IllegalArgumentException("존재하지 않는 이메일입니다."));

//        패스워드 일치 여부
        if(!passwordEncoder.matches(dto.getPassword(),member.getPassword())){
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        return member;
    }
}
