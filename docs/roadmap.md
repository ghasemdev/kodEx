# KodEx — Product Roadmap & Spec Index

**Last updated**: 2026-06-08
**Active branch**: `develop`

> One row per spec. Each spec lives in `specs/<NNN>-<slug>/` and follows the standard
> Spec Kit layout: `spec.md`, `plan.md`, `tasks.md`, `research.md`, `data-model.md`, `quickstart.md`.

---

## Spec Status

| # | Slug | Title | Status | Branch |
|---|------|-------|--------|--------|
| 001 | `project-base-setup` | Project foundation, CI/CD, Docker, Ktor, Kilua scaffold | ✅ Done | `develop` |
| 002 | `design-system` | Design tokens, components, i18n, screenshot tests | ✅ Done | `develop` |
| 003 | `landing-page` | Public landing page — navbar, hero, features, gamification teaser, footer | ✅ Done | `develop` |
| 004 | `auth` | Register/login (email+password + OAuth GitHub/Google), passkeys, 2FA (TOTP), JWT 15 min, refresh token 7 days (PG), email verification, profile edit | 🔜 Next | — |
| 005 | `exam-browser` | Problems page — catalogue, filters, search, difficulty | ⬜ Planned | — |
| 006 | `contest-list` | Contests page — active, upcoming, past; registration flow | ⬜ Planned | — |
| 007 | `contest-taking` | In-contest exam taking — code editor, submit, SSE status, results | ⬜ Planned | — |
| 008 | `sandbox-execution` | Sandbox runner integration, execution diff viewer, resource stats | ⬜ Planned | — |
| 009 | `exam-creation` | Exam Creator role: create / edit / publish exam, test cases | ⬜ Planned | — |
| 010 | `leaderboard` | Global + per-contest rankings, score history, filters | ⬜ Planned | — |
| 011 | `profile-gamification` | User profile, badge engine, trophy case, skill radar, activity graph | ⬜ Planned | — |
| 012 | `admin-dashboard` | Admin: user management, exam moderation, audit log, platform stats | ⬜ Planned | — |

---

## Access Control Matrix

Most of the site is **publicly accessible** — no login required to browse.
Auth is required only for actions that involve personal data or competition integrity.

| Page / Action | Guest | Participant | Exam Creator | Admin |
|---------------|-------|-------------|--------------|-------|
| Landing page | ✅ | ✅ | ✅ | ✅ |
| Problems list & detail | ✅ | ✅ | ✅ | ✅ |
| Contest list & detail | ✅ | ✅ | ✅ | ✅ |
| Leaderboard (global + contest) | ✅ | ✅ | ✅ | ✅ |
| Public profile (any user) | ✅ | ✅ | ✅ | ✅ |
| Register for a contest | ❌ | ✅ | ✅ | ✅ |
| Submit code (in contest) | ❌ | ✅ | ✅ | ✅ |
| View own profile / stats / badges | ❌ | ✅ | ✅ | ✅ |
| Create / edit / publish exam | ❌ | ❌ | ✅ | ✅ |
| Admin dashboard | ❌ | ❌ | ❌ | ✅ |

> Rate limiting applies to all endpoints; stricter limits on submit and auth routes.

---

## Role Model

```
PARTICIPANT     → default role on register; can browse everything public,
                  join contests, submit code, view own profile.

EXAM_CREATOR    → elevated role (granted by Admin); can create, edit, and
                  publish exams. Also has all PARTICIPANT permissions.

ADMIN           → full platform access; can manage users, moderate exams,
                  view audit logs, grant EXAM_CREATOR role.
                  An Admin CAN also act as EXAM_CREATOR.
```

---

## Global Components (implemented once, used everywhere)

### Navbar

```
╔══════════════════════════════════════════════════════════════════════════╗
║  ⬡ KodEx     [Problems]  [Contests]  [Leaderboard]  [+ Create Exam]     ║
║                                                   [Sign In] [Sign Up]   ║
╚══════════════════════════════════════════════════════════════════════════╝

When authenticated:
╔══════════════════════════════════════════════════════════════════════════╗
║  ⬡ KodEx     [Problems]  [Contests]  [Leaderboard]  [+ Create Exam]     ║
║                                              🆓 Free   [👤 ghasem ▾]   ║
╚══════════════════════════════════════════════════════════════════════════╝

                                                         ┌──────────────────┐
                                                         │ 👤  My Profile   │
                                                         │ 🎖️  My Badges    │
                                                         │ 🏆  My Trophies  │
                                                         │ 📊  My Stats     │
                                                         │ 🏅  My Contests  │
                                                         │ ─────────────── │
                                                         │ ⚙️   Settings    │
                                                         │ 🚪  Log Out      │
                                                         └──────────────────┘
```

