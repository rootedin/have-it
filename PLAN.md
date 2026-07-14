# Have It — 개발 계획서
> 습관 형성을 돕는 개인용 모바일 앱. 이 문서는 Claude Code에서 실제 개발을 진행할 때 참조하는 스펙 문서입니다.

---

## 0. 전제 및 가정

- **개발 규모**: 1인 또는 소규모 사이드 프로젝트
- **플랫폼**: **Android 전용** (iOS 없음)
- **기술 스택**: **네이티브 Android — Kotlin + Jetpack Compose**. 크로스플랫폼이 필요 없어졌으므로 Flutter의 핵심 장점(단일 코드베이스로 양 플랫폼 대응)이 사라짐. 홈 화면 위젯/잠금화면 체크(F12), 알림 권한 처리, Room 기반 통계 쿼리(히트맵 집계·스트릭 계산)에서 네이티브가 더 수월함
- **데이터**: **로컬 전용, 서버/클라우드 통신 없음** (확정). 클라우드 동기화는 고려 대상에서 제외하고, 대신 로컬 백업(내보내기/가져오기)을 Phase 2~3 후보로 둠
- **소셜 기능 없음**: 개인 습관 형성에 집중, 계정/서버 없이도 완전히 동작

---

## 1. 프로젝트 개요

| 항목 | 내용 |
|---|---|
| 앱 이름 | Have It |
| 한 줄 설명 | 작은 습관을 매일 쌓아가는 개인용 습관 형성 앱 |
| 핵심 가치 | 실패해도 관대하게, 작은 단위로 시작해서 꾸준히 쌓는 경험 |
| 타겟 사용자 | 의지만으로 습관 형성이 어려운 일반 사용자 (특정 도메인 한정 아님) |

### 네이밍 컨셉

"Habit"이 "Have it"으로 완성된다는 언어유희. 앱을 열 때마다 사용자에게 "습관을 가졌다/해냈다"는 자기암시를 준다.

---

## 2. 브랜드 & UX 원칙

- **톤**: 실패에 관대하고 다정함. 스트릭이 끊겨도 처벌보다 "다시 시작"을 자연스럽게 표현
- **시작 단위**: 온보딩에서 습관을 의도적으로 아주 작게 쪼개도록 유도 (실패 확률을 낮추는 것이 핵심 설계 원칙)
- **컬러 방향**: 보라 계열 메인 컬러 (`#534AB7` 등 purple 600 스톱), 텍스트는 밝은 톤(`#EEEDFE`)으로 대비
- **다크모드**: 필수 지원

---

## 3. 로딩 화면 상세 스펙

브랜드 첫인상이므로 정확한 타이밍을 명세합니다.

| 시점 | 동작 |
|---|---|
| 0ms | 화면 중앙에 "Habit" 텍스트 표시 (font-weight 500, 배경색 위 밝은 텍스트) |
| 0~1000ms | 정적 유지 |
| 1000ms | 텍스트가 "Have It!"으로 전환되며 scale 1 → 1.06 (350ms ease) 후 다시 1로 복귀 |
| 1000ms (동시) | 서브 카피 페이드인 ("매일 조금씩, 쌓여갑니다", 400ms) |
| 1000ms (동시) | 체크마크 아이콘 페이드인 (400ms) |
| ~1800ms | 애니메이션 종료, 홈 화면으로 전환 |

구현은 Jetpack Compose의 `Animatable` + `EaseOut` 계열 Easing 조합으로 진행 (Flutter의 `AnimationController` + `Curves.easeOut`에 대응). 실제 프로토타입은 이전 대화에서 인터랙티브 위젯으로 확인함.

---

## 4. 데이터 모델

리뷰 과정에서 발견된 두 가지를 수정함: (1) `Routine`의 `habitIds`/`order` 중복 필드 제거, (2) `UserSettings`에 프리즈 카드 월별 리셋 추적 필드 추가. 또한 `reminderTime`은 습관 삭제 시 orphan 데이터가 남지 않도록 `UserSettings`의 별도 Map이 아니라 `Habit`에 직접 귀속시킴.

