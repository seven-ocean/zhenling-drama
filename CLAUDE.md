# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ZhenLing Drama is an AI Short Drama Generation Platform. Users create dramas, define characters/scenes, and the system auto-generates storyboards with AI-generated images, videos, and TTS audio.

## Build Commands

### Backend (Java 17 + Spring Boot 3)

```bash
cd backend
mvn spring-boot:run                          # Dev server on port 8080
mvn spring-boot:run -Dspring-boot.run.profiles=dev  # With dev profile
mvn test                                      # Run tests
mvn test -Dtest=StoryboardServiceTest        # Run specific test
mvn package -DskipTests                       # Build JAR without tests
```

### Frontend (Vue 3 + Vite + TypeScript)

```bash
cd frontend
npm install                                  # Install dependencies
npm run dev                                  # Dev server on port 5173 (proxies /api to backend)
npm run build                                # Production build
npm run preview                              # Preview production build
```

### Database

```bash
mysql -u root -p < sql/zhenling_drama.sql    # Initial setup
```

## Architecture

### Backend Layer Structure

```
controller/  → REST endpoints (DramaController, StoryboardController, VideoController, etc.)
service/    → Business logic (StoryboardService, VideoService, ImageGenerationService, etc.)
mapper/     → MyBatis Plus data access
entity/     → JPA/MyBatis entities (Drama, Storyboard, Character, Scene, Video, etc.)
dto/        → Request/response objects
config/     → Spring configuration (CorsConfig, RedisConfig, OssProperties, etc.)
common/     → Shared utilities (R, BusinessException, GlobalExceptionHandler, IdUtils)
```

### AI Adapter Pattern

`AiServiceFactory` manages all adapters and handles routing. Each adapter implements `AiAdapter`:


| Adapter          | Capabilities                                                                  |
| ---------------- | ----------------------------------------------------------------------------- |
| `MiniMaxAdapter` | Video (Hailuo v2.3), TTS (speech-02-hd), Image (M2.5 multi-ref), Text, Vision |
| `OpenAiAdapter`  | DALL-E image, GPT-4o text                                                     |
| `DoubaoAdapter`  | Seedream image, volcengine video (with lip-sync audio reference support)      |

`AiServiceFactory` auto-routes by type (text/image/video/tts), and also handles model-based routing:

- Models starting with `doubao-seedream` or `vc-seedream` → DoubaoAdapter
- Models starting with `image-` → MiniMaxAdapter (MiniMax image models)
- `FIRST_LAST_FRAME` video mode with volcengine → generates with start/end frames + audio reference

Key interface methods:

```java
String generateImage(String prompt, String model);
String generateImageWithReferences(String prompt, List<String> referenceImageUrls, String model);
String generateVideo(String imageUrl, String prompt, String model);
String generateVideoMultiMode(String mode, String prompt, String imageUrl, String firstFrameUrl, ...);
String generateTTS(String text, String voiceId, String model);
String analyzeImage(String prompt, String imageUrl);
```

### Video Generation Flow

1. User inputs drama description → `StoryboardService.generate()` splits into storyboards
2. Parallel AI generation: SceneService (scene images), ImageGenerationService (character images), ImageReferenceService (composite preview), VideoService (MiniMax/Hailuo), TtsService
3. `EpisodeExportService.compose()` combines storyboards: FFmpeg concat + AudioSyncEngine (pad/extend) + SubtitleRenderer (ASS burn-in)
4. Upload to OSS + save final URL

**Video modes supported:**

- `TEXT_TO_VIDEO` — text to video
- `IMAGE_TO_VIDEO` — image to video
- `FIRST_LAST_FRAME` — video with start/end frames (volcengine supports audio reference for lip-sync)
- `SUBJECT_REFERENCE` — subject reference (routed to IMAGE_TO_VIDEO for volcengine)

### Frontend Structure

```
src/
  views/
    Workbench.vue       # Core editing UI (storyboard list + detail panel)
    DramaList.vue       # Drama management
    MediaStudio.vue     # Asset preview/playback
    AiConfig.vue        # AI provider configuration
    StorageSettings.vue # OSS configuration
    TaskTracker.vue     # Task monitoring
  stores/
    drama.ts            # Pinia store for drama/character/scene/storyboard state
  composables/
    useToast.ts         # Toast notification composable
  router/               # Vue Router configuration
  components/           # Shared UI components
```

## Key Configuration

### application.yml (backend/src/main/resources/)

- `server.port: 8080`
- `spring.datasource` — MySQL connection
- `oss.*` — Aliyun OSS storage config
- `ai.default-*-provider` — routing for text/image/video/tts

### Environment Variables

- `OSS_ACCESS_KEY_ID`, `OSS_ACCESS_KEY_SECRET` — OSS credentials
- `MYSQL_PASSWORD` — database password

### AI Config Types

Configs stored in `ai_configs` table with type field (text/image/video/tts):

- Each type can have multiple providers configured
- `AiServiceFactory.loadAllConfigs()` initializes adapters on startup
- `AiConfigService` manages CRUD via `/ai-configs` API

### Frontend Stores

- `useDramaStore` — manages currentDrama, characters, scenes, storyboards
- Provides add/update/remove methods for all entities

## API Base

Base path: `/api/v1/`

Core endpoints:

- `POST /storyboards/generate` — AI generate storyboards from description
- `POST /image-reference/generate` — generate composite preview images (multi-character support)
- `POST /videos/generate` — async video generation
- `POST /exports/compose` — combine storyboards into episode MP4