- **[Problems]** → Spec 005: full problem catalogue from past contests + practice set
- **[Contests]** → Spec 006: active, upcoming, and past contests + registration
- **[Leaderboard]** → Spec 010: global rankings
- **[+ Create Exam]** → visible only for `EXAM_CREATOR` and `ADMIN` roles
- **Plan badge** (`🆓 Free` / `⭐ Pro`) shown next to profile when logged in
- Profile dropdown: each item navigates to the relevant section of the profile page

### Footer

```
╔══════════════════════════════════════════════════════════════════════════╗
║                                                                          ║
║   ⬡ KodEx                                                                ║
║   The Kotlin & Android competitive coding platform.                      ║
║                                                                          ║
║   Platform          Community         Legal                              ║
║   ─────────         ─────────         ─────                              ║
║   Problems          GitHub →          Privacy Policy                     ║
║   Contests          Twitter / X →     Terms of Service                   ║
║   Leaderboard       Telegram →        Cookie Policy                      ║
║   Create Exam       Discord →                                            ║
║                                       Contact Us                         ║
║   About KodEx                         support@kodex.dev                  ║
║                                                                          ║
║   ─────────────────────────────────────────────────────────────────────  ║
║   © 2026 KodEx · Built with Kotlin                    🐙 GitHub Repo →  ║
╚══════════════════════════════════════════════════════════════════════════╝
```

---

## Exam Types (3 types total)

> **Injection** and **Project** are the same type — project template exams use the same
> hidden-test-case injection mechanism. There is no separate "Project" type in the data model.

| Type | Code | Description |
|------|------|-------------|
| Multiple Choice (Quiz) | `QUIZ` | 4-option question; one or more correct answers |
| I/O Test Cases | `IO` | Contestant writes code; judged by input→output pairs |
| Injection / Project | `INJECTION` | Hidden test cases injected at grade time; works for both standalone Kotlin and Android project templates |

---

## 003 — Landing Page

**Goal**: Convert first-time visitors into sign-ups via an animated, feature-rich public page.
**Stack**: Kilua/JS + **GSAP 3.x** (npm) + ScrollTrigger plugin. Kotlin `@JsModule` wrappers.

### Full page layout

```
┌──────────────────────────────────────────────────────────────────────────┐
│  NAVBAR  (global component — see above)                                  │
├──────────────────────────────────────────────────────────────────────────┤
│  Section 1 — HERO                                                        │
├──────────────────────────────────────────────────────────────────────────┤
│  Section 2 — EXAM TYPES                                                  │
├──────────────────────────────────────────────────────────────────────────┤
│  Section 3 — GAMIFICATION                                                │
├──────────────────────────────────────────────────────────────────────────┤
│  Section 4 — PROBLEM DATASET                                             │
├──────────────────────────────────────────────────────────────────────────┤
│  Section 5 — LEADERBOARD PREVIEW                                         │
├──────────────────────────────────────────────────────────────────────────┤
│  Section 6 — CREATE YOUR EXAM  (CTA)                                     │
├──────────────────────────────────────────────────────────────────────────┤
│  FOOTER  (global component — see above)                                  │
└──────────────────────────────────────────────────────────────────────────┘
```

### Section 1 — Hero

```
┌──────────────────────────────────────────────────────────────────────────┐
│  ⬡ KodEx    [Problems]  [Contests]  [Leaderboard]       [Sign In][Sign Up]│
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│   THE KOTLIN &                      ┌───────────────────────────────┐   │
│   ANDROID CODING                    │  // KodEx Live Demo           │   │
│   ARENA                             │                               │   │
│                                     │  fun solve(n: Int): Int {     │   │
│   Compete. Build. Master.           │    return n * n    ◂── typed  │   │
│                                     │  }                            │   │
│   [Get Started →]                   │                               │   │
│   [Explore Problems]                │  ▶  Compiling...              │   │
│                                     │                               │   │
│   ·  1,247 problems                 │  ✓  Test 1   9ms    PASS     │   │
│   ·  8,432 developers               │  ✓  Test 2   12ms   PASS     │   │
│   ·  342 contests                   │  ✓  Test 3   8ms    PASS 🎉  │   │
│                                     └───────────────────────────────┘   │
│                                                                          │
│  GSAP:  counters roll up · code typed char-by-char (stagger 0.05s)      │
│         compile pulse → test rows slide in one-by-one → confetti burst  │
└──────────────────────────────────────────────────────────────────────────┘
```

