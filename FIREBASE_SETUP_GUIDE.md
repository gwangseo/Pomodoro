# Firebase 설정 가이드

## 1. Firebase 프로젝트 생성

1. [Firebase Console](https://console.firebase.google.com/)에 접속
2. "프로젝트 만들기" 클릭
3. 프로젝트 이름을 "Pomodoro Timer"로 설정
4. Google Analytics 활성화 (선택사항)
5. 프로젝트 생성 완료

## 2. Android 앱 등록

1. Firebase Console에서 "Android" 아이콘 클릭
2. Android 패키지 이름 입력: `com.app.pomodoro`
3. 앱 닉네임 입력: "Pomodoro Timer"
4. SHA-1 인증서 지문 추가 (선택사항)
5. `google-services.json` 파일 다운로드

## 3. google-services.json 파일 배치

다운로드한 `google-services.json` 파일을 다음 위치에 배치:
```
app/google-services.json
```

## 4. Firebase 서비스 활성화

### 4.1 Authentication 활성화
1. Firebase Console에서 "Authentication" 메뉴 클릭
2. "시작하기" 클릭
3. "로그인 방법" 탭에서 "Google" 활성화
4. 프로젝트 지원 이메일 설정

### 4.2 Firestore Database 활성화
1. Firebase Console에서 "Firestore Database" 메뉴 클릭
2. "데이터베이스 만들기" 클릭
3. 보안 규칙 설정:
   - 테스트 모드에서 시작 (30일)
   - 또는 프로덕션 모드에서 시작

## 5. 보안 규칙 설정

Firestore Database > 규칙 탭에서 다음 규칙 설정:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // 사용자 문서 접근 규칙
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // 세션 문서 접근 규칙
    match /sessions/{sessionId} {
      allow read, write: if request.auth != null && 
        request.auth.uid == resource.data.userId;
    }
  }
}
```

## 6. Web Client ID 설정

### 6.1 웹 앱 추가 (웹 버전 연동용)
1. Firebase Console에서 "웹" 아이콘 클릭
2. 앱 닉네임 입력: "Pomodoro Timer Web"
3. 웹 앱 등록

### 6.2 Web Client ID 가져오기
1. Firebase Console에서 프로젝트 설정 클릭
2. "일반" 탭에서 "웹 API 키" 복사
3. 또는 Google Cloud Console에서 OAuth 2.0 클라이언트 ID 확인

### 6.3 AuthRepository에서 Web Client ID 설정
`app/src/main/java/com/app/pomodoro/data/repository/AuthRepository.kt` 파일에서:

```kotlin
.requestIdToken("YOUR_WEB_CLIENT_ID") // 여기에 실제 Web Client ID 입력
```

## 7. 웹 버전 연동을 위한 API 구조

### 7.1 세션 데이터 구조
```json
{
  "id": "session_id",
  "userId": "user_uid",
  "startTime": 1640995200000,
  "endTime": 1640997000000,
  "duration": 1800,
  "sessionType": "WORK",
  "isCompleted": true,
  "createdAt": 1640995200000
}
```

### 7.2 사용자 데이터 구조
```json
{
  "email": "user@example.com",
  "name": "사용자명",
  "lastLogin": 1640995200000
}
```

## 8. 웹 버전 개발 시 참고사항

### 8.1 Firebase Web SDK 설정
```javascript
// 웹 버전에서 사용할 Firebase 설정
const firebaseConfig = {
  apiKey: "your-api-key",
  authDomain: "your-project.firebaseapp.com",
  projectId: "your-project-id",
  storageBucket: "your-project.appspot.com",
  messagingSenderId: "123456789",
  appId: "your-app-id"
};
```

### 8.2 동일한 데이터 구조 사용
- 세션 ID 형식 통일
- 사용자 ID (Firebase UID) 사용
- 동일한 필드명과 데이터 타입 사용

## 9. 테스트

1. 앱 실행 후 Google 로그인 테스트
2. 타이머 세션 완료 후 클라우드 저장 확인
3. Firebase Console에서 데이터 확인
4. 로그아웃 후 다시 로그인하여 데이터 동기화 확인

## 10. 문제 해결

### 10.1 Google 로그인 실패
- SHA-1 인증서 지문 확인
- OAuth 2.0 클라이언트 ID 확인
- Firebase 프로젝트 설정 확인

### 10.2 Firestore 접근 오류
- 보안 규칙 확인
- 인증 상태 확인
- 네트워크 연결 확인

### 10.3 데이터 동기화 문제
- 사용자 ID 일치 확인
- 데이터 구조 확인
- 오프라인 모드 설정 확인
