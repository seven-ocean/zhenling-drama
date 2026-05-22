<div align="center">

# 🔮 臻灵短剧 · ZhenLing Drama

**AI Short Drama Generation Platform · AI Short Drama Creation & Generation**

[Features](#feature-modules) · [Architecture](#technical-architecture) · [Quick Start](#quick-start) · [Config](#configuration-reference) · [API](#api-documentation)

[![License](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-green.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.x-brightgreen.svg)](https://vuejs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-blue.svg)](https://www.typescriptlang.org/)

</div>

<h2 align="center">Short Drama Composition Showcase</h2>

<video controls preload="none" width="100%" style="max-width: 800px; display: block; margin: 0 auto;">
  <source src="https://raw.githubusercontent.com/seven-ocean/zhenling-drama/commercial/assets/%E7%9F%AD%E5%89%A7%E5%90%88%E6%88%90%E6%95%88%E6%9E%9CV1.mp4" type="video/mp4">
</video>

<p align="center"><a href="https://raw.githubusercontent.com/seven-ocean/zhenling-drama/commercial/assets/%E7%9F%AD%E5%89%A7%E5%90%88%E6%88%90%E6%95%88%E6%9E%9CV1.mp4">▶ Cannot play? Click to download</a></p>

---

## 📞 Contact Us

If you are interested in this project or have custom development needs, feel free to scan the QR code below to reach out to us:


|              Yuyoung Tech Official              |           Open Source Tech Exchange Group           |
| :----------------------------------------------: | :--------------------------------------------------: |
| ![Official QR](./assets/昱扬科技【二维码】.png) | ![Group QR](./assets/昱扬科技【开源技术交流群】.png) |
| [https://www.yuyoung.cn](https://www.yuyoung.cn) |                                                      |

---

## Feature Modules

### 1. Drama Management

> **Entry:** `/dramas` → Drama List Page


| Feature        | Description                                                 |
| -------------- | ----------------------------------------------------------- |
| Create Drama   | Enter title, description, cover to init structure           |
| Episode Manage | Multi-episode support, each episode has its own storyboards |
| Delete/Restore | Soft delete, data recoverable                               |

**Data Model:** `drama_id`, `title`, `description`, `cover_image`, `status`(draft/in_progress/completed), `total_episodes`, `created_episodes`

![02-短剧列表页.png](./assets/02-短剧列表页.png)

![03-创建短剧弹窗.png](./assets/03-创建短剧弹窗.png)

---

### 2. Workbench

> **Entry:** `/workbench/:dramaId` → Drama Creative Workbench

The workbench is the core creative interface, with storyboard list on the left and detail editor on the right.

**Storyboard Data Model:**

- `scene_id` / `character_id(s)` → bound scene and multi-character images (multiple characters in one shot)
- `shot_type` → wide / medium / close-up / extreme-close-up
- `shot_direction` → tracking / fixed / aerial-dive ...
- `dialogue` → script (used for TTS and subtitles)
- `action` → action description (AI prompt material)
- `status` → pending / generating / completed / failed

**Workflow:**

```
User inputs script description
       ↓
AI StoryboardService.generate()
       ↓
Auto-split into shots (shot-level granularity)
       ↓
① Scene image AI generation (SceneService)
② Character image AI generation (ImageGenerationService)
③ Reference image generation (ImageReferenceService) — Character × Scene preview
④ Video AI generation (VideoService → MiniMax/Hailuo)
⑤ Voice AI generation (TtsService)
       ↓
Each storyboard: pending → generating → completed/failed
       ↓
Episode composition: EpisodeExportService.compose()
       ↓
FFmpeg VideoComposeService + SubtitleRenderer + AudioSyncEngine
       ↓
Output complete MP4, upload to OSS, record in DB
```

**FEAT-003 Reference Image Generation:**

Before video synthesis, generate a "Character × Scene" preview image for each storyboard. Supports multiple character images passed to MiniMax multi-image reference synthesis, allowing you to verify the effect before triggering expensive video generation.

![04-工作台全貌.png](./assets/04-工作台全貌.png)

![05-分镜编辑面板.png](./assets/05-分镜编辑面板.png)

![06-AI生成状态展示.png](./assets/06-AI生成状态展示.png)

---

### 3. Media Studio

> **Entry:** `/media/:dramaId` → Asset Preview and Editing

- Storyboard video preview (online playback)
- Storyboard audio preview
- Video clip trimming preview
- Asset library management (scene/character/video/audio categorization)
- **Reference Image Auto-fill**: Auto-fills generated reference image when jumping from Workbench

![07-媒体工作室1.png](./assets/07-媒体工作室1.png)

![07-媒体工作室2.png](./assets/07-媒体工作室2.png)

![07-媒体工作室3.png](./assets/07-媒体工作室3.png)

---

### 4. AI Config Center

> **Entry:** `/settings/ai`


| Config Item      | Description                              |
| ---------------- | ---------------------------------------- |
| OpenAI API Key   | GPT-4o image generation / text           |
| MiniMax API Key  | Hailuo video / TTS                       |
| Default Provider | Set default AI vendor                    |
| Routing Strategy | Route by function (text/image/video/tts) |

**Config Interface:** Form-based management, API keys shown masked, encrypted on save.

![08-AI配置页.png](./assets/08-AI配置页.png)

![08-APIKey管理截图.png](./assets/08-APIKey管理截图.png)

---

### 5. Storage Settings

> **Entry:** `/settings/storage`


| Feature        | Description                                 |
| -------------- | ------------------------------------------- |
| OSS Toggle     | Enable/disable Aliyun OSS                   |
| Bucket Config  | endpoint / bucket / custom domain           |
| AccessKey Mgmt | AK/SK masked management                     |
| Storage Status | View used/total capacity (requires OSS SDK) |

![09-存储配置1.png](./assets/09-存储配置1.png)

![09-存储配置2.png](./assets/09-存储配置2.png)

---

### 6. Task Tracker

> **Entry:** `/settings/tasks`

- Real-time task list (generating / success / failed)
- Progress percentage display
- Failure retry mechanism
- Paginated historical task records

![10-任务追踪.png](./assets/10-任务追踪.png)

---

## Technical Architecture

### System Architecture Diagram

![01-系统架构图.png](./assets/01-系统架构图.png)

### Core Data Flow

#### AI Video Generation Flow

```
Browser                              Backend                               AI Provider
   │                                     │                                      │
   │ POST /api/v1/videos/generate        │                                      │
   │ { storyboard_id, provider }         │                                      │
   │───────────────────────────────────► │                                      │
   │                                     │ Validate AI Config (ApiKey)
   │                                     │  ───────────────────────────────────►│
   │                                     │           API Call
   │                                     │  ◄───────────────────────────────────│
   │                                     │   task_id (async)
   │                                     │
   │  200 OK { task_id }                 │
   │◄────────────────────────────────────│
   │                                     │ (backend polling)
   │                                     │  GET /query/{task_id}
   │                                     │  ───────────────────────────────────►│
   │                                     │  ◄───────────────────────────────────│
   │                                     │    video_url / failed
   │                                     │
   │                                     │   FFmpeg compose (storyboard-level)
   │                                     │   Upload to OSS / save locally
   │                                     │   Update DB (video.url)
```

#### Episode Export Flow

```
Storyboard list (N storyboards)
       │
       ├─ Storyboard 1: video_url + audio_url + dialogue → FFmpeg → storyboard1.mp4
       ├─ Storyboard 2: video_url + audio_url + dialogue → FFmpeg → storyboard2.mp4
       │    ...
       └─ Storyboard N: ... → FFmpeg → storyboardN.mp4
              │
              ▼
    AudioSyncEngine (smart alignment: audio duration > video duration → PadSilence / ExtendFreeze)
              │
              ▼
    SubtitleRenderer (burn ASS subtitles into video)
              │
              ▼
    FFmpeg Concat (concatenate all clips in order)
              │
              ▼
    OSS Upload + EpisodeExport Record
              │
              ▼
         Final MP4 URL
```

### AI Adapter Architecture

```java
public interface AiAdapter {
    // Image generation (supports multi-image reference)
    String generateImage(String prompt, String model);
    String generateImageWithReferences(String prompt, List<String> referenceImageUrls, String model);
    // Video generation (returns task_id)
    String generateVideo(String imageUrl, String prompt, String model, Integer duration, String resolution);
    // TTS
    String generateTTS(String text, String voiceId, String model);
    // Video task status polling
    String pollVideoStatus(String taskId);
}
```

Current implementations:

- `MiniMaxAdapter` — Hailuo video v2.3 / TTS speech-02-hd / M2.5 text / **multi-image reference generation**
- `OpenAiAdapter` — DALL-E image / GPT-4o text

### Database Schema


| Table Name        | Description                         |
| ----------------- | ----------------------------------- |
| `dramas`          | Drama main table                    |
| `characters`      | Character table (appearance/voice)  |
| `scenes`          | Scene table (scene image/prompts)   |
| `storyboards`     | Storyboard table (core, multi-char) |
| `videos`          | Video clip table                    |
| `audios`          | Audio/TTS table                     |
| `assets`          | Asset file table (OSS URL)          |
| `task_logs`       | Async task log                      |
| `episode_exports` | Episode export record               |
| `ai_configs`      | AI config (encrypted storage)       |

### Project Structure

```
zhenling-drama/
├── backend/                         # Java 17 + SpringBoot 3 Backend
│   ├── src/main/java/com/drama/
│   │   ├── controller/                       # REST API
│   │   │   └── ImageReferenceController.java # Reference image API (FEAT-003)
│   │   ├── service/                         # Business logic
│   │   │   ├── ImageReferenceService.java   # Reference image (multi-character)
│   │   │   ├── ImageGenerationService.java  # AI image gen (multi-image ref)
│   │   │   └── adapter/
│   │   │       ├── AiAdapter.java           # Unified interface
│   │   │       └── MiniMaxAdapter.java     # MiniMax multi-image impl
│   │   ├── entity/
│   │   │   └── Storyboard.java              # Has characterIds field
│   │   └── dto/
│   │       └── StoryboardRefStatus.java    # Batch ref image status DTO
│   └── sql/
│       └── zhenling_drama.sql
│
├── frontend/                        # Vue3 + Vite + TypeScript Frontend
│   └── src/views/
│       ├── Workbench.vue            # ⭐ Storyboard editor (ref image panel)
│       └── MediaStudio.vue          # Asset preview (ref image auto-fill)
│
└── assets/                          # README image assets
```

---

## Quick Start

### Environment Requirements


| Dependency | Version | Description                       |
| ---------- | ------- | --------------------------------- |
| JDK        | 17+     | Lombok / SpringBoot 3 requirement |
| Maven      | 3.9+    |                                   |
| Node.js    | 18+     | Frontend build                    |
| MySQL      | 8.0+    |                                   |
| Redis      | 6+      | Cache / rate limiting             |
| FFmpeg     | Any     | `which ffmpeg` to verify          |

### Backend Startup

```bash
cd backend

# Database init (fresh)
mysql -u root -p < ../sql/zhenling_drama.sql

# Add multi-character column to existing DB
mysql -u zhenling_drama -pzhenling_drama zhenling_drama -e \
  "ALTER TABLE storyboards ADD COLUMN character_ids TEXT AFTER scene_id;"

# Configure environment variables
export OSS_ACCESS_KEY_ID=your_key
export OSS_ACCESS_KEY_SECRET=your_secret

# Dev startup
mvn spring-boot:run
# Or specify profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

> Backend: `http://localhost:8080`

### Frontend Startup

```bash
cd frontend
npm install
npm run dev
```

> Frontend: `http://localhost:5173` (Vite proxies `/api` to backend)

---

## Configuration Reference

**`application.yml` Key Config Items:**

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/zhenling_drama?useSSL=false&serverTimezone=Asia/Shanghai
    username: zhenling_drama
    password: ${MYSQL_PASSWORD:zhenling_drama}

oss:
  enabled: true
  provider: aliyun
  endpoint: http://oss-cn-chengdu.aliyuncs.com
  access-key-id: ${OSS_ACCESS_KEY_ID}
  access-key-secret: ${OSS_ACCESS_KEY_SECRET}
  bucket-name: your-bucket
  base-path: drama-assets

cors:
  allowed-origins:
    - http://localhost:5173

ai:
  default-text-provider: minimax
  default-image-provider: minimax   # MiniMax-Image-01 (supports multi-image ref)
  default-video-provider: minimax
  default-tts-provider: minimax

ffmpeg:
  path: ""
```

---

## API Documentation

Base path: `/api/v1/`


| Method | Path                              | Description                      |
| ------ | --------------------------------- | -------------------------------- |
| GET    | `/dramas`                         | Get drama list                   |
| POST   | `/dramas`                         | Create drama                     |
| GET    | `/dramas/{id}`                    | Get drama detail                 |
| PUT    | `/dramas/{id}`                    | Update drama                     |
| DELETE | `/dramas/{id}`                    | Delete drama                     |
| GET    | `/storyboards/drama/{dramaId}`    | Get storyboard list              |
| POST   | `/storyboards/generate`           | AI generate storyboards          |
| PUT    | `/storyboards/{id}`               | Update storyboard (multi-char)   |
| POST   | `/image-reference/generate`       | Generate ref image (multi-char)  |
| GET    | `/image-reference/{storyboardId}` | Get storyboard reference image   |
| GET    | `/image-reference/list/{dramaId}` | Batch get reference image status |
| DELETE | `/image-reference/{storyboardId}` | Delete reference image           |
| POST   | `/videos/generate`                | AI generate video (async)        |
| GET    | `/videos/{id}`                    | Query video status               |
| POST   | `/audios/tts`                     | AI voice generation              |
| POST   | `/exports/compose`                | Compose episode into film        |
| GET    | `/ai-configs`                     | Get AI config list               |
| POST   | `/ai-configs`                     | Create/update AI config          |
| GET    | `/task-logs`                      | Task log list                    |

---

## License

[LICENSE](LICENSE)