### Section 2 — Exam Types

```
┌──────────────────────────────────────────────────────────────────────────┐
│  "Three ways to challenge developers"                                    │
│                                                                          │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────────┐  │
│  │ 📝 Multiple      │  │ ⚙️  I/O Test      │  │ 💉 Injection /       │  │
│  │    Choice        │  │    Cases          │  │    Project           │  │
│  │                  │  │                   │  │                      │  │
│  │  Q: What is a    │  │  Input:           │  │  Your code compiles  │  │
│  │  sealed class?   │  │  5  3             │  │  against hidden test  │  │
│  │                  │  │  ─────────        │  │  cases. No peeking.  │  │
│  │  ○ abstract      │  │  Output:          │  │                      │  │
│  │  ● sealed        │  │  8                │  │  Supports both       │  │
│  │  ○ interface     │  │                   │  │  Kotlin snippets and  │  │
│  │  ○ data          │  │  ✓ Correct        │  │  Android project     │  │
│  │                  │  │                   │  │  templates.          │  │
│  └──────────────────┘  └──────────────────┘  └──────────────────────┘  │
│                                                                          │
│  GSAP:  cards fan in from bottom with stagger on ScrollTrigger enter    │
└──────────────────────────────────────────────────────────────────────────┘
```

### Section 3 — Gamification

```
┌──────────────────────────────────────────────────────────────────────────┐
│  "Level up. Earn badges. Own the leaderboard."             (dark bg)     │
│                                                                          │
│  ┌─── Tiers ──────────────────────────────────────────────────────────┐ │
│  │  🥉 Junior  ──►  🥈 Senior  ──►  🥇 Master  ──►  💎 Grandmaster   │ │
│  │  Coder          Coder                                              │ │
│  │                                                                    │ │
│  │  ████████░░░░░░░░░░░░  You are here: Senior Coder (5 / 10 exams) │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                          │
│  ┌─── Badges ─────────────────────────────────────────────────────────┐ │
│  │                                                                    │ │
│  │  🥷 Kotlin Ninja    🏗️ Android Architect    🌙 Night Owl           │ │
│  │  🔥 Steady Hand     🐛 Bug Hunter           ⚡ Speed Demon          │ │
│  │  🎲 Creative Chaos  👋 Hello World                                 │ │
│  │                                                                    │ │
│  │  ← hover badge → flip card → name + unlock condition shown        │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                          │
│  ┌─── Trophy Case preview ────────────────────────────────────────────┐ │
│  │  🏆  KodEx Open 2026  ·  1st Place  ·  ghasem.shirdel             │ │
│  │  🥈  Kotlin Sprint #3  ·  2nd Place  ·  2026-04-22                │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                          │
│  GSAP:  badge icons float in + shimmer on hover; tier bar animates fill │
└──────────────────────────────────────────────────────────────────────────┘
```

### Section 4 — Problem Dataset

```
┌──────────────────────────────────────────────────────────────────────────┐
│  "Practice on 1,247 real contest problems — or just explore."            │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  [All ▾]  [Kotlin ▾]  [Android ▾]   Easy ●   Medium ○   Hard ○  │   │
│  ├──────────────────────────────────────────────────────────────────┤   │
│  │  #001  Two Sum Kotlin Style           Easy    Kotlin   ✓ Solved  │   │
│  │  #002  Coroutine Flow Merge          Medium   Kotlin   ○         │   │
│  │  #003  Android ViewModel Lifecycle   Medium  Android   ✓ Solved  │   │
│  │  #004  Sealed Class Pattern           Hard    Kotlin   ○         │   │
│  │  #005  Compose Recomposition Fix      Hard   Android   ○         │   │
│  │  ···                                                             │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│  Note: solve status shown only when logged in; list is fully public.    │
│  GSAP:  table rows slide up staggered on ScrollTrigger enter            │
└──────────────────────────────────────────────────────────────────────────┘
```

