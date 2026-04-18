package com.invest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.util.UriComponentsBuilder;

@ConfigurationProperties(prefix = "invest")
public class InvestProperties {

    private Stock stock = new Stock();
    private YahooFinance yahooFinance = new YahooFinance();

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        if (stock != null) {
            this.stock = stock;
        }
    }

    public YahooFinance getYahooFinance() {
        return yahooFinance;
    }

    public void setYahooFinance(YahooFinance yahooFinance) {
        if (yahooFinance != null) {
            this.yahooFinance = yahooFinance;
        }
    }

    public static class Stock {

        private long refreshMs = 15_000L;

        public long getRefreshMs() {
            return refreshMs;
        }

        public void setRefreshMs(long refreshMs) {
            this.refreshMs = refreshMs;
        }
    }

    public static class YahooFinance {

        /** 공식 차트 API를 끄고(예: 모의서버만 쓸 때) 조회를 막습니다. */
        private boolean enabled = true;

        private String chartBaseUrl = "https://query1.finance.yahoo.com";
        private String chartPath = "/v8/finance/chart/{symbol}";
        private String queryRange = "1d";
        private String queryInterval = "1m";
        private String userAgent = "Mozilla/5.0 (compatible; InvestBE/1.0)";

        private int connectTimeoutMs = 5_000;
        private int readTimeoutMs = 20_000;

        /** RapidAPI 등 프록시에서 요구하는 키(없으면 미전송). */
        private String apiKey = "";

        /** apiKey 를 넣을 때 사용할 헤더 이름 (예: X-RapidAPI-Key). */
        private String apiKeyHeader = "X-RapidAPI-Key";

        /** 선택: 일부 프록시가 Referer 를 요구할 때. */
        private String referer = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getChartBaseUrl() {
            return chartBaseUrl;
        }

        public void setChartBaseUrl(String chartBaseUrl) {
            this.chartBaseUrl = chartBaseUrl;
        }

        public String getChartPath() {
            return chartPath;
        }

        public void setChartPath(String chartPath) {
            this.chartPath = chartPath;
        }

        public String getQueryRange() {
            return queryRange;
        }

        public void setQueryRange(String queryRange) {
            this.queryRange = queryRange;
        }

        public String getQueryInterval() {
            return queryInterval;
        }

        public void setQueryInterval(String queryInterval) {
            this.queryInterval = queryInterval;
        }

        public String getUserAgent() {
            return userAgent;
        }

        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey == null ? "" : apiKey;
        }

        public String getApiKeyHeader() {
            return apiKeyHeader;
        }

        public void setApiKeyHeader(String apiKeyHeader) {
            this.apiKeyHeader = apiKeyHeader == null ? "" : apiKeyHeader;
        }

        public String getReferer() {
            return referer;
        }

        public void setReferer(String referer) {
            this.referer = referer == null ? "" : referer;
        }

        public String buildChartUrl(String symbol) {
            String sym = symbol.trim().toUpperCase();
            String base = chartBaseUrl.endsWith("/") ? chartBaseUrl.substring(0, chartBaseUrl.length() - 1) : chartBaseUrl;
            String path = chartPath.startsWith("/") ? chartPath : "/" + chartPath;
            path = path.replace("{symbol}", sym);
            return UriComponentsBuilder.fromUriString(base + path)
                    .queryParam("range", queryRange)
                    .queryParam("interval", queryInterval)
                    .build(true)
                    .toUriString();
        }
    }
}
