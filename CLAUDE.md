# invest-be — Backend CLAUDE.md

## 프로젝트 개요
주식 투자 시뮬레이션 백엔드 API. Spring Boot 3 + JPA + H2(로컬) / PostgreSQL(프로덕션).

## 기술 스택
- **Java 17** + **Spring Boot 3.3.5** + **Gradle**
- **Spring Data JPA** + **H2**(로컬) / **PostgreSQL**(프로덕션)
- **Spring Actuator** — 헬스체크 (`/actuator/health`)
- 빌드: `./gradlew bootRun` / 테스트: `./gradlew test`
- 프론트엔드: `../invest-fe` (포트 5173), 백엔드: 포트 8080

## 환경변수
| 변수 | 설명 |
|------|------|
| `GOOGLE_CLIENT_ID` | Google OAuth 2.0 클라이언트 ID |
| `YAHOO_API_KEY` | RapidAPI 키 (Yahoo Finance 프록시 사용 시) |
| `SPRING_DATASOURCE_URL` | 프로덕션 DB URL (Supabase PostgreSQL) |
| `SPRING_DATASOURCE_USERNAME` | DB 사용자명 |
| `SPRING_DATASOURCE_PASSWORD` | DB 비밀번호 |

## 소스 구조
```
src/main/java/com/invest/
├── InvestApplication.java
├── config/
│   ├── AppConfig.java           # RestClient 빈 등록
│   └── InvestProperties.java   # invest.* 설정 바인딩
├── domain/
│   ├── User.java
│   ├── Holding.java             # 복합 PK (userId + symbol)
│   ├── LedgerEntry.java
│   ├── DividendRecord.java      # (userId, symbol, exDate) 유니크 제약
│   └── TransactionType.java    # BUY | SELL | ADD_MONEY | SUBTRACT_MONEY | DIVIDEND_REINVEST
├── repo/
│   ├── UserRepository.java
│   ├── HoldingRepository.java
│   ├── LedgerEntryRepository.java
│   └── DividendRecordRepository.java
├── service/
│   ├── PortfolioService.java
│   ├── TradingService.java
│   ├── UserService.java
│   ├── WalletService.java
│   ├── WhatIfService.java
│   ├── DividendService.java     # @Scheduled 1시간마다 배당 재투자
│   ├── LedgerService.java
│   ├── GoogleTokenVerifier.java # tokeninfo 엔드포인트 검증
│   └── stock/
│       ├── StockPriceRegistry.java  # 인메모리 캐시 + @Scheduled 15초 갱신
│       ├── YahooChartClient.java    # 시세/히스토리/배당 조회
│       └── YahooSearchClient.java  # 종목 검색
└── web/
    ├── AuthController.java
    ├── StockController.java
    ├── OrderController.java
    ├── WalletController.java
    ├── PortfolioController.java
    ├── LedgerController.java
    ├── GlobalExceptionHandler.java
    ├── CorsConfig.java
    └── dto/                     # 모든 Request/Response DTO
```

## REST API 엔드포인트
| 메서드 | 경로 | 설명 |
|--------|------|------|
| `POST` | `/api/auth/google` | Google ID 토큰 검증 → 로그인/회원가입 |
| `GET` | `/api/stocks/{symbol}/quote` | 실시간 시세 조회 |
| `GET` | `/api/stocks/search?q=` | 종목 검색 (한글 가능) |
| `GET` | `/api/stocks/{symbol}/history?range=` | 차트 히스토리 |
| `POST` | `/api/orders/buy` | 주식 매수 |
| `POST` | `/api/orders/sell` | 주식 매도 |
| `POST` | `/api/wallet/deposit` | 현금 입금 |
| `POST` | `/api/wallet/withdraw` | 현금 출금 |
| `GET` | `/api/portfolio?userId=` | 포트폴리오 요약 |
| `GET` | `/api/portfolio/holdings?userId=` | 보유 종목 목록 |
| `GET` | `/api/portfolio/whatif?userId=&symbol=` | 만약에 시뮬레이션 |
| `GET` | `/api/ledger?userId=&types=` | 거래 원장 |
| `GET` | `/actuator/health` | 헬스체크 (CORS 허용) |

## 도메인 엔티티

### User
```
id VARCHAR(128) PK   — Google OAuth subject
balance DECIMAL(19,4)
email VARCHAR(256)
name VARCHAR(128)
picture_url VARCHAR(512)
```

### Holding (복합 PK)
```
user_id VARCHAR(64) PK
symbol VARCHAR(16) PK
quantity DECIMAL(19,8)
average_cost DECIMAL(19,8)  — 주당 평균 매입가 (KRW)
```

### LedgerEntry
```
id BIGINT PK AUTO
user_id VARCHAR(64)
type TransactionType
symbol VARCHAR(16)     — nullable (입출금 시 null)
quantity DECIMAL(19,8) — nullable
unit_price DECIMAL(19,8)
cash_delta DECIMAL(19,4) — 양수: 입금/매도, 음수: 출금/매수
created_at TIMESTAMP
```

### DividendRecord
```
id BIGINT PK AUTO
user_id VARCHAR(64)
symbol VARCHAR(16)
ex_date DATE
dividend_per_share DECIMAL(19,8)  — USD
shares_held DECIMAL(19,8)
reinvested_shares DECIMAL(19,8)
price_krw DECIMAL(19,4)
processed_at TIMESTAMP
UNIQUE (user_id, symbol, ex_date)  — 중복 처리 방지
```