```
Habit
- id: string (uuid)
- name: string
- icon: string (아이콘 식별자)
- color: string (hex)
- frequency: enum (daily, weekly, custom_days)
- customDays: List<int>?  // frequency가 custom_days일 때 요일(0-6)
- triggerSentence: string?  // if-then 형식, 예: "커피 내린 후 → 스쿼트 10개"
- reminderHour: int?     // 리마인더 시(선택). Habit에 직접 귀속 → 습관 삭제 시 자동 정리
- reminderMinute: int?
- createdAt: DateTime
- archivedAt: DateTime?

CheckIn
- id: string (uuid)
- habitId: string (FK -> Habit)
- epochDay: long  // 날짜만 표현 (LocalDate.toEpochDay 기준, 타임존 이슈 회피)
- completed: bool
- usedFreezeCard: bool  // 프리즈 카드로 스트릭을 보호한 날인지 (히트맵/통계에 "방패" 아이콘으로 구분 표시)
- note: string?  // 짧은 회고, 이모지 or 한줄 메모

Routine
- id: string (uuid)
- name: string  // 예: "아침 루틴"
- timeOfDay: enum (morning, afternoon, evening)
- orderedHabitIds: List<string>  // 실행 순서 = 소속 습관 목록 (별도 habitIds 필드 없음)

UserSettings  // 단일 레코드, DataStore(Preferences)로 관리 — Room 테이블 아님
- notificationsEnabled: bool
- freezeCardsAvailable: int          // 이번 달 남은 프리즈 카드 개수
- freezeCardsPerMonth: int           // 월별 지급 개수, MVP는 1로 고정 (설정 화면에서 조정 가능하도록 값만 분리)
- freezeCardsResetYearMonth: string  // "2026-07" 형식, 마지막으로 리셋된 연월 — 이 값과 현재 연월이 다르면 월초에 리셋
- theme: enum (light, dark, system)
```

### 스트릭 계산 규칙 (F3 관련, 리뷰에서 확정)

- **예정된 날만 카운트**한다. `custom_days`/`weekly` 습관은 예정되지 않은 날을 스트릭 계산에서 완전히 제외하고, 예정된 날끼리 연속 완료했는지만 본다.
- 프리즈 카드를 사용한 날(`usedFreezeCard=true`)은 스트릭을 끊지 않지만, 히트맵/완료율 통계에서는 일반 완료와 **구분 표시**(방패 아이콘)하고 완료율 집계에서도 별도 항목으로 센다.

---

## 5. 화면 구성

1. **Splash / Loading** — Habit → Have It! 애니메이션 (섹션 3 참조), 브랜드 퍼플 고정 배경
2. **Add Habit** — 습관 추가 폼 (온보딩 겸용): 이름/이모지 아이콘/컬러/반복(매일·요일 지정·주 1회)/if-then 트리거
3. **Home** — 오늘의 체크리스트: 날짜 헤더, 진행률 링 카드(프리즈 보유 표시), 습관 카드(스트릭 뱃지, 탭 체크 토글), 빈 상태 유도 화면, FAB
4. **Habit Detail** — 스트릭/완료율 스탯, 최근 14일 도트 히스토리, 프리즈 카드 사용 배너, 보관/삭제
5. **Weekly Report** — 요일별 완료율 바 차트, 주 평균, 주간 습관 별도 집계
6. **Settings** — 테마(시스템/라이트/다크), 프리즈 카드 현황, 알림 토글
7. **Routine Builder** — (Phase 2) 습관을 아침/저녁 루틴으로 묶기 — 데이터 모델만 준비됨, 화면은 미구현

---

## 6. 기능 명세

### Phase 1 — MVP (4~6주)

**F1. 습관 추가**
- 이름, 아이콘, 컬러, 반복 주기(매일/특정 요일) 입력
- if-then 트리거 문장 입력 필드 (선택, 예시 placeholder 제공)
- 수용 기준: 습관 생성 후 즉시 홈 화면 체크리스트에 반영됨

**F2. 오늘의 체크리스트**
- 오늘 날짜 기준으로 완료해야 할 습관 목록 표시
- 탭 한 번으로 완료 처리 (CheckIn 레코드 생성)
- 수용 기준: 앱을 껐다 켜도 완료 상태 유지 (Room DB 반영)

