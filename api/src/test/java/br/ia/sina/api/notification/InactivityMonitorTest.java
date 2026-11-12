package br.ia.sina.api.notification;

import br.ia.sina.api.notification.service.InactivityMonitor;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InactivityMonitorTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    InactivityMonitor monitor;

    @Autowired
    EntityManager em;

    @MockitoBean
    GuardianNotifier notifier;

    @Test
    void avisaUmaVezEVoltaAAvisarDepoisDoHeartbeat() throws Exception {
        Instant now = Instant.now();
        Device idoso = register("PROTECTED");
        setLastSeen(idoso.id(), now.minus(Duration.ofMinutes(40)));

        monitor.check(now);
        monitor.check(now.plus(Duration.ofMinutes(5)));
        verify(notifier, times(1)).notifyInactive(argThat(d -> d.getId().equals(idoso.id())));
        assertThat(notifiedAt(idoso.id())).isNotNull();

        mvc.perform(post("/v1/devices/me/heartbeat").header("Authorization", "Bearer " + idoso.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"listenerEnabled\":true}"))
                .andExpect(status().isNoContent());
        assertThat(notifiedAt(idoso.id())).isNull();

        setLastSeen(idoso.id(), now.minus(Duration.ofMinutes(31)));
        monitor.check(now);
        verify(notifier, times(2)).notifyInactive(argThat(d -> d.getId().equals(idoso.id())));
    }

    @Test
    void falhaNoAvisoDesfazAMarcaETentaDeNovoNoProximoCiclo() throws Exception {
        Instant now = Instant.now();
        Device idoso = register("PROTECTED");
        setLastSeen(idoso.id(), now.minus(Duration.ofMinutes(40)));
        doThrow(new IllegalStateException("falha")).doNothing()
                .when(notifier).notifyInactive(argThat(d -> d.getId().equals(idoso.id())));

        assertThat(monitor.check(now)).isZero();
        assertThat(notifiedAt(idoso.id())).isNull();

        assertThat(monitor.check(now.plus(Duration.ofMinutes(5)))).isEqualTo(1);
        assertThat(notifiedAt(idoso.id())).isNotNull();
        verify(notifier, times(2)).notifyInactive(argThat(d -> d.getId().equals(idoso.id())));
    }

    @Test
    void ignoraAtivoNuncaVistoEFamiliar() throws Exception {
        Instant now = Instant.now();
        Device ativo = register("PROTECTED");
        setLastSeen(ativo.id(), now.minus(Duration.ofMinutes(10)));
        Device nuncaVisto = register("PROTECTED");
        Device familiar = register("GUARDIAN");
        setLastSeen(familiar.id(), now.minus(Duration.ofHours(2)));

        monitor.check(now);

        for (Device d : new Device[]{ativo, nuncaVisto, familiar}) {
            verify(notifier, never()).notifyInactive(argThat(x -> x.getId().equals(d.id())));
            assertThat(notifiedAt(d.id())).isNull();
        }
    }

    private record Device(UUID id, String token) {
    }

    private Device register(String role) throws Exception {
        String body = mvc.perform(post("/v1/devices").contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"" + role + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new Device(UUID.fromString(JsonPath.read(body, "$.deviceId")), JsonPath.read(body, "$.deviceToken"));
    }

    private void setLastSeen(UUID id, Instant at) {
        em.flush();
        jdbc.update("update device set last_seen_at = ? where id = ?", Timestamp.from(at), id);
    }

    private Timestamp notifiedAt(UUID id) {
        em.flush();
        return jdbc.queryForObject("select inactive_notified_at from device where id = ?", Timestamp.class, id);
    }
}
