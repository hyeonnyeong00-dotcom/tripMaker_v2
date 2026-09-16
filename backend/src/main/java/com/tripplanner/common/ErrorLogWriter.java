package com.tripplanner.common;

import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * error_log 적재 전담(§5.6-a). {@link GlobalExceptionHandler}의 응답 생성 지점 한 곳에서만 호출한다.
 * <p>별도 트랜잭션({@code REQUIRES_NEW})으로 적재하고 실패는 삼킨다: STORAGE_ERROR는 DB 장애 상황이라
 * 이 insert도 실패하는데, 그때 원래의 에러 응답까지 깨지면 안 되기 때문이다(실패는 파일 로그로만 남긴다).
 * <p>{@code @Transactional}을 쓰지 않고 {@link TransactionTemplate}을 쓰는 이유: 같은 빈 안에서 호출하면
 * 프록시를 타지 않아 전파 속성이 무시되기 때문이다(자기 호출 문제).
 */
@Component
public class ErrorLogWriter {

    private static final Logger log = LoggerFactory.getLogger(ErrorLogWriter.class);

    /** message 컬럼이 과도하게 길어지지 않도록 자른다(클라이언트 노출 메시지 기준). */
    private static final int MAX_MESSAGE_LENGTH = 500;

    private final ErrorLogRepository errorLogRepository;
    private final TransactionTemplate transactionTemplate;

    public ErrorLogWriter(ErrorLogRepository errorLogRepository, PlatformTransactionManager transactionManager) {
        this.errorLogRepository = errorLogRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void write(ErrorCode errorCode, String code, String message, String path) {
        try {
            transactionTemplate.executeWithoutResult(status -> persist(errorCode, code, message, path));
        } catch (RuntimeException e) {
            // 에러 응답 경로에서 다시 예외를 던지면 계약 밖 응답이 나가므로 삼키고 로그만 남긴다.
            log.warn("[{}] error_log 적재 실패 (code={})", ErrorCodes.DB_ACCESS_FAILED, code, e);
        }
    }

    private void persist(ErrorCode errorCode, String code, String message, String path) {
        ErrorLog entity = new ErrorLog();
        entity.setErrorCode(code);
        entity.setErrorCategory(errorCode.name());
        entity.setMessage(truncate(message));
        entity.setPath(path);
        entity.setCreatedAt(OffsetDateTime.now());
        errorLogRepository.save(entity);
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= MAX_MESSAGE_LENGTH ? message : message.substring(0, MAX_MESSAGE_LENGTH);
    }
}
