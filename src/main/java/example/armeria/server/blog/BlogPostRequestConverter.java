package example.armeria.server.blog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.armeria.common.AggregatedHttpRequest;
import com.linecorp.armeria.common.annotation.Nullable;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.annotation.RequestConverterFunction;

import java.lang.reflect.ParameterizedType;
import java.util.concurrent.atomic.AtomicInteger;

public class BlogPostRequestConverter implements RequestConverterFunction {
    //요청 변환기 -> 클라이언트의 요청 본문을 Java 객체로 변환해 준다.
    private static final ObjectMapper mapper = new ObjectMapper();
    private AtomicInteger idGenerator = new AtomicInteger(0); //Blog Post ID

    static String stringValue(JsonNode jsonNode, String field) {
        //JSON 객체에서 특정 키 값을 검색하는 메서드
        JsonNode value = jsonNode.get(field);
        if(value == null) {
            throw new IllegalArgumentException(field + " is missing");
        }
        return value.textValue();
    }


    //convertRequest() 정의
    @Override
    public Object convertRequest(ServiceRequestContext ctx, //요청을 처리하는 컨텍스트로, 요청에 대한 메타데이터와 유틸리티 메소드를 포함한다.
                                 AggregatedHttpRequest request, // 사용자로부터 받은 HTTP 요청을 통합(aggregated) 형태로 제공한다. 통홥된 요청은 HTTP 메시지의 본문을 모두 읽은 상태다.
                                 Class<?> expectedResultType,  // 요청 결과로 기대되는 자바 객체의 클래스 타입이다.
                                 @Nullable ParameterizedType expectedParameterizedResultType) /**기대되는 결과의 파라미터화된 타입을 포함할 수 있는 클래스 타입이다. 이는 제너릭 타입을 다룰 때 사용된다.
                                 파라미터화된 타입이란 제네릭을 사용하여 클래스나 인터페이스에 구체적인 타입을 적용한 것이다.
                                  ex) List<String> 은 List<E> 라는 인터페이스에 String 타입을 파라미터로 제공한 파라미터화된 타입이다.
                                 요약하자면 메서드가 처리해야 할 데이터의 제네릭 타입 정보를 포함하며, 이 정보는 null일 수도 있다.**/
            throws Exception {
        /** 클라이언트로부터 오는 HTTP 요청을 자바 객체 (`BlogPost`)로 변환하는 역할을 합니다.
            이 클래스는 `RequestConverterFunction` 인터페이스를 구현하고 있으며, Armeria 프레임워크의 일부로서 사용자 정의 요청 변환 로직을 제공할 수 있습니다. **/
        if (expectedResultType == BlogPost.class) { //이 변환기는 처리할 수 있는 타입 (`BlogPost`) 인지 확인한다. 매칭되면 내부 로직이 수행됨
            JsonNode jsonNode = mapper.readTree(request.contentUtf8()); // 요청의 본문을 UTF-8 문자열로 읽은 후, Jackson 라이브러리의 `ObjectMapper`를 사용하여 JSON 구조로 파싱한다.
            int id = idGenerator.getAndIncrement(); //새로운 `BlogPost` 객체에 할당될 고유 ID를 생성한다. getAndIncrement() 메서드는 현재 값을 1을 증가시킨 값을 반환한다.
            String title = stringValue(jsonNode, "title"); //JSON 객체에서 "title"과 "content" 필드의 값을 추출한다. 값이 없으면 예외를 던짐
            String content = stringValue(jsonNode, "content");
            return new BlogPost(id, title, content); // Create an instance of BlogPost object
        }
        return RequestConverterFunction.fallthrough(); //요청 처리가 이 변환기에 의해 수행되지 않을 경우, 다른 변환기에 처리를 위임한다. 이는 변환기 체인에서 다음 변환기로 요청을 넘기는 기능이다.

        /** 정리하자면, `convertRequest` 메서드는 HTTP 요청을 받아 그 내용을 파싱하고, 필요한 정보를 추출하여 `BlogPost` 객체를 생성한 후 반환한다. **/
    }
}
