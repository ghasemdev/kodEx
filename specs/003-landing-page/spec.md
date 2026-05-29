# Feature Specification: Landing Page

**Feature Branch**: `feature/003-landing-page`

**Created**: 2026-05-29

**Status**: Draft

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — First-Time Visitor Understands the Platform (Priority: P1)

A developer who has never heard of KodEx lands on the home page. Within seconds, they
understand what KodEx is, see a live demonstration of the core workflow (write code →
compile → test cases pass), and can identify the three exam types available on the platform.

**Why this priority**: Without communicating value instantly, every subsequent feature is
irrelevant — visitors leave before they see the sign-up button. This is the most critical
conversion gate.

**Independent Test**: Load the landing page as a logged-out visitor. The hero section is
visible without scrolling, the animated code demonstration plays automatically, and the
exam type section is reachable by scrolling.

**Acceptance Scenarios**:

1. **Given** a visitor opens the landing page, **When** the page finishes loading,
   **Then** the hero section is immediately visible and the code editor animation starts
   automatically without any user interaction.
2. **Given** the animation is playing in Kotlin mode, **When** the simulated code
   "compiles" successfully, **Then** the test result rows appear one by one and a success
   indicator fires when all tests pass; after a short pause the demo automatically
   switches to Android mode and replays the sequence with an injection-style test scenario.
3. **Given** the Android tab is active in the demo, **When** the injection animation plays,
   **Then** a message indicating hidden test cases are being injected appears, followed
   by test result rows for Android-specific assertions (click handler, ViewModel state,
   UI assertion).
3. **Given** a visitor scrolls down, **When** they reach the exam types section,
   **Then** they see three clearly labelled types (Multiple Choice, I/O Test Cases,
   Injection / Project) each with a short example of what the experience looks like.

---

### User Story 2 — Visitor Explores Gamification and Decides to Sign Up (Priority: P2)

A competitive developer scrolls through the page, sees the badge and tier system, the
trophy case preview, and the leaderboard snippet. They decide the platform is worth
joining and click "Get Started".

**Why this priority**: Gamification is KodEx's primary differentiator. Highlighting it on
the landing page converts developers who care about progress and competition. This directly
drives registration.

**Independent Test**: Navigate to the gamification section via scrolling. Badge cards,
tier progression, and a leaderboard preview row are all visible and interactive. The "Get
Started" button in the hero navigates to the registration page.

**Acceptance Scenarios**:

1. **Given** a visitor scrolls to the gamification section, **When** they hover over a
   badge card, **Then** the card reveals the badge name and the condition to unlock it.
2. **Given** a visitor reads the tier progression bar, **When** they observe it,
   **Then** they can clearly identify four tiers (Junior Coder → Senior Coder → Master →
   Grandmaster) with a visual indicator showing relative progress.
3. **Given** a visitor reads the leaderboard preview, **When** they observe the table,
   **Then** they see at least the top 3 ranked users with their tier, score, and streak
   information.
4. **Given** a visitor clicks "Get Started" or "Sign Up", **When** the click registers,
   **Then** they are navigated to the registration page.

---

### User Story 3 — Visitor Discovers the Problem Dataset (Priority: P3)

A developer interested in practice rather than competition finds the problem dataset
section and understands that KodEx also functions as a personal practice resource (similar
to competitive practice platforms), browsable without an account.

**Why this priority**: Lowers the barrier to entry. Visitors who are not yet ready to
compete can see immediate value in the free problem set, making sign-up feel lower-stakes.

**Independent Test**: Scroll to the problem dataset section. A sample list of problems is
visible with difficulty labels, types, and a link to the full problem list.

**Acceptance Scenarios**:

1. **Given** a visitor scrolls to the problem dataset section, **When** they see the
   sample table, **Then** problems are shown with title, type, difficulty level, and a
   clear indicator that the full list is accessible without login.
2. **Given** a visitor clicks "Explore Problems" (or equivalent), **When** the click
   registers, **Then** they are navigated to the problems catalogue page.

---

### User Story 4 — Educator / Organiser Discovers the Exam Creation Feature (Priority: P4)

A university professor or team lead lands on the page and scrolls to the exam creation
section. They understand they can host their own Kotlin or Android exam with automated
grading. They click the CTA to learn more or sign up.

**Why this priority**: Exam creators are a key supply-side user persona. Surfacing this
feature on the landing page broadens the top of the funnel beyond just participants.

**Independent Test**: Scroll to the "Create Your Exam" section. Key benefits are listed.
The CTA button is clickable.

**Acceptance Scenarios**:

1. **Given** a visitor scrolls to the exam creation section, **When** they see the content,
   **Then** the three exam types, automated grading, and real-time leaderboard are
   mentioned as benefits.
2. **Given** a visitor clicks the exam creation CTA, **When** the click registers,
   **Then** they are directed to either the sign-up page or an exam creation info page.

---

### User Story 5 — Returning Logged-In User Navigates via Navbar (Priority: P2)

