<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
<!-- SPECKIT END -->

## SPECKIT Commands

SPECKIT slash commands are loaded from `.specify/extensions/*/commands/`. 
If a command like `/speckit.maqa-ci.setup` is missing, ensure:
1. Extension folder exists: `.specify/extensions/maqa-ci/`
2. Command file exists: `.specify/extensions/maqa-ci/commands/speckit.maqa-ci.setup.md`
3. Extension is registered in `.specify/extensions.yml` under `installed`

If `/speckit.maqa-ci.setup` is still not available, run:
```bash
specify ext install maqa-ci
```