### Section 5 — Leaderboard Preview

```
┌──────────────────────────────────────────────────────────────────────────┐
│  "See where you stand."                                                  │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  #    Name                Tier         Score   Solved  Streak    │   │
│  ├──────────────────────────────────────────────────────────────────┤   │
│  │  🥇1  ghasem.shirdel      💎 Grand     9,840    342   🔥 47d     │   │
│  │  🥈2  kotlin.ninja        🥇 Master    8,220    289   🔥 23d     │   │
│  │  🥉3  android.arch        🥇 Master    7,105    201   🔥 12d     │   │
│  │   ·   ···                 ···          ···      ···    ···       │   │
│  │  📍48 you                 🥈 Senior    2,430     67   🔥  5d     │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│  [View Full Leaderboard →]                                               │
│                                                                          │
│  "You" row visible only when logged in; rest of table always public.    │
│  GSAP:  score counters roll up; "you" row highlighted and pulsing       │
└──────────────────────────────────────────────────────────────────────────┘
```

### Section 6 — Create Your Exam (CTA)

```
┌──────────────────────────────────────────────────────────────────────────┐
│                                                                          │
│         ╔═══════════════════════════════════════════════════════╗        │
│         ║  "Host your own Kotlin competition."                  ║        │
│         ║                                                       ║        │
│         ║  ✓  Three exam types (Quiz, I/O, Injection)           ║        │
│         ║  ✓  Automated sandboxed grading                       ║        │
│         ║  ✓  Real-time leaderboard                             ║        │
│         ║  ✓  Badge & trophy rewards for participants           ║        │
│         ║  ✓  Export results as CSV                             ║        │
│         ║                                                       ║        │
│         ║       [Create an Exam →]      [See Demo]              ║        │
│         ╚═══════════════════════════════════════════════════════╝        │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

### Tech Notes — 003

```
Animation:   GSAP 3.x + ScrollTrigger
npm dep:     npm("gsap", "3.12.5")  in app/webApp/build.gradle.kts
Interop:     @JsModule("gsap") external object Gsap { ... }

Hero sequence (gsap.timeline):
  1. type code char-by-char          stagger 0.05s per char
  2. cursor blink                    repeat 3
  3. "▶ Compiling..." text swap      0.3s
  4. test rows slide in              stagger 0.15s each
  5. confetti burst on all-pass      gsap.call { confetti() }

Counter roll-up:  gsap.to(el, { innerHTML: 1247, snap: "innerHTML",
                                duration: 2, ease: "power1.out" })
Scroll sections:  ScrollTrigger per section, start = "top 80%"
```

---

## 004 — Authentication

**Goal**: Register, login (email + OAuth), JWT, refresh token, role guard.

```
┌─────────────────────────────────────────────────────────────────┐
│  Sign In                                                         │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                                                           │  │
│  │                   ⬡  KodEx                               │  │
│  │                                                           │  │
│  │   Email      [_____________________________]             │  │
│  │   Password   [_____________________________]  👁          │  │
│  │                                                           │  │
│  │   [Sign In]                  [Forgot Password?]          │  │
│  │                                                           │  │
│  │   ──────────────── or continue with ───────────────      │  │
│  │                                                           │  │
│  │   [🐙  Continue with GitHub]                             │  │
│  │   [G   Continue with Google]                             │  │
│  │                                                           │  │
│  │   Don't have an account?  [Sign Up →]                    │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

**OAuth providers**: GitHub + Google via `ktor-server-auth` OAuth2 server-side code flow (PKCE-ready for future mobile).
**Key tasks**:
- Register / login (email+password) with password strength meter + autocomplete hints
- OAuth GitHub / Google — fetch avatar, name, email on first login
- Passkeys (WebAuthn/FIDO2) — register + authenticate
- Email verification on register (magic link or 6-digit OTP — TBD)
- Forgot password flow
- JWT access token (15 min) + refresh token (7 days, hashed SHA-256, stored in PostgreSQL — no Redis required)
- 2FA TOTP (Google Authenticator / Authy) — enable from profile; nudge banner after first login
- Logout API (revoke refresh token in PG)
- Profile edit: avatar, name, surname, birthdate, location, LinkedIn URL, GitHub URL
- Role guard middleware; `EXAM_CREATOR` role grant by Admin

