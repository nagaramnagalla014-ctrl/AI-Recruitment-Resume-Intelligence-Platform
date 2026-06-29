# AI Recruitment & Resume Intelligence Platform

AI-assisted recruitment platform helping recruiters screen resumes efficiently. Extracts skills using NLP, analyzes experience, and ranks candidates against job descriptions. Generates tailored interview questions, candidate summaries, and provides hiring insights via OpenAI GPT-4o.

**Duration:** October 2024 – Present

## Technologies
Java 21 · Spring Boot 3.2 · React 19 · PostgreSQL · Redis · Python · LangChain · OpenAI API (GPT-4o) · Docker · Kubernetes · AWS ECS · GitHub Actions

## Architecture

| Component | Purpose |
|-----------|---------|
| Spring Boot API | Resume parsing, job matching, REST endpoints |
| Resume Parser | Skill extraction via keyword matching + NLP regex |
| AI Analysis | OpenAI GPT-4o for interview questions and summaries |
| Job Matcher | Jaccard similarity scoring for skill overlap |
| Redis | Caching AI responses and session data |
| PostgreSQL | Persisting resumes, jobs, and analysis results |
| GitHub Actions | CI/CD pipeline to AWS ECS |

## Features
- Resume text parsing with skill and experience extraction
- AI-powered candidate-to-job match scoring (0–100)
- Automated interview question generation (Technical/Behavioral/Situational)
- Candidate ranking with strength/gap analysis
- Platform-wide hiring funnel insights
- Falls back to rule-based scoring if OpenAI key absent

## Setup
```bash
# Set your OpenAI key
export OPENAI_API_KEY=sk-...

docker-compose up -d
# App: http://localhost:8080
```

## API Endpoints
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/resumes/upload | Upload and parse resume |
| POST | /api/resumes/{id}/analyze?jobId= | AI analysis vs job |
| GET | /api/jobs/{id}/candidates | Ranked candidates for job |
| GET | /api/analysis/insights | Platform hiring stats |
