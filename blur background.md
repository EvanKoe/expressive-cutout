# Blur effect on background cover in the music player



## Context

I got an issue on Github saying: 

```
Add an option to apply a blur effect to the background album art in the music player, rather than simply darkening it.
```



## Goal

I would like you to add a blur slider in `Dynamic tiles > Music` that uses Haze to blur the cover that's in background. It would be a separate feature from the dim background, and both can work alongside.

## Constraints

- Do not install other libraries (use Haze),
- Follow [AGENTS.md](http://AGENTS.md) and [CLAUDE.md](http://CLAUDE.md) 

## Acceptance

- Builds without errors
- Code quality follows CODING_STYLE.md 
- The user sees the options only when `Album art as background` is enabled
- The user sees two settings: a switch to enable/disable the blur, and a slider to set its strength
- The options use `AnimatedVisibility` to animate their apparition/disparition