package example.armeria.server.blog;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.MediaType;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.testing.junit5.server.ServerExtension;

//`assertThatJson와 assertThat은 JSON 데이터와 일반 객체를 검증하기 위한 메서드를 제공
//JsonProcessingException, ObjectMapper는 JSON 데이터를 java 객체로 변환하거나 그 반대로 변환할 때 사용됨


@TestMethodOrder(OrderAnnotation.class) //테스트 메서드가 실행될 순서로 구성하기 위한 어노테이션
public class BlogServiceTest {
    //Armeria 프레임워크를 사용하여 작성된 웹 서버의 HTTP API를 테스트하는 JUnit 테스트 클래스
    private static final ObjectMapper mapper = new ObjectMapper(); //JSON 데이터와 Java 객체 간의 변환을 담당하는 ObjectMapper 인스턴스다.

    @RegisterExtension //JUnit 5의 확장 기능을 등록하는 어노테이션 server 필드에 적용되어 ServerExtension을 통해 테스트 서버를 구성한다.
    static final ServerExtension server = new ServerExtension() {
        @Override
        protected void configure(ServerBuilder sb) throws Exception {
            //ServerBuilder를 사용하여 BlogService를 서버에 등록함으로써, 이 서비스에 대한 HTTP 요청을 처리할 수 있도록 설정
            sb.annotatedService(new BlogService());
        }
    };

    private static HttpRequest createBlogPostRequest(Map<String, String> content)
            throws JsonProcessingException {
        //이 메서드는 블로그 포스트를 생성하는 HTTP POST 요청을 구성한다. 이 메서드는 JSON으로 포맷된 본문 내용을 요청에 포함한다.
        return HttpRequest.builder()
                .post("/blogs")
                .content(MediaType.JSON_UTF_8, mapper.writeValueAsString(content))
                .build();
    }

    @Test
    @Order(1)
    void createBlogPost() throws JsonProcessingException {
        //webClinet를 사용하여 서버에 POST 요청을 보내고, 응답을 검증한다.
        //assertThatJson을 사용하여 JSON 응답이 예상하는 값과 일치하는지 확인한다.
        //whenIgnoringPaths는 JSON에서 특정 경로 (여기서는 생성 및 수정 날짜)를 무시할 때 사용된다.
        final WebClient client = WebClient.of(server.httpUri()); //주어진 URI에서 작동할 WebClient 객체를 생성한다. 여기서 Server.httpUri()는 ServerExtension에 의해 구성된 테스트 서버의 주소를 반환한다. 이 주소는 테스트 서버가 실제 네트워크 상에서 수신 대기하고 있는 HTTP URI다.
        final HttpRequest request = createBlogPostRequest(Map.of("title", "My first blog",
                "content", "Hello Armeria!"));
        final AggregatedHttpResponse res = client.execute(request).aggregate().join(); //.execute()는 HTTPRequest 객체를 사용하여 서버에 요청을 보낸다. 이 메서드는 비동기적으로 실행되며 CompletableFuture를 반환한다.
        //.aggregate()는 HTTPResponse를 AggregateHttpResponse로 변환한다. 이는 비동기 응답을 하나의 객체로 모으고, 모든 응답 데이터를 메모리에 로드할 수 있게 된다. 이 과정 또한 비동기적으로 처리되며 결과적으로 CompletableFuture<AggregatedHttpResponse> 객체를 반환합니다.
        //.join()은 CompletableFuture의 실행이 완료될 때까지 현재 스레드를 블로킹한다. 실행이 완료되면 결과 AggregatedHttpResponse를 반환한다. 이 객체에는 응답의 상태코드, 헤더, 본문 등이 포함되어 있다.

        final Map<String, Object> expected = Map.of("id", 0,
                "title", "My first blog",
                "content", "Hello Armeria!");

        assertThatJson(res.contentUtf8()).whenIgnoringPaths("createdAt", "modifiedAt")
                .isEqualTo(mapper.writeValueAsString(expected));

        /**
         *
         * HTTPResponse와 AggregatedHttpResponse
         * 1. HTTPResponse의 비동기적 특성:
         * Armeria 같은 비동기 웹 프레임워크에서 HttpResponse는 스트림 형태로 데이터를 전송합니다. 즉, 데이터는 여러 조각(chunk)으로 나눠져 시간에 따라 서버로부터 클라이언트로 전송될 수 있습니다. 이는 대용량 데이터를 처리하거나, 서버가 데이터를 즉시 모두 사용할 수 없을 때 유용합니다.
         *
         * 2. Aggregation의 필요성:
         * 비동기 응답을 한 번에 처리하고 싶거나 응답 전체를 한 객체에 담아서 사용하기 편리할 경우 aggregate() 함수를 사용합니다. 이 메서드는 HttpResponse의 모든 데이터를 수집하여, 그것이 완전히 수신될 때까지 기다린 후, 하나의 AggregatedHttpResponse 객체로 변환합니다.
         * 이 과정은 모든 응답 데이터를 메모리에 로드하기 때문에, 작은 응답에 대해서는 효과적이지만, 매우 큰 응답에 대해서는 메모리 문제를 일으킬 수 있습니다.
         *
         * 3. 단일 스레드 vs. 멀티 스레드:
         * 응답을 AggregatedHttpResponse로 변환하는 것은 일반적으로 단일 요청에 대해 수행됩니다. 비록 웹 서버 내부에서 여러 스레드가 다른 요청을 병렬로 처리할 수 있지만, 개별 요청과 그 응답은 특정 스레드 또는 스레드 풀에 의해 독립적으로 처리됩니다.
         * aggregate()는 이렇게 비동기적으로 발생하는 데이터 조각들을 모아 최종적인 응답 데이터를 구성합니다. 이는 여러 스레드의 작업을 합치는 것이 아니라, 단일 요청에 대한 데이터 조각들을 시간에 따라 수집하는 과정입니다.
         *
         * 결론적으로, AggregatedHttpResponse로의 변환은 클라이언트가 서버의 응답을 더 쉽게 사용할 수 있도록 전체 응답을 하나의 객체로 집약하는 것을 목적으로 합니다. 이는 멀티 스레드에서 작업을 합치는 것이 아니라, 단일 요청의 전체 응답을 효율적으로 처리하기 위한 메커니즘입니다.
         * **/
    }

