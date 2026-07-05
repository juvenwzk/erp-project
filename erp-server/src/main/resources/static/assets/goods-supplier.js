/**
 * 商品-供应商关联：商品表单(关联供应商) + 供应商表单/列表(关联商品)
 */
(function () {
  var STYLE_ID = 'erp-relation-style';
  var cache = { suppliers: null, goods: null };
  var listCache = { goods: {}, supplier: {} };
  var editContext = { goods: null, supplier: null };
  var lastEditClick = { goods: null, supplier: null };
  var openSelect = null;

  function authHeaders() {
    var headers = { 'Content-Type': 'application/json' };
    try {
      var loginStr = localStorage.getItem('erp_login');
      if (loginStr) {
        var info = JSON.parse(loginStr);
        if (info && info.token) headers['Authorization'] = 'Bearer ' + info.token;
      }
    } catch (e) {}
    return headers;
  }

  function apiPath(url) {
    if (!url) return '';
    return url.replace(/^\/api/, '').split('?')[0];
  }

  function sameId(a, b) {
    if (a == null || b == null) return false;
    return parseInt(a, 10) === parseInt(b, 10);
  }

  function normalizeIds(ids) {
    if (!Array.isArray(ids)) return [];
    var out = [];
    ids.forEach(function (id) {
      var n = parseInt(id, 10);
      if (!isNaN(n)) out.push(n);
    });
    return out;
  }

  function apiGet(url) {
    return fetch(url, { headers: authHeaders() })
      .then(function (r) { return r.json(); })
      .then(function (r) {
        if (r.code === 200) return r.data;
        var msg = (typeof window.__resolveApiError === 'function') ? window.__resolveApiError(r) : r.message;
        throw new Error(msg || '请求失败');
      });
  }

  function ensureStyle() {
    if (document.getElementById(STYLE_ID)) return;
    var style = document.createElement('style');
    style.id = STYLE_ID;
    style.textContent = [
      '.erp-rel-field { margin-bottom: 18px; position: relative; }',
      '.erp-rel-label { display: inline-block; width: 90px; text-align: right; padding-right: 12px; color: #606266; font-size: 14px; vertical-align: top; line-height: 32px; box-sizing: border-box; }',
      '.erp-rel-body { display: inline-block; width: calc(100% - 90px); vertical-align: top; }',
      '.erp-rel-select { position: relative; width: 100%; font-size: 14px; }',
      '.erp-rel-select__trigger { min-height: 32px; padding: 2px 30px 2px 8px; border: 1px solid #dcdfe6; border-radius: 4px; background: #fff; cursor: pointer; box-sizing: border-box; display: flex; flex-wrap: wrap; align-items: center; gap: 4px; }',
      '.erp-rel-select__trigger:hover { border-color: #c0c4cc; }',
      '.erp-rel-select.is-open .erp-rel-select__trigger { border-color: #409eff; }',
      '.erp-rel-select__placeholder { color: #a8abb2; line-height: 26px; font-size: 14px; }',
      '.erp-rel-select__arrow { position: absolute; right: 10px; top: 50%; transform: translateY(-50%); color: #a8abb2; font-size: 12px; pointer-events: none; }',
      '.erp-rel-select.is-open .erp-rel-select__arrow { transform: translateY(-50%) rotate(180deg); }',
      '.erp-rel-select__tag { display: inline-flex; align-items: center; height: 24px; padding: 0 8px; font-size: 12px; border-radius: 4px; background: #f0f2f5; color: #606266; border: 1px solid #e4e7ed; }',
      '.erp-rel-select__tag-close { margin-left: 4px; cursor: pointer; color: #909399; }',
      '.erp-rel-select__dropdown { display: none; position: absolute; left: 0; right: 0; top: calc(100% + 4px); z-index: 3000; background: #fff; border: 1px solid #e4e7ed; border-radius: 4px; box-shadow: 0 2px 12px rgba(0,0,0,.1); }',
      '.erp-rel-select.is-open .erp-rel-select__dropdown { display: block; }',
      '.erp-rel-select__filter { padding: 8px 10px 4px; border-bottom: 1px solid #f0f0f0; }',
      '.erp-rel-select__filter input { width: 100%; height: 28px; padding: 0 8px; border: 1px solid #dcdfe6; border-radius: 4px; font-size: 13px; outline: none; box-sizing: border-box; }',
      '.erp-rel-select__list { max-height: 200px; overflow-y: auto; padding: 4px 0; margin: 0; list-style: none; }',
      '.erp-rel-select__option { padding: 0 12px; height: 34px; line-height: 34px; font-size: 14px; color: #606266; cursor: pointer; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }',
      '.erp-rel-select__option:hover { background: #f5f7fa; }',
      '.erp-rel-select__option.is-selected { color: #409eff; font-weight: 500; background: #ecf5ff; }',
      '.erp-rel-select__option.is-hidden { display: none; }',
      '.erp-rel-select__empty { padding: 16px 12px; color: #909399; font-size: 13px; text-align: center; }',
      '.erp-rel-select.is-disabled .erp-rel-select__trigger { background: #f5f7fa; cursor: not-allowed; }',
      'th.erp-col-rel, td.erp-col-rel { min-width: 160px !important; width: 160px !important; }',
      'th.erp-col-rel .cell, td.erp-col-rel .cell { white-space: normal; word-break: break-all; line-height: 1.4; }',
      '.el-table[data-rel-supplier-pending="1"] { visibility: hidden !important; }'
    ].join('\n');
    document.head.appendChild(style);
  }

  function dialogTitle(dlg) {
    var t = dlg.querySelector('.el-dialog__title');
    return t ? (t.textContent || '').trim() : '';
  }

  function detectDialogType(title) {
    if (/分类/.test(title)) return null;
    if (/供应商/.test(title) && !/商品名称|新增商品|编辑商品/.test(title)) return 'supplier';
    if (/商品/.test(title)) return 'goods';
    return null;
  }

  function optionLabel(type, opt) {
    return type === 'goods' ? (opt.supplierName || ('供应商#' + opt.id)) : (opt.goodsName || ('商品#' + opt.id));
  }

  function loadSuppliers() {
    if (cache.suppliers) return Promise.resolve(cache.suppliers);
    return apiGet('/supplier/list').then(function (list) {
      cache.suppliers = Array.isArray(list) ? list : [];
      return cache.suppliers;
    });
  }

  function loadGoodsOptions() {
    if (cache.goods) return Promise.resolve(cache.goods);
    return apiGet('/goods/list?page=1&pageSize=9999').then(function (page) {
      cache.goods = (page && page.rows) ? page.rows : [];
      return cache.goods;
    });
  }

  function readRowId(tr) {
    var cells = tr.querySelectorAll('td');
    if (!cells.length) return null;
    for (var i = 0; i < Math.min(3, cells.length); i++) {
      var n = parseInt((cells[i].textContent || '').trim(), 10);
      if (!isNaN(n) && n > 0) return n;
    }
    return null;
  }

  function readRowName(tr, type) {
    var cells = tr.querySelectorAll('td');
    for (var i = 0; i < cells.length; i++) {
      var text = (cells[i].textContent || '').trim();
      if (!text || text === '-' || /^\d+$/.test(text)) continue;
      if (type === 'goods' && text.length >= 2) return text;
      if (type === 'supplier' && text.length >= 2) return text;
    }
    return null;
  }

  function closeAllSelects(except) {
    document.querySelectorAll('.erp-rel-select.is-open').forEach(function (el) {
      if (except && el === except) return;
      el.classList.remove('is-open');
    });
    if (!except) openSelect = null;
  }

  function applySelection(selectEl, selectedIds) {
    if (!selectEl) return;
    var selected = {};
    normalizeIds(selectedIds).forEach(function (id) { selected[id] = true; });
    selectEl.__selected = selected;
    renderTags(selectEl);
    syncOptionSelected(selectEl);
  }

  function renderTags(selectEl) {
    var trigger = selectEl.querySelector('.erp-rel-select__trigger');
    if (!trigger) return;
    var type = selectEl.getAttribute('data-rel-type');
    var selected = selectEl.__selected || {};
    var options = selectEl.__options || [];
    trigger.innerHTML = '';
    var ids = Object.keys(selected).filter(function (k) { return selected[k]; });
    if (!ids.length) {
      var ph = document.createElement('span');
      ph.className = 'erp-rel-select__placeholder';
      ph.textContent = type === 'goods' ? '请选择关联供应商' : '请选择关联商品';
      trigger.appendChild(ph);
    } else {
      ids.forEach(function (idStr) {
        var id = parseInt(idStr, 10);
        var opt = options.find(function (o) { return sameId(o.id, id); });
        if (!opt) return;
        var tag = document.createElement('span');
        tag.className = 'erp-rel-select__tag';
        tag.appendChild(document.createTextNode(optionLabel(type, opt)));
        var close = document.createElement('span');
        close.className = 'erp-rel-select__tag-close';
        close.innerHTML = '&times;';
        close.addEventListener('click', function (e) {
          e.stopPropagation();
          delete selected[id];
          selectEl.__selected = selected;
          renderTags(selectEl);
          syncOptionSelected(selectEl);
        });
        tag.appendChild(close);
        trigger.appendChild(tag);
      });
    }
    if (!selectEl.querySelector('.erp-rel-select__arrow')) {
      var arrow = document.createElement('span');
      arrow.className = 'erp-rel-select__arrow';
      arrow.innerHTML = '&#9662;';
      selectEl.appendChild(arrow);
    }
  }

  function syncOptionSelected(selectEl) {
    var selected = selectEl.__selected || {};
    selectEl.querySelectorAll('.erp-rel-select__option').forEach(function (li) {
      var id = parseInt(li.getAttribute('data-id'), 10);
      if (selected[id]) li.classList.add('is-selected');
      else li.classList.remove('is-selected');
    });
  }

  function filterOptions(selectEl, keyword) {
    var kw = (keyword || '').trim().toLowerCase();
    var visible = 0;
    selectEl.querySelectorAll('.erp-rel-select__option').forEach(function (li) {
      var text = (li.textContent || '').toLowerCase();
      if (!kw || text.indexOf(kw) >= 0) { li.classList.remove('is-hidden'); visible++; }
      else li.classList.add('is-hidden');
    });
    var empty = selectEl.querySelector('.erp-rel-select__empty');
    if (empty) empty.style.display = visible ? 'none' : 'block';
  }

  function buildSelectField(type, options, selectedIds, entityId) {
    ensureStyle();
    var wrap = document.createElement('div');
    wrap.className = 'erp-rel-field erp-rel-injected';
    wrap.setAttribute('data-rel-form', type);
    wrap.setAttribute('data-rel-entity-id', entityId ? String(entityId) : '');

    var label = document.createElement('span');
    label.className = 'erp-rel-label';
    label.textContent = type === 'goods' ? '关联供应商' : '关联商品';

    var body = document.createElement('div');
    body.className = 'erp-rel-body';
    var selectEl = document.createElement('div');
    selectEl.className = 'erp-rel-select';
    selectEl.setAttribute('data-rel-type', type);
    selectEl.__options = options;
    selectEl.__selected = {};

    var emptyHint = type === 'goods' ? '暂无供应商' : '暂无商品';

    if (!options.length) {
      selectEl.classList.add('is-disabled');
      var dt = document.createElement('div');
      dt.className = 'erp-rel-select__trigger';
      dt.innerHTML = '<span class="erp-rel-select__placeholder">' + emptyHint + '</span>';
      selectEl.appendChild(dt);
    } else {
      var trigger = document.createElement('div');
      trigger.className = 'erp-rel-select__trigger';
      selectEl.appendChild(trigger);

      var dropdown = document.createElement('div');
      dropdown.className = 'erp-rel-select__dropdown';
      var filterWrap = document.createElement('div');
      filterWrap.className = 'erp-rel-select__filter';
      var filterInput = document.createElement('input');
      filterInput.type = 'text';
      filterInput.placeholder = '输入关键字搜索';
      filterInput.addEventListener('input', function () { filterOptions(selectEl, filterInput.value); });
      filterInput.addEventListener('click', function (e) { e.stopPropagation(); });
      filterWrap.appendChild(filterInput);
      dropdown.appendChild(filterWrap);

      var list = document.createElement('ul');
      list.className = 'erp-rel-select__list';
      options.forEach(function (opt) {
        var li = document.createElement('li');
        li.className = 'erp-rel-select__option';
        li.setAttribute('data-id', String(opt.id));
        li.textContent = optionLabel(type, opt);
        li.addEventListener('click', function (e) {
          e.stopPropagation();
          var id = parseInt(opt.id, 10);
          if (selectEl.__selected[id]) delete selectEl.__selected[id];
          else selectEl.__selected[id] = true;
          renderTags(selectEl);
          syncOptionSelected(selectEl);
        });
        list.appendChild(li);
      });
      dropdown.appendChild(list);

      var empty = document.createElement('div');
      empty.className = 'erp-rel-select__empty';
      empty.textContent = '无匹配项';
      empty.style.display = 'none';
      dropdown.appendChild(empty);
      selectEl.appendChild(dropdown);

      trigger.addEventListener('click', function (e) {
        e.stopPropagation();
        if (selectEl.classList.contains('is-open')) {
          selectEl.classList.remove('is-open');
          openSelect = null;
        } else {
          closeAllSelects(selectEl);
          selectEl.classList.add('is-open');
          openSelect = selectEl;
          filterInput.value = '';
          filterOptions(selectEl, '');
          setTimeout(function () { filterInput.focus(); }, 0);
        }
      });
    }

    body.appendChild(selectEl);
    wrap.appendChild(label);
    wrap.appendChild(body);
    wrap.__selectEl = selectEl;
    applySelection(selectEl, selectedIds);
    return wrap;
  }

  function readSelectedIds(wrap) {
    var selectEl = wrap.__selectEl;
    if (!selectEl || !selectEl.__selected) return [];
    return Object.keys(selectEl.__selected).filter(function (k) { return selectEl.__selected[k]; }).map(function (k) { return parseInt(k, 10); });
  }

  function resolveEntityId(dlg, type) {
    var attr = dlg.getAttribute('data-rel-entity-id') || (dlg.querySelector('.erp-rel-injected') && dlg.querySelector('.erp-rel-injected').getAttribute('data-rel-entity-id'));
    if (attr) return parseInt(attr, 10);

    var ctx = editContext[type];
    if (ctx && ctx.id) {
      dlg.setAttribute('data-rel-entity-id', String(ctx.id));
      return ctx.id;
    }
    if (lastEditClick[type]) {
      dlg.setAttribute('data-rel-entity-id', String(lastEditClick[type]));
      return lastEditClick[type];
    }

    if (!/编辑/.test(dialogTitle(dlg))) return null;

    var form = dlg.querySelector('.el-form');
    if (!form) return null;
    var nameInput = form.querySelector('input');
    var nameVal = nameInput ? nameInput.value.trim() : '';
    var map = listCache[type] || {};
    for (var id in map) {
      var row = map[id];
      var rowName = type === 'goods' ? row.goodsName : row.supplierName;
      if (rowName === nameVal) {
        dlg.setAttribute('data-rel-entity-id', String(id));
        return parseInt(id, 10);
      }
    }
    return null;
  }

  function fetchRelationIds(type, entityId) {
    var ctx = editContext[type];
    if (ctx && sameId(ctx.id, entityId)) {
      if (type === 'goods' && ctx.supplierIds) return Promise.resolve(normalizeIds(ctx.supplierIds));
      if (type === 'supplier' && ctx.goodsIds) return Promise.resolve(normalizeIds(ctx.goodsIds));
    }
    var row = listCache[type][entityId];
    if (row) {
      if (type === 'goods' && row.supplierIds && row.supplierIds.length) return Promise.resolve(normalizeIds(row.supplierIds));
      if (type === 'supplier' && row.goodsIds && row.goodsIds.length) return Promise.resolve(normalizeIds(row.goodsIds));
    }
    var url = type === 'goods' ? ('/goods-supplier/by-goods/' + entityId) : ('/goods-supplier/by-supplier/' + entityId);
    return apiGet(url).then(function (relations) {
      return (relations || []).map(function (r) {
        return type === 'goods' ? r.supplierId : r.goodsId;
      });
    });
  }

  function refreshDialogEcho(dlg, type) {
    var wrap = dlg.querySelector('.erp-rel-injected[data-rel-form="' + type + '"]');
    if (!wrap) return;
    var entityId = resolveEntityId(dlg, type);
    if (!entityId) return;
    wrap.setAttribute('data-rel-entity-id', String(entityId));
    dlg.setAttribute('data-rel-entity-id', String(entityId));
    fetchRelationIds(type, entityId).then(function (ids) {
      applySelection(wrap.__selectEl, ids);
    }).catch(function () {});
  }

  function bindSaveButton(dlg, type) {
    var saveBtn = dlg.querySelector('.el-dialog__footer .el-button--primary');
    if (!saveBtn || saveBtn.getAttribute('data-rel-bound-' + type) === '1') return;
    saveBtn.setAttribute('data-rel-bound-' + type, '1');
    saveBtn.addEventListener('click', function () {
      var wrap = dlg.querySelector('.erp-rel-injected[data-rel-form="' + type + '"]');
      if (!wrap) return;
      var entityId = resolveEntityId(dlg, type);
      window.__erpRelPending = {
        type: type,
        entityId: entityId,
        ids: readSelectedIds(wrap)
      };
    }, true);
  }

  function injectDialog(dlg, type) {
    if (detectDialogType(dialogTitle(dlg)) !== type) return;
    var form = dlg.querySelector('.el-form');
    if (!form) return;

    var entityId = resolveEntityId(dlg, type);
    var isEdit = /编辑/.test(dialogTitle(dlg));

    if (dlg.querySelector('.erp-rel-injected[data-rel-form="' + type + '"]')) {
      refreshDialogEcho(dlg, type);
      bindSaveButton(dlg, type);
      return;
    }

    var loader = type === 'goods' ? loadSuppliers() : loadGoodsOptions();
    var relationPromise = (isEdit && entityId) ? fetchRelationIds(type, entityId) : Promise.resolve([]);

    Promise.all([loader, relationPromise]).then(function (res) {
      if (dlg.querySelector('.erp-rel-injected[data-rel-form="' + type + '"]')) {
        refreshDialogEcho(dlg, type);
        return;
      }
      entityId = resolveEntityId(dlg, type) || entityId;
      var field = buildSelectField(type, res[0], res[1] || [], entityId);

      var remarkItem = null;
      form.querySelectorAll('.el-form-item').forEach(function (item) {
        var lbl = item.querySelector('.el-form-item__label');
        if (lbl && /备注/.test(lbl.textContent || '')) remarkItem = item;
      });
      if (remarkItem) form.insertBefore(field, remarkItem);
      else form.appendChild(field);

      dlg.setAttribute('data-rel-done-' + type, '1');
      bindSaveButton(dlg, type);

      if (isEdit && !entityId) {
        var retries = 0;
        var timer = setInterval(function () {
          retries++;
          entityId = resolveEntityId(dlg, type);
          if (entityId || retries >= 12) {
            clearInterval(timer);
            if (entityId) refreshDialogEcho(dlg, type);
          }
        }, 200);
      }
    }).catch(function (err) {
      console.warn('[erp-relation]', err.message);
    });
  }

  function scanDialogs() {
    document.querySelectorAll('.el-dialog').forEach(function (dlg) {
      var type = detectDialogType(dialogTitle(dlg));
      if (!type) return;
      resolveEntityId(dlg, type);
      injectDialog(dlg, type);
    });
  }

  function getHeaderIndex(headerRow, th) {
    var headers = headerRow.querySelectorAll('th');
    for (var i = 0; i < headers.length; i++) {
      if (headers[i] === th) return i;
    }
    return -1;
  }

  function readSupplierRowId(tr) {
    var id = readRowId(tr);
    if (id && listCache.supplier[id]) return id;
    var cells = tr.querySelectorAll('td');
    for (var i = 0; i < cells.length; i++) {
      var name = (cells[i].textContent || '').trim();
      if (!name || name === '-') continue;
      for (var sid in listCache.supplier) {
        if (listCache.supplier[sid].supplierName === name) return parseInt(sid, 10);
      }
    }
    return id;
  }

  var supplierBodyObservers = typeof WeakMap !== 'undefined' ? new WeakMap() : null;

  function getBodyRows(table) {
    return table.querySelectorAll('.el-table__body tr');
  }

  function findRelationTh(headerRow, headerText) {
    var headers = headerRow.querySelectorAll('th');
    var i;
    for (i = 0; i < headers.length; i++) {
      if (headers[i].getAttribute('data-rel-col') === headerText) return headers[i];
    }
    for (i = 0; i < headers.length; i++) {
      if (new RegExp(headerText).test((headers[i].textContent || '').replace(/\s/g, ''))) return headers[i];
    }
    return null;
  }

  function createRelationCell(tag, headerText, text) {
    var el = document.createElement(tag);
    el.className = 'el-table__cell erp-col-rel';
    el.setAttribute('data-rel-col', headerText);
    el.innerHTML = '<div class="cell">' + (text || '-') + '</div>';
    return el;
  }

  function insertColAfterAnchor(table, anchorIndex) {
    table.querySelectorAll('colgroup').forEach(function (cg) {
      var cols = cg.querySelectorAll('col');
      if (!cols[anchorIndex]) return;
      var newCol = document.createElement('col');
      cg.insertBefore(newCol, cols[anchorIndex].nextSibling);
    });
  }

  function isTableAligned(table) {
    var headerRow = table.querySelector('.el-table__header tr');
    if (!headerRow) return false;
    var headerCount = headerRow.querySelectorAll('th').length;
    var rows = getBodyRows(table);
    if (!rows.length) return false;
    for (var i = 0; i < rows.length; i++) {
      if (rows[i].querySelectorAll('td').length !== headerCount) return false;
    }
    return true;
  }

  /** 仅移除脚本注入的列，绝不按索引删 Vue 原生单元格 */
  function removeInjectedRelationColumn(table, headerText) {
    table.querySelectorAll('th[data-rel-col="' + headerText + '"]').forEach(function (el) { el.remove(); });
    table.querySelectorAll('td[data-rel-col="' + headerText + '"]').forEach(function (el) { el.remove(); });
  }

  function insertRelationBodyCell(tr, anchorIndex, headerText, text) {
    if (tr.querySelector('td[data-rel-col="' + headerText + '"]')) return;
    var cells = tr.querySelectorAll('td');
    var anchorCell = cells[anchorIndex];
    var newTd = createRelationCell('td', headerText, text);
    if (anchorCell) {
      tr.insertBefore(newTd, anchorCell.nextSibling);
    } else {
      tr.appendChild(newTd);
    }
  }

  function upsertTableColumn(table, headerText, anchorHeader, getText, readIdFn) {
    var headerRow = table.querySelector('.el-table__header tr');
    if (!headerRow) return false;

    var bodyRows = getBodyRows(table);
    if (!bodyRows.length) return false;

    var anchorIndex = getHeaderIndex(headerRow, anchorHeader);
    if (anchorIndex < 0) return false;

    var readId = readIdFn || readRowId;
    var relTh = findRelationTh(headerRow, headerText);
    var headerCount = headerRow.querySelectorAll('th').length;
    var bodyCount = bodyRows[0].querySelectorAll('td').length;
    var sampleRelTd = bodyRows[0].querySelector('td[data-rel-col="' + headerText + '"]');

    if (relTh && bodyCount < headerCount && !sampleRelTd) {
      bodyRows.forEach(function (tr) {
        var rowId = readId(tr);
        var text = rowId ? (getText(rowId) || '-') : '-';
        insertRelationBodyCell(tr, anchorIndex, headerText, text);
      });
    } else if (!relTh) {
      relTh = createRelationCell('th', headerText, headerText);
      anchorHeader.parentNode.insertBefore(relTh, anchorHeader.nextSibling);
      insertColAfterAnchor(table, anchorIndex);
      bodyRows.forEach(function (tr) {
        var rowId = readId(tr);
        var text = rowId ? (getText(rowId) || '-') : '-';
        insertRelationBodyCell(tr, anchorIndex, headerText, text);
      });
    } else {
      bodyRows.forEach(function (tr) {
        var rowId = readId(tr);
        var text = rowId ? (getText(rowId) || '-') : '-';
        var td = tr.querySelector('td[data-rel-col="' + headerText + '"]');
        if (td) {
          td.innerHTML = '<div class="cell">' + text + '</div>';
        } else {
          insertRelationBodyCell(tr, anchorIndex, headerText, text);
        }
      });
    }

    if (relTh && !relTh.getAttribute('data-rel-col')) {
      relTh.setAttribute('data-rel-col', headerText);
    }

    var aligned = isTableAligned(table);
    table.setAttribute('data-rel-table', aligned ? '1' : '0');
    return aligned;
  }

  function watchSupplierTableBody(table, resyncFn) {
    if (!supplierBodyObservers) return;
    if (supplierBodyObservers.has(table)) return;
    var body = table.querySelector('.el-table__body');
    if (!body) return;
    var timer = null;
    var obs = new MutationObserver(function () {
      if (timer) clearTimeout(timer);
      timer = setTimeout(function () { resyncFn(table); }, 30);
    });
    obs.observe(body, { childList: true, subtree: true });
    supplierBodyObservers.set(table, obs);
  }

  function isSupplierTable(table) {
    var headerRow = table.querySelector('.el-table__header tr');
    if (!headerRow) return false;
    var found = false;
    headerRow.querySelectorAll('th').forEach(function (th) {
      var text = (th.textContent || '').replace(/\s/g, '');
      if (/供应商名称/.test(text)) found = true;
    });
    return found;
  }

  function indexSupplierList(list) {
    if (!Array.isArray(list)) return;
    list.forEach(function (r) {
      if (r && r.id != null) listCache.supplier[r.id] = r;
    });
  }

  function markSupplierTableReady(table) {
    table.setAttribute('data-rel-supplier-ready', '1');
    table.removeAttribute('data-rel-supplier-pending');
    table.style.visibility = '';
  }

  function hideSupplierTableUntilReady(table) {
    if (table.getAttribute('data-rel-supplier-ready') === '1') return;
    table.setAttribute('data-rel-supplier-pending', '1');
    table.style.visibility = 'hidden';
  }

  function prefetchSupplierList() {
    if (Object.keys(listCache.supplier).length) return Promise.resolve();
    return apiGet('/supplier/list').then(function (list) {
      indexSupplierList(list);
    }).catch(function () {});
  }

  function isGoodsTable(table) {
    var headerRow = table.querySelector('.el-table__header tr');
    if (!headerRow) return false;
    var found = false;
    headerRow.querySelectorAll('th').forEach(function (th) {
      var text = (th.textContent || '').replace(/\s/g, '');
      if (/商品名称/.test(text)) found = true;
    });
    return found;
  }

  /** 商品列表不展示关联列（仅在编辑表单中选择供应商） */
  function removeGoodsListRelationColumn() {
    document.querySelectorAll('.el-table').forEach(function (table) {
      if (!isGoodsTable(table)) return;
      var headerRow = table.querySelector('.el-table__header tr');
      if (!headerRow) return;
      var headers = headerRow.querySelectorAll('th');
      var removeIndexes = [];
      headers.forEach(function (th, idx) {
        var text = (th.textContent || '').replace(/\s/g, '');
        if (/关联供应/.test(text)) removeIndexes.push(idx);
      });
      if (!removeIndexes.length) {
        table.removeAttribute('data-rel-goods-col');
        return;
      }
      removeIndexes.sort(function (a, b) { return b - a; });
      removeIndexes.forEach(function (colIdx) {
        var thList = headerRow.querySelectorAll('th');
        if (thList[colIdx]) thList[colIdx].remove();
        table.querySelectorAll('.el-table__body tr').forEach(function (tr) {
          var cells = tr.querySelectorAll('td');
          if (cells[colIdx]) cells[colIdx].remove();
        });
        table.querySelectorAll('colgroup').forEach(function (cg) {
          var cols = cg.querySelectorAll('col');
          if (cols[colIdx]) cols[colIdx].remove();
        });
      });
      table.removeAttribute('data-rel-goods-col');
      table.removeAttribute('data-rel-table');
      table.removeAttribute('data-erp-ready');
      delete table.__erpColClasses;
    });
  }

  function enhanceSupplierTable() {
    document.querySelectorAll('.el-table').forEach(function (table) {
      if (!isSupplierTable(table)) return;
      if (table.getAttribute('data-rel-repairing') === '1') return;

      hideSupplierTableUntilReady(table);
      table.setAttribute('data-rel-repairing', '1');

      var headerRow = table.querySelector('.el-table__header tr');
      if (!headerRow) {
        table.removeAttribute('data-rel-repairing');
        return;
      }
      var anchor = null;
      headerRow.querySelectorAll('th').forEach(function (th) {
        if (/供应商名称/.test((th.textContent || '').replace(/\s/g, ''))) anchor = th;
      });
      if (!anchor) {
        markSupplierTableReady(table);
        table.removeAttribute('data-rel-repairing');
        return;
      }

      function display(id) {
        var row = listCache.supplier[id];
        if (!row) return '-';
        if (row.goodsNames) return row.goodsNames;
        if (row.goods && row.goods.length) return row.goods.map(function (g) { return g.goodsName; }).join('、');
        return '-';
      }

      function syncTable() {
        if (!getBodyRows(table).length) {
          table.removeAttribute('data-rel-repairing');
          return false;
        }
        var aligned = upsertTableColumn(table, '关联商品', anchor, display, readSupplierRowId);
        if (aligned) {
          table.setAttribute('data-rel-supplier-col', '1');
          delete table.__erpColClasses;
          markSupplierTableReady(table);
        }
        table.removeAttribute('data-rel-repairing');
        return aligned;
      }

      watchSupplierTableBody(table, function () {
        if (table.getAttribute('data-rel-repairing') === '1') return;
        if (!Object.keys(listCache.supplier).length) return;
        table.setAttribute('data-rel-repairing', '1');
        syncTable();
      });

      function finishEnhance() {
        if (!syncTable() && getBodyRows(table).length) {
          setTimeout(function () {
            if (table.getAttribute('data-rel-repairing') === '1') return;
            table.setAttribute('data-rel-repairing', '1');
            syncTable();
          }, 80);
        }
      }

      if (Object.keys(listCache.supplier).length) {
        finishEnhance();
        return;
      }

      apiGet('/supplier/list').then(function (list) {
        indexSupplierList(list);
        finishEnhance();
      }).catch(function () {
        markSupplierTableReady(table);
        table.removeAttribute('data-rel-repairing');
      });
    });
  }

  function invalidateListCache() {
    listCache.goods = {};
    listCache.supplier = {};
    document.querySelectorAll('.el-table').forEach(function (table) {
      table.removeAttribute('data-rel-goods-col');
      table.removeAttribute('data-rel-supplier-col');
      table.removeAttribute('data-rel-supplier-ready');
      table.removeAttribute('data-rel-supplier-pending');
      table.style.visibility = '';
    });
  }

  function hookFetch() {
    var prevFetch = window.fetch;
    if (prevFetch.__erpRelHooked) return;

    window.fetch = function (resource, init) {
      var url = typeof resource === 'string' ? resource : (resource && resource.url) || '';
      var path = apiPath(url);
      var method = ((init && init.method) || 'GET').toUpperCase();

      return prevFetch.apply(this, arguments).then(function (res) {
        return res.clone().json().then(function (body) {
          if (!body || body.code !== 200) return res;

          if (method === 'GET' && /^\/goods\/\d+$/.test(path) && body.data) {
            editContext.goods = { id: body.data.id, supplierIds: normalizeIds(body.data.supplierIds) };
            setTimeout(scanDialogs, 50);
          }
          if (method === 'GET' && /^\/supplier\/\d+$/.test(path) && body.data) {
            editContext.supplier = { id: body.data.id, goodsIds: normalizeIds(body.data.goodsIds) };
            setTimeout(scanDialogs, 50);
          }
          if (method === 'GET' && path === '/goods/list' && body.data && body.data.rows) {
            body.data.rows.forEach(function (r) { if (r && r.id != null) listCache.goods[r.id] = r; });
            setTimeout(function () { removeGoodsListRelationColumn(); scanDialogs(); }, 100);
          }
          if (method === 'GET' && path === '/supplier/list' && Array.isArray(body.data)) {
            indexSupplierList(body.data);
            enhanceSupplierTable();
            setTimeout(scanDialogs, 0);
          }

          if (window.__erpRelPending) {
            var isGoodsSave = /\/goods(\?|$)/.test(path) && (method === 'POST' || method === 'PUT');
            var isSupplierSave = /\/supplier(\?|$)/.test(path) && !/\/supplier\/list/.test(path) && (method === 'POST' || method === 'PUT');
            if (isGoodsSave || isSupplierSave) {
              var pending = window.__erpRelPending;
              var entityId = pending.entityId;
              if (method === 'POST' && body.data != null) entityId = typeof body.data === 'number' ? body.data : entityId;
              if (method === 'PUT' && init && init.body) {
                try { var p = JSON.parse(init.body); if (p.id) entityId = p.id; } catch (e) {}
              }
              if (entityId) {
                var bindUrl = pending.type === 'goods'
                  ? '/goods-supplier/goods/' + entityId
                  : '/goods-supplier/supplier/' + entityId;
                var bindBody = pending.type === 'goods'
                  ? { supplierIds: pending.ids || [] }
                  : { goodsIds: pending.ids || [] };
                window.__erpRelPending = null;
                cache.suppliers = null;
                cache.goods = null;
                invalidateListCache();
                return prevFetch(bindUrl, {
                  method: 'PUT',
                  headers: authHeaders(),
                  body: JSON.stringify(bindBody)
                }).then(function () {
                  prefetchSupplierList().then(function () { scheduleScan(true); });
                  return res;
                }).catch(function () { return res; });
              }
            }
          }
          return res;
        }).catch(function () { return res; });
      });
    };
    window.fetch.__erpRelHooked = true;
  }

  function bindEditClick() {
    document.addEventListener('click', function (e) {
      var btn = e.target.closest('.el-button');
      if (!btn || !/编辑/.test((btn.textContent || '').replace(/\s/g, ''))) return;
      var tr = btn.closest('tr');
      if (!tr) return;
      var id = readRowId(tr);
      if (!id) return;
      var page = document.body.getAttribute('data-page');
      if (page === 'goods') lastEditClick.goods = id;
      if (page === 'supplier') lastEditClick.supplier = id;
    }, true);
  }

  function runScan() {
    removeGoodsListRelationColumn();
    scanDialogs();
    enhanceSupplierTable();
  }

  var scanTimer = null;
  function scheduleScan(immediate) {
    if (immediate) {
      runScan();
      return;
    }
    if (scanTimer) clearTimeout(scanTimer);
    scanTimer = setTimeout(runScan, 50);
  }

  function start() {
    ensureStyle();
    hookFetch();
    bindEditClick();
    document.addEventListener('click', function () { closeAllSelects(); });
    prefetchSupplierList();
    removeGoodsListRelationColumn();
    var boot = 0;
    var bootTimer = setInterval(function () {
      removeGoodsListRelationColumn();
      boot++;
      if (boot >= 15) clearInterval(bootTimer);
    }, 400);
    var obs = new MutationObserver(function (mutations) {
      for (var i = 0; i < mutations.length; i++) {
        var nodes = mutations[i].addedNodes;
        for (var j = 0; j < nodes.length; j++) {
          var n = nodes[j];
          if (n.nodeType !== 1) continue;
          if (n.classList && n.classList.contains('el-table') && isSupplierTable(n)) {
            hideSupplierTableUntilReady(n);
          } else if (n.querySelector) {
            n.querySelectorAll('.el-table').forEach(function (t) {
              if (isSupplierTable(t)) hideSupplierTableUntilReady(t);
            });
          }
          if ((n.classList && (n.classList.contains('el-dialog') || n.classList.contains('el-table'))) ||
              (n.querySelector && (n.querySelector('.el-dialog') || n.querySelector('.el-table')))) {
            scheduleScan(true);
            return;
          }
        }
      }
    });
    obs.observe(document.body, { childList: true, subtree: true });
    scheduleScan(true);
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', start);
  else start();
})();