## 핵심 설계

### 환율 처리 중앙화
- `StockPriceRegistry.resolveKrwRate()` — 단일 소스. `KRW=X` 캐시 우선, 없으면 1500 폴백
- 모든 서비스(TradingService, PortfolioService, WhatIfService)가 이 메서드 사용
- `TradingService.buy/sell`의 `exchangeRate` 파라미터 제거 (항상 서버 캐시 환율 사용)

### 통화 처리
- Yahoo Finance `meta.currency` 필드를 `PriceData.currency`로 수신, `StockPriceRegistry.lastCurrency`에 캐시
- **`StockPriceRegistry.toKrw(price, symbol, krwRate)`**: 통화 변환 중앙 메서드
  - `KRW` 종목 → price 그대로 반환 (환율 미적용)
  - 그 외(USD 등) → `price * krwRate`
- `TradingService`와 `PortfolioService` 모두 `toKrw()` 사용 → 항상 같은 환율 소스, 같은 로직
- 클라이언트가 `OrderRequest.exchangeRate`를 보내더라도 서버에서 무시 (환율 불일치로 인한 가짜 손익 방지)
- **`StockPriceRegistry.getCurrency(symbol)`**: 캐시된 통화 코드 반환 (기본값 "USD")

### 주가 캐싱 (StockPriceRegistry)
- `ConcurrentHashMap` 인메모리 캐시
- `@Scheduled(fixedDelayString)` 15초 갱신
- `watch(symbol)` — `^[A-Z0-9.=^_\\-+]{1,20}$` 정규식 검증 (한글 등 비ASCII 차단)
- `lastPrice`(regularMarketPrice), `lastPrePrice`, `lastPostPrice`, `lastMarketState`, `lastCurrency` 각각 캐시
- **`getEffectivePrice(symbol)`**: marketState 기반 실효 가격 반환
  - `PRE`/`PREPRE` → preMarketPrice (없으면 regularMarketPrice)
  - `POST`/`POSTPOST`/`CLOSED` → postMarketPrice (없으면 regularMarketPrice)
  - `REGULAR` → regularMarketPrice
- **`getEffectiveKrwPrice(symbol, krwRate)`**: `getEffectivePrice` + `toKrw` 조합
- `PortfolioService`, `TradingService` 모두 `getEffectiveKrwPrice` 사용 → 프리/애프터마켓 반영

### 배당 재투자 (DividendService)
- `@Scheduled(fixedDelay=3_600_000)` 1시간마다 실행
- Yahoo Finance에서 최근 2년 배당 이력 조회
- `DividendRecord` 유니크 제약으로 중복 처리 방지 (idempotency)
- 현금 변동 없음 (`cashDelta = 0`), 보유 주수만 증가

### What-If 계산 (WhatIfService)
- `ADD_MONEY` 원장 내역으로 각 입금일 시세 조회
- `"2y_daily"` 키로 일봉 히스토리 사용 (weekly보다 정확)
- `TreeMap.floorEntry()` → 해당일 또는 이전 가장 가까운 거래일 가격 사용
- 제한: 과거 환율 미반영, 현재 환율로 통일 계산

### Google 인증
- `POST /api/auth/google` body: `{idToken: string}`
- `GoogleTokenVerifier` → `https://oauth2.googleapis.com/tokeninfo?id_token=` 호출
- `aud` 클레임 = `invest.google.client-id` 검증
- 신규 사용자 자동 생성, 기존 사용자 정보 업데이트(이메일/이름/사진)

### Yahoo Finance 히스토리 range 키
| UI 키 | Yahoo range | interval |
|-------|-------------|----------|
| `1d` | `1d` | `5m` |
| `1w` | `5d` | `60m` |
| `1mo` | `1mo` | `1d` |
| `3mo` | `3mo` | `1d` |
| `6mo` | `6mo` | `1d` |
| `1y` | `1y` | `1d` |
| `2y` | `2y` | `1wk` |
| `2y_daily` | `2y` | `1d` (WhatIfService 전용) |

### CORS
- `/api/**` — GET, POST, PUT, PATCH, DELETE
- `/actuator/**` — GET, OPTIONS (헬스체크용 별도 매핑 필수)

## 주요 설정 (application.yml)
```yaml
invest:
  stock:
    refresh-ms: 15000
  yahoo-finance:
    enabled: true
    chart-base-url: https://query1.finance.yahoo.com
    chart-path: /v8/finance/chart/{symbol}
    user-agent: "Mozilla/5.0 ..."
    api-key: ${YAHOO_API_KEY:}
    api-key-header: X-RapidAPI-Key
  google:
    client-id: ${GOOGLE_CLIENT_ID:}
```

## Supabase 프로덕션 주의사항
- `allow_jdbc_metadata_access: false` — 프로덕션 전용. 로컬에 추가하면 DDL 실패
- `spring.jpa.hibernate.ddl-auto: update` — 로컬
- 프로덕션 스키마 변경은 SQL 마이그레이션으로 직접 실행

## 로컬 개발
```bash
./gradlew bootRun
# H2 콘솔: http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:invest
```
