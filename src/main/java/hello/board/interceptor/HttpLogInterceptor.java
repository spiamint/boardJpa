package hello.board.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Slf4j
public class HttpLogInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String requestURI = request.getRequestURI();
        String queryString = request.getQueryString();
        String method = request.getMethod();
        String session = request.getSession(false) == null ? "null" : request.getSession().getId();

        if (queryString == null) {
            log.info("{} {} session = {}", method, requestURI, session);
        } else {
            log.info("{} {}?{} session = {}", method, requestURI, queryString, session);
        }

        // 다음 인터셉터 진행
        return true;
    }
}