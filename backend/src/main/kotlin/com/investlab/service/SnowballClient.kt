package com.investlab.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.investlab.model.KlinePoint
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Component
class SnowballClient(
    private val settingService: SettingService,
    private val objectMapper: ObjectMapper
) {
    private val client = OkHttpClient()
    private val baseUrl = "https://stock.xueqiu.com"

    fun fetchKlines(symbol: String, period: String, count: Int = 400): List<KlinePoint> {
        val token = settingService.getByKey("snowball.token")?.value
            ?: throw IllegalStateException("缺少雪球 token，请在设置中配置 key=snowball.token")
        val u = settingService.getByKey("snowball.u")?.value
            ?: throw IllegalStateException("缺少雪球 token，请在设置中配置 key=snowball.u")
        val url =
            "$baseUrl/v5/stock/chart/kline.json?symbol=$symbol&begin=${System.currentTimeMillis()}&period=$period&type=before&count=-$count&indicator=kline"
        val req = Request.Builder()
            .url(url)
            .header("Cookie", "xq_a_token=$token;u=$u")
            .header("User-Agent", "Mozilla/5.0")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException("雪球请求失败: ${resp.code}")
            }
            val body = resp.body?.string() ?: throw IllegalStateException("雪球返回空响应")
            val node = objectMapper.readTree(body)
            val data = node.get("data") ?: throw IllegalStateException("雪球返回缺少data")
            val column = data.get("column").map { it.asText() }
            val items = data.get("item")
            return parseKlines(column, items)
        }
    }

    private fun parseKlines(column: List<String>, items: JsonNode): List<KlinePoint> {
        // columns usually: ["timestamp","volume","open","high","low","close", ...]
        val idxTs = column.indexOf("timestamp")
        val idxOpen = column.indexOf("open")
        val idxHigh = column.indexOf("high")
        val idxLow = column.indexOf("low")
        val idxClose = column.indexOf("close")
        val idxVol = column.indexOf("volume")
        val results = mutableListOf<KlinePoint>()
        items.forEach { arr ->
            val ts = arr[idxTs].asLong()
            val date = Instant.ofEpochMilli(ts).atZone(ZoneId.of("Asia/Shanghai")).toLocalDate()
            fun num(i: Int) = arr[i].decimal()
            results.add(
                KlinePoint(
                    date = date,
                    open = num(idxOpen),
                    high = num(idxHigh),
                    low = num(idxLow),
                    close = num(idxClose),
                    volume = num(idxVol)
                )
            )
        }
        return applyIndicators(results)
    }

    private fun JsonNode.decimal(): BigDecimal =
        if (this.isNumber) this.decimalValue() else BigDecimal(this.asText("0"))

    // same indicator calculation as mock
    private fun applyIndicators(list: List<KlinePoint>): List<KlinePoint> {
        val closes = list.map { it.close }
        val ma = fun(window: Int, idx: Int): BigDecimal? {
            if (idx + 1 < window) return null
            val slice = closes.subList(idx + 1 - window, idx + 1)
            return slice.reduce { acc, v -> acc + v }
                .divide(BigDecimal(window), 4, RoundingMode.HALF_UP)
        }
        val period = 20
        val k = BigDecimal("2")

        return list.mapIndexed { idx, p ->
            val ma20 = if (idx + 1 < period) null else closes.subList(idx + 1 - period, idx + 1)
                .reduce { acc, v -> acc + v }
                .divide(BigDecimal(period), 4, RoundingMode.HALF_UP)
            val std = if (idx + 1 < period) null else {
                val slice = closes.subList(idx + 1 - period, idx + 1)
                val mean = ma20!!
                val variance = slice.map { (it - mean).pow(2) }
                    .reduce { acc, v -> acc + v }
                    .divide(BigDecimal(period), 6, RoundingMode.HALF_UP)
                variance.sqrt()
            }
            p.copy(
                ma51 = ma(51, idx),
                ma120 = ma(120, idx),
                ma250 = ma(250, idx),
                ma850 = ma(850, idx),
                bollMid = ma20,
                bollUpper = if (ma20 != null && std != null) ma20 + k * std else null,
                bollLower = if (ma20 != null && std != null) ma20 - k * std else null
            )
        }
    }

    private fun BigDecimal.sqrt(): BigDecimal {
        var g = BigDecimal(Math.sqrt(this.toDouble()))
        repeat(10) {
            g = (g + this.divide(g, 10, RoundingMode.HALF_UP)).divide(BigDecimal("2"), 10, RoundingMode.HALF_UP)
        }
        return g
    }
}
