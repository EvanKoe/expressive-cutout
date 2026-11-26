#!/usr/bin/env python3
"""Offline localization coverage/format regression checks (stdlib only)."""
from pathlib import Path
import json
import re
import xml.etree.ElementTree as ET

if not __debug__:
    raise SystemExit('Run without -O: these checks use assert')

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'app/src/main/res'

def read(folder):
    entries = {}
    for file in sorted((RES / folder).glob('*.xml')):
        for node in ET.parse(file).getroot():
            if node.tag not in ('string', 'plurals', 'string-array'):
                continue
            name = node.attrib['name']
            assert name not in entries, f'Duplicate resource {name}'
            entries[name] = node
    return entries

def tokens(text):
    # Java Formatter: indexed/unindexed arguments, flags, width, precision, dates and literals.
    pattern = r'%(?:(?:\d+\$)?[-#+ 0,(<]*\d*(?:\.\d+)?(?:[bBhHsScCdoxXeEfgGaA]|[tT][HIklMSLNpzZsQBbhAaCYyjmdeRTrDFc])|[%n])'
    return sorted(re.findall(pattern, text or ''))

assert tokens('%d %1$s %% %02d %1$tH %n') == sorted(['%d', '%1$s', '%%', '%02d', '%1$tH', '%n'])

base = read('values')
zh = read('values-b+zh+Hans')
required = {k: v for k, v in base.items() if v.get('translatable') != 'false'}
assert not required.keys() - zh.keys(), f'Missing: {required.keys() - zh.keys()}'
assert not zh.keys() - base.keys(), f'Unknown: {zh.keys() - base.keys()}'
assert not {k for k,v in base.items() if v.get('translatable') == 'false'} & zh.keys()
for key, source in required.items():
    target = zh[key]
    assert source.tag == target.tag, key
    assert ''.join(target.itertext()).strip(), f'Empty: {key}'
    assert source.get('formatted', 'true') == target.get('formatted', 'true'), key
    if source.get('formatted') == 'false':
        continue
    if source.tag == 'plurals':
        source_items = {item.get('quantity'): item for item in source}
        assert any(item.get('quantity') == 'other' for item in target), key
        for item in target:
            original = source_items.get(item.get('quantity'))
            if original is None:
                original = source_items['other']
            assert tokens(''.join(original.itertext())) == tokens(''.join(item.itertext())), key
    elif source.tag == 'string-array':
        assert len(source) == len(target), key
        for original, item in zip(source, target):
            assert tokens(''.join(original.itertext())) == tokens(''.join(item.itertext())), key
    else:
        assert tokens(''.join(source.itertext())) == tokens(''.join(target.itertext())), f'Format mismatch: {key}'
locales = [n.get('{http://schemas.android.com/apk/res/android}name') for n in ET.parse(RES / 'xml/locales_config.xml').getroot()]
assert len(locales) == len(set(locales)) and 'zh-Hans' in locales
assets = ROOT / 'app/src/main/assets'
en_notes = json.loads((assets / 'changelog.json').read_text(encoding='utf-8'))
zh_notes = json.loads((assets / 'changelog-zh-Hans.json').read_text(encoding='utf-8'))
assert len(en_notes) == len(zh_notes)
notes_count = 0
for en, cn in zip(en_notes, zh_notes):
    assert en['version'] == cn['version']
    assert cn['description'] and cn['description'] != en['description']
    notes_count += 1
    for section in ('features', 'bugfixes'):
        assert len(en[section]) == len(cn[section])
        assert all(a and a != b for a,b in zip(cn[section],en[section]))
        notes_count += len(cn[section])
# Stable icon IDs must remain machine-readable; only separate labels are translated.
catalog = (ROOT / 'app/src/main/java/com/ekoehler/expressivecutout/overlay/MaterialIconCatalog.kt').read_text(encoding='utf-8')
keys = re.findall(r'MaterialIconOption\("([^"]+)"',catalog)
assert keys and len(keys) == len(set(keys))
assert all('icon_label_' + k in zh for k in keys)
resolver = (ROOT / 'app/src/main/java/com/ekoehler/expressivecutout/overlay/IconResolver.kt').read_text(encoding='utf-8')
assert all('"' + k + '"' in resolver for k in ('answer', 'decline', 'hang up', '接听', '拒接', '挂断'))
identical = [k for k in required if ''.join(required[k].itertext()) == ''.join(zh[k].itertext())]
print(json.dumps({'translatable_resources': len(required), 'zh_resources': len(zh),
                  'formatted_resources': sum(v.get('formatted') != 'false' and bool(tokens(''.join(v.itertext()))) for v in required.values()),
                  'plural_resources': sum(v.tag == 'plurals' for v in required.values()),
                  'string_array_resources': sum(v.tag == 'string-array' for v in required.values()), 'changelog_entries': len(zh_notes),
                  'changelog_texts': notes_count, 'icon_labels': len(keys),
                  'intentionally_unchanged_brand_or_data_strings': identical}, ensure_ascii=False, indent=2))