**Depends on**: 002 (design system).

---

## 005 — Problems (Exam Browser)

**Goal**: Public problem catalogue. Filter by type, difficulty, language. Track solve status.

```
┌────────────────────────────────────────────────────────────────────┐
│  NAVBAR                                                             │
├──────────────────┬─────────────────────────────────────────────────┤
│  SIDEBAR         │  Problems                                        │
│                  │                                                  │
│  Type            │  Search: [___________________________] 🔍        │
│  ● All           │                                                  │
│  ○ Quiz          │  ┌──────────┬────────────┬──────────┬─────────┐ │
│  ○ I/O           │  │ Title    │ Type       │ Diff.    │ Status  │ │
│  ○ Injection     │  ├──────────┼────────────┼──────────┼─────────┤ │
│                  │  │ Two Sum  │ I/O        │ Easy  🟢 │ ✓ Done  │ │
│  Difficulty      │  │ Flows    │ Injection  │ Med   🟡 │ ○       │ │
│  ○ Easy          │  │ ViewModel│ Quiz       │ Med   🟡 │ ✓ Done  │ │
│  ○ Medium        │  │ Sealed   │ I/O        │ Hard  🔴 │ ○       │ │
│  ○ Hard          │  └──────────┴────────────┴──────────┴─────────┘ │
│                  │                                                  │
│  Language        │  [1] [2] [3] ··· [52]  →                        │
│  ○ Kotlin        │                                                  │
│  ○ Android       │  Note: "Status" column visible when logged in   │
└──────────────────┴─────────────────────────────────────────────────┘
```

Problems here are: problems from past contests (public after contest closes) + standalone
practice problems. No separate "LeetCode-style" dataset — same entity, different origin.
**Depends on**: 002 (design), 004 optional (for solve status).

---

## 006 — Contests List

**Goal**: Browse active, upcoming, and past contests. Register for upcoming/active.

```
┌────────────────────────────────────────────────────────────────────┐
│  NAVBAR                                                             │
├────────────────────────────────────────────────────────────────────┤
│  Contests                     [Active ●]  [Upcoming ○]  [Past ○]   │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ 🟢 LIVE   Kotlin Sprint #5                    ends in 1h 23m │  │
│  │           I/O + Injection  ·  3 problems  ·  847 registered  │  │
│  │           [Join Contest →]                                   │  │
│  ├──────────────────────────────────────────────────────────────┤  │
│  │ 🕐 SOON   KodEx Open 2026                  starts in 2d 4h  │  │
│  │           All types  ·  5 problems  ·  1,203 registered      │  │
│  │           [Register →]                                       │  │
│  ├──────────────────────────────────────────────────────────────┤  │
│  │ ⬛ PAST   Android Architecture Cup #2       2026-04-22       │  │
│  │           Injection  ·  3 problems  ·  Final standings →     │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  Register / Join requires auth. Viewing past contests is public.  │
└────────────────────────────────────────────────────────────────────┘
```

**Depends on**: 002, 004 (for registration).

---

## 007 — Contest Taking (Exam Page)

**Goal**: Authenticated contest participant takes an exam: sees problems, writes code,
submits, watches real-time execution status via SSE, sees test results.

```
┌───────────────────────────────────────────────────────────────────────┐
│  Kotlin Sprint #5  ·  Problem 2/3  ·  ⏱ 01:12:44 remaining          │
├──────────────────────────┬────────────────────────────────────────────┤
│  PROBLEM LIST (sidebar)  │  PROBLEM + EDITOR                          │
│                          │                                            │
│  ✓ 1. Two Sum Kotlin     │  Two Sum Kotlin Style       Easy           │
│  ▶ 2. Coroutine Merge    │  ─────────────────────────────────────     │
│  ○ 3. Sealed Dispatch    │  Given a list of ints, return the indices  │
│                          │  of the two numbers that add to target.    │
│  Your score: 100 / 200   │                                            │
│  Rank: #12  (live)       │  Examples                                  │
│                          │  Input:  [2,7,11,15], 9                    │
│                          │  Output: (0, 1)                            │
│                          │                                            │
│                          │  ┌──────────────────────────────────────┐ │
│                          │  │ fun solve(nums: List<Int>,            │ │
│                          │  │           target: Int): Pair<Int,Int> │ │
│                          │  │   // your code here                  │ │
│                          │  └──────────────────────────────────────┘ │
│                          │  Lang: [Kotlin ▾]           Theme: [Dark] │
│                          │                                            │
│                          │  [▶ Run Sample Tests]  [⬆ Submit]         │
│                          │                                            │
│                          │  ┌── Submission Status (SSE) ───────────┐ │
│                          │  │  ⏳ Queued                    00:00   │ │
│                          │  │  ⚙️  Compiling...              00:01  │ │
│                          │  │  🔬 Running test 2/5...        00:02  │ │
│                          │  │  ✅ All 5 tests passed  +100pts 🎉    │ │
│                          │  └───────────────────────────────────────┘ │
└──────────────────────────┴────────────────────────────────────────────┘
```