**F3. 스트릭 카운트 + 프리즈 카드**
- 연속 완료일수 계산 로직 (계산 규칙은 섹션 4 참조 — 예정된 날만 카운트)
- 월 1회(MVP 기본값, 설정 가능하게 값 분리) "프리즈 카드" 사용 가능 — 하루 놓쳐도 스트릭 유지, 히트맵엔 방패 아이콘으로 구분 표시
- 수용 기준: 프리즈 카드 소진 후에는 정상적으로 스트릭이 리셋됨. 매월 `freezeCardsResetYearMonth` 기준으로 자동 리셋됨

**F4. 기본 알림**
- 습관별 리마인더 시간 설정 (`Habit.reminderHour/Minute`)
- OS 네이티브 로컬 알림 사용 (서버 불필요) — `AlarmManager` (정확한 시각) + `NotificationCompat`
- Android 13+ `POST_NOTIFICATIONS` 런타임 권한 요청 플로우 포함
- Android 12+ 정확한 알람 스케줄링 시 `SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM` 권한 고려, 기기 재부팅 후 알람 재등록(`RECEIVE_BOOT_COMPLETED`) 포함

**F5. 로딩 화면 애니메이션**
- 섹션 3 스펙대로 구현

### Phase 2 — 리텐션 강화

**F6. 히트맵 캘린더** — 월 단위 완료 여부를 색상 농도로 표시, 프리즈 사용일은 방패 아이콘으로 구분
**F7. 주간 리포트** — 완료율 추이 라인/바 차트, "완료"와 "프리즈로 보호됨"을 별도 계열로 집계
**F8. 루틴 빌더** — 여러 습관을 순서대로 묶어 체크
**F9. 짧은 회고** — 완료/실패 시 이모지 또는 한줄 메모 (선택 사항, 부담 없게)
**F13. 로컬 백업/내보내기** — 전체 데이터를 JSON으로 내보내기/가져오기 (파일 공유 시트 활용). 서버 없이 기기 교체·재설치 대비

### Phase 3 — 차별화

**F10. 스마트 리마인더** — ~~완료 시간대 패턴 학습~~ → **제외** (사용자 요청)
**F11. 상관관계 인사이트** — ~~습관 간 상관관계 표시~~ → **제외** (사용자 요청)
**F12. 홈 화면 위젯** — ✅ 구현됨. `Glance` API 기반 App Widget, 오늘의 습관 목록 + 위젯에서 직접 체크

---

## 7. 패키지 구조 (Kotlin / Android 기준)

```
app/src/main/java/com/haveit/app/
  HaveItApplication.kt
  MainActivity.kt
  data/
    local/
      AppDatabase.kt
      Converters.kt
      entity/
        HabitEntity.kt
        CheckInEntity.kt
        RoutineEntity.kt
      dao/
        HabitDao.kt
        CheckInDao.kt
        RoutineDao.kt
    settings/
      UserSettingsRepository.kt     // DataStore(Preferences) 기반
    repository/
      HabitRepository.kt
      CheckInRepository.kt
      RoutineRepository.kt
  domain/
    streak/
      StreakCalculator.kt           // 예정된 날만 카운트하는 순수 함수
  data/
    backup/
      BackupManager.kt              // JSON 내보내기/가져오기 (F13)
  notification/
    ReminderScheduler.kt
    ReminderReceiver.kt             // 알람 수신 → 알림 표시 + 다음 예약
    BootReceiver.kt                 // 재부팅 후 알람 재등록
    HaveItNotifications.kt          // 알림 채널
  widget/
    HabitWidget.kt                  // Glance AppWidget (F12)
    HabitWidgetReceiver.kt
  ui/
    theme/
      Color.kt
      Type.kt
      Theme.kt
    splash/
      SplashScreen.kt
    addhabit/                       # 온보딩 겸용 습관 추가 폼
      AddHabitScreen.kt
      AddHabitViewModel.kt
    home/
      HomeScreen.kt
      HomeViewModel.kt
    habitdetail/
      HabitDetailScreen.kt
      HabitDetailViewModel.kt
    weeklyreport/
      WeeklyReportScreen.kt
      WeeklyReportViewModel.kt
    routine/                        # 루틴 빌더 (F8)
      RoutineBuilderScreen.kt
      RoutineBuilderViewModel.kt
    archive/                        # 보관함
      ArchiveScreen.kt
      ArchiveViewModel.kt
    settings/
      SettingsScreen.kt
      SettingsViewModel.kt
    components/
      HabitVisuals.kt               # 이모지/컬러 팔레트, 아이콘 버블
    navigation/
      Destinations.kt
      HaveItNavGraph.kt
```