A registered user visits the landing page (or any page). They use the global navigation
bar to jump to Problems, Contests, or Leaderboard. Their authentication state, plan
(Free/Pro), and avatar are visible in the navbar. Clicking the avatar reveals a dropdown
with navigation to their profile, badges, and settings.

**Why this priority**: The navbar is present on every page, making its correct behaviour
critical to all authenticated flows.

**Independent Test**: Log in, then load the landing page. The navbar shows the user's
avatar and plan badge. Clicking the avatar opens the dropdown. Each link navigates to
the correct destination.

**Acceptance Scenarios**:

1. **Given** a logged-out visitor is on the landing page, **When** they view the navbar,
   **Then** "Sign In" and "Sign Up" buttons are visible; no avatar or plan badge is shown.
2. **Given** a logged-in user is on any page, **When** they view the navbar,
   **Then** their plan badge (Free or Pro), avatar, and username are shown instead of
   sign-in buttons.
3. **Given** a logged-in user clicks their avatar, **When** the dropdown opens,
   **Then** it contains links to: My Profile, My Badges, My Trophies, My Stats,
   My Contests, Settings, and Log Out.
4. **Given** a logged-in user with the Exam Creator or Admin role, **When** they view
   the navbar, **Then** the "Create Exam" navigation item is visible.
5. **Given** any visitor (logged in or not) clicks a navbar link (Problems, Contests,
   Leaderboard), **When** the click registers, **Then** they are navigated to the
   correct page.

---

### Edge Cases

- What happens when the page loads on a slow connection and the hero animation has not
  yet started — does the page remain usable and readable?
- How is the problem dataset section displayed when the visitor is logged in — is their
  solve status shown in the sample table or hidden on the landing page?
- What does the leaderboard preview show if no contest has taken place yet (empty state)?
- What does the navbar show when a user's session has expired — does it gracefully fall
  back to the logged-out state?
- How does the "Create Exam" CTA behave for a visitor who is logged in but does not have
  the Exam Creator role?

---

## Requirements *(mandatory)*

### Functional Requirements

**Navbar (global)**

- **FR-001**: The navbar MUST be present at the top of every page and display the
  KodEx logo, Problems, Contests, and Leaderboard navigation links, a theme toggle
  (dark / light mode), and a language toggle (English / Persian).
- **FR-002**: When no user is authenticated, the navbar MUST show "Sign In" and "Sign Up"
  buttons in place of the user profile area.
- **FR-003**: When a user is authenticated, the navbar MUST show their avatar, username,
  and plan badge (Free / Pro).
- **FR-004**: The plan badge MUST visually distinguish between Free and Pro plans.
- **FR-005**: Clicking the avatar MUST open a dropdown with: My Profile, My Badges,
  My Trophies, My Stats, My Contests, Settings, Log Out.
- **FR-006**: The "Create Exam" navigation item MUST be visible only to users with the
  Exam Creator or Admin role; it MUST be hidden for regular Participants and guests.
- **FR-007**: The selected theme (dark / light) and language (English / Persian) MUST
  persist across page navigations and browser refreshes for the current visitor.
- **FR-008**: The navbar MUST be fully accessible by keyboard navigation.

**Hero Section**

- **FR-008**: The hero section MUST display a headline, a short tagline, a primary CTA
  ("Get Started"), a secondary CTA ("Explore Problems"), and three animated statistics
  (total problems, total developers, total contests) that count up on page load.
- **FR-009**: The hero section MUST include an animated code editor panel with two tabs:
  a Kotlin tab and an Android tab. The panel plays a scripted sequence automatically:
  code typed character by character → compilation step shown → test result rows appear
  one by one → success indicator fires. The sequence is purely presentational; no real
  code is executed.
- **FR-010**: After the Kotlin tab sequence completes, the demo MUST automatically
  switch to the Android tab and replay an injection-style variant: code typed →
  "injecting hidden test cases" message → test results for Android assertions appear.
- **FR-011**: The visitor MUST be able to manually switch between the Kotlin and Android
  tabs to replay either sequence on demand.
- **FR-012**: The animation MUST loop continuously after both sequences complete.
- **FR-013**: The animation MUST NOT block or delay page interactivity; it MUST be
  purely presentational.

**Exam Types Section**

- **FR-014**: The exam types section MUST present exactly three types: Multiple Choice,
  I/O Test Cases, and Injection / Project.
- **FR-015**: Each exam type MUST include a short description and a minimal illustrative
  example of what the experience looks like.

**Gamification Section**

- **FR-016**: The gamification section MUST show the four tier levels (Junior Coder,
  Senior Coder, Master, Grandmaster) with visual progression indicators.
- **FR-017**: The gamification section MUST show a badge showcase with at least the
  following badges: Kotlin Ninja, Android Architect, Night Owl, Steady Hand, Bug Hunter,
  Speed Demon, Creative Chaos, Hello World.
- **FR-018**: Hovering over a badge card MUST reveal the badge name and the condition
  required to unlock it.
- **FR-019**: The gamification section MUST include a trophy case preview showing
  sample contest placements (1st, 2nd, 3rd).

**Problem Dataset Section**

- **FR-020**: The problem dataset section MUST display a sample list of at least five
  problems with title, type, and difficulty.
