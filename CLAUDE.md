<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan:
specs/003-landing-page/plan.md

Constitution index + quick reference: .specify/memory/constitution.md
Constitution principles (read per task): .specify/memory/principles/<i-x>.md
Tech stack: .specify/memory/tech-stack.md
Security policy: .specify/memory/security_constitution.md
Research (library versions, decisions): specs/003-landing-page/research.md
Data model: specs/003-landing-page/data-model.md
Quick-start: specs/003-landing-page/quickstart.md
<!-- SPECKIT END -->

### Spec Kit

You MUST follow the memory-first workflow defined in `.specify/memory/workflow.md`.
Before planning, prepare context using the best available path:
- MCP: call `speckit_memory_refresh_cache`, `speckit_memory_search`, `speckit_memory_synthesize`
- Spec Kit commands: run `/speckit.memory-md.prepare-context`
- Markdown-first fallback: read `docs/memory/INDEX.md` + `memory-synthesis.md` manually

After implementation, capture durable lessons via `/speckit.memory-md.capture` or `/speckit.memory-md.capture-from-diff`.
