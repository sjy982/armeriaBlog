package example.armeria.server.blog;

import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.server.annotation.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BlogService {
    private final  Map<Integer, BlogPost> blogPosts = new ConcurrentHashMap<>();

    @Post("/blogs")
    @RequestConverter(BlogPostRequestConverter.class) //요청 변환기 등록
    public HttpResponse createBlogPost(BlogPost blogPost) {
        blogPosts.put(blogPost.getId(), blogPost);
        return HttpResponse.ofJson(blogPost); //생성된 게시물의 정보를 Armeria's를 통해 HTTP 응답으로 생성하고 반환한다.
    }

    @Get("/blogs/:id")
    public HttpResponse getBlogPost(@Param int id) {
        BlogPost blogPost = blogPosts.get(id);
        return HttpResponse.ofJson(blogPost);
    }

    @Get("/blogs") //이 어노테이션은 HTTP GET 요청을 이 메소드와 연결시키며, URL 경로는 "/blogs"다.
    @ProducesJson //이 어노테이션은 메소드가 JSON 형식의 응답을 생성한다는 것을 나타낸다.
    public Iterable<BlogPost> getBlogPosts(@Param @Default("true") boolean descending) {
        //반환 타입 Interable<BlogPost>는 블로그 포스트 객체의 반복가능한 컬렉션이다.
        if(descending) {
            //내림차순
            return blogPosts.entrySet()
                    .stream() // blogPosts 맵의 entrySet()을 호출하여 맵의 모든 엔트리를 스트림으로 변환한다.
                    .sorted(Collections.reverseOrder(Comparator.comparingInt(Entry::getKey))) //스트림의 엔트리들을 키(id)를 기준으로 정렬한다.
                    .map(Entry::getValue).collect(Collectors.toList()); //각 엔트레에서 getValue 메서드를 통해 BlogPost를 추출하고 이들을 리스트로 수집한다.
        }
        //오름차순
        return blogPosts.values().stream().collect(Collectors.toList()); //a맵의 값들을 직접 스트림으로 변환하고, 이들을 리스트로 수집하여 반환한다. 이는 기본적인 오름차순(삽입 순서)에 따른다.

        /**
         * `Entry set`은 Java의 `Map` 인터페이스에서 맵의 모든 키-값 쌍을 `Entry` 객체의 집합으로 반환하는 메소드인
         * `entrySet()`을 통해 얻어진다. `Entry`는 `Map.Entry` 인터페이스의 인스턴스로,
         * 맵의 한 쌍의 키(`getKey()`)와 (`getValue()`)를 포함한다.
         *
         * `Stream`은 Java 8에서 도입된 Stream API의 일부로, 컬렉션(리스트, 세트) 또는 배열 등의 요소를
         *  하나씩 참조하면서 람다식을 사용하여 반복, 필터, 맴핑 등의 연산을 수행할 수 있는 연속된 요소들의
         *  시퀀스다. 스트림을 사용함으로써 개발자는 데이터를 보다 선언적으로 처리할 수 있으며, 멀티스레드 환경
         *  에서도 안전하게 처리할 수 있다.
         *  -> 스트림을 사용하는 이유는 데이터를 파이프라인 방식으로 처리할 수 있게 하여, 데이터 컬렉션을 더 유연하고 효율적으로
         *  처리할 수 있기 때문이다.
         *  ->파이프라인은 여러 작업(필터링, 정렬, 매핑 등)이 연쇄적으로 이어지는 과정을 의미한다.
         *  각 작업은 입력을 받아 처리한 뒤, 그 결과를 다음 작업의 입력으로 전달한다. 이런 방식은 데이터 처리과정을 고도로 추상화
         *  하고, 각 단계를 명확하게 분리하여 유연하고 이해하기 쉬운 코드 작성을 가능하게 한다.
         *
         *
         *  `Comparator`는 Java에서 객체를 비교하는 메커니즘을 제공하는 인터페이스다. 이를 통해 사용자 정의
         *  비교 방법을 구현할 수 있으며,
         *  `Comparator.comparingInt`는 특히 정수 값을 비교하는 비교자를 생성한다.
         *
         *  `collect(Collectors.toList())`: 스트림의 모든 요소를 리스트로 수집한다. 리스트로 수집하는 이유는
         *  처리된 결과를 하나의 컬렉션으로 모으기 위해, 또한 API 사용자에게 표준적인 자료구조인 리스트 형태로
         *  데이터를 제공하기 위함이다.
          **/
    }

    @Put("/blogs/:id")
    public HttpResponse updateBlogPost(@Param int id, @RequestObject BlogPost blogPost) {
        //@RequestObject 요청 본문을 Java 객체로 변환해 준다.
        BlogPost oldBlogPost = blogPosts.get(id);

        if(oldBlogPost == null) {
            return HttpResponse.of(HttpStatus.NOT_FOUND); //Armeria의 컨테이닝을 사용하여 응답을 반환한다.
        }
        BlogPost newBlogPost = new BlogPost(
                id, blogPost.getTitle(),
                blogPost.getContent(),
                oldBlogPost.getCreatedAt(),
                blogPost.getCreatedAt()
        );
        blogPosts.put(id, newBlogPost);
        return HttpResponse.ofJson(newBlogPost);
    }

    @Blocking //실제 서비스를 사용하면 데이터베이스에 엑세스하고 운영하는 데 시간이 걸립니다. EventLoop가 블로킹되지 않도록 이러한 blocking 작업을 차단 작업 실행자(blocking 작업을 위임하는 거임)에게 넘겨야 합니다. 그 방법이 @Blocking
    @Delete("/blogs/:id")
    @ExceptionHandler(BadRequestExceptionHandler.class)
    public HttpResponse deleteBlogPost(@Param int id) {
        BlogPost removed = blogPosts.remove(id);

        if(removed == null) {
            //삭제할 게시물이 없다면
            throw new IllegalArgumentException("The blog post does not exist. ID: " + id);
        }
        return HttpResponse.of(HttpStatus.NO_CONTENT);
    }
}
