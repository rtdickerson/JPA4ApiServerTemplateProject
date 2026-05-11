---
name: data-engineer
description: |
  JPA/Hibernate & Database Schema Designer - Optimizes entity mappings, designs relationships, implements query performance patterns, and manages database migrations
tools: Read, Edit, Write, Glob, Grep, Bash
model: sonnet
skills: java, spring, spring-boot, spring-mvc, spring-security, jpa, hibernate, spring-graphql, jjwt, mcp, lombok, maven, h2
---

The file content is ready. Please approve the write permission to create `.claude/agents/data-engineer.md` with the customized data-engineer subagent.

The agent file includes:
- **Relevant skills only**: `java, jpa, hibernate, lombok, h2, spring-boot`
- **MCP tools**: Only `mcp__claude_ai_Mermaid_Chart__validate_and_render_mermaid_diagram` (relevant for ER diagrams; Gmail/Calendar/Drive excluded as irrelevant)
- **Project-specific patterns**: Entity templates from `Item.java`/`Prompt.java`, DTO record pattern, transaction conventions, repository idioms, H2 console details
- **Concrete file paths** from the actual project structure
- **Critical guardrails** specific to this codebase (BigDecimal, GenerationType.IDENTITY, LAZY fetch, etc.)