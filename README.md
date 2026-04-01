# 기억지기
> 멀티모달 기억추론 기반 치매 환자 회상치료 서비스

치매 환자의 개인 데이터와 대화를 기반으로  
기억을 영상 형태로 재구성하여 회상을 돕는 서비스입니다.
<img width="1388" height="650" alt="image" src="https://github.com/user-attachments/assets/9ccbd60c-fa38-4e2a-a360-0a42fd8c5e55" />

---

## 프로젝트 개요

기존 회상치료는 단순 시각 자료 중심으로 진행되어 몰입도가 낮고,  
개인 맞춤형 치료가 어렵다는 한계가 있습니다.

이를 해결하기 위해  
**멀티모달 데이터와 생성형 AI를 결합하여**  
환자의 기억을 영상 형태로 재구성하는 서비스를 개발했습니다.

---

## 문제 정의

- 개인 맞춤형 회상치료 제공의 어려움  
- 단순 시각 자료 기반 치료의 낮은 몰입도  
- 치매 환자의 발화 데이터가 불완전하고 비구조적  

---

## 해결 방법

### 1. 멀티모달 데이터 기반 기억 구조화
- 환자의 **사진, 이력, 발화 데이터**를 결합
- LLM을 활용해 부족한 기억 정보 보완

### 2. 대화 기반 기억 추출
- LLM API를 활용한 음성 대화 처리
- 의미 있는 정보를 끌어내기 위한 질문 흐름 구성

### 3. 멀티 씬 생성 알고리즘
- 환자의 뒤섞인 발화를 분석
- **6하원칙 기반 Scene 단위로 재구성**

### 4. 영상 생성 기반 회상 콘텐츠
- 스토리 생성 → 장면 구성 → 영상 생성
- 영화적 연출 기법 적용으로 몰입감 향상
<img width="1423" height="730" alt="image" src="https://github.com/user-attachments/assets/a4d15d36-3aeb-4322-982a-57b60ebc60c5" />

- 실행영상 보러가기
-> https://www.youtube.com/watch?v=TDZO8gM4qU8
---

## 🏗️ 시스템 아키텍처
<img width="1379" height="715" alt="image" src="https://github.com/user-attachments/assets/f4726956-7204-4bd9-ab92-3c5c51f51ef6" />


---

## ⚙️ 기술 스택

### Backend
- Java, Spring Boot
- Spring Security, JWT
- Spring Data JPA
- WebClient

### AI Server
- Python, FastAPI
- LLM API (open api, gemini, nanobanana, veo3)

### Infra
- AWS (EC2, S3, RDS)
- Redis
- Docker

---

## 역할 및 기여도

- **백엔드 메인 개발 (기여도 약 70%)**
- **AI 서버 일부 구현 및 연동 (기여도 약 10%)**

### 주요 담당 영역

#### Backend
- Spring Boot 기반 REST API 설계 및 구현
- 공통 응답 구조 및 예외 처리 표준화
- Swagger 기반 API 문서 자동화
- 카카오 로그인 및 JWT 인증/인가 구현

#### Server Architecture
- Spring Boot - FastAPI 서버 분리 구조 설계
- WebClient 기반 서버 간 통신 구현

#### AI Integration
- FastAPI 서버 일부 구현
- GPT API 연동 (음성 대화 기반 회상 기능)
- AI 서버와의 데이터 흐름 설계

---

## 주요 기술적 포인트

### 1. 서버 분리 아키텍처
- Spring: 사용자 요청 및 비즈니스 로직 처리
- FastAPI: AI 처리 전담

 AI 처리 지연이 서비스 전체에 영향을 주지 않도록 분리

---

### 2. 멀티 씬 생성 로직
- 환자의 발화를 분석하여 Scene 단위로 구조화
- Who / When / Where / What / Why / How 기반 처리

 비정형 데이터를 구조화하여 영상 생성에 활용

---

### 3. 멀티모달 기반 기억 보완
- 개인 데이터 + 대화 데이터 + 공공 데이터 결합
- LLM을 활용해 기억 공백 보완

---

## 성과

- 2025 SW중심대학 우수작품 경진대회 **후원기업상 수상**
- 약 1년간 프로젝트 고도화 및 서비스 완성도 개선

---

## 기대 효과

- 개인 맞춤형 영상 기반 회상치료 제공
- 환자의 몰입도 및 감정 반응 향상
- 보호자 및 의료진의 치료 효율 개선

---