**Exam type rendering**:
- `QUIZ` → multiple choice form, no code editor
- `IO` → code editor + visible sample test cases + submit
- `INJECTION` → code editor + "hidden tests will run on submit" message

**Depends on**: 004 (auth), 006 (contest entry), 008 (sandbox SSE).

---

## 008 — Sandbox Execution Integration

**Goal**: Frontend SSE consumption for execution status, test diff viewer, resource stats display.

```
┌─────────────────────────────────────────────────────────────┐
│  Execution result (embedded in 007)                          │
│                                                              │
│  ⏳  Queued                              00:00               │
│  ⚙️   Compiling...                        00:01               │
│  🔬  Running test 1 / 5                  00:02               │
│  🔬  Running test 2 / 5                  00:02               │
│  ❌  Test 3 failed                        00:03               │
│                                                              │
│  ┌── Diff: Test 3 ──────────────────────────────────────┐   │
│  │  Expected        │  Got                              │   │
│  │  8               │  9          ← highlighted red     │   │
│  └────────────────────────────────────────────────────── ┘   │
│                                                              │
│  CPU: 34ms  ·  Memory: 12 MB  ·  Exit code: 0               │
│                                                              │
│  Tests: 2 / 5 passed  ·  Score: +40 pts                     │
└─────────────────────────────────────────────────────────────┘
```

**Depends on**: sandbox-runner service (spec 001), 007 (submission trigger).

---

## 009 — Exam Creation (Exam Creator role)

**Goal**: `EXAM_CREATOR` or `ADMIN` creates, configures, previews, and publishes exams.

```
┌─────────────────────────────────────────────────────────────────────────┐
│  Create Exam                                       [Preview]  [Publish] │
├───────────────────────┬─────────────────────────────────────────────────┤
│  OUTLINE              │  EDITOR                                         │
│  ─────────            │                                                 │
│  ① Settings           │  Question 2 — I/O Problem                      │
│  ② Questions          │  ┌─────────────────────────────────────────┐   │
│    Q1 Quiz       ✓    │  │  Title:      [___________________________]│  │
│  ▶ Q2 I/O             │  │  Difficulty: [Medium ▾]                  │  │
│    Q3 Injection       │  │  Statement:  [rich text editor ········] │  │
│  [+ Add Question]     │  │                                          │  │
│                       │  │  Test Cases:                             │  │
│  ③ Contest Settings   │  │  ┌─────────────┬──────────────────────┐ │  │
│  · Duration           │  │  │ Input       │ Expected Output      │ │  │
│  · Max score per Q    │  │  │ [5  3      ]│ [8                 ] │ │  │
│  · Start datetime     │  │  │ [10 20     ]│ [30                ] │ │  │
│  · Registration limit │  │  └─────────────┴──────────────────────┘ │  │
│                       │  │  [+ Add Test Case]    [▶ Run Sample]    │  │
│                       │  └─────────────────────────────────────────┘  │
└───────────────────────┴─────────────────────────────────────────────────┘
```

**State machine**: `DRAFT` → `PUBLISHED` → `CLOSED` (terminal).
Editing test cases forbidden once `PUBLISHED`.
**Depends on**: 004 (role check), 008 (test run preview).

---

## 010 — Leaderboard

**Goal**: Global + per-contest rankings, live score updates during contests, filters.

