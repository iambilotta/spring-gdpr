# State machine — spring-gdpr-starter (as-is)

Auto-generated from every DECLARED transition table (`*Transition` enum under `**/domain/**` whose constants carry their resulting state). The state machine is DATA, so this `stateDiagram-v2` is a pure function of the AST — every arc is derived, never hand-drawn. A transition with no single target state (a generic edit, a removal) is listed below the diagram, not drawn as an arc. Run `make code-docs`; the source of truth is the transition table in the code, never this markdown.

_No declared transition table found. Declare the state machine as data — an enum `<Aggregate>Transition` under a `domain` package whose constants name their resulting state — and tracegate renders the diagram from it (no hand-drawing)._