    @Test
    @Order(2)
    void getBlogPost() throws JsonProcessingException {
        final WebClient client = WebClient.of(server.httpUri());
        final AggregatedHttpResponse res = client.get("/blogs/0").aggregate().join();
        final Map<String, Object> expected = Map.of(
                "id", 0,
                "title", "My first blog",
                "content", "Hello Armeria!"
        );

        assertThatJson(res.contentUtf8()).whenIgnoringPaths("createdAt", "modifiedAt")
                .isEqualTo(mapper.writeValueAsString(expected));
    }

    @Test
    @Order(3)
    void getBlogPosts() throws JsonProcessingException {
        final WebClient client = WebClient.of(server.httpUri());
        final HttpRequest request = createBlogPostRequest(Map.of(
                "title", "My second blog",
                "content", "Armeria is awesome!")
        );
        client.execute(request).aggregate().join();
        final AggregatedHttpResponse res = client.get("/blogs").aggregate().join();
        final List<Map<String, Object>> expected = List.of(
                Map.of("id", 1,
                        "title", "My second blog",
                        "content", "Armeria is awesome!"),
                Map.of("id", 0,
                        "title", "My first blog",
                        "content", "Hello Armeria!")
        );
        assertThatJson(res.contentUtf8()).whenIgnoringPaths("[*].createdAt", "[*].modifiedAt")
                .isEqualTo(mapper.writeValueAsString(expected));
    }

    @Test
    @Order(4)
    void updateBlogPosts() throws JsonProcessingException {
        final WebClient client = WebClient.of(server.httpUri());
        final Map<String, Object> updateContent = Map.of(
                "id", 0,
                "title", "My first blog",
                "content", "Hello awesome Armeria!"
        );
        final HttpRequest updateBlogPostRequest =
                HttpRequest.builder()
                        .put("/blogs/0")
                        .content(MediaType.JSON_UTF_8, mapper.writeValueAsString(updateContent))
                        .build();
        client.execute(updateBlogPostRequest).aggregate().join();
        final AggregatedHttpResponse res = client.get("/blogs/0").aggregate().join();
        final Map<String, Object> expected = Map.of(
                "id", 0,
                "title", "My first blog",
                "content", "Hello awesome Armeria!"
        );

        assertThatJson(res.contentUtf8()).whenIgnoringPaths("createdAt", "modifiedAt")
                .isEqualTo(mapper.writeValueAsString(expected));
    }

    @Test
    @Order(5)
    void badRequesetExceptionHandlerWhenTryingDeleteMissingBlogPost() throws JsonProcessingException {
        final WebClient client = WebClient.of(server.httpUri());
        final AggregatedHttpResponse res = client.delete("/blogs/100").aggregate().join();
        assertThat(res.status()).isSameAs(HttpStatus.BAD_REQUEST);
        assertThatJson(res.contentUtf8()).isEqualTo("{\"error\":\"The blog post does not exist. ID: 100\"}");
    }
}