```
┌────────────────────────────────────────────────────────────────────────┐
│  NAVBAR                                                                 │
├────────────────────────────────────────────────────────────────────────┤
│  Leaderboard                      [Global ▾]  [This Week ▾]            │
│                                                                        │
│  #    Avatar  Name              Tier       Score   Solved  Streak      │
│  ──────────────────────────────────────────────────────────────────    │
│  🥇1         ghasem.shirdel     💎 Grand   9,840    342    🔥 47d      │
│  🥈2         kotlin.ninja       🥇 Master  8,220    289    🔥 23d      │
│  🥉3         android.arch       🥇 Master  7,105    201    🔥 12d      │
│   ·   ·      ···                ···        ···      ···    ···         │
│  📍48        you                🥈 Senior  2,430     67    🔥  5d      │
│  ─────────────────────────────────────────────────────────────────     │
│                                                                        │
│  "You" row: visible only when logged in; pinned at bottom of visible  │
│  window. Full table is always public.                                  │
└────────────────────────────────────────────────────────────────────────┘
```

**Depends on**: 007 (submissions = score source), 011 (tier/badge display).

---

## 011 — Profile & Gamification

**Goal**: Public + private profile page, badge engine, trophy case, skill radar, activity graph.

```
┌───────────────────────────────────────────────────────────────────────┐
│  NAVBAR                                                                │
├───────────────────────────────────────────────────────────────────────┤
│  👤  ghasem.shirdel                                    [Edit Profile] │
│  💎 Grandmaster  ·  Joined May 2026  ·  🆓 Free Plan                  │
├───────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  STATS BAR                                                            │
│  342 solved  ·  9,840 pts  ·  🔥 47d streak  ·  #1 global            │
│                                                                       │
├───────────────┬───────────────────────────────────────────────────────┤
│  ACTIVITY     │  SKILL RADAR (pentagon)                               │
│               │                                                       │
│  Last 52 wks  │            Logic                                      │
│  ▂▄▆█▆▄▂▄▆   │             ▲                                         │
│  ▆█▆▂▄▆▆▆█   │  Framework ◄ ● ► Speed                                │
│               │     ↙           ↘                                    │
│  Easy   138   │  Architecture  Android                                │
│  Medium 163   │                                                       │
│  Hard    41   │  Each axis = avg score across problems in that category│
├───────────────┴───────────────────────────────────────────────────────┤
│                                                                       │
│  BADGES                                                               │
│  ┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐       │
│  │  🥷  │  │  🏗️  │  │  🌙  │  │  🔥  │  │  ⚡  │  │  🐛  │       │
│  │Ninja │  │Arch. │  │Night │  │Steady│  │Speed │  │Bug   │       │
│  │      │  │      │  │Owl   │  │Hand  │  │Demon │  │Hunter│       │
│  └──────┘  └──────┘  └──────┘  └──────┘  └──────┘  └──────┘       │
│  ┌──────┐  ┌──────┐                                                  │
│  │  🎲  │  │  👋  │  [+ 4 locked  🔒]                                │
│  │Chaos │  │Hello │                                                   │
│  └──────┘  └──────┘                                                  │
│                                                                       │
├───────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  TROPHY CASE                                                          │
│  🏆  KodEx Open 2026      ·  1st  ·  "Two Sum Showdown"  2026-05-10  │
│  🥈  Kotlin Sprint #3     ·  2nd  ·  "Coroutine Wars"    2026-04-22  │
│  🎖️  Android Cup #2       ·  3rd  ·  "MVVM Gauntlet"     2026-03-15  │
│                                                                       │
├───────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  CONTEST HISTORY                                                      │
│  ┌───────────────────┬──────────────┬────────┬──────────────────┐    │
│  │ Contest           │ Date         │ Rank   │ Score            │    │
│  ├───────────────────┼──────────────┼────────┼──────────────────┤    │
│  │ KodEx Open 2026   │ 2026-05-10   │  #1    │ 300 / 300        │    │
│  │ Kotlin Sprint #3  │ 2026-04-22   │  #2    │ 240 / 300        │    │
│  │ Android Cup #2    │ 2026-03-15   │  #3    │ 200 / 300        │    │
│  └───────────────────┴──────────────┴────────┴──────────────────┘    │
│                                                                       │
└───────────────────────────────────────────────────────────────────────┘
```

**Badge Engine rules** (server-side, in `server:domain`):

