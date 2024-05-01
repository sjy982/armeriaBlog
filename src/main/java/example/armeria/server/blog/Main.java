package example.armeria.server.blog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linecorp.armeria.server.Server; //Armeria 서버 인스턴스를 나타냅니다.
import com.linecorp.armeria.server.ServerBuilder; //ServerBuilder는 Server 인스턴스를 구성하고 만드는 데 사용되는 빌더 클래스입니다.
import com.linecorp.armeria.server.docs.DocService;

//https://velog.io/@tlsdntjd95/%EC%B4%88%EB%B3%B4-%EA%B0%9C%EB%B0%9C%EC%9E%90-%EC%98%A4%ED%94%88%EC%86%8C%EC%8A%A4-Armeria%EC%97%90-%EA%B8%B0%EC%97%AC%ED%95%B4%EB%B3%B4%EA%B8%B0 -> 기여 후기
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class); //로거 객체를 생성하여 이 클래스에서 발생하는 로그를 관리한다.
    public static void main(String[] args) {
        Server server = newServer(8080); //포트 8080에서 실행될 서버를 생성한다.

        server.closeOnJvmShutdown(); // JVM이 종료될 때 서버가 자동으로 닫히도록 설정한다.

        server.start().join(); // 서버를 시작하고, 현재 스레드가 서버가 종료될 때까지 대기하도록 join 메소드를 호출한다. 이는 서버가 계속 실행되도록 한다.

        logger.info("Server has been started. Serving dummy service at http://127.0.0.1:8080",
                server.activeLocalPort()); //서버가 시작되면 로그를 통해 서버가 시작되었으며, 어떤 주소에서 서비스를 제공하는지 알려줍니다. server.activeLocalPort()는 서버가 실제로 사용 중인 포트 번호를 반환합니다.
    }

    static Server newServer(int port) {
        ServerBuilder sb = Server.builder(); //인스턴스를 생성합니다. 이 빌더는 서버 구성을 위해 사용됩니다.
        DocService docService =
                DocService.builder()
                          .exampleRequests(BlogService.class,
                                "createBlogPost",
                                "{\"title\":\"My first blog\", \"content\":\"Hello Armeria!\"}")
                          .build();
        return sb.http(port) //서버가 리스닝할 HTTP 포트를 설정합니다.
                 .annotatedService(new BlogService())
                 .serviceUnder("/docs", docService)
                 .build();

//                .service("/", (ctx, req) -> HttpResponse.of("Hello, Armeria!")) //특정 HTTP 경로에 대한 요청을 처리할 서비스를 등록합니다. (String path: 서비스가 반응할 URI 경로, HttpService service: 해당 경로에 대한 요청을 처리할 서비스 객체입니다. 이 객체는 요청을 받아 응답을 생성합니다.)
//                .build(); // 설정된 옵션을 바탕으로 Server 인스턴스를 생성합니다.
    }
}