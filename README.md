# sleepCare 🌙
**수면환경 적응형 스마트 알람 서비스**

수면 데이터와 웨어러블 기기 분석을 통해 최적의 기상 타이밍을 찾아주는 스마트 알람 애플리케이션입니다.

## 🚀 Quick Start

### Frontend
```bash
cd frontend
npm install
npm run dev
```
- 접속 주소: `http://localhost:5173`

### Backend 
```bash

```

## 🛠 Tech Stack

### Frontend
- **Framework**: React (Vite)
- **Styling**: Tailwind CSS
- **Icons**: Lucide React
- **Design System**: Antigravity Atomic Design
- **State Management**: React Hooks (Custom Hooks)

### Backend


## 📂 Project Structure
```
sleepCare/
├── frontend/           # React 클라이언트 소스코드
│   ├── src/
│   │   ├── components/ # 원자 단위 UI 컴포넌트 (Button, InputField 등)
│   │   ├── layouts/    # 페이지 레이아웃 (PageWrapper 등)
│   │   ├── pages/      # 화면 단위 컴포넌트 (Login, Home 등)
│   │   ├── hooks/      # 커스텀 훅 (useNotification 등)
│   │   └── types/      # TypeScript 타입 정의
├── spring-server/      # Spring Boot 백엔드 서버
└── README.md
```


## 📝 Git Commit Convention

Git 커밋 메시지를 작성할 때 다음 컨벤션을 따르는 것을 권장합니다.  
각 커밋 메시지는 **타입(Type)** 과 **설명(Description)** 으로 구성됩니다.  

```
<Type>: <설명>
```

### Type (타입)

|Type (타입)|설명|
|---|---|
|**Feat**|새로운 기능 추가|
|**Fix**|버그 수정 또는 오타(typo) 수정|
|**Refactor**|코드 리팩토링|
|**Design**|CSS 등 사용자 UI 디자인 변경|
|**Comment**|필요한 주석 추가 및 변경|
|**Style**|코드 포맷팅, 세미콜론 누락, 코드 변경이 없는 경우|
|**Test**|테스트 코드 추가, 수정, 삭제 (비즈니스 로직에 변경이 없는 경우)|
|**Chore**|위에 해당하지 않는 기타 변경사항 (빌드 스크립트 수정, assets image, 패키지 매니저 등)|
|**Init**|프로젝트 초기 생성|
|**Rename**|파일 또는 폴더명 수정 및 이동|
|**Remove**|파일 삭제 작업만 수행하는 경우|
