package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberDto {

    private String username;
    private int age;

    /**
     * 어노테이션 달고 compile querydsl 해줘야함
     * 그러면 DTO도 Q파일로 생성
     * QueryDsl 라이브러리 의존성이 생긴다는 단점
     */
    @QueryProjection  
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
