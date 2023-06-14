package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach // 각의 테스트 메서드가 실행되기 전에 수행해야 할 초기화 작업
    public void before() {
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

    }

    // JPQL 버젼
    @Test
    public void startJPQL() {
        // member1을 찾아라
        String qlstring = "select m from Member m where m.username =:username";

        Member findMember = em.createQuery(qlstring, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    // QueryDSL 버젼
    @Test
    public void startQuerydsl() {

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1")) // 파라미터 바인딩 처리
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");

        // 장점1. 컴파일 시 오류를 잡아줌
        // 장점2. 파라미터 바인딩을 해결해줌
    }

    @Test
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"),
                        member.age.eq(10))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetchTest(){
        // List조회
//        List<Member> fetch = queryFactory
//                .selectFrom(member)
//                .fetch();
//
//        // 단 건
//        Member findMember1 = queryFactory
//                .selectFrom(member)
//                .fetchOne();
//
//        // 처음 한 건 조회
//        Member fondMember2 = queryFactory
//                .selectFrom(member)
//                .fetchFirst();

        // 페이징에서 사용
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();

        results.getTotal();
        // count 쿼리로 변경
        long count = queryFactory
                .selectFrom(member)
                .fetchCount();
    }

    /*
    *  회원 정렬 순서
    *  1. 회원 나이 내림차순
    *  2. 회원 이름 올림차순
    * 이름이 없으면 마지막에 출력(nulls last
    * */
    @Test
    public void sort(){
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();

    }

    @Test
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)  // 몇번째 부터 시작할 것이냐, 하나를 skip
                .limit(2)
                .fetch();
        assertThat(result.size()).isEqualTo(2);
    }


    // 전체 조회수 보기
    @Test
    public void paging2() {
        QueryResults<Member> QueryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)  // 몇번째 부터 , 하나를 skip
                .limit(2)   // 몇번째 까지
                .fetchResults();
        assertThat(QueryResults.getTotal()).isEqualTo(4);
        assertThat(QueryResults.getLimit()).isEqualTo(2);
        assertThat(QueryResults.getOffset()).isEqualTo(1);
        assertThat(QueryResults.getResults().size()).isEqualTo(2);

    }
}