| Badge | Trigger condition |
|-------|-------------------|
| 🥷 Kotlin Ninja | 3 Kotlin exams with 100% score |
| 🏗️ Android Architect | Injection/Project exam scored with "Clean Architecture" tag |
| 🌙 Night Owl | Participate in any exam that starts after 00:00 local time |
| 🔥 Steady Hand | Participate in at least 1 exam per week for 5 consecutive weeks |
| 🐛 Bug Hunter | Submit an accepted bug report on an exam question |
| ⚡ Speed Demon | Submit a correct solution within the first 10 minutes of a contest |
| 🎲 Creative Chaos | Solution flagged "unconventional" by the grader yet passes all tests |
| 👋 Hello World | First code submission ever on the platform |

**Public profile**: badges, trophies, activity graph, skill radar, contest history are all public.
Activity graph and stats require the viewer to be logged in only for the "You" highlighting.
**Depends on**: 007 (submission events trigger badge checks), 010 (rank data for trophies).

---

## 012 — Admin Dashboard

**Goal**: Platform metrics, user management, exam moderation queue, audit log, role assignment.

```
┌─────────────────────────────────────────────────────────────────────┐
│  Admin Dashboard                              [👤 ghasem.shirdel ▾] │
├──────────────┬──────────────────────────────────────────────────────┤
│  SIDEBAR     │  OVERVIEW                                            │
│  ──────────  │  ┌──────────┐  ┌──────────┐  ┌────────────────────┐ │
│  📊 Overview │  │ Users    │  │ Contests │  │ Submissions        │ │
│  👤 Users    │  │  8,432   │  │   342    │  │    47,210          │ │
│  📋 Exams    │  │ +12 today│  │ +2 today │  │  +891 today        │ │
│  🏅 Contests │  └──────────┘  └──────────┘  └────────────────────┘ │
│  📦 Sandbox  │                                                      │
│  📜 Audit    │  EXAM MODERATION QUEUE                               │
│              │  · "Coroutine Challenge"   ninja    [✓ Approve][✗]   │
│              │  · "Android MVVM Quiz"    arch     [✓ Approve][✗]   │
│              │                                                      │
│              │  RECENT AUDIT LOG                                    │
│              │  14:32  user #42 submitted to contest #312           │
│              │  14:31  sandbox timeout — contest #311, prob #2      │
│              │  14:28  admin granted EXAM_CREATOR to user #98       │
└──────────────┴──────────────────────────────────────────────────────┘
```

**Depends on**: all prior specs.

---

## SDD Commands — Starting Spec 003

```bash
# 1. Create feature branch
/speckit-git-feature       # → branch name: feature/003-landing-page

# 2. Write the spec (WHAT to build, not HOW)
/speckit-specify           # → creates specs/003-landing-page/spec.md

# 3. Implementation plan + task breakdown
/speckit-plan              # → creates plan.md + tasks.md

# 4. Implement task by task
/speckit-implement         # → run per task group

# 5. After implementation: capture durable lessons
/speckit-memory-md-capture

# 6. Security review of branch
/speckit-security-review-branch

# 7. Commit
/speckit-git-commit
```

---

## GSAP Kotlin/JS Integration (spec 003)

```kotlin
// build.gradle.kts — add inside jsMain + wasmJsMain dependencies
implementation(npm("gsap", "3.12.5"))

// GsapInterop.kt
@JsModule("gsap")
@JsNonModule
external object Gsap {
    fun to(targets: dynamic, vars: dynamic): dynamic
    fun from(targets: dynamic, vars: dynamic): dynamic
    fun fromTo(targets: dynamic, fromVars: dynamic, toVars: dynamic): dynamic
    fun timeline(vars: dynamic = definedExternally): dynamic
    fun registerPlugin(vararg plugins: dynamic)
    fun set(targets: dynamic, vars: dynamic)
}

@JsModule("gsap/ScrollTrigger")
@JsNonModule
external object ScrollTriggerPlugin

// Usage in landing page init
fun setupHeroAnimation(editorEl: HTMLElement) {
    Gsap.registerPlugin(ScrollTriggerPlugin)
    val tl = Gsap.timeline()
    tl.to(editorEl, js("{ opacity:1, duration:0.4 }"))
    // chain further steps...
}
```

> Framer Motion is **not usable** outside React — confirmed. GSAP is the correct choice.

---

*This roadmap is a living document. Update spec status when branches merge to `develop`.*
