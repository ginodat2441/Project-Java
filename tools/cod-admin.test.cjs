const { test } = require('node:test'),
  assert = require('node:assert/strict'),
  fs = require('node:fs');
const { JSDOM } = require('../.local/dom-tests/node_modules/jsdom');
const script = fs.readFileSync(
  __dirname + '/../backend_java/src/main/resources/static/admin/js/pages/orders/detail.js',
  'utf8',
);
async function editor(status = 1) {
  const dom = new JSDOM('<div id="fields"></div><div id="form-error"></div>', {
      url: 'http://localhost/admin',
      runScripts: 'outside-only',
    }),
    w = dom.window;
  w.$ = (s) => w.document.querySelector(s);
  w.esc = (s) => String(s ?? '');
  w.money = (s) => String(s ?? 0);
  w.date = () => '';
  w.statusNames = { 1: 'Đã xác nhận', 2: 'Đang giao', 3: 'Hoàn thành' };
  w.input = (name, label, value) =>
    `<label>${label}<input name="${name}" value="${value || ''}"></label>`;
  w.select = (name, label, options, value) =>
    `<label>${label}<select name="${name}">${options.map((o) => `<option value="${o.ID}" ${String(o.ID) === String(value) ? 'selected' : ''}>${o.NAME}</option>`).join('')}</select></label>`;
  w.button = (label, action, id) =>
    `<button type="button" data-action="${action}" data-id="${id}">${label}</button>`;
  w.table = () => '';
  w.modal = (title, html) => (w.$('#fields').innerHTML = html);
  w.api = async () => ({
    order: {
      BILL_CODE: 'TEST',
      STATUS: status,
      PAYMENT_METHOD: 'COD',
      PAYMENT_STATUS: status === 3 ? 1 : 0,
    },
    lines: [],
    history: [],
    inventoryDocuments: [],
  });
  w.eval(script);
  await w.orderEditor(1);
  return dom;
}
test('admin payment method and collected status cannot be edited on an open COD order', async () => {
  const dom = await editor();
  for (const name of ['paymentMethod', 'paymentStatus'])
    assert.equal(dom.window.$(`[name="${name}"]`).disabled, true);
  assert.equal(dom.window.$('[name="carrier"]').disabled, false);
  dom.window.close();
});
test('finished order locks shipping fields and hides save action', async () => {
  const dom = await editor(3);
  for (const name of ['carrier', 'trackingCode'])
    assert.equal(dom.window.$(`[name="${name}"]`).disabled, true);
  assert.equal(dom.window.$('[data-action="save-shipping"]').hidden, true);
  dom.window.close();
});
