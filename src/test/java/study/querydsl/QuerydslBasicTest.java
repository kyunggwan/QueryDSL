package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;


import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

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
    public void resultFetchTest() {
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

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순
     * 2. 회원 이름 올림차순
     * 이름이 없으면 마지막에 출력(nulls last
     */
    @Test
    public void sort() {
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

    @Test
    public void aggregation() {
        // 데이터 타입이 여러개가 들어오기 때문에 Tuple을 썼다
        // 많이 안씀, DTO를 많이 쓴다
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetch();
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     **/
    @Test
    public void group() throws Exception {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * teamA에 속한 모든 회원을 찾아라
     */
    @Test
    public void join() throws Exception {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)    // member의 연관관계 칼럼, 조인 테이블
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 예시로 해봄, 연관관게가 없는 field join
     * 세타 조인
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    public void theta_join() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team) // 연관관계가 없으므로 그냥 나열
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL 문법 예시
     * select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    public void join_on_filtering() {
        // tuple인 이유: select가 여러가지 타입(member, team)
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA")) // leftJoin시 조건
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple" + tuple);
        }
    }

    /**
     * 연관관계 없는 엔티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     */
    @Test
    public void join_on_no_relation() {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple" + tuple);
        }
    }

    /**
     * fetchJoin이 없을 때
     **/
    @PersistenceUnit // 증명할때 사용
            EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() throws Exception {

        em.flush(); // 현재까지의 모든 변경사항을 DB에 반영
        em.clear(); // EntityManager의 상태 초기화, 캐쉬된 엔티티를 비움

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    /**
     * 패치 조인 적용
     **/
    @Test
    public void fetchJoinUse() throws Exception {

        em.flush(); // 현재까지의 모든 변경사항을 DB에 반영
        em.clear(); // EntityManager의 상태 초기화, 캐쉬된 엔티티를 비움

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    /**
     * 서브쿼리
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery() throws Exception {

        // alias가 중복되면 안되기에 만듦
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(   // 서브쿼리
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    /**
     * 서브쿼리
     * 나이가 평균 이상인 회원 조회
     */
    @Test
    public void subQueryGoe() throws Exception {

        // alias가 중복되면 안되기에 만듦
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(   // 서브쿼리
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }

    /**
     * 서브쿼리 - select
     * 유져 평균 나이 출력
     * jpa 서브 쿼리의 한계점: from 절에 subquery가 안된다
     * 해결방안 1: 대게 서브쿼리를 join으로 변경 가능
     * 해결방안 2: 쿼리를 2번 분리해서 실행
     * 해결방안 3: nativeSQL
     */
    @Test
    public void selectSubQuery() throws Exception {

        // alias가 중복되면 안되기에 만듦
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(
                        member.username,
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple" + tuple);
        }
    }

    @Test
    public void basicCase() throws Exception {

        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * DB에서 가급적 조작하지말고, front에서 하는게 좋음
     */
    @Test
    public void complexCase() throws Exception {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 상수 추가
     */
    @Test
    public void constant() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 문자에 추가
     */
    @Test
    public void concat() throws Exception {
        List<String> result = queryFactory
                // 문자가 아닌 다른 타입들을 stringValue()로 문자로 변환
                // 보통 ENUM을 처리할 때 자주 사용
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void simpleProjection() throws Exception {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        System.out.println("result = " + result);
    }

    @Test
    public void tupleProjection() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    /**
     * 순수 JPQL에서 DTO를 조회할 때는 new 명령어를 사용해야함
     * 생성자 방식만 지원함
     */
    @Test
    public void findDtoByJPQL() throws Exception {
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("MemberDto = " + memberDto);
        }
    }

    /**
     * QueryDSL은 아래 3가지 방법을 지원한다
     * 1. 프로퍼티 접근
     * 기본생성자에 setter를 통해서 주입
     **/
    @Test
    public void findDtoBySetter() {
        // Projections라는 queryDSL 문법?으로 이름과 나이를 주입
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("MemberDto = " + memberDto);
        }
    }

    /**
     * QueryDSL
     * 2. 필드 직접 접근
     * Field에 바로 주입
     **/
    @Test
    public void findDtoByField() {
        // Projections라는 queryDSL 문법?으로 이름과 나이를 주입
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("MemberDto = " + memberDto);
        }
    }

    /**
     * QueryDSL
     * 3. 생성자 사용
     * 생성자에 주입
     **/
    @Test
    public void findDtoByConstructor() {
        // Projections라는 queryDSL 문법?으로 이름과 나이를 주입
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("MemberDto = " + memberDto);
        }
    }

    /**
     * 3-1. 생성자 주입방식에 QueryProjection 까지 지원한다
     * field. setter의 경우 이름 매칭이 중요
     * allias. subquery시 방법
     * 단점 : dto도 querydsl의존, 의존도가 높아짐
     */
    @Test
    public void findUserDto() {
        QMember memberSub = new QMember("memberSub");
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        // 서브쿼리
                        Expressions.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    public void findUserDtoByConstructor() {
        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    /**
     * 위의 함수와 차이
     * 타입 미스가 난다
     * 위의 constructor는 실행해서 앎(런타임 오류)
     * 아래는 compile 시점에 타입 미스로 앎
     */
    @Test
    public void findDtoByQueryProjection() throws Exception {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 동적 쿼리를 해결하는 두가지 방식
     * 1. BooleanBuilder
     * 2. Where 다중 파라미터 사용
     *
     * 1. BooleanBuilder
     */
    @Test
    public void dynamicQuery_BooleanBuilder() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        // 파라미터가 존재하면 해당 파라미터가 들어감
        BooleanBuilder builder = new BooleanBuilder();
        // 유저가 비면 안되는 경우 이렇게 초기값 세팅도 가능
        // BooleanBuilder builder = new BooleanBuilder(member.username.eq(usernameCond));
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }
        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        /**
         * 동적 쿼리
         * 메인쿼리 부분
         * name만 있는 경우, name으로만 검색
         * name, age가 있는 경우, name, age로 검색
         */
        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    /**
     * 2. Where 다중 파라미터
     * where조건에 null값은 무시된다.
     * 메서드를 다른 쿼리에서도 재활용 할 수 있다
     * 쿼리 자체의 가독성이 높아진다
     */
    @Test
    public void dynamicQuery_WhereParam() throws Exception {
        // 초기값 세팅
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        // where 다중파라미터는 메인쿼리가 먼저 나옴
        // 동적 쿼리를 쓰는지 더 파악하기 쉽다고 함
        return queryFactory
                .selectFrom(member)
//                .where(usernameEq(usernameCond), ageEq(ageCond))
                .where(allEq(usernameCond, ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    // 장점: 이렇게 조립해서 씀!!, 쿼리 조합!
    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    // 광고 상태 isServicable, 날짜가 IN
//    private BooleanExpression isServicable(String usernameCond, Integer ageCond) {
//        return isValid(usernameCond).and(DateBetweenIn(ageCond));
//    }
}
