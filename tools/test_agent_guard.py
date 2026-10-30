"""Tests for tools/agent_guard.py:  python3 -m unittest discover -s tools -p 'test_*.py'"""

import json
import subprocess
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

import agent_guard  # noqa: E402

GUARD = Path(__file__).parent / "agent_guard.py"


def on(branch):
    return lambda: branch


class BashRules(unittest.TestCase):
    def denied(self, command, branch="feat/x"):
        return agent_guard.check_bash(command, on(branch)) is not None

    def test_push_to_main_is_denied(self):
        for command in [
            "git push origin main",
            "git push origin HEAD:main",
            "git push origin feat/x:refs/heads/main",
            "git push --all",
            "git push origin --force feat/x",
            "git push -f",
            "git push origin +feat/x",
            "git push --no-verify",
            "cd app && git push origin main",
            "bash -c 'git push origin main'",
            "git status\ngit push origin main",
            "cd /tmp\ngit reset --hard",
        ]:
            with self.subTest(command=command):
                self.assertTrue(self.denied(command))

    def test_bare_push_depends_on_the_branch(self):
        self.assertTrue(self.denied("git push", branch="main"))
        self.assertFalse(self.denied("git push", branch="feat/x"))

    def test_head_resolves_to_the_branch(self):
        self.assertTrue(self.denied("git push -u origin HEAD", branch="main"))
        self.assertTrue(self.denied("git push origin @", branch="main"))
        self.assertFalse(self.denied("git push -u origin HEAD", branch="feat/x"))

    def test_normal_pushes_pass(self):
        for command in [
            "git push -u origin feat/42-blueprint",
            "git push --force-with-lease origin feat/42-blueprint",
        ]:
            with self.subTest(command=command):
                self.assertFalse(self.denied(command))

    def test_hooks_and_discarding_work_are_denied(self):
        for command in [
            "git commit --no-verify -m wip",
            "git commit -n -m wip",
            "git commit -anm wip",
            "git clean --force -d",
            "git -c core.hooksPath=/dev/null commit -m x",
            "git config core.hooksPath .nohooks",
            "git reset --hard origin/main",
            "git clean -fdx",
            "git checkout -- .",
            "git restore .",
        ]:
            with self.subTest(command=command):
                self.assertTrue(self.denied(command))

    def test_data_destruction_is_denied(self):
        for command in [
            "psql $DB_URL -c 'DROP TABLE plants'",
            "echo 'truncate rooms' | psql",
            "atlas schema clean --url $DB_URL",
            "docker volume rm greenprint_pg",
            "docker compose down -v",
            "docker system prune -a --volumes",
            'docker compose exec postgres psql -c "TRUNCATE plants"',
        ]:
            with self.subTest(command=command):
                self.assertTrue(self.denied(command))

    def test_gate_and_shared_daemons_are_protected(self):
        for command in [
            "./gradlew detektBaseline",
            "echo '<x/>' > config/detekt/baseline.xml",
            "./gradlew --stop",
            "pkill -f GradleDaemon",
            "killall java",
        ]:
            with self.subTest(command=command):
                self.assertTrue(self.denied(command))

    def test_mentions_and_everyday_commands_pass(self):
        for command in [
            "git commit -m 'docs: never git push origin main'",
            "grep -rn 'DROP TABLE' server/migrations",
            "git status && git diff",
            "./gradlew prePRcheck --console=plain",
            "docker compose down",
            "git checkout -b feat/42-blueprint",
            "git reset --soft HEAD~1",
            "atlas migrate apply --url $DB_URL",
            "git restore --staged .",
            "git commit -am 'fix: wording'",
            "cat > notes.md <<'EOF'\nnever git push origin main\nEOF\ngit status",
            'git commit -m "first line\nsecond line mentions git reset --hard"',
        ]:
            with self.subTest(command=command):
                self.assertFalse(self.denied(command))


class HookProtocol(unittest.TestCase):
    def run_hook(self, payload):
        return subprocess.run(
            [sys.executable, str(GUARD)], input=payload, capture_output=True, text=True, check=False
        )

    def test_denial_is_hook_json(self):
        out = self.run_hook(json.dumps({"tool_name": "Bash", "tool_input": {"command": "git push origin main"}}))
        self.assertEqual(out.returncode, 0)
        self.assertEqual(json.loads(out.stdout)["hookSpecificOutput"]["permissionDecision"], "deny")

    def test_baseline_edit_is_denied(self):
        payload = {"tool_name": "Write", "tool_input": {"file_path": "/repo/config/detekt/detekt-baseline.xml"}}
        out = self.run_hook(json.dumps(payload))
        self.assertIn("deny", out.stdout)

    def test_allowed_call_prints_nothing(self):
        out = self.run_hook(json.dumps({"tool_name": "Bash", "tool_input": {"command": "ls"}}))
        self.assertEqual((out.returncode, out.stdout), (0, ""))

    def test_bad_input_fails_open(self):
        out = self.run_hook("not json")
        self.assertEqual((out.returncode, out.stdout), (0, ""))
        self.assertIn("skipped", out.stderr)


if __name__ == "__main__":
    unittest.main()
