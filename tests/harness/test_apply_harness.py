from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from scripts.harness_lib.adoption import apply_harness, scan_project


class ApplyHarnessTests(unittest.TestCase):
    def make_target(self, root: Path) -> Path:
        target = root / 'target-service'
        target.mkdir()

        backend = target / 'backend'
        backend.mkdir()
        (backend / 'pom.xml').write_text(
            '<project><dependencies><dependency><artifactId>spring-boot-starter</artifactId></dependency></dependencies></project>',
            encoding='utf-8',
        )

        frontend = target / 'frontend'
        frontend.mkdir()
        (frontend / 'package.json').write_text(
            '{"scripts":{"test":"vitest","build":"vite build","lint":"eslint .","dev":"vite"},"dependencies":{"react":"latest","vite":"latest"}}',
            encoding='utf-8',
        )

        (target / 'AGENTS.md').write_text('existing project instructions\n', encoding='utf-8')
        return target

    def test_scan_detects_backend_and_frontend(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            target = self.make_target(Path(tmp))
            scan = scan_project(target)

            self.assertEqual(scan.backend.path, 'backend')
            self.assertEqual(scan.backend.stack, 'Spring Boot')
            self.assertEqual(scan.backend.test_command, 'mvn test')
            self.assertEqual(scan.primary_frontend.path, 'frontend')
            self.assertEqual(scan.primary_frontend.stack, 'React Vite')
            self.assertEqual(scan.primary_frontend.test_command, 'npm run test')

    def test_dry_run_reports_without_writing(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            tmp_path = Path(tmp)
            target = self.make_target(tmp_path)
            result = apply_harness(Path.cwd(), target)

            self.assertFalse((target / 'docs/harness/context/BASELINE.md').exists())
            self.assertIn('AGENTS.md', result.conflicts)
            self.assertIn('docs/harness/context/BASELINE.md', result.planned)
            self.assertFalse(any(item.startswith('docs/harness/plans/completed/') and item.endswith('.md') for item in result.planned))

    def test_apply_writes_profiles_and_preserves_existing_root_instruction(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            tmp_path = Path(tmp)
            target = self.make_target(tmp_path)
            result = apply_harness(Path.cwd(), target, apply=True)

            baseline = target / 'docs/harness/context/BASELINE.md'
            project_profile = target / 'docs/harness/profiles/project-profile.md'
            design_profile = target / 'docs/harness/profiles/design-system-profile.md'
            harness_yaml = target / 'docs/harness/harness.yaml'
            manifest = target / 'MANIFEST.md'

            self.assertTrue(baseline.exists())
            self.assertTrue(project_profile.exists())
            self.assertTrue(design_profile.exists())
            self.assertTrue(harness_yaml.exists())
            self.assertTrue(manifest.exists())
            self.assertIn('backend', baseline.read_text(encoding='utf-8'))
            self.assertIn('React Vite', project_profile.read_text(encoding='utf-8'))
            self.assertIn('사용하지 않는 영역은 `N/A`', project_profile.read_text(encoding='utf-8'))
            harness_text = harness_yaml.read_text(encoding='utf-8')
            manifest_text = manifest.read_text(encoding='utf-8')
            self.assertIn('agent_orchestration:', harness_text)
            self.assertNotIn('<backend-dir>', harness_text)
            self.assertEqual(manifest_text.split('\n## ', 1)[0].rstrip(), harness_text.rstrip())
            self.assertEqual((target / 'AGENTS.md').read_text(encoding='utf-8'), 'existing project instructions\n')
            self.assertIn('AGENTS.md', result.conflicts)

    def test_profile_only_does_not_copy_managed_scripts(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            tmp_path = Path(tmp)
            target = self.make_target(tmp_path)
            result = apply_harness(Path.cwd(), target, apply=True, profile_only=True)

            self.assertTrue((target / 'docs/harness/context/BASELINE.md').exists())
            self.assertFalse((target / 'scripts/apply-harness-to-project.py').exists())
            self.assertIn('profile-only mode skipped managed file copy', result.notes)

    def test_upgrade_overwrites_managed_docs_but_preserves_filled_profiles(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            tmp_path = Path(tmp)
            target = self.make_target(tmp_path)
            core_doc = target / 'docs/harness/README.md'
            profile = target / 'docs/harness/profiles/project-profile.md'
            core_doc.parent.mkdir(parents=True)
            profile.parent.mkdir(parents=True)
            core_doc.write_text('old core doc\n', encoding='utf-8')
            profile.write_text('filled project profile without placeholders\n', encoding='utf-8')

            result = apply_harness(Path.cwd(), target, apply=True)

            self.assertNotEqual(core_doc.read_text(encoding='utf-8'), 'old core doc\n')
            self.assertEqual(profile.read_text(encoding='utf-8'), 'filled project profile without placeholders\n')
            self.assertIn('docs/harness/profiles/project-profile.md (existing profile without placeholders)', result.skipped)


if __name__ == '__main__':
    unittest.main()
