package com.iambilotta.gdpr.starter.audit;

import java.time.Instant;
import java.util.List;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;

import com.iambilotta.gdpr.annotations.GdprPersonalData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PersonalDataAccessAdvisorTest {

    @Test
    void sinkFailureIsAbsorbedNotPropagated() throws Exception {
        AuditSink failing = new AuditSink() {
            @Override
            public void write(AuditAccessRecord record) {
                throw new RuntimeException("downstream sink down");
            }

            @Override
            public List<AuditAccessRecord> findBySubject(String subjectId, Instant from, Instant to) {
                return List.of();
            }
        };
        PersonalDataAccessAdvisor advisor = new PersonalDataAccessAdvisor(
                failing,
                ActorResolver.fixed("test-actor"),
                SubjectIdResolver.byParameterName());

        JoinPoint jp = mockJoinPoint();
        assertThatCode(() -> advisor.capture(jp)).doesNotThrowAnyException();
    }

    @Test
    void deliveriesReachTheSink() throws Exception {
        CapturingSink sink = new CapturingSink();
        PersonalDataAccessAdvisor advisor = new PersonalDataAccessAdvisor(
                sink,
                ActorResolver.fixed("test-actor"),
                SubjectIdResolver.byParameterName());

        advisor.capture(mockJoinPoint());

        assertThat(sink.captured).hasSize(1);
        AuditAccessRecord record = sink.captured.get(0);
        assertThat(record.actor()).isEqualTo("test-actor");
        assertThat(record.targetType()).isEqualTo(SampleTarget.class.getName());
        assertThat(record.targetMember()).isEqualTo("readEmail");
    }

    private static JoinPoint mockJoinPoint() throws NoSuchMethodException {
        java.lang.reflect.Method method = SampleTarget.class.getDeclaredMethod("readEmail", String.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        JoinPoint jp = mock(JoinPoint.class);
        when(jp.getSignature()).thenReturn((Signature) signature);
        when(jp.getArgs()).thenReturn(new Object[]{"alice-1"});
        return jp;
    }

    static class SampleTarget {

        @GdprPersonalData(description = "primary email")
        String readEmail(String subjectId) {
            return "x";
        }
    }

    static class CapturingSink implements AuditSink {

        final java.util.List<AuditAccessRecord> captured = new java.util.ArrayList<>();

        @Override
        public void write(AuditAccessRecord record) {
            captured.add(record);
        }

        @Override
        public java.util.List<AuditAccessRecord> findBySubject(String subjectId, Instant from, Instant to) {
            return List.of();
        }
    }
}
