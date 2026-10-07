const assert = require('node:assert/strict');
const { createHash } = require('node:crypto');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const { test } = require('node:test');

test('2024 source hashes, rows, areas and duplicate courses remain consistent', () => {
  const manifest = JSON.parse(readFileSync(join(__dirname, 'manifest.json'), 'utf8'));
  const dataset = JSON.parse(readFileSync(join(__dirname, 'records.json'), 'utf8'));
  assert.equal(manifest.curriculumYear, 2024);
  assert.equal(dataset.curriculumYear, 2024);
  assert.equal(manifest.sources.length, 15);
  assert.equal(dataset.records.length, 190);
  const courses = new Map();
  for (const source of manifest.sources) {
    assert.equal(createHash('sha256').update(readFileSync(join(__dirname, source.file))).digest('hex'), source.sha256);
    assert.equal(dataset.records.filter(r => `sources/${r.sourceFile}` === source.file).length, source.rows);
  }
  for (const row of dataset.records) {
    assert.equal(row.curriculumYear, 2024);
    assert.match(row.code, /^G(?:ED|EE)\d{4}$/);
    assert.match(row.credit, /^\d{1,2}\.\d$/);
    assert.equal(row.recognition, '교양선택');
    assert.equal(row.division, 'GENERAL_ELECTIVE');
    assert.match(row.areaCode, /^(GED[1-6]|GEE[1-7]|CREATIVE)$/);
    assert.ok(row.name && row.organizer && row.sourceRow >= 3);
    const identity = JSON.stringify([row.name, row.credit, row.category]);
    if (courses.has(row.code)) assert.equal(courses.get(row.code), identity);
    courses.set(row.code, identity);
  }
  assert.equal(courses.size, 170);
  assert.equal(manifest.uniqueCourses, courses.size);
  assert.equal(manifest.duplicateRows, dataset.records.length - courses.size);
  const restricted = new Set(dataset.records.filter(r => r.restrictionHint).map(r => r.code));
  assert.deepEqual([...restricted].sort(), manifest.deferredLinks.map(r => r.code).sort());
  assert.equal(restricted.size, 9);
  assert.equal(manifest.defaultSoftwareCurriculumLinks, courses.size - restricted.size);
});
