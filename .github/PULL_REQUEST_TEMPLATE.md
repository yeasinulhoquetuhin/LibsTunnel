## Summary

<!-- What does this change, and why? Link the issue it closes, for example: Closes #12 -->

## Type of change

- [ ] Bug fix
- [ ] New feature
- [ ] Documentation
- [ ] Build or packaging
- [ ] Dependency update

## Checklist

- [ ] `cd engine && go vet ./... && go test ./...` passes.
- [ ] `cd android && ./gradlew assembleDebug` succeeds.
- [ ] Documentation under `docs/` is updated where behaviour changed.
- [ ] `CHANGELOG.md` lists the change.
- [ ] No real server address, UUID, password, private key, `*.dark` or `*.libs` file is included.
- [ ] `third_party/THIRD-PARTY-NOTICES.md` and `third_party/licenses/` are updated if a dependency pin changed.
- [ ] Nothing was copied from a proprietary application.
