/**
 * 统一解析后端 Result 错误文案（兼容 message=「失败」且详情在 data 中的旧格式）
 */
(function (global) {
  function resolveApiError(result, fallback) {
    if (!result || typeof result !== 'object') {
      return fallback || '请求失败';
    }
    var msg = result.message;
    if ((!msg || msg === '失败') && typeof result.data === 'string' && result.data) {
      msg = result.data;
    }
    return msg || fallback || '请求失败';
  }

  global.__resolveApiError = resolveApiError;
})(window);
