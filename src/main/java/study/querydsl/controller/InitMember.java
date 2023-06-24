package study.querydsl.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

@Profile("local") // application.yml의 active의 profile: local인 것에 작동
@Component
@RequiredArgsConstructor
/**
 * 스프링 실행 시 자동으로 데이터를 넣기 위한 초기화 클래스입니다.
 * 조회용 API를 사용하기 위한 설정을 수행합니다.
 */
public class InitMember {

    private final InitMemberService initMemberService;

    /**
     * 스프링 빈의 라이프사이클 중 하나인 초기화(initialization) 단계에서 호출
     * 필요한 리소스를 준비하거나 외부 서비스와의 연결을 설정
     */
    @PostConstruct // main이 실행되면 실행
    public void init() {
        initMemberService.init();
    }

    @Service
    static class InitMemberService {
        @PersistenceContext
        private EntityManager em;

        @Transactional
        /**
         * 이 부분을 바로 생성자에 못넣는 이유
         *   : 스프링 라이프 사이클이 있어서 @Transactional 못 씀
         */
        public void init() {
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");
            em.persist(teamA);
            em.persist(teamB);

            for (int i = 0; i < 100; i++) {
                Team selectedTeam = i % 2 == 0 ? teamA : teamB;
                em.persist(new Member("member" + i, i, selectedTeam));
            }

        }
    }
}
