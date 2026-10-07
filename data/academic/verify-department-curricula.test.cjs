const assert = require('node:assert/strict');
const { createHash } = require('node:crypto');
const { existsSync, readFileSync } = require('node:fs');
const { join } = require('node:path');
const { test } = require('node:test');

const departments = [
  ['industrial-management', '산업경영학과', [42, 41, 41]],
  ['mechatronics', '메카트로닉스공학과', [46, 45, 45]],
  ['financial-investment', '금융투자학과', [40, 39, 39]],
  ['semiconductor-convergence', '반도체산업융합학과', [13, 26, 33]],
];
const divisions = { 교양필수: 'GENERAL_REQUIRED', 전공기초: 'MAJOR_FOUNDATION', 전공필수: 'MAJOR_REQUIRED', 전공선택: 'MAJOR_ELECTIVE' };

for (const [slug, name, counts] of departments) {
  for (const [index, year] of [2024, 2025, 2026].entries()) {
    test(`${year} ${name}: exact official courses remain in their own year and department`, () => {
      const root = join(__dirname, String(year), slug);
      assert.ok(existsSync(join(root, 'manifest.json')), 'official curriculum dataset is missing');
      const manifest = JSON.parse(readFileSync(join(root, 'manifest.json')));
      const sourceBytes = readFileSync(join(root, 'source.json'));
      const source = JSON.parse(sourceBytes);
      const dataset = JSON.parse(readFileSync(join(root, 'records.json')));
      const records = dataset.records;
      assert.equal(createHash('sha256').update(sourceBytes).digest('hex'), manifest.sourceSha256);
      assert.match(source.responseSha256, /^[a-f0-9]{64}$/);
      assert.equal(new URL(source.sourceUrl).hostname, 'sugang.inha.ac.kr');
      assert.equal(source.curriculumYear, year);
      assert.equal(source.departmentName, name);
      assert.equal(manifest.curriculumYear, year);
      assert.equal(manifest.departmentName, name);
      assert.equal(dataset.curriculumYear, year);
      assert.equal(dataset.departmentName, name);
      assert.equal(manifest.sourceRows, counts[index]);
      assert.equal(manifest.uniqueCourses, counts[index]);
      assert.equal(manifest.published, false);
      assert.equal(source.rows.length, counts[index]);
      assert.equal(records.length, counts[index]);
      assert.equal(new Set(records.map(r => r.code)).size, counts[index]);
      const deferred = [];
      records.forEach((row, i) => {
        const raw = source.rows[i];
        assert.equal(raw.KMAJOR, slug === 'semiconductor-convergence' ? '반도체산업융합' : name.replace(/과$/, ''));
        assert.equal(row.curriculumYear, year);
        assert.equal(row.departmentName, name);
        assert.equal(row.departmentEntityId, manifest.departmentEntityId);
        assert.equal(row.sourceRow, i + 1);
        assert.equal(row.code, raw.HAKSU_NO);
        assert.match(row.code, /^[A-Z]{3}\d{4}$/);
        assert.equal(row.name, raw.KWAMOK_KNAME);
        assert.equal(Number(row.credit), Number(raw.CREDIT));
        assert.equal(row.division, divisions[raw.JONG_KNAME]);
        assert.equal(row.category, raw.JONG_KGUBUN === '전공' ? 'MAJOR' : 'GENERAL_EDUCATION');
        assert.equal(row.areaName, raw.KAREA || null);
        assert.equal(row.majorArea, raw.KJUN_AREA || null);
        assert.equal(row.areaCode, null);
        assert.equal(row.recommendedYear, Number(raw.GAESEUL_YEAR[0]));
        const term = raw.GAESEUL_YEAR.match(/\(([^)]+)학기\)/)?.[1];
        assert.equal(row.recommendedTerm, term === '1' ? 'FIRST' : term === '2' ? 'SECOND' : term === '1,2' ? 'BOTH' : null);
        for (const field of ['KSGUBUN', 'KWAMOK_KGROUP', 'CURRICULUM_BIGO']) {
          if (raw[field]) assert.ok(row.note.includes(raw[field]), `${field} source note lost`);
        }
        const isShared = ['MTH1901', 'MTH1902'].includes(row.code);
        assert.equal(row.importStatus, isShared ? 'DEFERRED_SHARED_MAJOR' : 'READY');
        if (isShared) deferred.push(row.code);
      });
      const expectedDeferred = year > 2024 && ['mechatronics', 'semiconductor-convergence'].includes(slug) ? ['MTH1901', 'MTH1902'] : [];
      assert.deepEqual(deferred.sort(), expectedDeferred);
      assert.deepEqual(manifest.deferredLinks.map(r => r.code).sort(), expectedDeferred);
      assert.equal(manifest.importableLinks, counts[index] - expectedDeferred.length);
    });
  }
}
