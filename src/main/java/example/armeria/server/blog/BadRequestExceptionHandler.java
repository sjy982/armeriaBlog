package example.armeria.server.blog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.annotation.ExceptionHandlerFunction;


public class BadRequestExceptionHandler implements ExceptionHandlerFunction {
    //블로그 서비스에 대한 예외 처리기
    //Armeria의 인터페이스를 구현하는 사용자 정의 예외 처리기를 선언합니다. ExceptionHandlerFunction
    private static final ObjectMapper mapper = new ObjectMapper(); //JSON 데이터를 생성하거나 파싱하는 데 사용됩니다.

    @Override
    public HttpResponse handleException(ServiceRequestContext ctx, HttpRequest req, Throwable cause) {
        //예외처리기
        if(cause instanceof IllegalArgumentException) {
            String message = cause.getMessage();
            ObjectNode objectNode = mapper.createObjectNode();
            objectNode.put("error", message); //예외의 메시지를 추출하고, JSON 객체를 생성하여 에러 메시지를 "error" 키로 저장합니다.
            return HttpResponse.ofJson(HttpStatus.BAD_REQUEST, objectNode);
        }
        return ExceptionHandlerFunction.fallthrough(); //만약 예외가 IllegalArgumentException이 아닌 경우, 이 메소드는 예외 처리를 다음 예외 처리기로 넘기는 fallthrough()를 호출합니다.
    }
}