- **FR-021**: The problem dataset section MUST include a link to the full problem catalogue.
- **FR-022**: No authentication is required to view the problem sample on the landing page.

**Leaderboard Preview Section**

- **FR-023**: The leaderboard preview MUST show at least the top 3 ranked users with
  their tier badge, total score, solved count, and active streak.
- **FR-024**: When a visitor is logged in, a highlighted "you" row MUST be shown
  indicating the visitor's own rank.
- **FR-025**: The leaderboard preview MUST include a link to the full leaderboard page.

**Exam Creation CTA Section**

- **FR-026**: The exam creation section MUST list the key benefits of hosting an exam
  on KodEx (exam types, automated grading, real-time leaderboard, badge rewards).
- **FR-027**: The exam creation section MUST include a CTA button that directs the
  visitor to the sign-up page (if not logged in) or directly to the exam creation page
  (if logged in with the appropriate role).

**Footer**

- **FR-028**: The footer MUST include links to: About, Contact, Privacy Policy,
  Terms of Service, Cookie Policy.
- **FR-029**: The footer MUST include social media and community links: GitHub, Twitter/X,
  Telegram, Discord.
- **FR-030**: The footer MUST display the KodEx tagline and copyright notice.

**Scroll Animations**

- **FR-031**: All page sections below the hero MUST animate into view when they enter the
  viewport during scrolling — they MUST NOT be visible before the visitor scrolls to them.
- **FR-032**: Scroll animations MUST respect the visitor's operating system reduced-motion
  preference; when reduced motion is enabled, sections appear instantly without transitions.

**Accessibility & Responsiveness**

- **FR-033**: The landing page MUST be fully responsive across mobile (≥ 375 px),
  tablet (≥ 768 px), and desktop (≥ 1280 px) viewports. On mobile, navbar items
  collapse into a hamburger menu.
- **FR-034**: All interactive elements MUST be reachable and operable via keyboard alone.
- **FR-035**: All images and icons MUST have appropriate alternative text or ARIA labels.

---

### Key Entities

- **Visitor** (unauthenticated user): can view all landing page content; statistics are
  read-only; "you" personalisation is absent.
- **Authenticated User**: same content as visitor plus personalised navbar (avatar, plan
  badge, dropdown) and "you" row in leaderboard preview.
- **LandingStats**: aggregate counts (total problems, total registered users, total
  contests) fetched from the backend and displayed in the hero counter animation.
- **BadgeDefinition**: name, icon, and unlock condition for each badge displayed in
  the gamification section.
- **LeaderboardEntry**: user rank, username, tier, total score, solved count, streak
  length — used for the leaderboard preview rows.
- **ProblemSummary**: problem title, exam type, and difficulty level — used for the
  sample problem list rows.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A first-time visitor can identify the platform's purpose and at least two
  key features within 10 seconds of the hero section loading, confirmed by usability
  testing with at least 5 testers reaching ≥ 80% task success.
- **SC-002**: The hero animation sequence completes end-to-end (type → compile → tests
  pass) within 5 seconds of page load on a standard broadband connection.
- **SC-003**: The landing page achieves a Lighthouse Performance score of ≥ 80 on desktop
  and ≥ 70 on mobile; animations do not degrade the score by more than 10 points.
- **SC-004**: All landing page sections are accessible without login; zero auth-gated
  content appears on the page itself.
- **SC-005**: The page renders correctly (no layout breakage, no overlapping elements,
  full content visible) on screens from 375 px to 1920 px wide.
- **SC-006**: Keyboard-only navigation can reach every interactive element (links, buttons,
  badge cards) without mouse input.
- **SC-007**: All scroll-triggered animations play correctly on first scroll-into-view
  and do not replay on subsequent scrolls past the same section.
- **SC-008**: The navbar correctly reflects the visitor's authentication state within
  one page load; no flash of incorrect state (e.g., signed-in UI shown briefly for a
  guest).

---

## Assumptions

- Landing page statistics (total problems, users, contests) are served by a dedicated
  lightweight public API endpoint; no authentication is required to read them.
- The badge definitions (name, icon, unlock condition) are static enough for v1 to be
  hard-coded in the frontend; a dynamic API can replace this in a later spec.
- The sample problem rows and leaderboard preview rows in the landing page use realistic
  placeholder data for v1; they will be replaced with live API data in spec 005
  (Problems) and spec 010 (Leaderboard) respectively.
- The "you" row in the leaderboard preview requires only the viewer's rank and basic
  stats — not a full leaderboard query — and is fetched from the same auth session that
  powers the navbar.
- OAuth providers (GitHub, Google) are covered in spec 004 (Auth); sign-in and sign-up
  buttons on the landing page link to the auth page, not to OAuth flows directly.
- The "Create Exam" link in the navbar is role-gated at render time — no separate
  permission error page is needed for the navbar item itself.
- Mobile layout places navbar items in a hamburger menu; the hamburger design is in scope
  for this spec.
- Reduced-motion preference check is handled client-side by reading the OS/browser
  `prefers-reduced-motion` media query.
