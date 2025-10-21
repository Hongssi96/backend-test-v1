package im.bigs.pg.infra.pg.adapter

import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.application.pg.port.out.PgClientOutPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.LocalDateTime


@Component
class TestPgAdapter(
    @Value("\${pg.test.base-url}") private val baseUrl: String
) : PgClientOutPort {

    private val client: RestClient = RestClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
        .build()

    override fun supports(partnerId: Long): Boolean {
        return true
    }

    override fun approve(request: PgApproveRequest): PgApproveResult {
        val masked = "[bin=${request.cardBin}, last4=${request.cardLast4}]"
        val res = client.post()
            .uri("/api/v1/payments/approve")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .onStatus({ it.is4xxClientError || it.is5xxServerError }) { _, body ->
                throw IllegalStateException("TestPg approve failed $masked")
            }
            .body(TestPgApproveResponse::class.java)
            ?: throw IllegalStateException("Null response from TestPg $masked")

        return PgApproveResult(
            approvalCode = res.approvalCode,
            approvedAt = res.approvedAt
        )
    }

    private data class TestPgApproveResponse(
        val approvalCode: String,
        val approvedAt: LocalDateTime
    )
}