---

## 8. 개발 순서 체크리스트

Claude Code에서 순서대로 진행하기 위한 작업 단위입니다.

- [x] 프로젝트 초기화 (Android Studio/Gradle 프로젝트 생성, 패키지 설정)
- [x] 디자인 토큰 정의 (컬러, 폰트, 테마 — light/dark)
- [x] 데이터 모델 클래스 작성 (섹션 4, Room Entity 기준)
- [x] 로컬 DB 연동 (Room + DataStore)
- [x] 로딩 화면 애니메이션 구현 (섹션 3 스펙)
- [x] 습관 추가 화면 (F1) — 온보딩 겸용 폼 (이름/아이콘/컬러/반복/트리거), 저장 즉시 홈 반영 확인
- [x] 오늘의 체크리스트 화면 (F2) — 탭 체크 토글, 진행률 링, 재실행 후 상태 유지 확인
- [x] 스트릭 로직 + 프리즈 카드 (F3) — 홈/상세 스트릭 표시, 상세 화면 프리즈 사용 배너, 월별 리셋 로직
- [x] 로컬 알림 연동 (F4) — 추가/편집 폼 리마인더 시간, AlarmManager 스케줄러, Boot 재등록, POST_NOTIFICATIONS 권한 요청
- [x] 습관 편집 — 추가 폼 편집 모드 재사용, 상세 화면 메뉴에서 진입
- [x] 히트맵 캘린더 (F6) — 상세 화면 월 단위 히트맵, 프리즈 방패 구분, 이전/다음 달 이동
- [x] 주간 리포트 세분화 (F7) — 완료/프리즈 스택 바 + 범례, 프리즈로 지킨 날 집계
- [x] 루틴 빌더 (F8) — 루틴 CRUD, 습관 순서 지정, 홈 화면 루틴별 섹션 그룹핑
- [x] 짧은 회고 (F9) — 상세 화면 오늘의 한 줄 메모 + 지난 메모 표시
- [x] 로컬 백업 (F13) — 설정 화면 JSON 내보내기/가져오기 (SAF)
- [x] 홈 위젯 (F12) — Glance AppWidget, 오늘의 습관 목록 + 위젯에서 체크
- [x] 보관함 — 보관된 습관 조회·복원·영구삭제
- [x] 내부 테스트 빌드 (assembleDebug + 유닛테스트 9개 통과)
- [ ] 실기기 온디바이스 검증 — 신규 기능 (편집/알림/히트맵/루틴/백업/위젯) 화면 확인 대기
- [ ] Phase 3 잔여: F10 스마트 리마인더, F11 상관관계 인사이트 (사용자 요청으로 제외)

---

## 9. 비기능 요구사항

- **오프라인 우선**: 네트워크 없이 모든 핵심 기능 동작 (서버 통신 자체가 없음)
- **다크모드**: 시스템 설정 연동
- **접근성**: 최소 텍스트 크기, 색맹 고려한 히트맵 색상 대비
- **성능**: 습관 100개, 체크인 1년치 데이터에서도 버벅임 없이 로딩

---

## 10. 오픈 이슈 (추후 결정 필요)

- ~~클라우드 동기화/백업 필요 여부~~ → **미지원 확정**. 대신 로컬 JSON 내보내기/가져오기(F13, Phase 2)로 대체
- 앱 아이콘 및 전체 컬러 팔레트 최종 확정
- 알림 문구 톤앤매너 (다정한 리마인더 vs 중립적 알림)
- 패키지명 `com.haveit.app`은 임시값 — 실제 배포 전 확정 필요
