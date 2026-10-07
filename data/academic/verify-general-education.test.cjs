const assert = require('node:assert/strict');
const { createHash } = require('node:crypto');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const { test } = require('node:test');

for (const [year, rows, unique, linked] of [[2025, 247, 222, 213], [2026, 236, 215, 207]]) {
  test(`${year} official reports retain source fields and year-specific courses`, () => {
    const root = join(__dirname, String(year), 'general-education');
    const manifest = JSON.parse(readFileSync(join(root, 'manifest.json'), 'utf8'));
    const dataset = JSON.parse(readFileSync(join(root, 'records.json'), 'utf8'));
    assert.equal(manifest.curriculumYear, year);
    assert.equal(dataset.curriculumYear, year);
    assert.equal(manifest.sources.length, 15);
    assert.equal(manifest.sourceRows, rows);
    assert.equal(dataset.records.length, rows);
    const projected = new Map();
    for (const source of manifest.sources) {
      const bytes = readFileSync(join(root, source.file));
      assert.equal(createHash('sha256').update(bytes).digest('hex'), source.sha256);
      const original = JSON.parse(bytes);
      assert.equal(original.curriculumYear, year);
      assert.equal(original.sourceUrl, source.sourceUrl);
      assert.equal(original.rows.length, source.rows);
      assert.equal(original.reportedCount, source.reportedCount);
      assert.equal(new URL(original.sourceUrl).hostname, 'sugang.inha.ac.kr');
      assert.match(original.responseSha256, /^[a-f0-9]{64}$/);
      assert.equal(new Set(original.rows.map(r => r.HAKSU_NO)).size, source.rows);
      original.rows.forEach((row, index) => {
        assert.equal(Number(row.CNT), source.reportedCount);
        projected.set(`${source.file.slice(8)}:${index + 1}`, row);
      });
    }
    const courses = new Map();
    const restricted = new Set();
    assert.equal(new Set(dataset.records.map(r => `${r.sourceFile}:${r.sourceRow}`)).size, projected.size);
    for (const row of dataset.records) {
      const original = projected.get(`${row.sourceFile}:${row.sourceRow}`);
      assert.ok(original);
      assert.equal(row.curriculumYear, year);
      assert.equal(row.code, original.HAKSU_NO.trim());
      assert.equal(row.name, original.KWAMOK_KNAME.trim());
      assert.equal(Number(row.credit), Number(original.CREDIT));
      assert.equal(row.organizer, original.KDEPT.trim());
      assert.equal(row.areaName, original.KYOSUN_KAREA.trim());
      assert.equal(row.reportRowNumber, Number(original.RowNum));
      assert.equal(row.sourceNote, original.BIGO);
      assert.equal(row.recognition, original.JONG_KNAME);
      assert.equal(row.category, 'GENERAL_EDUCATION');
      assert.equal(row.division, 'GENERAL_ELECTIVE');
      const sourceArea = row.sourceFile.replace(/\.json$/, '');
      assert.equal(row.areaCode, sourceArea === 'GED1_ENGINEERING' ? 'GED1' : sourceArea);
      assert.equal(row.restrictionHint, sourceArea === 'GED1_ENGINEERING' ? 'engineering-accreditation'
        : /외국인|교환학생/.test(row.name) ? 'international-or-exchange-only' : null);
      assert.match(row.code, /^GE[DE]\d{4}$/);
      assert.match(row.areaCode, /^(GED[1-6]|GEE[1-7]|CREATIVE)$/);
      const identity = JSON.stringify([row.name, Number(row.credit), row.category]);
      if (courses.has(row.code)) assert.equal(courses.get(row.code), identity);
      courses.set(row.code, identity);
      if (row.restrictionHint) restricted.add(row.code);
    }
    assert.equal(courses.size, unique);
    assert.equal(manifest.uniqueCourses, unique);
    assert.equal(manifest.duplicateRows, rows - unique);
    assert.equal(manifest.defaultSoftwareCurriculumLinks, linked);
    assert.equal(unique - restricted.size, linked);
    assert.deepEqual([...restricted].sort(), manifest.deferredLinks.map(r => r.code).sort());
    const discrepancies = manifest.sources.filter(s => s.rows !== s.reportedCount);
    assert.equal(manifest.sourceCountDiscrepancies.length, discrepancies.length);
    for (const discrepancy of manifest.sourceCountDiscrepancies) {
      const source = JSON.parse(readFileSync(join(root, discrepancy.file)));
      const outline = JSON.parse(readFileSync(join(root, discrepancy.outlineSourceFile)));
      assert.equal(outline.rows.length, discrepancy.outlineCrossCheckRows);
      assert.equal(source.rows.length, discrepancy.actualRows);
      assert.equal(source.reportedCount, discrepancy.reportedCount);
      assert.deepEqual(outline.rows.map(r => r.code).sort(), source.rows.map(r => r.HAKSU_NO).sort());
    }
  });
}
