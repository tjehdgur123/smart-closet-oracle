# Smart Closet with Oracle XE

Android(Java/XML) 앱, Java REST API, Oracle Database XE 21c로 구성된 스마트 클로젯 시연 프로젝트입니다.

## 데이터 흐름

`Android 에뮬레이터 -> http://10.0.2.2:8080 -> Java API 서버 -> Oracle XEPDB1`

- Oracle `CLOTHES`: 옷 종류, 색상, 계절, 즐겨찾기, 사진 BLOB
- Oracle `COORDINATION`: 코디 이름, 즐겨찾기
- Oracle `COORDINATION_ITEM`: 코디와 옷의 다대다 관계 및 표시 순서
- 사진도 Oracle에 저장하므로 서버 DB에 실제 데이터가 남습니다.
- 앱 안에는 DB 암호가 없고, API 서버만 JDBC로 Oracle에 접속합니다.
- API 서버는 시연용으로 PC의 127.0.0.1에만 바인딩합니다.

## 최초 1회: DB 준비

PC에 Oracle XE 21c의 `OracleServiceXE`, `OracleOraDB21Home1TNSListener` 서비스가 실행 중이어야 합니다. Oracle XE에서 `XEPDB1`은 기본 PDB 서비스입니다.

프로젝트 폴더에서 PowerShell을 열고 다음을 실행합니다. 두 명령 모두 비밀번호를 대화형으로 묻습니다. 관리자 `SYSTEM` 암호는 프로젝트 파일에 적지 않습니다.

```powershell
sqlplus -L system@localhost:1521/XEPDB1 @db/create_user.sql
sqlplus -L smart_closet@localhost:1521/XEPDB1 @db/schema.sql
```

`create_user.sql`은 `SMART_CLOSET` 전용 계정에 로그인, 테이블 생성, USERS 테이블스페이스 200MB 할당 권한을 줍니다. `schema.sql`은 앱 테이블 3개와 외래키를 만듭니다. **이미 성공한 스크립트는 다시 실행하지 않습니다.** 계정이나 테이블이 이미 있으면 Oracle 오류가 납니다.

DB Server 운영 상태도 확인할 수 있습니다.

```powershell
Get-Service OracleServiceXE, OracleOraDB21Home1TNSListener
lsnrctl status
sqlplus -L system@localhost:1521/XE @db/admin_checks.sql
```

마지막 스크립트는 PDB 열림 상태와 `SMART_CLOSET` 세션 수를 읽습니다. 서버가 켜져 있을 때 SQL 요청 직후 세션 상태를 확인하는 시연에 쓸 수 있습니다.

## 매번: API 서버 실행

Android 앱보다 먼저 별도 PowerShell 창에서 실행하고, 창을 켜 둡니다.

```powershell
powershell -ExecutionPolicy Bypass -File .\server\run.ps1
```

암호 입력 후 `Smart Closet Oracle API ready...`가 보이면 준비됐습니다. 다른 PowerShell 창에서 상태를 확인할 수 있습니다.

```powershell
Invoke-RestMethod http://127.0.0.1:8080/health
```

결과의 `database`가 `oracle`, `connected`가 `True`여야 합니다. 서버가 처음 켜질 때 Oracle 연결 및 계정 권한을 검사합니다. 서버 실행이 실패하면 Oracle 계정 암호, XEPDB1 서비스, SQL 테이블 생성 여부를 확인하세요.

## Android 실행

Android Studio에서 이 프로젝트를 열고 Gradle Sync 후 AVD에 Run 합니다. 앱은 에뮬레이터의 호스트 PC 별칭 `10.0.2.2`로 API에 접속합니다. **실물 휴대폰은 주소가 달라 그대로는 연결되지 않습니다.** 이 시연은 AVD 기준입니다.

이전 버전에서 Room에 저장한 옷과 코디는 앱을 처음 열 때 Oracle로 한 번 가져옵니다. 중간에 실패하면 다음 실행 때 재시도하며, 가져온 항목은 중복 생성되지 않습니다. 원본 Room 파일은 삭제하지 않습니다. 새 등록과 수정은 Oracle API로 처리합니다.

## 발표 시연 순서

1. `Invoke-RestMethod .../health`로 Oracle 접속 확인
2. 앱에서 의류 사진을 등록하고, 내 옷장에서 확인
3. 코디북에서 여러 옷을 선택하고 코디 이름을 저장
4. 즐겨찾기 변경 및 의류 삭제를 시연
5. SQL*Plus에서 아래 조회 스크립트 실행

```powershell
sqlplus -L smart_closet@localhost:1521/XEPDB1 @db/demo_queries.sql
```

스크립트는 의류 속성/사진 크기, 코디별 의류 수, 코디와 의류의 조인 결과, 즐겨찾기를 출력합니다. 데이터를 변경하지 않는 SELECT만 실행합니다. UI 조작 직후 SQL 결과가 바뀌는 것이 Oracle 연동 증거입니다.

## 구현 위치

- `app/src/main/java/com/example/smartcloset/data/OracleApi.java`: Android HTTP 클라이언트
- `server/src/main/java/com/example/smartcloset/server/ServerMain.java`: HTTP API 및 Oracle JDBC SQL
- `db/create_user.sql`, `db/schema.sql`: DB 사용자/테이블 생성
- `db/demo_queries.sql`: 발표용 운영 조회
- `db/admin_checks.sql`: PDB/세션 상태 조회

## 참고

- Room 엔티티/DAO 코드는 기존 데이터를 Oracle로 가져올 때만 사용합니다.
- 시연용 API는 인증과 TLS가 없고 루프백에만 바인딩하므로 외부 네트워크에 공개하지 않습니다.
- 이미지 1개당 최대 6MB입니다.
- 기존 Room 사진 파일이 사라졌거나 6MB를 초과하면 자동 이전이 완료되지 않습니다. 앱에 오류가 표시되며 다음 실행 때 다시 시도합니다. 이 경우 먼저 파일을 확인하세요.
- 날씨 API는 아직 구현되지 않았습니다.
